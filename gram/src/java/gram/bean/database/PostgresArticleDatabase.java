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
import libWebsiteTools.imead.IMEADHolder;
import gram.bean.ArticleRepository;

/**
 *
 * @author alpha
 */
public class PostgresArticleDatabase extends ArticleDatabase {

    private final Map<String, List<Article>> searchCache = Collections.synchronizedMap(new LinkedHashMap<>(100));

    public PostgresArticleDatabase(EntityManagerFactory emf, IMEADHolder imead) {
        super(emf, imead);
    }

    @Override
    public ArticleRepository evict() {
        searchCache.clear();
        return super.evict();
    }

    /**
     *
     * @param term if string, search articles on this string. if Article with
     * suggestion field populated, will return search term suggestions.
     * @param limit
     * @return
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<Article> search(Object term, Integer limit) {
        if (null == term) {
            throw new IllegalArgumentException("No search term provided");
        }
        if (term instanceof Article art) {
            if (null == art.getSuggestion()) {
                throw new IllegalArgumentException("No search suggestion provided");
            }
            List<String> suggestions = new ArrayList<>();
            try (EntityManager em = gramPU.createEntityManager()) {
                Query q = em.createNativeQuery("SELECT word, similarity(?1, word) FROM gram.articlewords WHERE (word % ?1) = TRUE ORDER BY similarity DESC");
                List results = q.setParameter(1, art.getSuggestion()).setMaxResults(limit).getResultList();
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
                return suggestions.stream().map((suggestion) -> new Article().setSuggestion(suggestion)).toList();
            } catch (NoSuchElementException | NullPointerException n) {
            }
            return null;
        } else {
            if (searchCache.containsKey(term.toString())) {
                return searchCache.get(term.toString());
            }
            try (EntityManager em = gramPU.createEntityManager()) {
                Query q = em.createNativeQuery("SELECT r.* FROM gram.article r, websearch_to_tsquery(?1) query WHERE query @@ r.searchindexdata ORDER BY ts_rank_cd(r.searchindexdata, query) DESC, r.posted", Article.class);
                q.setParameter(1, term);
                if (null != limit) {
                    q.setMaxResults(limit);
                }
                if (60 < searchCache.size()) {
                    searchCache.remove(searchCache.keySet().iterator().next());
                }
                // cache immutable list to prevent unintended changes
                searchCache.put(term.toString(), List.copyOf(q.getResultList()));
                return q.getResultList();
            }
        }
    }

    @Override
    public void refreshSearch() {
        try (EntityManager em = gramPU.createEntityManager()) {
            em.getTransaction().begin();
            em.createNativeQuery("REFRESH MATERIALIZED VIEW gram.articlewords").executeUpdate();
            em.createNativeQuery("ANALYZE gram.articlewords").executeUpdate();
            em.getTransaction().commit();
        }
    }
}
