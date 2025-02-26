package libWebsiteTools.rss;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Represents a channel or subsection of an Rss feed. You shouldn't need to
 * extend unless you are adding a namespace to the feed
 *
 * @author alpha
 */
public class RssChannel implements Serializable, Publishable {

    public static final String GENERATOR = "Gram/libWebsiteTools v11.1";
    public static final String COUNT_ATTRIBUTE = "itemCount";
    protected final String docs = "http://cyber.law.harvard.edu/tech/rss";
    private final String[] namesOfDays = new String[]{"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};

    private String title = "Replace This Title";  // standard RSS parameters
    private String link = "Replace this link";
    private String description = "Replace this description";
    private String copyright;
    private String managingEditor;
    private String webMaster;
    private String rating;
    private String language;

    private String iUrl;                // image parameters
    private String iTitle;
    private String iLink;
    private String iDesc;
    private Integer iHeight;
    private Integer iWidth;

    private String cDomain;             // cloud parameters
    private String cPort;
    private String cPath;
    private String cRegister;
    private String cProtocol;

    private String tTitle;              // textInput parameters
    private String tDesc;
    private String tName;
    private String tLink;

    private OffsetDateTime pubDate;
    private OffsetDateTime lastBuildDate;
    private Integer ttl = 1400;         // about daily
    private boolean[] skipHours = new boolean[24];
    private boolean[] skipDays = new boolean[7];
    private int limit = 0;

    protected final ArrayList<RssItem> items = new ArrayList<>();
    protected final ArrayList<RssCategory> cats = new ArrayList<>();

    /**
     * conditionally attaches a new node to the given node with the content as
     * text if content == null, no node is created and this method will return
     * null
     *
     * @param parent node to attach to
     * @param name what the node should be named
     * @param content text to put into the node (toString is called on this)
     * @return new node, or null if content == null
     */
    public static Element cdataTextNode(Element parent, String name, Object content) {
        if (content != null) {
            Element n = parent.getOwnerDocument().createElement(name);
            parent.appendChild(n);
            n.appendChild(parent.getOwnerDocument().createCDATASection(content.toString()));
            return n;
        }
        return null;
    }

    /**
     * does what cdataTextNode does, but doesn't wrap the node in CDATA tags.
     *
     * @param parent node to attach to
     * @param name what the node should be named
     * @param content text to put into the node (toString is called on this)
     * @return new node, or null if content == null
     */
    public static Element textNode(Element parent, String name, Object content) {
        if (content != null) {
            Element n = parent.getOwnerDocument().createElement(name);
            parent.appendChild(n);
            n.setTextContent(content.toString());
            return n;
        }
        return null;
    }

    /**
     * default constructor please do not use
     */
    public RssChannel() {
    }

    /**
     * use this constructor, fill in the required fields for an RSS channel:
     *
     * @param iTitle title of channel ("My Blog")
     * @param iLink HTTP link to respective content ("http://myblog.com/")
     * @param iDesc description of the feed ("read my blog and feed")
     */
    public RssChannel(String iTitle, String iLink, String iDesc) {
        title = iTitle;
        link = iLink;
        description = iDesc;
        pubDate = OffsetDateTime.now();
    }

    /**
     * Inserts an RssItem into the channel, making it the top (most recent) item
     * in the feed. Will remove the last item if over the limit. Also updates
     * the lastBuildDate.
     *
     * @param i the RssItem to be added to the top of the feed
     */
    public void addItemToTop(RssItem i) {
        items.add(0, i);
        if (limit > 0 && items.size() > limit) {
            items.remove(limit);
        }
        lastBuildDate = OffsetDateTime.now();
    }

    /**
     * Inserts an RssItem into the channel, making it the last item in the feed.
     * This should be useful if you rebuild the feed from scratch each time.
     * Will remove the first item if over the limit. Also updates the
     * lastBuildDate.
     *
     * @param i the RssItem to be added
     */
    public void addItem(RssItem i) {
        items.add(i);
        if (limit > 0 && items.size() > limit) {
            items.remove(items.get(0));
        }
        lastBuildDate = OffsetDateTime.now();
    }

    /**
     * Adds a category to the channel.
     *
     * @param name
     * @param domain
     */
    public void addCategory(String name, String domain) {
        cats.add(new RssCategory(name, domain));
    }

    /**
     * removes all items and categories from this feed.
     */
    public void clearFeed() {
        items.clear();
        cats.clear();
    }

    /**
     * tells the aggregator (in theory) to not update at a given hour zero
     * based, 0 = midnight, 23 = 11pm.
     *
     * @param hour
     * @throws ArrayIndexOutOfBoundsException if hour outside of 0-23
     */
    public void setSkipHour(int hour) {
        skipHours[hour] = true;
    }

    /**
     * in theory, tells the aggregator to not update on a given day zero based,
     * 0 = Sunday, 6 = Saturday
     *
     * @param day
     * @throws ArrayIndexOutOfBoundsException if day outside of 0-6
     */
    public void setSkipDay(int day) {
        skipDays[day] = true;
    }

    /**
     * reset all values for skipDay and skipHour
     */
    public void resetSkips() {
        skipHours = new boolean[24];
        skipDays = new boolean[7];
    }

    @Override
    public Element publish(Element root) {
        Document XML = root.getOwnerDocument();
        Element chan = XML.createElement("channel");
        root.appendChild(chan);
        Element n;
        cdataTextNode(chan, "title", getTitle());
        cdataTextNode(chan, "link", getLink());
        cdataTextNode(chan, "description", getDescription());
        cdataTextNode(chan, "language", getLanguage());
        cdataTextNode(chan, "copyright", getCopyright());
        cdataTextNode(chan, "managingEditor", getManagingEditor());
        cdataTextNode(chan, "webMaster", getWebMaster());
        if (getPubDate() != null) {
            cdataTextNode(chan, "pubDate", DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(getPubDate()));
        }
        if (getLastBuildDate() != null) {
            cdataTextNode(chan, "lastBuildDate", DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(getLastBuildDate()));
        }
        for (RssCategory c : cats) {
            c.publish(chan);
        }
        cdataTextNode(chan, "generator", getGenerator());
        cdataTextNode(chan, "docs", getDocs());
        if (getIUrl() != null) {
            n = XML.createElement("cloud");
            XML.appendChild(n);
            n.setAttribute("domain", getCDomain());
            n.setAttribute("port", getCPort());
            n.setAttribute("path", getCPath());
            n.setAttribute("registerProcedure", getCRegister());
            n.setAttribute("protocol", getCProtocol());
        }
        cdataTextNode(chan, "ttl", getTtl());
        if (getIUrl() != null) {
            Element image = XML.createElement("image");
            XML.appendChild(image);
            cdataTextNode(image, "url", getIUrl());
            cdataTextNode(image, "title", getITitle());
            cdataTextNode(image, "link", getILink());
            cdataTextNode(image, "description", getIDesc());
            cdataTextNode(image, "height", getIHeight());
            cdataTextNode(image, "width", getIWidth());
        }
        cdataTextNode(chan, "rating", getRating());
        if (getTTitle() != null) {
            Element text = XML.createElement("textInput");
            XML.appendChild(text);
            cdataTextNode(text, "title", getTTitle());
            cdataTextNode(text, "description", getTDesc());
            cdataTextNode(text, "name", getTName());
            cdataTextNode(text, "link", getTLink());
        }
        n = null;
        for (Integer h = 0; h < skipHours.length; h++) {
            if (skipHours[h]) {
                Element hour = XML.createElement("hour");
                hour.setTextContent(h.toString());
                if (n == null) {
                    n = XML.createElement("skipHours");
                    chan.appendChild(n);
                }
                n.appendChild(hour);
            }
        }
        n = null;
        for (Integer d = 0; d < skipDays.length; d++) {
            if (skipDays[d]) {
                Element day = XML.createElement("day");
                day.setTextContent(namesOfDays[d]);
                if (n == null) {
                    n = XML.createElement("skipDays");
                    chan.appendChild(n);
                }
                n.appendChild(day);
            }
        }
        for (RssItem item : items) {
            item.publish(chan);
        }
        chan.setAttribute(COUNT_ATTRIBUTE, Integer.toString(items.size()));
        return chan;
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
     * @return the link
     */
    public String getLink() {
        return link;
    }

    /**
     * @param link the link to set
     */
    public void setLink(String link) {
        this.link = link;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return the copyright
     */
    public String getCopyright() {
        return copyright;
    }

    /**
     * @param copyright the copyright to set
     */
    public void setCopyright(String copyright) {
        this.copyright = copyright;
    }

    /**
     * @return the managingEditor
     */
    public String getManagingEditor() {
        return managingEditor;
    }

    /**
     * @param managingEditor the managingEditor to set
     */
    public void setManagingEditor(String managingEditor) {
        this.managingEditor = managingEditor;
    }

    /**
     * @return the webMaster
     */
    public String getWebMaster() {
        return webMaster;
    }

    /**
     * @param webMaster the webMaster to set
     */
    public void setWebMaster(String webMaster) {
        this.webMaster = webMaster;
    }

    /**
     * @return the generator
     */
    public String getGenerator() {
        return GENERATOR;
    }

    /**
     * @return the docs
     */
    public String getDocs() {
        return docs;
    }

    /**
     * @return the rating
     */
    public String getRating() {
        return rating;
    }

    /**
     * @param rating the rating to set
     */
    public void setRating(String rating) {
        this.rating = rating;
    }

    /**
     * @return the language
     */
    public String getLanguage() {
        return language;
    }

    /**
     * @param language the language to set
     */
    public void setLanguage(String language) {
        this.language = language;
    }

    /**
     * @return the iUrl
     */
    public String getIUrl() {
        return iUrl;
    }

    /**
     * @param iUrl the iUrl to set
     */
    public void setIUrl(String iUrl) {
        this.iUrl = iUrl;
    }

    /**
     * @return the iTitle
     */
    public String getITitle() {
        return iTitle;
    }

    /**
     * @param iTitle the iTitle to set
     */
    public void setITitle(String iTitle) {
        this.iTitle = iTitle;
    }

    /**
     * @return the iLink
     */
    public String getILink() {
        return iLink;
    }

    /**
     * @param iLink the iLink to set
     */
    public void setILink(String iLink) {
        this.iLink = iLink;
    }

    /**
     * @return the iDesc
     */
    public String getIDesc() {
        return iDesc;
    }

    /**
     * @param iDesc the iDesc to set
     */
    public void setIDesc(String iDesc) {
        this.iDesc = iDesc;
    }

    /**
     * @return the cDomain
     */
    public String getCDomain() {
        return cDomain;
    }

    /**
     * @param cDomain the cDomain to set
     */
    public void setCDomain(String cDomain) {
        this.cDomain = cDomain;
    }

    /**
     * @return the cPort
     */
    public String getCPort() {
        return cPort;
    }

    /**
     * @param cPort the cPort to set
     */
    public void setCPort(String cPort) {
        this.cPort = cPort;
    }

    /**
     * @return the cPath
     */
    public String getCPath() {
        return cPath;
    }

    /**
     * @param cPath the cPath to set
     */
    public void setCPath(String cPath) {
        this.cPath = cPath;
    }

    /**
     * @return the cRegister
     */
    public String getCRegister() {
        return cRegister;
    }

    /**
     * @param cRegister the cRegister to set
     */
    public void setCRegister(String cRegister) {
        this.cRegister = cRegister;
    }

    /**
     * @return the cProtocol
     */
    public String getCProtocol() {
        return cProtocol;
    }

    /**
     * @param cProtocol the cProtocol to set
     */
    public void setCProtocol(String cProtocol) {
        this.cProtocol = cProtocol;
    }

    /**
     * @return the tTitle
     */
    public String getTTitle() {
        return tTitle;
    }

    /**
     * @param tTitle the tTitle to set
     */
    public void setTTitle(String tTitle) {
        this.tTitle = tTitle;
    }

    /**
     * @return the tDesc
     */
    public String getTDesc() {
        return tDesc;
    }

    /**
     * @param tDesc the tDesc to set
     */
    public void setTDesc(String tDesc) {
        this.tDesc = tDesc;
    }

    /**
     * @return the tName
     */
    public String getTName() {
        return tName;
    }

    /**
     * @param tName the tName to set
     */
    public void setTName(String tName) {
        this.tName = tName;
    }

    /**
     * @return the tLink
     */
    public String getTLink() {
        return tLink;
    }

    /**
     * @param tLink the tLink to set
     */
    public void setTLink(String tLink) {
        this.tLink = tLink;
    }

    /**
     * @return the pubDate
     */
    public OffsetDateTime getPubDate() {
        return pubDate;
    }

    /**
     * @param pubDate the pubDate to set
     */
    public void setPubDate(OffsetDateTime pubDate) {
        this.pubDate = pubDate;
    }

    /**
     * @return the lastBuildDate
     */
    public OffsetDateTime getLastBuildDate() {
        return lastBuildDate;
    }

    /**
     * @param lastBuildDate the lastBuildDate to set
     */
    public void setLastBuildDate(OffsetDateTime lastBuildDate) {
        this.lastBuildDate = lastBuildDate;
    }

    /**
     * @return the ttl
     */
    public Integer getTtl() {
        return ttl;
    }

    /**
     * @param ttl the ttl to set
     */
    public void setTtl(Integer ttl) {
        this.ttl = ttl;
    }

    /**
     * @return the iHeight
     */
    public Integer getIHeight() {
        return iHeight;
    }

    /**
     * @param iHeight the iHeight to set
     */
    public void setIHeight(Integer iHeight) {
        if (iHeight < 401) {
            this.iHeight = iHeight;
        }
    }

    /**
     * @return the iWidth
     */
    public Integer getIWidth() {
        return iWidth;
    }

    /**
     * @param iWidth the iWidth to set
     */
    public void setIWidth(Integer iWidth) {
        if (iWidth < 145) {
            this.iWidth = iWidth;
        }
    }

    /**
     * @return the limit
     */
    public int getLimit() {
        return limit;
    }

    /**
     * @param limit The maximum amount of items that this channel can hold, and
     * automatically removes items, if needed
     */
    public void setLimit(int limit) {
        this.limit = limit;
        while (limit > 0 && items.size() > limit) {
            items.remove(limit);
        }
    }
}
