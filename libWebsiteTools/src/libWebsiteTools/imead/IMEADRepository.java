package libWebsiteTools.imead;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;
import libWebsiteTools.Repository;

/**
 *
 * @author alpha
 */
public interface IMEADRepository extends Repository<Localization> {

    /**
     * try to get value for key, using order of given locales
     *
     * @param key
     * @param locales
     * @return value
     * @throws RuntimeException if key cannot be found in locales
     */
    public String getLocal(String key, Collection<Locale> locales);

    /**
     * try to get value for key in specified locale (as String). will return
     * null if key not in locale, will throw NullPointerException if locale does
     * not exist.
     *
     * @param key
     * @param locale
     * @return value || null
     * @throws NullPointerException
     */
    public String getLocal(String key, String locale);

    public Collection<Locale> getLocales();

    /**
     * @return the localizedHash
     */
    public String getLocalizedHash();

    public List<Pattern> getPatterns(String key);

    /**
     * load all properties from DB
     *
     * @return map of Locale to Properties
     */
    public Map<Locale, Properties> getProperties();

    /**
     * equivalent to calling getLocal with the root locale
     *
     * @param key
     * @return value from keyValue map (from DB)
     */
    public String getValue(String key);
}
