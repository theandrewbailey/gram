package gram.servlet;

import java.io.IOException;
import java.text.MessageFormat;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.HttpHeaders;
import java.time.Duration;
import java.time.Instant;
import libWebsiteTools.security.SecurityRepo;
import libWebsiteTools.imead.Local;
import libWebsiteTools.turbo.RequestTimer;
import libWebsiteTools.tag.HtmlMeta;
import gram.IndexFetcher;
import gram.bean.GramLandlord;
import gram.bean.database.Article;
import gram.tag.Categorizer;
import gram.bean.GramTenant;

@WebServlet(name = "IndexServlet", description = "Gets all the posts of a single group, defaults to Home", urlPatterns = {"/index/*", "/index.html", "/index", "/"})
public class IndexServlet extends GramServlet {

    public static final String HOME_JSP = "/WEB-INF/category.jsp";

    private IndexFetcher getIndexFetcher(HttpServletRequest req) {
        IndexFetcher f = (IndexFetcher) req.getAttribute(IndexFetcher.class.getCanonicalName());
        if (null == f) {
            Instant start = Instant.now();
            GramTenant ten = GramLandlord.getTenant(req);
            String URI = req.getRequestURI();
            if (URI.startsWith(getServletContext().getContextPath())) {
                URI = URI.substring(getServletContext().getContextPath().length());
            }
            f = new IndexFetcher(ten, URI);
            RequestTimer.addTiming(req, "query", Duration.between(start, Instant.now()));
            req.setAttribute(IndexFetcher.class.getCanonicalName(), f);
        }
        return f;
    }

    @Override
    protected long getLastModified(HttpServletRequest request) {
        OffsetDateTime latest = OffsetDateTime.of(2000, 1, 1, 1, 1, 1, 1, ZoneOffset.UTC);
        try {
            IndexFetcher f = getIndexFetcher(request);
            for (Article a : f.getArticles()) {
                if (a.getModified().isAfter(latest)) {
                    latest = a.getModified();
                }
            }
        } catch (NumberFormatException n) {
        }
        return latest.toInstant().toEpochMilli();
    }

    @Override
    protected void doHead(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        IndexFetcher f = getIndexFetcher(request);
        Collection<Article> articles = f.getArticles();
        if (articles.isEmpty()) {
            request.setAttribute(IndexFetcher.class.getCanonicalName(), null);
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String ifNoneMatch = request.getHeader(HttpHeaders.IF_NONE_MATCH);
        String etag = request.getAttribute(HttpHeaders.ETAG).toString();
        response.setHeader(HttpHeaders.ETAG, etag);
        if (etag.equals(ifNoneMatch)) {
            request.setAttribute(IndexFetcher.class.getCanonicalName(), null);
            response.sendError(HttpServletResponse.SC_NOT_MODIFIED);
            return;
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        GramTenant ten = GramLandlord.getTenant(request);
        if (ten.isFirstTime()) {
            String url = AdminImeadServlet.class.getAnnotation(WebServlet.class).urlPatterns()[0];
            request.getRequestDispatcher(url).forward(request, response);
            return;
        }
        doHead(request, response);
        IndexFetcher f = getIndexFetcher(request);
        if (null != f && !response.isCommitted()) {
            Collection<Article> articles = f.getArticles();
            articles.stream().limit(2).forEach((art) -> {
                art.setSummary(art.getSummary().replaceAll(" loading=\"lazy\"", ""));
            });
            // dont bother if there is only 1 page total
            if (f.getCount() > 1) {
                request.setAttribute("pagen_first", f.getFirst());
                request.setAttribute("pagen_last", f.getLast());
                request.setAttribute("pagen_current", f.getPage());
                request.setAttribute("pagen_count", f.getCount());
            } else if (null == f.getSection() && 0 == ten.getArts().count(null)) {
                String message = MessageFormat.format(ten.getImead().getLocal("page_noPosts", Local.resolveLocales(ten.getImead(), request)), new Object[]{request.getAttribute(SecurityRepo.BASE_URL).toString() + "adminLogin"});
                request.setAttribute(CoronerServlet.ERROR_MESSAGE_PARAM, message);
                request.getServletContext().getRequestDispatcher(CoronerServlet.ERROR_JSP).forward(request, response);
                return;
            } else if (HttpServletResponse.SC_NOT_FOUND == response.getStatus()) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            if (null != f.getSection()) {
                request.setAttribute("curGroup", f.getSection().getName());
                request.setAttribute("title", f.getSection().getName());
            }
            request.setAttribute("articles", articles);
            request.setAttribute("articleCategory", f.getSection());
            request.setAttribute("index", true);
            for (Article art : f.getArticles()) {
                if (null != art.getImageurl()) {
                    HtmlMeta.addPropertyTag(request, "og:image", art.getImageurl());
                    break;
                }
            }

            StringBuilder description = new StringBuilder(70).append(ten.getImead().getLocal(GramServlet.SITE_TITLE, Local.resolveLocales(ten.getImead(), request)));
            if (null == f.getSection() && 1 != f.getPage()) {
                description.append(", all categories, page ").append(f.getPage());
            } else if (null != f.getSection()) {
                description.append(", ").append(f.getSection()).append(" category, page ").append(f.getPage());
            } else {
                description.append(", ").append(ten.getImead().getLocal(GramServlet.TAGLINE, Local.resolveLocales(ten.getImead(), request)));
            }

            HtmlMeta.addPropertyTag(request, "og:description", description.toString());
            HtmlMeta.addPropertyTag(request, "og:site_name", ten.getImead().getLocal(GramServlet.SITE_TITLE, Local.resolveLocales(ten.getImead(), request)));
            HtmlMeta.addPropertyTag(request, "og:type", "website");
            HtmlMeta.addNameTag(request, "description", description.toString());
            HtmlMeta.addLink(request, "canonical", Categorizer.getUrl(request.getAttribute(SecurityRepo.BASE_URL).toString(), f.getSection().getName(), f.getPage()));
            JsonArrayBuilder itemList = Json.createArrayBuilder();
            itemList.add(HtmlMeta.getLDBreadcrumb(ten.getImead().getLocal("page_title", Local.resolveLocales(ten.getImead(), request)), 1, request.getAttribute(SecurityRepo.BASE_URL).toString()));
            if (null == f.getSection() && 1 == f.getPage() && null == request.getAttribute(Local.OVERRIDE_LOCALE_PARAM)) {
                JsonObjectBuilder potentialAction = Json.createObjectBuilder().add("@type", "SearchAction").add("target", ten.getImeadValue(SecurityRepo.BASE_URL) + "search?searchTerm={search_term_string}").add("query-input", "required name=search_term_string");
                JsonObjectBuilder search = Json.createObjectBuilder().add("@context", "https://schema.org").add("@type", "WebSite").add("url", ten.getImeadValue(SecurityRepo.BASE_URL)).add("potentialAction", potentialAction.build());
                HtmlMeta.addLDJSON(request, search.build().toString());
            } else if (null != f.getSection()) {
                itemList.add(HtmlMeta.getLDBreadcrumb(f.getSection().getName(), 2, Categorizer.getUrl(request.getAttribute(SecurityRepo.BASE_URL).toString(), f.getSection().getName(), null)));
            }
            JsonObjectBuilder breadcrumbs = Json.createObjectBuilder().add("@context", "https://schema.org").add("@type", "BreadcrumbList").add("itemListElement", itemList.build());
            HtmlMeta.addLDJSON(request, breadcrumbs.build().toString());
            request.getServletContext().getRequestDispatcher(HOME_JSP).forward(request, response);
        }
    }
}
