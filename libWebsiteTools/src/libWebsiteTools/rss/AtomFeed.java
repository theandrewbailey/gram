package libWebsiteTools.rss;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.DOMException;

/**
 *
 * @author alpha
 */
public abstract class AtomFeed extends AtomCommonAttribs implements Feed, Publishable {

    private final List<AtomPerson> authors = new ArrayList<>();
    private final List<AtomCategory> categories = new ArrayList<>();
    private final List<AtomPerson> contributors = new ArrayList<>();
    private final List<AtomLink> links = new ArrayList<>();
    private final List<AtomEntry> entries = new ArrayList<>();
    private AtomGenerator generator = new AtomGenerator();
    private AtomId id;
    private AtomId logo;
    private AtomText rights;
    private AtomText title;
    private AtomText subtitle;
    private AtomDate updated = new AtomDate();

    // TODO: this, add "links"
    /**
     * unlike what publish() is supposed to do, this takes the root element, and
     * appends the feed's elements directly to it
     *
     * @param root of the document
     * @return root of the document (what else?)
     */
    @Override
    public Element publish(Element root) {
        super.publish(root);
        for (AtomPerson p : getAuthors()) {
            p.publish(root, "author");
        }
        for (AtomCategory c : getCategories()) {
            c.publish(root);
        }
        for (AtomLink l : getLinks()) {
            l.publish(root, "link");
        }
        for (AtomPerson c : getContributors()) {
            c.publish(root, "contributor");
        }
        if (getGenerator() != null) {
            getGenerator().publish(root);
        }
        getId().publish(root, "id");
        if (getLogo() != null) {
            getLogo().publish(root, "logo");
        }
        if (getRights() != null) {
            getRights().publish(root, "rights");
        }
        getTitle().publish(root, "title");
        if (getSubtitle() != null) {
            getSubtitle().publish(root, "subtitle");
        }
        getUpdated().publish(root);
        for (AtomEntry e : getEntries()) {
            e.publish(root, "entry");
        }
        return root;
    }

    /**
     * @return the authors
     */
    public List<AtomPerson> getAuthors() {
        return authors;
    }

    /**
     * @return the categories
     */
    public List<AtomCategory> getCategories() {
        return categories;
    }

    /**
     * @return the contributors
     */
    public List<AtomPerson> getContributors() {
        return contributors;
    }

    /**
     * @return the generator
     */
    public AtomGenerator getGenerator() {
        return generator;
    }

    public List<AtomLink> getLinks() {
        return links;
    }

    public List<AtomEntry> getEntries() {
        return entries;
    }

    /**
     * @param generator the generator to set
     */
    public void setGenerator(AtomGenerator generator) {
        this.generator = generator;
    }

    /**
     * @return the id
     */
    public AtomId getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(AtomId id) {
        this.id = id;
    }

    /**
     * @return the logo
     */
    public AtomId getLogo() {
        return logo;
    }

    /**
     * @param logo the logo to set
     */
    public void setLogo(AtomId logo) {
        this.logo = logo;
    }

    /**
     * @return the rights
     */
    public AtomText getRights() {
        return rights;
    }

    /**
     * @param rights the rights to set
     */
    public void setRights(AtomText rights) {
        this.rights = rights;
    }

    /**
     * @return the title
     */
    public AtomText getTitle() {
        return title;
    }

    /**
     * @param title the title to set
     */
    public void setTitle(AtomText title) {
        this.title = title;
    }

    /**
     * @return the subtitle
     */
    public AtomText getSubtitle() {
        return subtitle;
    }

    /**
     * @param subtitle the subtitle to set
     */
    public void setSubtitle(AtomText subtitle) {
        this.subtitle = subtitle;
    }

    /**
     * @return the updated
     */
    public AtomDate getUpdated() {
        return updated;
    }

    /**
     * @param updated the updated to set
     */
    public void setUpdated(AtomDate updated) {
        this.updated = updated;
    }

    @Override
    public long getLastModified(HttpServletRequest req) {
        return -1;
    }

    @Override
    public Document preWrite(HttpServletRequest req, HttpServletResponse res) {
        Document XML = null;
        try {
            XML = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            Element root = XML.createElement("feed");
            XML.createAttributeNS("http://www.w3.org/2005/Atom", "");
            XML.appendChild(publish(root));
        } catch (ParserConfigurationException | DOMException ex) {
            Logger log = Logger.getLogger(AtomFeed.class.getName());
            log.log(Level.SEVERE, "Error during AtomFeed refreshFeed()", ex);
        }
        return XML;
    }

    @Override
    public Feed postWrite(HttpServletRequest req) {
        return this;
    }

    @Override
    public Feed preAdd() {
        return this;
    }

    @Override
    public Feed postAdd() {
        return this;
    }

    @Override
    public Feed doHead(HttpServletRequest req, HttpServletResponse res) {
        return this;
    }

    @Override
    public Feed preRemove() {
        return this;
    }

    @Override
    public Feed postRemove() {
        return this;
    }
}
