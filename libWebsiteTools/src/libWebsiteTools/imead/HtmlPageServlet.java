package libWebsiteTools.imead;

import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import libWebsiteTools.BaseServlet;
import libWebsiteTools.Landlord;
import libWebsiteTools.Tenant;
import libWebsiteTools.security.SecurityRepository;

/**
 *
 * @author alpha
 */
@WebServlet(name = "HtmlPageServlet", description = "Lookup htmlpage_* value in IMEAD and show it in html template.", urlPatterns = {"/page/*","/error/*"})
public class HtmlPageServlet extends BaseServlet {

    public static final Pattern IMEAD_ID_PATTERN = Pattern.compile(".*?/page/(.*?).html(\\?.*)?$");
    public static final Pattern ERROR_PATTERN = Pattern.compile(".*?/error/(\\d+)$");
    public static final String IMEAD_KEY_PREFIX = "htmlpage_";
    private static final String HTMLPAGE_JSP = "/WEB-INF/htmlpage.jsp";
    private static final String HTMLPAGE_IFRAME_JSP = "/WEB-INF/htmlpageIframe.jsp";

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!handleError(request, response)) {
            super.doPost(request, response);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Matcher m = IMEAD_ID_PATTERN.matcher(request.getRequestURL().toString());
        if (m.find()) {
            showPage(request, response, m.group(1));
            return;
        }
        if (!handleError(request, response)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private boolean handleError(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Matcher m = ERROR_PATTERN.matcher(request.getRequestURL().toString());
        if (m.find()) {
            try {
                int errorCode = Integer.parseInt(m.group(1));
                showError(request, response, errorCode);
            } catch (NumberFormatException t) {
                showError(request, response, 500);
            }
            return true;
        }
        return false;
    }

    public static boolean isPage(Tenant ten, List<Locale> resolvedLocales, String pagename) {
        String IMEAD_ID = IMEAD_KEY_PREFIX + pagename;
        try {
            String pagetext = ten.getImead().getLocal(IMEAD_ID, resolvedLocales);
            if (null == pagetext || pagetext.isBlank()) {
                return false;
            }
        } catch (Exception p) {
            return false;
        }
        return true;
    }

    public static void showPage(HttpServletRequest request, HttpServletResponse response, String imeadId) throws ServletException, IOException {
        Tenant ten = Landlord.getTenant(request);
        List<Locale> resolvedLocales = Local.resolveLocales(ten.getImead(), request);
        if (imeadId.endsWith("_markdown")) {
            Object baseURL = request.getAttribute(SecurityRepository.BASE_URL);
            permaMove(response, baseURL.toString() + "page/" + imeadId.replaceAll("_markdown$", "") + ".html");
            return;
        }
        String IMEAD_ID = IMEAD_KEY_PREFIX + imeadId;
        try {
            String pagetext = ten.getImead().getLocal(IMEAD_ID, resolvedLocales);
            if (null != pagetext) {
                request.setAttribute("IMEAD_ID", IMEAD_ID);
                request.setAttribute("htmlpagetext", pagetext);
                String iframe = request.getParameter("iframe");
                request.getServletContext().getRequestDispatcher(null != iframe ? HTMLPAGE_IFRAME_JSP : HTMLPAGE_JSP).forward(request, response);
                return;
            }
        } catch (LocalizedStringNotFoundException | IllegalStateException z) {
        }
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
    }
}
