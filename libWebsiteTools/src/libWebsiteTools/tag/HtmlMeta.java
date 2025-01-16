package libWebsiteTools.tag;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.jsp.JspException;
import jakarta.servlet.jsp.JspWriter;
import jakarta.servlet.jsp.PageContext;
import jakarta.servlet.jsp.tagext.SimpleTagSupport;
import libWebsiteTools.security.SecurityRepo;
import libWebsiteTools.rss.Feed;
import libWebsiteTools.rss.DynamicFeed;
import libWebsiteTools.Landlord;
import libWebsiteTools.Tenant;

/**
 * puts <meta> and <link> tags on pages.
 *
 * @author alpha
 */
public class HtmlMeta extends SimpleTagSupport {

    public static final String META_NAME_TAGS = "$_HTML_META_NAME_TAGS";
    public static final String META_PROPERTY_TAGS = "$_HTML_META_PROPERTY_TAGS";
    public static final String LINK_TAGS = "$_HTML_LINK_TAGS";
    public static final String LDJSON = "$_HTML_LDJSON";

    @SuppressWarnings("unchecked")
    public static void addPropertyTag(HttpServletRequest req, String property, String content) {
        List tags = (List) req.getAttribute(META_PROPERTY_TAGS);
        if (tags == null) {
            tags = new ArrayList<>();
            req.setAttribute(META_PROPERTY_TAGS, tags);
        }
        tags.add(new AbstractMap.SimpleEntry<>(property, content));
    }

    @SuppressWarnings("unchecked")
    public static void addNameTag(HttpServletRequest req, String name, String content) {
        List tags = (List) req.getAttribute(META_NAME_TAGS);
        if (tags == null) {
            tags = new ArrayList<>();
            req.setAttribute(META_NAME_TAGS, tags);
        }
        tags.add(new AbstractMap.SimpleEntry<>(name, content));
    }

    @SuppressWarnings("unchecked")
    public static void addLink(HttpServletRequest req, String rel, String href) {
        if (null == rel || null == href) {
            return;
        }
        List tags = (List) req.getAttribute(LINK_TAGS);
        if (tags == null) {
            tags = new ArrayList<>();
            req.setAttribute(LINK_TAGS, tags);
        }
        tags.add(new AbstractMap.SimpleEntry<>(rel, href));
    }

    @SuppressWarnings("unchecked")
    public static void addLDJSON(HttpServletRequest req, String json) {
        if (null == json) {
            return;
        }
        List jsons = (List) req.getAttribute(LDJSON);
        if (jsons == null) {
            jsons = new ArrayList<>();
            req.setAttribute(LDJSON, jsons);
        }
        jsons.add(json);
    }

    public static JsonObjectBuilder getLDBreadcrumb(String name, Integer position, String url) {
        JsonObjectBuilder crumb = Json.createObjectBuilder().add("@type", "ListItem").add("position", position).add("name", name);
        if (null != url) {
            crumb.add("item", url);
        }
        return crumb;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void doTag() throws JspException, IOException {
        JspWriter output = getJspContext().getOut();
        HttpServletRequest req = ((HttpServletRequest) ((PageContext) getJspContext()).getRequest());
        Tenant ten = Landlord.getTenant(req);
        output.println(String.format("<meta charset=\"%s\"/>", ((PageContext) getJspContext()).getResponse().getCharacterEncoding()));
        Object baseURL = ((HttpServletRequest) ((PageContext) getJspContext()).getRequest()).getAttribute(SecurityRepo.BASE_URL);
        if (null != baseURL && !baseURL.toString().isEmpty()) {
            output.println(String.format("<base href=\"%s\"/>", baseURL.toString()));
        }
        try {
            for (Map.Entry<String, String> tag : (List<Map.Entry<String, String>>) getJspContext().findAttribute(META_NAME_TAGS)) {
                output.println(String.format("<meta name=\"%s\" content=\"%s\"/>", tag.getKey(), tag.getValue()));
            }
        } catch (NullPointerException n) {
        }
        try {
            for (Map.Entry<String, String> tag : (List<Map.Entry<String, String>>) getJspContext().findAttribute(META_PROPERTY_TAGS)) {
                output.println(String.format("<meta property=\"%s\" content=\"%s\"/>", tag.getKey(), tag.getValue()));
            }
        } catch (NullPointerException n) {
        }
        try {
            for (Map.Entry<String, String> tag : (List<Map.Entry<String, String>>) getJspContext().findAttribute(LINK_TAGS)) {
                output.println(String.format("<link rel=\"%s\" href=\"%s\">", tag.getKey(), tag.getValue()));
            }
        } catch (NullPointerException n) {
        }
        for (Feed feed : ten.getFeeds().getAll(null)) {
            if (feed instanceof DynamicFeed) {
                for (Map.Entry<String, String> entry : ((DynamicFeed) feed).getFeedURLs(req).entrySet()) {
                    output.println(String.format("<link rel=\"alternate\" href=\"%srss/%s\" title=\"%s\" type=\"%s\">",
                            ten.getImeadValue(SecurityRepo.BASE_URL), entry.getKey(), entry.getValue(), feed.getMimeType().toString()));
                }
            }
        }
        try {
            for (Object json : (List) getJspContext().findAttribute(LDJSON)) {
                output.println(String.format("<script type=\"application/ld+json\">%s</script>", json.toString()));
            }
        } catch (NullPointerException n) {
        }
    }
}
