package libWebsiteTools.rss;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.chrono.IsoChronology;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.time.format.SignStyle;
import java.time.format.TextStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.annotation.PreDestroy;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import libWebsiteTools.Repository;

/**
 * The feed bucket, the source from which all RSS feeds come. Does what the
 * label reads: stores your feeds when you're not using them.
 *
 * @author alpha
 */
public class FeedBucket implements Repository<Feed> {

    public static final String TIME_FORMAT = "EEE, dd MMM yyyy HH:mm:ss z";
    private static final Logger LOG = Logger.getLogger(FeedBucket.class.getName());
    private final Map<String, Feed> feeds = Collections.synchronizedMap(new HashMap<>());
    public final static DateTimeFormatter RFC1123;

    static {
        Map<Long, String> dow = Map.ofEntries(Map.entry(1L, "Mon"),
                Map.entry(2L, "Tue"), Map.entry(3L, "Wed"), Map.entry(4L, "Thu"),
                Map.entry(5L, "Fri"), Map.entry(6L, "Sat"), Map.entry(7L, "Sun"));
        Map<Long, String> moy = Map.ofEntries(
                Map.entry(1L, "Jan"), Map.entry(2L, "Feb"), Map.entry(3L, "Mar"),
                Map.entry(4L, "Apr"), Map.entry(5L, "May"), Map.entry(6L, "Jun"),
                Map.entry(7L, "Jul"), Map.entry(8L, "Aug"), Map.entry(9L, "Sep"),
                Map.entry(10L, "Oct"), Map.entry(11L, "Nov"), Map.entry(12L, "Dec"));
        RFC1123 = new DateTimeFormatterBuilder().parseCaseInsensitive().parseLenient()
                .optionalStart().appendText(ChronoField.DAY_OF_WEEK, dow).appendLiteral(", ").optionalEnd()
                .appendValue(ChronoField.DAY_OF_MONTH, 1, 2, SignStyle.NOT_NEGATIVE).appendLiteral(' ')
                .appendText(ChronoField.MONTH_OF_YEAR, moy).appendLiteral(' ')
                .appendValue(ChronoField.YEAR, 4).appendLiteral(' ')
                .appendValue(ChronoField.HOUR_OF_DAY, 2).appendLiteral(':')
                .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
                .optionalStart().appendLiteral(':').appendValue(ChronoField.SECOND_OF_MINUTE, 2).optionalEnd()
                .appendLiteral(' ')
                .optionalStart().appendZoneText(TextStyle.SHORT).optionalEnd()
                .optionalStart().appendOffset("+HHMM", "GMT")
                .toFormatter().withResolverStyle(ResolverStyle.SMART).withChronology(IsoChronology.INSTANCE);
    }

    public static OffsetDateTime parseTimeFormat(DateTimeFormatter formatter, String dateTimeStr) {
        TemporalAccessor t = formatter.parse(dateTimeStr);
        return Instant.from(t).atZone(ZoneId.from(t)).toOffsetDateTime();
    }

    /**
     * returns an XML factory good to use.
     *
     * @param indent
     * @return
     * @throws RuntimeException if the transformer cannot be created
     */
    public static Transformer getTransformer(boolean indent) {
        TransformerFactory xFormFact = TransformerFactory.newInstance();
        xFormFact.setAttribute("indent-number", 4);
        try {
            Transformer trans = xFormFact.newTransformer();
            if (indent) {
                trans.setOutputProperty(OutputKeys.INDENT, "yes");
            }
            return trans;
        } catch (TransformerConfigurationException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * uses @Feed to extract the desired URL for the feed, and uses it to call
     * addFeed(String, iFeed)
     *
     * @param entities feed objects to add
     * @see libRssServlet.Feed
     */
    @Override
    public List<Feed> upsert(Collection<Feed> entities) {
        for (Feed feed : entities) {
            if (feeds.containsKey(feed.getName())) {
                continue;
            }
            synchronized (feed) {
                feed = feed.preAdd();
                if (null == feed) {
                    throw new NullPointerException("A feed can't be added, nor served.");
                }
                feeds.put(feed.getName(), feed);
                feed.postAdd();
            }
            LOG.log(Level.FINEST, "Feed added, name: {0} class: {1}", new Object[]{feed.getName(), feed.getClass().getName()});
        }
        return new ArrayList<>(entities);
    }

    /**
     * returns a feed object by name
     *
     * @param name name
     * @return iFeed
     */
    @Override
    public Feed get(Object name) {
        if (feeds.containsKey(name.toString())) {
            return feeds.get(name.toString());
        }
        for (Feed feed : feeds.values()) {
            if (feed instanceof DynamicFeed && ((DynamicFeed) feed).willHandle(name.toString())) {
                return feed;
            }
        }
        return null;
    }

    @Override
    public List<Feed> search(Object term, Integer limit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Feed> getAll(Integer limit) {
        return new ArrayList<>(feeds.values());
    }

    @Override
    public void processArchive(Consumer<Feed> operation, Boolean transaction) {
        feeds.values().stream().forEachOrdered(operation);
    }

    @Override
    public FeedBucket evict() {
        return this;
    }

    /**
     *
     * @param term ignored
     * @return
     */
    @Override
    public Long count(Object term) {
        return Integer.valueOf(feeds.size()).longValue();
    }

    /**
     * graciously removes a feed by name
     *
     * @param name name of feed to remove
     */
    @Override
    public Feed delete(Object name) {
        if (!feeds.containsKey(name.toString())) {
            throw new IllegalStateException(String.format("Feed doesn't exist, name: %1s", name));
        }
        Feed feed = get(name);
        synchronized (feed) {
            feed.preRemove();
            feeds.remove(name.toString()).postRemove();
        }
        LOG.log(Level.FINE, "Feed removed: {0}", name);
        return feed;
    }

    @PreDestroy
    @SuppressWarnings("unused")
    private void destroy() {
        for (String el : new ArrayList<>(feeds.keySet())) {
            try {
                delete(el);
            } catch (Exception x) {
            }
        }
        LOG.log(Level.FINE, "FeedBucket destroyed");
    }
}
