package libWebsiteTools.turbo;

import com.aayushatharva.brotli4j.Brotli4jLoader;
import com.aayushatharva.brotli4j.encoder.BrotliOutputStream;
import com.aayushatharva.brotli4j.encoder.Encoder;
import com.github.luben.zstd.ZstdOutputStream;
import com.github.luben.zstd.util.Native;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.HttpHeaders;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.GZIPOutputStream;
import libWebsiteTools.CompressionAlgorithm;

/**
 *
 * @author alpha
 */
public abstract class CompressedOutput extends ServletOutputStream implements CompressionAlgorithm<HttpServletResponse, HttpServletRequest> {

    public static final Map<String, CompressedOutput> TESTERS;
    public static final Zstd ZSTD = new Zstd();
    public static final Brotli BR = new Brotli();
    public static final Gzip GZIP = new Gzip();
    public static final None NONE = new None();

    static {
        LinkedHashMap<String, CompressedOutput> algorithms = new LinkedHashMap<>();
        Native.load();
        if (Native.isLoaded()) {
            algorithms.put(ZSTD.getType(), ZSTD);
        }
        Brotli4jLoader.ensureAvailability();
        if (Brotli4jLoader.isAvailable()) {
            algorithms.put(BR.getType(), BR);
        }
        algorithms.put(GZIP.getType(), GZIP);
        algorithms.put(NONE.getType(), NONE);
        TESTERS = Collections.unmodifiableMap(algorithms);
    }

    public static class Zstd extends CompressedOutput {

        public Zstd() {
        }

        @Override
        public String getType() {
            return "zstd";
        }

        @Override
        public boolean shouldCompress(HttpServletRequest req) {
            if (Native.isLoaded()) {
                String encoding = req.getHeader(HttpHeaders.ACCEPT_ENCODING);
                if (null != encoding) {
                    return Arrays.asList(encoding.split(", ")).contains(getType());
                }
            }
            return false;
        }

        @Override
        public OutputStream getOutputStream(HttpServletResponse res) throws IOException {
            if (null == compressedStream) {
                res.setHeader(HttpHeaders.CONTENT_ENCODING, getType());
                collector = new ByteArrayOutputStream(20000);
                compressedStream = new ZstdOutputStream(collector).setWorkers(1);
            }
            return compressedStream;
        }
    }

    public static class Brotli extends CompressedOutput {

        public Brotli() {
        }

        @Override
        public String getType() {
            return "br";
        }

        @Override
        public boolean shouldCompress(HttpServletRequest req) {
            if (Brotli4jLoader.isAvailable()) {
                String encoding = req.getHeader(HttpHeaders.ACCEPT_ENCODING);
                if (null != encoding) {
                    return Arrays.asList(encoding.split(", ")).contains(getType());
                }
            }
            return false;
        }

        @Override
        public OutputStream getOutputStream(HttpServletResponse res) throws IOException {
            if (null == compressedStream) {
                res.setHeader(HttpHeaders.CONTENT_ENCODING, getType());
                collector = new ByteArrayOutputStream(20000);
                // assuming this is going to a JSP that has mostly text
                Encoder.Parameters params = new Encoder.Parameters().setMode(Encoder.Mode.TEXT);
                compressedStream = new BrotliOutputStream(collector, params);
            }
            return compressedStream;
        }
    }

    public static class Gzip extends CompressedOutput {

        public Gzip() {
        }

        @Override
        public String getType() {
            return "gzip";
        }

        @Override
        public boolean shouldCompress(HttpServletRequest req) {
            String encoding = req.getHeader(HttpHeaders.ACCEPT_ENCODING);
            if (null != encoding) {
                return Arrays.asList(encoding.split(", ")).contains(getType());
            }
            return false;
        }

        @Override
        public OutputStream getOutputStream(HttpServletResponse res) throws IOException {
            if (null == compressedStream) {
                res.setHeader(HttpHeaders.CONTENT_ENCODING, getType());
                collector = new ByteArrayOutputStream(20000);
                compressedStream = new GZIPOutputStream(collector);
            }
            return compressedStream;
        }
    }

    public static class None extends CompressedOutput {

        @Override
        public String getType() {
            return "none";
        }
        protected ByteArrayOutputStream uncompressedStream;

        public None() {
            getOutputStream(null);
        }

        @Override
        public boolean shouldCompress(HttpServletRequest req) {
            return true;
        }

        @Override
        public ByteArrayOutputStream getOutputStream(HttpServletResponse res) {
            if (null == uncompressedStream) {
                collector = new ByteArrayOutputStream(65000);
                uncompressedStream = collector;
                compressedStream = collector;
            }
            return uncompressedStream;
        }
    }

    public static String getTypeString(HttpServletRequest req) {
        for (Map.Entry<String, CompressedOutput> a : TESTERS.entrySet()) {
            if (a.getValue().shouldCompress(req)) {
                return a.getKey();
            }
        }
        return NONE.getType();
    }

    public static CompressedOutput getInstance(HttpServletRequest req) {
        for (Map.Entry<String, CompressedOutput> a : CompressedOutput.TESTERS.entrySet()) {
            if (a.getValue().shouldCompress(req)) {
                try {
                    return a.getValue().getClass().getConstructor().newInstance();
                } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
        return new CompressedOutput.None();
    }

    protected OutputStream compressedStream;
    protected final AtomicBoolean open = new AtomicBoolean(true);
    protected ByteArrayOutputStream collector;

    @Override
    public byte[] getResult() {
        try {
            flush();
            close();
        } catch (IOException ex) {
        }
        return collector.toByteArray();
    }

    @Override
    public HttpServletResponse setResult(HttpServletResponse input, byte[] compressedData) {
        try {
            ServletOutputStream out = input.getOutputStream();
            out.write(compressedData);
            out.flush();
        } catch (IOException ix) {
        }
        return input;
    }

    @Override
    public void close() throws IOException {
        if (open.compareAndSet(true, false)) {
            compressedStream.close();
        }
    }

    @Override
    public void flush() throws IOException {
        compressedStream.flush();
    }

    @Override
    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (!open.get()) {
            throw new IOException("Stream closed!");
        }
        compressedStream.write(b, off, len);
    }

    @Override
    public void write(int b) throws IOException {
        if (!open.get()) {
            throw new IOException("Stream closed!");
        }
        compressedStream.write(b);
    }

    @Override
    public boolean isReady() {
        return open.get();
    }

    @Override
    public void setWriteListener(WriteListener wl) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return getType();
    }

    public boolean equals(Object obj) {
        return this.getClass().getCanonicalName().equals(obj.getClass().getCanonicalName());
    }
}
