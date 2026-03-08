package gram;

import gram.bean.GramTenant;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
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
import libWebsiteTools.tag.HtmlScript;
import libWebsiteTools.tag.StyleSheet;

/**
 * instantiate as needed to process articles in threads
 *
 * @author alpha
 */
public class ArticleProcessor implements Callable<Article> {

    public static final Pattern IMG_ATTRIB_PATTERN = Pattern.compile("<img (.+?)\\s?/?>");
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
            HashMap<String, String> origAttribs = new HashMap<>();
            String origImgTag = imgAttribMatcher.group(1);
            Matcher attribMatcher = ATTRIB_PATTERN.matcher(origImgTag);
            while (attribMatcher.find()) {
                origAttribs.put(attribMatcher.group(1), attribMatcher.group(2));
            }
            try {
                getImageInfo(URLDecoder.decode(origAttribs.get("src"), "UTF-8"), origAttribs);
                PictureTag pictureTag = new PictureTag(ten, origAttribs);
                if (pictureTag.getLowImageSize() != 0) {
                    lowSizeEstimate += pictureTag.getLowImageSize();
                }
                if (pictureTag.getHighImageSize() != 0) {
                    highSizeEstimate += pictureTag.getHighImageSize();
                }
                origAttribs.remove("type");
                if (Integer.parseInt(origAttribs.get("width")) < 960) {
                    origAttribs.put("class", "small");
                }
                String oldtag = imgAttribMatcher.group(0);
                String newtag = new StringBuilder(500).append(pictureTag.get()).append(PictureTag.createTag("img", origAttribs).append("/>")).append("</picture>").toString();
                html = html.replace(oldtag, newtag);
                if (null == art.getSummary() && Integer.parseInt(origAttribs.get("width")) >= 600 && Integer.parseInt(origAttribs.get("height")) >= 300) {
                    art.setImageurl(origAttribs.get("src"));
                    origAttribs.put("loading", "lazy");
                    String summary = String.format("<article class=\"article%s\" id=\"%s\"><a class=\"withFigure\" href=\"%s\"><figure>%s<figcaption><h1>%s</h1></figcaption></figure></a>%s</article>",
                            art.getArticleid(), art.getUuid().toString(), ArticleUrl.getUrl("", art, null),
                            pictureTag.get().append(PictureTag.createTag("img", origAttribs).append("/>").toString()).append("</picture>").toString(), art.getArticletitle(), paragraph);
                    art.setSummary(PictureTag.deres(summary));
                }
            } catch (NoResultException n) {
                if (null == art.getSummary()) {
                    art.setImageurl(origAttribs.get("src"));
                    origAttribs.put("loading", "lazy");
                    art.setSummary(String.format("<article class=\"article%s\" id=\"%s\"><a class=\"withFigure\" href=\"%s\"><figure>%s<figcaption><h1>%s</h1></figcaption></figure></a>%s</article>",
                            art.getArticleid(), art.getUuid().toString(), ArticleUrl.getUrl("", art, null),
                            PictureTag.createTag("img", origAttribs).append("/>").append("</picture>").toString(), art.getArticletitle(), paragraph
                    ));
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

    /**
     *
     * @param url search fileuploads for this URL
     * @param attributes pass through attributes
     * @return ["url", "type", "width", "height"]
     */
    @SuppressWarnings("UseSpecificCatch")
    private Map<String, String> getImageInfo(String url, Map<String, String> attributes) {
        if (null == attributes) {
            attributes = new HashMap<>();
        }
        try {
            Fileupload fileUpload = ten.getFile().get(BaseFileServlet.getNameFromURL(url));
            if (null == fileUpload) {
                throw new NoResultException();
            }
            attributes.put("src", BaseFileServlet.getImmutableURL(ten.getImeadValue(SecurityRepository.BASE_URL), fileUpload));
            attributes.put("type", fileUpload.getMimetype());
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(fileUpload.getFiledata()));
            attributes.put("width", Integer.toString(image.getWidth()));
            attributes.put("height", Integer.toString(image.getHeight()));
        } catch (IllegalArgumentException | NullPointerException ia) {
            // file hasn't been uploaded, just guess something reasonable
            attributes.put("src", url);
            attributes.put("width", "960");
            attributes.put("height", "540");
        } catch (IOException e) {
        }
        return attributes;
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
