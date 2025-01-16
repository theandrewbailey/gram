package gram.servlet;

import gram.AdminPermission;
import gram.UtilStatic;
import gram.bean.BackupDaemon;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.HttpHeaders;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.zip.ZipOutputStream;
import libWebsiteTools.security.SecurityRepo;
import gram.tag.ArticleUrl;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import gram.bean.GramTenant;
import gram.IndexFetcher;
import gram.bean.GramLandlord;
import gram.bean.database.Article;
import gram.bean.database.Section;
import gram.tag.Categorizer;
import jakarta.servlet.ServletOutputStream;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;

/**
 *
 * @author alpha
 */
@WebServlet(name = "AdminMilligram", description = "Static site generator, output in zip form.", urlPatterns = {"/mg"})
public class AdminMilligram extends AdminServlet {

    private static int[] ERROR_CODES = new int[]{400, 401, 403, 404, 405, 422, 500, 501};
    private static final Logger LOG = Logger.getLogger(AdminMilligram.class.getName());

    @Override
    public AdminPermission[] getRequiredPermissions() {
        return new AdminPermission[]{AdminPermission.Password.IMPORT_EXPORT, AdminPermission.Password.EDIT_POSTS};
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        GramTenant ten = GramLandlord.getTenant(request);
        HttpClient.Builder hbuilder = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).executor(new ForkJoinPool());
        try {
            SSLContext sx = SSLContext.getInstance("TLS");
            sx.init(null, TRUST_ALL, new SecureRandom());
            hbuilder.sslContext(sx);
        } catch (KeyManagementException | NoSuchAlgorithmException gx) {
            throw new RuntimeException(gx);
        }
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + ten.getBackup().getZipStem() + OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd'-ssg.zip'")));
        response.setContentType("application/zip");
        ServletOutputStream ttr = response.getOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(ttr)) {
            Semaphore rateLimiter = new Semaphore(Runtime.getRuntime().availableProcessors() * 2);
            HttpClient hclient = hbuilder.build();
            Queue<Future> tasks = new ConcurrentLinkedQueue<>();
            tasks.add(ten.getExec().submit(() -> {
                getLocale(ten, "", hclient, rateLimiter, zip);
            }));
            tasks.add(ten.getExec().submit(() -> {
                ten.getFile().processArchive((f) -> {
                    try {
                        BackupDaemon.addFileToZip(zip, "file/" + f.getFilename(), f.getMimetype(), f.getAtime(), f.getFiledata());
                    } catch (IOException ix) {
                        throw new RuntimeException(ix);
                    }
                }, false);
            }));
            // TODO: fix alternate locales
//            ArrayList<Locale> locales = new ArrayList<>(ten.getImead().getLocales());
//            locales.remove(Locale.ROOT);
//            for (Locale l : locales) {
//                bigTasks.add(ten.getExec().submit(() -> {
//                    getLocale(ten, l.toLanguageTag() + "/", hclient, rateLimiter, zip);
//                }));
//            }
            UtilStatic.finish(tasks);
        }
    }

    private static Queue<CompletableFuture> getLocale(GramTenant ten, String locale, HttpClient hclient, Semaphore rateLimiter, ZipOutputStream zip) {
        final BlockingQueue<CompletableFuture> tasks = new LinkedBlockingQueue<>();
        tasks.addAll(getAllPagesOfCategory(ten, locale, null, hclient, rateLimiter, zip));
        // Repository.processArchive() causes thread deadlock (I think)
        for (Article a : ten.getArts().getAll(null)) {
            String url = ArticleUrl.getUrl(ten.getImeadValue(SecurityRepo.BASE_URL) + locale, a, null);
            String zipPath = ArticleUrl.getUrl(locale, a, null);
            tasks.add(milligramAddPageToZip(ten, locale, hclient, rateLimiter, zip, zipPath, url));
        }
        for (Section s : ten.getSects().getAll(null)) {
            tasks.addAll(getAllPagesOfCategory(ten, locale, s.getName(), hclient, rateLimiter, zip));
        }
//        ten.getArts().processArchive((a) -> {
//            String url = ArticleUrl.getUrl(ten.getImeadValue(SecurityRepo.BASE_URL) + locale, a, null);
//            String zipPath = ArticleUrl.getUrl(locale, a, null);
//            tasks.add(milligramAddPageToZip(ten, locale, hclient, rateLimiter, zip, zipPath, url));
//        }, Boolean.FALSE);
//        ten.getSects().processArchive((s) -> {
//            tasks.addAll(getAllPagesOfCategory(ten, locale, s.getName(), hclient, rateLimiter, zip));
//        }, Boolean.FALSE);
        for (int e : ERROR_CODES) {
            tasks.add(milligramAddPageToZip(ten, locale, hclient, rateLimiter, zip, e + ".html", ten.getImeadValue(SecurityRepo.BASE_URL) + locale + "coroner/" + e));
        }
        UtilStatic.join(tasks);
        return tasks;
    }

    private static Queue<CompletableFuture> getAllPagesOfCategory(GramTenant ten, String locale, String category, HttpClient hclient, Semaphore rateLimiter, ZipOutputStream zip) {
        final Queue<CompletableFuture> tasks = new ConcurrentLinkedQueue<>();
        IndexFetcher f = new IndexFetcher(ten, Categorizer.getUrl("/", category, 1));
        String baseURL = ten.getImeadValue(SecurityRepo.BASE_URL) + locale;
        for (int p = 1; p <= f.getCount(); p++) {
            tasks.add(milligramAddPageToZip(ten, locale, hclient, rateLimiter, zip, Categorizer.getUrl("", category, p), Categorizer.getUrl(baseURL, category, p)));
        }
        return tasks;
    }

    private static CompletableFuture milligramAddPageToZip(GramTenant ten, String locale, HttpClient hclient, Semaphore rateLimiter, ZipOutputStream zip, String zipPath, String url) {
        String baseURL = ten.getImeadValue(SecurityRepo.BASE_URL);
        URI requestURL = URI.create(url + locale + "?nocache&milligram");
        HttpRequest hreq = HttpRequest.newBuilder(requestURL).GET().build();
        String goToRoot = "../".repeat(zipPath.length() - zipPath.replace("/", "").length());
        while (true) {
            try {
                rateLimiter.acquire();
                break;
            } catch (InterruptedException px) {
                throw new RuntimeException(px);
            }
        }
        return hclient.sendAsync(hreq, HttpResponse.BodyHandlers.ofString()).thenAccept((t) -> {
            LOG.log(Level.FINEST, "Milligram received: {0}", requestURL);
            rateLimiter.release();
            try {
                String body = t.body().replaceAll("<base href=\".*?\"/>", "").replaceAll(baseURL, "");
                body = body.replaceAll("(fileImmutable/\\w+)", goToRoot + "file");
                body = body.replaceAll(" integrity=\"sha256-(?:[A-Za-z0-9]|[+/])+={0,2}\"", "");
                body = body.replaceAll(" href=\"article/", " href=\"" + goToRoot + "article/").replaceAll(" href=\"index/", " href=\"" + goToRoot + "index/");
                byte[] bodybytes = body.getBytes("UTF-8");
                BackupDaemon.addFileToZip(zip, zipPath, "text/html", OffsetDateTime.now(), bodybytes);
            } catch (IOException ix) {
                throw new RuntimeException(ix);
            }
        });
    }

    private static final TrustManager[] TRUST_ALL = {new X509ExtendedTrustManager() {
        @Override
        public void checkClientTrusted(X509Certificate[] xcs, String string, Socket socket) throws CertificateException {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] xcs, String string, Socket socket) throws CertificateException {
        }

        @Override
        public void checkClientTrusted(X509Certificate[] xcs, String string, SSLEngine ssle) throws CertificateException {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] xcs, String string, SSLEngine ssle) throws CertificateException {
        }

        @Override
        public void checkClientTrusted(X509Certificate[] xcs, String string) throws CertificateException {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] xcs, String string) throws CertificateException {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }};
}
