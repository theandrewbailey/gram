package gram;

import gram.bean.GramTenant;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import libWebsiteTools.JVMNotSupportedError;
import gram.bean.ArticleRepository;
import gram.bean.database.Article;
import gram.bean.database.Section;

/**
 *
 * @author alpha
 */
public class IndexFetcher {

    public static final String POSTS_PER_PAGE = "site_pagenation_post_count";
    public static final String PAGES_AROUND_CURRENT = "site_pagenation_around_current";
    public static final Pattern INDEX_PATTERN = Pattern.compile(".*?(?:/index)?(?:/(\\D*?))?(?:/([0-9]*)(?:\\?.*)?)?(?:\\.html)?$");
    public static final Pattern ARTICLE_PATTERN = Pattern.compile(".*?/(?:(?:article)|(?:comments)|(?:amp)|(?:edit))/([0-9]*)(?:/[\\w\\-\\.\\(\\)\\[\\]\\{\\}\\+,%_]*/?)?(?:\\?.*)?(?:#.*)?$");
    private int page = 1;
    private Section section = null;
    private int count = 0;
    private int first = 1;
    private final int pagesAroundCurrent;
    private List<Article> articles = Collections.<Article>emptyList();
    private String URI;
    private Long counted = 0L;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(200).append("Class: ").append(IndexFetcher.class.getName());
        sb.append(", URI: ").append(URI);
        sb.append(", section: ").append(section);
        sb.append(", article count: ").append(articles.size());
        sb.append(", current page: ").append(page);
        sb.append(", page count: ").append(count);
        return sb.toString();
    }

    /**
     * detect page numbers from URI
     *
     * @param URI
     * @return
     */
    public static String getPageNumber(String URI) {
        Matcher m = INDEX_PATTERN.matcher(URI);
        if (m.matches()) {
            return m.group(2);
        }
        m = ARTICLE_PATTERN.matcher(URI);
        if (m.matches()) {
            return m.group(2);
        }
        return "1";
    }

    /**
     * @param URI
     * @return
     * @throws RuntimeException
     */
    public static String getArticleIdFromURI(String URI) {
        Matcher m = ARTICLE_PATTERN.matcher(URI);
        if (m.matches()) {
            if (null != m.group(1)) {
                return m.group(1);
            }
        }
        throw new NumberFormatException("Can't parse article ID from " + URI);
    }

    public static Article getArticleFromURI(GramTenant ten, String URI) {
        try {
            return ten.getArts().get(Integer.valueOf(getArticleIdFromURI(URI)));
        } catch (NumberFormatException x) {
            return null;
        }
    }

    /**
     * @param URI
     * @return
     */
    private static String getCategoryFromURI(String URI, String defaultCategory) {
        Matcher m = INDEX_PATTERN.matcher(URI.replace("%20", " "));
        if (m.matches()) {
            if (null != m.group(1) || null != m.group(2)) {
                try {
                    String cate = m.group(1);
                    return null == cate || cate.isEmpty() ? defaultCategory : URLDecoder.decode(cate, "UTF-8");
                } catch (UnsupportedEncodingException ex) {
                    // not gonna happen
                    throw new JVMNotSupportedError(ex);
                }
            }
        }
        if ("/".equals(URI) || URI.isEmpty() || "/index.html".equals(URI) || "/index".equals(URI)) {
            return defaultCategory;
        }
        throw new IllegalArgumentException("Invalid category URL: " + URI);
    }

    public IndexFetcher(GramTenant ten, String URI) {
        this.URI = URI;
        String sect;
        int ppp = null != ten.getImeadValue(POSTS_PER_PAGE) ? Integer.parseInt(ten.getImeadValue(POSTS_PER_PAGE)) : 7;
        pagesAroundCurrent = null != ten.getImeadValue(PAGES_AROUND_CURRENT) ? Integer.parseInt(ten.getImeadValue(PAGES_AROUND_CURRENT)) : 3;
        try {
            String pagenum = getPageNumber(URI);
            if (null != pagenum) {
                page = pagenum.isEmpty() ? 1 : Integer.parseInt(pagenum);
            }
            sect = getCategoryFromURI(URI, ten.getImeadValue(ArticleRepository.DEFAULT_CATEGORY));
        } catch (RuntimeException e) {
            return;
        }
        try {
            Integer.valueOf(sect);
            sect = null;
        } catch (NumberFormatException e) {
        }
        if (null != sect && sect.equals(ten.getImeadValue(ArticleRepository.DEFAULT_CATEGORY))) {
            sect = null;
        }
        // get total of all, to display number of pages limit
        if (count == 0) {
            counted = ten.getSects().count(sect);
//            if (null == section) {
//                counted = ten.getArts().count(null);
//            } else {
//                Section thisSection = ten.getSects().get(section);
//                if (null != thisSection) {
////                    counted = thisSection.getArticleCollection().size();
//                    counted = ten.getSects().count(section);
//                }
//            }
            count = (int) Math.ceil(counted / ppp);
        }
        // wierd algoritim to determine how many pagination links to other pages on this page
        if (page + pagesAroundCurrent > count) {
            first = count - pagesAroundCurrent * 2;
        } else if (page - pagesAroundCurrent > 0) {
            first = page - pagesAroundCurrent;
        }
        if (first < 1) {
            first = 1;
        }
        articles = ten.getArts().getBySection(sect, page, ppp, null);
        if (null != sect && !articles.isEmpty()) {
            section = articles.get(0).getSectionid();
        }
        if (null == section) {
            section = new Section(null, ten.getImeadValue(ArticleRepository.DEFAULT_CATEGORY), null);
        }
    }

    public List<Article> getArticles() {
        return articles;
    }

    public int getCount() {
        return count;
    }

    public int getFirst() {
        return first;
    }

    public int getLast() {
        return Math.min(first + pagesAroundCurrent * 2, count);
    }

    public boolean isValid() {
        return section != null && page != 0;
    }

    public Section getSection() {
        return section;
    }

    public int getPage() {
        return page;
    }
}
