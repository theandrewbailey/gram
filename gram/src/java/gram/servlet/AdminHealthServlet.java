package gram.servlet;

import gram.AdminPermission;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.time.Instant;
import libWebsiteTools.turbo.CachedPage;
import libWebsiteTools.file.FileUtil;
import libWebsiteTools.file.Fileupload;
import libWebsiteTools.imead.Local;
import libWebsiteTools.security.CertPath;
import libWebsiteTools.security.CertUtil;
import libWebsiteTools.security.GuardFilter;
import libWebsiteTools.turbo.RequestTimer;
import libWebsiteTools.security.SecurityRepo;
import libWebsiteTools.tag.AbstractInput;
import libWebsiteTools.turbo.RequestTimes;
import gram.UtilStatic;
import gram.bean.SiteExporter;
import gram.bean.GramLandlord;
import gram.bean.database.Article;
import gram.bean.database.Comment;
import gram.rss.ErrorRss;
import gram.bean.GramTenant;
import gram.bean.SiteMilligramExporter;
import jakarta.ejb.EJB;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.util.concurrent.ForkJoinPool;
import javax.net.ssl.SSLContext;
import libWebsiteTools.Landlord;

/**
 *
 * @author alpha
 */
@WebServlet(name = "AdminHealth", description = "Show some vital stats about the server and blog", urlPatterns = {"/adminHealth"})
public class AdminHealthServlet extends AdminServlet {

    public static final String HEALTH_COMMANDS = "site_healthCommands";
    public static final String ADMIN_HEALTH = "/WEB-INF/admin/adminHealth.jsp";
    @EJB
    private Landlord landlord; // so that reload can call init() to instantiate other new tenants

    @Override
    public AdminPermission[] getRequiredPermissions() {
        return new AdminPermission[]{AdminPermission.Password.HEALTH};
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        GramTenant ten = GramLandlord.getTenant(request);
        String action = AbstractInput.getParameter(request, "action");
        if ("reload".equals(action)) {
            landlord.init();
            ten.reset();
            ten.isFirstTime();
            ten.getExec().submit(new SiteExporter(ten));
            request.getSession().invalidate();
            response.setHeader("Clear-Site-Data", "*");
            response.sendRedirect(request.getAttribute(SecurityRepo.BASE_URL).toString());
        } else if ("error".equals(action)) {
            response.sendRedirect(ten.getImeadValue(SecurityRepo.BASE_URL) + "rss/" + ErrorRss.NAME);
        } else {
            doGet(request, response);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        GramTenant ten = GramLandlord.getTenant(request);
        request.setAttribute("processes", ten.getExec().submit(() -> {
            LinkedHashMap<String, Future<String>> processes = new LinkedHashMap<>();
            for (String command : ten.getImeadValue(HEALTH_COMMANDS).split("\n")) {
                processes.put(command, ten.getExec().submit(() -> {
                    Instant start = Instant.now();
                    try {
                        return new String(FileUtil.runProcess(command.trim(), null, 1000));
                    } catch (IOException | RuntimeException t) {
                        return t.getLocalizedMessage();
                    } finally {
                        RequestTimer.addTiming(request, "command;desc=\"" + command.trim() + "\"", Duration.between(start, Instant.now()));
                    }
                }));
            }
            return processes;
        }));
        request.setAttribute("articles", ten.getExec().submit(() -> {
            Instant start = Instant.now();
            List<Article> arts = ten.getArts().getAll(null);
            RequestTimer.addTiming(request, "articleQuery", Duration.between(start, Instant.now()));
            return arts;
        }));
        request.setAttribute("comments", ten.getExec().submit(() -> {
            Instant start = Instant.now();
            List<Comment> comms = ten.getComms().getAll(null);
            RequestTimer.addTiming(request, "commentQuery", Duration.between(start, Instant.now()));
            return comms;
        }));
        request.setAttribute("files", ten.getExec().submit(() -> {
            Instant start = Instant.now();
            List<Fileupload> files = ten.getFile().getFileMetadata(null);
            RequestTimer.addTiming(request, "fileQuery", Duration.between(start, Instant.now()));
            return files;
        }));
        request.setAttribute("cached", ten.getExec().submit(() -> {
            Instant start = Instant.now();
            ArrayList<String> cached = new ArrayList<>();
            cached.add(UtilStatic.htmlFormat("Total hits: " + ten.getGlobalCache().getTotalHits(), false, false, true));
            ArrayList<CachedPage> pages = new ArrayList<>(ten.getGlobalCache().getAll(null).values());
            Collections.sort(pages, (page, other) -> {
                int difference = other.getHits() - page.getHits();
                if (0 == difference) {
                    long time = page.getCreated().toEpochSecond() - other.getCreated().toEpochSecond();
                    return time < 0 ? 1 : time > 0 ? -1 : 0;
                } else {
                    return difference; // reverse it, too
                }
            });
            try {
                for (CachedPage page : pages) {
                    Integer hits = page.getHits();
                    String key = page.getLookup() + "\nExpires: " + DateTimeFormatter.RFC_1123_DATE_TIME.format(page.getExpires()) + "\nHits: " + hits;
                    cached.add(UtilStatic.htmlFormat(key, false, false, true));
                }
            } catch (Exception ex) {
                System.out.println(ex.toString());
            }
            RequestTimer.addTiming(request, "cachedQuery", Duration.between(start, Instant.now()));
            return cached;
        }));
        request.setAttribute("performance", ten.getExec().submit(() -> {
            Map<String, List<RequestTimes>> perfs = ten.getPerfStats().getAll();
            Map<String, Map<String, String>> out = new LinkedHashMap<>(perfs.size() * 2);
            for (Map.Entry<String, List<RequestTimes>> e : perfs.entrySet()) {
                List<RequestTimes> reqtimes = e.getValue();
                Map<String, List<Long>> allValues = new LinkedHashMap<>(reqtimes.size() * 2);
                int count = 0;
                for (RequestTimes reqtime : reqtimes) {
                    if (reqtime.isCached()) {
                        continue;
                    }
                    ++count;
                    for (Map.Entry<String, Duration> time : reqtime.getTimings().entrySet()) {
                        List<Long> vals = allValues.get(time.getKey());
                        if (null == vals) {
                            vals = new ArrayList<>(reqtimes.size() * 2);
                            allValues.put(time.getKey(), vals);
                        }
                        vals.add(time.getValue().toNanos());
                    }
                }
                if (allValues.isEmpty()) {
                    continue;
                }
                Map<String, String> result = new LinkedHashMap<>(allValues.size() * 2);
                for (Map.Entry<String, List<Long>> vals : allValues.entrySet()) {
                    Collections.sort(vals.getValue());
                    List<Long> list = vals.getValue();
                    result.put(vals.getKey(), String.format("%.3f ms", list.get(list.size() / 2) / 1000000.0f));
                }
                out.put(e.getKey() + " (" + count + "×)", result);
            }
            return out;
        }));
        Map<X509Certificate, LinkedHashMap> certInfo = new HashMap<>();
        request.setAttribute("certInfo", certInfo);
        List<CertPath<X509Certificate>> certPaths = null;
        if (null != ten.getImeadValue(GuardFilter.CERTIFICATE_NAME)) {
            try {
                CertUtil certUtil = ten.getError().getCerts();
                certPaths = getCertInfos(certInfo, certUtil.getServerCertificateChain(ten.getImeadValue(GuardFilter.CERTIFICATE_NAME)));
            } catch (RuntimeException x) {
//                ten.getError().logException(request, "Certificate error", "building certificate chain", x);
            }
        }
        if (null == certPaths) {
            try {
                HttpClient.Builder hbuilder = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).executor(new ForkJoinPool());
                try {
                    SSLContext sx = SSLContext.getInstance("TLS");
                    sx.init(null, SiteMilligramExporter.TRUST_ALL, new SecureRandom());
                    hbuilder.sslContext(sx);
                } catch (KeyManagementException | NoSuchAlgorithmException gx) {
                    throw new RuntimeException(gx);
                }
                HttpClient hclient = hbuilder.build();
                URI url = new URI(ten.getImeadValue(SecurityRepo.BASE_URL));
                HttpRequest hreq = HttpRequest.newBuilder(url).GET().build();
                HttpResponse<String> hres = hclient.send(hreq, HttpResponse.BodyHandlers.ofString());
                Certificate[] certs = hres.sslSession().get().getPeerCertificates();
                X509Certificate primaryCert = (X509Certificate) certs[0];
                certPaths = getCertInfos(certInfo,
                        new CertUtil(certs).getServerCertificateChain(primaryCert.getSubjectX500Principal().getName()));
            } catch (Exception x) {
            }
        }
        if (null != certPaths) {
            request.setAttribute("certPaths", certPaths);
        }
        request.setAttribute("locales", Local.resolveLocales(ten.getImead(), request));
        request.getRequestDispatcher(ADMIN_HEALTH).forward(request, response);
    }

    public static List<CertPath<X509Certificate>> getCertInfos(Map<X509Certificate, LinkedHashMap> certInfo, List<CertPath<X509Certificate>> certPaths) {
        for (CertPath<X509Certificate> path : certPaths) {
            for (X509Certificate x509 : path.getCertificates()) {
                LinkedHashMap<String, String> cert = CertUtil.formatCert(x509);
                if (null != cert) {
                    certInfo.put(x509, cert);
                }
            }
        }
        return certPaths;
    }
}
