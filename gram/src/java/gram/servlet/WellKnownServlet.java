package gram.servlet;

import gram.bean.GramLandlord;
import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import gram.bean.GramTenant;

/**
 *
 * @author alpha
 */
@WebServlet(name = "WellKnownServlet", description = "Servlet for well known files", urlPatterns = {"/robots.txt", "/favicon.ico", "/browserconfig.xml", "/.well-known/*"})
public class WellKnownServlet extends GramServlet {

    @Override
    protected void doHead(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String URL = request.getRequestURI().replaceFirst(request.getServletContext().getContextPath(), "");
        GramTenant ten = GramLandlord.getTenant(request);
        switch (URL) {
            case "/favicon.ico":
                GramServlet.permaMove(response, ten.getImeadValue("site_favicon"));
                break;
            default:
                if (URL.contains("/.well-known/")) {
                    request.getServletContext().getRequestDispatcher(URL.replaceFirst("/.well-known/", FileServlet.class.getAnnotation(WebServlet.class).urlPatterns()[0] + "/")).forward(request, response);
                } else if (null != ten.getFile().get(URL.substring(1))) {
                    String next = "/file" + URL;
                    request.getServletContext().getRequestDispatcher(next).forward(request, response);
                } else {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                }
                break;
        }
    }
}
