package gram.tag;

import gram.bean.GramLandlord;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.jsp.JspException;
import jakarta.servlet.jsp.PageContext;
import jakarta.servlet.jsp.tagext.SimpleTagSupport;
import libWebsiteTools.security.SecurityRepo;
import libWebsiteTools.JVMNotSupportedError;
import gram.bean.database.Section;
import gram.bean.GramTenant;

/**
 *
 * @author alpha
 */
public class Categorizer extends SimpleTagSupport {

    private Section category;
    private Integer page;

    @Override
    public void doTag() throws JspException, IOException {
        if (category != null) {
            execute(category);
            return;
        }
        HttpServletRequest req = ((HttpServletRequest) ((PageContext) getJspContext()).getRequest());
        GramTenant ten = GramLandlord.getTenant(req);
        for (Section sect : ten.getSects().getAll(null)) {
            execute(sect);
        }
    }

    private void execute(Section sect) throws JspException, IOException {
        Object baseURL = ((HttpServletRequest) ((PageContext) getJspContext()).getRequest()).getAttribute(SecurityRepo.BASE_URL);
        if (null != baseURL) {
            getJspContext().setAttribute("_section_url", getUrl(baseURL.toString(), sect.getName(), page));
            getJspContext().setAttribute("_section_name", sect.getName());
            getJspContext().setAttribute("_section_uuid", sect.getUuid());
            getJspBody().invoke(null);
        }
    }

    public static String getUrl(String baseURL, String category, Integer page) {
        StringBuilder url = new StringBuilder(70).append(baseURL).append("index");
        if (null != category && !category.isEmpty()) {
            try {
                String title = URLEncoder.encode(category, "UTF-8");
                url.append('/').append(title);
            } catch (UnsupportedEncodingException ex) {
                throw new JVMNotSupportedError(ex);
            }
        }
        if (null != page && 1 != page) {
            url.append('/').append(page);
        }
        return url.append(".html").toString();
    }

    /**
     * @param category the category to set
     */
    public void setCategory(Section category) {
        this.category = category;
    }

    /**
     * @param page the page to set
     */
    public void setPage(Integer page) {
        this.page = page;
    }
}
