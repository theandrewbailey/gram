package libWebsiteTools.tag;

import jakarta.servlet.ServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.jsp.JspWriter;
import jakarta.servlet.jsp.PageContext;
import jakarta.servlet.jsp.tagext.SimpleTagSupport;
import java.io.StringWriter;
import java.util.UUID;
import java.util.logging.Logger;
import libWebsiteTools.AllBeanAccess;
import libWebsiteTools.file.BaseFileServlet;
import libWebsiteTools.file.Fileupload;
import libWebsiteTools.imead.Local;

/**
 * Will link all css files described in imead.localization site_css value.
 *
 * @author alpha
 */
public class StyleSheet extends SimpleTagSupport {

    public static final String SITE_CSS_KEY = "site_css";
    private static final String CSP_STYLES = "$_CSP_STYLES";
//    private static final String EXTERNAL_INTEGRITY_TEMPLATE = "<link rel=\"stylesheet\" href=\"%s\" integrity=\"%s-%s\" nonce=\"%s\"/>";
//    private static final String EXTERNAL_TEMPLATE = "<link rel=\"stylesheet\" href=\"%s\" nonce=\"%s\"/>";
    private static final String EXTERNAL_INTEGRITY_TEMPLATE = "<link rel=\"stylesheet\" href=\"%s\" integrity=\"%s-%s\"/>";
    private static final String EXTERNAL_TEMPLATE = "<link rel=\"stylesheet\" href=\"%s\"/>";
    private static final String STYLE_TAG_TEMPLATE = "<style nonce=\"%s\">%s</style>";
    private static final String WARNING = "Encountered style tag '%s' in JSP '%s'. This is contrary to best practices. Try to refactor into an external stylesheet.";
    private static final Logger LOG = Logger.getLogger(StyleSheet.class.getName());

    @SuppressWarnings("unchecked")
    public static List<Fileupload> getCssFiles(AllBeanAccess beans, HttpServletRequest req) {
        try {
            List files = (List) req.getAttribute(SITE_CSS_KEY);
            if (files != null) {
                return files;
            }
        } catch (Exception x) {
        }
        try {
            List<String> filenames = new ArrayList<>();
            List<Fileupload> files = new ArrayList<>();
            for (String filename : beans.getImead().getLocal(SITE_CSS_KEY, Local.resolveLocales(beans.getImead(), req)).split("\n")) {
                List<Fileupload> f = beans.getFile().getFileMetadata(Arrays.asList(filename));
                if (null != f && !f.isEmpty()) {
                    files.addAll(f);
                } else {
                    filenames.add(BaseFileServlet.getNameFromURL(filename));
                }
            }
            files.addAll(beans.getFile().getFileMetadata(filenames));
            req.setAttribute(SITE_CSS_KEY, files);
            return files;
        } catch (Exception x) {
            return new ArrayList<>();
        }
    }

//    @SuppressWarnings("unchecked")
//    public static List<String> getHashes(ServletRequest req) {
//        List<String> hashes = (List) req.getAttribute(CSP_STYLES);
//        if (hashes == null) {
//            hashes = new ArrayList<>();
//            req.setAttribute(CSP_STYLES, hashes);
//        }
//        return hashes;
//    }

    @Override
    public void doTag() throws IOException {
        HttpServletRequest req = ((HttpServletRequest) ((PageContext) getJspContext()).getRequest());
//        List<String> hashes = getHashes(req);
//        try {
//            StringWriter bodyWriter = new StringWriter(1000);
//            getJspBody().invoke(bodyWriter);
//            String body = bodyWriter.toString();
//            if (!body.isEmpty()) {
//                String nonce = UUID.randomUUID().toString();
//                LOG.warning(String.format(WARNING, nonce, req.getRequestURL().toString()));
//                getJspContext().getOut().println(String.format(STYLE_TAG_TEMPLATE, nonce, body));
//                hashes.add("'nonce-" + nonce + "'");
//                return;
//            }
//        } catch (Exception ex) {
//        }
        JspWriter output = getJspContext().getOut();
        AllBeanAccess beans = (AllBeanAccess) req.getAttribute(AllBeanAccess.class.getCanonicalName());
        for (Fileupload f : getCssFiles(beans, (HttpServletRequest) ((PageContext) getJspContext()).getRequest())) {
            String nonce = UUID.randomUUID().toString();
            // TOTAL HACK: this assumes that the StyleSheet is hosted locally 
            try {
                // will create a unique URL based on the file's last update time, so browsers will get and cache a new resource
                String url = f.getUrl();
                // TOTAL HACK: this assumes that the etag is a base64 sha-2 hash of the file contents ONLY, for subresource integrity
                switch (f.getEtag().length()) { // different flavors of sha-2 will have different digest lengths
                    case 44:
                        output.println(String.format(EXTERNAL_INTEGRITY_TEMPLATE, url, "sha256", f.getEtag(), nonce));
//                        hashes.add("'sha256-" + f.getEtag() + "'");
                        break;
                    case 64:
                        output.println(String.format(EXTERNAL_INTEGRITY_TEMPLATE, url, "sha384", f.getEtag(), nonce));
//                        hashes.add("'sha384-" + f.getEtag() + "'");
                        break;
                    case 88:
                        output.println(String.format(EXTERNAL_INTEGRITY_TEMPLATE, url, "sha512", f.getEtag(), nonce));
//                        hashes.add("'sha512-" + f.getEtag() + "'");
                        break;
                    default: // can't recognize
                        output.println(String.format(EXTERNAL_TEMPLATE, url, nonce));
                        break;
                }
//                hashes.add("'nonce-" + nonce + "'");
            } catch (IOException | NullPointerException e) {
                //output.print(String.format(TEMPLATE, css));
            }
        }
    }
}
