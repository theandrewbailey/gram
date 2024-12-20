package libWebsiteTools.turbo;

import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.HttpHeaders;
import libWebsiteTools.AllBeanAccess;
import libWebsiteTools.imead.Local;
import libWebsiteTools.imead.LocalizedStringNotFoundException;
import libWebsiteTools.rss.FeedBucket;
import libWebsiteTools.security.SecurityRepo;
import libWebsiteTools.tag.HtmlMeta;
import libWebsiteTools.tag.HtmlScript;
import libWebsiteTools.tag.HtmlTime;
import libWebsiteTools.tag.StyleSheet;

/**
 *
 * @author alpha
 */
@WebFilter(description = "Adds security headers and potentially adds to cache.", filterName = "JspFilter", dispatcherTypes = {DispatcherType.REQUEST, DispatcherType.FORWARD}, urlPatterns = {"*.jsp"})
public class JspFilter implements Filter {

    //public static final String CONTENT_SECURITY_POLICY = "site_security_csp";
    public static final String FEATURE_POLICY = "site_security_features";
    public static final String PERMISSIONS_POLICY = "site_security_permissions";
    public static final String REFERRER_POLICY = "site_security_referrer";
    public static final String PRIMARY_LOCALE_PARAM = "$_LIBIMEAD_PRIMARY_LOCALE";
    public static final String CONTENT_SECURITY_POLICY = "Content-Security-Policy";
    public static final String REPORTING_ENDPOINTS = "Reporting-Endpoints";
    public static final String VARY_HEADER = String.join(", ", new String[]{
        HttpHeaders.ACCEPT_ENCODING, HttpHeaders.ACCEPT_LANGUAGE});
//    private static final String CSP_TEMPLATE = "default-src 'self'; img-src 'self' data:; font-src data:; object-src 'none'; form-action 'self'; frame-ancestors 'self'; base-uri 'self'; upgrade-insecure-requests; script-src %s; style-src %s; report-uri %sreport; report-to cspend;";
    private static final String CSP_TEMPLATE = "default-src 'self'; img-src 'self' data:; font-src data:; object-src 'none'; form-action 'self'; frame-ancestors 'self'; base-uri 'self'; upgrade-insecure-requests; script-src %s; style-src %s; report-uri %sreport;";
    private static final String REPORTING_TEMPLATE = "cspend=\"%sreport\"";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = ((HttpServletRequest) request);
        HttpServletResponse res = (HttpServletResponse) response;
        AllBeanAccess beans = (AllBeanAccess) req.getAttribute(AllBeanAccess.class.getCanonicalName());
        Locale primaryLocale = Local.resolveLocales(beans.getImead(), req).get(0);
        if (Locale.ROOT.equals(primaryLocale)) {
            primaryLocale = Locale.getDefault();
        }
        request.setAttribute(PRIMARY_LOCALE_PARAM, primaryLocale);
        HtmlMeta.addNameTag(req, "viewport", beans.getImeadValue("site_viewport"));
        HtmlMeta.addLink(req, "shortcut icon", beans.getImeadValue("site_favicon"));
        try {
            String icon = beans.getImeadValue("site_appleTouchIcon");
            if (null != icon) {
                HtmlMeta.addLink(req, "apple-touch-icon", beans.getFile().getFileMetadata(Arrays.asList(icon)).get(0).getUrl());
            }
        } catch (Exception x) {
        }
        try {
            String theme = beans.getImeadValue("site_themeColor");
            if (null != theme) {
                HtmlMeta.addNameTag(req, "theme-color", theme);
            }
        } catch (Exception x) {
        }
        res.setHeader("Accept-Ranges", "none");
        res.setHeader(HttpHeaders.VARY, VARY_HEADER);
        res.setHeader(HttpHeaders.CONTENT_LANGUAGE, primaryLocale.toLanguageTag());
        if (null != beans.getImeadValue(FEATURE_POLICY)) {
            res.setHeader("Feature-Policy", beans.getImeadValue(FEATURE_POLICY));
        }
        if (null != beans.getImeadValue(PERMISSIONS_POLICY)) {
            res.setHeader("Permissions-Policy", beans.getImeadValue(PERMISSIONS_POLICY));
        }
        if (null != beans.getImeadValue(REFERRER_POLICY)) {
            res.setHeader("Referrer-Policy", beans.getImeadValue(REFERRER_POLICY));
        }
        // unnecessary for modern browsers
        //res.setHeader("X-Frame-Options", "SAMEORIGIN");
        //res.setHeader("X-Xss-Protection", "1; mode=block");
        if (null == request.getAttribute(HtmlTime.FORMAT_VAR)) {
            try {
                request.setAttribute(HtmlTime.FORMAT_VAR, beans.getImead().getLocal(HtmlTime.SITE_DATEFORMAT_LONG, Local.resolveLocales(beans.getImead(), (HttpServletRequest) request)));
            } catch (LocalizedStringNotFoundException lx) {
                request.setAttribute(HtmlTime.FORMAT_VAR, FeedBucket.TIME_FORMAT);
            }
        }
        PageCache cache = beans.getGlobalCache().getCache(req, res);
        CachedPage page = capturePage(chain, req, res);
        if (null != cache) {
            cache.put(PageCache.getLookup(beans.getImead(), req), page);
        }
        res.flushBuffer();
    }

    private CachedPage capturePage(FilterChain chain, HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        AllBeanAccess beans = (AllBeanAccess) req.getAttribute(AllBeanAccess.class.getCanonicalName());
        String etag = res.getHeader(HttpHeaders.ETAG);
        if (null == etag) {
            etag = PageCache.getETag(beans.getImead(), req);
            res.setHeader(HttpHeaders.ETAG, etag);
        }
        RequestTimer.getFrontTime(req);
        try (CompressedServletWrapper wrap = CompressedServletWrapper.getInstance(req, res)) {
            chain.doFilter(req, wrap);
            // csp calculation probably shouldn't be here...
//            String stylesheetHashes = String.join(" ", StyleSheet.getHashes(req));
//            if (stylesheetHashes.isEmpty()) {
//                stylesheetHashes = "'none'";
//            }
            String stylesheetHashes = "'self'";
            String scriptHashes = String.join(" ", HtmlScript.getHashes(req));
            if (scriptHashes.isEmpty()) {
                scriptHashes = "'none'";
            }
//            String scriptHashes = "'self'";
            String csp = String.format(CSP_TEMPLATE, scriptHashes, stylesheetHashes, beans.getImeadValue(SecurityRepo.BASE_URL));
            res.setHeader(CONTENT_SECURITY_POLICY, csp);
//            res.setHeader(REPORTING_ENDPOINTS, String.format(REPORTING_TEMPLATE, beans.getImeadValue(SecurityRepo.BASE_URL)));
            wrap.flushBuffer();
            byte[] responseBytes = wrap.getOutputStream().getResult();
            res.setHeader(RequestTimer.SERVER_TIMING, RequestTimer.getTimingHeader(req, Boolean.FALSE));
            res.setContentLength(responseBytes.length);
            wrap.getOutputStream().setResult(res, responseBytes);
            return new CachedPage(res, responseBytes, PageCache.getLookup(beans.getImead(), req));
        }
    }
}
