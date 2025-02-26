package gram.bean.database;

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

/**
 *
 * @author alpha
 */
public interface ArticleRepository extends Repository<Article> {

    public static final Pattern ARTICLE_TERM = Pattern.compile("(.+?)(?=(?: \\d.*)|(?:[:,] .*)|(?: \\(\\d+\\))|(?: \\()|(?: IX|IV|V?I{0,3})$)");

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
        art.setEtag(Base64.getEncoder().encodeToString(hashArticle(art, art.getCommentCollection())));
        art.setModified(OffsetDateTime.now());
    }

    public static byte[] hashArticle(Article e, Collection<Comment> comments) {
        try {
            MessageDigest sha = HashUtil.getSHA256();
            sha.update(e.getArticletitle().getBytes("UTF-8"));
            sha.update(e.getPostedhtml().getBytes("UTF-8"));
            sha.update(e.getPostedname().getBytes("UTF-8"));
            if (null != e.getSectionid()) {
                sha.update(e.getSectionid().getName().getBytes("UTF-8"));
            }
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
