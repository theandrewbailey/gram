package gram.servlet;

import gram.bean.GramLandlord;
import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import libWebsiteTools.BaseServlet;
import gram.bean.GramTenant;
import jakarta.ejb.EJB;

/**
 *
 * @author alpha
 */
public abstract class GramServlet extends BaseServlet {

    public static final String SITE_TITLE = "page_title";
    public static final String TAGLINE = "page_tagline";
    @EJB
    protected GramLandlord gramlord;

    @Override
    protected void service​(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        GramTenant ten = GramLandlord.getTenant(req);
        req.setAttribute("milligram", null != req.getParameter("milligram"));
        req.setAttribute("categories", ten.getCategories().getAll(null));
        super.service(req, res);
    }
}
