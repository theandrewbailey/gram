package gram.bean.database;

import gram.AdminPermission;
import gram.UtilStatic;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import libWebsiteTools.imead.IMEADRepository;

/**
 *
 * @author alpha
 */
public interface GramIMEADRepository extends IMEADRepository {

    /**
     *
     * @return a map of pre-set hard-coded passwords, to check if current
     * passwords have been changed.
     */
    public static Map<String, String> getOriginalPasswords() {
        Map<String, String> passwords = new HashMap<>();
        try {
            Properties IMEAD = UtilStatic.getInitialProperties();
            for (AdminPermission p : AdminPermission.Password.values()) {
                if (null == p.getKey()) {
                    continue;
                }
                passwords.put(p.getKey(), IMEAD.get(p.getKey()).toString());
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        return passwords;
    }

    /**
     *
     * @return should a first time configuration page be shown?
     */
    public boolean isFirstTime();

}
