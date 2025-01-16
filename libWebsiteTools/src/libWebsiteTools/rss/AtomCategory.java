package libWebsiteTools.rss;

import org.w3c.dom.Element;

/**
 *
 * @author alpha
 */
public class AtomCategory extends AtomCommonAttribs {

    private String term = "should define this";
    private String scheme;
    private String label;

    public AtomCategory(String t, String s, String l) {
        term = t;
        scheme = s;
        label = l;
    }

    public AtomCategory() {
    }

    @Override
    public Element publish(Element xml) {
        Element item = super.publish(xml, "category");
        item.setAttribute("term", getTerm());
        if (getScheme() != null) {
            item.setAttribute("scheme", getScheme());
        }
        if (getLabel() != null) {
            item.setAttribute("label", getLabel());
        }
        return item;
    }

    /**
     * @return the term
     */
    public String getTerm() {
        return term;
    }

    /**
     * @param term the term to set
     */
    public void setTerm(String term) {
        this.term = term;
    }

    /**
     * @return the scheme
     */
    public String getScheme() {
        return scheme;
    }

    /**
     * @param scheme the scheme to set
     */
    public void setScheme(String scheme) {
        this.scheme = scheme;
    }

    /**
     * @return the label
     */
    public String getLabel() {
        return label;
    }

    /**
     * @param label the label to set
     */
    public void setLabel(String label) {
        this.label = label;
    }
}
