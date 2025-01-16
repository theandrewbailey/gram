package libWebsiteTools.rss;

import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import org.w3c.dom.Element;

/**
 *
 * @author alpha
 */
public class AtomDate extends AtomCommonAttribs {

    private OffsetDateTime dateTime;

    @Override
    public Element publish(Element xml) {
        Element item = super.publish(xml, "updated");
        item.setTextContent(new SimpleDateFormat(DATE_FORMAT).format(getDateTime()));
        return item;
    }

    /**
     * @return the dateTime
     */
    public OffsetDateTime getDateTime() {
        return dateTime;
    }

    /**
     * @param dateTime the dateTime to set
     */
    public void setDateTime(OffsetDateTime dateTime) {
        this.dateTime = dateTime;
    }

}
