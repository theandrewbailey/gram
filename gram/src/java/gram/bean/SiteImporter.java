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
import java.util.concurrent.ExecutionException;
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
import libWebsiteTools.security.SecurityRepo;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 *
 * @author alpha
 */
public class SiteImporter {

    private final static Pattern IMEAD_BACKUP_FILE = Pattern.compile("IMEAD(?:-(.+?))?\\.properties");
    private final static Logger LOG = Logger.getLogger(SiteImporter.class.getName());
    private final Map<String, String> mimes = new ConcurrentHashMap<>(1000);
    private final GramTenant ten;

    public SiteImporter(GramTenant ten) {
        this.ten = ten;
    }

    /**
     * takes a zip file backup of the site and restores everything
     *
     * @param zip
     * @throws java.io.IOException
     */
    public void restoreFromZip(ZipInputStream zip) throws IOException {
        ten.reset();
        final Queue<Future> restoreTasks = new ConcurrentLinkedQueue<>();
        Future<List<Article>> masterArticleTask = null;
        Future<List<Comment>> masterCommentTask = null;
        Queue<Fileupload> fileUploads = new ConcurrentLinkedQueue<>();
        for (ZipEntry zipFile = zip.getNextEntry(); zipFile != null; zipFile = zip.getNextEntry()) {
            if (zipFile.isDirectory()) {
                continue;
            }
            LOG.log(Level.FINER, "Processing file: {0}", zipFile.getName());
            switch (zipFile.getName()) {
                case ArticleRss.NAME:
                    final InputStream articleStream = new ByteArrayInputStream(FileUtil.getByteArray(zip));
                    masterArticleTask = ten.getExec().submit(new ArticleRssImporter(ten, articleStream));
                    break;
                case CommentRss.NAME:
                    final InputStream commentStream = new ByteArrayInputStream(FileUtil.getByteArray(zip));
                    masterCommentTask = ten.getExec().submit(new CommentRssImporter(commentStream));
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
                                if (fileUploads.size() > Runtime.getRuntime().availableProcessors() * 8) {
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
        UtilStatic.finish(restoreTasks).clear();
        ten.getFile().upsert(fileUploads);
        fileUploads.clear();
        restoreTasks.add(ten.getExec().submit(() -> {
            ten.getFile().evict().processArchive((file) -> {
                if (mimes.containsKey(file.getFilename()) && !file.getMimetype().equals(mimes.get(file.getFilename()))) {
                    file.setMimetype(mimes.get(file.getFilename()));
                }
                String fileUrl = BaseFileServlet.getImmutableURL(ten.getImeadValue(SecurityRepo.BASE_URL), file);
                if (!fileUrl.equals(file.getUrl())) {
                    file.setUrl(fileUrl);
                }
            }, true);
        }, true));
        try {
            if (null != masterArticleTask) {
                masterArticleTask.get();
                if (null != masterCommentTask) {
                    ten.getComms().upsert(masterCommentTask.get());
                }
            }
        } catch (InterruptedException | ExecutionException ex) {
            throw new RuntimeException(ex);
        }
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

        private final GramTenant ten;
        private final Document rssFeed;

        public ArticleRssImporter(GramTenant ten, InputStream rssStream) {
            this.ten = ten;
            try {
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                dbf.setIgnoringElementContentWhitespace(true);
                rssFeed = dbf.newDocumentBuilder().parse(rssStream);
            } catch (Exception ex) {
                throw new RuntimeException("Can't parse article RSS stream.", ex);
            }
        }

        @Override
        public List<Article> call() throws Exception {
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
                // conversion
                if (null == art.getPostedhtml() || null == art.getPostedmarkdown()) {
                    ArticleProcessor.convert(art);
                }
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
            for (Article art : articles) {
                art.setArticleid(null);
            }
            ten.getArts().delete(null);
            ten.getArts().upsert(articles);
            return articles;
        }
    }

    private static class CommentRssImporter implements Callable<List<Comment>> {

        private final Document rssFeed;

        public CommentRssImporter(InputStream rssStream) {
            try {
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                dbf.setIgnoringElementContentWhitespace(true);
                rssFeed = dbf.newDocumentBuilder().parse(rssStream);
            } catch (Exception ex) {
                throw new RuntimeException("Can't parse comment RSS stream.", ex);
            }
        }

        @Override
        public List<Comment> call() throws Exception {
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
            return comments;
        }
    }
}
