package libWebsiteTools.rss;

import java.io.Serializable;
import java.util.Collection;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * interface for a RSS Feed.
 *
 * to put feed into service, add to the FeedBucket EJB. feeds are guaranteed to
 * added and destroyed in the order declared.
 *
 * AbstractRssFeed is a mostly complete implementation (includes DOM), and
 * recommend usage of @Feed for your implementations.
 *
 * @author alpha
 * @see libWebsiteTools.rss.RssServlet
 */
public interface Feed extends Serializable {

    public static final String COPYRIGHT = "site_rss_copyright";
    public static final String MASTER = "site_rss_master";

    public enum MimeType {
        RSS("application/rss+xml"), ATOM("application/atom+xml");
        public final String type;

        MimeType(String type) {
            this.type = type;
        }

        @Override
        public String toString() {
            return type;
        }
    }

    /**
     *
     * @return what this feed should be called, for purposes of URLs
     */
    public default String getName() {
        return this.getClass().getSimpleName() + ".rss";
    }

    /**
     * RSS types are the only ones implemented at this time.
     *
     * @return MIME type of this feed
     */
    public default MimeType getMimeType() {
        return MimeType.RSS;
    }

    /**
     * preAdd will be called before this feed is made available.
     *
     * @return this
     */
    public default Feed preAdd() {
        return this;
    }

    /**
     * postAdd will be called after this feed is made available.
     *
     * @return this
     */
    public default Feed postAdd() {
        return this;
    }

    /**
     * lastModified will return when the feed last changed.
     *
     * @param req
     * @return milliseconds since epoch
     * @see HttpServlet
     */
    public default long getLastModified(HttpServletRequest req) {
        return -1;
    }

    /**
     * HttpServlet.doHead(HttpServletRequest, HttpServletResponse) calls this.
     * Setting status to something other than 200 is respected.
     *
     * @param req
     * @param res
     * @return this
     */
    public default Feed doHead(HttpServletRequest req, HttpServletResponse res) {
        return this;
    }

    /**
     * preWrite will be called on every request for the feed. it must return the
     * XML to send back to user, or set the HTTP response status to something
     * other than 200.
     *
     * there is no external synchronized block around this call and postWrite.
     * if thread-safety is needed, you must provide it on your own.
     *
     * @param req useful for getting the session object
     * @param res useful for setting headers
     * @return XML document to write to output stream
     */
    public default Document preWrite(HttpServletRequest req, HttpServletResponse res) {
        return null;
    }

    /**
     * postWrite is called after preWrite.
     *
     * there is no external synchronized block around this call and postWrite.
     * if thread-safety is needed, you must provide it on your own.
     *
     * @param req useful for getting the session object
     * @return this
     */
    public default Feed postWrite(HttpServletRequest req) {
        return this;
    }

    /**
     * preRemove will be called before the feed is removed from service.
     *
     * @return this
     */
    public default Feed preRemove() {
        return this;
    }

    /**
     * postRemove will be called after the feed is removed from service.
     *
     * @return this
     */
    public default Feed postRemove() {
        return this;
    }

    /**
     * rebuild the DOM behind this RSS feed, with the given channels
     *
     * @param channels
     * @return RSS XML output
     */
    public static Document refreshFeed(Collection<RssChannel> channels) {
        Document XML = null;
        try {
            XML = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            Element root = XML.createElement("rss");
            XML.appendChild(root);
            root.setAttribute("version", "2.0");
            for (RssChannel chan : channels) {
                chan.publish(root);
            }
        } catch (ParserConfigurationException | DOMException x) {
            throw new RuntimeException(x.getMessage(), x);
        }
        return XML;
    }
}
