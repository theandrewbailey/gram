package libWebsiteTools.rss;

import java.io.Serializable;
import org.w3c.dom.Element;

/**
 * a simple category in an RSS feed shouldn't need to worry about this much
 *
 * @author alpha
 */
public class RssCategory implements Serializable, Publishable {

    private String name = "Replace this name";
    private String domain;

    /**
     * default constructor please do not use
     */
    public RssCategory() {
    }

    /**
     * please use this
     *
     * @param iName name of the category (required)
     */
    public RssCategory(String iName) {
        name = iName;
    }

    /**
     * ... or this
     *
     * @param iName name of the category (required)
     * @param iDomain the http address to a list of similar entries
     */
    public RssCategory(String iName, String iDomain) {
        name = iName;
        domain = iDomain;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the domain
     */
    public String getDomain() {
        return domain;
    }

    /**
     * @param domain the domain to set
     */
    public void setDomain(String domain) {
        this.domain = domain;
    }

    @Override
    public Element publish(Element e) {
        Element n = RssChannel.cdataTextNode(e, "category", getName());
        if (getDomain() != null) {
            n.setAttribute("domain", getDomain());
        }
        return n;
    }
}
