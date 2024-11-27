package toilet.servlet;

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
import libWebsiteTools.security.SecurityRepo;
import libWebsiteTools.imead.Local;
import libWebsiteTools.imead.LocalizedStringNotFoundException;
import libWebsiteTools.rss.FeedBucket;
import libWebsiteTools.turbo.RequestTimer;
import libWebsiteTools.tag.AbstractInput;
import libWebsiteTools.tag.HtmlMeta;
import libWebsiteTools.tag.HtmlTime;
import toilet.ArticleProcessor;
import toilet.IndexFetcher;
import toilet.bean.ArticleRepository;
import toilet.bean.ToiletBeanAccess;
import toilet.bean.database.Article;
import toilet.tag.ArticleUrl;
import toilet.tag.Categorizer;

@WebServlet(name = "ArticleServlet", description = "Gets a single article from the DB with comments", urlPatterns = {"/article/*", "/amp/*"})
public class ArticleServlet extends ToiletServlet {

    private static final String ARTICLE_JSP = "/WEB-INF/singleArticle.jsp";
    public static final String SPAM_WORDS = "site_spamwords";

    @Override
    protected long getLastModified(HttpServletRequest request) {
        try {
            ToiletBeanAccess beans = allBeans.getInstance(request);
            Instant now = Instant.now();
            Article art = IndexFetcher.getArticleFromURI(beans, request.getRequestURI());
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
        ToiletBeanAccess beans = allBeans.getInstance(request);
        Article art = (Article) request.getAttribute(Article.class.getCanonicalName());
        if (null == art) {
            Instant start = Instant.now();
            try {
                art = IndexFetcher.getArticleFromURI(beans, request.getRequestURI());
                RequestTimer.addTiming(request, "query", Duration.between(start, Instant.now()));
                request.setAttribute(Article.class.getCanonicalName(), art);
            } catch (RuntimeException ex) {
                RequestTimer.addTiming(request, "query", Duration.between(start, Instant.now()));
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
        }
        if (null == art) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String properUrl = ArticleUrl.getUrl(request.getAttribute(SecurityRepo.BASE_URL).toString(), art, null);
        String actual = request.getAttribute(AbstractInput.ORIGINAL_REQUEST_URL).toString();
        if (!actual.contains(properUrl) && null == request.getAttribute("searchSuggestion")) {
            request.setAttribute(Article.class.getCanonicalName(), null);
            ToiletServlet.permaMove(response, properUrl);
            return;
        }
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
            ToiletBeanAccess beans = allBeans.getInstance(request);
            List<Article> pageArticles = Collections.synchronizedList(new ArrayList<>());
            pageArticles.add(art);
            request.setAttribute("seeAlso", beans.getExec().submit(() -> {
                Instant start = Instant.now();
                Collection<Article> seeAlso = getArticleSuggestions(beans.getArts(), art);
                RequestTimer.addTiming(request, "seeAlsoQuery", Duration.between(start, Instant.now()));
                pageArticles.addAll(seeAlso);
                return seeAlso;
            }));
            List<Locale> resolvedLocales = Local.resolveLocales(beans.getImead(), request);
            request.setAttribute("seeAlsoTerm", null != art.getSuggestion() ? art.getSuggestion() : ArticleRepository.getArticleSuggestionTerm(art));
            // keep track of articles referenced on the page, to help de-duplicate links and maximize unique articles linked to
            request.setAttribute("articles", pageArticles);
            request.setAttribute(Article.class.getSimpleName(), art);
            request.setAttribute("title", art.getArticletitle());
            request.setAttribute("articleCategory", art.getSectionid().getName());
            String commentCount = " " + (1 == art.getCommentCollection().size()
                    ? ("1 " + beans.getImead().getLocal("page_comment", resolvedLocales) + ".")
                    : (art.getCommentCollection().size() + " " + beans.getImead().getLocal("page_comments", resolvedLocales) + "."));
            request.setAttribute("commentCount", commentCount);
            if (art.getComments()) {
                request.setAttribute("commentForm", getCommentFormUrl(art, (Locale) request.getAttribute(Local.OVERRIDE_LOCALE_PARAM)));
                String format = FeedBucket.TIME_FORMAT;
                try {
                    format = beans.getImead().getLocal(HtmlTime.SITE_DATEFORMAT_LONG, resolvedLocales);
                } catch (LocalizedStringNotFoundException x) {
                }
                DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern(format);
                String postedDate = timeFormat.format(art.getPosted().toZonedDateTime());
                String footer = MessageFormat.format(beans.getImead().getLocal("page_articleFooter", resolvedLocales),
                        new Object[]{postedDate, art.getSectionid().getName()}) + commentCount;
                request.setAttribute("commentFormTitle", footer);
            }
            HtmlMeta.addNameTag(request, "description", art.getDescription());
            HtmlMeta.addNameTag(request, "author", art.getPostedname());
            HtmlMeta.addPropertyTag(request, "og:title", art.getArticletitle());
            HtmlMeta.addPropertyTag(request, "og:url", ArticleUrl.getUrl(request.getAttribute(SecurityRepo.BASE_URL).toString(), art, null));
            if (null != art.getImageurl()) {
                HtmlMeta.addPropertyTag(request, "og:image", art.getImageurl());
            }
            if (null != art.getDescription()) {
                HtmlMeta.addPropertyTag(request, "og:description", art.getDescription());
            }
            HtmlMeta.addPropertyTag(request, "og:site_name", beans.getImead().getLocal(ToiletServlet.SITE_TITLE, resolvedLocales));
            HtmlMeta.addPropertyTag(request, "og:type", "article");
            HtmlMeta.addPropertyTag(request, "og:article:published_time", DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(art.getPosted().toZonedDateTime()));
            HtmlMeta.addPropertyTag(request, "og:article:modified_time", DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(art.getModified().toZonedDateTime()));
            HtmlMeta.addPropertyTag(request, "og:article:author", art.getPostedname());
            HtmlMeta.addPropertyTag(request, "og:article:section", art.getSectionid().getName());
            HtmlMeta.addLink(request, "canonical", ArticleUrl.getUrl(request.getAttribute(SecurityRepo.BASE_URL).toString(), art, null));
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
            itemList.add(HtmlMeta.getLDBreadcrumb(beans.getImead().getLocal("page_title", resolvedLocales), 1, request.getAttribute(SecurityRepo.BASE_URL).toString()));
            itemList.add(HtmlMeta.getLDBreadcrumb(art.getSectionid().getName(), 2, Categorizer.getUrl(request.getAttribute(SecurityRepo.BASE_URL).toString(), art.getSectionid().getName(), null)));
            itemList.add(HtmlMeta.getLDBreadcrumb(art.getArticletitle(), 3, ArticleUrl.getUrl(beans.getImeadValue(SecurityRepo.BASE_URL), art, null)));
            JsonObjectBuilder breadcrumbs = Json.createObjectBuilder().add("@context", "https://schema.org").add("@type", "BreadcrumbList").add("itemListElement", itemList);
            HtmlMeta.addLDJSON(request, breadcrumbs.build().toString());
            request.getServletContext().getRequestDispatcher(ARTICLE_JSP).forward(request, response);
        }
    }

    public String getCommentFormUrl(Article art, Locale lang) {
        return "comments/" + art.getArticleid() + "?iframe";
    }

    /**
     *
     * @param arts Article repository to get articles from
     * @param art get articles similar to these
     * @return up to 6 similar articles, or null if something exploded
     */
    @SuppressWarnings("unchecked")
    public static Collection<Article> getArticleSuggestions(ArticleRepository arts, Article art) {
        try {
            Collection<Article> seeAlso = new LinkedHashSet<>(arts.search(null != art.getSuggestion() ? art.getSuggestion() : ArticleRepository.getArticleSuggestionTerm(art), 7));
            if (7 > seeAlso.size()) {
                seeAlso.addAll(arts.getBySection(art.getSectionid().getName(), 1, 7, null));
            }
            if (7 > seeAlso.size()) {
                seeAlso.addAll(arts.getBySection(null, 1, 7, null));
            }
            seeAlso.remove(art);
            List<Article> temp = Arrays.asList(Arrays.copyOf(seeAlso.toArray(Article[]::new), 6));
            seeAlso = new ArrayList(temp);
            seeAlso.removeAll(Collections.singleton(null));
            // sort articles without images last
            // show low-res images only
            for (Article a : temp) {
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
        }
        return new ArrayList<>();
    }
}
