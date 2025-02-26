package gram.servlet;

import gram.AdminPermission;
import gram.bean.SiteImporter;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipInputStream;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import jakarta.ws.rs.core.HttpHeaders;
import java.util.Arrays;
import libWebsiteTools.security.GuardFilter;
import libWebsiteTools.turbo.RequestTimer;
import libWebsiteTools.security.SecurityRepo;
import libWebsiteTools.tag.AbstractInput;
import gram.bean.SiteExporter;
import gram.bean.GramLandlord;
import gram.bean.GramTenant;

@WebServlet(name = "AdminImportServlet", description = "Download the entire site as a zip. Insert articles, comments, and files via zip file upload", urlPatterns = {"/adminImport", "/*/adminImport", "/adminExport"})
public class AdminImportServlet extends AdminServlet {

    public static final String ADMIN_IMPORT_EXPORT = "/WEB-INF/admin/adminImportExport.jsp";

    @Override
    public AdminPermission[] getRequiredPermissions() {
        return new AdminPermission[]{AdminPermission.Password.IMPORT_EXPORT, AdminPermission.FIRSTTIME};
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        GramTenant ten = GramLandlord.getTenant(request);
        SiteExporter backup = new SiteExporter(ten);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + backup.getArchiveName("zip"));
        response.setContentType("application/zip");
        backup.writeZip(response.getOutputStream(), Arrays.asList(SiteExporter.BackupTypes.values()));
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        GramTenant ten = GramLandlord.getTenant(request);
        try {
            Part p = AbstractInput.getPart(request, "zip");
            InputStream i = p.getInputStream();
            ZipInputStream zip = new ZipInputStream(i);
            new SiteImporter(ten).restoreFromZip(zip);
            request.getSession().invalidate();
            response.setHeader("Clear-Site-Data", "*");
            response.setHeader(RequestTimer.SERVER_TIMING, RequestTimer.getTimingHeader(request, Boolean.FALSE));
            response.sendRedirect(request.getAttribute(SecurityRepo.BASE_URL).toString());
        } catch (IOException ex) {
            ten.getError().logException(request, "Restore from zip failed", null, ex);
            request.setAttribute(GuardFilter.HANDLED_ERROR, true);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } catch (ServletException | IllegalStateException sx) {
            request.getRequestDispatcher(ADMIN_IMPORT_EXPORT).forward(request, response);
        }
    }
}
