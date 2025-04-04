package libWebsiteTools;

import jakarta.enterprise.concurrent.ManagedExecutorService;
import libWebsiteTools.file.FileRepository;
import libWebsiteTools.imead.IMEADRepository;
import libWebsiteTools.rss.Feed;
import libWebsiteTools.security.ExceptionRepository;
import libWebsiteTools.sitemap.SiteMapper;
import libWebsiteTools.turbo.CachedPage;
import libWebsiteTools.turbo.PageCache;
import libWebsiteTools.turbo.PageCacheProvider;
import libWebsiteTools.turbo.PageCaches;
import libWebsiteTools.turbo.PerfStats;

/**
 * Provides access to all data for a single tenant. Tenant must create
 * underlying data structures (database tables, files, etc.) as needed on
 * instantiation.
 *
 * @author alpha
 */
public interface Tenant extends AutoCloseable {

    public ManagedExecutorService getExec();

    public ExceptionRepository getError();

    public FileRepository getFile();

    public IMEADRepository getImead();

    public default String getImeadValue(String key) {
        return getImead().getValue(key);
    }

    public Repository<Feed> getFeeds();

    public SiteMapper getMapper();

    public PageCacheProvider getPageCacheProvider();

    public PerfStats getPerfStats();

    /**
     * Called when big state changes occur that require temporary caches to be
     * emptied (new article, changed configuration, etc), or when you want to
     * guarantee retrieval of latest data (backup).
     *
     * @return this
     */
    public Tenant reset();

    /**
     * Delete all data in storage, excluding backups. Database tables and
     * folders may be deleted, but must be re-created empty on re-instantiation
     * of the Tenant.
     */
    public void deleteAll();

    public default PageCache getGlobalCache() {
        return (PageCache) getPageCacheProvider().getCacheManager().<String, CachedPage>getCache(PageCaches.DEFAULT_URI);
    }
}
