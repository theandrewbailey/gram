package gram.servlet;

import gram.AdminPermission;
import gram.bean.SiteExporter;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.HttpHeaders;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import gram.bean.GramTenant;
import gram.bean.GramLandlord;
import gram.bean.SiteMilligramExporter;
import jakarta.servlet.ServletOutputStream;

/**
 *
 * @author alpha
 */
@WebServlet(name = "AdminMilligram", description = "Static site generator, output in zip form.", urlPatterns = {"/mg"})
public class AdminMilligram extends AdminServlet {

    @Override
    public AdminPermission[] getRequiredPermissions() {
        return new AdminPermission[]{AdminPermission.Password.IMPORT_EXPORT, AdminPermission.Password.EDIT_POSTS};
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        GramTenant ten = GramLandlord.getTenant(request);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + new SiteExporter(ten).getArchiveStem() + "_" + OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd'-ssg.zip'")));
        response.setContentType("application/zip");
        ServletOutputStream ttr = response.getOutputStream();
        new SiteMilligramExporter(ten).writeZip(ttr);
    }
}
