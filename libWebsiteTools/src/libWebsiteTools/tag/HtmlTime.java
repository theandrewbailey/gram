package libWebsiteTools.tag;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import jakarta.servlet.jsp.JspException;
import jakarta.servlet.jsp.tagext.SimpleTagSupport;
import java.time.format.DateTimeFormatterBuilder;

/**
 *
 * @author alpha
 */
public class HtmlTime extends SimpleTagSupport {

    public static final String FORMAT_VAR = "$_LIBWEBSITETOOLS_DATETIME_FORMAT";
    public static final String SITE_DATEFORMAT_LONG = "site_dateFormatLong";
    private String pattern;
    private OffsetDateTime datetime;
    private final DateTimeFormatter dateTimeFormat = new DateTimeFormatterBuilder().appendPattern("uuuu-MM-dd'T'HH:mm:ss.SSSXXXXX").toFormatter();
    private final SimpleDateFormat htmlFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    @Override
    public void doTag() throws JspException, IOException {
            ZonedDateTime z = datetime.toZonedDateTime();
            StringBuilder out = new StringBuilder("<time datetime=\"");
            out.append(dateTimeFormat.format(datetime));
            out.append("\" >");
            Object uniFormat = getJspContext().findAttribute(FORMAT_VAR);
            if (pattern != null) {
                htmlFormat.applyPattern(pattern);
                out.append(htmlFormat.format(Date.from(datetime.toInstant())));
            } else if (uniFormat != null) {
                htmlFormat.applyPattern(uniFormat.toString());
                out.append(htmlFormat.format(Date.from(datetime.toInstant())));
            } else {
                out.append(new SimpleDateFormat().format(Date.from(datetime.toInstant())));
            }
            out.append("</time>");
            getJspContext().getOut().print(out.toString());
    }

    /**
     * @return the format
     */
    public String getPattern() {
        return pattern;
    }

    /**
     * @param pattern
     */
    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    /**
     * @return the datetime
     */
    public OffsetDateTime getDatetime() {
        return datetime;
    }

    /**
     * @param datetime the datetime to set
     */
    public void setDatetime(OffsetDateTime datetime) {
        this.datetime = datetime;
    }
}
