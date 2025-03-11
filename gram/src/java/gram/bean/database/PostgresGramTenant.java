package gram.bean.database;

import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.persistence.EntityManagerFactory;
import java.util.Arrays;
import java.util.Map;
import libWebsiteTools.Repository;
import libWebsiteTools.file.FileDatabase;
import libWebsiteTools.file.FileRepository;
import libWebsiteTools.imead.IMEADHolder;
import libWebsiteTools.rss.FeedBucket;
import libWebsiteTools.security.GuardFilter;
import libWebsiteTools.security.HashUtil;
import libWebsiteTools.security.SecurityRepo;
import libWebsiteTools.sitemap.SiteMapper;
import libWebsiteTools.turbo.PageCacheProvider;
import libWebsiteTools.turbo.PerfStats;
import gram.SitemapProvider;
import gram.rss.ArticleRss;
import gram.rss.CommentRss;
import gram.rss.ErrorRss;
import gram.AdminPermission;
import gram.bean.GramTenant;
import java.util.Collections;
import javax.sql.DataSource;
import libWebsiteTools.rss.Feed;

/**
 * Tenant class to use for Postgres + Eclipselink.
 *
 * @author alpha
 */
public class PostgresGramTenant implements GramTenant {

    private final ManagedExecutorService exec;
    private final EntityManagerFactory gramPU;
    private final PageCacheProvider pageCacheProvider;
    private final SecurityRepo error;
    private final FileRepository file;
    private final IMEADHolder imead;
    private final Repository<Feed> feeds;
    private final ArticleRepository arts;
    private final Repository<Comment> comms;
    private final Repository<Section> sects;
    private final SiteMapper mapper;
    private final PerfStats perfs;
    private Boolean firstTime;
    private final Map<String, String> originalPasswords;

    public PostgresGramTenant(String jndiName, DataSource ds, ManagedExecutorService es) {
        GramEclipselinkUnitInfo info = new GramEclipselinkUnitInfo(jndiName, ds);
        gramPU = info.getEntityManagerFactory();
        exec = es;

        pageCacheProvider = new PageCacheProvider();
        perfs = new PerfStats();
        feeds = new FeedBucket();
        imead = new GramIMEADDatabase(gramPU);
        file = new FileDatabase(gramPU);
        comms = new CommentDatabase(gramPU);
        arts = new PostgresArticleDatabase(gramPU);
        sects = new CategoryDatabase(gramPU);
        error = new SecurityRepo(gramPU, imead);
        try {
            String certName = getImeadValue(GuardFilter.CERTIFICATE_NAME);
            if (null != certName && !certName.isBlank()) {
                error.getCerts().verifyCertificate(certName);
            }
        } catch (RuntimeException rx) {
            error.logException(null, "High security not available", null, rx);
        }
        mapper = new SiteMapper().addSource(new SitemapProvider(this));

        originalPasswords = Collections.unmodifiableMap(GramTenant.getOriginalPasswords());
        getFeeds().upsert(Arrays.asList(new ArticleRss(), new CommentRss(), new ErrorRss()));
    }

    /**
     * called when big state changes occur that require temporary caches to be
     * emptied (new article, changed configuration, etc), or when you want to
     * guarantee retrieval of latest data (backup).
     */
    @Override
    public synchronized void reset() {
        gramPU.getCache().evictAll();
        getImead().evict();
        getArts().evict();
        getCategories().evict();
        getFile().evict();
        getPerfStats().evict();
        getGlobalCache().clear();
        firstTime = null;
    }

    @Override
    public synchronized void destroy() {
        gramPU.close();
    }

    @Override
    public synchronized boolean isFirstTime() {
        if (null != firstTime) {
            return firstTime;
        }
        for (AdminPermission p : AdminPermission.Password.values()) {
            if (null == p.getKey()) {
                continue;
            }
            String original = originalPasswords.get(p.getKey());
            String current = getImeadValue(p.getKey());
            if (null == original || null == current || original.equals(current) || !HashUtil.ARGON2_ENCODING_PATTERN.matcher(current).matches()) {
                firstTime = true;
                return true;
            }
        }
        firstTime = false;
        return false;
    }

    @Override
    public ArticleRepository getArts() {
        return arts;
    }

    @Override
    public Repository<Comment> getComms() {
        return comms;
    }

    @Override
    public Repository<Section> getCategories() {
        return sects;
    }

    @Override
    public ManagedExecutorService getExec() {
        return exec;
    }

    @Override
    public SecurityRepo getError() {
        return error;
    }

    @Override
    public FileRepository getFile() {
        return file;
    }

    @Override
    public IMEADHolder getImead() {
        return imead;
    }

    @Override
    public Repository<Feed> getFeeds() {
        return feeds;
    }

    @Override
    public PageCacheProvider getPageCacheProvider() {
        return pageCacheProvider;
    }

    @Override
    public SiteMapper getMapper() {
        return mapper;
    }

    @Override
    public PerfStats getPerfStats() {
        return perfs;
    }
}
