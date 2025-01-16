package libWebsiteTools.rss;

import org.w3c.dom.Element;

/**
 *
 * @author alpha
 */
public class AtomText extends AtomCommonAttribs {

    public static enum Type {
        text,
        html,
        xhtml
    }
    private Type type;
    private String text = "you should have used the other constructor";

    public AtomText(Type ty, String tx) {
        type = ty;
        text = tx;
    }

    public AtomText(String tx) {
        text = tx;
    }

    public AtomText() {
    }

    /**
     * this is a generic type, name what you will
     * @param xml
     * @param name
     * @return
     */
    @Override
    public Element publish(Element xml, String name) {
        Element item = super.publish(xml, name);
        switch (getType()) {
            case html:
                item.setAttribute("type", "html");
                break;
            case xhtml:
                item.setAttribute("type", "xhtml");
                item.setAttribute("xmlns:xhtml", "http://www.w3.org/1999/xhtml");
                break;
            case text:
                item.setAttribute("type", "text");
                break;
        }
        item.setTextContent(getText());
        return item;
    }

    /**
     * should use other override instead
     * @param xml
     * @return
     */
    @Override
    public Element publish(Element xml) {
        return publish(xml, "text");
    }

    /**
     * @return the type
     */
    public Type getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(Type type) {
        this.type = type;
    }

    /**
     * @return the text
     */
    public String getText() {
        return text;
    }

    /**
     * @param text the text to set
     */
    public void setText(String text) {
        this.text = text;
    }
}
