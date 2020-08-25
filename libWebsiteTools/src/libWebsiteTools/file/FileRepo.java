package libWebsiteTools.file;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.LocalBean;
import javax.ejb.Startup;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceUnit;
import javax.persistence.TypedQuery;
import libWebsiteTools.bean.SecurityRepo;
import libWebsiteTools.cache.CachedPage;
import libWebsiteTools.cache.PageCache;
import libWebsiteTools.cache.PageCacheProvider;
import libWebsiteTools.cache.PageCaches;
import libWebsiteTools.db.Repository;

@Startup
@Stateless
@LocalBean
public class FileRepo implements Repository<Fileupload> {

    public static final String LOCAL_NAME = "java:module/FileRepo";
    public static final String DEFAULT_MIME_TYPE = "application/octet-stream";
    @Inject
    private PageCacheProvider pageCacheProvider;
    private PageCache globalCache;
    @PersistenceUnit
    private EntityManagerFactory PU;
    private static final Logger LOG = Logger.getLogger(FileRepo.class.getName());

    @Override
    @PostConstruct
    public void evict() {
        if (null == globalCache) {
            globalCache = (PageCache) pageCacheProvider.getCacheManager().<String, CachedPage>getCache(PageCaches.DEFAULT_URI);
        }
        PU.getCache().evict(Fileupload.class);
        PU.getCache().evict(Filemetadata.class);
        EntityManager em = PU.createEntityManager();
        em.getTransaction().begin();
        em.createNativeQuery("REFRESH MATERIALIZED VIEW tools.filemetadata").executeUpdate();
        em.getTransaction().commit();
    }

    @Override
    public Fileupload get(Object filename) {
        Fileupload out;
        EntityManager em = PU.createEntityManager();
        try {
            out = em.find(Fileupload.class, filename, SecurityRepo.USE_CACHE_HINT);
            LOG.log(Level.FINEST, "File retrieved: {0}", filename);
            return out;
        } catch (NoResultException n) {
            LOG.log(Level.FINEST, "File doesn't exist: {0}", filename);
            return null;
        } finally {
            em.close();
        }
    }

    /**
     *
     * @param names
     * @return metadata of requested names, or everything for null
     */
    public List<Filemetadata> getFileMetadata(List<String> names) {
        if (null != names && names.isEmpty()) {
            return new ArrayList<>();
        }
        EntityManager em = PU.createEntityManager();
        try {
            return null == names ? em.createNamedQuery("Filemetadata.findAll", Filemetadata.class).getResultList()
                    : em.createNamedQuery("Filemetadata.findByFilenames", Filemetadata.class).setParameter("filenames", names).getResultList();
        } catch (NoResultException n) {
            return null;
        } finally {
            em.close();
        }
    }

    @Override
    public List<Fileupload> upsert(Collection<Fileupload> entities) {
        ArrayList<Fileupload> out = new ArrayList<>(entities.size());
        EntityManager em = PU.createEntityManager();
        try {
            em.getTransaction().begin();
            for (Fileupload upload : entities) {
                if (null == em.find(Filemetadata.class, upload.getFilename())) {
                    em.persist(upload);
                    LOG.log(Level.INFO, "File added {0}", upload.getFilename());
                } else {
                    upload = em.merge(upload);
                    LOG.log(Level.INFO, "File upserted {0}", upload.getFilename());
                }
                out.add(upload);
            }
            em.getTransaction().commit();
        } catch (RuntimeException d) {
            LOG.log(Level.SEVERE, "Files not committed");
            throw d;
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }
        globalCache.clear();
        return out;
    }

    @Override
    public Fileupload delete(Object filename) {
        EntityManager em = PU.createEntityManager();
        Fileupload f = em.find(Fileupload.class, filename);
        if (null != f) {
            try {
                em.getTransaction().begin();
                em.remove(f);
                em.getTransaction().commit();
            } finally {
                em.close();
            }
        }
        globalCache.clear();
        return f;
    }

    /**
     * process all files one by one
     *
     * @param operation
     * @param transaction are you modifying anything?
     */
    @Override
    public void processArchive(Consumer<Fileupload> operation, Boolean transaction) {
        EntityManager em = PU.createEntityManager();
        try {
            if (transaction) {
                em.getTransaction().begin();
                em.createNamedQuery("Fileupload.findAll", Fileupload.class).getResultStream().forEachOrdered(operation);
                em.getTransaction().commit();
            } else {
                em.createNamedQuery("Fileupload.findAll", Fileupload.class).getResultStream().forEachOrdered(operation);
            }
        } finally {
            em.close();
        }
    }

    @Override
    public List<Fileupload> getAll(Integer limit) {
        EntityManager em = PU.createEntityManager();
        try {
            TypedQuery<Fileupload> q = em.createNamedQuery("Fileupload.findAll", Fileupload.class);
            if (null != limit) {
                q.setMaxResults(limit);
            }
            return q.getResultList();
        } finally {
            em.close();
        }
    }

    @Override
    public Long count() {
        EntityManager em = PU.createEntityManager();
        try {
            TypedQuery<Long> qn = em.createNamedQuery("Fileupload.count", Long.class);
            return qn.getSingleResult();
        } finally {
            em.close();
        }
    }
}
