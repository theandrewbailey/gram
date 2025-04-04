package gram.rss;

import static gram.CategoryFetcher.PAGES_AROUND_CURRENT;
import static gram.CategoryFetcher.POSTS_PER_PAGE;
import gram.bean.GramLandlord;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.HttpHeaders;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import libWebsiteTools.imead.Local;
import libWebsiteTools.security.SecurityRepository;
import libWebsiteTools.security.HashUtil;
import libWebsiteTools.rss.RssChannel;
import libWebsiteTools.rss.RssServlet;
import org.w3c.dom.Document;
import gram.bean.database.Article;
import gram.bean.database.Section;
import gram.servlet.GramServlet;
import gram.tag.ArticleUrl;
import gram.tag.Categorizer;
import libWebsiteTools.rss.Feed;
import libWebsiteTools.rss.DynamicFeed;
import gram.bean.GramTenant;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class ArticleRss implements DynamicFeed {

    public static final String NAME = "Articles.rss";
    private static final Logger LOG = Logger.getLogger(Article.class.getName());
    private static final Pattern NAME_PATTERN = Pattern.compile("(.*?)Articles\\.rss");

    public ArticleRss() {
    }

    public Document createFeed(GramTenant ten, List<Locale> locales, String catName, Integer numEntries) {
        RssChannel entries = new RssChannel(null == catName
                ? ten.getImead().getLocal(GramServlet.SITE_TITLE, locales)
                : ten.getImead().getLocal(GramServlet.SITE_TITLE, locales) + " - " + catName,
                ten.getImeadValue(SecurityRepository.BASE_URL), ten.getImead().getLocal(GramServlet.TAGLINE, locales));
        entries.setWebMaster(ten.getImeadValue(Feed.MASTER));
        entries.setManagingEditor(entries.getWebMaster());
        entries.setLanguage(locales.get(0).toLanguageTag());
        entries.setCopyright(ten.getImeadValue(Feed.COPYRIGHT));
        List<Article> articles = ten.getArts().search(new Section(catName), numEntries);
        List<Duration> timings = new ArrayList<>(articles.size() + 1);
        OffsetDateTime lastTime = OffsetDateTime.now();
        for (Article art : articles) {
            String text = art.getPostedhtml();
            GramRssItem i = new GramRssItem(text);
            entries.addItem(i);
            i.setTitle(art.getArticletitle());
            i.setAuthor(art.getPostedname());
            i.setLink(ArticleUrl.getUrl(ten.getImeadValue(SecurityRepository.BASE_URL), art, null));
            i.setGuid(art.getUuid().toString());
            i.setPubDate(art.getPosted());
            i.setMarkdownSource(art.getPostedmarkdown());
            i.setSuggestion(art.getSuggestion());
            i.setDescription(art.getPostedhtml());
            i.setMetadescription(art.getDescription());
            i.setSummary(art.getSummary());
            i.setImageURL(art.getImageurl());
            if (null != art.getSectionid()) {
                i.addCategory(art.getSectionid().getName(), Categorizer.getUrl(ten.getImeadValue(SecurityRepository.BASE_URL), catName, null));
            }
            if (art.getComments()) {
                i.setComments(i.getLink() + "#comments");
            }
            timings.add(Duration.between(art.getPosted(), lastTime).abs());
            lastTime = art.getPosted();
        }
        if (articles.size() >= Integer.parseInt(ten.getImeadValue(POSTS_PER_PAGE))) {
            Double average = timings.stream().mapToLong((t) -> t.toMinutes()).average().getAsDouble() * 0.4;
            entries.setTtl(average.intValue());
        }
        return Feed.refreshFeed(Arrays.asList(entries));
    }

    @Override
    public String getName() {
        return NAME;
    }

    /**
     *
     * @param req
     * @return Map of (category)Articles.rss, (category) articles
     */
    @Override
    public Map<String, String> getFeedURLs(HttpServletRequest req) {
        Map<String, String> urls = new LinkedHashMap<>();
        urls.put(getName(), "All articles");
        GramTenant ten = GramLandlord.getTenant(req);
        for (Section category : ten.getCategories().getAll(null)) {
            urls.put(category.getName() + getName(), category.getName() + " articles");
        }
        return urls;
    }

    /**
     *
     * @param name
     * @return if name matches (.*?)Articles\\.rss
     */
    @Override
    public boolean willHandle(String name) {
        return NAME_PATTERN.matcher(name).matches();
    }

    @Override
    public Feed preAdd() {
        return this;
    }

    @Override
    public Feed doHead(HttpServletRequest req, HttpServletResponse res) {
        if (null == req.getAttribute(NAME)) {
            GramTenant ten = GramLandlord.getTenant(req);
            List<Locale> resolvedLocales = Local.resolveLocales(ten.getImead(), req);
            Object name = req.getAttribute(RssServlet.class.getSimpleName());
            Matcher regex = NAME_PATTERN.matcher(name.toString());
            regex.find();
            String category = (regex.group(1) != null && !regex.group(1).isEmpty())
                    ? regex.group(1) : null;
            try {
                Document XML = createFeed(ten, resolvedLocales, category,
                        Integer.parseInt(ten.getImeadValue(POSTS_PER_PAGE)) * Integer.parseInt(ten.getImeadValue(PAGES_AROUND_CURRENT)));
                DOMSource DOMsrc = new DOMSource(XML);
                StringWriter holder = new StringWriter(100000);
                StreamResult str = new StreamResult(holder);
                Transformer trans = TransformerFactory.newInstance().newTransformer();
                trans.transform(DOMsrc, str);
                String etag = "\"" + HashUtil.getSHA256Hash(holder.toString()) + "\"";
                res.setHeader(HttpHeaders.ETAG, etag);
                req.removeAttribute(Local.LOCALE_PARAM);
                req.setAttribute(Local.OVERRIDE_LOCALE_PARAM, resolvedLocales.get(0));
                req.setAttribute(HttpHeaders.ETAG, etag);
                req.setAttribute(NAME, XML);
                if (etag.equals(req.getHeader(HttpHeaders.IF_NONE_MATCH))) {
                    res.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                }
            } catch (NumberFormatException n) {
                LOG.log(Level.SEVERE, "Article feed will not be available due to an invalid parameter.");
                return null;
            } catch (TransformerException ex) {
                LOG.log(Level.SEVERE, "Article feed will not be available due to an XML transformation error.", ex);
                res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return null;
            }
        }
        return this;
    }

    @Override
    public Document preWrite(HttpServletRequest req, HttpServletResponse res) {
        doHead(req, res);
        return HttpServletResponse.SC_OK == res.getStatus() ? (Document) req.getAttribute(NAME) : null;
    }
}
