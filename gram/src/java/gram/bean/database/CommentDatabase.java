package gram.bean.database;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.TypedQuery;
import libWebsiteTools.Repository;

/**
 *
 * @author alpha
 */
public class CommentDatabase implements Repository<Comment> {

    private static final Logger LOG = Logger.getLogger(CommentDatabase.class.getName());
    private final EntityManagerFactory gramPU;

    public CommentDatabase(EntityManagerFactory emf) {
        this.gramPU = emf;
    }

    /**
     * only supports adding comments.
     *
     * @param entities
     * @return
     */
    @Override
    public List<Comment> upsert(Collection<Comment> entities) {
        ArrayList<Comment> out = new ArrayList<>(entities.size());
        HashSet<Integer> updatedArticles = new HashSet<>(entities.size() * 2);
        try (EntityManager em = gramPU.createEntityManager()) {
            em.getTransaction().begin();
            for (Comment c : entities) {
                Article art = em.find(Article.class, c.getArticleid().getArticleid());
                if (c.getPosted() == null) {
                    c.setPosted(OffsetDateTime.now());
                }
                c.setArticleid(art);
                em.persist(c);
                out.add(c);
                LOG.log(Level.FINE, "Comment added");
                updatedArticles.add(art.getArticleid());
            }
            em.getTransaction().commit();
            em.getTransaction().begin();
            for (Integer articleid : updatedArticles) {
                Article a = em.find(Article.class, articleid);
                em.refresh(a);
                ArticleRepository.updateArticleHash(a);
            }
            em.getTransaction().commit();
            return out;
        }
    }

    @Override
    public Comment get(Object commentId) {
        try (EntityManager em = gramPU.createEntityManager()) {
            return em.find(Comment.class, commentId);
        }
    }

    @Override
    public List<Comment> search(Object term, Integer limit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Comment delete(Object commentId) {
        try (EntityManager em = gramPU.createEntityManager()) {
            em.getTransaction().begin();
            Comment c = em.find(Comment.class, commentId);
            Article e = c.getArticleid();
            em.remove(c);
            em.getTransaction().commit();
            LOG.log(Level.FINE, "Comment deleted");
            em.refresh(e);
            em.getTransaction().begin();
            ArticleRepository.updateArticleHash(e);
            em.getTransaction().commit();
            return c;
        }
    }

    @Override
    public List<Comment> getAll(Integer limit) {
        try (EntityManager em = gramPU.createEntityManager()) {
            TypedQuery<Comment> q = em.createNamedQuery("Comment.findAll", Comment.class);
            if (null != limit) {
                q.setMaxResults(limit);
            }
            return q.getResultList();
        }
    }

    @Override
    public void processArchive(Consumer<Comment> operation, Boolean transaction) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CommentDatabase evict() {
        gramPU.getCache().evict(Comment.class);
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
            TypedQuery<Long> qn = em.createNamedQuery("Comment.count", Long.class);
            Long output = qn.getSingleResult();
            LOG.log(Level.FINER, "Counted comments, got {0}", new Object[]{output});
            return output;
        }
    }
}
