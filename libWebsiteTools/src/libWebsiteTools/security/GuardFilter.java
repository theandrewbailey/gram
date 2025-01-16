package libWebsiteTools.security;

import libWebsiteTools.turbo.RequestTimer;
import java.io.IOException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.ejb.EJB;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.core.HttpHeaders;
import libWebsiteTools.turbo.CachedPage;
import libWebsiteTools.turbo.PageCache;
import libWebsiteTools.imead.IMEADHolder;
import libWebsiteTools.imead.Local;
import libWebsiteTools.tag.AbstractInput;
import libWebsiteTools.Landlord;
import libWebsiteTools.Tenant;

@WebFilter(description = "DoS preventer (maybe) and reverse proxy", filterName = "GuardFilter", dispatcherTypes = {DispatcherType.REQUEST}, urlPatterns = {"/*"}, asyncSupported = true)
public class GuardFilter implements Filter {

    public static final String STRICT_TRANSPORT_SECURITY = "Strict-Transport-Security";
    public static final String KILLED_REQUEST = "$_LIBWEBSITETOOLS_KILLED_REQUEST";
    public static final String HANDLED_ERROR = "$_LIBWEBSITETOOLS_HANDLED_ERROR";
    public static final String CERTIFICATE_NAME = "site_security_certificateName";
    public static final String DEFAULT_REQUEST_ENCODING = "UTF-8";
    public static final String DEFAULT_RESPONSE_ENCODING = "UTF-8";
    public static final String VARY_HEADER = String.join(", ", new String[]{HttpHeaders.ACCEPT_ENCODING});
    private static final String VIA_HEADER = "site_via";
    private static final Logger LOG = Logger.getLogger(GuardFilter.class.getName());
    @EJB
    private Landlord landlord;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        OffsetDateTime localNow = RequestTimer.getStartTime(request);
        HttpServletRequest req = (HttpServletRequest) request;
        req.setCharacterEncoding(DEFAULT_REQUEST_ENCODING);
        HttpServletResponse res = (HttpServletResponse) response;
        response.setCharacterEncoding(DEFAULT_RESPONSE_ENCODING);
        res.setStatus(HttpServletResponse.SC_OK);
        Tenant ten = landlord.setTenant(req);
        AbstractInput.getTokenURL(req);
        // kill suspicious requests
        if (null == req.getSession(false) || req.getSession().isNew()) {
            if (ten.getError().inHoneypot(SecurityRepo.getIP(req))) {
                req.setAttribute(SpinnerServlet.REASON, "IP already in honeypot");
//                req.getRequestDispatcher("/spin").forward(request, response);
                return;
            }
            if (IMEADHolder.matchesAny(req.getRequestURL(), ten.getImead().getPatterns(SecurityRepo.HONEYPOTS))) {
                req.setAttribute(SpinnerServlet.REASON, "IP added to honeypot: Illegal URL");
//                req.getRequestDispatcher("/spin").forward(request, response);
                return;
            }
            String userAgent = req.getHeader("User-Agent");
            if (userAgent != null && IMEADHolder.matchesAny(userAgent, ten.getImead().getPatterns(SecurityRepo.DENIED_USER_AGENTS))) {
                req.setAttribute(SpinnerServlet.REASON, "IP added to honeypot: Illegal User-Agent");
//                req.getRequestDispatcher("/spin").forward(request, response);
                return;
            }
        }
        switch (req.getMethod().toUpperCase()) {
            case "GET":
            case "POST":
            case "HEAD":
            case "OPTIONS":
            case "PUT":
            case "DELETE":
                break;
            default:
                killInHoney(req, res);
                ten.getError().logException((HttpServletRequest) request, null, "IP added to honeypot: Illegal method", null);
                return;
        }
        // set variables and headers
        req.setAttribute(SecurityRepo.BASE_URL, ten.getImeadValue(SecurityRepo.BASE_URL));
        res.setDateHeader(HttpHeaders.DATE, localNow.toInstant().toEpochMilli());
        res.setHeader("X-Content-Type-Options", "nosniff");
        res.setHeader(HttpHeaders.VARY, VARY_HEADER);
        CertUtil certs = ten.getError().getCerts();
        if (isSecure(req) && null != certs.getSubject()) {
            try {
                certs.getSubject().checkValidity();
                OffsetDateTime expInstant = certs.getCertExpDate().toInstant().atOffset(ZoneOffset.UTC);
                if (localNow.isBefore(expInstant)) {
                    Duration d = Duration.between(localNow, expInstant).abs();
                    res.setHeader(STRICT_TRANSPORT_SECURITY, "max-age=" + d.getSeconds() + "; includeSubDomains");
                }
            } catch (CertificateExpiredException | CertificateNotYetValidException | RuntimeException ex) {
                ten.getError().logException(req, "High security misconfigured", null, ex);
                try {
                    certs.verifyCertificate(ten.getImeadValue(CERTIFICATE_NAME));
                } catch (Exception rx) {
                }
            }
        }
        if (null == res.getHeader(HttpHeaders.CACHE_CONTROL)) {
            res.setHeader(HttpHeaders.CACHE_CONTROL, "public, max-age=99999, s-maxage=99");
            res.setDateHeader(HttpHeaders.EXPIRES, localNow.plusSeconds(99999).toInstant().toEpochMilli());
        }
        // set request language
        String forwardURL = null;
        Locale selected = Local.resolveLocales(ten.getImead(), req).get(0);
        String servletPath = req.getServletPath();
        if (null != selected && !Locale.ROOT.equals(selected) && ten.getImead().getLocales().contains(selected)) {
            req.setAttribute(SecurityRepo.BASE_URL, ten.getImeadValue(SecurityRepo.BASE_URL) + selected.toString() + "/");
            String rootPath = "/" + selected.toString();
            if (!servletPath.startsWith(rootPath + "/") && !servletPath.equals(rootPath)) {
                forwardURL = req.getContextPath() + "/" + selected.toString() + req.getRequestURI().substring(req.getContextPath().length());
                if (null != req.getQueryString()) {
                    forwardURL += "?" + req.getQueryString();
                }
                res.sendRedirect(forwardURL);
                return;
            }
            forwardURL = req.getRequestURI().substring(req.getContextPath().length() + selected.toString().length() + 1);
            if (forwardURL.equals("")) {
                forwardURL = "/";
            }
            if (null != req.getQueryString()) {
                forwardURL += "?" + req.getQueryString();
            }
        }
        // carry on
        try {
            if (!reverseProxy(ten, req, res)) {
                if (null != forwardURL) {
                    req.getServletContext().getRequestDispatcher(forwardURL).forward(request, response);
                } else {
                    chain.doFilter(req, res);
                }
            }
            if (res.getStatus() >= 400 && req.getAttribute(HANDLED_ERROR) == null) {
                ten.getError().logException(req, null, "HTTP " + res.getStatus(), null);
            }
            res.flushBuffer();
        } catch (IOException | ServletException x) {
            LOG.log(Level.SEVERE, "Exception caught in GuardFilter", x);
            ten.getError().logException(req, null, null, x);
        }
    }

    public static boolean isSecure(HttpServletRequest req) {
        return req.isSecure() || "https".equals(req.getHeader("x-forwarded-proto"));
    }

    public boolean killInHoney(HttpServletRequest req, HttpServletResponse res) {
        kill(req, res);
        return Landlord.getTenant(req).getError().putInHoneypot(SecurityRepo.getIP(req));
    }

    public static void kill(ServletRequest request, ServletResponse response) {
        request.setAttribute(KILLED_REQUEST, KILLED_REQUEST);
        try {
            request.getInputStream().close();
            response.getOutputStream().close();
        } catch (IOException ex) {
            // I dont give a flip
        }
    }

    /**
     * Look up response from cache, and if found, write to response. Bypass with
     * nocache" URL query parameter.
     *
     * @param beans
     * @param req
     * @param res
     * @return if something was found and written to response
     * @throws IOException
     */
    public static boolean reverseProxy(Tenant ten, HttpServletRequest req, HttpServletResponse res) throws IOException {
        if (null != req.getParameter("nocache")) {
            res.setHeader(HttpHeaders.ETAG, PageCache.getETag(ten.getImead(), req));
            return false;
        }
        String lookup = PageCache.getLookup(ten.getImead(), req);
        String ifNoneMatch = req.getHeader(HttpHeaders.IF_NONE_MATCH);
        if (null == ifNoneMatch) {
            ifNoneMatch = "";
        }
        CachedPage page;
        switch (req.getMethod()) {
            case HttpMethod.GET:
                page = ten.getGlobalCache().get(lookup);
                if (null != page && page.isApplicable(req)) {
                    writeHeaders(ten.getImead(), res, page);
                    if (ifNoneMatch.equals(page.getHeader(HttpHeaders.ETAG)) || ifNoneMatch.equals(PageCache.getETag(ten.getImead(), req))) {
                        res.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                        return true;
                    }
                    res.setHeader(RequestTimer.SERVER_TIMING, RequestTimer.getTimingHeader(req, Boolean.TRUE));
                    ServletOutputStream out = res.getOutputStream();
                    out.write(page.getBody());
                    out.flush();
                    res.flushBuffer();
                    page.hit();
                    ten.getGlobalCache().incrementTotalHit();
                    return true;
                }
                res.setHeader(HttpHeaders.ETAG, PageCache.getETag(ten.getImead(), req));
                break;
            case HttpMethod.HEAD:
                page = ten.getGlobalCache().get(lookup);
                if (null != page && page.isApplicable(req)) {
                    if (ifNoneMatch.equals(PageCache.getETag(ten.getImead(), req))) {
                        res.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                    }
                    writeHeaders(ten.getImead(), res, page);
                    res.setHeader(RequestTimer.SERVER_TIMING, RequestTimer.getTimingHeader(req, Boolean.TRUE));
                    page.hit();
                    ten.getGlobalCache().incrementTotalHit();
                    return true;
                }
                res.setHeader(HttpHeaders.ETAG, PageCache.getETag(ten.getImead(), req));
                break;
            default:
                break;
        }
        return false;
    }

    private static void writeHeaders(IMEADHolder imead, HttpServletResponse res, CachedPage page) {
        res.setHeader("Via", imead.getValue(VIA_HEADER));
        res.setStatus(page.getStatus());
        if (null != page.getContentType()) {
            res.setContentType(page.getContentType());
        }
        res.setContentLength(page.getBody().length);
        for (Map.Entry<String, String> field : page.getHeaders().entrySet()) {
            res.setHeader(field.getKey(), field.getValue());
        }
    }

    @Override
    public void destroy() {
    }
}
