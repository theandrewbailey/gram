package gram.bean.database;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;
import java.util.function.Consumer;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import libWebsiteTools.Repository;
import gram.UtilStatic;

/**
 *
 * @author alpha
 */
public class CategoryDatabase implements Repository<Section> {

    private final EntityManagerFactory gramPU;
    private List<Section> allSections;

    public CategoryDatabase(EntityManagerFactory emf) {
        this.gramPU = emf;
    }

    @Override
    public Collection<Section> upsert(Collection<Section> entities) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Section get(Object id) {
        try (EntityManager em = gramPU.createEntityManager()) {
            if (id instanceof Integer) {
                return em.find(Section.class, id);
            } else if (id instanceof String) {
                return em.createNamedQuery("Category.findByName", Section.class).setParameter("name", id).getSingleResult();
            }
        } catch (NoResultException n) {
            return null;
        }
        throw new IllegalArgumentException("Bad type. Must be String or Integer.");
    }

    @Override
    public List<Section> search(Object term, Integer limit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Section delete(Object id) {
        throw new UnsupportedOperationException();
    }

    /**
     * @param limit ignored
     * @return sections sorted by popularity
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<Section> getAll(Integer limit) {
        if (null == allSections) {
            List<Object[]> sectionsByArticlesPosted;
            try (EntityManager em = gramPU.createEntityManager()) {
                sectionsByArticlesPosted = em.createNamedQuery("Category.byArticlesPosted").getResultList();
            }
            try {
                double now = OffsetDateTime.now().toInstant().toEpochMilli();
                TreeMap<Double, Section> popularity = new TreeMap<>();
                for (Object[] data : sectionsByArticlesPosted) {
                    Section section = (Section) data[0];
                    if (null == section.getName()) {
                        continue;
                    }
                    double years = (now - ((OffsetDateTime) data[1]).toInstant().toEpochMilli()) / 31536000000.0;
                    double points = ((Long) data[2]).doubleValue();
                    // score = average posts per year since category first started
                    double score = UtilStatic.score(points, years, 1.8);
                    popularity.put(score, section);
                }
                allSections = new ArrayList<>(popularity.values());
                Collections.reverse(allSections);
            } catch (ArrayIndexOutOfBoundsException ex) {
                return new ArrayList<>();
            }
        }
        return allSections;
    }

    /**
     * Return number of sections in DB. Does not include empty/default section.
     *
     * @param term ignored
     * @return
     */
    @Override
    public Long count(Object term) {
        try (EntityManager em = gramPU.createEntityManager()) {
            TypedQuery<Long> qn = em.createNamedQuery("Category.count", Long.class);
            Long output = qn.getSingleResult();
            return output;
        }
    }

    @Override
    public void processArchive(Consumer<Section> operation, Boolean transaction) {
        try (EntityManager em = gramPU.createEntityManager()) {
            if (transaction) {
                em.getTransaction().begin();
                em.createNamedQuery("Category.findAll", Section.class).getResultStream().forEach(operation);
                em.getTransaction().commit();
            } else {
                em.createNamedQuery("Category.findAll", Section.class).getResultStream().forEach(operation);
            }
        }
    }

    @Override
    public CategoryDatabase evict() {
        gramPU.getCache().evict(Section.class);
        allSections = null;
        return this;
    }
}
