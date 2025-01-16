package gram.servlet;

import gram.bean.GramLandlord;
import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import libWebsiteTools.imead.Local;
import libWebsiteTools.imead.LocalizedStringNotFoundException;
import gram.bean.GramTenant;

@WebServlet(name = "CoronerServlet", description = "Error page stuff", urlPatterns = {"/coroner", "/coroner/*"})
public class CoronerServlet extends GramServlet {

    private final String[] vars = {"javax.servlet.error.status_code",
        "javax.servlet.error.exception_type", "javax.servlet.error.message",
        "javax.servlet.error.exception", "javax.servlet.error.request_uri"};

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        GramTenant ten = GramLandlord.getTenant(request);
        String errorCode;
        String[] split = request.getRequestURI().split("coroner/");
        if (2 == split.length) {
            errorCode = split[1];
        } else {
            Object[] messages = new Object[5];
            for (int x = 0; x < messages.length; x++) {
                messages[x] = request.getAttribute(vars[x]);
            }
            errorCode = null != messages[0] ? messages[0].toString() : "501";
        }
        request.setAttribute("title", "ERROR " + errorCode);
        try {
            String errorMessage = ten.getImead().getLocal(ERROR_PREFIX + errorCode, Local.resolveLocales(ten.getImead(), request));
            if (null == errorMessage) {
                errorMessage = ten.getImead().getLocal(ERROR_PREFIX + "404", Local.resolveLocales(ten.getImead(), request));
            }
            showError(request, response, errorMessage);
        } catch (LocalizedStringNotFoundException lx) {
            showError(request, response, errorCode);
        }
    }
}
