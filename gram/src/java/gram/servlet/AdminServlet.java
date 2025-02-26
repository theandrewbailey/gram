package gram.servlet;

import gram.AdminPermission;
import gram.bean.GramLandlord;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.core.HttpHeaders;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import libWebsiteTools.JVMNotSupportedError;
import libWebsiteTools.turbo.RequestTimer;
import gram.bean.GramTenant;
import libWebsiteTools.security.SecurityRepo;

/**
 * Servlets that require a login to use must extend this class. Also provides
 * functionality that other servlets can use to verify logins for features that
 * require authorization.
 *
 * @author alpha
 * @see gram.servlet.AdminPermission
 */
public abstract class AdminServlet extends GramServlet {

    /**
     * I couldn't get annotations to work with enums, so subclasses must
     * implement this.
     *
     * @return The session must have one of these for the request to be
     * authorized.
     */
    public abstract AdminPermission[] getRequiredPermissions();

    /**
     *
     * @param req
     * @return Is the user authorized to make this request?
     */
    public boolean isAuthorized(HttpServletRequest req) {
        return isAuthorized(req, getRequiredPermissions());
    }

    /**
     * @param req
     * @param permissions
     * @return Does the session have the given scope saved, or does the
     * request's Authorization header have the password for the given
     * permission?
     */
    public static boolean isAuthorized(HttpServletRequest req, AdminPermission[] permissions) {
        Instant start = Instant.now();
        try {
            for (AdminPermission permission : permissions) {
                if (permission.isAuthenticated(req)) {
                    return true;
                }
            }
            String authHeader = req.getHeader(HttpHeaders.AUTHORIZATION).substring(6);
            String decoded = new String(Base64.getDecoder().decode(authHeader), "UTF-8");
            String[] parts = decoded.split(":", 2);
            for (AdminPermission permission : permissions) {
                if (1 < parts.length && null != permission.getAuthenticator(req, parts[1]).call()) {
                    return true;
                }
            }
        } catch (UnsupportedEncodingException ex) {
            throw new JVMNotSupportedError(ex);
        } catch (Exception n) {
        } finally {
            RequestTimer.addTiming(req, "checkAuth", Duration.between(start, Instant.now()));
        }
        return false;
    }

    /**
     * Check if the given password matches any password for any scope, and save
     * scope to session. If none found, clear any existing scope from session.
     * Used by login page. Will use multiple threads to test all scopes in
     * parallel.
     *
     * @param req
     * @param password
     * @return scope that password matches, or null if none found.
     */
    public AdminPermission authorize(HttpServletRequest req, String password) {
        GramTenant ten = GramLandlord.getTenant(req);
        Instant start = Instant.now();
        List<Future<AdminPermission>> checkers = new ArrayList<>(AdminPermission.Password.values().length);
        HttpSession sess = req.getSession();
        try {
            for (AdminPermission per : AdminPermission.Password.values()) {
                checkers.add(ten.getExec().submit(per.getAuthenticator(req, password)));
            }
            for (Future<AdminPermission> test : checkers) {
                if (null != test.get()) {
                    return test.get();
                }
            }
        } catch (InterruptedException | ExecutionException ex) {
            sess.removeAttribute(ten.getImeadValue(SecurityRepo.BASE_URL) + AdminPermission.class.getCanonicalName());
            throw new RuntimeException("Something went wrong while verifying passwords.", ex);
        } finally {
            RequestTimer.addTiming(req, "authorize", Duration.between(start, Instant.now()));
        }
        sess.removeAttribute(ten.getImeadValue(SecurityRepo.BASE_URL) + AdminPermission.class.getCanonicalName());
        return null;
    }

    @Override
    protected void service​(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        res.setHeader(HttpHeaders.CACHE_CONTROL, "private, no-store");
        res.setDateHeader(HttpHeaders.EXPIRES, OffsetDateTime.now().toInstant().toEpochMilli());
        if (isAuthorized(req)) {
            super.service(req, res);
        } else {
            res.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"" + getRequiredPermissions()[0].getKey() + "\", charset=\"UTF-8\"");
            res.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }
}
