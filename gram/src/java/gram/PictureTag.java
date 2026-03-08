package gram;

import gram.bean.GramTenant;
import jakarta.persistence.NoResultException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import libWebsiteTools.file.BaseFileServlet;
import libWebsiteTools.file.Fileupload;
import libWebsiteTools.security.SecurityRepository;

/**
 *
 * @author alpha
 */
public class PictureTag {

    // "gram.css" -> ["gram", null, null, null, "css"]
    // "post/2019_MEAndromedaCombat×2½.avif" -> ["post/2019_MEAndromedaCombat", "2", null, "½", "avif"]
    // "post/2021_GTA3×0.5.avif" -> ["post/2021_GTA3", "0", ".5", null, "avif"]
    public static final Pattern IMG_MULTIPLIER = Pattern.compile("^(.+?)(?:×(\\d+)?(?:(\\.\\d+)|([⅒⅑⅛⅐⅙⅕¼⅓⅖⅜½⅗⅔⅝¾⅘⅚⅞]))?)?\\.(\\w+)$");
    public static final String FORMAT_PRIORITY = "site_imagePriority";
    public static final Map<String, Double> FRACTIONS = Map.ofEntries(
            Map.entry("⅒", 0.1), Map.entry("⅑", 1.0 / 9), Map.entry("⅛", 0.125),
            Map.entry("⅐", 1.0 / 7), Map.entry("⅙", 1.0 / 6), Map.entry("⅕", 0.2),
            Map.entry("¼", 0.25), Map.entry("⅓", 1.0 / 3), Map.entry("⅖", 0.4),
            Map.entry("⅜", 0.375), Map.entry("½", 0.5), Map.entry("⅗", 0.6),
            Map.entry("⅔", 2.0 / 3), Map.entry("⅝", 0.625), Map.entry("¾", 0.75),
            Map.entry("⅘", 0.8), Map.entry("⅚", 5.0 / 6), Map.entry("⅞", 0.875)
    );

    private StringBuilder pictureTag;
    private final AtomicLong lowImageSize = new AtomicLong(0);
    private final AtomicLong highImageSize = new AtomicLong(0);
    private List<Fileupload> fileUploads;
    private String fileStem;

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

    public PictureTag(GramTenant ten, Map<String, String> origAttribs) throws UnsupportedEncodingException {
        BigDecimal width = UtilStatic.parseDecimal(origAttribs.get("width"), BigDecimal.ZERO);
        pictureTag = new StringBuilder(500).append("<picture>");
        String tempImageURL = URLDecoder.decode(origAttribs.get("src"), "UTF-8").replaceAll(ten.getImeadValue(SecurityRepository.BASE_URL), "");
        Matcher stemmer = IMG_MULTIPLIER.matcher(tempImageURL);
        if (stemmer.find() && null != ten.getImeadValue(FORMAT_PRIORITY)) {
            String name = tempImageURL;
            try {
                fileStem = stemmer.group(1);
                name = BaseFileServlet.getNameFromURL(fileStem);
            } catch (NoResultException lp) {
            }
            List<Fileupload> files = ten.getFile().search(name, null);
            LinkedHashSet<Fileupload> tempUploads = new LinkedHashSet<>(files.size() + 1);
            if (!files.isEmpty()) {
                lowImageSize.set(Integer.MAX_VALUE);
            }
            String finalName = name;
            for (String mime : ten.getImeadValue(FORMAT_PRIORITY).replaceAll("\r", "").split("\n")) {
                List<String> srcset = new ArrayList<>();
                Stream<Fileupload> fileStr = files.stream().filter((Fileupload file) -> {
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
                });
                List<Fileupload> fileList = fileStr.toList();
                tempUploads.addAll(fileList);
                fileList.forEach((Fileupload file) -> {
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
            fileUploads = Collections.unmodifiableList(List.copyOf(tempUploads));
        }
    }

    /**
     *
     * @param tagname
     * @param attributes
     * @return an unterminated opening tag with the given tagname and attributes
     */
    public static StringBuilder createTag(String tagname, Map<String, String> attributes) {
        StringBuilder tag = new StringBuilder(200).append("<").append(tagname);
        for (Map.Entry<String, String> attribute : attributes.entrySet()) {
            tag.append(" ").append(attribute.getKey());
            if (null != attribute.getValue()) {
                tag.append("=\"").append(attribute.getValue()).append("\"");
            }
        }
        return tag;
    }

    @Override
    public String toString() {
        return pictureTag.toString();
    }

    /**
     * Remove all image sizes, except for the smallest
     *
     * @param tags
     * @return String
     */
    public static String deres(String tags) {
        return tags.replaceAll("w, https?://.*? \\d+", "").replaceAll("x, https?://.*? \\d+", "");
    }

    public StringBuilder get() {
        return pictureTag;
    }

    public Long getLowImageSize() {
        return lowImageSize.get();
    }

    public Long getHighImageSize() {
        return highImageSize.get();
    }

    public List<Fileupload> getFileUploads() {
        return fileUploads;
    }

    public String getFileStem() {
        return fileStem;
    }
}
