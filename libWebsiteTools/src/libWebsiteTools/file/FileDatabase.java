package libWebsiteTools.file;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;

public class FileDatabase implements FileRepository {

    protected final EntityManagerFactory PU;
    private static final Logger LOG = Logger.getLogger(FileDatabase.class.getName());

    public FileDatabase(EntityManagerFactory PU) {
        this.PU = PU;
        evict();
    }

    @Override
    public synchronized FileDatabase evict() {
        PU.getCache().evict(Fileupload.class);
        return this;
    }

    @Override
    public Fileupload get(Object filename) {
        Fileupload out;
        try (EntityManager em = PU.createEntityManager()) {
            out = em.find(Fileupload.class, filename);
            LOG.log(Level.FINEST, "File retrieved: {0}", filename);
            return out;
        } catch (NoResultException n) {
            LOG.log(Level.FINEST, "File doesn't exist: {0}", filename);
            return null;
        }
    }

    /**
     *
     * @param names
     * @return metadata of requested names, or everything for null
     */
    @Override
    public List<Fileupload> getFileMetadata(List<String> names) {
        if (null != names && names.isEmpty()) {
            return new ArrayList<>();
        }
        try (EntityManager em = PU.createEntityManager()) {
            return null == names ? em.createNamedQuery("Filemetadata.findAll", Fileupload.class).getResultList()
                    : em.createNamedQuery("Filemetadata.findByFilenames", Fileupload.class).setParameter("filenames", names).getResultList();
        } catch (NoResultException n) {
            return null;
        }
    }

    /**
     *
     * @param term search files with names like this
     * @param limit ignored
     * @return
     */
    @Override
    public List<Fileupload> search(Object term, Integer limit) {
        try (EntityManager em = PU.createEntityManager()) {
            return em.createNamedQuery("Filemetadata.search", Fileupload.class).setParameter("term", term).getResultList();
        } catch (NoResultException n) {
            return null;
        }
    }

    @Override
    public List<Fileupload> search(Fileupload term, Integer limit) {
        try (EntityManager em = PU.createEntityManager()) {
            return em.createNamedQuery("Filemetadata.searchByType", Fileupload.class).setParameter("term", term.getMimetype()).getResultList();
        } catch (NoResultException n) {
            return null;
        }
    }

    @Override
    public List<Fileupload> upsert(Collection<Fileupload> entities) {
        ArrayList<Fileupload> out = new ArrayList<>(entities.size());
        try (EntityManager em = PU.createEntityManager()) {
            try {
                em.getTransaction().begin();
                for (Fileupload upload : entities) {
                    upload = em.merge(upload);
                    LOG.log(Level.FINE, "File upserted {0}", upload.getFilename());
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
            }
        }
        return out;
    }

    @Override
    public Fileupload delete(Object filename) {
        try (EntityManager em = PU.createEntityManager()) {
            Fileupload f = em.find(Fileupload.class, filename);
            if (null != f) {
                em.getTransaction().begin();
                em.remove(f);
                em.getTransaction().commit();
            }
            return f;
        }
    }

    /**
     * @param operation This will be run in parallel.
     * @param transaction Should changes be saved?
     */
    @Override
    public void processArchive(Consumer<Fileupload> operation, Boolean transaction) {
        try (EntityManager em = PU.createEntityManager()) {
            if (transaction) {
                em.getTransaction().begin();
                em.createNamedQuery("Fileupload.findAll", Fileupload.class).getResultStream().forEach(operation);
                em.getTransaction().commit();
            } else {
                em.createNamedQuery("Fileupload.findAll", Fileupload.class).getResultStream().forEach(operation);
            }
        }
    }

    @Override
    public List<Fileupload> getAll(Integer limit) {
        try (EntityManager em = PU.createEntityManager()) {
            TypedQuery<Fileupload> q = em.createNamedQuery("Fileupload.findAll", Fileupload.class);
            if (null != limit) {
                q.setMaxResults(limit);
            }
            return q.getResultList();
        }
    }

    /**
     *
     * @param term ignored
     * @return
     */
    @Override
    public Long count(Object term) {
        try (EntityManager em = PU.createEntityManager()) {
            TypedQuery<Long> qn = em.createNamedQuery("Fileupload.count", Long.class);
            return qn.getSingleResult();
        }
    }
}
