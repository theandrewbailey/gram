package gram.bean;

import gram.CategoryFetcher;
import gram.UtilStatic;
import gram.bean.database.Article;
import gram.bean.database.ArticleRepository;
import gram.bean.database.Comment;
import gram.bean.database.GramIMEADRepository;
import gram.bean.database.Section;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import libWebsiteTools.Repository;
import libWebsiteTools.file.FileRepository;
import libWebsiteTools.imead.IMEADHolder;
import libWebsiteTools.imead.Localization;
import libWebsiteTools.rss.Feed;
import libWebsiteTools.security.CertUtil;
import libWebsiteTools.security.ExceptionRepository;
import libWebsiteTools.security.Exceptionevent;
import libWebsiteTools.security.HashUtil;
import libWebsiteTools.sitemap.SiteMapper;
import libWebsiteTools.turbo.PageCacheProvider;
import libWebsiteTools.turbo.PerfStats;

/**
 *
 * @author alpha
 */
public class NullTenant implements GramTenant {

    private final NullIMEAD imead;

    public NullTenant(String jndiName) {
        imead = new NullIMEAD(jndiName);
    }

    @Override
    public ArticleRepository getArts() {
        return new NullArticles();
    }

    @Override
    public Repository<Comment> getComms() {
        return new NullComments();
    }

    @Override
    public Repository<Section> getCategories() {
        return new NullCategories();
    }

    @Override
    public ManagedExecutorService getExec() {
        return null;
    }

    @Override
    public ExceptionRepository getError() {
        return new NullErrors();
    }

    @Override
    public FileRepository getFile() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public GramIMEADRepository getImead() {
        return imead;
    }

    @Override
    public Repository<Feed> getFeeds() {
        return new NullFeed();
    }

    @Override
    public SiteMapper getMapper() {
        return new SiteMapper();
    }

    @Override
    public PageCacheProvider getPageCacheProvider() {
        return new PageCacheProvider();
    }

    @Override
    public PerfStats getPerfStats() {
        return new PerfStats();
    }

    @Override
    public NullTenant reset() {
        return this;
    }

    @Override
    public void close() {
    }

    @Override
    public void deleteAll() {
    }

}

class NullIMEAD extends IMEADHolder implements GramIMEADRepository {

    private final List<Localization> locals;

    public NullIMEAD(String jndiName) {
        List<Localization> tempLocals;
        try {
            tempLocals = UtilStatic.getNewLocalizations(this, UtilStatic.getInitialProperties(), Locale.ROOT);
        } catch (IOException ex) {
            tempLocals = new ArrayList<>();
        }
        tempLocals.add(new Localization("", "htmlpage_noPosts", "<p class=\"error\">JNDI database resource " + jndiName + "  can&#39;t be used. Check your server&#39;s configuration and try again. Verify this connection works, rename this resource, or implement support for it.</p>"));
        tempLocals.add(new Localization("", "page_footerFormat", ""));
        tempLocals.add(new Localization("", "page_sideBottom", ""));
        tempLocals.add(new Localization("", "page_sideTop", ""));
        tempLocals.add(new Localization("", "page_footer", ""));
        tempLocals.add(new Localization("", "site_css", ""));
        tempLocals.add(new Localization("", "site_javascript", ""));
        locals = Collections.unmodifiableList(tempLocals);
        evict();
    }

    @Override
    public Collection<Localization> upsert(Collection<Localization> entities) {
        return Collections.unmodifiableList(new ArrayList<>(entities));
    }

    @Override
    public Localization get(Object id) {
        return null;
    }

    @Override
    public List<Localization> getAll(Integer limit) {
        return locals;
    }

    @Override
    public List<Localization> search(Object term, Integer limit) {
        return Collections.unmodifiableList(new ArrayList<>());
    }

    @Override
    public Localization delete(Object id) {
        return null;
    }

    @Override
    public void processArchive(Consumer<Localization> operation, Boolean transaction) {
    }

    @Override
    public Repository<Localization> evict() {
        patterns.clear();
        filteredCache.clear();
        localizedCache = Collections.unmodifiableMap(getProperties());
        localizedHash = HashUtil.getSHA256Hash(localizedCache.toString());
        return this;
    }

    @Override
    public Long count(Object term) {
        return 0L;
    }

    @Override
    public boolean isFirstTime() {
        return false;
    }
}

class NullErrors implements ExceptionRepository {

    @Override
    public Collection<Exceptionevent> upsert(Collection<Exceptionevent> entities) {
        return Collections.unmodifiableList(new ArrayList<>());
    }

    @Override
    public Exceptionevent get(Object id) {
        return null;
    }

    @Override
    public List<Exceptionevent> getAll(Integer limit) {
        return Collections.unmodifiableList(new ArrayList<>());
    }

    @Override
    public List<Exceptionevent> search(Object term, Integer limit) {
        return Collections.unmodifiableList(new ArrayList<>());
    }

    @Override
    public Exceptionevent delete(Object id) {
        return null;
    }

    @Override
    public void processArchive(Consumer<Exceptionevent> operation, Boolean transaction) {
    }

    @Override
    public ExceptionRepository evict() {
        return this;
    }

    @Override
    public Long count(Object term) {
        return 0L;
    }

    @Override
    public CertUtil getCerts() {
        return new CertUtil();
    }

    @Override
    public boolean inHoneypot(String ip) {
        return false;
    }

    @Override
    public void logException(HttpServletRequest req, String title, String desc, Throwable t) {
    }

    @Override
    public boolean putInHoneypot(String ip) {
        return false;
    }
}

class NullArticles implements ArticleRepository {

    @Override
    public List<Article> search(Article term, Integer limit) {
        return Collections.unmodifiableList(new ArrayList<>());
    }

    @Override
    public List<Article> search(CategoryFetcher term, Integer limit) {
        return Collections.unmodifiableList(new ArrayList<>());
    }

    @Override
    public List<Article> search(Section term, Integer limit) {
        return Collections.unmodifiableList(new ArrayList<>());
    }

    @Override
    public Collection<Article> upsert(Collection<Article> entities) {
        return Collections.unmodifiableList(new ArrayList<>());
    }

    @Override
    public Article get(Object id) {
        return null;
    }

    @Override
    public List<Article> getAll(Integer limit) {
        return Collections.unmodifiableList(new ArrayList<>());
    }

    @Override
    public List<Article> search(Object term, Integer limit) {
        return Collections.unmodifiableList(new ArrayList<>());
    }

    @Override
    public Article delete(Object id) {
        return null;
    }

    @Override
    public void processArchive(Consumer<Article> operation, Boolean transaction) {
    }

    @Override
    public Repository<Article> evict() {
        return this;
    }

    @Override
    public Long count(Object term) {
        return 0L;
    }
}

class NullComments implements Repository<Comment> {

    @Override
    public Collection<Comment> upsert(Collection<Comment> entities) {
        return Collections.unmodifiableList(new ArrayList<>());
    }

    @Override
    public Comment get(Object id) {
        return null;
    }

    @Override
    public List<Comment> getAll(Integer limit) {
        return Collections.unmodifiableList(new ArrayList<>());
    }

    @Override
    public List<Comment> search(Object term, Integer limit) {
        return Collections.unmodifiableList(new ArrayList<>());
    }

    @Override
    public Comment delete(Object id) {
        return null;
    }

    @Override
    public void processArchive(Consumer<Comment> operation, Boolean transaction) {
    }

    @Override
    public Repository<Comment> evict() {
        return this;
    }

    @Override
    public Long count(Object term) {
        return 0L;
    }
}

class NullCategories implements Repository<Section> {

    @Override
    public Collection<Section> upsert(Collection<Section> entities) {
        return Collections.unmodifiableList(new ArrayList<>());
    }

    @Override
    public Section get(Object id) {
        return null;
    }

    @Override
    public List<Section> getAll(Integer limit) {
        return Collections.unmodifiableList(new ArrayList<>());
    }

    @Override
    public List<Section> search(Object term, Integer limit) {
        return Collections.unmodifiableList(new ArrayList<>());
    }

    @Override
    public Section delete(Object id) {
        return null;
    }

    @Override
    public void processArchive(Consumer<Section> operation, Boolean transaction) {
    }

    @Override
    public Repository<Section> evict() {
        return this;
    }

    @Override
    public Long count(Object term) {
        return 0L;
    }
}

class NullFeed implements Repository<Feed> {

    @Override
    public Collection<Feed> upsert(Collection<Feed> entities) {
        return Collections.unmodifiableList(new ArrayList<>());
    }

    @Override
    public Feed get(Object id) {
        return null;
    }

    @Override
    public List<Feed> getAll(Integer limit) {
        return Collections.unmodifiableList(new ArrayList<>());
    }

    @Override
    public List<Feed> search(Object term, Integer limit) {
        return Collections.unmodifiableList(new ArrayList<>());
    }

    @Override
    public Feed delete(Object id) {
        return null;
    }

    @Override
    public void processArchive(Consumer<Feed> operation, Boolean transaction) {
    }

    @Override
    public Repository<Feed> evict() {
        return this;
    }

    @Override
    public Long count(Object term) {
        return 0L;
    }
}
