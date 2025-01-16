package gram.bean;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
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
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import jakarta.ws.rs.core.HttpHeaders;
import java.util.Map;
import org.eclipse.persistence.jpa.PersistenceProvider;
import libWebsiteTools.Landlord;

/**
 * Easy way to ensure static functions have access to requisite bean classes.
 *
 * @author alpha
 */
@Startup
@Singleton
@LocalBean
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class GramLandlord implements Landlord {

    private static final String DEFAULT_DATASOURCE = "java/gram/default";
    private final HashMap<String, GramTenant> tenants = new HashMap<>();
    @Resource
    private ManagedExecutorService defaultExec;
//    @PersistenceUnit
//    private EntityManagerFactory defaultPU;

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

    @PreDestroy
    private void cleanup() {
        for (GramTenant ten : tenants.values()) {
            ten.destroy();
        }
        tenants.clear();
    }

    @PostConstruct
    @SuppressWarnings("unused")
    private void init() {
        cleanup();
//        tenants.put(DEFAULT_DATASOURCE, new PostgresGramTenant(defaultPU, defaultExec));
        PersistenceProvider pp = new PersistenceProvider();
        Map<String, DataSource> dataSources = traverseContext(null, "");
        for (Map.Entry<String, DataSource> pair : dataSources.entrySet()) try {
            String jndi = pair.getKey();
            if (jndi.startsWith("java/gram/")) {
                DataSource s = pair.getValue();
                GramUnitInfo info = new GramUnitInfo(jndi, s);
                EntityManagerFactory emf = pp.createContainerEntityManagerFactory(info, new HashMap<>());
                GramTenant ten = new PostgresGramTenant(emf, defaultExec);
                tenants.put(DEFAULT_DATASOURCE.equals(jndi)
                        ? DEFAULT_DATASOURCE : jndi.replaceFirst("java/gram/", ""), ten);
            }
        } catch (RuntimeException rx) {
            Logger.getLogger(GramLandlord.class.getName()).log(Level.SEVERE, "Can't initialize beans for " + pair.getKey(), rx);
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
            ten.getBackup().run();
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
