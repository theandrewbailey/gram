package gram;

import gram.bean.GramLandlord;
import gram.bean.GramTenant;
import gram.servlet.AdminFileServlet;
import gram.servlet.AdminHealthServlet;
import gram.servlet.AdminImeadServlet;
import gram.servlet.AdminImportServlet;
import gram.servlet.AdminPostServlet;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import java.util.concurrent.Callable;
import libWebsiteTools.security.HashUtil;
import libWebsiteTools.security.SecurityRepository;

/**
 * Defines the permissions used to administer the site. A password is associated
 * with each one. When entered into /adminLogin, the password is checked against
 * the hash stored at the given IMEAD key. If successful, the user is directed
 * to the URL associated with the matching hash.
 *
 * @author alpha
 */
public interface AdminPermission {

    public String getUrl();

    public String getKey();

    public default Callable<AdminPermission> getAuthenticator(HttpServletRequest req, String toVerify) {
        return new PasswordAuthenticator(this, req, toVerify);
    }

    default AdminPermission authenticate(HttpServletRequest req) {
        GramTenant ten = GramLandlord.getTenant(req);
        req.getSession().setAttribute(ten.getImeadValue(SecurityRepository.BASE_URL) + AdminPermission.class.getCanonicalName(), this);
        return this;
    }

    default boolean isAuthenticated(HttpServletRequest req) {
        GramTenant ten = GramLandlord.getTenant(req);
        return this.equals(req.getSession().getAttribute(ten.getImeadValue(SecurityRepository.BASE_URL) + AdminPermission.class.getCanonicalName()));
    }

    public static enum Password implements AdminPermission {
        EDIT_POSTS("admin_editPosts", AdminPostServlet.class.getAnnotation(WebServlet.class).urlPatterns()[0]),
        FILES("admin_files", AdminFileServlet.class.getAnnotation(WebServlet.class).urlPatterns()[0]),
        HEALTH("admin_health", AdminHealthServlet.class.getAnnotation(WebServlet.class).urlPatterns()[0]),
        IMEAD("admin_imead", AdminImeadServlet.class.getAnnotation(WebServlet.class).urlPatterns()[0]),
        IMPORT_EXPORT("admin_importExport", AdminImportServlet.class.getAnnotation(WebServlet.class).urlPatterns()[0]);
        private final String key;
        private final String url;

        Password(String imeadKey, String path) {
            this.key = imeadKey;
            this.url = path;
        }

        @Override
        public String getKey() {
            return key;
        }

        @Override
        public String getUrl() {
            return url;
        }
    }
    public static AdminPermission FIRSTTIME = new AdminPermission() {
        @Override
        public String getUrl() {
            return AdminImeadServlet.class.getAnnotation(WebServlet.class).urlPatterns()[0];
        }

        @Override
        public String getKey() {
            return "firsttime";
        }

        @Override
        public boolean isAuthenticated(HttpServletRequest req) {
            return GramLandlord.getTenant(req).getImead().isFirstTime();
        }

        @Override
        public Callable<AdminPermission> getAuthenticator(HttpServletRequest req, String toVerify) {
            return new FirsttimeAuthenticator(this, req);
        }
    };
}

class PasswordAuthenticator implements Callable<AdminPermission> {

    private final String hash;
    private final String toVerify;
    private final AdminPermission perm;
    private final HttpServletRequest r;

    public PasswordAuthenticator(AdminPermission perm, HttpServletRequest req, String toVerify) {
        this.hash = GramLandlord.getTenant(req).getImeadValue(perm.getKey());
        this.r = req;
        this.toVerify = toVerify;
        this.perm = perm;
    }

    @Override
    public AdminPermission call() {
        return HashUtil.verifyArgon2Hash(hash, toVerify) ? perm.authenticate(r) : null;
    }
}

class FirsttimeAuthenticator implements Callable<AdminPermission> {

    private final HttpServletRequest r;
    private final AdminPermission perm;
    private final GramTenant ten;

    public FirsttimeAuthenticator(AdminPermission perm, HttpServletRequest req) {
        this.ten = GramLandlord.getTenant(req);
        this.r = req;
        this.perm = perm;
    }

    @Override
    public AdminPermission call() {
        return ten.getImead().isFirstTime() ? perm.authenticate(r) : null;
    }
}
