package libWebsiteTools.file;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URLDecoder;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoField;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jakarta.ejb.EJBException;
import jakarta.persistence.NoResultException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.core.HttpHeaders;
import java.time.Duration;
import java.time.Instant;
import libWebsiteTools.BaseServlet;
import libWebsiteTools.security.GuardFilter;
import libWebsiteTools.security.SecurityRepo;
import libWebsiteTools.JVMNotSupportedError;
import libWebsiteTools.turbo.CachedContent;
import libWebsiteTools.turbo.PageCache;
import libWebsiteTools.imead.IMEADHolder;
import libWebsiteTools.imead.Local;
import libWebsiteTools.turbo.RequestTimer;
import libWebsiteTools.Landlord;
import libWebsiteTools.Tenant;

public abstract class BaseFileServlet extends BaseServlet {

    // 0b1111111111111111111111100000000 == 0x7fffff00 == 2,147,483,392 ms == 24 days, 20 hours 31 minutes 23.392 seconds exactly
    public static final long MAX_AGE_MILLISECONDS = 0b1111111111111111111111100000000;
    public static final String MAX_AGE_SECONDS = Long.toString(MAX_AGE_MILLISECONDS / 1000);
    public static final long YEAR_SECONDS = 31535000;
    private static final String CROSS_SITE_REQUEST = "page_error_cross_site_request";
    // [ origin, timestamp for immutable requests (guaranteed null if not immutable), file path, query string ]
    private static final Pattern FILE_URL = Pattern.compile("^(.*?)file(?:Immutable/([^/]+))?/([^\\?]+)\\??(.*)?");

    public static String getNameFromURL(CharSequence URL) {
        try {
            Matcher m = FILE_URL.matcher(URLDecoder.decode(URL.toString(), "UTF-8"));
            if (!m.matches() || m.groupCount() < 3) {
                throw new NoResultException("Unable to get filename from " + URL);
            }
            return m.group(3);
        } catch (UnsupportedEncodingException ex) {
            throw new JVMNotSupportedError(ex);
        }
    }

    public static boolean isImmutableURL(CharSequence URL) {
        try {
            Matcher m = FILE_URL.matcher(URLDecoder.decode(URL.toString(), "UTF-8"));
            return m.matches() && m.groupCount() > 1 && null != m.group(2);
        } catch (UnsupportedEncodingException ex) {
            throw new JVMNotSupportedError(ex);
        }
    }

    public static String getImmutableURL(String baseURL, Fileupload f) {
        if (null == baseURL) {
            throw new IllegalArgumentException("base URL empty!");
        } else if (null == f) {
            throw new IllegalArgumentException("no file metadata!");
        } else if (null == f.getFilename() || "".equals(f.getFilename())) {
            throw new IllegalArgumentException("empty filename!");
        } else if (null == f.getAtime()) {
            throw new IllegalArgumentException("no modified time for " + f.getFilename());
        }
        return new StringBuilder(300).append(baseURL).append("fileImmutable/")
                .append(Base64.getUrlEncoder().encodeToString(BigInteger.valueOf(f.getAtime().toInstant().toEpochMilli()).toByteArray()))
                .append("/").append(f.getFilename()).toString();
    }

    @Override
    protected long getLastModified(HttpServletRequest request) {
        Tenant ten = Landlord.getTenant(request);
        Fileupload c;
        try {
            Instant start = Instant.now();
            c = ten.getFile().get(getNameFromURL(request.getRequestURL()));
            RequestTimer.addTiming(request, "query", Duration.between(start, Instant.now()));
            c.getAtime();
            request.setAttribute(BaseFileServlet.class.getCanonicalName(), c);
        } catch (Exception ex) {
            return -1;
        }
        return c.getAtime().withOffsetSameInstant(ZoneOffset.UTC).getLong(ChronoField.INSTANT_SECONDS);
    }

    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Matcher originMatcher = SecurityRepo.ORIGIN_PATTERN.matcher(request.getHeader("Origin"));
        Tenant ten = Landlord.getTenant(request);
        if (null == request.getHeader("Origin")
                || (null == request.getHeader("Access-Control-Request-Method")
                && null == request.getHeader("Access-Control-Request-Headers"))) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        if (IMEADHolder.matchesAny(request.getHeader("Origin"), ten.getImead().getPatterns(SecurityRepo.ALLOWED_ORIGINS)) && originMatcher.matches()) {
            response.setHeader("Access-Control-Allow-Origin", originMatcher.group(1));
        } else {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        response.setHeader("Access-Control-Request-Method", "GET");
        if (null != request.getHeader("Access-Control-Request-Headers")) {
            response.setHeader("Access-Control-Request-Headers", request.getHeader("Access-Control-Request-Headers"));
        }
        response.setHeader("Access-Control-Max-Age", MAX_AGE_SECONDS);
    }

    @Override
    protected void doHead(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        OffsetDateTime localNow = OffsetDateTime.now();
        Fileupload c = (Fileupload) request.getAttribute(BaseFileServlet.class.getCanonicalName());
        Tenant ten = Landlord.getTenant(request);
        if (null == c) {
            Instant start = Instant.now();
            try {
                String name = getNameFromURL(request.getRequestURL());
                c = ten.getFile().get(name);
                RequestTimer.addTiming(request, "query", Duration.between(start, Instant.now()));
                if (null == c) {
                    throw new FileNotFoundException(name);
                }
                request.setAttribute(BaseFileServlet.class.getCanonicalName(), c);
            } catch (FileNotFoundException | NoResultException ex) {
                response.setHeader(HttpHeaders.CACHE_CONTROL, "public, max-age=" + MAX_AGE_SECONDS);
                response.setDateHeader(HttpHeaders.EXPIRES, localNow.toInstant().toEpochMilli());
                if (HttpMethod.HEAD.equals(request.getMethod()) && fromApprovedDomain(request, ten.getImead().getPatterns(SecurityRepo.ALLOWED_ORIGINS))) {
                    request.setAttribute(GuardFilter.HANDLED_ERROR, true);
                }
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
        }
        if (!isAuthorized(request, c.getMimetype(), ten.getImead().getPatterns(SecurityRepo.ALLOWED_ORIGINS))) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            ten.getError().logException(request, null, ten.getImead().getLocal(CROSS_SITE_REQUEST, Local.resolveLocales(ten.getImead(), request)), null);
            request.setAttribute(GuardFilter.HANDLED_ERROR, true);
            return;
        }
        Matcher canonicalMatcher = SecurityRepo.ORIGIN_PATTERN.matcher(request.getAttribute(SecurityRepo.BASE_URL).toString());
        canonicalMatcher.matches();
        response.setHeader("Access-Control-Allow-Origin", canonicalMatcher.group(1));
        if (isImmutableURL(request.getRequestURL())) {
            response.setHeader(HttpHeaders.CACHE_CONTROL, "public, max-age=" + YEAR_SECONDS + ", immutable");
            response.setDateHeader(HttpHeaders.EXPIRES, localNow.plusYears(1).toInstant().toEpochMilli());
        } else {
            response.setHeader(HttpHeaders.CACHE_CONTROL, "public, max-age=" + MAX_AGE_SECONDS);
            response.setDateHeader(HttpHeaders.EXPIRES, localNow.toInstant().toEpochMilli() + MAX_AGE_MILLISECONDS);
        }
        response.setDateHeader(HttpHeaders.LAST_MODIFIED, c.getAtime().toInstant().toEpochMilli());
        response.setContentType(c.getMimetype());
        if (!c.getMimetype().startsWith("text")) {
            response.setCharacterEncoding(null);
        }
        CompressionSorter sorted = CompressionSorter.getInstance(c, request);
        request.setAttribute(CompressionSorter.class.getCanonicalName(), sorted);
        sorted.getOutputStream(response);
        String etag = "\"" + c.getEtag() + sorted.getType() + "\"";
        response.setHeader(HttpHeaders.ETAG, etag);
        if (etag.equals(request.getHeader(HttpHeaders.IF_NONE_MATCH))) {
            response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
        }
        response.setHeader("Accept-Ranges", "none");
        response.setHeader(HttpHeaders.VARY, HttpHeaders.ACCEPT_ENCODING);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doHead(request, response);
        Tenant ten = Landlord.getTenant(request);
        Fileupload c = (Fileupload) request.getAttribute(BaseFileServlet.class.getCanonicalName());
        if (HttpServletResponse.SC_OK == response.getStatus() && null != c) {
            CompressionSorter sorted = (CompressionSorter) request.getAttribute(CompressionSorter.class.getCanonicalName());
            response.setHeader(RequestTimer.SERVER_TIMING, RequestTimer.getTimingHeader(request, Boolean.FALSE));
            response.getOutputStream().write(sorted.getResult());
            PageCache global = ten.getGlobalCache().getCache(request, response);
            if (null != global) {
                global.put(PageCache.getLookup(ten.getImead(), request), new CachedContent(ten.getImead().getPatterns(SecurityRepo.ALLOWED_ORIGINS), response, sorted.getResult(), PageCache.getLookup(ten.getImead(), request)));
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Tenant ten = Landlord.getTenant(request);
        Instant start = Instant.now();
        try {
            List<Fileupload> uploadedfiles = FileUtil.getFilesFromRequest(request, "filedata");
            //boolean overwrite = AbstractInput.getParameter(request, "overwrite") != null;
            for (Fileupload uploadedfile : uploadedfiles) {
                if (null != ten.getFile().get(uploadedfile.getFilename())) {
                    request.setAttribute("ERROR_MESSAGE", "File exists: " + uploadedfile.getFilename());
                    return;
                }
                uploadedfile.setUrl(getImmutableURL(ten.getImeadValue(SecurityRepo.BASE_URL), uploadedfile));
            }
            ten.getFile().upsert(uploadedfiles);
            RequestTimer.addTiming(request, "save", Duration.between(start, Instant.now()));
            PageCache global = ten.getGlobalCache();
            for (Fileupload uploadedfile : uploadedfiles) {
                if (null != global) {
                    global.removeAll(global.searchLookups(uploadedfile.getFilename()));
                }
                FileCompressorJob.startAllJobs(ten, uploadedfile);
            }
            request.setAttribute("uploadedfiles", uploadedfiles);
        } catch (FileNotFoundException fx) {
            request.setAttribute("ERROR_MESSAGE", "File not sent");
        } catch (EJBException | IOException | ServletException ex) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    public static boolean isAuthorized(HttpServletRequest req, String mimetype, List<Pattern> acceptableDomains) {
        if (null == mimetype) {
            throw new IllegalArgumentException("No mimetype.");
        }
        if (mimetype.startsWith("image") || mimetype.startsWith("audio") || mimetype.startsWith("video")) {
            try {
                if (!fromApprovedDomain(req, acceptableDomains)) {
                    return false;
                }
            } catch (NullPointerException n) {
                // lucky
            }
        }
        return true;
    }

    public static boolean fromApprovedDomain(HttpServletRequest req, List<Pattern> acceptableDomains) {
        String referrer = req.getHeader("referer");
        if (null != referrer) {
            return IMEADHolder.matchesAny(referrer, acceptableDomains);
        }
        return true;
    }
}
