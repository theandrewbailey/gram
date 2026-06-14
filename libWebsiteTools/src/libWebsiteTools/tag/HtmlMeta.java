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
import java.util.Locale;
import libWebsiteTools.security.SecurityRepository;
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
    public static final String LINK_LOCALE_URLS = "$_HTML_LINK_LOCALE_URLS";
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
    public static void addLocaleURL(HttpServletRequest req, Locale locale, String URL) {
        List tags = (List) req.getAttribute(LINK_LOCALE_URLS);
        if (tags == null) {
            tags = new ArrayList<>();
            req.setAttribute(LINK_LOCALE_URLS, tags);
        }
        if (locale.toLanguageTag().isEmpty()) {
            tags.add(new AbstractMap.SimpleEntry<>(Locale.getDefault().getLanguage(), URL));
        } else {
            tags.add(new AbstractMap.SimpleEntry<>(locale.toLanguageTag(), URL));
        }
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
        StringBuilder out = new StringBuilder(4000).append("<meta charset=\"").append(((PageContext) getJspContext()).getResponse().getCharacterEncoding()).append("\"/>\n");
        Object baseURL = ((HttpServletRequest) ((PageContext) getJspContext()).getRequest()).getAttribute(SecurityRepository.BASE_URL);
        if (null != baseURL && !baseURL.toString().isEmpty()) {
            out.append("<base href=\"").append(baseURL.toString()).append("\"/>\n");
        } else {
            baseURL = ten.getImeadValue(SecurityRepository.BASE_URL);
        }
        emptyBuilder(output, out);
        try {
            for (Map.Entry<String, String> tag : (List<Map.Entry<String, String>>) getJspContext().findAttribute(META_NAME_TAGS)) {
                out.append("<meta name=\"").append(tag.getKey()).append("\" content=\"").append(tag.getValue()).append("\"/>\n");
            }
        } catch (NullPointerException n) {
        }
        emptyBuilder(output, out);
        try {
            for (Map.Entry<String, String> tag : (List<Map.Entry<String, String>>) getJspContext().findAttribute(META_PROPERTY_TAGS)) {
                out.append("<meta property=\"").append(tag.getKey()).append("\" content=\"").append(tag.getValue()).append("\"/>\n");
            }
        } catch (NullPointerException n) {
        }
        emptyBuilder(output, out);
        try {
            for (Map.Entry<String, String> tag : (List<Map.Entry<String, String>>) getJspContext().findAttribute(LINK_TAGS)) {
                out.append("<link rel=\"").append(tag.getKey()).append("\" href=\"").append(tag.getValue()).append("\">\n");
            }
        } catch (NullPointerException n) {
        }
        emptyBuilder(output, out);
        try {
            for (Map.Entry<String, String> tag : (List<Map.Entry<String, String>>) getJspContext().findAttribute(LINK_LOCALE_URLS)) {
                out.append("<link rel=\"alternate\" hreflang=\"").append(tag.getKey()).append("\" href=\"").append(tag.getValue()).append("\">\n");
            }
        } catch (NullPointerException n) {
        }
        emptyBuilder(output, out);
        for (Feed feed : ten.getFeeds().getAll(null)) {
            if (feed instanceof DynamicFeed dFeed) {
                for (Map.Entry<String, String> entry : dFeed.getFeedURLs(req).entrySet()) {
                    out.append("<link rel=\"alternate\" href=\"").append(baseURL.toString()).append("rss/").append(entry.getKey())
                            .append("\" title=\"").append(entry.getValue())
                            .append("\" type=\"").append(feed.getMimeType().toString()).append("\">\n");
                }
            }
        }
        emptyBuilder(output, out);
        try {
            for (Object json : (List) getJspContext().findAttribute(LDJSON)) {
                out.append("<script type=\"application/ld+json\">").append(json.toString()).append("</script>\n");
            }
        } catch (NullPointerException n) {
        }
        emptyBuilder(output, out);
    }

    public static void emptyBuilder(JspWriter output, StringBuilder out) throws IOException {
        if (!out.isEmpty()) {
            output.print(out.toString());
            out.setLength(0);
        }
    }
}
