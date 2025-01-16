package libWebsiteTools.rss;

import org.w3c.dom.Element;

/**
 *
 * @author alpha
 */
public class AtomGenerator extends AtomCommonAttribs {

    private String generator = "libRssServlet";
    private String version = "2.0";
    private String uri;

    public AtomGenerator() { }

    @Override
    public Element publish(Element xml) {
        Element item = super.publish(xml, "generator");
        item.setAttribute("uri", getUri());
        item.setAttribute("version", getVersion());
        item.setTextContent(getGenerator());
        return item;
    }

    /**
     * @return the generator
     */
    public String getGenerator() {
        return generator;
    }

    /**
     * @param generator the generator to set
     */
    public void setGenerator(String generator) {
        this.generator = generator;
    }

    /**
     * @return the version
     */
    public String getVersion() {
        return version;
    }

    /**
     * @param version the version to set
     */
    public void setVersion(String version) {
        this.version = version;
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
