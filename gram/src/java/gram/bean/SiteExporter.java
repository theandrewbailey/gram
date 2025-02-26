package gram.bean;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import libWebsiteTools.security.SecurityRepo;
import libWebsiteTools.JVMNotSupportedError;
import libWebsiteTools.file.Fileupload;
import libWebsiteTools.rss.FeedBucket;
import org.w3c.dom.Document;
import gram.UtilStatic;
import gram.rss.ArticleRss;
import gram.rss.CommentRss;
import libWebsiteTools.rss.Feed;

/**
 * so I can sleep at night, knowing my stuff is being backed up
 *
 * @author alpha
 */
public class SiteExporter implements Runnable {

    public final static String MIMES_TXT = "mimes.txt";
    public final static String FILE_DIR = "files";
    public final static Integer PROCESSING_CHUNK_SIZE = 16;
    private final static String MASTER_DIR = "site_backup";
    private final static Logger LOG = Logger.getLogger(SiteExporter.class.getName());
    private final GramTenant ten;

    public SiteExporter(GramTenant ten) {
        this.ten = ten;
    }

    public static enum BackupTypes {
        ARTICLES, COMMENTS, LOCALIZATIONS, FILES;
    }

    @Override
    public void run() {
        cleanupZips();
        backup();
        backupToZip();
    }

    public void cleanupZips() {
        LOG.log(Level.FINE, "Cleaning up zip backups");
        String master = ten.getImeadValue(MASTER_DIR);
        final String zipStem = getArchiveStem();
        File zipDir = new File(master);
        List<File> siteZips = Arrays.asList(zipDir.listFiles((File file)
                -> file.getName().startsWith(zipStem) && file.getName().endsWith(".zip")));
        Pattern datePattern = Pattern.compile(".*?(\\d{8})\\.zip$");
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        // set files last modified dates by looking at filename timestamp
        for (File z : siteZips) {
            try {
                GregorianCalendar zipDay = new GregorianCalendar();
                Matcher dateMatcher = datePattern.matcher(z.getName());
                dateMatcher.find();
                zipDay.setTime(dateFormat.parse(dateMatcher.group(1)));
                GregorianCalendar actual = new GregorianCalendar();
                actual.setTime(new java.util.Date(z.lastModified()));
                if (zipDay.get(Calendar.YEAR) != actual.get(Calendar.YEAR)
                        || zipDay.get(Calendar.MONTH) != actual.get(Calendar.MONTH)
                        || zipDay.get(Calendar.DAY_OF_MONTH) != actual.get(Calendar.DAY_OF_MONTH)) {
                    z.setLastModified(zipDay.getTimeInMillis());
                }
            } catch (ParseException ex) {
            }
        }
        // sort by last modified date
        Collections.sort(siteZips, (File f, File g)
                -> Long.compare(f.lastModified(), g.lastModified()));
        // keep 1 backup per month for 2 years
        GregorianCalendar monthlies = new GregorianCalendar();
        monthlies.add(Calendar.YEAR, -2);
        monthlies.set(Calendar.DAY_OF_MONTH, 1);
        monthlies.set(Calendar.HOUR_OF_DAY, 0);
        monthlies.set(Calendar.MINUTE, 0);
        monthlies.set(Calendar.SECOND, 0);
        monthlies.set(Calendar.MILLISECOND, 0);
        // keep daily backups for 1 month
        GregorianCalendar dailies = new GregorianCalendar();
        dailies.add(Calendar.MONTH, -1);
        dailies.set(Calendar.HOUR_OF_DAY, 0);
        dailies.set(Calendar.MINUTE, 0);
        dailies.set(Calendar.SECOND, 0);
        dailies.set(Calendar.MILLISECOND, 0);
        for (File f : siteZips) {
            if (f.lastModified() >= dailies.getTimeInMillis()) {
                // this is a daily backup, watch for next day
                dailies.add(Calendar.DAY_OF_MONTH, 1);
            } else if (f.lastModified() >= monthlies.getTimeInMillis()) {
                // this is a monthly backup, watch for next month
                monthlies.add(Calendar.MONTH, 1);
            } else {
                // not a daily or monthly, so delete
                LOG.log(Level.FINE, "Deleting backup {0}", f.getName());
                f.delete();
            }
        }
    }

    /**
     * dumps articles, comments, and uploaded files to a directory (specified by
     * site_backup key in ten.getImead().keyValue)
     */
    public void backup() {
        ten.getImead().evict();
        ten.getArts().evict();
        ten.getFile().evict();
        if (null == ten.getImeadValue(MASTER_DIR)) {
            throw new IllegalArgumentException(MASTER_DIR + " not configured.");
        }
        final String master = ten.getImeadValue(MASTER_DIR) + getArchiveStem() + File.separator;
        String content = master + FILE_DIR + File.separator;
        File contentDir = new File(content);
        if (!contentDir.exists()) {
            contentDir.mkdirs();
        }
        final OffsetDateTime localNow = OffsetDateTime.now();
        final Queue<Future> backupTasks = new ConcurrentLinkedQueue<>();
        backupTasks.add(ten.getExec().submit(() -> {
            ten.getFile().processArchive((f) -> {
                if (shouldFileBeBackedUp(f)) {
                    String fn = content + f.getFilename();
                    try {
                        LOG.log(Level.FINE, "Writing file {0}", f.getFilename());
                        writeFile(fn, f.getAtime(), f.getFiledata());
                    } catch (IOException ex) {
                        LOG.log(Level.SEVERE, "Error writing " + f.getFilename(), ex);
                        ten.getError().logException(null, "Backup failure", ex.getMessage() + SecurityRepo.NEWLINE + "while backing up " + fn, null);
                    }
                }
            }, false);
        }));
        backupTasks.add(ten.getExec().submit(() -> {
            try {
                LOG.log(Level.FINE, "Writing Articles.rss");
                writeFile(master + File.separator + ArticleRss.NAME, localNow, xmlToBytes(new ArticleRss().createFeed(ten, null, null)));
            } catch (IOException ex) {
                LOG.log(Level.SEVERE, "Error writing Articles.rss to file: " + master + File.separator + ArticleRss.NAME, ex);
                ten.getError().logException(null, "Backup failure", "Error writing Articles.rss to file: " + master + File.separator + ArticleRss.NAME, ex);
            }
        }));
        backupTasks.add(ten.getExec().submit(() -> {
            try {
                LOG.log(Level.FINE, "Writing Comments.rss");
                writeFile(master + File.separator + CommentRss.NAME, localNow, xmlToBytes(Feed.refreshFeed(Arrays.asList(new CommentRss().createChannel(ten, ten.getComms().getAll(null))))));
            } catch (IOException ex) {
                LOG.log(Level.SEVERE, "Error writing Comments.rss to file: " + master + File.separator + CommentRss.NAME, ex);
                ten.getError().logException(null, "Backup failure", "Error writing Comments.rss to file: " + master + File.separator + CommentRss.NAME, ex);
            }
        }));
        backupTasks.add(ten.getExec().submit(() -> {
            StringBuilder mimes = new StringBuilder(1000000);
            ten.getFile().processArchive((f) -> {
                if (shouldFileBeBackedUp(f)) {
                    synchronized (mimes) {
                        mimes.append(f.getFilename()).append(": ").append(f.getMimetype()).append('\n');
                    }
                }
            }, false);
            try {
                LOG.log(Level.FINE, "Writing mimes.txt");
                writeFile(master + MIMES_TXT, localNow, mimes.toString().getBytes("UTF-8"));
            } catch (IOException ex) {
                LOG.log(Level.SEVERE, "Error writing mimes.txt: " + master + MIMES_TXT, ex);
                ten.getError().logException(null, "Backup failure", "Error writing mimes.txt: " + master + MIMES_TXT, ex);
            }
        }));
        backupTasks.add(ten.getExec().submit(() -> {
            Map<Locale, Properties> propMap = ten.getImead().getProperties();
            Map<String, String> localeFiles = new HashMap<>(propMap.size() * 2);
            for (Locale l : propMap.keySet()) {
                try {
                    StringWriter propertiesContent = new StringWriter(10000);
                    propMap.get(l).store(propertiesContent, null);
                    String name = l != Locale.ROOT ? "IMEAD-" + l.toLanguageTag() + ".properties" : "IMEAD.properties";
                    writeFile(master + name, localNow, propertiesContent.toString().getBytes("UTF-8"));
                } catch (IOException ex) {
                    ten.getError().logException(null, "Can't backup properties", "Can't backup properties for locale " + l.toLanguageTag(), ex);
                }
            }
            return localeFiles;
        }));
        UtilStatic.finish(backupTasks);
    }

    /**
     * stuffs everything to a zip file in the backup directory (specified by
     * site_backup key in ten.getImead().keyValue) can be used for the import
     * functionality
     *
     * @see BackupDaemon.createZip(OutputStream wrapped)
     */
    public void backupToZip() {
        LOG.log(Level.FINE, "Backup to zip procedure initiating");
        String master = ten.getImeadValue(MASTER_DIR);
        String zipName = getArchiveName("zip");
        String fn = master + zipName;
        try (FileOutputStream out = new FileOutputStream(fn)) {
            writeZip(out, Arrays.asList(BackupTypes.values()));
            LOG.log(Level.FINE, "Backup to zip procedure finished");
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Error writing zip file: " + fn, ex);
            ten.getError().logException(null, "Backup failure", ex.getMessage() + SecurityRepo.NEWLINE + "while backing up " + fn, null);
        }
    }

    /**
     * stuffs types of things into a zip and dumps it on the given OutputStream.
     * can be used to save to file or put on a response
     *
     * @param wrapped put zip on this
     * @param types put zip on this
     */
    public void writeZip(OutputStream wrapped, List<BackupTypes> types) {
        final Queue<Future> backupTasks = new ConcurrentLinkedQueue<>();
        OffsetDateTime localNow = OffsetDateTime.now();
        try (ZipOutputStream zip = new ZipOutputStream(wrapped)) {
            if (types.contains(BackupTypes.ARTICLES)) {
                backupTasks.add(ten.getExec().submit(() -> {
                    byte[] xmlBytes = xmlToBytes(new ArticleRss().createFeed(ten, null, null));
                    try {
                        addFileToZip(zip, ArticleRss.NAME, "text/xml", localNow, xmlBytes);
                    } catch (IOException ix) {
                        throw new RuntimeException(ix);
                    }
                }));
            }
            if (types.contains(BackupTypes.COMMENTS)) {
                backupTasks.add(ten.getExec().submit(() -> {
                    byte[] xmlBytes = xmlToBytes(Feed.refreshFeed(Arrays.asList(new CommentRss().createChannel(ten, ten.getComms().getAll(null)))));
                    try {
                        addFileToZip(zip, CommentRss.NAME, "text/xml", localNow, xmlBytes);
                    } catch (IOException ix) {
                        throw new RuntimeException(ix);
                    }
                }));
            }
            if (types.contains(BackupTypes.LOCALIZATIONS)) {
                backupTasks.add(ten.getExec().submit(() -> {
                    Map<Locale, Properties> propMap = ten.getImead().getProperties();
                    Map<String, String> localeFiles = new HashMap<>(propMap.size() * 2);
                    for (Locale l : propMap.keySet()) {
                        try {
                            StringWriter propertiesContent = new StringWriter(10000);
                            propMap.get(l).store(propertiesContent, null);
                            String localeString = l != Locale.ROOT ? l.toLanguageTag() : "";
                            localeFiles.put(localeString, propertiesContent.toString());
                        } catch (IOException ex) {
                            ten.getError().logException(null, "Can't backup properties", "Can't backup properties for locale " + l.toLanguageTag(), ex);
                        }
                    }
                    for (Map.Entry<String, String> locale : localeFiles.entrySet()) {
                        String name = 0 == locale.getKey().length() ? "IMEAD.properties" : "IMEAD-" + locale.getKey() + ".properties";
                        addFileToZip(zip, name, "text/plain", localNow, locale.getValue().getBytes());
                    }
                    return localeFiles;
                }));
            }
            if (types.contains(BackupTypes.FILES)) {
                backupTasks.add(ten.getExec().submit(() -> {
                    StringBuilder mimes = new StringBuilder(1000000);
                    ten.getFile().processArchive((f) -> {
                        if (shouldFileBeBackedUp(f)) {
                            synchronized (mimes) {
                                mimes.append(f.getFilename()).append(": ").append(f.getMimetype()).append('\n');
                            }
                        }
                    }, false);
                    try {
                        addFileToZip(zip, MIMES_TXT, "text/plain", localNow, mimes.toString().getBytes("UTF-8"));
                    } catch (IOException ix) {
                        throw new RuntimeException(ix);
                    }
                }));
                backupTasks.add(ten.getExec().submit(() -> {
                    ten.getFile().processArchive((f) -> {
                        if (shouldFileBeBackedUp(f)) {
                            try {
                                addFileToZip(zip, FILE_DIR + "/" + f.getFilename(), f.getMimetype(), f.getAtime(), f.getFiledata());
                            } catch (IOException ix) {
                                throw new RuntimeException(ix);
                            }
                        }
                    }, false);
                }));
            }
            UtilStatic.finish(backupTasks);
        } catch (IOException | RuntimeException rx) {
            LOG.log(Level.SEVERE, null, rx);
        }
    }

    /**
     *
     * @param extension like "zip"
     * @return site title (a-zA-Z_0-9 only) + today's date (year + month + day)
     * + .extension
     */
    public String getArchiveName(String extension) {
        String stem = getArchiveStem() + "_";
        return stem + OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "." + extension;
    }

    /**
     * @return site title (a-zA-Z_0-9 only), plus "_" character is a digit
     */
    public String getArchiveStem() {
        Matcher originMatcher = SecurityRepo.ORIGIN_PATTERN.matcher(ten.getImead().getValue(SecurityRepo.BASE_URL));
        originMatcher.find();
        String stem = originMatcher.group(3).replace(".", "-");
        return stem;
    }

    /**
     * adds a file to the given ZIP stream
     *
     * @param zip zip stream
     * @param name of file
     * @param mime file type
     * @param time modified time (from Date.getTime), defaults to -1
     * @param content file contents
     * @return the created entry
     * @throws IOException something went wrong
     */
    public static ZipEntry addFileToZip(ZipOutputStream zip, String name, String mime, OffsetDateTime time, byte[] content) throws IOException {
        ZipEntry out = new ZipEntry(name);
        out.setMethod(ZipEntry.DEFLATED);
        out.setTime(time.toInstant().toEpochMilli());
        out.setComment(mime);
        synchronized (zip) {
            zip.putNextEntry(out);
            zip.write(content);
            zip.closeEntry();
            LOG.log(Level.FINEST, "File added to zip: {0}", name);
        }
        return out;
    }

    /**
     * makes a really big "string" from the given XML object
     *
     * @param xml
     * @return big string
     */
    private byte[] xmlToBytes(Document xml) {
        DOMSource DOMsrc = new DOMSource(xml);
        StringWriter sw = new StringWriter();
        StreamResult str = new StreamResult(sw);
        Transformer trans = FeedBucket.getTransformer(false);
        try {
            trans.transform(DOMsrc, str);
            return sw.toString().getBytes("UTF-8");
        } catch (TransformerException ex) {
            throw new RuntimeException(ex);
        } catch (UnsupportedEncodingException ex) {
            throw new JVMNotSupportedError(ex);
        }
    }

    /**
     * writes a file
     *
     * @param filename what to name it
     * @param content what it contains
     * @throws IOException something went boom
     */
    private void writeFile(String filename, OffsetDateTime time, byte[] content) throws IOException {
        File f = new File(filename);
        if (!f.exists()) {
            List<String> parts = Arrays.asList(filename.split("/"));
            String dir = "";
            for (String p : parts) {
                if (!p.equals(parts.get(parts.size() - 1))) {
                    dir += File.separator + p;
                }
            }
            File d = new File(dir);
            if (!d.exists()) {
                d.mkdirs();
            }
            f.createNewFile();
            try (FileOutputStream tempStr = new FileOutputStream(f, false)) {
                tempStr.write(content);
            }
        } else if (!Instant.ofEpochMilli(f.lastModified()).equals(time.toInstant())) {
            try (FileOutputStream tempStr = new FileOutputStream(f, false)) {
                tempStr.write(content);
            }
        }
    }

    /**
     * filter out any temporary cached files. currently none.
     *
     * @param f
     * @return true if this should be included in backups
     */
    private static boolean shouldFileBeBackedUp(Fileupload f) {
        return null != f;
    }
}
