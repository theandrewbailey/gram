package gram.bean.database;

import gram.AdminPermission;
import gram.UtilStatic;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.TypedQuery;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import libWebsiteTools.imead.IMEADDatabase;
import libWebsiteTools.imead.Localization;
import libWebsiteTools.security.HashUtil;

/**
 *
 * @author alpha
 */
public class GramIMEADDatabase extends IMEADDatabase implements GramIMEADRepository {

    private static final Logger LOG = Logger.getLogger(GramIMEADDatabase.class.getName());
    private static final Map<String, String> originalPasswords = Collections.unmodifiableMap(GramIMEADRepository.getOriginalPasswords());
    private Boolean firstTime;

    public GramIMEADDatabase(EntityManagerFactory PU) {
        super(PU);
        ArrayList<Localization> locals = new ArrayList<>();
        // in a new release, some properties may be added
        try {
            locals.addAll(UtilStatic.getNewLocalizations(this, UtilStatic.getInitialProperties(), Locale.ROOT));
        } catch (IOException ox) {
            LOG.log(Level.SEVERE, null, ox);
        }
        if (!locals.isEmpty()) {
            upsert(locals);
        }
    }

    @Override
    public List<Localization> search(Object term, Integer limit) {
        try (EntityManager em = PU.createEntityManager()) {
            TypedQuery<Localization> q = em.createNamedQuery("Localization.searchKeys", Localization.class).setParameter("term", term.toString());
            if (null != limit) {
                q.setMaxResults(limit);
            }
            return q.getResultList();
        }
    }

    @Override
    public List<Localization> search(Localization term, Integer limit) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public synchronized GramIMEADDatabase evict() {
        super.evict();
        firstTime = null;
        return this;
    }

    @Override
    public synchronized boolean isFirstTime() {
        if (null != firstTime) {
            return firstTime;
        }
        for (AdminPermission p : AdminPermission.Password.values()) {
            if (null == p.getKey()) {
                continue;
            }
            String original = originalPasswords.get(p.getKey());
            String current = getValue(p.getKey());
            if (null == original || null == current || original.equals(current) || !HashUtil.ARGON2_ENCODING_PATTERN.matcher(current).matches()) {
                firstTime = true;
                return true;
            }
        }
        firstTime = false;
        return false;
    }
}
