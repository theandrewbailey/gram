package gram.servlet;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;
import java.util.TreeSet;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedHashMap;
import java.time.Duration;
import java.time.Instant;
import libWebsiteTools.imead.Local;
import libWebsiteTools.tag.HtmlMeta;
import libWebsiteTools.turbo.RequestTimer;
import gram.IndexFetcher;
import gram.UtilStatic;
import gram.bean.GramLandlord;
import gram.bean.database.Article;
import gram.tag.ArticleUrl;
import gram.bean.GramTenant;

@WebServlet(name = "SearchServlet", description = "Searches articles", urlPatterns = {"/search"})
public class SearchServlet extends GramServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        GramTenant ten = GramLandlord.getTenant(request);
        String ifNoneMatch = request.getHeader(HttpHeaders.IF_NONE_MATCH);
        String etag = request.getAttribute(HttpHeaders.ETAG).toString();
        if (etag.equals(ifNoneMatch)) {
            request.setAttribute(Article.class.getCanonicalName(), null);
            response.sendError(HttpServletResponse.SC_NOT_MODIFIED);
            return;
        }
        int articleLimit = Integer.valueOf(ten.getImeadValue(IndexFetcher.POSTS_PER_PAGE)) * Integer.valueOf(ten.getImeadValue(IndexFetcher.PAGES_AROUND_CURRENT));
        String searchTerm = request.getParameter("searchTerm");
        String searchSuggestion = request.getParameter("suggestion");
        if (null != searchTerm && !searchTerm.isEmpty()) {
            if (50 < searchTerm.length()) {
                response.sendError(422);
                return;
            }
            Instant start = Instant.now();
            List<Article> results = new ArrayList<>(ten.getArts().search(searchTerm, articleLimit));
            Locale loc = Local.resolveLocales(ten.getImead(), request).get(0);
            try {
                if (!results.stream().anyMatch((art) -> {
                    return art.getArticletitle().toLowerCase(loc).contains(searchTerm.toLowerCase(loc)) || art.getPostedmarkdown().toLowerCase(loc).contains(searchTerm.toLowerCase(loc));
                })) {
                    String suggestion = ten.getArts().search(new Article().setSuggestion(searchTerm), 1).get(0).getSuggestion();
                    if (!suggestion.equals(searchTerm)) {
                        request.setAttribute("searchSuggestion", suggestion);
                        request.setAttribute("searchURL", "search?searchTerm=" + URLEncoder.encode(suggestion, "UTF-8"));
                    }
                }
            } catch (IndexOutOfBoundsException | NullPointerException n) {
            }
            RequestTimer.addTiming(request, "query", Duration.between(start, Instant.now()));
            if (results.isEmpty()) {
                showError(request, response, 42);
                return;
            } else if (1 == results.size()) {
                Article art = (Article) results.get(0);
                String url = ArticleUrl.getUrl("/", art, null);
                request.setAttribute(Article.class.getCanonicalName(), art);
                request.getServletContext().getRequestDispatcher(url).forward(request, response);
                return;
            }
            results.stream().limit(2).forEach((art) -> {
                art.setSummary(art.getSummary().replaceAll(" loading=\"lazy\"", ""));
            });
            request.setAttribute("articles", results);
            request.setAttribute("searchterm", searchTerm);
            for (Article art : (List<Article>) results) {
                if (null != art.getImageurl()) {
                    HtmlMeta.addPropertyTag(request, "og:image", art.getImageurl());
                    break;
                }
            }
            HtmlMeta.addPropertyTag(request, "og:site_name", ten.getImead().getLocal(GramServlet.SITE_TITLE, Local.resolveLocales(ten.getImead(), request)));
            HtmlMeta.addPropertyTag(request, "og:type", "website");
            HtmlMeta.addPropertyTag(request, "og:description", "Search results");
            HtmlMeta.addNameTag(request, "description", "Search results");
            HtmlMeta.addNameTag(request, "robots", "noindex");
            request.getServletContext().getRequestDispatcher(IndexServlet.HOME_JSP).forward(request, response);
        } else if (null != searchSuggestion && !searchSuggestion.isEmpty()) {
            if (20 < searchSuggestion.length()) {
                response.sendError(422);
                return;
            }
            ArrayList<String> words = new ArrayList<>(Arrays.asList(searchSuggestion.split("(?<!(\"|').{0,255}) | (?!.*\\1.*)")));
            StringJoiner baseJoin = new StringJoiner(" ");
            ArrayList<String> last2 = new ArrayList<>();
            for (int index = 0; index < words.size(); ++index) {
                if (index + 2 >= words.size()) {
                    last2.add(words.get(index));
                } else {
                    baseJoin.add(words.get(index));
                }
            }
            String base = 0 == baseJoin.length() ? "" : baseJoin.toString() + " ";
            MultivaluedHashMap<Integer, String> countResults = new MultivaluedHashMap<>();
            LinkedHashMap<String, List<String>> wordMap = new LinkedHashMap<>(last2.size());
            baseJoin = new StringJoiner(" ");
            Instant start = Instant.now();
            for (String word : last2) {
                baseJoin.add(word);
                if (word.startsWith("\"") || word.startsWith("-") || word.contains("|")) {
                    wordMap.put(word, Arrays.asList(word));
                } else {
                    List<String> suggs = ten.getArts().search(new Article().setSuggestion(word), Integer.valueOf(ten.getImeadValue(IndexFetcher.POSTS_PER_PAGE))).stream().map((art) -> art.getSuggestion()).toList();
                    wordMap.put(word, suggs);
                }
            }
            for (Article word : ten.getArts().search(new Article().setSuggestion(baseJoin.toString()), Integer.valueOf(ten.getImeadValue(IndexFetcher.POSTS_PER_PAGE)))) {
                List<Article> result = ten.getArts().search(base + word.getSuggestion(), articleLimit);
                if (!result.isEmpty()) {
                    countResults.add(Integer.MIN_VALUE, base + word.getSuggestion());
                }
            }
            RequestTimer.addTiming(request, "query", Duration.between(start, Instant.now()));
            Collection<List<String>> values = wordMap.values();
            Iterator<List<String>> viter = values.iterator();
            List<String> firsts = viter.next();
            List<String> seconds = null;
            if (2 == values.size()) {
                seconds = viter.next();
            }
            if (null == firsts) {
                firsts = seconds;
                seconds = null;
            }
            if (null != firsts) {
                for (String first : firsts) {
                    String stem = base + first;
                    if (null == seconds) {
                        List<Article> result = ten.getArts().search(stem, articleLimit);
                        if (!result.isEmpty()) {
                            countResults.add(result.size(), stem);
                        }
                    } else {
                        for (String second : seconds) {
                            if (!stem.contains(second)) {
                                List<Article> result = ten.getArts().search(stem + second, articleLimit);
                                if (!result.isEmpty()) {
                                    countResults.add(result.size(), stem + second);
                                }
                                result = ten.getArts().search(stem + " " + second, articleLimit);
                                if (!result.isEmpty()) {
                                    countResults.add(result.size(), stem + " " + second);
                                }
                            }
                        }
                    }
                }
            }
            LinkedHashSet<String> suggestions = new LinkedHashSet<>(countResults.size());
            TreeSet<Integer> order = new TreeSet<>(countResults.keySet());
            for (Integer count : order) {
                suggestions.addAll(countResults.get(count));
            }
            JsonArrayBuilder itemList = Json.createArrayBuilder(suggestions);
            request.setAttribute("json", itemList);
            request.getServletContext().getRequestDispatcher(UtilStatic.JSON_OUT).forward(request, response);
        } else {
            response.sendError(422, "Unprocessable Entity");
        }
    }
}
