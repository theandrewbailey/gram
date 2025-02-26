package gram.servlet;

import gram.AdminPermission;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import libWebsiteTools.Markdowner;
import libWebsiteTools.tag.AbstractInput;
import gram.ArticleProcessor;
import gram.UtilStatic;
import gram.bean.GramLandlord;
import gram.bean.database.Article;

/**
 *
 * @author alpha
 */
@WebServlet(name = "AdminDingus", description = "Show a commonmark dingus", urlPatterns = {"/adminDingus"})
public class AdminDingus extends AdminServlet {

    public static final String ADMIN_DINGUS = "/WEB-INF/admin/adminDingus.jsp";
    public static final String ADMIN_DINGUS_IFRAME = "/WEB-INF/admin/adminDingusIframe.jsp";

    @Override
    public AdminPermission[] getRequiredPermissions() {
        return new AdminPermission[]{AdminPermission.Password.EDIT_POSTS};
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.getRequestDispatcher(ADMIN_DINGUS).forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String markdown = AbstractInput.getParameter(request, "postedmarkdown");
        if (null != markdown) {
            Article art = new Article();
            art.setPostedhtml(Markdowner.getHtml(markdown));
            art.setPostedmarkdown(markdown);
            new ArticleProcessor(GramLandlord.getTenant(request), art).call();
            request.setAttribute(Article.class.getSimpleName(), art);
            art.setSummary(UtilStatic.htmlFormat(art.getPostedhtml(), false, false, true));
        }
        request.getRequestDispatcher(ADMIN_DINGUS_IFRAME).forward(request, response);
    }
}
