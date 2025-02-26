package gram.bean.database;

import gram.CategoryFetcher;
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

/**
 *
 * @author alpha
 */
public class PostgresArticleDatabase extends ArticleDatabase {

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
        if (term instanceof CategoryFetcher) {
            return super.search(term, limit);
        } else if (term instanceof Section) {
            return super.search(term, limit);
        } else if (term instanceof Article art) {
            if (null != art.getSuggestion()) {
                if (suggestionCache.containsKey(art.getSuggestion())) {
                    return suggestionCache.get(art.getSuggestion());
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
                    List<Article> artResults = suggestions.stream().map((suggestion) -> new Article().setSuggestion(suggestion)).toList();
                    suggestionCache.put(art.getSuggestion(), List.copyOf(artResults));
                    return artResults;
                } catch (NoSuchElementException | NullPointerException n) {
                    return null;
                }
            } else if (null != art.getUrl()) {
                if (suggestionCache.containsKey(art.getUrl())) {
                    return suggestionCache.get(art.getUrl());
                }
                try (EntityManager em = gramPU.createEntityManager()) {
                    TypedQuery<Article> q = em.createQuery("SELECT a FROM Article a WHERE a.postedmarkdown LIKE :url ORDER BY a.posted DESC", Article.class)
                            .setParameter("url", "%" + art.getUrl() + "%");
                    if (null != limit) {
                        q.setMaxResults(limit);
                    }
                    suggestionCache.put(art.getUrl(), List.copyOf(q.getResultList()));
                    return q.getResultList();
                } catch (NoSuchElementException | NullPointerException n) {
                    return null;
                }
            }
            throw new IllegalArgumentException("No search term provided");
        } else {
            if (articleCache.containsKey(term.toString())) {
                return articleCache.get(term.toString());
            }
            try (EntityManager em = gramPU.createEntityManager()) {
                Query q = em.createNativeQuery("SELECT r.* FROM gram.article r join gram.articlesearchindex s on r.articleid=s.articleid, websearch_to_tsquery(?1) query WHERE query @@ s.searchindexdata ORDER BY ts_rank_cd(s.searchindexdata, query) DESC, r.posted", Article.class);
                q.setParameter(1, term);
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
    }

    @Override
    public void refreshSearch() {
        try (EntityManager em = gramPU.createEntityManager()) {
            em.getTransaction().begin();
            em.createNativeQuery("REFRESH MATERIALIZED VIEW gram.articlewords").executeUpdate();
            em.createNativeQuery("REFRESH MATERIALIZED VIEW gram.articlesearchindex").executeUpdate();
            em.createNativeQuery("ANALYZE gram.articlewords").executeUpdate();
            em.getTransaction().commit();
        }
    }
}
