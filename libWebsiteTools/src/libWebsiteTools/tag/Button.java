package libWebsiteTools.tag;

import java.io.StringWriter;

/**
 *
 * @author alpha
 */
public class Button extends AbstractInput {

    private String type;
    private String action;
    private String method;
    private String target;

    @Override
    public StringBuilder createTag() {
        StringBuilder out = new StringBuilder(400).append("<button type=\"").append(getType()).append("\" name=\"").append(getName());
        if (null != getId()) {
            out.append("\" id=\"").append(getId());
        }
        if (null != getAccesskey()) {
            out.append("\" accesskey=\"").append(getAccesskey());
        }
        if (getAutofocus()) {
            out.append("\" autofocus=\"autofocus");
        }
        if (getDisabled()) {
            out.append("\" disabled=\"disabled");
        }
        if (null != getStyleClass()) {
            out.append("\" class=\"").append(getStyleClass());
        }
        if (null != getTabindex()) {
            out.append("\" tabindex=\"").append(getTabindex().toString());
        }
        if (null != getTitle()) {
            out.append("\" title=\"").append(getTitle());
        }
        if (null != getTarget()) {
            out.append("\" formtarget=\"").append(getTarget());
        }
        if (null != getValue()) {
            out.append("\" value=\"").append(getValue());
        }
        if (null != getAction()) {
            out.append("\" formaction=\"").append(getAction());
        }
        if (null != getMethod()) {
            out.append("\" formmethod=\"").append(getMethod());
        }
        out.append("\">");
        StringWriter body = new StringWriter(100);
        try {
            getJspBody().invoke(body);
        } catch (Exception ex) {
        }
        out.append(body.toString());
        return out.append("</button>");
    }

    @Override
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }
}
