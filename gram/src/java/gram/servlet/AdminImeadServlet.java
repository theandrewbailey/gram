package gram.servlet;

import gram.AdminPermission;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.time.Instant;
import libWebsiteTools.security.SecurityRepository;
import libWebsiteTools.imead.Local;
import libWebsiteTools.imead.Localization;
import libWebsiteTools.imead.LocalizationPK;
import libWebsiteTools.security.HashUtil;
import libWebsiteTools.turbo.RequestTimer;
import libWebsiteTools.tag.AbstractInput;
import gram.bean.GramLandlord;
import gram.bean.GramTenant;
import java.util.Collection;
import libWebsiteTools.Markdowner;
import libWebsiteTools.file.FileCompressorJob;
import libWebsiteTools.file.FileUtil;
import libWebsiteTools.file.Fileupload;
import libWebsiteTools.imead.LocalizedStringNotFoundException;

/**
 *
 * @author alpha
 */
@WebServlet(name = "AdminImead", description = "Edit IMEAD properties", urlPatterns = {"/adminImead"})
public class AdminImeadServlet extends AdminServlet {

    public static final String ADMIN_IMEAD = "WEB-INF/admin/adminImead.jsp";
    private static final String SITE_ARGON2_PARAMETERS = "site_argon2_parameters";
    private static final String ALLOWED_ORIGINS_TEMPLATE = "%s\n^https?://(?:10\\.[0-9]{1,3}\\.|192\\.168\\.)[0-9]{1,3}\\.[0-9]{1,3}(?::[0-9]{1,5})?(?:/.*)?$\n^https?://(?:[a-zA-Z]+\\.)+?google(?:\\.com)?(?:\\.[a-zA-Z]{2}){0,2}(?:$|/.*)\n^https?://(?:[a-zA-Z]+\\.)+?googleusercontent(?:\\.com)?(?:\\.[a-zA-Z]{2}){0,2}(?:$|/.*)\n^https?://(?:[a-zA-Z]+\\.)+?feedly\\.com(?:$|/.*)\n^https?://(?:[a-zA-Z]+\\.)+?slack\\.com(?:$|/.*)\n^https?://(?:[a-zA-Z]+\\.)+?bing\\.com(?:$|/.*)\n^https?://(?:[a-zA-Z]+\\.)+?yandex(?:\\.com)?(?:\\.[a-zA-Z]{2})?(?:/.*)?$\n^https?://images\\.rambler\\.ru(?:$|/.*)\n^https?://(?:[a-zA-Z]+\\.)+?yahoo(?:\\.com)?(?:\\.[a-zA-Z]{2})?(?:/.*)?$\n^https?://(?:[a-zA-Z]+\\.)+?duckduckgo\\.com(?:$|/.*)\n^https?://(?:[a-zA-Z]+\\.)+?baidu\\.com(?:$|/.*)";

    @Override
    public AdminPermission[] getRequiredPermissions() {
        return new AdminPermission[]{AdminPermission.Password.IMEAD, AdminPermission.FIRSTTIME};
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        GramTenant ten = GramLandlord.getTenant(request);
        Instant start = Instant.now();
        if (ten.getImead().isFirstTime()) {
            if (null == ten.getImeadValue(SecurityRepository.BASE_URL)) {
                ArrayList<Localization> locals = new ArrayList<>();
                String canonicalRoot = AbstractInput.getTokenURL(request);
                if (!canonicalRoot.endsWith("/")) {
                    canonicalRoot += "/";
                }
                Matcher originMatcher = SecurityRepository.ORIGIN_PATTERN.matcher(canonicalRoot);
                if (originMatcher.matches()) {
                    String currentReg = originMatcher.group(2).replace(".", "\\.");
                    locals.add(new Localization("", SecurityRepository.ALLOWED_ORIGINS, String.format(ALLOWED_ORIGINS_TEMPLATE, currentReg)));
                    locals.add(new Localization("", SecurityRepository.BASE_URL, canonicalRoot));
                }
                ten.getImead().upsert(locals);
                ten.getImead().evict();
                // load default files
                checkAndLoadFile(ten, "gram-vaporwave.css", "text/css");
                checkAndLoadFile(ten, "gram.js", "text/javascript");
                ten.getArts().refreshSearch();
                request.setAttribute(SecurityRepository.BASE_URL, canonicalRoot);
            }
            request.setAttribute("FIRST_TIME_SETUP", "FIRST_TIME_SETUP");
        }
        RequestTimer.addTiming(request, "query", Duration.between(start, Instant.now()));
        showProperties(ten, request, response);
    }

    private static void checkAndLoadFile(GramTenant ten, String filename, String type) throws IOException {
        Collection<Fileupload> files = new ArrayList<>();
        if (ten.getFile().getFileMetadata(List.of(filename)).isEmpty()) {
            files.addAll(ten.getFile().upsert(List.of(
                    FileUtil.loadFile(ten, filename, "text/css", AdminImeadServlet.class.getResourceAsStream("/" + filename)))));
        }
        for (Fileupload file : files) {
            FileCompressorJob.startAllJobs(ten, file);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        GramTenant ten = GramLandlord.getTenant(request);
        Instant start = Instant.now();
        boolean initialFirstTime = ten.getImead().isFirstTime();
        // save things
        String action = AbstractInput.getParameter(request, "action");
        if (null == action) {
        } else if ("save".equals(action) || "".equals(action)) {
            ArrayList<Localization> props = new ArrayList<>();
            ArrayList<Localization> overrides = new ArrayList<>();
            HashSet<LocalizationPK> errors = new HashSet<>();
            request.setAttribute("ERRORS", errors);
            String argon2_parameters = ten.getImeadValue(SITE_ARGON2_PARAMETERS);
            for (Localization l : new LocalizationRetriever(request)) {
                String previousValue = null;
                try {
                    previousValue = ten.getImead().getLocal(l.getLocalizationPK().getKey(), l.getLocalizationPK().getLocalecode());
                } catch (LocalizedStringNotFoundException g) {
                }
                if (!l.getValue().equals(previousValue)) {
                    if (l.getLocalizationPK().getKey().startsWith("admin_")
                            && !HashUtil.ARGON2_ENCODING_PATTERN.matcher(l.getValue()).matches()) {
                        if (null != previousValue && !HashUtil.ARGON2_ENCODING_PATTERN.matcher(previousValue).matches() && previousValue.equals(l.getValue())) {
                            errors.add(l.getLocalizationPK());
                            request.setAttribute(GramServlet.ERROR_MESSAGE_PARAM, ten.getImead().getLocal("page_error_adminadmin", Local.resolveLocales(ten.getImead(), request)));
                        }
                        l.setValue(HashUtil.getArgon2Hash(argon2_parameters, l.getValue()));
                    }
                    if (l.getLocalizationPK().getKey().endsWith("_markdown")) {
                        overrides.add(new Localization(l.getLocalizationPK().getLocalecode(), l.getLocalizationPK().getKey().replaceFirst("_markdown$", ""), Markdowner.getHtml(l.getValue())));
                    }
                    props.add(l);
                }
            }
            props.addAll(overrides);
            if (errors.isEmpty() && !props.isEmpty()) {
                ten.getImead().upsert(props);
            }
        } else if (action.startsWith("delete")) {
            String[] params = action.split("\\|");
            ten.getImead().delete(new LocalizationPK(params[2], params[1]));
        }
        ten.reset();
        RequestTimer.addTiming(request, "save", Duration.between(start, Instant.now()));
        if (initialFirstTime && !ten.getImead().isFirstTime()) {
            request.getSession().invalidate();
            response.sendRedirect(ten.getImeadValue(SecurityRepository.BASE_URL));
        } else {
            showProperties(ten, request, response);
        }
    }

    private static class LocalizationRetriever implements Iterable<Localization>, Iterator<Localization> {

        private final HttpServletRequest req;
        private int current = -1;

        public LocalizationRetriever(HttpServletRequest req) {
            this.req = req;
        }

        @Override
        public boolean hasNext() {
            String key = AbstractInput.getParameter(req, "key" + (current + 1));
            return null != key && !key.isEmpty();
        }

        @Override
        public Localization next() {
            if (hasNext()) {
                ++current;
                return new Localization(AbstractInput.getParameter(req, "locale" + current).trim(), AbstractInput.getParameter(req, "key" + current).trim(), AbstractInput.getParameter(req, "value" + current));
            } else {
                throw new NoSuchElementException();
            }
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Iterator<Localization> iterator() {
            return this;
        }
    }

    public static void showProperties(GramTenant ten, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Map<Locale, List<Localization>> imeadProperties = new HashMap<>();
        for (Localization L : ten.getImead().getAll(null)) {
            Locale locale = new Locale(L.getLocalizationPK().getLocalecode());
            if (!imeadProperties.containsKey(locale)) {
                imeadProperties.put(locale, new ArrayList<>());
            }
            imeadProperties.get(locale).add(L);
        }
        List<Localization> security = new ArrayList<>();
        for (Localization property : imeadProperties.get(Locale.forLanguageTag(""))) {
            if (property.getLocalizationPK().getKey().startsWith("admin_") || property.getLocalizationPK().getKey().startsWith("site_security_")) {
                security.add(property);
            }
        }
        for (Localization property : security) {
            imeadProperties.get(Locale.forLanguageTag("")).remove(property);
        }
        request.setAttribute("security", security);
        request.setAttribute("imeadProperties", imeadProperties);
        List<String> locales = new ArrayList<>();
        for (Locale l : ten.getImead().getLocales()) {
            locales.add(l.toLanguageTag());
        }
        request.setAttribute("locales", locales);
        request.getRequestDispatcher(ADMIN_IMEAD).forward(request, response);
    }
}
