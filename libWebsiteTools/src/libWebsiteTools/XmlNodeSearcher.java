package libWebsiteTools;

import java.util.Iterator;
import java.util.NoSuchElementException;
import org.w3c.dom.Node;

/**
 * searches for direct child nodes of an XML node that have the given name.
 *
 * @author alpha
 */
public class XmlNodeSearcher implements Iterable<Node> {

    private final String nodeName;
    private final Node parent;

    public XmlNodeSearcher(Node parent, String nodeName) {
        this.nodeName = nodeName;
        this.parent = parent;
    }

    public long getLength() {
        long count = 0L;
        for (Node current = parent.getFirstChild(); current != null; current = current.getNextSibling()) {
            if (nodeName.equals(current.getNodeName())) {
                ++count;
            }
        }
        return count;
    }

    @Override
    public Iterator<Node> iterator() {
        return new Iterator<Node>() {
            private Node current = parent.getFirstChild();
            private Node returnthis;

            @Override
            public boolean hasNext() {
                if (returnthis != null) {
                    return true;
                }
                if (current == null) {
                    return false;
                }
                for (; current != null; current = current.getNextSibling()) {
                    if (nodeName.equals(current.getNodeName())) {
                        returnthis = current;
                        return true;
                    }
                }
                return false;
            }

            @Override
            public Node next() {
                if (hasNext()) {
                    Node temp = returnthis;
                    returnthis = null;
                    current = current.getNextSibling();
                    return temp;
                } else {
                    throw new NoSuchElementException();
                }
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
}
