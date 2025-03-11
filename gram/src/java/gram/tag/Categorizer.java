package gram.tag;

import gram.bean.GramLandlord;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.jsp.JspException;
import jakarta.servlet.jsp.PageContext;
import jakarta.servlet.jsp.tagext.SimpleTagSupport;
import libWebsiteTools.security.SecurityRepo;
import libWebsiteTools.JVMNotSupportedError;
import gram.bean.database.Section;
import gram.bean.GramTenant;
import gram.bean.database.Article;
import gram.servlet.GramServlet;
import java.util.List;
import java.util.Locale;
import libWebsiteTools.imead.Local;

/**
 *
 * @author alpha
 */
public class Categorizer extends SimpleTagSupport {

    private Article article;
    private Section category;
    private Integer page;

    @Override
    public void doTag() throws JspException, IOException {
        if (null != category) {
            execute(category);
            return;
        }
        HttpServletRequest req = ((HttpServletRequest) ((PageContext) getJspContext()).getRequest());
        GramTenant ten = GramLandlord.getTenant(req);
        if (null != article) {
            if (null != article.getSectionid()) {
                execute(article.getSectionid());
            } else {
                List<Locale> resolvedLocales = Local.resolveLocales(ten.getImead(), req);
                Object baseURL = req.getAttribute(SecurityRepo.BASE_URL);
                getJspContext().setAttribute("_category_url", getUrl(baseURL.toString(), null, page));
                getJspContext().setAttribute("_category_name", ten.getImead().getLocal(GramServlet.SITE_TITLE, resolvedLocales));
                getJspContext().setAttribute("_category_uuid", "00000000-0000-0000-0000-000000000000");
                getJspBody().invoke(null);
            }
            return;
        }
        for (Section cat : ten.getCategories().getAll(null)) {
            execute(cat);
        }
    }

    private void execute(Section category) throws JspException, IOException {
        Object baseURL = ((HttpServletRequest) ((PageContext) getJspContext()).getRequest()).getAttribute(SecurityRepo.BASE_URL);
        if (null != baseURL) {
            getJspContext().setAttribute("_category_url", getUrl(baseURL.toString(), category.getName(), page));
            getJspContext().setAttribute("_category_name", category.getName());
            getJspContext().setAttribute("_category_uuid", category.getUuid());
            getJspBody().invoke(null);
        }
    }

    public static String getUrl(String baseURL, String category, Integer page) {
        StringBuilder url = new StringBuilder(70).append(baseURL).append("index");
        if (null != category && !category.isEmpty()) {
            try {
                String title = URLEncoder.encode(category, "UTF-8");
                url.append('/').append(title);
            } catch (UnsupportedEncodingException ex) {
                throw new JVMNotSupportedError(ex);
            }
        }
        if (null != page && 1 != page) {
            url.append('/').append(page);
        }
        return url.append(".html").toString();
    }

    public void setArticle(Article article) {
        this.article = article;
    }

    public void setCategory(Section category) {
        this.category = category;
    }

    public void setPage(Integer page) {
        this.page = page;
    }
}
