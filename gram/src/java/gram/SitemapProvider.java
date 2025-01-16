package gram;

import gram.bean.GramTenant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import libWebsiteTools.security.SecurityRepo;
import libWebsiteTools.sitemap.ChangeFreq;
import libWebsiteTools.sitemap.UrlMap;
import gram.bean.database.Article;
import gram.bean.database.Section;
import gram.tag.ArticleUrl;

/**
 *
 * @author alpha
 */
public class SitemapProvider implements Iterable<UrlMap> {

    private final GramTenant ten;

    public SitemapProvider(GramTenant ten) {
        this.ten = ten;
    }

    @Override
    public Iterator<UrlMap> iterator() {
        List<Article> entries = new ArrayList<>(ten.getArts().getAll(null));
        Collections.reverse(entries);
        List<Section> sects = ten.getSects().getAll(null);
        ArrayList<UrlMap> urlMap = new ArrayList<>(entries.size() + sects.size() + 10);
        urlMap.add(new UrlMap(ten.getImeadValue(SecurityRepo.BASE_URL), null, ChangeFreq.daily, "0.7"));
        @SuppressWarnings("null")
        int maxArticleID = !entries.isEmpty() ? entries.get(entries.size() - 1).getArticleid() : 1;
        for (Article e : entries) {
            float difference = maxArticleID - e.getArticleid();
            difference = 1f - (difference / 50f);
            if (difference < 0.1f) {
                difference = 0.1f;
            }
            ChangeFreq freq = ChangeFreq.weekly;
            if (!e.getComments()) {
                freq = ChangeFreq.never;
                difference = 0.1f;
            } else {
                OffsetDateTime date=OffsetDateTime.now();
                date.minusMonths(1);
                if (date.isAfter(e.getPosted())) {
                    freq = ChangeFreq.monthly;
                }
                date.minusMonths(5);
                if (date.isAfter(e.getPosted())) {
                    freq = ChangeFreq.yearly;
                }
            }
            urlMap.add(new UrlMap(ArticleUrl.getUrl(ten.getImeadValue(SecurityRepo.BASE_URL), e, null), e.getModified(), freq, String.format("%.1f", difference)));
        }
        for (Section s : sects) {
            String name = s.getName();
            IndexFetcher f = new IndexFetcher(ten, "/index/" + name);
            if (!name.isEmpty()) {
                name = name + "/";
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
                urlMap.add(new UrlMap(ten.getImeadValue(SecurityRepo.BASE_URL) + "index/" + name + x, null, ChangeFreq.weekly, String.format("%.1f", difference)));
            }
        }
        return urlMap.iterator();
    }
}
