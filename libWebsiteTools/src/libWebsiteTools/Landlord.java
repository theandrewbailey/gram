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
     *
     * @param req Determine a bunch of beans for this request, and save them to
     * it, such that getTenant(req) will return the same.
     * @return a bunch of beans that will fulfill the given request.
     */
    public Tenant setTenant(HttpServletRequest req);

    public static Tenant getTenant(HttpServletRequest req) {
        return (Tenant) req.getAttribute(Tenant.class.getCanonicalName());
    }
}
