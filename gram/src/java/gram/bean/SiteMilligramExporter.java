package gram.bean;

import gram.CategoryFetcher;
import gram.UtilStatic;
import gram.bean.database.Article;
import gram.bean.database.Section;
import gram.rss.ArticleRss;
import gram.rss.CommentRss;
import gram.tag.ArticleUrl;
import gram.tag.Categorizer;
import jakarta.ws.rs.core.HttpHeaders;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipOutputStream;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;
import libWebsiteTools.BaseServlet;
import libWebsiteTools.imead.HtmlPageServlet;
import libWebsiteTools.imead.Localization;
import libWebsiteTools.security.SecurityRepository;

/**
 *
 * @author alpha
 */
public class SiteMilligramExporter implements Runnable {

    private static int[] ERROR_CODES = new int[]{400, 401, 403, 404, 405, 422, 500, 501};
    private static final Logger LOG = Logger.getLogger(SiteMilligramExporter.class.getName());
    private final GramTenant ten;
    private final OutputStream ttr;

    public SiteMilligramExporter(GramTenant ten, OutputStream ttr) {
        this.ten = ten;
        this.ttr = ttr;
    }

    @Override
    public void run() {
        Semaphore rateLimiter = new Semaphore(Runtime.getRuntime().availableProcessors() * 2);
        HttpClient.Builder hbuilder = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).executor(new ForkJoinPool());
        try {
            SSLContext sx = SSLContext.getInstance("TLS");
            sx.init(null, TRUST_ALL, new SecureRandom());
            hbuilder.sslContext(sx);
        } catch (KeyManagementException | NoSuchAlgorithmException gx) {
            throw new RuntimeException(gx);
        }
        HttpClient hclient = hbuilder.build();
        Queue<Future> tasks = new ConcurrentLinkedQueue<>();
        try (ZipOutputStream zip = new ZipOutputStream(ttr)) {
            tasks.add(ten.getExec().submit(() -> {
                ten.getFile().processArchive((f) -> {
                    try {
                        SiteExporter.addFileToZip(zip, "file/" + f.getFilename(), f.getMimetype(), f.getAtime(), f.getFiledata());
                    } catch (IOException ix) {
                        throw new RuntimeException(ix);
                    }
                }, false);
            }));
            tasks.add(ten.getExec().submit(() -> {
                getLocale(ten, "", hclient, rateLimiter, zip);
            }));
            List<Locale> locales = new ArrayList<>(ten.getImead().getLocales());
            locales.remove(Locale.ROOT);
            for (Locale l : locales) {
                tasks.add(ten.getExec().submit(() -> {
                    getLocale(ten, l.toLanguageTag() + "/", hclient, rateLimiter, zip);
                }));
            }
            UtilStatic.finish(tasks);
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
    }

    private static Queue<CompletableFuture> getLocale(GramTenant ten, String locale, HttpClient hclient, Semaphore rateLimiter, ZipOutputStream zip) {
        final BlockingQueue<CompletableFuture> subtasks = new LinkedBlockingQueue<>();
        subtasks.addAll(getAllPagesOfCategory(ten, locale, null, hclient, rateLimiter, zip));
        // Repository.processArchive() causes thread deadlock (I think)
        String baseURL = ten.getImeadValue(SecurityRepository.BASE_URL);
        subtasks.add(milligramAddPageToZip(ten, locale, hclient, rateLimiter, zip, locale + "index.html", baseURL + locale + "index.html"));
        subtasks.add(milligramAddPageToZip(ten, locale, hclient, rateLimiter, zip, locale + "rss/" + ArticleRss.NAME, baseURL + "rss/" + ArticleRss.NAME));
        subtasks.add(milligramAddPageToZip(ten, locale, hclient, rateLimiter, zip, locale + "rss/" + CommentRss.NAME, baseURL + "rss/" + CommentRss.NAME));
        for (Article a : ten.getArts().getAll(null)) {
            String url = ArticleUrl.getUrl(baseURL + locale, a, null);
            String zipPath = ArticleUrl.getUrl(locale, a, null);
            subtasks.add(milligramAddPageToZip(ten, locale, hclient, rateLimiter, zip, zipPath, url));
            if (null != a.getComments() && a.getComments()) {
                String feedURL = locale + "rss/Comments" + a.getArticleid() + ".rss";
                subtasks.add(milligramAddPageToZip(ten, locale, hclient, rateLimiter, zip, feedURL, baseURL + feedURL));
            }
        }
        for (Section category : ten.getCategories().getAll(null)) {
            subtasks.addAll(getAllPagesOfCategory(ten, locale, category.getName(), hclient, rateLimiter, zip));
            subtasks.add(milligramAddPageToZip(ten, locale, hclient, rateLimiter, zip, locale + "rss/" + category.getName() + ArticleRss.NAME, baseURL + "rss/" + category.getName() + ArticleRss.NAME));
        }
        for (int e : ERROR_CODES) {
            subtasks.add(milligramAddPageToZip(ten, locale, hclient, rateLimiter, zip, locale + e + ".html", ten.getImeadValue(SecurityRepository.BASE_URL) + locale + "page/error" + e + ".html"));
        }
        HashSet<String> alreadyRequested = new HashSet<>();
        for (Localization l : ten.getImead().search(HtmlPageServlet.IMEAD_KEY_PREFIX, null)) {
            if (l.getLocalizationPK().getKey().startsWith(HtmlPageServlet.IMEAD_KEY_PREFIX)
                    && !l.getLocalizationPK().getKey().endsWith("_markdown")
                    && !l.getLocalizationPK().getKey().startsWith(BaseServlet.ERROR_PREFIX)
                    && !l.getLocalizationPK().getKey().equals(HtmlPageServlet.IMEAD_KEY_PREFIX + "noPosts")) {
                String pagename = l.getLocalizationPK().getKey().replace(HtmlPageServlet.IMEAD_KEY_PREFIX, "");
                String zipPath = locale + "page/" + pagename + ".html";
                if (!alreadyRequested.contains(zipPath)) {
                    subtasks.add(milligramAddPageToZip(ten, locale, hclient, rateLimiter, zip, zipPath, ten.getImeadValue(SecurityRepository.BASE_URL) + locale + "page/" + pagename + ".html"));
                    alreadyRequested.add(zipPath);
                }
            }
        }
        UtilStatic.join(subtasks);
        return subtasks;
    }

    private static Queue<CompletableFuture> getAllPagesOfCategory(GramTenant ten, String locale, String category, HttpClient hclient, Semaphore rateLimiter, ZipOutputStream zip) {
        final Queue<CompletableFuture> subtasks = new ConcurrentLinkedQueue<>();
        CategoryFetcher f = new CategoryFetcher(ten, Categorizer.getUrl("/", category, 1));
        String baseURL = ten.getImeadValue(SecurityRepository.BASE_URL) + locale;
        for (int p = 1; p <= f.getPageCount(); p++) {
            subtasks.add(milligramAddPageToZip(ten, locale, hclient, rateLimiter, zip, Categorizer.getUrl(locale, category, p), Categorizer.getUrl(baseURL, category, p)));
        }
        return subtasks;
    }

    private static CompletableFuture milligramAddPageToZip(GramTenant ten, String locale, HttpClient hclient, Semaphore rateLimiter, ZipOutputStream zip, String zipPath, String url) {
        String baseURL = ten.getImeadValue(SecurityRepository.BASE_URL);
        URI requestURL = URI.create(url + "?nocache&milligram");
        HttpRequest hreq = HttpRequest.newBuilder(requestURL).GET().build();
        int fileAncestorDistance = zipPath.length() - zipPath.replace("/", "").length();
        int localeAncestorDistance = fileAncestorDistance - (locale.isEmpty() ? 0 : 1);
        String goToFileRoot = "../".repeat(fileAncestorDistance);
        String goToLocaleRoot = "../".repeat(localeAncestorDistance);
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
            if (200 <= t.statusCode() && t.statusCode() <= 299) {
                try {
                    String body = t.body().replaceAll("<base href=\".*?\"/>", "").
                            replaceAll(" href=\"" + baseURL + locale + "index.html", " href=\"" + goToLocaleRoot + "index.html").
                            replaceAll(baseURL, "").
                            replaceAll("(fileImmutable/[0-9A-Za-z=_-]+)", goToFileRoot + "file").
                            replaceAll(" integrity=\"sha256-(?:[A-Za-z0-9]|[+/])+={0,2}\"", "").
                            replaceAll(" href=\"" + locale + "article/", " href=\"" + goToLocaleRoot + "article/").
                            replaceAll(" href=\"" + locale + "index/", " href=\"" + goToLocaleRoot + "index/").
                            replaceAll(" href=\"article/", " href=\"" + goToLocaleRoot + "article/").
                            replaceAll(" href=\"index/", " href=\"" + goToLocaleRoot + "index/").
                            replaceAll(" href=\"rss/", " href=\"" + goToLocaleRoot + "rss/");
                    byte[] bodybytes = body.getBytes("UTF-8");
                    String type = t.headers().firstValue(HttpHeaders.CONTENT_TYPE).get().split(";")[0];
                    SiteExporter.addFileToZip(zip, zipPath, type, OffsetDateTime.now(), bodybytes);
                } catch (IOException ix) {
                    throw new RuntimeException(ix);
                }
            }
        });
    }

    public static final TrustManager[] TRUST_ALL = {new X509ExtendedTrustManager() {
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
