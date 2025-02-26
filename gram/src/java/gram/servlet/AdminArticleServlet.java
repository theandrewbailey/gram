package gram.servlet;

import gram.AdminPermission;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.regex.Matcher;
import libWebsiteTools.imead.Local;
import libWebsiteTools.rss.FeedBucket;
import libWebsiteTools.turbo.RequestTimer;
import libWebsiteTools.security.SecurityRepo;
import libWebsiteTools.tag.AbstractInput;
import gram.ArticleProcessor;
import gram.CategoryFetcher;
import gram.bean.database.ArticleRepository;
import gram.bean.GramLandlord;
import gram.bean.database.Article;
import gram.bean.database.Section;
import gram.tag.ArticleUrl;
import gram.bean.GramTenant;
import java.util.UUID;

/**
 *
 * @author alpha
 */
@WebServlet(name = "adminArticle", description = "Administer articles (and sometimes comments)", urlPatterns = {"/adminArticle", "/edit/*"})
public class AdminArticleServlet extends AdminServlet {

    public static final String ADMIN_ADD_ARTICLE = "/WEB-INF/admin/adminArticleAdd.jsp";
    public static final String ADMIN_ADD_ARTICLE_IFRAME = "/WEB-INF/admin/adminArticleAddIframe.jsp";
    private static final String DEFAULT_NAME = "site_defaultName";

    @Override
    public AdminPermission[] getRequiredPermissions() {
        return new AdminPermission[]{AdminPermission.Password.EDIT_POSTS};
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        GramTenant ten = GramLandlord.getTenant(request);
        Instant start = Instant.now();
        Article art = ArticleServlet.getArticleFromURL(ten, request.getRequestURI());
        if (null == art) {
            art = ArticleServlet.getArticleFromURL(ten, request.getHeader("Referer"));
        }
//        if (null == art) {
//            art = (Article) request.getSession().getAttribute(ten.getImeadValue(SecurityRepo.BASE_URL) + Article.class.getCanonicalName());
//        }
        if (null == art) {
            art = new Article(UUID.randomUUID());
        }
        RequestTimer.addTiming(request, "query", Duration.between(start, Instant.now()));
        AdminArticleServlet.displayArticleEdit(ten, request, response, art);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Matcher validator = AbstractInput.DEFAULT_REGEXP.matcher("");
        GramTenant ten = GramLandlord.getTenant(request);
        Article art = updateArticleFromPage(request);
        if ("Preview".equals(request.getParameter("action"))) {
            AdminArticleServlet.displayArticleEdit(ten, request, response, art);
            return;
        } else if (!validator.reset(art.getArticletitle()).matches()
                || !validator.reset(art.getDescription()).matches()
                || !validator.reset(art.getPostedname()).matches()
                || !validator.reset(art.getPostedmarkdown()).matches()
                || (null != art.getSectionid() && !validator.reset(art.getSectionid().getName()).matches())) {
            request.setAttribute(CoronerServlet.ERROR_MESSAGE_PARAM, ten.getImead().getLocal("page_patternMismatch", Local.resolveLocales(ten.getImead(), request)));
            AdminArticleServlet.displayArticleEdit(ten, request, response, art);
            return;
        }
        Instant start = Instant.now();
        art = ten.getArts().upsert(Arrays.asList(art)).iterator().next();
        RequestTimer.addTiming(request, "save", Duration.between(start, Instant.now()));
        response.setHeader(RequestTimer.SERVER_TIMING, RequestTimer.getTimingHeader(request, Boolean.FALSE));
        ten.getArts().evict();
        ten.getGlobalCache().clear();
        request.getSession().invalidate();
        response.setHeader("Clear-Site-Data", "*");
        response.sendRedirect(ArticleUrl.getUrl(request.getAttribute(SecurityRepo.BASE_URL).toString(), art, null));
        ten.getExec().submit(() -> {
            ten.getArts().refreshSearch();
        });
    }

    private Article updateArticleFromPage(HttpServletRequest req) {
        Instant start = Instant.now();
        GramTenant ten = GramLandlord.getTenant(req);
        Article art = (Article) req.getSession().getAttribute(ten.getImeadValue(SecurityRepo.BASE_URL) + Article.class.getCanonicalName());
        boolean isNewArticle = null == art.getArticleid();
        if (isNewArticle) {
            int nextID = ten.getArts().count(null).intValue();
            art.setArticleid(++nextID);
            req.setAttribute("isNewArticle", true);
        } else {
            req.setAttribute("isNewArticle", false);
        }
        art.setArticletitle(AbstractInput.getParameter(req, "articletitle").trim());
        art.setDescription(AbstractInput.getParameter(req, "description").trim());
        String catName = AbstractInput.getParameter(req, "section").trim();
        art.setSectionid(catName.isEmpty() ? null : new Section(catName));
        art.setPostedname(AbstractInput.getParameter(req, "postedname") == null || AbstractInput.getParameter(req, "postedname").isEmpty()
                ? ten.getImeadValue(DEFAULT_NAME)
                : AbstractInput.getParameter(req, "postedname"));
        String date = AbstractInput.getParameter(req, "posted").trim();
        if (date != null) {
            try {
                art.setPosted(FeedBucket.parseTimeFormat(DateTimeFormatter.ISO_OFFSET_DATE_TIME, date));
            } catch (DateTimeException p) {
                art.setPosted(OffsetDateTime.now());
            }
        }
        art.setComments(AbstractInput.getParameter(req, "comments") != null);
        art.setPostedmarkdown(AbstractInput.getParameter(req, "postedmarkdown").trim());
        String suggestion = AbstractInput.getParameter(req, "suggestion");
        if (null != suggestion && suggestion.length() > 0) {
            art.setSuggestion(suggestion.trim());
        } else {
            art.setSuggestion(null);
        }
        try {
            art.setImageurl(null);
            return new ArticleProcessor(ten, ArticleProcessor.convert(art)).call();
        } finally {
            if (isNewArticle) {
                art.setArticleid(null);
            }
            RequestTimer.addTiming(req, "parse", Duration.between(start, Instant.now()));
        }
    }

    public static void displayArticleEdit(GramTenant ten, HttpServletRequest request, HttpServletResponse response, Article art) throws ServletException, IOException {
        Instant start = Instant.now();
        Collection<Article> seeAlso = ArticleServlet.getArticleSuggestions(ten.getArts(), art);
        request.setAttribute("seeAlso", seeAlso);
        RequestTimer.addTiming(request, "seeAlsoQuery", Duration.between(start, Instant.now()));
        if (art.getCommentCollection() == null) {
            art.setCommentCollection(new ArrayList<>());
        }
        LinkedHashSet<String> groups = new LinkedHashSet<>();
        for (Section category : ten.getCategories().getAll(null)) {
            groups.add(category.getName());
        }
        request.setAttribute("groups", groups);
        if (null != art.getSectionid() && !groups.add(art.getSectionid().getName())) {
            groups.add(art.getSectionid().getName());
        }
        request.setAttribute(Article.class.getSimpleName(), art);
        request.setAttribute("iframeSrc", null == art.getArticleid() ? "" : "src=\"edit/" + art.getArticleid() + "?action=Preview\"");
        request.setAttribute("defaultSearchTerm", ArticleRepository.getArticleSuggestionTerm(art));
        request.getSession().setAttribute(ten.getImeadValue(SecurityRepo.BASE_URL) + Article.class.getCanonicalName(), art);
        request.setAttribute("seeAlsoTerm", null != art.getSuggestion() ? art.getSuggestion() : ArticleRepository.getArticleSuggestionTerm(art));
        String formattedDate = null != art.getPosted() ? DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(art.getPosted()) : "";
        request.setAttribute("formattedDate", formattedDate);
        if ("Preview".equals(request.getParameter("action"))) {
            request.getRequestDispatcher(ADMIN_ADD_ARTICLE_IFRAME).forward(request, response);
        } else {
            request.getRequestDispatcher(ADMIN_ADD_ARTICLE).forward(request, response);
        }
    }
}
