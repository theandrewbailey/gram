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
import libWebsiteTools.imead.IMEADHolder;
import gram.UtilStatic;
import gram.bean.ArticleRepository;

/**
 *
 * @author alpha
 */
public class SectionDatabase implements Repository<Section> {

    private final EntityManagerFactory gramPU;
    private final IMEADHolder imead;
    private List<Section> allSections;

    public SectionDatabase(EntityManagerFactory emf, IMEADHolder imead) {
        this.gramPU = emf;
        this.imead = imead;
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
                return em.createNamedQuery("Section.findByName", Section.class).setParameter("name", id).getSingleResult();
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
//            Article empty = new Article();
            List<Object[]> sectionsByArticlesPosted;
            try (EntityManager em = gramPU.createEntityManager()) {
                sectionsByArticlesPosted = em.createNamedQuery("Section.byArticlesPosted").getResultList();
            }
            try {
                double now = OffsetDateTime.now().toInstant().toEpochMilli();
                TreeMap<Double, Section> popularity = new TreeMap<>();
                for (Object[] data : sectionsByArticlesPosted) {
                    Section section = (Section) data[0];
                    if (section.getName().equals(imead.getValue(ArticleRepository.DEFAULT_CATEGORY))) {
                        continue;
                    }
                    double years = (now - ((OffsetDateTime) data[1]).toInstant().toEpochMilli()) / 31536000000.0;
                    double points = ((Long) data[2]).doubleValue();
                    // score = average posts per year since category first started
                    double score = UtilStatic.score(points, years, 1.8);
                    popularity.put(score, section);
//                    List<Article> articles = new ArrayList<>(section.getArticleCollection().size());
//                    for (int i = 0; i < section.getArticleCollection().size(); i++) {
//                        articles.add(empty);
//                    }
//                    section.setArticleCollection(articles);
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
     *
     * @param term ignored
     * @return
     */
    @Override
    public Long count(Object term) {
        try (EntityManager em = gramPU.createEntityManager()) {
            TypedQuery<Long> qn = null == term ? em.createNamedQuery("Article.count", Long.class)
                    : em.createNamedQuery("Article.countBySection", Long.class).setParameter("section", term);
            Long output = qn.getSingleResult();
            return output;
        }
    }

    @Override
    public void processArchive(Consumer<Section> operation, Boolean transaction) {
        try (EntityManager em = gramPU.createEntityManager()) {
            if (transaction) {
                em.getTransaction().begin();
                em.createNamedQuery("Section.findAll", Section.class).getResultStream().forEach(operation);
                em.getTransaction().commit();
            } else {
                em.createNamedQuery("Section.findAll", Section.class).getResultStream().forEach(operation);
            }
        }
    }

    @Override
    public SectionDatabase evict() {
        gramPU.getCache().evict(Section.class);
        allSections = null;
        return this;
    }
}
