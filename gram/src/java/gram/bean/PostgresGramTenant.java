package gram.bean;

import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.persistence.EntityManagerFactory;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
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
import gram.bean.database.Comment;
import gram.bean.database.CommentDatabase;
import gram.bean.database.PostgresArticleDatabase;
import gram.bean.database.Section;
import gram.bean.database.SectionDatabase;
import gram.rss.ArticleRss;
import gram.rss.CommentRss;
import gram.rss.ErrorRss;
import gram.AdminPermission;
import gram.UtilStatic;
import gram.bean.database.GramIMEADDatabase;

/**
 *
 * @author alpha
 */
public class PostgresGramTenant implements GramTenant {

    private static final Logger LOG = Logger.getLogger(PostgresGramTenant.class.getName());
    private final ManagedExecutorService exec;
    private final EntityManagerFactory gramPU;
    private PageCacheProvider pageCacheProvider;
    private SecurityRepo error;
    private FileRepository file;
    private IMEADHolder imead;
    private FeedBucket feeds;
    private ArticleRepository arts;
    private Repository<Comment> comms;
    private Repository<Section> sects;
    private BackupDaemon backup;
    private SiteMapper mapper;
    private PerfStats perfs;
    private Boolean firstTime;
    private final Map<String, String> originalPasswords = new HashMap<>();

    public PostgresGramTenant(EntityManagerFactory fact, ManagedExecutorService ex) {
        gramPU = fact;
        exec = ex;
        getFeeds().upsert(Arrays.asList(new ArticleRss(), new CommentRss(), new ErrorRss()));
    }

    /**
     * called when big state changes occur that require temporary caches to be
     * emptied (new article, changed configuration, etc).
     */
    @Override
    public synchronized void reset() {
        gramPU.getCache().evictAll();
        getImead().evict();
        getArts().evict();
        getSects().evict();
        getFile().evict();
        getPerfStats().evict();
        getGlobalCache().clear();
        firstTime = null;
    }

    @Override
    public void destroy() {
        gramPU.close();
    }

    @Override
    public boolean isFirstTime() {
        if (null != firstTime) {
            return firstTime;
        }
        if (originalPasswords.isEmpty()) {
            try {
                Properties IMEAD = UtilStatic.getInitialProperties();
                for (AdminPermission p : AdminPermission.Password.values()) {
                    if (null == p.getKey()) {
                        continue;
                    }
                    originalPasswords.put(p.getKey(), IMEAD.get(p.getKey()).toString());
                }
            } catch (Exception ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
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
        if (null == arts) {
            arts = new PostgresArticleDatabase(gramPU, getImead());
        }
        return arts;
    }

    @Override
    public Repository<Comment> getComms() {
        if (null == comms) {
            comms = new CommentDatabase(gramPU);
        }
        return comms;
    }

    @Override
    public Repository<Section> getSects() {
        if (null == sects) {
            sects = new SectionDatabase(gramPU, getImead());
        }
        return sects;
    }

    @Override
    public BackupDaemon getBackup() {
        if (null == backup) {
            backup = new BackupDaemon(this);
        }
        return backup;
    }

    @Override
    public ManagedExecutorService getExec() {
        return exec;
    }

    @Override
    public SecurityRepo getError() {
        if (null == error) {
            error = new SecurityRepo(gramPU, getImead());
            try {
                String certName = getImeadValue(GuardFilter.CERTIFICATE_NAME);
                if (null != certName && !certName.isBlank()) {
                    error.getCerts().verifyCertificate(certName);
                }
            } catch (RuntimeException rx) {
                error.logException(null, "High security not available", null, rx);
            }
        }
        return error;
    }

    @Override
    public FileRepository getFile() {
        if (null == file) {
            file = new FileDatabase(gramPU);
        }
        return file;
    }

    @Override
    public IMEADHolder getImead() {
        if (null == imead) {
            imead = new GramIMEADDatabase(gramPU);
        }
        return imead;
    }

    @Override
    public FeedBucket getFeeds() {
        if (null == feeds) {
            feeds = new FeedBucket();
        }
        return feeds;
    }

    @Override
    public PageCacheProvider getPageCacheProvider() {
        if (null == pageCacheProvider) {
            pageCacheProvider = new PageCacheProvider();
        }
        return pageCacheProvider;
    }

    @Override
    public SiteMapper getMapper() {
        if (null == mapper) {
            mapper = new SiteMapper();
            mapper.addSource(new SitemapProvider(this));
        }
        return mapper;
    }

    @Override
    public PerfStats getPerfStats() {
        if (null == perfs) {
            perfs = new PerfStats();
        }
        return perfs;
    }
}
