package libWebsiteTools.tag;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;
import jakarta.servlet.jsp.JspException;
import jakarta.servlet.jsp.PageContext;
import jakarta.servlet.jsp.tagext.SimpleTagSupport;
import java.util.UUID;
import libWebsiteTools.security.GuardFilter;
import libWebsiteTools.security.HashUtil;

/**
 * base class for all input elements
 *
 * @author alpha
 */
public abstract class AbstractInput extends SimpleTagSupport {

    public static final String DISABLE_FIELDNAME_OBFUSCATION = "$_LIBWEBSITETOOLS_DISABLE_FIELDNAME_OBFUSCATION";
    public static final String DISABLE_REFERRER_CHECKING = "$_LIBWEBSITETOOLS_DISABLE_REQUEST_TOKEN_REFERRER_CHECKING";
    public static final String ORIGINAL_REQUEST_URL = "$_LIBWEBSITETOOLS_ORIGINAL_REQUEST_URL";
    public static final String LAST_CHECKBOX_ID = "$_LAST_CHECKBOX_ID";
    public static final String DEFAULT_PATTERN = "^[\\u000A\\u000D\\u0020-\\uFFFF\\u20000-\\u2FFFF]*$";
    public static final Pattern DEFAULT_REGEXP = Pattern.compile(DEFAULT_PATTERN);

    public static final String[] INPUT_MODES = new String[]{"verbatim", "latin", "latin-name", "latin-prose",
        "full-width-latin", "kana", "katakana", "numeric", "tel", "email", "url"};
    public static final String[] AUTOCOMPLETE = new String[]{"on", "off", "name", "honorific-prefix", "given-name",
        "additional-name", "family-name", "honorific-suffix", "nickname",
        "email", "username", "new-password", "current-password", "organization-title", "organization",
        "street-address", "address-line1", "address-line2", "address-line3", "address-line4",
        "address-level4", "address-level3", "address-level2", "address-level1",
        "country", "country-name", "postal-code",
        "cc-name", "cc-given-name", "cc-additional-name", "cc-family-name", "cc-number",
        "cc-exp", "cc-exp-month", "cc-exp-year", "cc-csc", "cc-type",
        "transaction-currency", "transaction-amount", "language",
        "bday", "bday-day", "bday-month", "bday-year",
        "sex", "tel", "url", "photo"};

    static {
        Arrays.sort(INPUT_MODES);
        Arrays.sort(AUTOCOMPLETE);
    }

    private String accesskey;
    private Boolean checked = false;
    private String id;
    private String name;
    private String label;
    private Boolean labelNextLine = true;
    private Integer length;
    private Integer maxLength;
    private Integer size;
    private String styleClass;
    private Integer tabindex;
    private String value;
    private String title;
    private Boolean autofocus = false;
    private Boolean disabled = false;
    private Boolean required = false;
    private Boolean multiple = false;
    private String inputMode;
    private String autocomplete;
    protected String valueMissing;
    protected String pattern;
    protected String patternMismatch;
    protected HttpServletRequest req;
    private String cachedName;
    private String placeholder;

    public static String getParameter(HttpServletRequest req, String parameter) {
        return req.getParameter(getIncomingHash(req, parameter));
    }

    public static Part getPart(HttpServletRequest req, String name) throws IOException, ServletException {
        return req.getPart(getIncomingHash(req, name));
    }

    public static List<Part> getParts(HttpServletRequest req, String name) throws IOException, ServletException {
        String lookfor = getIncomingHash(req, name);
        List<Part> parts = new ArrayList<>();
        for (Part p : req.getParts()) {
            if (lookfor.equals(p.getName())) {
                parts.add(p);
            }
        }
        return parts;
    }

    public static String getTokenURL(HttpServletRequest req) {
        if (null != req.getAttribute(ORIGINAL_REQUEST_URL)) {
            return req.getAttribute(ORIGINAL_REQUEST_URL).toString();
        }
        StringBuilder urlBuf = new StringBuilder().
                append(GuardFilter.isSecure(req) ? "https://" : "http://").
                append(req.getHeader("Host"));
        /*String port = req.getHeader("x-forwarded-port");
        if (null != port && !"80".equals(port) && !"443".equals(port)) {
            urlBuf.append(":").append(port);
        } else if (80 != req.getServerPort() && 443 != req.getServerPort()) {
            urlBuf.append(":").append(req.getServerPort());
        }*/
        urlBuf.append(req.getRequestURI());
        if (req.getQueryString() != null) {
            urlBuf.append("?").append(req.getQueryString());
        }
        String url = urlBuf.toString();
        req.setAttribute(ORIGINAL_REQUEST_URL, url);
        return url;
    }

    public static String getIncomingHash(HttpServletRequest req, String str) {
        if (null != req.getServletContext().getAttribute(DISABLE_FIELDNAME_OBFUSCATION)
                || (null != req.getSession(false)
                && null != req.getSession().getAttribute(DISABLE_FIELDNAME_OBFUSCATION))) {
            return str;
        }
        if (null != req.getServletContext().getAttribute(DISABLE_REFERRER_CHECKING)
                || (null != req.getSession(false)
                && null != req.getSession().getAttribute(DISABLE_REFERRER_CHECKING))) {
            return HashUtil.getHmacSHA256Hash(req.getSession().getId(), str);
        }
        String url = req.getHeader("referer");
        if (url.endsWith("?")) {
            url = url.substring(0, url.length() - 1);
        }
        return HashUtil.getHmacSHA256Hash(req.getSession().getId(), url + str);
    }

    public static String getOutgoingHash(HttpServletRequest req, String str) {
        if (null != req.getServletContext().getAttribute(DISABLE_FIELDNAME_OBFUSCATION)
                || (null != req.getSession(false)
                && null != req.getSession().getAttribute(DISABLE_FIELDNAME_OBFUSCATION))) {
            return str;
        }
        if (null != req.getServletContext().getAttribute(DISABLE_REFERRER_CHECKING)
                || (null != req.getSession(false)
                && null != req.getSession().getAttribute(DISABLE_REFERRER_CHECKING))) {
            return HashUtil.getHmacSHA256Hash(req.getSession().getId(), str);
        }
        String url = getTokenURL(req);
        return HashUtil.getHmacSHA256Hash(req.getSession().getId(), url + str);
    }

    public static String escapeAttribute(String attr) {
        if (null == attr) {
            return null;
        }
        StringBuilder out = new StringBuilder(attr.length() * 2);
        for (int i = 0; i < attr.length(); i++) {
            switch (attr.charAt(i)) {
                case '<':
                    out.append("&lt;");
                    break;
                case '>':
                    out.append("&gt;");
                    break;
                case '&':
                    out.append("&amp;");
                    break;
                case '"':
                    out.append("&quot;");
                    break;
                case '\'':
                    out.append("&#x27;");
                    break;
                case '/':
                    out.append("&#x2F;");
                    break;
                default:
                    out.append(attr.charAt(i));
            }
        }
        return out.toString();
    }

    public abstract String getType();

    @Override
    public void doTag() throws JspException, IOException {
        req = (HttpServletRequest) ((PageContext) getJspContext()).getRequest();
        getJspContext().getOut().print(createTag());
    }

    protected StringBuilder label(StringBuilder out) {
        if (null == getId()) {
            setId(UUID.randomUUID().toString());
        }
        if (null != getLabel()) {
            out.append("<label for=\"").append(getId());
            out.append("\">");
            out.append(getLabel());
            out.append(getLabelNextLine() ? "</label><br/>" : "</label>");
        }
        getJspContext().setAttribute(LAST_CHECKBOX_ID, getId());
        return out;
    }

    /**
     * @return all but the final "/>" of the input tag (will close all attribute
     * quotes)
     */
    protected StringBuilder createIncompleteTag() {
        StringBuilder out = new StringBuilder(300);
        out.append("<input name=\"").append(getName()).append("\" type=\"").append(getType());
        if (null != getId()) {
            out.append("\" id=\"").append(getId());
        }
        if (null != getAccesskey()) {
            out.append("\" accesskey=\"").append(getAccesskey());
        }
        if (getChecked()) {
            out.append("\" checked=\"checked");
        }
        if (getAutofocus()) {
            out.append("\" autofocus=\"autofocus");
        }
        if (getDisabled()) {
            out.append("\" disabled=\"disabled");
        }
        if (getRequired()) {
            out.append("\" required=\"required");
        }
        if (null != getValueMissing()) {
            out.append("\" data-valuemissing=\"").append(escapeAttribute(getValueMissing()));
        }
        if (null != getLength()) {
            out.append("\" length=\"").append(getLength().toString());
        }
        if (null != getMaxLength()) {
            out.append("\" maxlength=\"").append(getMaxLength().toString());
        }
        if (null != getSize()) {
            out.append("\" size=\"").append(getSize().toString());
        }
        if (null != getStyleClass()) {
            out.append("\" class=\"").append(escapeAttribute(getStyleClass()));
        }
        if (null != getPattern()) {
            out.append("\" pattern=\"").append(getPattern());
        }
        if (null != getPatternMismatch()) {
            out.append("\" data-patternmismatch=\"").append(escapeAttribute(getPatternMismatch()));
        }
        if (null != getTabindex()) {
            out.append("\" tabindex=\"").append(getTabindex().toString());
        }
        if (null != getTitle()) {
            out.append("\" title=\"").append(escapeAttribute(getTitle()));
        }
        if (null != getValue()) {
            out.append("\" value=\"").append(escapeAttribute(getValue()));
        }
        if (null != getPlaceholder()) {
            out.append("\" placeholder=\"").append(escapeAttribute(getPlaceholder()));
        }
        if (null != inputMode && 0 <= Arrays.binarySearch(INPUT_MODES, inputMode)) {
            out.append("\" inputmode=\"").append(inputMode);
        }
        if (multiple) {
            out.append("\" multiple=\"true");
        }
        if (null != autocomplete && 0 <= Arrays.binarySearch(AUTOCOMPLETE, autocomplete)) {
            out.append("\" autocomplete=\"").append(autocomplete);
        }
        return out.append("\"");
    }

    /**
     * This is the entrypoint for tags. Whatever comes out of this is all that
     * gets put on the page.
     *
     * @return
     */
    public StringBuilder createTag() {
        return label(new StringBuilder(400)).append(createIncompleteTag()).append("/>");
    }

    public String getAccesskey() {
        return accesskey;
    }

    public void setAccesskey(String accesskey) {
        this.accesskey = accesskey;
    }

    public Boolean getChecked() {
        return checked;
    }

    public void setChecked(Boolean checked) {
        if (checked != null) {
            this.checked = checked;
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        if (cachedName == null && req != null) {
            cachedName = getOutgoingHash(req, name);
        }
        return cachedName != null ? cachedName : name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Integer getLength() {
        return length;
    }

    public void setLength(Integer length) {
        this.length = length;
    }

    public Integer getTabindex() {
        return tabindex;
    }

    public void setTabindex(Integer tabindex) {
        this.tabindex = tabindex;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getStyleClass() {
        return styleClass;
    }

    public void setStyleClass(String styleClass) {
        this.styleClass = styleClass;
    }

    public Integer getMaxLength() {
        return maxLength;
    }

    public void setMaxLength(Integer maxLength) {
        this.maxLength = maxLength;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public Boolean getLabelNextLine() {
        return labelNextLine;
    }

    public void setLabelNextLine(Boolean labelNextLine) {
        this.labelNextLine = labelNextLine;
    }

    public Boolean getAutofocus() {
        return autofocus;
    }

    public void setAutofocus(Boolean autofocus) {
        this.autofocus = autofocus;
    }

    public Boolean getDisabled() {
        return disabled;
    }

    public void setDisabled(Boolean disabled) {
        this.disabled = disabled;
    }

    public Boolean getRequired() {
        return required;
    }

    public void setRequired(Boolean required) {
        this.required = required;
    }

    public String getPattern() {
        return null != pattern ? pattern : AbstractInput.DEFAULT_PATTERN;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public String getValueMissing() {
        return valueMissing;
    }

    public void setValueMissing(String valueMissing) {
        this.valueMissing = valueMissing;
    }

    public String getPatternMismatch() {
        return patternMismatch;
    }

    public void setPatternMismatch(String patternMismatch) {
        this.patternMismatch = patternMismatch;
    }

    public void setMultiple(boolean multiple) {
        this.multiple = multiple;
    }

    public void setInputMode(String inputMode) {
        this.inputMode = inputMode;
    }

    public void setAutocomplete(String autocomplete) {
        this.autocomplete = autocomplete;
    }

    public String getPlaceholder() {
        return placeholder;
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
    }
}
