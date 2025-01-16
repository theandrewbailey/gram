package gram.tag;

import gram.bean.GramLandlord;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.jsp.JspException;
import jakarta.servlet.jsp.PageContext;
import jakarta.servlet.jsp.tagext.SimpleTagSupport;
import gram.bean.database.Article;
import gram.bean.GramTenant;
import gram.bean.database.Section;

public class RecentArticles extends SimpleTagSupport {

    private Integer number = 10;
    private Section section;
    private String var = "_article";

    @Override
    @SuppressWarnings("unchecked")
    public void doTag() throws JspException, IOException {
        List<Integer> excludes = null;
        List<Article> articles = null;
        HttpServletRequest req = ((HttpServletRequest) ((PageContext) getJspContext()).getRequest());
        try {
            articles = (List<Article>) req.getAttribute("articles");
            excludes = articles.stream().mapToInt((t) -> {
                return t.getArticleid();
            }).boxed().collect(Collectors.toList());
        } catch (NullPointerException x) {
            if (null == articles) {
                articles = new ArrayList<>();
                req.setAttribute("articles", articles);
            }
        }
        GramTenant ten = GramLandlord.getTenant(req);
//        Instant start = Instant.now();
        List<Article> latest = ten.getArts().getBySection(null != section ? section.getName() : null, 1, number, excludes);
        if (2 > latest.size()) {
            latest = ten.getArts().getBySection(null != section ? section.getName() : null, 1, number, null);
        }
//        RequestTimer.addTiming(req, "recent-" + category, Duration.between(start, Instant.now()));
        for (Article e : latest) {
            getJspContext().setAttribute(getVar(), e);
            getJspBody().invoke(null);
            articles.add(e);
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
