package libWebsiteTools.rss;

import org.w3c.dom.Element;

/**
 *
 * @author alpha
 */
public class AtomLink extends AtomCommonAttribs {

    public static enum RelType {
        alternate,
        related,
        self,
        enclosure,
        via;
    }
    private String href = "http://you.should.have.used.the.other.constructor";
    private RelType rel = RelType.self;
    private String type = "advice";
    private String hreflang = "en-US";
    private String title = "advice";
    private Integer length = 0;

    /**
     * 
     * @param u href (the url)
     * @param r relationship to parent
     * @param typ type (freeform)
     * @param lang language of destination
     * @param ttl title
     * @param len
     */
    public AtomLink(String u, RelType r, String typ, String lang, String ttl, Integer len) {
        href = u;
        rel = r;
        type = typ;
        hreflang = lang;
        title = ttl;
        length = len;
    }

    public AtomLink() {
    }

    @Override
    public Element publish(Element xml) {
        super.publish(xml);
        xml.setAttribute("href", getHref());
        if (getRel() != null) {
            switch (getRel()) {
                case alternate:
                    xml.setAttribute("rel", "alternate");
                    break;
                case related:
                    xml.setAttribute("rel", "related");
                    break;
                case self:
                    xml.setAttribute("rel", "self");
                    break;
                case enclosure:
                    xml.setAttribute("rel", "enclosure");
                    break;
                case via:
                    xml.setAttribute("rel", "via");
                    break;
            }
        }
        if (getType() != null) {
            xml.setAttribute("type", getType());
        }
        if (getHreflang() != null) {
            xml.setAttribute("hreflang", getHreflang());
        }
        if (getTitle() != null) {
            xml.setAttribute("title", getTitle());
        }
        if (getLength() != null) {
            xml.setAttribute("length", getLength().toString());
        }
        return xml;
    }

    /**
     * @return the href
     */
    public String getHref() {
        return href;
    }

    /**
     * @param href the href to set
     */
    public void setHref(String href) {
        this.href = href;
    }

    /**
     * @return the rel
     */
    public RelType getRel() {
        return rel;
    }

    /**
     * @param rel the rel to set
     */
    public void setRel(RelType rel) {
        this.rel = rel;
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * @return the hreflang
     */
    public String getHreflang() {
        return hreflang;
    }

    /**
     * @param hreflang the hreflang to set
     */
    public void setHreflang(String hreflang) {
        this.hreflang = hreflang;
    }

    /**
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * @param title the title to set
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * @return the length
     */
    public Integer getLength() {
        return length;
    }

    /**
     * @param length the length to set
     */
    public void setLength(Integer length) {
        this.length = length;
    }
}
