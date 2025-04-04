package gram.servlet;

import gram.bean.GramLandlord;
import gram.bean.GramTenant;
import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Locale;
import libWebsiteTools.imead.HtmlPageServlet;
import libWebsiteTools.imead.Local;

/**
 *
 * @author alpha
 */
@WebServlet(name = "HomeServlet", description = "Gets the homepage, or articles from all categories", urlPatterns = {"/", "/index.html"})
public class HomeServlet extends GramServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        GramTenant ten = GramLandlord.getTenant(request);
        List<Locale> resolvedLocales = Local.resolveLocales(ten.getImead(), request);
        if (ten.getImead().isFirstTime()) {
            String url = AdminImeadServlet.class.getAnnotation(WebServlet.class).urlPatterns()[0];
            request.getRequestDispatcher(url).forward(request, response);
        } else if (HtmlPageServlet.isPage(ten, resolvedLocales, "home")) {
            request.getRequestDispatcher("/page/home.html").forward(request, response);
        } else {
            request.getRequestDispatcher("/index/1.html").forward(request, response);
        }
    }
}
