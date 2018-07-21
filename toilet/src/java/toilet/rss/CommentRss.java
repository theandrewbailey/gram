package toilet.rss;

import java.io.StringWriter;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import libOdyssey.bean.GuardHolder;
import libWebsiteTools.HashUtil;
import libWebsiteTools.imead.IMEADHolder;
import libWebsiteTools.rss.Feed;
import libWebsiteTools.rss.entity.AbstractRssFeed;
import libWebsiteTools.rss.entity.RssChannel;
import libWebsiteTools.rss.entity.RssItem;
import org.w3c.dom.Document;
import toilet.UtilStatic;
import toilet.bean.EntryRepo;
import toilet.bean.UtilBean;
import toilet.db.Comment;
import toilet.tag.ArticleUrl;

@WebListener("The RSS feed for comments, autoadded")
@Feed(CommentRss.NAME)
public class CommentRss extends AbstractRssFeed {

    public static final String NAME = "Comments.rss";
    private static final String COMMENT_COUNT = "rss_commentCount";
    @EJB
    private EntryRepo entry;
    @EJB
    private IMEADHolder imead;
    private Document XML;
    private Date lastUpdated = new Date(0);
    private String etag = "";

    public CommentRss() {
    }

    public Document generateFeed(Integer numEntries) {
        // if instantiated manually
        if (entry == null && imead == null) {
            entry = UtilStatic.getBean(EntryRepo.LOCAL_NAME, EntryRepo.class);
            imead = UtilStatic.getBean(UtilBean.IMEAD_LOCAL_NAME, IMEADHolder.class);
        }

        lastUpdated = new Date(0);
        RssChannel entries = new RssChannel(imead.getValue(UtilBean.SITE_TITLE) + " - Comments", imead.getValue(GuardHolder.CANONICAL_URL), imead.getValue(UtilBean.TAGLINE));
        entries.setWebMaster(imead.getValue(UtilBean.MASTER));
        entries.setManagingEditor(entries.getWebMaster());
        entries.setLanguage(imead.getValue(UtilBean.LANGUAGE));

        List<Comment> lComments = entry.getCommentArchive(numEntries);

        for (Comment c : lComments) {
            RssItem i = new RssItem(c.getPostedhtml());
            entries.addItem(i);
            i.addCategory(c.getArticleid().getSectionid().getName(), imead.getValue(GuardHolder.CANONICAL_URL) + "index/group=" + c.getArticleid().getSectionid().getName());
            i.setLink(ArticleUrl.getUrl(imead.getValue(GuardHolder.CANONICAL_URL), c.getArticleid()) + "#comments");
            i.setGuid(i.getLink());
            i.setPubDate(c.getPosted());
            i.setTitle("Re: " + c.getArticleid().getArticletitle());
            i.setAuthor(c.getPostedname());
            if (c.getArticleid().getComments()) {
                i.setComments(ArticleUrl.getUrl(imead.getValue(GuardHolder.CANONICAL_URL), c.getArticleid()) + "#comments");
            }
            if (i.getPubDate().after(lastUpdated)) {
                lastUpdated = i.getPubDate();
            }
        }
        return refreshFeed(entries);
    }

    @Override
    public void preAdd() {
        XML = generateFeed(Integer.valueOf(imead.getValue(COMMENT_COUNT)));
        try {
            DOMSource DOMsrc = new DOMSource(XML);
            StringWriter holder = new StringWriter(10000);
            StreamResult str = new StreamResult(holder);
            Transformer trans = TransformerFactory.newInstance().newTransformer();
            trans.transform(DOMsrc, str);
            etag = "\"" + HashUtil.getHash(holder.toString()) + "\"";
        } catch (TransformerException ex) {
            Logger.getLogger(CommentRss.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void doHead(HttpServletRequest req, HttpServletResponse res) {
        res.setHeader("Cache-Control", "public, max-age=" + 10000);
        res.setDateHeader("Last-Modified", lastUpdated.getTime());
        res.setDateHeader("Expires", new Date().getTime() + 10000000);
        res.setHeader("ETag", etag);
        if (etag.equals(req.getHeader("If-None-Match"))) {
            res.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
        }
    }

    @Override
    public long getLastModified() {
        return lastUpdated.getTime();
    }

    @Override
    public Document preWrite(HttpServletRequest req, HttpServletResponse res) {
        doHead(req, res);
        return HttpServletResponse.SC_OK == res.getStatus() ? XML : null;
    }
}
