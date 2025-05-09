package gram;

import gram.bean.GramTenant;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import libWebsiteTools.JVMNotSupportedError;
import libWebsiteTools.security.SecurityRepository;
import libWebsiteTools.Markdowner;
import libWebsiteTools.file.BaseFileServlet;
import libWebsiteTools.file.Fileupload;
import gram.bean.database.Article;
import gram.tag.ArticleUrl;
import jakarta.persistence.NoResultException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import libWebsiteTools.tag.HtmlScript;
import libWebsiteTools.tag.StyleSheet;

/**
 * instantiate as needed to process articles in threads
 *
 * @author alpha
 */
public class ArticleProcessor implements Callable<Article> {

    public static final String FORMAT_PRIORITY = "site_imagePriority";
    public static final Pattern IMG_ATTRIB_PATTERN = Pattern.compile("<img (.+?)\\s?/?>");
    // "gram.css" -> ["gram", null, null, null, "css"]
    // "post/2019_MEAndromedaCombat×2½.avif" -> ["post/2019_MEAndromedaCombat", "2", null, "½", "avif"]
    // "post/2021_GTA3×0.5.avif" -> ["post/2021_GTA3", "0", ".5", null, "avif"]
    public static final Pattern IMG_MULTIPLIER = Pattern.compile("^(.+?)(?:×(\\d+)?(?:(\\.\\d+)|([⅒⅑⅛⅐⅙⅕¼⅓⅖⅜½⅗⅔⅝¾⅘⅚⅞]))?)?\\.(\\w+)$");
    public static final Map<String, Double> FRACTIONS = Map.ofEntries(
            Map.entry("⅒", 0.1), Map.entry("⅑", 1.0 / 9), Map.entry("⅛", 0.125),
            Map.entry("⅐", 1.0 / 7), Map.entry("⅙", 1.0 / 6), Map.entry("⅕", 0.2),
            Map.entry("¼", 0.25), Map.entry("⅓", 1.0 / 3), Map.entry("⅖", 0.4),
            Map.entry("⅜", 0.375), Map.entry("½", 0.5), Map.entry("⅗", 0.6),
            Map.entry("⅔", 2.0 / 3), Map.entry("⅝", 0.625), Map.entry("¾", 0.75),
            Map.entry("⅘", 0.8), Map.entry("⅚", 5.0 / 6), Map.entry("⅞", 0.875)
    );
    public static final Pattern ATTRIB_PATTERN = Pattern.compile("([^\\s=]+)(?:=[\\\"](.*?)[\\\"])?");
    public static final Pattern PARA_PATTERN = Pattern.compile("(<p>.*?</p>)", Pattern.DOTALL);
    public static final Pattern A_PATTERN = Pattern.compile("</?a(?:\\s.*?)?>");
    public static final Pattern HTML_TAG_PATTERN = Pattern.compile("<.+?>");
    // !?\["?(.+?)"?\]\(\S+?(?:\s"?(.+?)"?)?\)
    public static final Pattern MARKDOWN_LINK_PATTERN = Pattern.compile("!?\\[\"?(.+?)\"?\\]\\(\\S+?(?:\\s\"?(.+?)\"?)?\\)");

    private static final Logger LOG = Logger.getLogger(ArticleProcessor.class.getName());
    private final GramTenant ten;
    private final Article art;
    private long lowSizeEstimate = 0;
    private long highSizeEstimate = 0;
    private int resourceCount = 0;

    public static BigDecimal getImageMultiplier(String imageName) {
        Matcher m = IMG_MULTIPLIER.matcher(imageName);
        if (m.find()) {
            BigDecimal total = BigDecimal.ZERO;
            if (null != m.group(2)) {
                total = total.add(new BigDecimal(m.group(2)));
            }
            if (null != m.group(3)) {
                total = total.add(new BigDecimal(m.group(3)));
            } else if (null != m.group(4)) {
                total = total.add(new BigDecimal(FRACTIONS.get(m.group(4))));
            }
            return total.equals(BigDecimal.ZERO) ? BigDecimal.ONE : total;
        }
        return BigDecimal.ONE;
    }

    public ArticleProcessor(GramTenant ten, Article art) {
        this.art = art;
        this.ten = ten;
    }

    /**
     * article MUST have ID set, or else your homepage won't have links that go
     * anywhere!
     *
     * @return processed article (same object as passed in constructor)
     */
    @Override
    public Article call() {
        if (null == art) {
            throw new IllegalArgumentException("Can't process an article when YOU DON'T PASS IT!");
        }
        if (null != art.getPostedmarkdown() && null == art.getPostedhtml()) {
            art.setPostedhtml(Markdowner.getHtml(art.getPostedmarkdown()));
        } else if (null != art.getPostedhtml() && null == art.getPostedmarkdown()) {
            art.setPostedmarkdown(Markdowner.getMarkdown(art.getPostedhtml()));
            LOG.log(Level.FINE, "The markdown for article {0} was copied from HTML.", art.getArticletitle());
        } else if (null == art.getPostedmarkdown() && null == art.getPostedhtml()) {
            throw new IllegalArgumentException(String.format("The text for article %s cannot be recovered, because it has no HTML or markdown.", art.getArticletitle()));
        }
        if (null == art.getUuid()) {
            art.setUuid(UUID.randomUUID());
        }
        lowSizeEstimate = 0;
        highSizeEstimate = 0;
        String html = art.getPostedhtml();
        String paragraph = "";
        Matcher paraMatcher = PARA_PATTERN.matcher(html);
        while (paraMatcher.find()) {
            if (!paraMatcher.group(1).startsWith("<p><img ")) {
                paragraph = paraMatcher.group(1);
                paragraph = A_PATTERN.matcher(paragraph).replaceAll("");
                break;
            }
        }
        Matcher imgAttribMatcher = IMG_ATTRIB_PATTERN.matcher(html);
        while (imgAttribMatcher.find()) {
            resourceCount++;
            try {
                HashMap<String, String> origAttribs = new HashMap<>();
                String origImgTag = imgAttribMatcher.group(1);
                Matcher attribMatcher = ATTRIB_PATTERN.matcher(origImgTag);
                while (attribMatcher.find()) {
                    origAttribs.put(attribMatcher.group(1), attribMatcher.group(2));
                }
                getImageInfo(URLDecoder.decode(origAttribs.get("src"), "UTF-8"), origAttribs);
                StringBuilder pictureTag = new StringBuilder(500).append("<picture>");
                String tempImageURL = URLDecoder.decode(origAttribs.get("src"), "UTF-8").replaceAll(ten.getImeadValue(SecurityRepository.BASE_URL), "");
                Matcher stemmer = IMG_MULTIPLIER.matcher(tempImageURL);
                if (stemmer.find() && null != ten.getImeadValue(FORMAT_PRIORITY)) {
                    AtomicLong lowImageSize = new AtomicLong(0);
                    AtomicLong highImageSize = new AtomicLong(0);
                    String name = tempImageURL;
                    try {
                        name = BaseFileServlet.getNameFromURL(stemmer.group(1));
                    } catch (NoResultException lp) {
                    }
                    List<Fileupload> files = ten.getFile().search(name, null);
                    if (!files.isEmpty()) {
                        lowImageSize.set(Integer.MAX_VALUE);
                    }
                    String finalName = name;
                    for (String mime : ten.getImeadValue(FORMAT_PRIORITY).replaceAll("\r", "").split("\n")) {
                        List<String> srcset = new ArrayList<>();
                        files.stream().filter((Fileupload file) -> {
                            if (mime.equals(file.getMimetype())) {
                                Matcher sorter = IMG_MULTIPLIER.matcher(file.getFilename());
                                if (sorter.find()) {
                                    return finalName.equals(sorter.group(1));
                                }
                            }
                            return false;
                        }).sorted((Fileupload file1, Fileupload file2) -> {
                            BigDecimal x1 = getImageMultiplier(file1.getFilename());
                            BigDecimal x2 = getImageMultiplier(file2.getFilename());
                            return x1.subtract(x2).multiply(new BigDecimal(1000)).intValue();
                        }).forEach((Fileupload file) -> {
                            synchronized (lowImageSize) {
                                if (file.getDatasize() < lowImageSize.get()) {
                                    lowImageSize.set(file.getDatasize());
                                }
                            }
                            synchronized (highImageSize) {
                                if (highImageSize.get() < file.getDatasize()) {
                                    highImageSize.set(file.getDatasize());
                                }
                            }
                            BigDecimal width = UtilStatic.parseDecimal(origAttribs.get("width"), BigDecimal.ZERO);
                            try {
                                BigDecimal multiplier = getImageMultiplier(file.getFilename());
                                if (0 != width.intValue()) {
                                    // optimized for theandrewbailey.com and Google Pagespeed Insights
                                    int wvalue = Double.valueOf(Math.floor(width.multiply(multiplier).doubleValue() * 1.41)).intValue();
                                    srcset.add(ten.getImeadValue(SecurityRepository.BASE_URL) + file.getUrl() + " " + wvalue + "w");
                                } else {
                                    srcset.add(ten.getImeadValue(SecurityRepository.BASE_URL) + file.getUrl() + " " + multiplier + "x");
                                }
                            } catch (Exception x) {
                                srcset.add(ten.getImeadValue(SecurityRepository.BASE_URL) + file.getUrl() + (0 != width.intValue() ? " " + width + "w" : " 1x"));
                            }
                        });
                        if (!srcset.isEmpty()) {
                            Map<String, String> attribs = Map.of("type", mime, "srcset", String.join(", ", srcset));
                            pictureTag.append(createTag("source", attribs).append("/>"));
                        }
                    }
                    this.lowSizeEstimate += lowImageSize.get();
                    this.highSizeEstimate += highImageSize.get();
                }
                origAttribs.remove("type");
                if (Integer.parseInt(origAttribs.get("width")) < 960) {
                    origAttribs.put("class", "small");
                }
                String oldtag = imgAttribMatcher.group(0);
                String newtag = new StringBuilder(500).append(pictureTag).append(createTag("img", origAttribs).append("/>")).append("</picture>").toString();
                html = html.replace(oldtag, newtag);
                if (null == art.getSummary() && Integer.parseInt(origAttribs.get("width")) >= 600 && Integer.parseInt(origAttribs.get("height")) >= 300) {
                    art.setImageurl(origAttribs.get("src"));
                    origAttribs.put("loading", "lazy");
                    art.setSummary(String.format("<article class=\"article%s\" id=\"%s\"><a class=\"withFigure\" href=\"%s\"><figure>%s<figcaption><h1>%s</h1></figcaption></figure></a>%s</article>",
                            art.getArticleid(), art.getUuid().toString(), ArticleUrl.getUrl("", art, null),
                            pictureTag.append(createTag("img", origAttribs).append("/>").toString()).append("</picture>").toString(), art.getArticletitle(), paragraph
                    ));
                    deres(art);
                }
            } catch (UnsupportedEncodingException enc) {
                throw new JVMNotSupportedError(enc);
            }
        }
        art.setPostedhtml(html);
        if (null == art.getSummary()) {
            art.setSummary(String.format("<article class=\"article%s\" id=\"%s\"><header><a href=\"%s\"><h1>%s</h1></a></header>%s</article>",
                    art.getArticleid(), art.getUuid().toString(), ArticleUrl.getUrl("", art, null), art.getArticletitle(), paragraph));
        }
        return art;
    }

    @SuppressWarnings("UseSpecificCatch")
    private Map<String, String> getImageInfo(String url, Map<String, String> attributes) {
        if (null == attributes) {
            attributes = new HashMap<>();
        }
        try {
            Fileupload fileUpload = ten.getFile().get(BaseFileServlet.getNameFromURL(url));
            attributes.put("src", BaseFileServlet.getImmutableURL(ten.getImeadValue(SecurityRepository.BASE_URL), fileUpload));
            attributes.put("type", fileUpload.getMimetype());
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(fileUpload.getFiledata()));
            attributes.put("width", Integer.toString(image.getWidth()));
            attributes.put("height", Integer.toString(image.getHeight()));
        } catch (NoResultException | IllegalArgumentException | NullPointerException ia) {
            // file hasn't been uploaded, just guess something reasonable
            attributes.put("src", url);
            attributes.put("width", "960");
            attributes.put("height", "540");
        } catch (IOException e) {
        }
        return attributes;
    }

    /**
     *
     * @param tagname
     * @param attributes
     * @return an unterminated opening tag with the given tagname and attributes
     */
    private StringBuilder createTag(String tagname, Map<String, String> attributes) {
        StringBuilder tag = new StringBuilder(200).append("<").append(tagname);
        for (Map.Entry<String, String> attribute : attributes.entrySet()) {
            tag.append(" ").append(attribute.getKey());
            if (null != attribute.getValue()) {
                tag.append("=\"").append(attribute.getValue()).append("\"");
            }
        }
        return tag;
    }

    /**
     * Remove all image sizes, except for the smallest
     *
     * @param art
     * @return art
     */
    public static Article deres(Article art) {
        art.setSummary(art.getSummary().replaceAll("w, https?://.*? \\d+", ""));
        return art;
    }

    public Article getArt() {
        return art;
    }

    public long getLowSizeEstimate() {
        return lowSizeEstimate;
    }

    public long getHighSizeEstimate() {
        return highSizeEstimate;
    }

    public long getFixedSizeEstimate() {
        List<Fileupload> cssFiles = StyleSheet.getCssFiles(ten, null);
        List<Fileupload> javascriptFiles = HtmlScript.getJavascriptFiles(ten, null);
        Fileupload fileterm = new Fileupload();
        fileterm.setMimetype("font");
        try {
            return cssFiles.stream().mapToLong((f) -> {
                return f.getDatasize();
            }).sum() + javascriptFiles.stream().mapToLong((f) -> {
                return f.getDatasize();
            }).sum() + ten.getFile().search(fileterm, null).stream().mapToLong((f) -> {
                return f.getDatasize();
            }).sum() + art.getPostedhtml().getBytes("UTF-8").length;
        } catch (UnsupportedEncodingException dt) {
            throw new JVMNotSupportedError(dt);
        }
    }

    public int getResourceCount() {
        List<Fileupload> cssFiles = StyleSheet.getCssFiles(ten, null);
        List<Fileupload> javascriptFiles = HtmlScript.getJavascriptFiles(ten, null);
        Fileupload fileterm = new Fileupload();
        fileterm.setMimetype("font");
        return resourceCount + cssFiles.size() + javascriptFiles.size() + ten.getFile().search(fileterm, null).size();
    }
}
