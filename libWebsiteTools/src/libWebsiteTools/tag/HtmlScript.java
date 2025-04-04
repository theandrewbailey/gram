package libWebsiteTools.tag;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.jsp.PageContext;
import jakarta.servlet.jsp.tagext.SimpleTagSupport;
import java.io.StringWriter;
import java.util.UUID;
import java.util.logging.Logger;
import libWebsiteTools.file.BaseFileServlet;
import libWebsiteTools.file.Fileupload;
import libWebsiteTools.imead.Local;
import libWebsiteTools.Landlord;
import libWebsiteTools.Tenant;
import libWebsiteTools.security.SecurityRepository;

/**
 *
 * @author alpha
 */
public class HtmlScript extends SimpleTagSupport {

    public static final String SITE_JAVASCRIPT_KEY = "site_javascript";
    private static final String CSP_SCRIPTS = "$_CSP_SCRIPTS";
    private static final String EXTERNAL_INTEGRITY_TEMPLATE = "<script type=\"%s\" src=\"%s\" async=\"async\" integrity=\"%s-%s\" nonce=\"%s\"></script>";
    private static final String EXTERNAL_TEMPLATE = "<script type=\"%s\" src=\"%s\" async=\"async\" nonce=\"%s\"></script>";
    private static final String SCRIPT_TAG_TEMPLATE = "<script type=\"%s\" nonce=\"%s\">%s</script>";
    private static final String WARNING = "Encountered inline javascript '%s' in JSP '%s'. This is contrary to best practices. Try to refactor into an external script, and use tag data- attributes to pass data.";
    private static final Logger LOG = Logger.getLogger(HtmlScript.class.getName());

    @SuppressWarnings("unchecked")
    public static List<Fileupload> getJavascriptFiles(Tenant ten, HttpServletRequest req) {
        try {
            List files = (List) req.getAttribute(SITE_JAVASCRIPT_KEY);
            if (files != null) {
                return files;
            }
        } catch (Exception x) {
        }
        List<Fileupload> files = new ArrayList<>();
        try {
            List<String> filenames = new ArrayList<>();
            for (String filename : ten.getImead().getLocal(SITE_JAVASCRIPT_KEY, Local.resolveLocales(ten.getImead(), req)).split("\n")) {
                List<Fileupload> f = ten.getFile().getFileMetadata(Arrays.asList(filename));
                if (null != f && !f.isEmpty()) {
                    files.addAll(f);
                } else {
                    filenames.add(BaseFileServlet.getNameFromURL(filename));
                }
            }
            files.addAll(ten.getFile().getFileMetadata(filenames));
            req.setAttribute(SITE_JAVASCRIPT_KEY, List.copyOf(files));
        } catch (Exception x) {
        }
        return files;
    }

    @SuppressWarnings("unchecked")
    public static List<String> getHashes(HttpServletRequest req) {
        List<String> hashes = (List) req.getAttribute(CSP_SCRIPTS);
        if (hashes == null) {
            hashes = new ArrayList<>();
            req.setAttribute(CSP_SCRIPTS, hashes);
        }
        return hashes;
    }

    @Override
    public void doTag() throws IOException {
        HttpServletRequest req = ((HttpServletRequest) ((PageContext) getJspContext()).getRequest());
        List<String> hashes = getHashes(req);
        try {
            StringWriter bodyWriter = new StringWriter(1000);
            getJspBody().invoke(bodyWriter);
            String body = bodyWriter.toString();
            if (!body.isEmpty()) {
                String nonce = UUID.randomUUID().toString();
                LOG.warning(String.format(WARNING, nonce, req.getRequestURL().toString()));
                getJspContext().getOut().println(String.format(SCRIPT_TAG_TEMPLATE, "text/javascript", nonce, body));
                hashes.add("'nonce-" + nonce + "'");
                return;
            }
        } catch (Exception ex) {
        }
        Tenant ten = Landlord.getTenant(req);
        for (Fileupload f : getJavascriptFiles(ten, (HttpServletRequest) ((PageContext) getJspContext()).getRequest())) {
            String nonce = UUID.randomUUID().toString();
            // TOTAL HACK: this assumes that the CSS is hosted locally 
            try {
                // will create a unique URL based on the file's last update time, so browsers will get and cache a new resource
                String url = ten.getImeadValue(SecurityRepository.BASE_URL) + f.getUrl();
                // TOTAL HACK: this assumes that the etag is a base64 sha-2 hash of the file contents ONLY, for subresource integrity
                switch (f.getEtag().length()) { // different flavors of sha-2 will have different digest lengths
                    case 44:
                        getJspContext().getOut().println(String.format(EXTERNAL_INTEGRITY_TEMPLATE, f.getMimetype(), url, "sha256", f.getEtag(), nonce));
//                        hashes.add("'sha256-" + f.getEtag() + "'");
                        break;
                    case 64:
                        getJspContext().getOut().println(String.format(EXTERNAL_INTEGRITY_TEMPLATE, f.getMimetype(), url, "sha384", f.getEtag(), nonce));
//                        hashes.add("'sha384-" + f.getEtag() + "'");
                        break;
                    case 88:
                        getJspContext().getOut().println(String.format(EXTERNAL_INTEGRITY_TEMPLATE, f.getMimetype(), url, "sha512", f.getEtag(), nonce));
//                        hashes.add("'sha512-" + f.getEtag() + "'");
                        break;
                    default: // can't recognize
                        getJspContext().getOut().println(String.format(EXTERNAL_TEMPLATE, f.getMimetype(), url, nonce));
                        break;
                }
                hashes.add("'nonce-" + nonce + "'");
            } catch (IOException | NullPointerException e) {
                //getJspContext().getOut().print(String.format("<script type=\"text/javascript\" src=\"%s\" async=\"true\"></script>", javascript));
            }
        }
    }
}
