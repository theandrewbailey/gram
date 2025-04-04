package libWebsiteTools.imead;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jakarta.ejb.EJBException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.jsp.JspException;
import jakarta.servlet.jsp.PageContext;
import jakarta.servlet.jsp.tagext.SimpleTagSupport;
import jakarta.ws.rs.core.HttpHeaders;
import java.io.Writer;
import libWebsiteTools.Landlord;
import libWebsiteTools.Tenant;

/**
 *
 * @author alpha
 */
public class Local extends SimpleTagSupport {

    /**
     * this is the request attribute name that holds the list of user agent
     * locales + server default locale.
     */
    public static final String LOCALE_PARAM = "$_LIBIMEAD_LOCALES";
    /**
     * to override default locale selection, place a java.util.Locale object on
     * the SESSION with this name, and it will have first priority over user
     * agent and server locales. if a resource cannot be resolved for the
     * overridden locale, default behavior will be used.
     */
    public static final String OVERRIDE_LOCALE_PARAM = "$_LIBIMEAD_OVERRIDE_LOCALE";
    public static final Pattern LANG_URL_PATTERN = Pattern.compile("^/([A-Za-z]{2}|x-[A-Za-z\\-]*)(?:(/.*?))?$");
    private String key;
    private List<String> params = new ArrayList<>();

    private String locale;

    /**
     * retrieves locales off request, and sets them on the request.returned list
     * can have (in order):
     *
     * session locale (see OVERRIDE_LOCALE_PARAM) user agent (browser) set
     * locales (see LOCALE_PARAM) server default locale (locale.getDefault())
     * (will always be present)
     *
     * @param req
     * @param imead
     * @return locales or Default and ROOT if req is null
     * @see OVERRIDE_LOCALE_PARAM
     * @see LOCALE_PARAM
     */
    @SuppressWarnings("unchecked")
    public static List<Locale> resolveLocales(IMEADRepository imead, HttpServletRequest req) {
        if (null == req) {
            return List.of(Locale.getDefault(), Locale.ROOT);
        }
        List<Locale> out = (List<Locale>) req.getAttribute(LOCALE_PARAM);
        if (null == out) {
            Collection<Locale> locales = imead.getLocales();
            LinkedHashSet<Locale> lset = new LinkedHashSet<>();
            Locale override;
            Matcher langMatcher = LANG_URL_PATTERN.matcher(req.getServletPath());
            if (langMatcher.find()) {
                override = locales.stream().filter((l) -> l.toLanguageTag().equals(langMatcher.group(1))).findAny().orElse(null);
                if (null != override && !Locale.ROOT.equals(override) && imead.getLocales().contains(override)) {
                    req.setAttribute(Local.OVERRIDE_LOCALE_PARAM, override);
                }
            }
            override = (Locale) req.getAttribute(OVERRIDE_LOCALE_PARAM);
            if (null != req.getSession(false) && null != req.getSession(false).getAttribute(OVERRIDE_LOCALE_PARAM)) {
                override = (Locale) req.getSession(false).getAttribute(OVERRIDE_LOCALE_PARAM);
            }
            if (null != override && !lset.contains(override)) {
                lset.add(override);
            } else if (null != req.getHeader(HttpHeaders.ACCEPT_LANGUAGE)) {
                List<Locale.LanguageRange> ranges = Locale.LanguageRange.parse(req.getHeader(HttpHeaders.ACCEPT_LANGUAGE));
                for (Locale l : Locale.filter(ranges, locales)) {
                    if (locales.contains(l)) {
                        lset.add(l);
                        break;
                    }
                }
            } else if (locales.contains(Locale.forLanguageTag(Locale.getDefault().getLanguage()))) {
                lset.add(Locale.getDefault());
            }
            lset.add(Locale.ROOT);
            out = List.copyOf(lset);
            req.setAttribute(LOCALE_PARAM, out);
        }
        return out;
    }

    public static String getLocaleString(IMEADRepository imead, HttpServletRequest req) {
        ArrayList<String> langTags = new ArrayList<>();
        for (Locale l : resolveLocales(imead, req)) {
            langTags.add(l.toString());
        }
        return String.join(", ", langTags);
    }

    @SuppressWarnings({"unchecked", "UseSpecificCatch", "ThrowableResultIgnored"})
    protected String getValue() {
        try {
            getJspBody().invoke(Writer.nullWriter());
        } catch (Exception n) {
        }
        HttpServletRequest req = ((HttpServletRequest) ((PageContext) getJspContext()).getRequest());
        Tenant ten = Landlord.getTenant(req);
        try {
            if (locale != null) {
                return MessageFormat.format(ten.getImead().getLocal(getKey(), locale), getParams().toArray());
            }
            return MessageFormat.format(ten.getImead().getLocal(getKey(), resolveLocales(ten.getImead(), (HttpServletRequest) ((PageContext) getJspContext()).getRequest())), getParams().toArray());
        } catch (EJBException e) {
            if (!(e.getCause() instanceof LocalizedStringNotFoundException)) {
                throw e;
            }
        } catch (NullPointerException n) {
        }
        return "";
    }

    @Override
    public void doTag() throws JspException, IOException {
        try {
            getJspContext().getOut().print(getValue());
        } catch (IOException x) {
            // don't do anything
        }
    }

    public void setLocale(String l) {
        locale = l;
    }

    public void setKey(String k) {
        key = k;
    }

    public String getKey() {
        return key;
    }

    public void setParams(List<String> p) {
        params = p;
    }

    public List<String> getParams() {
        return params;
    }
}
