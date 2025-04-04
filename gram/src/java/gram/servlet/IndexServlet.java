package gram.servlet;

import java.io.IOException;
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
import libWebsiteTools.security.SecurityRepository;
import libWebsiteTools.imead.Local;
import libWebsiteTools.turbo.RequestTimer;
import libWebsiteTools.tag.HtmlMeta;
import gram.CategoryFetcher;
import gram.bean.GramLandlord;
import gram.bean.database.Article;
import gram.tag.Categorizer;
import gram.bean.GramTenant;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

@WebServlet(name = "IndexServlet", description = "Get articles of a category", urlPatterns = {"/index/*"})
public class IndexServlet extends GramServlet {

    public static final String HOME_JSP = "/WEB-INF/category.jsp";

    private CategoryFetcher getCategoryFetcher(HttpServletRequest req) {
        CategoryFetcher f = (CategoryFetcher) req.getAttribute(CategoryFetcher.class.getCanonicalName());
        if (null == f) {
            Instant start = Instant.now();
            GramTenant ten = GramLandlord.getTenant(req);
            String URL = req.getRequestURI();
            if (URL.startsWith(getServletContext().getContextPath())) {
                URL = URL.substring(getServletContext().getContextPath().length());
            }
            f = new CategoryFetcher(ten, URL);
            req.setAttribute(CategoryFetcher.class.getCanonicalName(), f);
            RequestTimer.addTiming(req, "query", Duration.between(start, Instant.now()));
        }
        return f;
    }

    @Override
    protected long getLastModified(HttpServletRequest request) {
        OffsetDateTime latest = OffsetDateTime.of(2000, 1, 1, 1, 1, 1, 1, ZoneOffset.UTC);
        try {
            CategoryFetcher f = getCategoryFetcher(request);
            for (Article a : f.getArticles()) {
                if (a.getModified().isAfter(latest)) {
                    latest = a.getModified();
                }
            }
        } catch (IllegalArgumentException n) {
        }
        return latest.toInstant().toEpochMilli();
    }

    @Override
    protected void doHead(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
//        GramTenant ten = GramLandlord.getTenant(request);
        try {
            CategoryFetcher f = getCategoryFetcher(request);
            f.getArticles();
        } catch (IllegalArgumentException gp) {
            request.setAttribute(CategoryFetcher.class.getCanonicalName(), null);
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String ifNoneMatch = request.getHeader(HttpHeaders.IF_NONE_MATCH);
        String etag = request.getAttribute(HttpHeaders.ETAG).toString();
        response.setHeader(HttpHeaders.ETAG, etag);
        if (etag.equals(ifNoneMatch)) {
            request.setAttribute(CategoryFetcher.class.getCanonicalName(), null);
            response.sendError(HttpServletResponse.SC_NOT_MODIFIED);
//            return;
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        GramTenant ten = GramLandlord.getTenant(request);
        List<Locale> resolvedLocales = Local.resolveLocales(ten.getImead(), request);
        doHead(request, response);
        if (!response.isCommitted()) {
            CategoryFetcher f = getCategoryFetcher(request);
            if (null != f) {
                Collection<Article> articles = f.getArticles();
                articles.stream().limit(2).forEach((art) -> {
                    art.setSummary(art.getSummary().replaceAll(" loading=\"lazy\"", ""));
                });
                // dont bother if there is only 1 page total
                if (f.getPageCount() > 1) {
                    request.setAttribute("pagen_first", f.getFirstPage());
                    request.setAttribute("pagen_last", f.getLastPage());
                    request.setAttribute("pagen_current", f.getCurrentPage());
                    request.setAttribute("pagen_count", f.getPageCount());
                } else if ((null == f.getCategory() || null == f.getCategory().getSectionid()) && 0 == ten.getArts().count(null)) {
                    request.getRequestDispatcher("/page/noPosts.html").forward(request, response);
                    return;
                } else if (HttpServletResponse.SC_NOT_FOUND == response.getStatus()) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                if (null != f.getCategory()) {
                    request.setAttribute("curGroup", f.getCategory().getName());
                    request.setAttribute("title", f.getCategory().getName());
                }
                request.setAttribute("articles", articles);
                request.setAttribute("articleCategory", f.getCategory());
                request.setAttribute("index", true);

                StringBuilder description = new StringBuilder(70).append(ten.getImead().getLocal(GramServlet.SITE_TITLE, resolvedLocales));
                if ((null == f.getCategory() || null == f.getCategory().getName()) && 1 != f.getCurrentPage()) {
                    description.append(", all categories, page ").append(f.getCurrentPage());
                } else if (null != f.getCategory() && null != f.getCategory().getName()) {
                    description.append(", ").append(f.getCategory()).append(" category, page ").append(f.getCurrentPage());
                } else {
                    description.append(", ").append(ten.getImead().getLocal(GramServlet.TAGLINE, resolvedLocales));
                }

                HtmlMeta.addNameTag(request, "description", description.toString());
                if (null == request.getParameter("milligram")) {
                    String catName = null != f.getCategory() ? f.getCategory().getName() : ten.getImead().getLocal(GramServlet.SITE_TITLE, resolvedLocales);
                    String catNameNull = null != f.getCategory() ? f.getCategory().getName() : null;
                    String canonical = Categorizer.getUrl(request.getAttribute(SecurityRepository.BASE_URL).toString(), catNameNull, f.getCurrentPage());
                    HtmlMeta.addLink(request, "canonical", canonical);
                    HashSet<Locale> locales = new HashSet<>(ten.getImead().getLocales());
                    locales.add(Locale.getDefault());
                    for (Locale l : locales) {
                        if ("und".equals(l.toLanguageTag())) {
                            continue;
                        }
                        String base = ten.getImeadValue(SecurityRepository.BASE_URL);
                        if (l != Locale.getDefault() && !l.toLanguageTag().isEmpty()) {
                            base += l.toLanguageTag() + "/";
                        }
                        String langUrl = Categorizer.getUrl(base, catNameNull, f.getCurrentPage());
                        if (!langUrl.equals(canonical)) {
                            HtmlMeta.addLocaleURL(request, l, langUrl);
                        }
                    }

                    for (Article art : f.getArticles()) {
                        if (null != art.getImageurl()) {
                            HtmlMeta.addPropertyTag(request, "og:image", art.getImageurl());
                            break;
                        }
                    }
                    HtmlMeta.addPropertyTag(request, "og:description", description.toString());
                    HtmlMeta.addPropertyTag(request, "og:site_name", ten.getImead().getLocal(GramServlet.SITE_TITLE, resolvedLocales));
                    HtmlMeta.addPropertyTag(request, "og:type", "website");
                    JsonArrayBuilder itemList = Json.createArrayBuilder();
                    itemList.add(HtmlMeta.getLDBreadcrumb(ten.getImead().getLocal("page_title", resolvedLocales), 1, request.getAttribute(SecurityRepository.BASE_URL).toString()));
                    if (null == f.getCategory() && 1 == f.getCurrentPage() && null == request.getAttribute(Local.OVERRIDE_LOCALE_PARAM)) {
                        JsonObjectBuilder potentialAction = Json.createObjectBuilder().add("@type", "SearchAction").add("target", ten.getImeadValue(SecurityRepository.BASE_URL) + "search?searchTerm={search_term_string}").add("query-input", "required name=search_term_string");
                        JsonObjectBuilder search = Json.createObjectBuilder().add("@context", "https://schema.org").add("@type", "WebSite").add("url", ten.getImeadValue(SecurityRepository.BASE_URL)).add("potentialAction", potentialAction.build());
                        HtmlMeta.addLDJSON(request, search.build().toString());
                    } else if (null != f.getCategory() && null != catNameNull) {
                        itemList.add(HtmlMeta.getLDBreadcrumb(catName, 2, Categorizer.getUrl(request.getAttribute(SecurityRepository.BASE_URL).toString(), catNameNull, null)));
                    }
                    JsonObjectBuilder breadcrumbs = Json.createObjectBuilder().add("@context", "https://schema.org").add("@type", "BreadcrumbList").add("itemListElement", itemList.build());
                    HtmlMeta.addLDJSON(request, breadcrumbs.build().toString());
                }
                request.getServletContext().getRequestDispatcher(HOME_JSP).forward(request, response);
            }
        }
    }
}
