package gram;

import gram.bean.GramTenant;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import libWebsiteTools.JVMNotSupportedError;
import gram.bean.database.Article;
import gram.bean.database.Section;
import java.util.Iterator;
import java.util.stream.Collectors;

/**
 *
 * @author alpha
 */
public class CategoryFetcher implements Iterable<Article> {

    public static final String POSTS_PER_PAGE = "site_pagenation_post_count";
    public static final String PAGES_AROUND_CURRENT = "site_pagenation_around_current";
    public static final Pattern INDEX_PATTERN = Pattern.compile(".*?(?:/index)?(?:/(\\D*?))?(?:/([0-9]*)(?:\\?.*)?)?(?:\\.html)?$");
    private int page = 1;
    private Section category = new Section();
    private final int postsPerPage;
    private final int pagesAroundCurrent;
    private List<Article> articles;
    private List<Integer> excludes;
    private Long articleCount;
    private final GramTenant ten;

    /**
     * @param URL
     * @return page number from URL
     */
    private static String getPageNumber(String URL) {
        Matcher m = INDEX_PATTERN.matcher(URL);
        if (m.matches()) {
            return m.group(2);
        }
        return "1";
    }

    /**
     * @param URL
     * @return category name or null
     */
    private static String getCategoryFromURL(String URL) {
        Matcher m = INDEX_PATTERN.matcher(URL.replace("%20", " "));
        if (m.matches()) {
            if (null != m.group(1) || null != m.group(2)) {
                try {
                    String catName = m.group(1);
                    return null == catName || catName.isEmpty() ? null : URLDecoder.decode(catName, "UTF-8");
                } catch (UnsupportedEncodingException ex) {
                    // not gonna happen
                    throw new JVMNotSupportedError(ex);
                }
            }
        }
        if ("/".equals(URL) || URL.isEmpty() || "/index.html".equals(URL) || "/index".equals(URL)) {
            return null;
        }
        throw new IllegalArgumentException("Invalid category URL: " + URL);
    }

    public CategoryFetcher(GramTenant ten, String URL) {
        this.ten = ten;
        postsPerPage = Integer.parseInt(ten.getImeadValue(POSTS_PER_PAGE));
        pagesAroundCurrent = Integer.parseInt(ten.getImeadValue(PAGES_AROUND_CURRENT));
//        try {
        String pagenum = getPageNumber(URL);
        if (null != pagenum) {
            page = pagenum.isEmpty() ? 1 : Integer.parseInt(pagenum);
        }
        String catName = getCategoryFromURL(URL);
        if (null != catName) {
            category = ten.getCategories().get(catName);
        }
        if (null != catName && null == category) {
            throw new IllegalArgumentException("Category " + catName + " not found.");
        }
        if (null != catName && getArticles().isEmpty()) {
            throw new IllegalArgumentException("Category " + catName + ", page " + getCurrentPage() + " not found.");
        }
//        } catch (RuntimeException e) {
//            return;
//        }
    }

    public List<Article> getArticles() {
        if (null == articles) {
            articles = ten.getArts().search(this, getPostsPerPage());
        }
        return articles;
    }

    @Override
    public Iterator<Article> iterator() {
        return getArticles().iterator();
    }

    public CategoryFetcher without(List<Article> excludes) {
        if (null != excludes && !excludes.isEmpty()) {
            this.excludes = excludes.stream().mapToInt((t) -> {
                return t.getArticleid();
            }).boxed().collect(Collectors.toList());
        }
        return this;
    }

    public int getPageCount() {
        return (int) Math.ceil(getArticleCount().floatValue() / getPostsPerPage());
    }

    public Long getArticleCount() {
        if (null == articleCount) {
            if (null == category.getName()) {
                articleCount = ten.getArts().count(null);
            } else {
                articleCount = ten.getArts().count(category);
            }
        }
        return articleCount;
    }

    public int getFirstPage() {
        int count = getPageCount();
        // wierd algoritim to determine how many pagination links to other pages on this page
        if (page + pagesAroundCurrent > count) {
            return Math.max(1, count - pagesAroundCurrent * 2);
        } else if (page - pagesAroundCurrent > 0) {
            return page - pagesAroundCurrent;
        }
        return 1;
    }

    public int getLastPage() {
        return Math.min(getFirstPage() + pagesAroundCurrent * 2, getPageCount());
    }

    public Section getCategory() {
        return category;
    }

    public int getCurrentPage() {
        return page;
    }

    public List<Integer> getExcludes() {
        return excludes;
    }

    public int getPostsPerPage() {
        return postsPerPage;
    }
}
