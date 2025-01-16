package libWebsiteTools.rss;

import org.w3c.dom.Element;

/**
 *
 * @author alpha
 */
public class AtomPerson extends AtomCommonAttribs {

    private String name = "anonymous";
    private String uri = "http://google.com";
    private String email = "not@this.again";

    /**
     * conditionally attaches a new node to the given node with the content as text
     * if content == null, no node is created and this method will return null
     * @param parent node to attach to
     * @param name what the node should be named
     * @param content text to put into the node (toString is called on this)
     * @return new node, or null if content == null
     */
    public static Element textNode(Element parent, String name, Object content) {
        if (content != null) {
            Element n = parent.getOwnerDocument().createElement(name);
            n.setTextContent(content.toString());
            parent.appendChild(n);
            return n;
        }
        return null;
    }

    public AtomPerson(String n, String u, String e) {
        name = n;
        uri = u;
        email = e;
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
        textNode(xml, "name", this.getName());
        textNode(xml, "uri", getUri());
        textNode(xml, "email", getEmail());
        return item;
    }

    /**
     * should probably use other override
     * @param xml
     * @return
     */
    @Override
    public Element publish(Element xml) {
        return publish(xml, "person");
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

    /**
     * @return the email
     */
    public String getEmail() {
        return email;
    }

    /**
     * @param email the email to set
     */
    public void setEmail(String email) {
        this.email = email;
    }
}
