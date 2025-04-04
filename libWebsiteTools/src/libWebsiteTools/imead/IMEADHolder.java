package libWebsiteTools.imead;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IllformedLocaleException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Internationalization Made Easy And Dynamic
 *
 * @author alpha
 */
public abstract class IMEADHolder implements IMEADRepository {

    private static final Logger LOG = Logger.getLogger(IMEADHolder.class.getName());
    protected Map<Locale, Properties> localizedCache = new HashMap<>();
    protected final Map<String, List<Pattern>> patterns = new HashMap<>();
    protected final Map<String, Map<Locale, Map<String, String>>> filteredCache = new HashMap<>();
    protected String localizedHash = "";

    @Override
    public List<Localization> search(Object term, Integer limit) {
        throw new UnsupportedOperationException();
    }

    /**
     * load all properties from DB
     *
     * @return map of Locale to Properties
     */
    @Override
    public Map<Locale, Properties> getProperties() {
        Map<Locale, Properties> output = new HashMap<>();
        for (Localization l : getAll(null)) {
            Locale local;
            if (l.getLocalizationPK().getLocalecode().startsWith("x-")) {
                String sub = l.getLocalizationPK().getLocalecode().substring(2);
                try {
                    local = new Locale.Builder().setExtension(Locale.PRIVATE_USE_EXTENSION, sub).build();
                } catch (IllformedLocaleException m) {
                    continue;
                }
            } else {
                local = Locale.forLanguageTag(l.getLocalizationPK().getLocalecode());
            }
            if (!output.containsKey(local)) {
                output.put(local, new SortedProperties());
            }
            Properties props = output.get(local);
            props.put(l.getLocalizationPK().getKey(), l.getValue());
        }
        return output;
    }

    @Override
    public List<Pattern> getPatterns(String key) {
        if (!patterns.containsKey(key) && null != getValue(key)) {
            List<Pattern> temps = new ArrayList<>();
            for (String line : getValue(key).split("\n")) {
                temps.add(Pattern.compile(line.replaceAll("\r", "")));
            }
            patterns.put(key, Collections.unmodifiableList(temps));
        }
        return patterns.get(key);
    }

    public static boolean matchesAny(CharSequence subject, List<Pattern> regexes) {
        if (null != regexes) {
            for (Pattern p : regexes) {
                if (p.matcher(subject).matches()) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public Collection<Locale> getLocales() {
        return localizedCache.keySet();
    }

    /**
     * equivalent to calling getLocal with the root locale
     *
     * @param key
     * @return value from keyValue map (from DB)
     */
    @Override
    public String getValue(String key) {
        try {
            return localizedCache.get(Locale.ROOT).getProperty(key);
        } catch (NullPointerException n) {
            return null;
        }
    }

    /**
     * try to get value for key, using order of given locales
     *
     * @param key
     * @param locales
     * @return value
     * @throws RuntimeException if key cannot be found in locales
     */
    @Override
    public String getLocal(String key, Collection<Locale> locales) {
        for (Locale l : locales) {
            if (localizedCache.containsKey(l)) {
                String retrun = localizedCache.get(l).getProperty(key);
                if (retrun != null) {
                    return retrun;
                }
            }
        }
        String arrayString = Arrays.toString(locales.stream().map((l) -> l.toLanguageTag()).toArray());
        LOG.log(Level.FINER, "Key {0} not found in locales {1}", new Object[]{key, arrayString});
        throw new LocalizedStringNotFoundException(key, arrayString);
    }

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
    @Override
    public String getLocal(String key, String locale) {
        Locale l = Locale.forLanguageTag(locale);
        if (null == l) {
            throw new LocalizedStringNotFoundException("anything (as in, not set up)", locale);
        }
        try {
            return localizedCache.get(l).getProperty(key);
        } catch (NullPointerException n) {
            throw new LocalizedStringNotFoundException(key, locale);
        }
    }

    /**
     * @return the localizedHash
     */
    @Override
    public String getLocalizedHash() {
        return localizedHash;
    }
}
