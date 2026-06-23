package gram.servlet;

import java.io.IOException;
import java.text.MessageFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.HttpHeaders;
import java.time.Duration;
import java.time.Instant;
import libWebsiteTools.security.SecurityRepository;
import libWebsiteTools.imead.Local;
import libWebsiteTools.imead.LocalizedStringNotFoundException;
import libWebsiteTools.rss.FeedBucket;
import libWebsiteTools.turbo.RequestTimer;
import libWebsiteTools.tag.HtmlMeta;
import libWebsiteTools.tag.HtmlTime;
import gram.bean.database.ArticleRepository;
import gram.bean.GramLandlord;
import gram.bean.database.Article;
import gram.tag.ArticleUrl;
import gram.tag.Categorizer;
import gram.bean.GramTenant;
import gram.bean.database.Category;
import jakarta.persistence.NoResultException;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import libWebsiteTools.SearchableRepository;

@WebServlet(name = "ArticleServlet", description = "Gets a single article from the DB with comments", urlPatterns = {"/article/*"})
public class ArticleServlet extends GramServlet {

    private static final String ARTICLE_JSP = "/WEB-INF/singleArticle.jsp";
    public static final String SPAM_WORDS = "site_spamwords";
    public static final Pattern ARTICLE_PATTERN = Pattern.compile(".*?/(?:(?:article)|(?:comments)|(?:edit))/([0-9]*)(?:/[\\w\\-\\.\\(\\)\\[\\]\\{\\}\\+,%_]*/?)?(?:\\?.*)?(?:#.*)?$");
    public static final Pattern URL_PATTERN = Pattern.compile(".*?/(?:(?:article)|(?:comments)|(?:edit))/([\\w\\-\\.\\(\\)\\[\\]\\{\\}\\+,%_]*)");
    public static final Pattern GUESS_ARTICLE = Pattern.compile(".*(?<=/)([^/\\.]*)\\..*");

    /**
     * @param URL
     * @return
     * @throws RuntimeException
     */
    public static String getArticleIdFromURL(String URL) {
        Matcher m = ARTICLE_PATTERN.matcher(URL);
        if (m.matches()) {
            if (null != m.group(1)) {
                return m.group(1);
            }
        }
        throw new NumberFormatException("Can't parse article ID from " + URL);
    }

    public static Article getArticleFromURL(GramTenant ten, String URL) {
        try {
            return ten.getArts().get(Integer.decode(getArticleIdFromURL(URL)));
        } catch (NumberFormatException x) {
            Matcher m = URL_PATTERN.matcher(URL);
            boolean matches = m.matches();
            if (matches) {
                String group1 = m.group(1);
                try {
                    return ten.getArts().get(group1);
                } catch (IllegalStateException | NoResultException n) {
                }
            }
            return null;
        }
    }

    @Override
    protected long getLastModified(HttpServletRequest request) {
        try {
            GramTenant ten = GramLandlord.getTenant(request);
            Instant now = Instant.now();
            Article art = getArticleFromURL(ten, request.getRequestURI());
            RequestTimer.addTiming(request, "query", Duration.between(now, Instant.now()));
            request.setAttribute(Article.class.getCanonicalName(), art);
            return art.getModified().toInstant().toEpochMilli();
        } catch (RuntimeException ex) {
        }
        return 0L;
    }

    @Override
    @SuppressWarnings("UnnecessaryReturnStatement")
    protected void doHead(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        GramTenant ten = GramLandlord.getTenant(request);
        Article art = (Article) request.getAttribute(Article.class.getCanonicalName());
        if (null == art) {
            Instant start = Instant.now();
            try {
                art = getArticleFromURL(ten, request.getRequestURI());
                RequestTimer.addTiming(request, "query", Duration.between(start, Instant.now()));
                request.setAttribute(Article.class.getCanonicalName(), art);
            } catch (RuntimeException ex) {
                if (ten.getArts() instanceof SearchableRepository<Article> searchableArticles) {
                    Matcher m = GUESS_ARTICLE.matcher(request.getRequestURI());
                    if (m.find()) {
                        String term = m.group(1).replace('-', ' ');
                        Collection<Article> seeAlso = searchableArticles.search(term, 18);
                        if (!seeAlso.isEmpty()) {
                            request.setAttribute("seeAlso", seeAlso);
                            request.setAttribute("articles", seeAlso);
                            request.setAttribute("seeAlsoTerm", term);
                        }
                    }
                }
                RequestTimer.addTiming(request, "query", Duration.between(start, Instant.now()));
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
        }
        if (null == art) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
//        String properUrl = ArticleUrl.getUrl(request.getAttribute(SecurityRepository.BASE_URL).toString(), art, null, null, false);
//        String actual = request.getAttribute(AbstractInput.ORIGINAL_REQUEST_URL).toString();

//        if (!actual.contains(properUrl) && null == request.getAttribute("searchSuggestion")) {
//            request.setAttribute(Article.class.getCanonicalName(), null);
//            GramServlet.permaMove(response, properUrl);
//            return;
//        }
        response.setDateHeader(HttpHeaders.DATE, art.getModified().toInstant().toEpochMilli());
        String ifNoneMatch = request.getHeader(HttpHeaders.IF_NONE_MATCH);
        String etag = request.getAttribute(HttpHeaders.ETAG).toString();

        if (etag.equals(ifNoneMatch)) {
            request.setAttribute(Article.class.getCanonicalName(), null);
            response.sendError(HttpServletResponse.SC_NOT_MODIFIED);
            return;
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doHead(request, response);
        Article art = (Article) request.getAttribute(Article.class.getCanonicalName());
        if (null != art && !response.isCommitted()) {
            showArticle(request, response, art);
        }
    }

    public static void showArticle(HttpServletRequest request, HttpServletResponse response, Article art) throws ServletException, IOException {
        GramTenant ten = GramLandlord.getTenant(request);
        List<Article> pageArticles = Collections.synchronizedList(new ArrayList<>());
        pageArticles.add(art);
        Instant start = Instant.now();
        if (ten.getArts() instanceof SearchableRepository<Article> searchableArticles) {
            Collection<Article> seeAlso = getArticleSuggestions(searchableArticles, art);
            request.setAttribute("seeAlso", seeAlso);
            RequestTimer.addTiming(request, "seeAlsoQuery", Duration.between(start, Instant.now()));
            pageArticles.addAll(seeAlso);
        }
        request.setAttribute("seeAlsoTerm", null != art.getSuggestion() ? art.getSuggestion() : ArticleRepository.getArticleSuggestionTerm(art));
        List<Locale> resolvedLocales = Local.resolveLocales(ten.getImead(), request);
        // keep track of articles referenced on the page, to help de-duplicate links and maximize unique articles linked to
        request.setAttribute("articles", pageArticles);
        request.setAttribute(Article.class.getSimpleName(), art);
        request.setAttribute("title", art.getArticletitle());
        request.setAttribute("articleCategory", art.getCategoryid());
        String commentCount = " " + (1 == art.getCommentCollection().size()
                ? ("1 " + ten.getImead().getLocal("page_comment", resolvedLocales) + ".")
                : (art.getCommentCollection().size() + " " + ten.getImead().getLocal("page_comments", resolvedLocales) + "."));
        request.setAttribute("commentCount", commentCount);
        if (null == art.getCategoryid()) {
            art.setCategoryid(new Category(ten.getImead().getLocal(GramServlet.SITE_TITLE, resolvedLocales)));
        }
        String catName = art.getCategoryid().getName();
        if (art.getComments()) {
            request.setAttribute("commentForm", "comments/" + art.getArticleid() + "?iframe");
            String format = FeedBucket.TIME_FORMAT;
            try {
                format = ten.getImead().getLocal(HtmlTime.SITE_DATEFORMAT_LONG, resolvedLocales);
            } catch (LocalizedStringNotFoundException x) {
            }
            DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern(format);
            String postedDate = timeFormat.format(art.getPosted().toZonedDateTime());
            String footer = MessageFormat.format(ten.getImead().getLocal("page_articleFooter", resolvedLocales),
                    new Object[]{postedDate, catName}) + commentCount;
            request.setAttribute("commentFormTitle", footer);
        }
        HtmlMeta.addNameTag(request, "description", art.getDescription());
        HtmlMeta.addNameTag(request, "author", art.getPostedname());
        if (null == request.getParameter("milligram")) {
            String canonical = ArticleUrl.getUrl(request.getAttribute(SecurityRepository.BASE_URL).toString(), art, null, null, true);
            HtmlMeta.addLink(request, "canonical", canonical);
            HashSet<Locale> locales = new HashSet<>(ten.getImead().getLocales());
            locales.add(Locale.getDefault());
            for (Locale l : locales) {
                if ("und".equals(l.toLanguageTag())) {
                    continue;
                }
                String base = ten.getImeadValue(SecurityRepository.BASE_URL);
                if (l != Locale.getDefault() && !l.toLanguageTag().isEmpty()) {
                    base += l.toLanguageTag() + "/";
                }
                String langUrl = ArticleUrl.getUrl(base, art, null, null, false);
                if (!langUrl.equals(canonical)) {
                    HtmlMeta.addLocaleURL(request, l, langUrl);
                }
            }

            HtmlMeta.addPropertyTag(request, "og:title", art.getArticletitle());
            HtmlMeta.addPropertyTag(request, "og:url", ArticleUrl.getUrl(request.getAttribute(SecurityRepository.BASE_URL).toString(), art, null, null, false));
            if (null != art.getImageurl()) {
                HtmlMeta.addPropertyTag(request, "og:image", art.getImageurl());
            }
            if (null != art.getDescription()) {
                HtmlMeta.addPropertyTag(request, "og:description", art.getDescription());
            }
            HtmlMeta.addPropertyTag(request, "og:site_name", ten.getImead().getLocal(GramServlet.SITE_TITLE, resolvedLocales));
            HtmlMeta.addPropertyTag(request, "og:type", "article");
            HtmlMeta.addPropertyTag(request, "og:article:published_time", DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(art.getPosted().toZonedDateTime()));
            HtmlMeta.addPropertyTag(request, "og:article:modified_time", DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(art.getModified().toZonedDateTime()));
            HtmlMeta.addPropertyTag(request, "og:article:author", art.getPostedname());
            HtmlMeta.addPropertyTag(request, "og:article:section", catName);
            if (null != art.getImageurl()) {
                JsonArrayBuilder image = Json.createArrayBuilder();
                image.add(art.getImageurl());
                JsonObjectBuilder article = Json.createObjectBuilder().add("@context", "https://schema.org").add("@type", "Article").
                        add("headline", art.getArticletitle()).add("image", image).
                        add("datePublished", DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(art.getPosted().toZonedDateTime())).
                        add("dateModified", DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(art.getModified().toZonedDateTime()));
                HtmlMeta.addLDJSON(request, article.build().toString());
            }
            JsonArrayBuilder itemList = Json.createArrayBuilder();
            itemList.add(HtmlMeta.getLDBreadcrumb(ten.getImead().getLocal("page_title", resolvedLocales), 1, request.getAttribute(SecurityRepository.BASE_URL).toString()));
            itemList.add(HtmlMeta.getLDBreadcrumb(catName, 2, Categorizer.getUrl(request.getAttribute(SecurityRepository.BASE_URL).toString(), null != art.getCategoryid() ? art.getCategoryid().getName() : null, null)));
            itemList.add(HtmlMeta.getLDBreadcrumb(art.getArticletitle(), 3, ArticleUrl.getUrl(ten.getImeadValue(SecurityRepository.BASE_URL), art, null, null, false)));
            JsonObjectBuilder breadcrumbs = Json.createObjectBuilder().add("@context", "https://schema.org").add("@type", "BreadcrumbList").add("itemListElement", itemList);
            HtmlMeta.addLDJSON(request, breadcrumbs.build().toString());
        }
        request.getServletContext().getRequestDispatcher(ARTICLE_JSP).forward(request, response);
    }

    /**
     *
     * @param arts Article repository to get articles from
     * @param art get articles similar to this one
     * @return similar articles, or null if something exploded
     */
    @SuppressWarnings("unchecked")
    public static Collection<Article> getArticleSuggestions(SearchableRepository<Article> arts, Article art) {
        try {
            // search with article's search term, up to 12
            Article tempArt = new Article(art.getArticleid());
            tempArt.setArticletitle(art.getArticletitle());
            tempArt.setSuggestion(null != art.getSuggestion() ? art.getSuggestion() : ArticleRepository.getArticleSuggestionTerm(art));
            tempArt.setUrl(ArticleUrl.getUrl("", art, null, null, false));
            tempArt.setCategoryid(art.getCategoryid());
            Collection<Article> seeAlso = new LinkedHashSet<>(arts.search(tempArt, 8));
            // limit to 6
            List<Article> tempArr = Arrays.asList(Arrays.copyOf(seeAlso.toArray(Article[]::new), 6));
            seeAlso = new ArrayList(tempArr);
            seeAlso.removeAll(Collections.singleton(null));
            // sort articles without images last
            for (Article a : tempArr) {
                if (null == a) {
                    break;
                } else if (null == a.getImageurl()) {
                    seeAlso.remove(a);
                    seeAlso.add(a);
                }
            }
            if (!seeAlso.isEmpty()) {
                return seeAlso;
            }
        } catch (Exception x) {
            throw new RuntimeException(x);
        }
        return new ArrayList<>();
    }
}
