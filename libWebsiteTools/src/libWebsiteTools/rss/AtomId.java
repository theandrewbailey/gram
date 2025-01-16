package libWebsiteTools.rss;

import org.w3c.dom.Element;

/**
 *
 * @author alpha
 */
public class AtomId extends AtomCommonAttribs {

    private String uri = "http://you.should.have.used.the.other.constructor";

    public AtomId() {
    }

    public AtomId(String u) {
        uri = u;
    }

    @Override
    public Element publish(Element xml, String name) {
        Element item = super.publish(xml, name);
        item.setTextContent(getUri());
        return item;
    }

    /**
     * @return the uri
     */
    public String getUri() {
        return uri;
    }

    /**
     * @param uri the uri to set
     */
    public void setUri(String uri) {
        this.uri = uri;
    }
}
