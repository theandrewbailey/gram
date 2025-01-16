package libWebsiteTools;

import jakarta.enterprise.concurrent.ManagedExecutorService;
import libWebsiteTools.file.FileRepository;
import libWebsiteTools.imead.IMEADHolder;
import libWebsiteTools.rss.FeedBucket;
import libWebsiteTools.security.SecurityRepo;
import libWebsiteTools.sitemap.SiteMapper;
import libWebsiteTools.turbo.CachedPage;
import libWebsiteTools.turbo.PageCache;
import libWebsiteTools.turbo.PageCacheProvider;
import libWebsiteTools.turbo.PageCaches;
import libWebsiteTools.turbo.PerfStats;

/**
 * Provides access to all data for a single tenant.
 *
 * @author alpha
 */
public interface Tenant {

    public ManagedExecutorService getExec();

    public SecurityRepo getError();

    public FileRepository getFile();

    public IMEADHolder getImead();

    public default String getImeadValue(String key) {
        return getImead().getValue(key);
    }

    public FeedBucket getFeeds();

    public SiteMapper getMapper();

    public PageCacheProvider getPageCacheProvider();

    public PerfStats getPerfStats();

    public void reset();

    public void destroy();

    public default PageCache getGlobalCache() {
        return (PageCache) getPageCacheProvider().getCacheManager().<String, CachedPage>getCache(PageCaches.DEFAULT_URI);
    }
}
