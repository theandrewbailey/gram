package toilet.bean;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import libWebsiteTools.security.HashUtil;
import libWebsiteTools.JVMNotSupportedError;
import libWebsiteTools.Repository;
import toilet.db.Article;
import toilet.db.Comment;

/**
 *
 * @author alpha
 */
public interface ArticleRepository extends Repository<Article> {

    public static final String DEFAULT_CATEGORY = "site_defaultCategory";
    public static final Pattern ARTICLE_TERM = Pattern.compile("(.+?)(?=(?: \\d.*)|(?:[:,] .*)|(?: \\(\\d+\\))|(?: \\()|(?: IX|IV|V?I{0,3})$)");

    /**
     *
     * @param sect Section to get articles from, can be null
     * @param page Get the nth page, can be null with perPage
     * @param perPage How many articles per page, can be null with page
     * @param exclude Don't include these articles (by ID), can be null
     * @return A list that represents a single page of articles
     */
    public abstract List<Article> getBySection(String sect, Integer page, Integer perPage, List<Integer> exclude);

    /**
     *
     * @param searchTerm Perform a search based on this term
     * @param limit Return maximum of this many. Can be null to return all possible matches.
     * @return Articles that match the searchTerm, sorted by relevance
     */
    public abstract List<Article> search(String searchTerm, Integer limit);

    /**
     *
     * @param searchTerm A possible search term
     * @param limit Only return this many
     * @return A list of search terms that might be better (e.g. fixed spelling)
     */
    public abstract List<String> getSearchSuggestion(String searchTerm, Integer limit);

    /**
     * Perform any kind of re-index operation necessary for search. Needs done
     * when any article is updated or changed.
     */
    public abstract void refreshSearch();

    /**
     * Try to guess an appropriate search term to retrieve similar articles.
     *
     * @param art Article to get an appropriate search term from
     * @return String suitable to pass to article search to retrieve similar
     * articles
     */
    public static String getArticleSuggestionTerm(Article art) {
        String term = art.getArticletitle();
        if (null == term) {
            return "";
        }
        Matcher articleMatch = ARTICLE_TERM.matcher(term);
        if (articleMatch.find()) {
            term = articleMatch.group(1).trim();
        }
        return term;
    }

    public static void updateArticleHash(Article art) {
        art.setEtag(Base64.getEncoder().encodeToString(hashArticle(art, art.getCommentCollection(), art.getSectionid().getName())));
        art.setModified(OffsetDateTime.now());
    }

    public static byte[] hashArticle(Article e, Collection<Comment> comments, String sect) {
        try {
            MessageDigest sha = HashUtil.getSHA256();
            sha.update(e.getArticletitle().getBytes("UTF-8"));
            sha.update(e.getPostedhtml().getBytes("UTF-8"));
            sha.update(sect.getBytes("UTF-8"));
            sha.update(e.getPostedname().getBytes("UTF-8"));
            if (e.getDescription() != null) {
                sha.update(e.getDescription().getBytes("UTF-8"));
            }
            sha.update(e.getModified().toString().getBytes("UTF-8"));
            if (comments != null) {
                for (Comment c : comments) {
                    sha.update(c.getPostedhtml().getBytes("UTF-8"));
                    sha.update(c.getPosted().toString().getBytes("UTF-8"));
                    sha.update(c.getPostedname().getBytes("UTF-8"));
                }
            }
            return sha.digest();
        } catch (UnsupportedEncodingException enc) {
            throw new JVMNotSupportedError(enc);
        }
    }
}
