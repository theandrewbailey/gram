package libWebsiteTools;

import jakarta.ejb.Local;
import jakarta.servlet.http.HttpServletRequest;

/**
 * References all tenants in a potentially multi-tenant environment.
 *
 * @author alpha
 */
@Local
public interface Landlord {

    /**
     * Initialize all tenants ahead of time.
     */
    public void init();

    /**
     * Close and destroy tenants.
     */
    public void cleanup();

    /**
     *
     * @param req Determine and set which tenant/datasource/repositories to use
     * for this request, such that getTenant(req) will return the same.
     * @return the tenant that will fulfill the given request.
     */
    public Tenant setTenant(HttpServletRequest req);

    /**
     * Re-instantiate the Tenant associated with the given request, which is
     * then used for subsequent requests for all users.
     *
     * @param req
     * @return
     */
    public Tenant replaceTenant(HttpServletRequest req);

    public static Tenant getTenant(HttpServletRequest req) {
        return (Tenant) req.getAttribute(Tenant.class.getCanonicalName());
    }
}
