package libWebsiteTools.turbo;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.configuration.CacheEntryListenerConfiguration;
import javax.cache.configuration.Configuration;
import javax.cache.integration.CompletionListener;
import javax.cache.processor.EntryProcessor;
import javax.cache.processor.EntryProcessorException;
import javax.cache.processor.EntryProcessorResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.ext.RuntimeDelegate;
import libWebsiteTools.imead.IMEADHolder;
import libWebsiteTools.security.HashUtil;
import libWebsiteTools.imead.Local;
import libWebsiteTools.tag.AbstractInput;

/**
 *
 * @author alpha
 */
public class PageCache implements Cache<String, CachedPage> {

    public static final String PAGECACHE_LOOKUP="$_PAGECACHE_LOOKUP";
    private Map<String, CachedPage> cache;
    private final CacheManager cman;
    private final String name;
    private OffsetDateTime lastFlush = OffsetDateTime.now();
    private final AtomicInteger totalHits=new AtomicInteger();
    private final AtomicInteger totalPages=new AtomicInteger();

    public PageCache() {
        cache = Collections.synchronizedMap(new InternalCache(100));
        cman = null;
        name = null;
    }

    public PageCache(CacheManager cman, String name, int capacity) {
        this.cman = cman;
        this.name = name;
        cache = Collections.synchronizedMap(new InternalCache(capacity));
    }

    /**
     *
     * @param term search cache keys for this term
     * @return complete key that exists
     */
    public Set<String> searchLookups(String term) {
        HashSet<String> out = new HashSet<>();
        for (String key : new ArrayList<>(cache.keySet())) {
            if (key.contains(term)) {
                out.add(key);
            }
        }
        return out;
    }

    public static String getLookup(IMEADHolder imead, HttpServletRequest req) {
        Object lookup = req.getAttribute(PAGECACHE_LOOKUP);
        if (null == lookup) {
            StringBuilder lookupBuild = new StringBuilder(300).append(req.getAttribute(AbstractInput.ORIGINAL_REQUEST_URL).toString());
            lookupBuild.append("\n").append(CompressedOutput.getTypeString(req));
            lookupBuild.append("; ").append(Local.getLocaleString(imead, req));
            lookupBuild.append("; ").append(imead.getLocalizedHash());
            lookup = lookupBuild.toString();
            req.setAttribute(PAGECACHE_LOOKUP, lookup);
        }
        return lookup.toString();
    }

    public static String getETag(IMEADHolder imead, HttpServletRequest req) {
        Object etag = req.getAttribute(HttpHeaders.ETAG);
        if (null == etag) {
            etag = "W/\"" + HashUtil.getSHA256Hash(PageCache.getLookup(imead, req)) + "\"";
            req.setAttribute(HttpHeaders.ETAG, etag);
        }
        return etag.toString();
    }

    public PageCache getCache(HttpServletRequest req, HttpServletResponse res) {
        int status = res.getStatus();
        if (200 == status || 404 == status || 410 == status) {
            CacheControl cc = RuntimeDelegate.getInstance().createHeaderDelegate(CacheControl.class).fromString(res.getHeader(HttpHeaders.CACHE_CONTROL));
            if (cc.isNoCache() || cc.isNoStore() || null != req.getParameter("nocache")) {
                return null;
            }
            if (cc.isPrivate()) {
                /*PageCache privateCache = (PageCache) req.getSession().getAttribute(PageCache.class.getCanonicalName());
            if (null == privateCache) {
                privateCache = new PageCache(100);
                req.getSession().setAttribute(PageCache.class.getCanonicalName(), privateCache);
            }
            return privateCache;*/
                return null;
            }
            return this;
        }
        return null;
    }

    @Override
    public CachedPage get(String etag) {
        CachedPage page = cache.get(etag);
        if (null != page && page.isExpired(lastFlush)) {
            cache.remove(etag);
            return null;
        }
        return page;
    }

    @Override
    public void put(String lookup, CachedPage page) {
        cache.put(lookup, page);
        incrementTotalPage();
    }

    @Override
    public Map<String, CachedPage> getAll(Set<? extends String> set) {
        return new HashMap<>(cache);
    }

    @Override
    public boolean containsKey(String k) {
        return cache.containsKey(k);
    }

    @Override
    public void loadAll(Set<? extends String> set, boolean bln, CompletionListener cl) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public CachedPage getAndPut(String k, CachedPage v) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void putAll(Map<? extends String, ? extends CachedPage> map) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean putIfAbsent(String k, CachedPage v) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean remove(String k) {
        return null != cache.remove(k);
    }

    @Override
    public boolean remove(String k, CachedPage v) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public CachedPage getAndRemove(String k) {
        return cache.remove(k);
    }

    @Override
    public boolean replace(String k, CachedPage v, CachedPage v1) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean replace(String k, CachedPage v) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public CachedPage getAndReplace(String k, CachedPage v) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void removeAll(Set<? extends String> set) {
        for (String key : set) {
            cache.remove(key);
        }
    }

    @Override
    public void removeAll() {
        cache.clear();
    }

    @Override
    public <C extends Configuration<String, CachedPage>> C getConfiguration(Class<C> type) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public <T> T invoke(String k, EntryProcessor<String, CachedPage, T> ep, Object... os) throws EntryProcessorException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public <T> Map<String, EntryProcessorResult<T>> invokeAll(Set<? extends String> set, EntryProcessor<String, CachedPage, T> ep, Object... os) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public CacheManager getCacheManager() {
        return cman;
    }

    @Override
    public void clear() {
        lastFlush = OffsetDateTime.now();
        cache.clear();
        totalHits.set(0);
        totalPages.set(0);
    }

    @Override
    public void close() {
        cache.clear();
        cache = null;
    }

    @Override
    public boolean isClosed() {
        return null == cache;
    }

    @Override
    public <T> T unwrap(Class<T> type) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void registerCacheEntryListener(CacheEntryListenerConfiguration<String, CachedPage> celc) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void deregisterCacheEntryListener(CacheEntryListenerConfiguration<String, CachedPage> celc) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Iterator<Entry<String, CachedPage>> iterator() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private class InternalCache extends LinkedHashMap<String, CachedPage> {

        private final int capacity;
        private OffsetDateTime lastFlush = OffsetDateTime.now();

        public InternalCache(int capacity) {
            super(capacity * 2, 0.8f, true);
            this.capacity = capacity;
        }

        @Override
        public void clear() {
            super.clear();
            lastFlush = OffsetDateTime.now();
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<String, CachedPage> entry) {
            return entry.getValue().isExpired(getLastFlush()) || size() > capacity;
        }

        public OffsetDateTime getLastFlush() {
            return lastFlush;
        }
    }

    public Integer getTotalHits() {
        return totalHits.get();
    }

    public void incrementTotalHit() {
        totalHits.incrementAndGet();
    }

    public Integer getTotalPages() {
        return totalPages.get();
    }

    public void incrementTotalPage() {
        totalPages.incrementAndGet();
    }
}
