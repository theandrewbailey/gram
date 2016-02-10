package toilet.bean;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import libOdyssey.bean.GuardHolder;
import libWebsiteTools.JVMNotSupportedError;
import libWebsiteTools.imead.IMEADHolder;
import libWebsiteTools.sitemap.ChangeFreq;
import libWebsiteTools.sitemap.SiteMaster;
import libWebsiteTools.sitemap.UrlMap;
import toilet.db.Article;
import toilet.tag.ArticleUrl;
import toilet.tag.Categorizer;

/**
 *
 * @author alpha
 */
@Startup
@Singleton
@LocalBean
public class StateCache implements Iterable<UrlMap> {

    public static final String LOCAL_NAME = "java:module/StateCache";
    public static final String PPP = "index_ppp";
    public static final String PAC = "index_pac";
    public static final Pattern INDEX_PATTERN = Pattern.compile(".*?/index/(?:(\\D*)/)?([0-9]*)(?:\\?.*)?$");
    public static final Pattern ARTICLE_PATTERN = Pattern.compile(".*?/article/([0-9]*)(?:/[\\w\\-\\.\\(\\)\\[\\]\\{\\}\\+,%_]*/?)?(?:\\?.*)?(?:#\\d*)?$");
    private int pagesAroundCurrent = 3;
    private int postsPerPage = 5;
    private String canonicalUrl ;
    private volatile List<String> categories;
    private final AtomicReference<List<UrlMap>> urlMap = new AtomicReference<>();
    @PersistenceUnit
    private EntityManagerFactory toiletPU;
    @EJB
    private SiteMaster siteMaster;
    @EJB
    private IMEADHolder imead;
    @EJB
    private EntryRepo entry;

    @PostConstruct
    private void init() {
        siteMaster.addSource(this);
        reset();
    }

    public synchronized void reset() {
        entry.evict();
        categories = new ArrayList<>();
        getArticleCategories();
        urlMap.set(null);
        // pagination params
        if (imead.getValue(PPP) != null) {
            postsPerPage = Integer.parseInt(imead.getValue(PPP));
        }
        if (imead.getValue(PAC) != null) {
            pagesAroundCurrent = Integer.parseInt(imead.getValue(PAC));
        }
        canonicalUrl = imead.getValue(GuardHolder.CANONICAL_URL);
    }

    /**
     * @return list of all groups (categories)
     */
    public synchronized List<String> getArticleCategories() {
        if (null == categories || categories.isEmpty()) {
            EntityManager em = toiletPU.createEntityManager();
            try {
                List<String> cateTemp = em.createQuery("SELECT a.sectionid.name FROM Article a GROUP BY a.sectionid.name ORDER BY COUNT(a.sectionid.name)", String.class).getResultList();
                List<Long> cateCount = em.createQuery("SELECT count(a.sectionid.name) FROM Article a GROUP BY a.sectionid.name ORDER BY COUNT(a.sectionid.name)", Long.class).getResultList();
                List<Date> cateDate = em.createQuery("SELECT min(a.posted) FROM Article a GROUP BY a.sectionid.name ORDER BY COUNT(a.sectionid.name)", Date.class).getResultList();
                double now = new Date().getTime();
                TreeMap<Double, String> popularity = new TreeMap<>();
                for (int i = 0; i < cateTemp.size(); i++) {
                    // score = average posts per year since category first started
                    double score = cateCount.get(i).doubleValue() / ((now - cateDate.get(i).getTime()) / 31556736.0);
                    popularity.put(score, cateTemp.get(i));
                }
                cateTemp = new ArrayList<>(popularity.values());
                cateTemp.remove(imead.getValue(EntryRepo.DEFAULT_CATEGORY));
                Collections.reverse(cateTemp);
                categories = Collections.unmodifiableList(cateTemp);
            } finally {
                em.close();
            }
        }
        return categories;
    }

    @Override
    public Iterator<UrlMap> iterator() {
        if (urlMap.get() == null || urlMap.get().isEmpty()) {
            synchronized (urlMap) {
                if (urlMap.get() != null && !urlMap.get().isEmpty()) {
                    return urlMap.get().iterator();
                }
                List<Article> entries = entry.getArticleArchive(null);
                Collections.reverse(entries);
                List<String> cates = new ArrayList<>(getArticleCategories());
                cates.add("");
                List<UrlMap> tempUrlMap = new ArrayList<>(entries.size() + cates.size() + 10);
                urlMap.set(tempUrlMap);

                int maxArticleID = 1;
                for (Article e : entries) {
                    float difference = maxArticleID - e.getArticleid();
                    difference = 1f - (difference / 50f);
                    if (difference < 0.1f) {
                        difference = 0.1f;
                    }
                    ChangeFreq freq = ChangeFreq.weekly;
                    if (!e.getComments()) {
                        freq = ChangeFreq.never;
                    } else {
                        GregorianCalendar date = new GregorianCalendar();
                        date.add(GregorianCalendar.MONTH, -1);
                        if (date.getTimeInMillis() > e.getPosted().getTime()) {
                            freq = ChangeFreq.monthly;
                        }
                        date.add(GregorianCalendar.MONTH, -5);
                        if (date.getTimeInMillis() > e.getPosted().getTime()) {
                            freq = ChangeFreq.yearly;
                        }
                    }
                    tempUrlMap.add(new UrlMap(ArticleUrl.getUrl(canonicalUrl, e), e.getModified(), freq, String.format("%.1f", difference)));
                }
                for (String c : cates) {
                    IndexFetcher f = new IndexFetcher("/index/" + c);
                    if (!c.isEmpty()) {
                        c = c + "/";
                    }
                    int countTo = f.getCount();
                    if (f.getLast() > countTo) {
                        countTo = f.getLast();
                    }
                    for (int x = 1; x <= countTo; x++) {
                        float difference = 0.5f - (x / 10f);
                        if (difference < 0.1f) {
                            difference = 0.1f;
                        }
                        tempUrlMap.add(new UrlMap(canonicalUrl + "index/" + c + x, null, ChangeFreq.weekly, String.format("%.1f", difference)));
                    }
                }
            }
        }
        return urlMap.get().iterator();
    }

    public IndexFetcher getIndexFetcher(String inURI){
        return new IndexFetcher(inURI);
    }

    public class IndexFetcher {

        private int page = 1;
        private String group = null;
        private int count = 0;
        private int first = 1;

        private IndexFetcher(String inURI) {
            String URI = inURI;
            try {
                page = Integer.valueOf(getPageNumber(URI));
                group = getCategoryFromURI(URI.replace("%20", " "));
            } catch (Exception e) {
                return;
            }
            try {
                Integer.valueOf(group);
                group = null;
            } catch (Exception e) {
            }
            if (imead.getValue(EntryRepo.DEFAULT_CATEGORY).equals(group)) {
                group = null;
            }

            // get total of all, to display number of pages limit
            if (count == 0) {
                double counted = entry.countArticlesInSection(group);
                count = (int) Math.ceil(counted / postsPerPage);
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
        }

        /**
         * @param URI
         * @return
         */
        private String getCategoryFromURI(String URI) {
            Matcher m = INDEX_PATTERN.matcher(URI);
            if (m.matches()) {
                if (null != m.group(1)) {
                    try {
                        return URLDecoder.decode(m.group(1), "UTF-8");
                    } catch (UnsupportedEncodingException ex) {
                        // not gonna happen
                        throw new JVMNotSupportedError(ex);
                    }
                }
            }
            return imead.getValue(EntryRepo.DEFAULT_CATEGORY);
        }

        public List<Article> getArticles() {
            return entry.getSection(group, page, postsPerPage);
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

        public String getLink() {
            return Categorizer.getUrl(canonicalUrl, group, null);
        }

        public String getDescription() {
            StringBuilder d=new StringBuilder(70).append(imead.getValue(UtilBean.SITE_TITLE)).append(", ");
            if (null==group){
                d.append("all categories, page ");
            }else{
                d.append(group).append(" category, page ");
            }
            return d.append(page).toString();
        }

        public boolean isValid() {
            return group != null && page != 0;
        }

        public String getGroup() {
            return group;
        }

        public int getPage() {
            return page;
        }
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
        throw new RuntimeException();
    }

}
