package gram.bean;

import gram.AdminPermission;
import gram.UtilStatic;
import gram.bean.database.ArticleRepository;
import jakarta.ejb.Local;
import libWebsiteTools.Repository;
import gram.bean.database.Comment;
import gram.bean.database.Section;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import libWebsiteTools.Tenant;

/**
 * Provides access to all blog data for a single tenant.
 *
 * @author alpha
 */
@Local
public interface GramTenant extends Tenant {

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

    public boolean isFirstTime();

    public ArticleRepository getArts();

    public Repository<Comment> getComms();

    public Repository<Section> getCategories();
}
