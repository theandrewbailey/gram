package gram;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import libWebsiteTools.imead.IMEADHolder;
import libWebsiteTools.imead.Localization;

public final class UtilStatic {

    public static final String INITIAL_PROPERTIES_FILE = "/IMEAD.properties";
    public static final String JSON_OUT = "/WEB-INF/jsonOut.jsp";
    private static final Logger LOG = Logger.getLogger(UtilStatic.class.getName());

    public UtilStatic() {
        throw new UnsupportedOperationException("You cannot instantiate this class");
    }

    /**
     * reads a properties file, and returns a properties object
     *
     * @param file properties file
     * @return a properties object
     * @throws IOException
     */
    public static Properties getProperties(InputStream file) throws IOException {
        Properties IMEAD = new Properties();
        IMEAD.load(file);
        return IMEAD;
    }

    /**
     *
     * @return initial default properties
     * @throws IOException
     */
    public static Properties getInitialProperties() throws IOException {
        return getProperties(UtilStatic.class.getResourceAsStream(INITIAL_PROPERTIES_FILE));
    }

    /**
     * reads a properties file, and returns Localization objects that aren't
     * already loaded. this will not return Localizations that are changed; only
     * ones that aren't already present.
     *
     * @param imead
     * @param props
     * @param locale
     * @return
     * @throws IOException
     */
    public static List<Localization> getNewLocalizations(IMEADHolder imead, Properties props, Locale locale) throws IOException {
        List<Localization> locals = new ArrayList<>();
        for (Map.Entry<Object, Object> property : props.entrySet()) {
            try {
                String test = imead.getLocal(property.getKey().toString(), locale.toString());
                test.toLowerCase();
            } catch (RuntimeException r) {
                locals.add(new Localization(locale.toString(), property.getKey().toString(), property.getValue().toString()));
            }
        }
        return locals;
    }

    /**
     * handy formula to rank things by popularity over time. stolen from hacker
     * news.
     *
     * @param points number of votes, events, etc.
     * @param lifetime units are unspecified (seconds, days, years, etc.), but
     * they must be the same
     * @param gravity exponential decay factor, try using 1.8
     * @return
     */
    public static double score(double points, double lifetime, double gravity) {
        return points / Math.pow(lifetime, gravity);
    }

    public static Collection<Future> finish(Collection<Future> these) {
        for (Future task : these) {
            try {
                task.get();
            } catch (InterruptedException | ExecutionException ex) {
                LOG.log(Level.SEVERE, "Tried to finish a bunch of jobs, but couldn't.", ex);
                throw new RuntimeException(ex);
            }
        }
        return these;
    }

    public static Collection<CompletableFuture> join(Collection<CompletableFuture> these) {
        for (CompletableFuture task : these) {
            task.join();
        }
        return these;
    }

    /**
     * removes gratoutious amounts of spaces in the given string
     *
     * @param in input string
     * @return sans extra spaces
     */
    public static String removeSpaces(String in) {
        StringBuilder sb = new StringBuilder();
        for (String r : in.split(" ")) {
            if (!r.isEmpty()) {
                sb.append(r);
                sb.append(' ');
            }
        }
        return sb.toString().trim();
    }

    /**
     * removes validation breaking characters from the given string
     *
     * @param in input string
     * @param link keep "<" and ">", preserving embedded links
     * @param addPtags
     * @param addBreaks convert line breaks to br tags
     * @return formatted string
     */
    public static String htmlFormat(String in, boolean link, boolean addPtags, boolean addBreaks) {
        StringBuilder sb = new StringBuilder(in.length() + 1000);
        if (addPtags) {
            sb.append("<p>");
        }
        in = removeSpaces(in);
        boolean inBrack = false;
        for (char c : in.toCharArray()) {
            switch (c) {
                case '<':
                    if (!link) {
                        sb.append("&lt;");
                    } else if (!inBrack) {
                        sb.append(c);
                        inBrack = true;
                    }
                    break;
                case '>':
                    if (!link) {
                        sb.append("&gt;");
                    } else if (inBrack) {
                        sb.append(c);
                        inBrack = false;
                    }
                    break;
                case '"':
                    if (!inBrack) {
                        sb.append("&quot;");
                    } else {
                        sb.append(c);
                    }
                    break;
                case '\'':
                    if (!inBrack) {
                        sb.append("&#39;");
                    } else {
                        sb.append(c);
                    }
                    break;
                case '&':
                    if (link) {
                        sb.append(c);
                    } else if (!inBrack) {
                        sb.append("&amp;");
                    } else {
                        sb.append(c);
                    }
                    break;
                case '\r':
                    continue;
                case '\n':
                    if (addBreaks && !inBrack) {
                        sb.append("<br/>");
                    } else {
                        sb.append(c);
                    }
                    break;
                default:
                    sb.append(c);
            }
        }
        if (addPtags) {
            sb.append("</p>");
        }
        String out = sb.toString().replace("<br/><br/>", "</p>\n<p>");
        return out;
    }

    /**
     * reverses htmlFormat
     *
     * @param s input string
     * @param link
     * @return
     */
    public static String htmlUnformat(String s, boolean link) {
        String out = s.replace("</p>\n<p>", "\n\n").substring(3);
        out = out.substring(0, out.length() - 5);
        out = out.replace("</p><p>", "\n\n").replace("&quot;", "\"").replace("<br/>", "\n").replace("&", "&amp;");   // need to include amp, because browsers won't take it literally.
        out = link ? out : out.replace("&lt;", "<").replace("&gt;", ">");
        return out;
    }

    public static BigDecimal parseDecimal(String str, BigDecimal otherwise) {
        try {
            return new BigDecimal(str);
        } catch (NumberFormatException | NullPointerException n) {
            return otherwise;
        }
    }
}
