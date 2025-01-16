package libWebsiteTools.rss;

import java.util.ArrayList;
import java.util.List;
import org.w3c.dom.Element;

/**
 *
 * @author alpha
 */
public class AtomEntry extends AtomCommonAttribs {

    private final List<AtomPerson> authors = new ArrayList<>();
    private final List<AtomCategory> categorys = new ArrayList<>();
    private final List<AtomPerson> contributors = new ArrayList<>();
    private final List<AtomLink> links = new ArrayList<>();
    private AtomText content;
    private AtomId id;
    private AtomDate published;
    private AtomText rights;
    private AtomFeed source;
    private AtomText summary;
    private AtomText title;
    private AtomDate updated = new AtomDate();

    @Override
    public Element publish(Element xml) {
        Element e = super.publish(xml);
        getId().publish(e, "id");
        getTitle().publish(e, "title");
        getUpdated().publish(e);
        for (AtomPerson p : getAuthors()) {
            p.publish(e, "author");
        }
        if (getContent() != null) {
            getContent().publish(e, "content");
        }
        if (getSummary() != null) {
            getSummary().publish(e, "summary");
        }
        for (AtomCategory c : getCategorys()) {
            c.publish(e);
        }
        for (AtomLink l : getLinks()) {
            l.publish(e, "link");
        }
        for (AtomPerson c : getContributors()) {
            c.publish(e, "contributor");
        }
        if (getPublished() != null) {
            getPublished().publish(e, "published");
        }
        if (getRights() != null) {
            getRights().publish(e, "rights");
        }
        if (getSource() != null) {
            getSource().publish(e, "source");
        }
        return e;
    }

    public AtomFeed getSource() {
        if (source != null) {
            source.getEntries().clear();
        }
        return source;
    }

    public void setSource(AtomFeed src) {
        source = src;
        source.getEntries().clear();
    }

    /**
     * @return the authors
     */
    public List<AtomPerson> getAuthors() {
        return authors;
    }

    /**
     * @return the categorys
     */
    public List<AtomCategory> getCategorys() {
        return categorys;
    }

    /**
     * @return the contributors
     */
    public List<AtomPerson> getContributors() {
        return contributors;
    }
    /**
     * @return the links
     */
    public List<AtomLink> getLinks() {
        return links;
    }

    /**
     * @return the content
     */
    public AtomText getContent() {
        return content;
    }

    /**
     * @param content the content to set
     */
    public void setContent(AtomText content) {
        this.content = content;
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
     * @return the published
     */
    public AtomDate getPublished() {
        return published;
    }

    /**
     * @param published the published to set
     */
    public void setPublished(AtomDate published) {
        this.published = published;
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
     * @return the summary
     */
    public AtomText getSummary() {
        return summary;
    }

    /**
     * @param summary the summary to set
     */
    public void setSummary(AtomText summary) {
        this.summary = summary;
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
}
