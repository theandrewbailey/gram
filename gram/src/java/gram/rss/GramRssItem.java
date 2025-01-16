package gram.rss;

import libWebsiteTools.rss.RssItem;
import org.w3c.dom.Element;

/**
 *
 * @author alpha
 */
public class GramRssItem extends RssItem {

    private static final String TAB_URI = "https://theandrewbailey.com/";
    private static final String TAB_NAMESPACE = "tab";

    private static final String MARKDOWN_NAME = "markdownsource";
    public static final String MARKDOWN_ELEMENT_NAME = GramRssItem.TAB_NAMESPACE + ":" + GramRssItem.MARKDOWN_NAME;

    private static final String TAB_METADESC_NAME = "metadescription";
    public static final String TAB_METADESC_ELEMENT_NAME = GramRssItem.TAB_NAMESPACE + ":" + GramRssItem.TAB_METADESC_NAME;

    private static final String SUGGESTION_NAME = "articleSuggestionTerm";
    public static final String SUGGESTION_ELEMENT_NAME = GramRssItem.TAB_NAMESPACE + ":" + GramRssItem.SUGGESTION_NAME;

    private static final String SUMMARY_NAME = "summary";
    public static final String SUMMARY_ELEMENT_NAME = GramRssItem.TAB_NAMESPACE + ":" + GramRssItem.SUMMARY_NAME;

    private static final String IMAGEURL_NAME = "imageURL";
    public static final String IMAGEURL_ELEMENT_NAME = GramRssItem.TAB_NAMESPACE + ":" + GramRssItem.IMAGEURL_NAME;

    private String markdownSource;
    private String metadescription;
    private String suggestion;
    private String summary;
    private String imageURL;

    /**
     * default constructor please do not use
     */
    public GramRssItem() {
    }

    /**
     * use this constructor
     *
     * @param iDesc description of the item in the feed (required)
     */
    public GramRssItem(String iDesc) {
        setDescription(iDesc);
    }

    @Override
    public Element publish(Element chan) {
        Element item = super.publish(chan);
        if (null != markdownSource) {
            Element n = chan.getOwnerDocument().createElementNS(TAB_URI, MARKDOWN_NAME);
            n.setPrefix(TAB_NAMESPACE);
            n.appendChild(chan.getOwnerDocument().createCDATASection(getMarkdownSource()));
            item.appendChild(n);
        }
        if (null != metadescription) {
            Element n = chan.getOwnerDocument().createElementNS(TAB_URI, TAB_METADESC_NAME);
            n.setPrefix(TAB_NAMESPACE);
            n.appendChild(chan.getOwnerDocument().createCDATASection(getMetadescription()));
            item.appendChild(n);
        }
        if (null != suggestion) {
            Element n = chan.getOwnerDocument().createElementNS(TAB_URI, SUGGESTION_ELEMENT_NAME);
            n.setPrefix(TAB_NAMESPACE);
            n.appendChild(chan.getOwnerDocument().createCDATASection(getSuggestion()));
            item.appendChild(n);
        }
        if (null != summary) {
            Element n = chan.getOwnerDocument().createElementNS(TAB_URI, SUMMARY_ELEMENT_NAME);
            n.setPrefix(TAB_NAMESPACE);
            n.appendChild(chan.getOwnerDocument().createCDATASection(getSummary()));
            item.appendChild(n);
        }
        if (null != imageURL) {
            Element n = chan.getOwnerDocument().createElementNS(TAB_URI, IMAGEURL_ELEMENT_NAME);
            n.setPrefix(TAB_NAMESPACE);
            n.appendChild(chan.getOwnerDocument().createCDATASection(getImageURL()));
            item.appendChild(n);
        }
        return item;
    }

    public String getMarkdownSource() {
        return markdownSource;
    }

    public void setMarkdownSource(String markdownSource) {
        this.markdownSource = markdownSource;
    }

    public String getMetadescription() {
        return metadescription;
    }

    public void setMetadescription(String metadescription) {
        this.metadescription = metadescription;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public void setSuggestion(String suggestion) {
        this.suggestion = suggestion;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getImageURL() {
        return imageURL;
    }

    public void setImageURL(String imageURL) {
        this.imageURL = imageURL;
    }

}
