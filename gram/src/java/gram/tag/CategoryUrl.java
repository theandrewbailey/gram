package gram.tag;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.jsp.JspException;
import jakarta.servlet.jsp.PageContext;
import jakarta.servlet.jsp.tagext.SimpleTagSupport;
import libWebsiteTools.security.SecurityRepository;
import libWebsiteTools.JVMNotSupportedError;
import gram.bean.database.Section;

public class CategoryUrl extends SimpleTagSupport {

    private Section category;
    private boolean link = true;
    private String cssClass;
    private String target;
    private String text;
    private String id;
    private Integer page;

    @Override
    public void doTag() throws JspException, IOException {
        StringBuilder b = new StringBuilder(300);
        if (link) {
            b.append("<a href=\"");
        }
        String baseURL = ((HttpServletRequest) ((PageContext) getJspContext()).getRequest()).getAttribute(SecurityRepository.BASE_URL).toString();
        b.append(getUrl(baseURL, null != category.getSectionid() ? category.getName() : null, page));
        if (link && id != null) {
            b.append("\" id=\"").append(id);
        } else if (link) {
            b.append("\" id=\"").append(category.getUuid());
        }
        if (link && cssClass != null) {
            b.append("\" class=\"").append(cssClass);
        }
        if (link && target != null) {
            b.append("\" target=\"").append(target);
        }
        if (link) {
            b.append("\">").append(text == null ? category.getName() : text).append("</a>");
        }
        getJspContext().getOut().print(b.toString());
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
        if (null != page) {
            url.append('/').append(page);
        }
        return url.append(".html").toString();
    }

    public void setCategory(Section category) {
        this.category = category;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setLink(boolean link) {
        this.link = link;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setCssClass(String cssClass) {
        this.cssClass = cssClass;
    }

    public void setPage(Integer page) {
        this.page = page;
    }
}
