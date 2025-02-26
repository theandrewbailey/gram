package libWebsiteTools.imead;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import libWebsiteTools.security.HashUtil;

/**
 *
 * @author alpha
 */
public class IMEADDatabase extends IMEADHolder {

    protected final EntityManagerFactory PU;

    public IMEADDatabase(EntityManagerFactory PU) {
        this.PU = PU;
        evict();
    }

    /**
     * refresh cache of all properties from the DB
     */
    @Override
    public IMEADHolder evict() {
        PU.getCache().evict(Localization.class);
        localizedHash = HashUtil.getSHA256Hash(localizedCache.toString());
        patterns.clear();
        filteredCache.clear();
        localizedCache = Collections.unmodifiableMap(getProperties());
        return this;
    }

    /**
     * remove property from DB and refresh cache
     *
     * @param localPK
     */
    @Override
    public Localization delete(Object localPK) {
        try (EntityManager em = PU.createEntityManager()) {
            Localization out = em.find(Localization.class, localPK);
            em.getTransaction().begin();
            em.remove(out);
            em.getTransaction().commit();
            evict();
            return out;
        }
    }

    /**
     * add all specified Localizations to DB and refresh cache
     *
     * @param entities
     */
    @Override
    public List<Localization> upsert(Collection<Localization> entities) {
        ArrayList<Localization> out = new ArrayList<>(entities.size());
        boolean dirty = false;
        try (EntityManager em = PU.createEntityManager()) {
            em.getTransaction().begin();
            for (Localization l : entities) {
                Localization existing = em.find(Localization.class, l.localizationPK);
                if (null != existing && !existing.getValue().equals(l.getValue())) {
                    existing.setValue(l.getValue());
                    dirty = true;
                    out.add(existing);
                } else if (null == existing) {
                    em.persist(l);
                    dirty = true;
                    out.add(l);
                }
            }
            em.getTransaction().commit();
        }
        if (dirty) {
            evict();
        }
        return out;
    }

    @Override
    public Localization get(Object localPK) {
        try (EntityManager em = PU.createEntityManager()) {
            return em.find(Localization.class, localPK);
        }
    }

    @Override
    public List<Localization> getAll(Integer limit) {
        try (EntityManager em = PU.createEntityManager()) {
            TypedQuery<Localization> q = em.createNamedQuery("Localization.findAll", Localization.class);
            if (null != limit) {
                q.setMaxResults(limit);
            }
            return q.getResultList();
        }
    }

    @Override
    public void processArchive(Consumer<Localization> operation, Boolean transaction) {
        try (EntityManager em = PU.createEntityManager()) {
            if (transaction) {
                em.getTransaction().begin();
                em.createNamedQuery("Localization.findAll", Localization.class).getResultStream().forEachOrdered(operation);
                em.getTransaction().commit();
            } else {
                em.createNamedQuery("Localization.findAll", Localization.class).getResultStream().forEachOrdered(operation);
            }
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
            TypedQuery<Long> qn = em.createNamedQuery("Localization.count", Long.class);
            return qn.getSingleResult();
        }
    }
}
