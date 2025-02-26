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
import gram.bean.database.Section;

public class RecentArticles extends SimpleTagSupport {

    private Integer number;
    private Section section;
    private String var = "_article";

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
        String sectionName = null != section ? section.getName() : null;
        String sectionURL = Categorizer.getUrl("/", sectionName, null);
        List<Article> latest = ten.getArts().search(new CategoryFetcher(ten, sectionURL).without(excludes), number);
        if (2 > latest.size()) {
            latest = ten.getArts().search(new CategoryFetcher(ten, sectionURL), number);
        }
        for (Article e : latest) {
            getJspContext().setAttribute(getVar(), e);
            getJspBody().invoke(null);
            excludes.add(e);
        }
    }

    public void setNumber(Integer number) {
        this.number = number;
    }

    public void setSection(Section section) {
        this.section = section;
    }

    public String getVar() {
        return var;
    }

    public void setVar(String var) {
        this.var = var;
    }
}
