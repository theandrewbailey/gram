package gram.bean;

import gram.ArticleProcessor;
import gram.UtilStatic;
import gram.bean.database.Article;
import gram.bean.database.Comment;
import gram.bean.database.Section;
import gram.rss.ArticleRss;
import gram.rss.CommentRss;
import gram.rss.GramRssItem;
import gram.servlet.ArticleServlet;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.parsers.DocumentBuilderFactory;
import libWebsiteTools.JVMNotSupportedError;
import libWebsiteTools.XmlNodeSearcher;
import libWebsiteTools.file.BaseFileServlet;
import libWebsiteTools.file.FileCompressorJob;
import libWebsiteTools.file.FileRepository;
import libWebsiteTools.file.FileUtil;
import libWebsiteTools.file.Fileupload;
import libWebsiteTools.imead.Localization;
import libWebsiteTools.rss.FeedBucket;
import libWebsiteTools.rss.RssChannel;
import libWebsiteTools.security.HashUtil;
import libWebsiteTools.security.SecurityRepository;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 *
 * @author alpha
 */
public class SiteImporter implements Runnable {

    private final static Pattern IMEAD_BACKUP_FILE = Pattern.compile("IMEAD(?:-(.+?))?\\.properties");
    private final static Logger LOG = Logger.getLogger(SiteImporter.class.getName());
    private final Map<String, String> mimes = new ConcurrentHashMap<>(1000);
    private final GramTenant ten;
    private final ArticleRssImporter articleImporter = new ArticleRssImporter();
    private final CommentRssImporter commentImporter = new CommentRssImporter();
    private final ZipInputStream zip;

    /**
     * takes a zip file backup of the site and restores everything
     *
     * @param ten
     * @param zip
     */
    public SiteImporter(GramTenant ten, ZipInputStream zip) {
        this.ten = ten;
        this.zip = zip;
    }

    @Override
    public void run() {
        ten.reset();
        final Queue<Fileupload> fileUploads = new ConcurrentLinkedQueue<>();
        final Queue<Future> restoreTasks = new ConcurrentLinkedQueue<>();
        try {
            for (ZipEntry zipFile = zip.getNextEntry(); zipFile != null; zipFile = zip.getNextEntry()) {
                if (zipFile.isDirectory()) {
                    continue;
                }
                LOG.log(Level.FINER, "Processing file: {0}", zipFile.getName());
                switch (zipFile.getName()) {
                    case ArticleRss.NAME:
                        articleImporter.setTask(ten.getExec().submit(articleImporter.with(
                                new ByteArrayInputStream(FileUtil.getByteArray(zip)))));
                        break;
                    case CommentRss.NAME:
                        commentImporter.setTask(ten.getExec().submit(commentImporter.with(
                                new ByteArrayInputStream(FileUtil.getByteArray(zip)))));
                        break;
                    case SiteExporter.MIMES_TXT:
                        String mimeString = new String(FileUtil.getByteArray(zip));
                        for (String mimeEntry : mimeString.split("\n")) {
                            try {
                                String[] parts = mimeEntry.split(": ");
                                mimes.put(parts[0], parts[1]);
                            } catch (ArrayIndexOutOfBoundsException a) {
                            }
                        }
                        break;
                    default:
                        Matcher imeadBackup = IMEAD_BACKUP_FILE.matcher(zipFile.getName());
                        if (imeadBackup.find()) {
                            String properties = new String(FileUtil.getByteArray(zip));
                            restoreTasks.add(ten.getExec().submit(() -> {
                                String locale = null != imeadBackup.group(1) ? imeadBackup.group(1) : "";
                                Properties props = new Properties();
                                try {
                                    props.load(new StringReader(properties));
                                    ArrayList<Localization> localizations = new ArrayList<>(props.size() * 2);
                                    for (String key : props.stringPropertyNames()) {
                                        localizations.add(new Localization(locale, key, props.getProperty(key)));
                                    }
                                    ten.getImead().upsert(localizations);
                                } catch (IOException ex) {
                                    ten.getError().logException(null, "Can't restore properties", "Can't restore properties for locale " + locale, ex);
                                }
                                return props;
                            }));
                        } else if (zipFile.getName().startsWith(SiteExporter.FILE_DIR)) {
                            Fileupload incomingFile = new Fileupload();
                            incomingFile.setAtime(Instant.ofEpochMilli(zipFile.getTime()).atOffset(ZoneOffset.UTC));
                            incomingFile.setFilename(zipFile.getName().replace(SiteExporter.FILE_DIR + "/", ""));
                            incomingFile.setMimetype(zipFile.getComment());
                            incomingFile.setFiledata(FileUtil.getByteArray(zip));
                            restoreTasks.add(ten.getExec().submit(() -> {
                                incomingFile.setEtag(HashUtil.getSHA256Hash(incomingFile.getFiledata()));
                                if (null == incomingFile.getMimetype()) {
                                    incomingFile.setMimetype(FileRepository.DEFAULT_MIME_TYPE);
                                }
                                Fileupload existingFile = ten.getFile().get(incomingFile.getFilename());
                                if (null == existingFile) {
                                    fileUploads.add(incomingFile);
                                } else if (!incomingFile.getEtag().equals(existingFile.getEtag())) {
                                    LOG.log(Level.FINER, "Existing file different, updating {0}", incomingFile.getFilename());
                                    fileUploads.add(incomingFile);
                                }
                                synchronized (fileUploads) {
                                    if (fileUploads.size() > 100) {
                                        ten.getFile().upsert(fileUploads);
                                        fileUploads.clear();
                                    }
                                }
                                return null;
                            }));
                        }
                        break;
                }
            }
            zip.close();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        UtilStatic.finish(restoreTasks).clear();
        restoreTasks.add(ten.getExec().submit(() -> {
            try {
                if (null != articleImporter.getTask()) {
                    List<Article> articles = articleImporter.getTask().get();
                    String baseURL = ten.getImeadValue(SecurityRepository.BASE_URL);
                    final Queue<Future> reprocessTasks = new ConcurrentLinkedQueue<>();
                    for (Article a : articles) {
                        // this import might be from a different environment, so reprocess
                        if (a.getPostedhtml().contains("<img ") && !a.getPostedhtml().contains(baseURL)) {
                            if (null != a.getPostedmarkdown()) {
                                a.setPostedhtml(null);
                            }
                            a.setSummary(null);
                        }
                        if (null == a.getPostedhtml() || null == a.getPostedmarkdown() || null == a.getSummary()) {
                            reprocessTasks.add(ten.getExec().submit(new ArticleProcessor(ten, a)));
                        }
                    }
                    UtilStatic.finish(reprocessTasks).clear();
                    ten.getArts().delete(null);
                    ten.getArts().upsert(articles);
                    if (null != commentImporter.getTask()) {
                        ten.getComms().upsert(commentImporter.getTask().get());
                    }
                }
            } catch (Exception p) {
                throw new RuntimeException(p);
            }
        }, true));
        restoreTasks.add(ten.getExec().submit(() -> {
            ten.getFile().upsert(fileUploads);
            fileUploads.clear();
            ten.getFile().evict().processArchive((file) -> {
                if (mimes.containsKey(file.getFilename()) && !file.getMimetype().equals(mimes.get(file.getFilename()))) {
                    file.setMimetype(mimes.get(file.getFilename()));
                }
                String fileUrl = BaseFileServlet.getImmutableURL("", file);
                if (!fileUrl.equals(file.getUrl())) {
                    file.setUrl(fileUrl);
                }
            }, true);
        }, true));
        UtilStatic.finish(restoreTasks).clear();
        ten.reset();
        ten.getExec().submit(() -> {
            ten.getArts().refreshSearch();
            ten.getFile().processArchive((f) -> {
                FileCompressorJob.startAllJobs(ten, f);
            }, false);
        });
    }

    private static class ArticleRssImporter implements Callable<List<Article>> {

        private Document rssFeed;
        private Future<List<Article>> task;

        public synchronized ArticleRssImporter with(InputStream rssStream) {
            if (null == rssStream) {
                throw new IllegalArgumentException("rssStream is null.");
            }
            if (null != rssFeed) {
                throw new IllegalStateException("ArticleRssImporter.with() called too many times.");
            }
            try {
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                dbf.setIgnoringElementContentWhitespace(true);
                rssFeed = dbf.newDocumentBuilder().parse(rssStream);
            } catch (Exception ex) {
                throw new RuntimeException("Can't parse article RSS stream.", ex);
            }
            return this;
        }

        @Override
        public List<Article> call() throws Exception {
            if (null == rssFeed) {
                throw new IllegalArgumentException("rssFeed not set. Call ArticleRssImporter.with()");
            }
            Node channel = rssFeed.getDocumentElement().getFirstChild();
            List<Article> articles;
            try {
                int length = Integer.parseInt(channel.getAttributes().getNamedItem(RssChannel.COUNT_ATTRIBUTE).getNodeValue());
                articles = new ArrayList<>(length * 2);
            } catch (Exception e) {
                articles = new ArrayList<>(100);
            }
            for (Node item : new XmlNodeSearcher(channel, "item")) {
                Article art = new Article(UUID.randomUUID());
                art.setComments(Boolean.FALSE);
                for (Node n : new XmlNodeSearcher(item, "link")) {
                    art.setArticleid(Integer.decode(ArticleServlet.getArticleIdFromURL(n.getTextContent().trim())));
                }
                for (Node n : new XmlNodeSearcher(item, "title")) {
                    art.setArticletitle(n.getTextContent().trim());
                }
                for (Node n : new XmlNodeSearcher(item, "pubDate")) {
                    art.setPosted(FeedBucket.parseTimeFormat(DateTimeFormatter.ISO_OFFSET_DATE_TIME, n.getTextContent().trim()));
                }
                for (Node n : new XmlNodeSearcher(item, "category")) {
                    art.setSectionid(new Section(null, n.getTextContent().trim(), UUID.randomUUID()));
                }
                for (Node n : new XmlNodeSearcher(item, "comments")) {
                    art.setComments(null != n);
                }
                for (Node n : new XmlNodeSearcher(item, "author")) {
                    art.setPostedname(n.getTextContent().trim());
                }
                for (Node n : new XmlNodeSearcher(item, GramRssItem.TAB_METADESC_ELEMENT_NAME)) {
                    try {
                        art.setDescription(URLDecoder.decode(n.getTextContent().trim(), "UTF-8"));
                    } catch (UnsupportedEncodingException enc) {
                        throw new JVMNotSupportedError(enc);
                    }
                }
                for (Node n : new XmlNodeSearcher(item, GramRssItem.MARKDOWN_ELEMENT_NAME)) {
                    art.setPostedmarkdown(n.getTextContent().trim());
                }
                for (Node n : new XmlNodeSearcher(item, "description")) {
                    art.setPostedhtml(n.getTextContent().trim());
                }
                for (Node n : new XmlNodeSearcher(item, "guid")) try {
                    art.setUuid(UUID.fromString(n.getTextContent().trim()));
                } catch (IllegalArgumentException x) {
                }
                for (Node n : new XmlNodeSearcher(item, GramRssItem.SUGGESTION_ELEMENT_NAME)) {
                    art.setSuggestion(n.getTextContent().trim());
                }
                for (Node n : new XmlNodeSearcher(item, GramRssItem.SUMMARY_ELEMENT_NAME)) {
                    art.setSummary(n.getTextContent().trim());
                }
                for (Node n : new XmlNodeSearcher(item, GramRssItem.IMAGEURL_ELEMENT_NAME)) {
                    art.setImageurl(n.getTextContent().trim());
                }
                art.setCommentCollection(null);
                articles.add(art);
            }
            articles.sort((Article a, Article r) -> {
                if (null != a.getArticleid() && null != r.getArticleid()) {
                    return a.getArticleid() - r.getArticleid();
                } else if (null != a.getArticleid()) {
                    return 1;
                } else if (null != r.getArticleid()) {
                    return -1;
                }
                return 0;
            });
            rssFeed = null;
            return articles;
        }

        public Future<List<Article>> getTask() {
            return task;
        }

        public void setTask(Future<List<Article>> task) {
            this.task = task;
        }
    }

    private static class CommentRssImporter implements Callable<List<Comment>> {

        private Document rssFeed;
        private Future<List<Comment>> task;

        public synchronized CommentRssImporter with(InputStream rssStream) {
            if (null == rssStream) {
                throw new IllegalArgumentException("rssStream is null.");
            }
            if (null != rssFeed) {
                throw new IllegalStateException("CommentRssImporter.with() called too many times.");
            }
            try {
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                dbf.setIgnoringElementContentWhitespace(true);
                rssFeed = dbf.newDocumentBuilder().parse(rssStream);
            } catch (Exception ex) {
                throw new RuntimeException("Can't parse comment RSS stream.", ex);
            }
            return this;
        }

        @Override
        public List<Comment> call() throws Exception {
            if (null == rssFeed) {
                throw new IllegalArgumentException("rssFeed not set. Call CommentRssImporter.with()");
            }
            Node channel = rssFeed.getDocumentElement().getFirstChild();
            List<Comment> comments;
            try {
                int length = Integer.parseInt(channel.getAttributes().getNamedItem(RssChannel.COUNT_ATTRIBUTE).getNodeValue());
                comments = new ArrayList<>(length * 2);
            } catch (Exception e) {
                comments = new ArrayList<>(100);
            }
            for (Node item : new XmlNodeSearcher(channel, "item")) {
                Comment comm = new Comment(UUID.randomUUID());
                for (Node n : new XmlNodeSearcher(item, "description")) {
                    comm.setPostedhtml(n.getTextContent().replace("&lt;", "<").replace("&gt;", ">"));
                }
                for (Node n : new XmlNodeSearcher(item, "pubDate")) {
                    comm.setPosted(FeedBucket.parseTimeFormat(DateTimeFormatter.ISO_OFFSET_DATE_TIME, n.getTextContent().trim()));
                }
                for (Node n : new XmlNodeSearcher(item, "author")) {
                    comm.setPostedname(n.getTextContent());
                }
                for (Node n : new XmlNodeSearcher(item, "link")) {
                    comm.setArticleid(new Article(Integer.decode(ArticleServlet.getArticleIdFromURL(n.getTextContent()))));
                    break;
                }
                for (Node n : new XmlNodeSearcher(item, "guid")) try {
                    comm.setUuid(UUID.fromString(n.getTextContent().trim()));
                } catch (IllegalArgumentException x) {
                    comm.setUuid(UUID.randomUUID());
                }
                comments.add(comm);
            }
            for (Comment c : comments) {
                c.setCommentid(null);
            }
            rssFeed = null;
            return comments;
        }

        public Future<List<Comment>> getTask() {
            return task;
        }

        public void setTask(Future<List<Comment>> task) {
            this.task = task;
        }
    }
}
