package gram.servlet;

import gram.AdminPermission;
import gram.bean.GramLandlord;
import java.io.IOException;
import java.util.List;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.HttpHeaders;
import libWebsiteTools.file.BaseFileServlet;
import libWebsiteTools.file.Fileupload;
import gram.bean.GramTenant;

@WebServlet(name = "FileServlet", description = "Handles uploading files, and serves files through inherited class", urlPatterns = {"/file/*", "/fileImmutable/*", "/file", "/*/file"})
public class FileServlet extends BaseFileServlet {

    public static final String DEFAULT_TYPE = "text/html;charset=UTF-8";

    @Override
    @SuppressWarnings("unchecked")
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!AdminServlet.isAuthorized(request, new AdminPermission[]{AdminPermission.Password.FILES})) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        super.doPost(request, response);
        try {
            Fileupload uploaded = ((List<Fileupload>) request.getAttribute("uploadedfiles")).get(0);
            String[] split = AdminFileServlet.splitDirectoryAndName(uploaded.getFilename());
            request.setAttribute("opened_dir", split[0]);
        } catch (RuntimeException r) {
        }
        GramTenant ten = GramLandlord.getTenant(request);
        AdminFileServlet.showFileList(request, response, ten.getFile().getFileMetadata(null));
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        response.setContentType(DEFAULT_TYPE);
        super.doGet(request, response);
        if (HttpServletResponse.SC_OK != response.getStatus()) {
            String accept = request.getHeader(HttpHeaders.ACCEPT);
            if (null != accept && accept.contains("text/html")) {
                response.setContentType(DEFAULT_TYPE);
                response.sendError(response.getStatus());
            }
        }
    }

    @Override
    protected void doHead(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        response.setContentType(DEFAULT_TYPE);
        super.doHead(request, response);
    }
}
