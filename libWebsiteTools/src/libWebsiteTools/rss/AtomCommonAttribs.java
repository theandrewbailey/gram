package libWebsiteTools.rss;

import java.io.Serializable;
import org.w3c.dom.Element;

/**
 *
 * @author alpha
 */
public class AtomCommonAttribs implements Serializable, Publishable {

    public static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    private String xmlBase;
    private String xmlLang;

    /**
     * @param e parent xml node of this object
     * @param name of what the node is supposed to be
     * @return this object's node (empty)
     */
    public static Element create(Element e, String name) {
        return e.getOwnerDocument().createElement(name);
    }

    /**
     * @param e parent xml node of this object
     * @param name of what the node is supposed to be
     * @return this object's node with base elements
     */
    public Element publish(Element e, String name) {
        return publish(create(e, name));
    }

    /**
     * @param e this object's xml node
     * @return e this object's xml node
     */
    @Override
    public Element publish(Element e) {
        if (xmlBase != null) {
            e.setAttribute("xml:base", xmlBase);
        }
        if (xmlLang != null) {
            e.setAttribute("xml:lang", xmlLang);
        }
        return e;
    }

    public AtomCommonAttribs() {
        this("Not valid", "en-US");
    }

    public AtomCommonAttribs(String base, String lang) {
        xmlBase = base;
        xmlLang = lang;
    }

    /**
     * @return the xmlBase
     */
    public String getXmlBase() {
        return xmlBase;
    }

    /**
     * @param xmlBase the xmlBase to set
     */
    public void setXmlBase(String xmlBase) {
        this.xmlBase = xmlBase;
    }

    /**
     * @return the xmlLang
     */
    public String getXmlLang() {
        return xmlLang;
    }

    /**
     * @param xmlLang the xmlLang to set
     */
    public void setXmlLang(String xmlLang) {
        this.xmlLang = xmlLang;
    }

}
