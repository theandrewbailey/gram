package gram.servlet;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.HttpHeaders;
import java.time.Duration;
import libWebsiteTools.security.HashUtil;
import libWebsiteTools.JVMNotSupportedError;
import libWebsiteTools.security.SecurityRepo;
import libWebsiteTools.turbo.PageCache;
import libWebsiteTools.imead.Local;
import libWebsiteTools.tag.AbstractInput;
import libWebsiteTools.tag.HtmlMeta;
import gram.IndexFetcher;
import gram.UtilStatic;
import gram.bean.GramLandlord;
import gram.bean.database.Article;
import gram.bean.database.Comment;
import gram.rss.CommentRss;
import libWebsiteTools.rss.DynamicFeed;
import libWebsiteTools.turbo.RequestTimer;
import gram.bean.GramTenant;
import java.util.UUID;

/**
 *
 * @author alpha
 */
@WebServlet(name = "CommentServlet", description = "Display article comments", urlPatterns = {"/comments/*"})
public class CommentServlet extends GramServlet {

    private static final String COMMENT_JSP = "/WEB-INF/comments.jsp";
    private static final String IFRAME_JSP = "/WEB-INF/commentsIframe.jsp";

    @Override
    protected long getLastModified(HttpServletRequest request) {
        GramTenant ten = GramLandlord.getTenant(request);
        boolean spamSuspected = (request.getSession(false) == null || request.getSession().isNew()) && request.getParameter("referer") == null;
        try {
            Instant start = Instant.now();
            Article art = IndexFetcher.getArticleFromURI(ten, request.getRequestURI());
            RequestTimer.addTiming(request, "query", Duration.between(start, Instant.now()));
            request.setAttribute(Article.class.getCanonicalName(), art);
            return spamSuspected ? art.getModified().toInstant().toEpochMilli() - 10000 : art.getModified().toInstant().toEpochMilli();
        } catch (RuntimeException ex) {
        }
        return 0L;
    }

    @Override
    protected void doHead(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        Article art = (Article) request.getAttribute(Article.class.getCanonicalName());
        GramTenant ten = GramLandlord.getTenant(request);
        if (null == art) {
            try {
                Instant start = Instant.now();
                art = IndexFetcher.getArticleFromURI(ten, request.getRequestURI());
                RequestTimer.addTiming(request, "query", Duration.between(start, Instant.now()));
                request.setAttribute(Article.class.getCanonicalName(), art);
            } catch (RuntimeException ex) {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return;
            }
        }
        if (null == art) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        boolean spamSuspected = (request.getSession(false) == null || request.getSession().isNew()) && request.getParameter("referer") == null;
        response.setDateHeader(HttpHeaders.DATE, spamSuspected ? art.getModified().toInstant().toEpochMilli() - 10000 : art.getModified().toInstant().toEpochMilli());
        request.setAttribute("spamSuspected", spamSuspected);
        response.setHeader(HttpHeaders.CACHE_CONTROL, "private, no-store");
        String ifNoneMatch = request.getHeader(HttpHeaders.IF_NONE_MATCH);
        try {
            MessageDigest md = HashUtil.getSHA256();
            md.update(request.getRequestURI().getBytes("UTF-8"));
            md.update(Local.getLocaleString(ten.getImead(), request).getBytes("UTF-8"));
            md.update(ten.getImead().getLocalizedHash().getBytes("UTF-8"));
            md.update(request.getSession().getId().getBytes("UTF-8"));
            md.update(art.getEtag().getBytes("UTF-8"));
            md.update(spamSuspected ? "s".getBytes("UTF-8") : "h".getBytes("UTF-8"));
            String etag = "W/\"" + Base64.getEncoder().encodeToString(md.digest()) + "\"";
            response.setHeader(HttpHeaders.ETAG, etag);
            request.setAttribute(HttpHeaders.ETAG, etag);
            if (etag.equals(ifNoneMatch)) {
                request.setAttribute(Article.class.getCanonicalName(), null);
                response.sendError(HttpServletResponse.SC_NOT_MODIFIED);
            }
        } catch (UnsupportedEncodingException enc) {
            throw new JVMNotSupportedError(enc);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doHead(request, response);
        HtmlMeta.addNameTag(request, "robots", "noindex");
        Article art = (Article) request.getAttribute(Article.class.getCanonicalName());
        if (null != art) {
            request.setAttribute(Article.class.getSimpleName(), art);
            request.setAttribute("title", art.getArticletitle());
            request.setAttribute("articleCategory", art.getSectionid());
            request.setAttribute("commentForm", request.getAttribute(SecurityRepo.BASE_URL).toString() + "comments/" + art.getArticleid() + (null == request.getParameter("iframe") ? "" : "?iframe"));
            request.getServletContext().getRequestDispatcher(null == request.getParameter("iframe") ? COMMENT_JSP : IFRAME_JSP).forward(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Matcher validator = AbstractInput.DEFAULT_REGEXP.matcher("");
        GramTenant ten = GramLandlord.getTenant(request);
        Instant start = Instant.now();
        String postName = AbstractInput.getParameter(request, "name");
        String postText = AbstractInput.getParameter(request, "text");
        if (null == postText || postText.isEmpty()
                || null == postName || postName.isEmpty()) {
            response.sendError(422, "Unprocessable Entity");
            return;
        }
        postName = postName.trim();
        postText = UtilStatic.removeSpaces(postText);
        String referred = request.getHeader("Referer");
        if (request.getSession().isNew() || referred == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        String[] spamwords = ten.getImeadValue(ArticleServlet.SPAM_WORDS).split("\n");
        for (String ua : spamwords) {
            if (Pattern.matches(ua, postText.toLowerCase())) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
        }
        Comment newComment = new Comment();
        newComment.setUuid(UUID.randomUUID());
        newComment.setPostedhtml(UtilStatic.htmlFormat(postText, false, true, true));
        newComment.setPostedname(UtilStatic.htmlFormat(postName, false, false, true));
        if (!validator.reset(postName).matches()
                || !validator.reset(postText).matches()
                || newComment.getPostedname().length() > 250 || newComment.getPostedhtml().length() > 64000) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        Article art = IndexFetcher.getArticleFromURI(ten, request.getRequestURI());
        // prevent posting from old page
        String postRequest = AbstractInput.getParameter(request, "original-request-time");
        if (null != postRequest) {
            OffsetDateTime postRequestDate = Instant.ofEpochMilli(Long.valueOf(postRequest) + 100).atZone(ZoneId.systemDefault()).toOffsetDateTime();
            for (Comment existingComment : art.getCommentCollection()) {
                if ((existingComment.getPostedname().equals(newComment.getPostedname())) && existingComment.getPosted().isAfter(postRequestDate)
                        || existingComment.getPostedhtml().equals(newComment.getPostedhtml())) {
                    response.setStatus(HttpServletResponse.SC_CONFLICT);
                    request.getSession().setAttribute("LastPostedName", postName);
                    request.setAttribute("commentText", postText);
                    doGet(request, response);
                    return;
                }
            }
        }
        newComment.setArticleid(art);
        ten.getComms().upsert(Arrays.asList(newComment));
        request.getSession().setAttribute("LastPostedName", postName);
        RequestTimer.addTiming(request, "save", Duration.between(start, Instant.now()));
        Article refreshedArt = IndexFetcher.getArticleFromURI(ten, request.getRequestURI());
        request.setAttribute(Article.class.getCanonicalName(), refreshedArt);
        ten.getExec().submit(() -> {
            PageCache global = ten.getGlobalCache();
            global.removeAll(global.searchLookups("/" + refreshedArt.getArticleid().toString() + "/"));
            for (String url : ((DynamicFeed) ten.getFeeds().get(CommentRss.NAME)).getFeedURLs(request).keySet()) {
                global.removeAll(global.searchLookups("rss/" + url));
            }
        });
        doGet(request, response);
    }
}
