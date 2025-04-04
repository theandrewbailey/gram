package libWebsiteTools;

import jakarta.ejb.EJB;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import libWebsiteTools.imead.HtmlPageServlet;

/**
 * Sends HTTP 405 for all HTTP methods. If one is overridden, it won't.
 *
 * @author alpha
 */
@MultipartConfig(maxRequestSize = 999999999)
public abstract class BaseServlet extends HttpServlet {

    public static final String ERROR_PREFIX = HtmlPageServlet.IMEAD_KEY_PREFIX + "error";
    public static final String ERROR_MESSAGE_PARAM = "ERROR_MESSAGE";
    @EJB
    protected Landlord landlord;

    /**
     * Sends an error message to the browser. Does not change HTTP response
     * code.
     *
     * @param req
     * @param res
     * @param errorCode
     * @throws jakarta.servlet.ServletException
     * @throws java.io.IOException
     */
    public static void showError(HttpServletRequest req, HttpServletResponse res, Integer errorCode) throws ServletException, IOException {
        if (400 <= errorCode && errorCode < 600) {
            res.setStatus(errorCode);
        }
        req.setAttribute("title", "HTTP ERROR " + errorCode);
        HtmlPageServlet.showPage(req, res, "error" + errorCode);
    }

    /**
     * tells the client to go to a new location. WHY is this not included in the
     * standard servlet API????
     *
     * @param res
     * @param newLocation
     */
    public static void permaMove(HttpServletResponse res, String newLocation) {
        res.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
        res.setHeader("Location", newLocation);
    }

    @Override
    protected void service​(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        try {
            req.setAttribute(WebServlet.class.getCanonicalName(), getClass().getAnnotation(WebServlet.class).urlPatterns()[0]);
        } catch (NullPointerException nx) {
        }
        super.service(req, res);
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    @Override
    protected void doHead(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    @Override
    protected void doTrace(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }
}
