package libWebsiteTools;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.commonmark.Extension;
import org.commonmark.ext.autolink.AutolinkExtension;
import org.commonmark.ext.footnotes.FootnotesExtension;
import org.commonmark.ext.front.matter.YamlFrontMatterExtension;
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.ext.heading.anchor.HeadingAnchorExtension;
import org.commonmark.ext.image.attributes.ImageAttributesExtension;
import org.commonmark.ext.ins.InsExtension;
import org.commonmark.ext.task.list.items.TaskListItemsExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

/**
 *
 * @author alpha
 */
public class Markdowner {

    private static final Logger LOG = Logger.getLogger(Markdowner.class.getName());
    private static final List<Extension> COMMONMARK_EXTENSIONS = List.of(
            AutolinkExtension.create(),
            FootnotesExtension.create(),
            YamlFrontMatterExtension.create(),
            StrikethroughExtension.create(),
            TablesExtension.create(),
            HeadingAnchorExtension.create(),
            ImageAttributesExtension.create(),
            InsExtension.create(),
            TaskListItemsExtension.create());
    private static final Parser COMMONMARK_PARSER = Parser.builder().extensions(COMMONMARK_EXTENSIONS).build();
    private static final HtmlRenderer COMMONMARK_HTML_RENDERER = HtmlRenderer.builder().extensions(COMMONMARK_EXTENSIONS).build();

    public static String getMarkdown(String html) {
        // TODO: find library to convert HTML to proper markdown, pass through HTML for now
        LOG.log(Level.FINER, "HTML (not) converted to markdown");
        return html;
    }

    public static String getHtml(String markdown) {
        Node n = COMMONMARK_PARSER.parse(markdown);
        String html = COMMONMARK_HTML_RENDERER.render(n);
        LOG.log(Level.FINER, "converted markdown to HTML");
        return html;
    }
}
