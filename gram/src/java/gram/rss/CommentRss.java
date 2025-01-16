package gram.rss;

import gram.bean.GramLandlord;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.HttpHeaders;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import libWebsiteTools.imead.Local;
import libWebsiteTools.security.SecurityRepo;
import libWebsiteTools.security.HashUtil;
import libWebsiteTools.rss.RssChannel;
import libWebsiteTools.rss.RssItem;
import libWebsiteTools.rss.RssServlet;
import org.w3c.dom.Document;
import gram.bean.database.Article;
import gram.bean.database.Comment;
import gram.servlet.GramServlet;
import gram.tag.ArticleUrl;
import gram.tag.Categorizer;
import libWebsiteTools.rss.Feed;
import libWebsiteTools.rss.DynamicFeed;
import gram.bean.GramTenant;

public class CommentRss implements DynamicFeed {

    public static final String NAME = "Comments.rss";
    private static final String COMMENT_COUNT = "site_rss_commentCount";
    private static final Logger LOG = Logger.getLogger(CommentRss.class.getName());
    private static final Pattern NAME_PATTERN = Pattern.compile("Comments(.*?)\\.rss");

    public CommentRss() {
    }

    public Document createFeed(GramTenant ten, Object articleId) {
        RssChannel entries;
        if (null == articleId) {
            entries = createChannel(ten, ten.getComms().getAll(Integer.valueOf(ten.getImeadValue(COMMENT_COUNT))));
        } else {
            Article art = ten.getArts().get(Integer.parseInt(articleId.toString()));
            List<Comment> lComments = new ArrayList<>(art.getCommentCollection());
            Collections.reverse(lComments);
            entries = createChannel(ten, lComments);
            entries.setTitle(art.getArticletitle() + " - Comments");
            entries.setDescription("from " + ten.getImead().getValue(GramServlet.SITE_TITLE));
        }
        return Feed.refreshFeed(Arrays.asList(entries));
    }

    public RssChannel createChannel(GramTenant ten, Collection<Comment> lComments) {
        RssChannel entries = new RssChannel(ten.getImead().getValue(GramServlet.SITE_TITLE) + " - Comments", ten.getImeadValue(SecurityRepo.BASE_URL), ten.getImead().getValue(GramServlet.TAGLINE));
        entries.setWebMaster(ten.getImeadValue(Feed.MASTER));
        entries.setManagingEditor(entries.getWebMaster());
        entries.setLanguage(ten.getImeadValue(Feed.LANGUAGE));
        for (Comment c : lComments) {
            RssItem i = new RssItem(c.getPostedhtml());
            entries.addItem(i);
            i.addCategory(c.getArticleid().getSectionid().getName(), Categorizer.getUrl(ten.getImeadValue(SecurityRepo.BASE_URL), c.getArticleid().getSectionid().getName(), null));
            i.setLink(ArticleUrl.getUrl(ten.getImeadValue(SecurityRepo.BASE_URL), c.getArticleid(), "comments"));
            i.setGuid(c.getUuid().toString());
            i.setPubDate(c.getPosted());
            i.setTitle(c.getArticleid().getArticletitle());
            i.setAuthor(c.getPostedname());
            if (c.getArticleid().getComments()) {
                i.setComments(ArticleUrl.getUrl(ten.getImeadValue(SecurityRepo.BASE_URL), c.getArticleid(), "comments"));
            }
        }
        return entries;
    }

    @Override
    public String getName() {
        return NAME;
    }

    /**
     * will return Comments.rss and if on an article, Comments(articleID).rss
     *
     * @param req
     * @return
     */
    @Override
    public Map<String, String> getFeedURLs(HttpServletRequest req) {
        Article art = (Article) req.getAttribute(Article.class.getSimpleName());
        HashMap<String, String> output = new HashMap<>();
        output.put(getName(), "All Comments");
        if (null != art && null != art.getComments() && art.getComments()) {
            output.put("Comments" + art.getArticleid() + ".rss", art.getArticletitle() + " Comments");
        }
        return output;
    }

    /**
     *
     * @param name
     * @return if name matches Comments(.*?)//.rss
     */
    @Override
    public boolean willHandle(String name) {
        return NAME_PATTERN.matcher(name).matches();
    }

    @Override
    public Feed preAdd() {
        return this;
    }

    @Override
    public Feed doHead(HttpServletRequest req, HttpServletResponse res) {
        if (null == req.getAttribute(NAME)) {
            GramTenant ten = GramLandlord.getTenant(req);
            Object name = req.getAttribute(RssServlet.class.getSimpleName());
            Matcher regex = NAME_PATTERN.matcher(name.toString());
            String group = regex.find() ? regex.group(1) : null;
            if (null != group && group.isEmpty()) {
                group = null;
            }
            try {
                Document XML = createFeed(ten, group);
                DOMSource DOMsrc = new DOMSource(XML);
                StringWriter holder = new StringWriter(10000);
                StreamResult str = new StreamResult(holder);
                Transformer trans = TransformerFactory.newInstance().newTransformer();
                trans.transform(DOMsrc, str);
                String etag = "\"" + HashUtil.getSHA256Hash(holder.toString()) + "\"";
                res.setHeader(HttpHeaders.ETAG, etag);
                req.removeAttribute(Local.LOCALE_PARAM);
                req.setAttribute(Local.OVERRIDE_LOCALE_PARAM, Locale.forLanguageTag(ten.getImeadValue(Feed.LANGUAGE)));
                req.setAttribute(HttpHeaders.ETAG, etag);
                req.setAttribute(NAME, XML);
                if (etag.equals(req.getHeader(HttpHeaders.IF_NONE_MATCH))) {
                    res.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                }
            } catch (TransformerException ex) {
                LOG.log(Level.SEVERE, "Coment feed will not be available due to an XML transformation error.", ex);
                return null;
            }
        }
        return this;
    }

    @Override
    public Document preWrite(HttpServletRequest req, HttpServletResponse res) {
        doHead(req, res);
        return HttpServletResponse.SC_OK == res.getStatus() ? (Document) req.getAttribute(NAME) : null;
    }
}
