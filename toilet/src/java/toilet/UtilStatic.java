package toilet;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class UtilStatic {

    public static final String JSON_OUT = "/WEB-INF/jsonOut.jsp";
    private static final Logger LOG = Logger.getLogger(UtilStatic.class.getName());

    public UtilStatic() {
        throw new UnsupportedOperationException("You cannot instantiate this class");
    }

    /**
     * handy formula to rank things by popularity over time. stolen from hacker
     * news.
     *
     * @param points
     * @param lifetime
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
                //libWebsiteTools.AllBeanAccess.getBean(SecurityRepo.LOCAL_NAME, SecurityRepo.class).logException(null, "Multithread Exception", "Tried to finish a bunch of jobs, but couldn't.", ex);
                throw new RuntimeException(ex);
            }
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
        } catch (NumberFormatException|NullPointerException n) {
            return otherwise;
        }
    }
}
