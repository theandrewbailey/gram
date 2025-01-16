package gram.servlet;

import gram.AdminPermission;
import gram.bean.GramLandlord;
import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import libWebsiteTools.security.GuardFilter;
import libWebsiteTools.tag.AbstractInput;
import gram.bean.GramTenant;

@WebServlet(name = "AdminLoginServlet", description = "Show the login page", urlPatterns = {"/adminLogin"})
public class AdminLoginServlet extends AdminServlet {

    public static final String ADMIN_LOGIN_PAGE = "/WEB-INF/adminLogin.jsp";

    @Override
    public AdminPermission[] getRequiredPermissions() {
        throw new UnsupportedOperationException("You're always allowed to see the login page. How did you call this?");
    }

    /**
     * You're always allowed to see the login page.
     *
     * @param req
     * @return true
     */
    @Override
    public boolean isAuthorized(HttpServletRequest req) {
        return true;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        GramTenant ten = GramLandlord.getTenant(request);
        if (ten.isFirstTime()) {
            String url = AdminImeadServlet.class.getAnnotation(WebServlet.class).urlPatterns()[0];
            request.getRequestDispatcher(url).forward(request, response);
            return;
        }
        request.getRequestDispatcher(ADMIN_LOGIN_PAGE).forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String answer = AbstractInput.getParameter(request, "answer");
        GramTenant ten = GramLandlord.getTenant(request);
        try {
            if (null == answer) {
                request.getRequestDispatcher(ADMIN_LOGIN_PAGE).forward(request, response);
            }
            AdminPermission per = authorize(request, answer);
            if (null != per) {
                request.getRequestDispatcher(per.getUrl()).forward(request, response);
            } else {
                ten.getError().logException(request, "Bad Login", "Tried to access restricted area. Login not recognized: " + answer, null);
                request.getSession().setAttribute(AdminPermission.class.getCanonicalName(), null);
                request.setAttribute(GuardFilter.HANDLED_ERROR, true);
                request.getRequestDispatcher(IndexServlet.class.getAnnotation(WebServlet.class).urlPatterns()[0]).forward(request, response);
            }
        } catch (RuntimeException ex) {
            ten.getError().logException(request, "Multithread Exception", "Something happened while verifying passwords", ex);
            request.setAttribute(GuardFilter.HANDLED_ERROR, true);
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            } catch (IllegalStateException is) {
                // oops, can't do anything now
            }
        }
    }
}
