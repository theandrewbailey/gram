package gram.bean.database;

import gram.UtilStatic;
import jakarta.persistence.EntityManagerFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import libWebsiteTools.imead.IMEADDatabase;
import libWebsiteTools.imead.Localization;

/**
 *
 * @author alpha
 */
public class GramIMEADDatabase extends IMEADDatabase {

    private static final Logger LOG = Logger.getLogger(GramIMEADDatabase.class.getName());

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
}
