package gram.bean;

import gram.bean.database.PostgresGramTenant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import jakarta.ejb.ConcurrencyManagement;
import jakarta.ejb.ConcurrencyManagementType;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameClassPair;
import javax.naming.NamingException;
import jakarta.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import jakarta.ws.rs.core.HttpHeaders;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import libWebsiteTools.Landlord;

/**
 * Singleton EJB that stores tenants for running blogs.
 *
 * @author alpha
 */
@Startup
@Singleton
@LocalBean
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class GramLandlord implements Landlord {

    private static final String DEFAULT_DATASOURCE = "java/gram/default";
    private final Map<String, GramTenant> tenants = new ConcurrentHashMap<>();
    @Resource
    private ManagedExecutorService defaultExec;

    @Override
    public GramTenant setTenant(HttpServletRequest req) {
        GramTenant ten = getTenant(req);
        if (null == ten) {
            String host = req.getHeader(HttpHeaders.HOST);
            ten = tenants.get(host);
            if (null == ten) {
                ten = tenants.get(DEFAULT_DATASOURCE);
            }
            req.setAttribute(libWebsiteTools.Tenant.class.getCanonicalName(), ten);
        }
        return ten;
    }

    public static GramTenant getTenant(HttpServletRequest req) {
        return (GramTenant) req.getAttribute(libWebsiteTools.Tenant.class.getCanonicalName());
    }

    private GramTenant instantiateTenant(String jndi, DataSource s) {
        try {
            DatabaseMetaData metaData = s.getConnection().getMetaData();
            switch (metaData.getDatabaseProductName()) {
                case "PostgreSQL":
                    return new PostgresGramTenant(jndi, s, defaultExec);
            }
        } catch (SQLException ex) {
        }
        throw new RuntimeException("JNDI resource " + jndi + " can't be used. Verify this connection works, rename this resource, or implement support for it.");
    }

    @PostConstruct
    @Override
    public void init() {
        Map<String, GramTenant> newTenants = new HashMap<>();
        for (Map.Entry<String, DataSource> pair : traverseContext(null, "").entrySet()) {
            String jndi = pair.getKey();
            if (jndi.startsWith("java/gram/")) {
                String dsName = DEFAULT_DATASOURCE.equals(jndi)
                        ? DEFAULT_DATASOURCE : jndi.replaceFirst("java/gram/", "");
                if (tenants.containsKey(dsName)) {
                    newTenants.put(dsName, tenants.get(dsName));
                    continue;
                }
                newTenants.put(dsName, instantiateTenant(jndi, pair.getValue()));
            }
        }
        synchronized (tenants) {
            for (Map.Entry<String, GramTenant> e : tenants.entrySet()) {
                if (!newTenants.containsKey(e.getKey())) {
                    e.getValue().destroy();
                    tenants.remove(e.getKey());
                }
            }
            tenants.clear();
            tenants.putAll(newTenants);
        }
    }

    @PreDestroy
    @Override
    public void cleanup() {
        synchronized (tenants) {
            for (GramTenant ten : tenants.values()) {
                ten.destroy();
            }
            tenants.clear();
        }
    }

    private Map<String, DataSource> traverseContext(Context ic, String lastSpace) {
        HashMap<String, DataSource> subNames = new HashMap<>();
        try {
            if (null == ic) {
                ic = new InitialContext();
            }
            ArrayList<NameClassPair> contextList = Collections.list(ic.list(""));
            for (NameClassPair nc : contextList) {
                if ("__SYSTEM".equals(nc.getName())) {
                    continue;
                }
                try {
                    Object o = ic.lookup(nc.getName());
                    if (o instanceof Context context) {
                        subNames.putAll(traverseContext(context, lastSpace + nc.getName() + "/"));
                    } else if (o instanceof DataSource ds && !nc.getName().endsWith("__pm")) {
                        subNames.put(lastSpace + nc.getName(), ds);
                    }
                } catch (NamingException ex) {
                }
            }
        } catch (NamingException ex) {
        }
        return subNames;
    }

    /**
     * lookup an EJB. avoid using this, because it's not fast.
     *
     * @param <T>
     * @param name
     * @param type
     * @return
     * @deprecated
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public static <T> T getBean(String name, Class<T> type) {
        try {
            return (T) new InitialContext().lookup(name);
        } catch (NamingException n) {
            throw new RuntimeException("Attempted to look up invalid bean, name:" + name + " type:" + type.getName(), n);
        }
    }

    @Schedule(persistent = false, hour = "1")
    @SuppressWarnings("unused")
    private void nightly() {
        for (GramTenant ten : tenants.values()) {
            new SiteExporter(ten).run();
            ten.getError().evict();
        }
    }

    @Schedule(persistent = false, minute = "*", hour = "*", dayOfWeek = "*", month = "*")
    @SuppressWarnings("unused")
    private void sweep() {
        for (GramTenant ten : tenants.values()) {
            ten.getPageCacheProvider().sweep();
        }
    }
}
