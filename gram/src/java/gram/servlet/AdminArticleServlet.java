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
import libWebsiteTools.security.SecurityRepository;
import libWebsiteTools.tag.AbstractInput;
import gram.ArticleProcessor;
import gram.PictureTag;
import gram.bean.database.ArticleRepository;
import gram.bean.GramLandlord;
import gram.bean.database.Article;
import gram.bean.database.Section;
import gram.tag.ArticleUrl;
import gram.bean.GramTenant;
import java.io.UnsupportedEncodingException;
import java.text.MessageFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import libWebsiteTools.JVMNotSupportedError;
import libWebsiteTools.SearchableRepository;
import libWebsiteTools.file.Fileupload;
import libWebsiteTools.tag.FileSize;

/**
 *
 * @author alpha
 */
@WebServlet(name = "adminArticle", description = "Administer articles (and sometimes comments)", urlPatterns = {"/adminArticle", "/edit/*"})
public class AdminArticleServlet extends AdminServlet {

    public static final String ADMIN_ADD_ARTICLE = "/WEB-INF/admin/adminArticleAdd.jsp";
    public static final String ADMIN_ADD_ARTICLE_IFRAME = "/WEB-INF/admin/adminArticleAddIframe.jsp";
    public static final long HIGH_SIZE_LIMIT = 1440000L; // conventional 3.5" floppy maximum
    public static final long LOW_SIZE_LIMIT = 500000L;
    public static final long FIXED_SIZE_LIMIT = 200000L;
    public static final int RESOURCE_COUNT_LIMIT = 10;
    private static final String DEFAULT_NAME = "site_defaultName";

    @Override
    public AdminPermission[] getRequiredPermissions() {
        return new AdminPermission[]{AdminPermission.Password.EDIT_POSTS};
    }

    private static ArticleProcessor getProcessor(HttpServletRequest req, Article art) {
        GramTenant ten = GramLandlord.getTenant(req);
        if (art == null) {
            art = (Article) req.getSession().getAttribute(ten.getImeadValue(SecurityRepository.BASE_URL) + Article.class.getCanonicalName());
        }
        if (art == null) {
            art = ArticleServlet.getArticleFromURL(ten, req.getRequestURI());
        }
        if (art == null) {
            art = new Article(UUID.randomUUID());
        }
        req.getSession().setAttribute(ten.getImeadValue(SecurityRepository.BASE_URL) + Article.class.getCanonicalName(), art);
        ArticleProcessor artPro = new ArticleProcessor(ten, art);
        return artPro;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        GramTenant ten = GramLandlord.getTenant(request);
        request.getSession().removeAttribute(ten.getImeadValue(SecurityRepository.BASE_URL) + Article.class.getCanonicalName());
        if (null != request.getParameter("iframe") && "Preview".equals(request.getParameter("action"))) {
            ArticleProcessor artPro = getProcessor(request, null);
            Article art = artPro.getArt();
            if (null != art.getArticletitle() && null != art.getPostedhtml() && null != art.getPosted()) {
                request.setAttribute(Article.class.getSimpleName(), art);
                checkSize(ten, request, artPro);
                if (ten.getArts() instanceof SearchableRepository<Article> searchableArticles) {
                    Collection<Article> seeAlso = ArticleServlet.getArticleSuggestions(searchableArticles, art);
                    request.setAttribute("seeAlso", seeAlso);
                }
                request.getRequestDispatcher(ADMIN_ADD_ARTICLE_IFRAME).forward(request, response);
            }
        } else {
            request.getSession().removeAttribute(ten.getImeadValue(SecurityRepository.BASE_URL) + Article.class.getCanonicalName());
            Instant start = Instant.now();
            Article art = ArticleServlet.getArticleFromURL(ten, request.getRequestURI());
            if (null == art) {
                art = ArticleServlet.getArticleFromURL(ten, request.getHeader("Referer"));
            }
            if (null == art) {
                art = new Article(UUID.randomUUID());
            }
            ArticleProcessor artPro = getProcessor(request, art);
            RequestTimer.addTiming(request, "query", Duration.between(start, Instant.now()));
            AdminArticleServlet.displayArticleEdit(ten, request, response, artPro);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        GramTenant ten = GramLandlord.getTenant(request);
        Matcher validator = AbstractInput.DEFAULT_REGEXP.matcher("");
        ArticleProcessor artPro = updateArticleFromPage(request);
        Article art = artPro.getArt();
        if ("Preview".equals(request.getParameter("action"))) {
            AdminArticleServlet.displayArticleEdit(ten, request, response, artPro);
            return;
        } else if (!validator.reset(art.getArticletitle()).matches()
                || !validator.reset(art.getDescription()).matches()
                || !validator.reset(art.getPostedname()).matches()
                || !validator.reset(art.getPostedmarkdown()).matches()
                || (null != art.getSectionid() && !validator.reset(art.getSectionid().getName()).matches())) {
            request.setAttribute(GramServlet.ERROR_MESSAGE_PARAM, ten.getImead().getLocal("page_patternMismatch", Local.resolveLocales(ten.getImead(), request)));
            AdminArticleServlet.displayArticleEdit(ten, request, response, artPro);
            return;
        }
        Instant start = Instant.now();
        ten.getArts().upsert(Arrays.asList(art));
        if (null == art.getArticleid()) {
            art = ten.getArts().getAll(1).get(0);
        }
        RequestTimer.addTiming(request, "save", Duration.between(start, Instant.now()));
        response.setHeader(RequestTimer.SERVER_TIMING, RequestTimer.getTimingHeader(request, Boolean.FALSE));
        ten.getArts().evict().warmCache();
        ten.getGlobalCache().clear();
        request.getSession().removeAttribute(ten.getImeadValue(SecurityRepository.BASE_URL) + Article.class.getCanonicalName());
        request.getSession().invalidate();
        response.setHeader("Clear-Site-Data", "*");
        response.sendRedirect(ArticleUrl.getUrl(request.getAttribute(SecurityRepository.BASE_URL).toString(), art, null));
        if (ten.getArts() instanceof SearchableRepository<Article> searchableArticles) {
            ten.getExec().submit(searchableArticles::refreshSearch);
        }
    }

    private ArticleProcessor updateArticleFromPage(HttpServletRequest req) {
        Instant start = Instant.now();
        GramTenant ten = GramLandlord.getTenant(req);
        Article art = (Article) req.getSession().getAttribute(ten.getImeadValue(SecurityRepository.BASE_URL) + Article.class.getCanonicalName());
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
        List<Locale> resolvedLocales = Local.resolveLocales(ten.getImead(), req);
        String catName = AbstractInput.getParameter(req, "section").trim();
        art.setSectionid(new Section(catName.isEmpty() ? ten.getImead().getLocal(GramServlet.SITE_TITLE, resolvedLocales) : catName));
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
            art.setSummary(null);
            art.setPostedhtml(null);
            ArticleProcessor artPro = getProcessor(req, art);
            artPro.call();
            return artPro;
        } finally {
            if (isNewArticle) {
                art.setArticleid(null);
            }
            RequestTimer.addTiming(req, "parse", Duration.between(start, Instant.now()));
        }
    }

    public static void displayArticleEdit(GramTenant ten, HttpServletRequest request, HttpServletResponse response, ArticleProcessor artPro) throws ServletException, IOException {
        Instant start = Instant.now();
        Article art = artPro.getArt();
        if (ten.getArts() instanceof SearchableRepository<Article> searchableArticles) {
            Collection<Article> seeAlso = ArticleServlet.getArticleSuggestions(searchableArticles, art);
            request.setAttribute("seeAlso", seeAlso);
        }
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
        request.setAttribute("iframeSrc", null == art.getArticleid() ? "adminArticle?action=Preview&iframe" : "edit/" + art.getArticleid() + "?action=Preview&iframe");
        request.setAttribute("defaultSearchTerm", ArticleRepository.getArticleSuggestionTerm(art));
        request.setAttribute(Article.class.getSimpleName(), art);
        request.setAttribute("seeAlsoTerm", null != art.getSuggestion() ? art.getSuggestion() : ArticleRepository.getArticleSuggestionTerm(art));
        String formattedDate = null != art.getPosted() ? DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(art.getPosted()) : "";
        request.setAttribute("formattedDate", formattedDate);
        getRecentImages(ten, request);
        if (null != request.getParameter("iframe")) {
            checkSize(ten, request, artPro);
            request.getRequestDispatcher(ADMIN_ADD_ARTICLE_IFRAME).forward(request, response);
        } else {
            request.getRequestDispatcher(ADMIN_ADD_ARTICLE).forward(request, response);
        }
    }

    public static void checkSize(GramTenant ten, HttpServletRequest request, ArticleProcessor artPro) {
        Long fixed = artPro.getFixedSizeEstimate();
        Long low = fixed + artPro.getLowSizeEstimate();
        Long high = fixed + artPro.getHighSizeEstimate();
        int resources = artPro.getResourceCount();
        if (fixed > FIXED_SIZE_LIMIT) {
            String message = ten.getImead().getLocal("page_admin_fixed_size_error", Local.resolveLocales(ten.getImead(), request));
            request.setAttribute(ERROR_MESSAGE_PARAM, MessageFormat.format(message, FileSize.readableFileSize(fixed)));
        } else if (low > LOW_SIZE_LIMIT || high > HIGH_SIZE_LIMIT) {
            String message = ten.getImead().getLocal("page_admin_size_error", Local.resolveLocales(ten.getImead(), request));
            request.setAttribute(ERROR_MESSAGE_PARAM, MessageFormat.format(message, low.equals(high) ? "" : FileSize.readableFileSize(low), FileSize.readableFileSize(high)));
        } else if (resources > RESOURCE_COUNT_LIMIT) {
            String message = ten.getImead().getLocal("page_admin_resource_count_error", Local.resolveLocales(ten.getImead(), request));
            request.setAttribute(ERROR_MESSAGE_PARAM, MessageFormat.format(message, resources));
        }
    }

    public static void getRecentImages(GramTenant ten, HttpServletRequest request) {
        List<Fileupload> queryResult = ten.getFile().getAll(50);
        LinkedHashMap<String, PictureTag> picMap = new LinkedHashMap<>(20);
        List<String> picList = new ArrayList<>(picMap.size());
        for (Fileupload file : queryResult) {
            if (file.getMimetype().startsWith("image/")) {
                Matcher matcher = PictureTag.IMG_MULTIPLIER.matcher(file.getFilename());
                matcher.find();
                String stem = matcher.group(1);
                if (!picMap.containsKey(stem)) {
                    try {
                        Map<String, String> attribs = Map.of("src", file.getUrl(), "alt", stem);
                        PictureTag pic = new PictureTag(ten, attribs);
                        picMap.put(stem, pic);
                        List<Fileupload> uploads = pic.getFileUploads();
                        String newtag = new StringBuilder(500).append("<a href=\"file/").append(uploads.get(uploads.size() - 1).getFilename()).append("\">")
                                .append(pic.get()).append(PictureTag.createTag("img", attribs).append("/>")).append("</picture></a>").toString();
                        picList.add(newtag);
                    } catch (UnsupportedEncodingException ex) {
                        throw new JVMNotSupportedError(ex);
                    }
                }
            }
        }
        request.setAttribute("recentPictures", picList);
    }
}
