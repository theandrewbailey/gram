package gram.bean.database;

import gram.CategoryFetcher;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author alpha
 */
public abstract class ArticleDatabase implements ArticleRepository {

    private static final Logger LOG = Logger.getLogger(ArticleRepository.class.getName());
    protected static final List<Integer> EXCLUDE_NOTHING = Arrays.asList(new Integer[]{Integer.MIN_VALUE});
    protected final EntityManagerFactory gramPU;

    public ArticleDatabase(EntityManagerFactory emf) {
        this.gramPU = emf;
    }

    @Override
    public List<Article> search(Category term, Integer limit) {
        if (null != term.getName()) {
            try (EntityManager em = gramPU.createEntityManager()) {
                TypedQuery<Article> q = em.createNamedQuery("Article.findByCategory", Article.class).setParameter("category", term.getName());
                q.setParameter("exclude", EXCLUDE_NOTHING);
                q.setMaxResults(limit);
                return q.getResultList();
            }
        }
        LOG.finer("Invalid search term provided, falling back to getAll()");
        return getAll(limit);
    }

    @Override
    public List<Article> search(CategoryFetcher term, Integer limit) {
        try (EntityManager em = gramPU.createEntityManager()) {
            TypedQuery<Article> q = term.getCategory().getName() == null
                    ? em.createNamedQuery("Article.findAll", Article.class)
                    : em.createNamedQuery("Article.findByCategory", Article.class).setParameter("category", term.getCategory().getName());
            q.setParameter("exclude", null == term.getExcludes() ? EXCLUDE_NOTHING : term.getExcludes());
            q.setFirstResult(limit * (term.getCurrentPage() - 1)); // pagination start
            q.setMaxResults(limit);                      // pagination limit
            return q.getResultList();
        }
    }

    /**
     * will save articles (or just one). articles must be pre-processed before
     * adding.
     *
     * @see ArticleProcessor
     * @param articles map of article to category
     * @return last article saved
     */
    @Override
    public List<Article> upsert(Collection<Article> articles) {
        LOG.log(Level.FINER, "Upserting {0} articles", articles.size());
        ArrayList<Article> out = new ArrayList<>(articles.size());
        try (EntityManager em = gramPU.createEntityManager()) {
            em.getTransaction().begin();
            for (Article art : articles) {
                if (null != art.getCategoryid()) {
                    Category category;
                    TypedQuery<Category> q = em.createNamedQuery("Category.findByName", Category.class).setParameter("name", art.getCategoryid().getName());
                    try {
                        category = q.getSingleResult();
                    } catch (NoResultException ex) {
                        category = new Category(art.getCategoryid().getName());
                        em.persist(category);
                    }
                    art.setCategoryid(category);
                }
                art.setModified(OffsetDateTime.now());
                art.setEtag(Base64.getEncoder().encodeToString(ArticleRepository.hashArticle(art, art.getCommentCollection())));
                em.merge(art);
                out.add(art);
                LOG.log(Level.FINE, "Article upserted {0}", new Object[]{art.getArticletitle()});
            }
            em.getTransaction().commit();
            return out;
        }
    }

    @Override
    public Article get(Object articleId) {
        try (EntityManager em = gramPU.createEntityManager()) {
            if (articleId instanceof Integer) {
                return em.find(Article.class, articleId);
            } else if (articleId instanceof String artStr) {
                return em.createNamedQuery("Article.findByURL", Article.class).setParameter("url", artStr.toLowerCase()).setParameter("exclude", EXCLUDE_NOTHING).getSingleResult();
            }
        }
        return null;
    }

    @Override
    public List<Article> getAll(Integer limit) {
        try (EntityManager em = gramPU.createEntityManager()) {
            TypedQuery<Article> q = em.createNamedQuery("Article.findAll", Article.class).setParameter("exclude", EXCLUDE_NOTHING);
            if (null != limit) {
                q.setMaxResults(limit);
            }
            return q.getResultList();
        }
    }

    @Override
    public Article delete(Object articleId) {
        try (EntityManager em = gramPU.createEntityManager()) {
            Article e = em.find(Article.class, articleId);
            em.getTransaction().begin();
            TypedQuery<Long> qn = em.createNamedQuery("Article.countByCategory", Long.class).setParameter("category", e.getCategoryid().getName());
            if (qn.getSingleResult() == 1) {
                em.remove(e);
                em.remove(e.getCategoryid());
            } else {
                em.remove(e);
            }
            em.getTransaction().commit();
            LOG.log(Level.FINE, "Article deleted");
            return e;
        }
    }

    /**
     *
     * @param operation
     * @param transaction Should changes be saved?
     */
    @Override
    public void processArchive(Consumer<Article> operation, Boolean transaction) {
        try (EntityManager em = gramPU.createEntityManager()) {
            if (transaction) {
                em.getTransaction().begin();
                em.createNamedQuery("Article.findAll", Article.class).setParameter("exclude", EXCLUDE_NOTHING).getResultStream().forEach(operation);
                em.getTransaction().commit();
            } else {
                em.createNamedQuery("Article.findAll", Article.class).setParameter("exclude", EXCLUDE_NOTHING).getResultStream().forEach(operation);
            }
        }
    }

    @Override
    public ArticleRepository evict() {
        gramPU.getCache().evict(Article.class);
        return this;
    }

    @Override
    public ArticleDatabase warmCache() {
        getAll(25);
        return this;
    }

    /**
     *
     * @param term ignored
     * @return
     */
    @Override
    public Long count(Object term) {
        try (EntityManager em = gramPU.createEntityManager()) {
            TypedQuery<Long> qn;
            if (term instanceof Category cat) {
                qn = em.createNamedQuery("Article.countByCategory", Long.class).setParameter("category", cat.getName());
            } else {
                qn = em.createNamedQuery("Article.count", Long.class);
            }
            Long output = qn.getSingleResult();
            return output;
        }
    }
}
