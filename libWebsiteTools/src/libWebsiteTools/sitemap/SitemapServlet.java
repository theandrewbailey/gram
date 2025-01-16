package libWebsiteTools.sitemap;

import java.io.IOException;
import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import libWebsiteTools.BaseServlet;
import org.w3c.dom.Document;
import libWebsiteTools.Landlord;
import libWebsiteTools.Tenant;

@WebServlet(name = "Sitemap", description = "Provides a sitemap xml", urlPatterns = {"/sitemap.xml"})
public class SitemapServlet extends BaseServlet {

    public static final String XML_JSP = "/XMLOut.jsp";
    private final TransformerFactory xFormFact = TransformerFactory.newInstance();
    private static final Logger LOG = Logger.getLogger(SitemapServlet.class.getName());

    @Override
    public void init() throws ServletException {
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Tenant ten = Landlord.getTenant(request);
        SiteMapper mapper = ten.getMapper();
        Document siteMap = mapper.getSiteMap();
        DOMSource DOMsrc = new DOMSource(siteMap);
        StringWriter write = new StringWriter(1000000);
        StreamResult str = new StreamResult(write);
        try {
            Transformer trans;
            synchronized (xFormFact) {
                trans = xFormFact.newTransformer();
            }
            trans.transform(DOMsrc, str);
            request.setAttribute("XMLOut", write.toString());
            // forward to JSP so feed may be cached
            request.getServletContext().getRequestDispatcher(XML_JSP).forward(request, response);
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Sitemap encountered error during request", ex);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
