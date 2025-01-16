package gram.servlet;

import gram.bean.GramLandlord;
import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import libWebsiteTools.BaseServlet;
import libWebsiteTools.imead.Local;
import gram.bean.GramTenant;

/**
 *
 * @author alpha
 */
public abstract class GramServlet extends BaseServlet {

    public static final String ERROR_PREFIX = "page_error_";
    public static final String ERROR_MESSAGE_PARAM = "ERROR_MESSAGE";
    public static final String ERROR_JSP = "/WEB-INF/error.jsp";
    public static final String ERROR_IFRAME_JSP = "/WEB-INF/errorIframe.jsp";
    public static final String SITE_TITLE = "page_title";
    public static final String TAGLINE = "page_tagline";

    @Override
    protected void service​(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        req.setAttribute("milligram", null != req.getParameter("milligram"));
        super.service(req, res);
    }

    protected void showError(HttpServletRequest req, HttpServletResponse res, String errorMessage) {
        req.setAttribute(ERROR_MESSAGE_PARAM, errorMessage);
        try {
            getServletContext().getRequestDispatcher((null == req.getParameter("iframe") ? ERROR_JSP : ERROR_IFRAME_JSP) + "?error=" + req.getAttribute("title")).forward(req, res);
        } catch (IllegalStateException | ServletException | IOException ix) {
        }
    }

    /**
     * Sends an error message to the browser. Does not change HTTP response
     * code.
     *
     * @param req
     * @param res
     * @param errorCode
     */
    protected void showError(HttpServletRequest req, HttpServletResponse res, Integer errorCode) {
        req.setAttribute("title", "ERROR " + errorCode);
        GramTenant ten = GramLandlord.getTenant(req);
        showError(req, res, ten.getImead().getLocal(ERROR_PREFIX + errorCode, Local.resolveLocales(ten.getImead(), req)));
    }
}
