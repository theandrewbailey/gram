package libWebsiteTools.sitemap;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 *
 * @author alpha
 */
public class SiteMapper {

    private final Set<Iterable<UrlMap>> sources = Collections.synchronizedSet(new HashSet<>());
    private static final Logger LOG = Logger.getLogger(SiteMapper.class.getName());

    public void addSource(Iterable<UrlMap> src) {
        sources.add(src);
    }

    public Document getSiteMap() {
        Document xml = null;
        LOG.log(Level.FINEST, "Sitemap requested");
        int renderLevel = 0;
        int urlcount = 0;
        int size = 71;
        try {
            ArrayList<UrlMap> urls = new ArrayList<>(100);
            for (Iterable<UrlMap> page : new ArrayList<>(sources)) {
                for (UrlMap u : page) {
                    urls.add(u);
                }
            }
            xml = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            Element root = xml.createElement("urlset");
            root.setAttribute("xmlns", "http://www.sitemaps.org/schemas/sitemap/0.9");
            root.setAttributeNS("http://www.w3.org/2001/XMLSchema-instance", "xsi:schemaLocation", "http://www.sitemaps.org/schemas/sitemap/0.9 http://www.sitemaps.org/schemas/sitemap/0.9/sitemap.xsd");
            xml.appendChild(root);

            for (UrlMap u : urls) {
                Element newUrl = xml.createElement("url");
                Integer newSize = write(u, newUrl, renderLevel);
                if (newSize != null && size + newSize > 10480000) {
                    ++renderLevel;
                    return xml;
                } else if (newSize != null) {
                    xml.getDocumentElement().appendChild(newUrl);
                    size += newSize;
                    if (++urlcount == 50000) {
                        ++renderLevel;
                        return xml;
                    }
                }
            }
        } catch (ParserConfigurationException ex) {
            LOG.log(Level.SEVERE, "Sitemap XML parser configured incorrectly", ex);
        }
        return xml;
    }

    private Integer write(UrlMap url, Element urlnode, int renderLevel) {
        int count = 22 + url.getLocation().length();
        Document xml = urlnode.getOwnerDocument();
        Element n;

        n = xml.createElement("loc");
        n.setTextContent(url.getLocation());
        urlnode.appendChild(n);

        if (url.getLastmod() != null) {
            String lastmod = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(url.getLastmod());
            count += 19 + lastmod.length();
            n = xml.createElement("lastmod");
            n.setTextContent(lastmod);
            urlnode.appendChild(n);
        }
        if (url.getChangefreq() != null) {
            count += 25 + url.getChangefreq().toString().length();
            n = xml.createElement("changefreq");
            n.setTextContent(url.getChangefreq().toString());
            urlnode.appendChild(n);
        }
        if (url.getPriority() != null) {
            if (Float.valueOf(url.getPriority()) < renderLevel / 100f) {
                return null;
            }
            count += 21 + url.getPriority().length();
            n = xml.createElement("priority");
            n.setTextContent(url.getPriority());
            urlnode.appendChild(n);
            return count;
        } else if (renderLevel != 0) {
            return count;
        } else {
            return null;
        }
    }
}
