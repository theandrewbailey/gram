package toilet.servlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.time.Instant;
import libWebsiteTools.file.Fileupload;
import libWebsiteTools.turbo.RequestTimer;
import libWebsiteTools.tag.AbstractInput;
import toilet.bean.ToiletBeanAccess;

/**
 *
 * @author alpha
 */
@WebServlet(name = "adminFile", description = "Performs admin stuff on file uploads", urlPatterns = {"/adminFile"})
public class AdminFileServlet extends AdminServlet {

    public static final String ADMIN_FILE = "/WEB-INF/adminFile.jsp";

    @Override
    public AdminServletPermission getRequiredPermission(HttpServletRequest req) {
        return AdminServletPermission.FILES;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        ToiletBeanAccess beans = allBeans.getInstance(request);
        showFileList(request, response, beans.getFile().getFileMetadata(null));
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        ToiletBeanAccess beans = allBeans.getInstance(request);
        String del = AbstractInput.getParameter(request, "action");
        if (del != null) {
            Fileupload deleted = beans.getFile().delete(del.split("\\|")[1]);
            String[] split = splitDirectoryAndName(deleted.getFilename());
            request.setAttribute("opened_dir", split[0]);
            beans.getFile().evict();
            beans.getGlobalCache().clear();
        }
        showFileList(request, response, beans.getFile().getFileMetadata(null));
    }

    public static void showFileList(HttpServletRequest request, HttpServletResponse response, List<Fileupload> uploads) throws ServletException, IOException {
        Instant start = Instant.now();
        LinkedHashMap<String, List<Fileupload>> files = new LinkedHashMap<>(uploads.size() * 2);
        List<String> directories = new ArrayList<>();
        // root "directory" first
        files.put("", new ArrayList<>());
        directories.add("");
        for (Fileupload f : uploads) {
            String[] split = splitDirectoryAndName(f.getFilename());
            List<Fileupload> temp = files.get(split[0]);
            if (temp == null) {
                temp = new ArrayList<>();
                files.put(split[0], temp);
                directories.add(split[0]);
            }
            f.setFilename(split[1]);
            temp.add(f);
        }
        request.setAttribute("files", files);
        request.setAttribute("directories", directories);
        if (null == request.getAttribute("opened_dir")) {
            request.setAttribute("opened_dir", "");
        }
        RequestTimer.addTiming(request, "query", Duration.between(start, Instant.now()));
        request.getRequestDispatcher(ADMIN_FILE).forward(request, response);
    }

    public static String[] splitDirectoryAndName(String filename) {
        String[] parts = filename.split("/", 2);
        String dir = parts.length == 2 ? parts[0] + "/" : "";
        String name = parts.length == 2 ? parts[1] : parts[0];
        return new String[]{dir, name};
    }
}
