package libWebsiteTools.rss;

import org.w3c.dom.Element;

/**
 *
 * @author alpha
 */
public interface Publishable {

    /**
     * adds this object's information to the given XML element if adding
     * additional elements or namespaces, override this method, and call
     * super.publish()
     *
     * @param e the parent xml node of this object
     * @return created element corresponding to this object
     */
    public Element publish(Element e);
}
