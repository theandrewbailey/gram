package libWebsiteTools.security;

import libWebsiteTools.turbo.RequestTimer;
import java.io.IOException;
import java.time.OffsetDateTime;
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
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import libWebsiteTools.BaseServlet;
import libWebsiteTools.turbo.CachedPage;
import libWebsiteTools.turbo.PageCache;
import libWebsiteTools.imead.IMEADHolder;
import libWebsiteTools.imead.Local;
import libWebsiteTools.tag.AbstractInput;
import libWebsiteTools.Landlord;
import libWebsiteTools.Tenant;
import libWebsiteTools.imead.IMEADRepository;
import libWebsiteTools.turbo.CompressedOutput;

@WebFilter(description = "DoS preventer (maybe) and reverse proxy", filterName = "GuardFilter", dispatcherTypes = {DispatcherType.REQUEST}, urlPatterns = {"/*"}, asyncSupported = true)
public class GuardFilter implements Filter {

    public static final String STRICT_TRANSPORT_SECURITY = "Strict-Transport-Security";
    public static final String KILLED_REQUEST = "$_LIBWEBSITETOOLS_KILLED_REQUEST";
    public static final String HANDLED_ERROR = "$_LIBWEBSITETOOLS_HANDLED_ERROR";
    public static final String CERTIFICATE_NAME = "site_security_certificateName";
    public static final String BOMB_REDIRECT = "site_bombRedirect";
    public static final String DEFAULT_REQUEST_ENCODING = "UTF-8";
    public static final String DEFAULT_RESPONSE_ENCODING = "UTF-8";
    public static final String VARY_HEADER = String.join(", ", new String[]{HttpHeaders.ACCEPT_ENCODING});
    private static final String VIA_HEADER = "site_via";
    private static final Logger LOG = Logger.getLogger(GuardFilter.class.getName());
    @EJB
    private Landlord landlord;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        RequestTimer.getStartTime(request);
        HttpServletRequest req = (HttpServletRequest) request;
        req.setCharacterEncoding(DEFAULT_REQUEST_ENCODING);
        HttpServletResponse res = (HttpServletResponse) response;
        response.setCharacterEncoding(DEFAULT_RESPONSE_ENCODING);
        res.setStatus(HttpServletResponse.SC_OK);
        Tenant ten = landlord.setTenant(req);
        AbstractInput.getTokenURL(req);

        if (isSuspiciousRequest(ten, req, res)) {
            return;
        }

        // carry on
        String forwardURL = setVars(ten, req, res);
        try {
            if (!reverseProxy(ten, req, res)) {
                if (null != forwardURL) {
                    req.getServletContext().getRequestDispatcher(forwardURL).forward(request, response);
                } else {
                    chain.doFilter(req, res);
                }
            }
            if (res.getStatus() >= 400 && req.getAttribute(HANDLED_ERROR) == null) {
                int status = res.getStatus();
                if (404 == status && IMEADHolder.matchesAny(AbstractInput.getTokenURL(req), ten.getImead().getPatterns(BaseServlet.SITE_404_TO_410))) {
                    res.setStatus(410);
                }
                ten.getError().logException(req, null, "HTTP " + res.getStatus(), null);
            }
            res.flushBuffer();
        } catch (ServletException x) {
            LOG.log(Level.SEVERE, "Exception caught in GuardFilter", x);
            ten.getError().logException(req, null, null, x);
        }
    }

    /**
     * Check request for some known traits of a suspicious or malicious request,
     * and, if so, close request.
     *
     * @param ten Tenant
     * @param req HttpServletRequest
     * @param res HttpServletResponse
     * @return if request was closed.
     */
    private static boolean isSuspiciousRequest(Tenant ten, HttpServletRequest req, HttpServletResponse res) {
        switch (req.getMethod().toUpperCase()) {
            case "GET":
            case "POST":
            case "HEAD":
            case "OPTIONS":
                break;
            case "PUT":
            case "DELETE":
            default:
                res.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                killInHoney(req, res);
                ten.getError().logException(req, null, "IP added to honeypot: Illegal method", null);
                return true;
        }
        if (null == req.getSession(false) || req.getSession().isNew()) {
            if (ten.getError().inHoneypot(SecurityRepository.getIP(req))) {
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                ten.getError().logException(req, null, "IP already in honeypot, bombing...", null);
                zipBomb(req, res);
                killInHoney(req, res);
                return true;
            }
            if (IMEADHolder.matchesAny(req.getRequestURL(), ten.getImead().getPatterns(SecurityRepository.HONEYPOTS))) {
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                ten.getError().logException(req, null, "IP added to honeypot: Illegal URL", null);
                killInHoney(req, res);
                return true;
            }
            String userAgent = req.getHeader("User-Agent");
            if (userAgent != null && IMEADHolder.matchesAny(userAgent, ten.getImead().getPatterns(SecurityRepository.DENIED_USER_AGENTS))) {
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                ten.getError().logException(req, null, "IP added to honeypot: Illegal User-Agent", null);
                killInHoney(req, res);
                return true;
            }
            String language = req.getHeader("Accept-Language");
            if (language != null && IMEADHolder.matchesAny(language, ten.getImead().getPatterns(SecurityRepository.DENIED_LANGUAGES))) {
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                ten.getError().logException(req, null, "IP added to honeypot: Illegal Accept-Language", null);
                killInHoney(req, res);
                return true;
            }
            String cookies = req.getHeader("Cookie");
            if (cookies != null && IMEADHolder.matchesAny(cookies, ten.getImead().getPatterns(SecurityRepository.DENIED_COOKIES))) {
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                ten.getError().logException(req, null, "IP added to honeypot: Illegal Cookie", null);
                killInHoney(req, res);
                return true;
            }
        }
        return false;
    }

    /**
     *
     * @param ten Tenant
     * @param req HttpServletRequest
     * @param res HttpServletResponse
     * @return if the request should be forwarded to another URL, forward to
     * this URL
     * @throws IOException
     */
    private static String setVars(Tenant ten, HttpServletRequest req, HttpServletResponse res) throws IOException {
        OffsetDateTime localNow = RequestTimer.getStartTime(req);
        req.setAttribute(SecurityRepository.BASE_URL, ten.getImeadValue(SecurityRepository.BASE_URL));
        res.setDateHeader(HttpHeaders.DATE, localNow.toInstant().toEpochMilli());
        res.setHeader("X-Content-Type-Options", "nosniff");
        res.setHeader(HttpHeaders.VARY, VARY_HEADER);
//        CertUtil certs = ten.getError().getCerts();
//        if (isSecure(req) && null != certs.getSubject()) {
//            try {
//                certs.getSubject().checkValidity();
//                OffsetDateTime expInstant = certs.getCertExpDate().toInstant().atOffset(ZoneOffset.UTC);
//                if (localNow.isBefore(expInstant)) {
//                    Duration d = Duration.between(localNow, expInstant).abs();
//                    res.setHeader(STRICT_TRANSPORT_SECURITY, "max-age=" + d.getSeconds() + "; includeSubDomains");
//                }
//            } catch (CertificateExpiredException | CertificateNotYetValidException | RuntimeException ex) {
//                ten.getError().logException(req, "High security misconfigured", null, ex);
//                try {
//                    certs.verifyCertificate(ten.getImeadValue(CERTIFICATE_NAME));
//                } catch (Exception rx) {
//                }
//            }
//        }
        if (null == res.getHeader(HttpHeaders.CACHE_CONTROL)) {
            res.setHeader(HttpHeaders.CACHE_CONTROL, "public, max-age=99999, s-maxage=99");
            res.setDateHeader(HttpHeaders.EXPIRES, localNow.plusSeconds(99999).toInstant().toEpochMilli());
        }
        String forwardURL = null;
        Locale selected = Local.resolveLocales(ten.getImead(), req).get(0);
        String servletPath = req.getServletPath();
        if (null != selected && !Locale.ROOT.equals(selected) && ten.getImead().getLocales().contains(selected)) {
            req.setAttribute(SecurityRepository.BASE_URL, ten.getImeadValue(SecurityRepository.BASE_URL) + selected.toLanguageTag() + "/");
            String rootPath = "/" + selected.toLanguageTag();
            if (!servletPath.startsWith(rootPath + "/") && !servletPath.equals(rootPath)) {
                forwardURL = req.getContextPath() + "/" + selected.toLanguageTag() + req.getRequestURI().substring(req.getContextPath().length());
                if (null != req.getQueryString()) {
                    forwardURL += "?" + req.getQueryString();
                }
                res.sendRedirect(forwardURL);
                return forwardURL;
            }
            forwardURL = req.getRequestURI().substring(req.getContextPath().length() + selected.toLanguageTag().length() + 1);
            if (forwardURL.equals("")) {
                forwardURL = "/";
            }
            if (null != req.getQueryString()) {
                forwardURL += "?" + req.getQueryString();
            }
        }
        return forwardURL;
    }

    /**
     *
     * @param req HttpServletRequest
     * @return if HttpServletRequest.isSecure(), or if "x-forwarded-proto:
     * https" header present
     */
    public static boolean isSecure(HttpServletRequest req) {
        return req.isSecure() || "https".equals(req.getHeader("x-forwarded-proto"));
    }

    private static boolean killInHoney(HttpServletRequest req, HttpServletResponse res) {
        kill(req, res);
        return Landlord.getTenant(req).getError().putInHoneypot(SecurityRepository.getIP(req));
    }

    public static boolean zipBomb(HttpServletRequest req, HttpServletResponse res) {
        Tenant ten = Landlord.getTenant(req);
        CompressedOutput compressAlgorithm = CompressedOutput.getInstance(req);
        File zstdBomb = new File(ten.getImeadValue("site_backup") + "bomb.zstd");
        File gzipBomb = new File(ten.getImeadValue("site_backup") + "bomb.gzip");
        try {
            if (CompressedOutput.ZSTD.equals(compressAlgorithm) && zstdBomb.exists()) {
                res.setHeader(HttpHeaders.CONTENT_ENCODING, "zstd");
                sendFile(res, zstdBomb);
                return true;
            } else if (CompressedOutput.GZIP.equals(compressAlgorithm) && gzipBomb.exists()) {
                res.setHeader(HttpHeaders.CONTENT_ENCODING, "gzip");
                sendFile(res, gzipBomb);
                return true;
            } else if (null != ten.getImeadValue(BOMB_REDIRECT)) {
                BaseServlet.permaMove(res, ten.getImeadValue(BOMB_REDIRECT));
            }
        } catch (IOException x) {
            return false;
        }
        return false;
    }

    private static void sendFile(HttpServletResponse res, File bomb) throws IOException {
        OutputStream out = res.getOutputStream();
        FileInputStream in = new FileInputStream(bomb);
        byte[] buffer = new byte[65535];
        int length;
        while ((length = in.read(buffer)) > 0) {
            out.write(buffer, 0, length);
        }
        in.close();
        out.flush();
    }

    private static void kill(ServletRequest request, ServletResponse response) {
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
     * @param ten
     * @param req
     * @param res
     * @return if something was found and written to response
     * @throws IOException
     */
    private static boolean reverseProxy(Tenant ten, HttpServletRequest req, HttpServletResponse res) throws IOException {
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

    private static void writeHeaders(IMEADRepository imead, HttpServletResponse res, CachedPage page) {
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
