package gram.tag;

import gram.CategoryFetcher;
import static gram.CategoryFetcher.POSTS_PER_PAGE;
import gram.bean.GramLandlord;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.jsp.JspException;
import jakarta.servlet.jsp.PageContext;
import jakarta.servlet.jsp.tagext.SimpleTagSupport;
import gram.bean.database.Article;
import gram.bean.GramTenant;
import gram.bean.database.Category;

public class RecentArticles extends SimpleTagSupport {

    private Integer number;
    private Category category;

    @Override
    @SuppressWarnings("unchecked")
    public void doTag() throws JspException, IOException {
        HttpServletRequest req = ((HttpServletRequest) ((PageContext) getJspContext()).getRequest());
        GramTenant ten = GramLandlord.getTenant(req);
        if (null == number) {
            number = Integer.valueOf(ten.getImeadValue(POSTS_PER_PAGE));
        }
        List<Article> excludes = null;
        try {
            excludes = (List<Article>) req.getAttribute("articles");
        } catch (NullPointerException x) {
        }
        if (null == excludes) {
            excludes = new ArrayList<>();
            req.setAttribute("articles", excludes);
        }
        String categoryName = null != category ? category.getName() : null;
        String categoryURL = Categorizer.getUrl("/", categoryName, null);
        List<Article> latest = ten.getArts().search(new CategoryFetcher(ten, categoryURL).without(excludes), number);
        if (2 > latest.size()) {
            latest = ten.getArts().search(new CategoryFetcher(ten, categoryURL), number);
        }
        if (!latest.isEmpty()) {
            getJspContext().setAttribute("_articles", latest);
            getJspBody().invoke(null);
            excludes.addAll(latest);
        }
    }

    public void setNumber(Integer number) {
        this.number = number;
    }

    public void setCategory(Category category) {
        this.category = category;
    }
}
