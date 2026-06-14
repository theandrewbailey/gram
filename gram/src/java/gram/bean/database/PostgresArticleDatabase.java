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
import java.util.logging.Level;
import java.util.logging.Logger;
import libWebsiteTools.SearchableRepository;

/**
 *
 * @author alpha
 */
public class PostgresArticleDatabase extends ArticleDatabase implements SearchableRepository<Article> {

    private final int CACHE_SIZE = 100;
    private static final Logger LOG = Logger.getLogger(PostgresArticleDatabase.class.getName());
    private final Map<Object, List<Article>> articleCache = Collections.synchronizedMap(new LinkedHashMap<>(CACHE_SIZE * 2));
    private final Map<String, List<Article>> suggestionCache = Collections.synchronizedMap(new LinkedHashMap<>(CACHE_SIZE * 2));

    public PostgresArticleDatabase(EntityManagerFactory emf) {
        super(emf);
    }

    @Override
    public ArticleRepository evict() {
        articleCache.clear();
        suggestionCache.clear();
        return super.evict();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Article> search(Article term, Integer limit) {
        if (null == term) {
            throw new IllegalArgumentException("No search term provided.");
        }
        if (null != term.getArticleid() && articleCache.containsKey(term.getArticleid())) {
            return articleCache.get(term.getArticleid());
        } else if (articleCache.containsKey(term.getSuggestion())) {
            return articleCache.get(term.getSuggestion());
        }
        List<Article> out = null;
        List<Object> params = new ArrayList<>();
        StringBuilder cte = new StringBuilder(1000).append("with fuzzy as (select r.*, (row_number() OVER (ORDER BY ts_rank_cd(s.searchindexdata, query) DESC, r.posted DESC)) as priority from gram.article r join gram.articlesearchindex s on r.articleid=s.articleid, ")
                .append("websearch_to_tsquery(?) query WHERE query @@ s.searchindexdata ORDER BY priority LIMIT ").append(limit).append(")");
        params.add(term.getSuggestion());
        StringBuilder union = new StringBuilder(200).append("select * from ((select * from fuzzy)");
        if (null != term.getUrl()) {
            cte.append(",urlrefs as (select r.*, (row_number() OVER (ORDER BY r.posted DESC))+100 as priority from gram.article r WHERE r.postedmarkdown LIKE '?' ORDER BY priority LIMIT ").append(limit).append(")");
            union.append(" union (select * from urlrefs)");
            params.add("%" + term.getUrl());
        }
        if (null != term.getCategoryid() && null != term.getCategoryid().getCategoryid()) {
            cte.append(",artsect as (select r.*, (row_number() OVER (ORDER BY r.posted DESC))+1000 as priority from gram.article r where r.categoryid=").append(term.getCategoryid().getCategoryid()).append(" ORDER BY priority LIMIT ").append(limit).append(")");
            union.append(" union (select * from artsect)");
        }
        if (null != term.getArticleid()) {
            cte.append(",arts as (select r.*, (row_number() OVER (ORDER BY r.posted DESC))+10000 as priority from gram.article r ORDER BY priority LIMIT ").append(limit).append(") ");
            union.append(" union (select * from arts)");
            union.append(") art where art.articleid not in (").append(term.getArticleid()).append(") ORDER BY priority LIMIT ").append(limit).append(";");
        } else {
            union.append(") art ORDER BY priority LIMIT ").append(limit).append(";");
        }
        try (EntityManager em = gramPU.createEntityManager()) {
            Query q = em.createNativeQuery(cte.append(union).toString(), Article.class);
            int pCount = 1;
            for (Object p : params) {
                q.setParameter(pCount++, p);
            }
            out = q.getResultList();
        } catch (Exception x) {
        }
        if (CACHE_SIZE < articleCache.size()) {
            articleCache.remove(articleCache.entrySet().iterator().next().getKey());
        }
        if (null != term.getArticleid()) {
            articleCache.put(term.getArticleid(), Collections.unmodifiableList(out));
        } else {
            articleCache.put(term.getSuggestion(), Collections.unmodifiableList(out));
        }
        return out;
    }

    /**
     *
     * @param term
     * @param limit
     * @return List of Article
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<Article> search(Object term, Integer limit) {
        if (null == term) {
            throw new IllegalArgumentException("No search term provided.");
        }
        if (term instanceof String) {
            List<String> suggestions = new ArrayList<>();
            if (suggestionCache.containsKey(term.toString())) {
                return suggestionCache.get(term.toString());
            }
            try (EntityManager em = gramPU.createEntityManager()) {
                Query q = em.createNativeQuery("SELECT word, similarity(?1, word) FROM gram.articlewords WHERE (word % ?1) = TRUE ORDER BY similarity DESC");
                List results = q.setParameter(1, term).setMaxResults(limit).getResultList();
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
                if (CACHE_SIZE < suggestionCache.size()) {
                    suggestionCache.remove(suggestionCache.entrySet().iterator().next().getKey());
                }
                suggestionCache.put(term.toString(), List.copyOf(artResults));
                return artResults;
            } catch (NullPointerException n) {
                return null;
            }
        }
        return null;
    }

    @Override
    public void refreshSearch() {
        try (EntityManager em = gramPU.createEntityManager()) {
            em.getTransaction().begin();
            em.createNativeQuery("ANALYZE gram.article").executeUpdate();
            em.createNativeQuery("REFRESH MATERIALIZED VIEW gram.articlewords").executeUpdate();
            em.createNativeQuery("REFRESH MATERIALIZED VIEW gram.articlesearchindex").executeUpdate();
            em.createNativeQuery("ANALYZE gram.articlewords").executeUpdate();
            em.getTransaction().commit();
            LOG.log(Level.FINER, "Article search re-indexed");
        }
    }
}
