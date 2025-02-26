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
    private static final List<Integer> EXCLUDE_NOTHING = Arrays.asList(new Integer[]{Integer.MIN_VALUE});
    protected final EntityManagerFactory gramPU;

    public ArticleDatabase(EntityManagerFactory emf) {
        this.gramPU = emf;
    }

    @Override
    public List<Article> search(Object term, Integer limit) {
        if (term instanceof Section category) {
            if (null != category.getName()) {
                try (EntityManager em = gramPU.createEntityManager()) {
                    TypedQuery<Article> q = em.createNamedQuery("Article.findByCategory", Article.class).setParameter("category", category.getName());
                    q.setParameter("exclude", EXCLUDE_NOTHING);
                    q.setMaxResults(limit);
                    return q.getResultList();
                }
            }
        } else if (term instanceof CategoryFetcher f) {
            try (EntityManager em = gramPU.createEntityManager()) {
                TypedQuery<Article> q = f.getCategory().getName() == null
                        ? em.createNamedQuery("Article.findAll", Article.class)
                        : em.createNamedQuery("Article.findByCategory", Article.class).setParameter("category", f.getCategory().getName());
                q.setParameter("exclude", null == f.getExcludes() ? EXCLUDE_NOTHING : f.getExcludes());
                q.setFirstResult(limit * (f.getCurrentPage() - 1)); // pagination start
                q.setMaxResults(limit);                      // pagination limit
                return q.getResultList();
            }
        }
        LOG.finer("Invalid search term provided, falling back to getAll()");
        return getAll(limit);
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
        Article dbArt;
        ArrayList<Article> out = new ArrayList<>(articles.size());
        try (EntityManager em = gramPU.createEntityManager()) {
            em.getTransaction().begin();
            for (Article art : articles) {
                boolean getnew = art.getArticleid() == null;
                dbArt = getnew ? new Article(art.getUuid()) : em.find(Article.class, art.getArticleid());
                // TODO: figure out how to upsert
                dbArt.setPosted(art.getPosted() == null ? dbArt.getModified() : art.getPosted());
                dbArt.setComments(art.getComments());
                dbArt.setCommentCollection(art.getComments() ? dbArt.getCommentCollection() : null);
                dbArt.setArticletitle(art.getArticletitle());
                dbArt.setPostedhtml(art.getPostedhtml());
                dbArt.setPostedmarkdown(art.getPostedmarkdown());
                dbArt.setPostedname(art.getPostedname());
                dbArt.setDescription(art.getDescription());
                dbArt.setSummary(art.getSummary());
                dbArt.setImageurl(art.getImageurl());
                dbArt.setSuggestion(art.getSuggestion());
                dbArt.setModified(OffsetDateTime.now());

                if (null == art.getSectionid()) {
                    dbArt.setSectionid(null);
                } else if (null != art.getSectionid() && (null == dbArt.getSectionid() || !dbArt.getSectionid().getName().equals(art.getSectionid().getName()))) {
                    Section category;
                    TypedQuery<Section> q = em.createNamedQuery("Category.findByName", Section.class).setParameter("name", art.getSectionid().getName());
                    try {
                        category = q.getSingleResult();
                    } catch (NoResultException ex) {
                        category = new Section(art.getSectionid().getName());
                        em.persist(category);
                    }
                    dbArt.setSectionid(category);
                }
                dbArt.setEtag(Base64.getEncoder().encodeToString(ArticleRepository.hashArticle(dbArt, dbArt.getCommentCollection())));
                if (getnew) {
                    em.persist(dbArt);
                }
                out.add(dbArt);
                LOG.log(Level.FINE, "Article added {0}", new Object[]{art.getArticletitle()});
            }
            em.getTransaction().commit();
            return out;
        } catch (Throwable x) {
            LOG.throwing(ArticleRepository.class.getCanonicalName(), "addArticles", x);
            throw x;
        }
    }

    @Override
    public Article get(Object articleId) {
        try (EntityManager em = gramPU.createEntityManager()) {
            return em.find(Article.class, articleId);
        }
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
            if (null == articleId) {
                em.getTransaction().begin();
                for (Comment c : em.createNamedQuery("Comment.findAll", Comment.class).getResultList()) {
                    em.remove(c);
                }
                for (Article a : em.createNamedQuery("Article.findAll", Article.class).setParameter("exclude", EXCLUDE_NOTHING).getResultList()) {
                    em.remove(a);
                }
                for (Section s : em.createNamedQuery("Category.findAll", Section.class).getResultList()) {
                    em.remove(s);
                }
                em.createNativeQuery("ALTER SEQUENCE gram.comment_commentid_seq RESTART; ALTER SEQUENCE gram.article_articleid_seq RESTART;").executeUpdate();
                em.getTransaction().commit();
                refreshSearch();
                LOG.log(Level.FINE, "All articles and comments deleted");
                return null;
            } else {
                Article e = em.find(Article.class, articleId);
                em.getTransaction().begin();
                TypedQuery<Long> qn = em.createNamedQuery("Article.countByCategory", Long.class).setParameter("category", e.getSectionid().getName());
                if (qn.getSingleResult() == 1) {
                    em.remove(e);
                    em.remove(e.getSectionid());
                } else {
                    em.remove(e);
                }
                em.getTransaction().commit();
                LOG.log(Level.FINE, "Article deleted");
                return e;
            }
        }
    }

    /**
     *
     * @param operation This will be run in parallel.
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

    /**
     *
     * @param term ignored
     * @return
     */
    @Override
    public Long count(Object term) {
        try (EntityManager em = gramPU.createEntityManager()) {
            TypedQuery<Long> qn;
            if (term instanceof Section sect) {
                qn = em.createNamedQuery("Article.countByCategory", Long.class).setParameter("category", sect.getName());
            } else {
                qn = em.createNamedQuery("Article.count", Long.class);
            }
            Long output = qn.getSingleResult();
            return output;
        }
    }
}
