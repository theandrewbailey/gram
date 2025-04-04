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
import gram.bean.database.Article;

public class ArticleUrl extends SimpleTagSupport {

    private Article article;
    private boolean link = true;
    private String anchor;
    private String cssClass;
    private String target;
    private String text;
    private String id;

    @Override
    public void doTag() throws JspException, IOException {
        StringBuilder b = new StringBuilder(300);
        if (link) {
            b.append("<a href=\"");
        }
        b.append(getUrl(((HttpServletRequest) ((PageContext) getJspContext()).getRequest()).getAttribute(SecurityRepository.BASE_URL).toString(), article, anchor));
        if (link && id != null) {
            b.append("\" id=\"").append(id);
        } else if (link) {
            b.append("\" id=\"").append(article.getUuid());
        }
        if (link && cssClass != null) {
            b.append("\" class=\"").append(cssClass);
        }
        if (link && target != null) {
            b.append("\" target=\"").append(target);
        }
        if (link) {
            b.append("\">").append(text == null ? article.getArticletitle() : text).append("</a>");
        }
        getJspContext().getOut().print(b.toString());
    }

    public static String getUrl(String baseURL, Article article, String anchor) {
        StringBuilder url = new StringBuilder(baseURL).append("article/").append(article.getArticleid()).append('/').append(getUrlArticleTitle(article)).append(".html");
        if (null != anchor) {
            url.append("#").append(anchor);
        }
        return url.toString();
    }

    private static String getUrlArticleTitle(Article article) {
        try {
            String title = null != article.getArticletitle() ? URLEncoder.encode(article.getArticletitle(), "UTF-8") : "";
            title = title.replaceAll("%[0-9A-F]{2}", "").replace(":", "").replace("+", "-").replace("--", "-");
            return title;
        } catch (UnsupportedEncodingException ex) {
            throw new JVMNotSupportedError(ex);
        }
    }

    public void setArticle(Article article) {
        this.article = article;
    }

    public void setAnchor(String anchor) {
        this.anchor = anchor;
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
}
