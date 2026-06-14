package libWebsiteTools.file;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.HttpHeaders;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.LinkedList;
import libWebsiteTools.CompressionAlgorithm;
import libWebsiteTools.Landlord;
import libWebsiteTools.Tenant;

/**
 * Because sometimes other compression methods have a smaller size for a random
 * file. Note: this class has a natural ordering that is inconsistent with
 * equals.
 *
 * @author alpha
 */
public abstract class CompressionSorter implements CompressionAlgorithm<HttpServletResponse, HttpServletRequest>, Comparable<CompressionSorter> {

    public static CompressionSorter getInstance(Fileupload f, HttpServletRequest req) {
        LinkedList<CompressionSorter> algorithms = new LinkedList<>();
        Tenant ten = Landlord.getTenant(req);
        ZstdFile z = new ZstdFile(f, ten);
        if (z.shouldCompress(req)) {
            algorithms.push(z);
        }
        BrFile b = new BrFile(f, ten);
        if (b.shouldCompress(req)) {
            algorithms.push(b);
        }
        GzipFile g = new GzipFile(f, ten);
        if (g.shouldCompress(req)) {
            algorithms.push(g);
        }
        algorithms.push(new PlainFile(f, ten));
        algorithms.sort(null);
        return algorithms.poll();
    }

    /**
     * Set response headers, return null
     *
     * @param res
     * @return null
     * @throws IOException
     */
    @Override
    public OutputStream getOutputStream(HttpServletResponse res) throws IOException {
        res.setHeader(HttpHeaders.CONTENT_ENCODING, getType());
        res.setContentLength(getResult().length);
        return null;
    }

    @Override
    public HttpServletResponse setResult(HttpServletResponse res, byte[] compressedData) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean shouldCompress(HttpServletRequest req) {
        if (0 != getSize()) {
            String encoding = req.getHeader(HttpHeaders.ACCEPT_ENCODING);
            if (null != encoding) {
                return Arrays.asList(encoding.split(", ")).contains(getType());
            }
        }
        return false;
    }

    @Override
    public int compareTo(CompressionSorter t) {
        return this.getSize() - t.getSize();
    }

    public abstract int getSize();

    private static class ZstdFile extends CompressionSorter {

        private final Fileupload f;
        private final Tenant ten;

        public ZstdFile(Fileupload file, Tenant t) {
            f = file;
            ten = t;
        }

        @Override
        public String getType() {
            return "zstd";
        }

        @Override
        public byte[] getResult() {
            if (null == f.getZstddata()) {
                f.setZstddata(ten.getFile().getFiledata(f, FileRepository.ZSTD_COMPRESSION));
            }
            return f.getZstddata();
        }

        @Override
        public int getSize() {
            return null != f.getZstdsize() ? f.getZstdsize() : 0;
        }

    }

    private static class BrFile extends CompressionSorter {

        private final Fileupload f;
        private final Tenant ten;

        public BrFile(Fileupload file, Tenant t) {
            f = file;
            ten = t;
        }

        @Override
        public String getType() {
            return "br";
        }

        @Override
        public byte[] getResult() {
            if (null == f.getBrdata()) {
                f.setBrdata(ten.getFile().getFiledata(f, FileRepository.BR_COMPRESSION));
            }
            return f.getBrdata();
        }

        @Override
        public int getSize() {
            return null != f.getBrsize() ? f.getBrsize() : 0;
        }
    }

    private static class GzipFile extends CompressionSorter {

        private final Fileupload f;
        private final Tenant ten;

        public GzipFile(Fileupload file, Tenant t) {
            f = file;
            ten = t;
        }

        @Override
        public String getType() {
            return "gzip";
        }

        @Override
        public byte[] getResult() {
            if (null == f.getGzipdata()) {
                f.setGzipdata(ten.getFile().getFiledata(f, FileRepository.GZIP_COMPRESSION));
            }
            return f.getGzipdata();
        }

        @Override
        public int getSize() {
            return null != f.getGzipsize() ? f.getGzipsize() : 0;
        }
    }

    private static class PlainFile extends CompressionSorter {

        private final Fileupload f;
        private final Tenant ten;

        public PlainFile(Fileupload file, Tenant t) {
            f = file;
            ten = t;
        }

        @Override
        public OutputStream getOutputStream(HttpServletResponse res) throws IOException {
            res.setContentLength(getResult().length);
            return null;
        }

        @Override
        public boolean shouldCompress(HttpServletRequest req) {
            return true;
        }

        @Override
        public String getType() {
            return "plain";
        }

        @Override
        public byte[] getResult() {
            if (null == f.getFiledata()) {
                f.setFiledata(ten.getFile().getFiledata(f, FileRepository.NO_COMPRESSION));
            }
            return f.getFiledata();
        }

        @Override
        public int getSize() {
            return null != f.getDatasize() ? f.getDatasize() : 0;
        }
    }
}
