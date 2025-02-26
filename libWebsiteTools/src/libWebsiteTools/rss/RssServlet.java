package libWebsiteTools.rss;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import libWebsiteTools.BaseServlet;
import org.w3c.dom.Document;
import libWebsiteTools.Landlord;
import libWebsiteTools.Tenant;

/**
 * Servlet for serving RSS/Atom feeds.
 *
 * @author alpha
 */
@WebServlet(name = "RssServlet", description = "RSS host servlet, writes XML DOM for RSS feeds", urlPatterns = {"/rss/*"}, loadOnStartup = 1)
public class RssServlet extends BaseServlet {

    public static final String INDENT = "RSS.indent";
    public static final Pattern RSS_NAME_REGEX = Pattern.compile("^.+?/rss/([^\\?]+?)(?:\\?.*)?$");
    private static final Map<Feed.MimeType, String> MIME_TYPE_JSP = Collections.unmodifiableMap(new HashMap<Feed.MimeType, String>() {
        {
            put(Feed.MimeType.RSS, "/RssOut.jsp");
            put(Feed.MimeType.ATOM, "/AtomOut.jsp");
        }
    });
    private static final Logger LOG = Logger.getLogger(RssServlet.class.getName());
    private final TransformerFactory xFormFact = TransformerFactory.newInstance();

    public static String getRssName(String URL) {
        Matcher matcher = RSS_NAME_REGEX.matcher(URL);
        return matcher.find() ? matcher.group(1) : null;
    }

    public Feed getFeed(HttpServletRequest req) {
        Feed feed = (Feed) req.getAttribute(Feed.class.getCanonicalName());
        if (null == feed) {
            String name = getRssName(req.getRequestURL().toString());
            req.setAttribute(RssServlet.class.getSimpleName(), name);
            Tenant ten = Landlord.getTenant(req);
            feed = ten.getFeeds().get(name);
            if (feed == null) {
                LOG.log(Level.FINE, "RSS feed {0} not found", name);
                return null;
            }
            req.setAttribute(Feed.class.getCanonicalName(), feed);
        }
        return feed;
    }

    @Override
    protected long getLastModified(HttpServletRequest request) {
        Feed feed = getFeed(request);
        if (feed == null) {
            LOG.log(Level.FINE, "RSS feed {0} not found", getRssName(request.getRequestURI()));
            return -1;
        }
        return feed.getLastModified(request);
    }

    @Override
    protected void doHead(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Feed feed = getFeed(request);
        if (feed == null) {
            LOG.log(Level.FINE, "RSS feed {0} not found", getRssName(request.getRequestURI()));
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        feed.doHead(request, response);
        if (HttpServletResponse.SC_OK != response.getStatus()) {
            response.sendError(response.getStatus());
        }
    }

    /**
     * outputs an RSS XML on the request, the URL determines which feed gets
     * written out /rss/peanutButter writes feed peanutButter to response
     *
     * @param request
     * @param response
     * @throws jakarta.servlet.ServletException
     * @throws java.io.IOException
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            Feed feed = getFeed(request);
            if (null == feed) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            LOG.log(Level.FINEST, "RSS feed {0} requested, servicing", getRssName(request.getRequestURI()));
            Document XML = feed.preWrite(request, response);
            if (HttpServletResponse.SC_OK != response.getStatus()) {
                response.sendError(response.getStatus());
                return;
            }
            DOMSource DOMsrc = new DOMSource(XML);
            StringWriter write = new StringWriter(1000000);
            StreamResult str = new StreamResult(write);
            Transformer trans;
            synchronized (xFormFact) {
                trans = xFormFact.newTransformer();
            }
            trans.transform(DOMsrc, str);
            request.setAttribute("RSSOut", write.toString());
            // forward to JSP so feed may be cached
            request.getServletContext().getRequestDispatcher(MIME_TYPE_JSP.get(feed.getMimeType())).forward(request, response);
            feed.postWrite(request);
        } catch (IOException | IllegalArgumentException | TransformerException ex) {
            LOG.log(Level.SEVERE, "Error occured while retrieving RSS feed " + getRssName(request.getRequestURI()), ex);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
