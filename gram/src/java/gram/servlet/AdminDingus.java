package gram.servlet;

import gram.AdminPermission;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import libWebsiteTools.tag.AbstractInput;
import gram.ArticleProcessor;
import gram.UtilStatic;
import gram.bean.GramLandlord;
import gram.bean.GramTenant;
import gram.bean.database.Article;
import libWebsiteTools.security.SecurityRepository;
import libWebsiteTools.turbo.JspFilter;

/**
 *
 * @author alpha
 */
@WebServlet(name = "AdminDingus", description = "Show a commonmark dingus", urlPatterns = {"/adminDingus"})
public class AdminDingus extends AdminServlet {

    public static final String ADMIN_DINGUS = "/WEB-INF/admin/adminDingus.jsp";
    public static final String ADMIN_DINGUS_IFRAME = "/WEB-INF/admin/adminDingusIframe.jsp";
    private static final String DEFAULT_CSP_SOURCES = "'self' 'unsafe-inline'";

    @Override
    public AdminPermission[] getRequiredPermissions() {
        return new AdminPermission[]{AdminPermission.Password.EDIT_POSTS};
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        GramTenant ten = GramLandlord.getTenant(request);
        String attrName = ten.getImeadValue(SecurityRepository.BASE_URL) + Article.class.getCanonicalName();
        request.getSession().removeAttribute(attrName);
        if (null != request.getParameter("iframe")) {
            Article art = (Article) request.getSession().getAttribute(attrName);
            request.setAttribute(Article.class.getSimpleName(), art);
            String csp = String.format(JspFilter.CSP_TEMPLATE, DEFAULT_CSP_SOURCES, DEFAULT_CSP_SOURCES, ten.getImeadValue(SecurityRepository.BASE_URL));
            response.setHeader(JspFilter.CONTENT_SECURITY_POLICY, csp);
            request.setAttribute(JspFilter.CONTENT_SECURITY_POLICY, true);
            request.getRequestDispatcher(ADMIN_DINGUS_IFRAME).forward(request, response);
        } else {
            AdminArticleServlet.getRecentImages(ten, request);
            request.getRequestDispatcher(ADMIN_DINGUS).forward(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        GramTenant ten = GramLandlord.getTenant(request);
        String markdown = AbstractInput.getParameter(request, "postedmarkdown");
        Article art = new Article();
        if (null != markdown) {
            art.setPostedmarkdown(markdown);
            ArticleProcessor processor = new ArticleProcessor(ten, art);
            processor.call();
            request.setAttribute(Article.class.getSimpleName(), art);
            request.getSession().setAttribute(ten.getImeadValue(SecurityRepository.BASE_URL) + Article.class.getCanonicalName(), art);
            art.setSummary(UtilStatic.htmlFormat(art.getPostedhtml(), false, false, true));
        }
        if (null != request.getParameter("iframe")) {
            String csp = String.format(JspFilter.CSP_TEMPLATE, DEFAULT_CSP_SOURCES, DEFAULT_CSP_SOURCES, ten.getImeadValue(SecurityRepository.BASE_URL));
            response.setHeader(JspFilter.CONTENT_SECURITY_POLICY, csp);
            request.setAttribute(JspFilter.CONTENT_SECURITY_POLICY, true);
            request.getRequestDispatcher(ADMIN_DINGUS_IFRAME).forward(request, response);
        } else {
            request.getSession().setAttribute(ten.getImeadValue(SecurityRepository.BASE_URL) + Article.class.getCanonicalName(), art);
            request.getRequestDispatcher(ADMIN_DINGUS).forward(request, response);
        }
    }
}
