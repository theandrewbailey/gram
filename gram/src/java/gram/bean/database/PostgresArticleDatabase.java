package gram.bean.database;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import jakarta.persistence.TypedQuery;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author alpha
 */
public class PostgresArticleDatabase extends ArticleDatabase {

    private static final Logger LOG = Logger.getLogger(PostgresArticleDatabase.class.getName());
    private final Map<String, List<Article>> articleCache = Collections.synchronizedMap(new LinkedHashMap<>(100));
    private final Map<String, List<Article>> suggestionCache = Collections.synchronizedMap(new LinkedHashMap<>(100));

    public PostgresArticleDatabase(EntityManagerFactory emf) {
        super(emf);
    }

    @Override
    public ArticleRepository evict() {
        articleCache.clear();
        return super.evict();
    }

    @Override
    public List<Article> search(Article term, Integer limit) {
        if (null != term.getSuggestion()) {
            if (suggestionCache.containsKey(term.getSuggestion())) {
                return suggestionCache.get(term.getSuggestion());
            }
            List<String> suggestions = new ArrayList<>();
            try (EntityManager em = gramPU.createEntityManager()) {
                Query q = em.createNativeQuery("SELECT word, similarity(?1, word) FROM gram.articlewords WHERE (word % ?1) = TRUE ORDER BY similarity DESC");
                List results = q.setParameter(1, term.getSuggestion()).setMaxResults(limit).getResultList();
                Iterator iter = results.iterator();
                while (iter.hasNext()) {
                    Object[] row = (Object[]) iter.next();
                    if (1 == limit) {
                        Float sim = (Float) row[1];
                        if (0.4f < sim) {
                            suggestions.add(row[0].toString());
                            break;
                        }
                    } else {
                        Float sim = (Float) row[1];
                        if (0.3f < sim) {
                            suggestions.add(row[0].toString());
                        }
                    }
                }
                List<Article> artResults = suggestions.stream().map((suggestion) -> new Article().setSuggestion(suggestion)).toList();
                suggestionCache.put(term.getSuggestion(), List.copyOf(artResults));
                return artResults;
            } catch (NoSuchElementException | NullPointerException n) {
                return null;
            }
        } else if (null != term.getUrl()) {
            if (suggestionCache.containsKey(term.getUrl())) {
                return suggestionCache.get(term.getUrl());
            }
            try (EntityManager em = gramPU.createEntityManager()) {
                TypedQuery<Article> q = em.createQuery("SELECT a FROM Article a WHERE a.postedmarkdown LIKE :url ORDER BY a.posted DESC", Article.class)
                        .setParameter("url", "%" + term.getUrl() + "%");
                if (null != limit) {
                    q.setMaxResults(limit);
                }
                suggestionCache.put(term.getUrl(), List.copyOf(q.getResultList()));
                return q.getResultList();
            } catch (NoSuchElementException | NullPointerException n) {
                return null;
            }
        }
        throw new IllegalArgumentException("No search term provided");
    }

    /**
     *
     * @param term if string, search articles on this string. if Article with
     * suggestion field populated, will return search term suggestions. If
     * CategoryFetcher or Section, will return articles in that Section (passed
     * to ArticleDatabase.search).
     * @param limit
     * @return List of Article
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<Article> search(Object term, Integer limit) {
        if (null == term) {
            throw new IllegalArgumentException("No search term provided.");
        }
        if (articleCache.containsKey(term.toString())) {
            return articleCache.get(term.toString());

        }
        try (EntityManager em = gramPU.createEntityManager()) {
            Query q = em.createNativeQuery("SELECT r.* FROM gram.article r join gram.articlesearchindex s on r.articleid=s.articleid, websearch_to_tsquery(?1) query WHERE query @@ s.searchindexdata ORDER BY ts_rank_cd(s.searchindexdata, query) DESC, r.posted", Article.class
            );
            q.setParameter(1, term.toString());
            if (null != limit) {
                q.setMaxResults(limit);
            }
            if (60 < articleCache.size()) {
                articleCache.remove(articleCache.keySet().iterator().next());
            }
            // cache immutable list to prevent unintended changes
            articleCache.put(term.toString(), List.copyOf(q.getResultList()));
            return q.getResultList();
        }
    }

    @Override
    public void refreshSearch() {
        try (EntityManager em = gramPU.createEntityManager()) {
            em.getTransaction().begin();
            em.createNativeQuery("REFRESH MATERIALIZED VIEW gram.articlewords").executeUpdate();
            em.createNativeQuery("REFRESH MATERIALIZED VIEW gram.articlesearchindex").executeUpdate();
            em.createNativeQuery("ANALYZE gram.articlewords").executeUpdate();
            em.getTransaction().commit();
            LOG.log(Level.FINER, "Article search re-indexed");
        }
    }
}
