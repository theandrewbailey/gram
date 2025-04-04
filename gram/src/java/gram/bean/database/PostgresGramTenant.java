package gram.bean.database;

import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.persistence.EntityManagerFactory;
import java.util.Arrays;
import libWebsiteTools.Repository;
import libWebsiteTools.file.FileDatabase;
import libWebsiteTools.file.FileRepository;
import libWebsiteTools.rss.FeedBucket;
import libWebsiteTools.security.GuardFilter;
import libWebsiteTools.security.SecurityRepository;
import libWebsiteTools.sitemap.SiteMapper;
import libWebsiteTools.turbo.PageCacheProvider;
import libWebsiteTools.turbo.PerfStats;
import gram.SitemapProvider;
import gram.rss.ArticleRss;
import gram.rss.CommentRss;
import gram.rss.ErrorRss;
import gram.bean.GramTenant;
import jakarta.persistence.EntityManager;
import jakarta.persistence.SharedCacheMode;
import jakarta.persistence.ValidationMode;
import jakarta.persistence.spi.ClassTransformer;
import jakarta.persistence.spi.PersistenceProvider;
import jakarta.persistence.spi.PersistenceUnitInfo;
import jakarta.persistence.spi.PersistenceUnitTransactionType;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.DataSource;
import libWebsiteTools.UUIDConverter;
import libWebsiteTools.file.Fileupload;
import libWebsiteTools.imead.Localization;
import libWebsiteTools.rss.Feed;
import libWebsiteTools.security.Exceptionevent;
import libWebsiteTools.security.Honeypot;

/**
 * Tenant class to use for Postgres + Eclipselink.
 *
 * @author alpha
 */
public class PostgresGramTenant implements GramTenant {

    private static final String CLOSED = "Persistence unit is closed.";
    private static final Logger LOG = Logger.getLogger(PostgresGramTenant.class.getName());
    private final ManagedExecutorService exec;
    private final EntityManagerFactory gramPU;
    private final PageCacheProvider pageCacheProvider;
    private final SecurityRepository error;
    private final FileRepository file;
    private final GramIMEADRepository imead;
    private final Repository<Feed> feeds;
    private final ArticleRepository arts;
    private final Repository<Comment> comms;
    private final Repository<Section> sects;
    private final SiteMapper mapper;
    private final PerfStats perfs;

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
        error = new SecurityRepository(gramPU, imead);
        try {
            String certName = getImeadValue(GuardFilter.CERTIFICATE_NAME);
            if (null != certName && !certName.isBlank()) {
                error.getCerts().verifyCertificate(certName);
            }
        } catch (RuntimeException rx) {
            error.logException(null, "High security not available", null, rx);
        }
        mapper = new SiteMapper().addSource(new SitemapProvider(this));

        getFeeds().upsert(Arrays.asList(new ArticleRss(), new CommentRss(), new ErrorRss()));
        getImead().isFirstTime();
    }

    @Override
    public synchronized GramTenant reset() {
        if (!gramPU.isOpen()) {
            throw new IllegalStateException(CLOSED);
        }
        gramPU.getCache().evictAll();
        getImead().evict();
        getArts().evict();
        getCategories().evict();
        getFile().evict();
        getPerfStats().evict();
        getGlobalCache().clear();
        return this;
    }

    @Override
    public synchronized void close() {
        gramPU.close();
    }

    @Override
    public synchronized void deleteAll() {
        reset();
        try (EntityManager em = gramPU.createEntityManager()) {
            em.getTransaction().begin();
            em.createNativeQuery("drop schema gram cascade").executeUpdate();
            em.createNativeQuery("drop schema tools cascade").executeUpdate();
            em.getTransaction().commit();
            LOG.log(Level.WARNING, "Database deleted!");
        }
        close();
    }

    @Override
    public ArticleRepository getArts() {
        if (!gramPU.isOpen()) {
            throw new IllegalStateException(CLOSED);
        }
        return arts;
    }

    @Override
    public Repository<Comment> getComms() {
        if (!gramPU.isOpen()) {
            throw new IllegalStateException(CLOSED);
        }
        return comms;
    }

    @Override
    public Repository<Section> getCategories() {
        if (!gramPU.isOpen()) {
            throw new IllegalStateException(CLOSED);
        }
        return sects;
    }

    @Override
    public ManagedExecutorService getExec() {
        if (!gramPU.isOpen()) {
            throw new IllegalStateException(CLOSED);
        }
        return exec;
    }

    @Override
    public SecurityRepository getError() {
        if (!gramPU.isOpen()) {
            throw new IllegalStateException(CLOSED);
        }
        return error;
    }

    @Override
    public FileRepository getFile() {
        if (!gramPU.isOpen()) {
            throw new IllegalStateException(CLOSED);
        }
        return file;
    }

    @Override
    public GramIMEADRepository getImead() {
        if (!gramPU.isOpen()) {
            throw new IllegalStateException(CLOSED);
        }
        return imead;
    }

    @Override
    public Repository<Feed> getFeeds() {
        if (!gramPU.isOpen()) {
            throw new IllegalStateException(CLOSED);
        }
        return feeds;
    }

    @Override
    public PageCacheProvider getPageCacheProvider() {
        if (!gramPU.isOpen()) {
            throw new IllegalStateException(CLOSED);
        }
        return pageCacheProvider;
    }

    @Override
    public SiteMapper getMapper() {
        if (!gramPU.isOpen()) {
            throw new IllegalStateException(CLOSED);
        }
        return mapper;
    }

    @Override
    public PerfStats getPerfStats() {
        if (!gramPU.isOpen()) {
            throw new IllegalStateException(CLOSED);
        }
        return perfs;
    }
}

class GramEclipselinkUnitInfo implements PersistenceUnitInfo {

    private static final PersistenceProvider eclipselink = new org.eclipse.persistence.jpa.PersistenceProvider();
    private final String unitName;
    private final DataSource d;

    public GramEclipselinkUnitInfo(String jndiName, DataSource ds) {
        this.unitName = jndiName;
        this.d = ds;
    }

    public EntityManagerFactory getEntityManagerFactory() {
        return eclipselink.createContainerEntityManagerFactory(this, new HashMap<>());
    }

    @Override
    public List<String> getManagedClassNames() {
        return List.of(UUIDConverter.class.getName(),
                Exceptionevent.class.getName(),
                Honeypot.class.getName(),
                Fileupload.class.getName(),
                Localization.class.getName(),
                Article.class.getName(),
                Section.class.getName(),
                Comment.class.getName());
    }

    @Override
    public Properties getProperties() {
        Properties p = new Properties();
        p.put("jakarta.persistence.schema-generation.database.action", "create");
        p.put("jakarta.persistence.schema-generation.create-source", "script");
        p.put("jakarta.persistence.schema-generation.create-script-source", "gramsetup.sql");
        // is this really needed?
        p.put("hibernate.hbm2ddl.import_files_sql_extractor", "org.hibernate.tool.hbm2ddl.MultipleLinesSqlCommandExtractor");
        return p;
    }

    @Override
    public URL getPersistenceUnitRootUrl() {
        try {
            // give it a jar of something, I don't care, excludeUnlistedClasses() is true and getManagedClassNames() returns the entities its trying to find
            URL purl = Localization.class.getProtectionDomain().getCodeSource().getLocation();
            return purl;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public String getPersistenceProviderClassName() {
        return "org.eclipse.persistence.jpa.PersistenceProvider";
    }

    @Override
    public String getPersistenceUnitName() {
        return unitName;
    }

    @Override
    public PersistenceUnitTransactionType getTransactionType() {
        return PersistenceUnitTransactionType.RESOURCE_LOCAL;
    }

    @Override
    public DataSource getJtaDataSource() {
        return null;
    }

    @Override
    public DataSource getNonJtaDataSource() {
        return d;
    }

    @Override
    public List<String> getMappingFileNames() {
        return List.of();
    }

    @Override
    public List<URL> getJarFileUrls() {
        return List.of(getPersistenceUnitRootUrl());
    }

    @Override
    public boolean excludeUnlistedClasses() {
        return true;
    }

    @Override
    public SharedCacheMode getSharedCacheMode() {
        return SharedCacheMode.ENABLE_SELECTIVE;
    }

    @Override
    public ValidationMode getValidationMode() {
        return ValidationMode.AUTO;
    }

    @Override
    public String getPersistenceXMLSchemaVersion() {
        return "2.0";
    }

    @Override
    public ClassLoader getClassLoader() {
        // use the current instance's loader to prevent ClassCastExceptions
        return this.getClass().getClassLoader();
    }

    @Override
    public void addTransformer(ClassTransformer ct) {
    }

    @Override
    public ClassLoader getNewTempClassLoader() {
        return null;
    }
}
