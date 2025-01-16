package libWebsiteTools.file;

import com.aayushatharva.brotli4j.Brotli4jLoader;
import com.aayushatharva.brotli4j.encoder.BrotliOutputStream;
import com.aayushatharva.brotli4j.encoder.Encoder;
import com.github.luben.zstd.ZstdOutputStream;
import com.github.luben.zstd.util.Native;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import libWebsiteTools.CompressionAlgorithm;
import libWebsiteTools.turbo.GzipOutputStream;
import libWebsiteTools.Tenant;

/**
 *
 * @author alpha
 */
public abstract class FileCompressorJob implements Callable<Boolean>, Comparable<FileCompressorJob>, CompressionAlgorithm<Fileupload, Fileupload> {

    static {
        Native.load();
        Brotli4jLoader.ensureAvailability();
    }

    public static class Zstd extends FileCompressorJob {

        private ByteArrayOutputStream collector;

        public Zstd(Tenant beans, Fileupload file) {
            super(beans, file);
            if (!Native.isLoaded()) {
                throw new IllegalStateException("Zstd is not loaded.");
            }
        }

        @Override
        public String getType() {
            return "zstd";
        }

        @Override
        public boolean shouldCompress(Fileupload file) {
            return null == file.getZstddata();
        }

        @Override
        public OutputStream getOutputStream(Fileupload file) throws IOException {
            collector = new ByteArrayOutputStream(file.getFiledata().length * 2);
            return new ZstdOutputStream(collector, 19);
        }

        @Override
        public Fileupload setResult(Fileupload file, byte[] compressedData) {
            file.setZstddata(compressedData);
            return file;
        }

        @Override
        public byte[] getResult() {
            return collector.toByteArray();
        }
    }

    public static class Brotli extends FileCompressorJob {

        private ByteArrayOutputStream collector;

        public Brotli(Tenant beans, Fileupload file) {
            super(beans, file);
            if (!Brotli4jLoader.isAvailable()) {
                throw new IllegalStateException("Brotli is not loaded.");
            }
        }

        @Override
        public String getType() {
            return "br";
        }

        @Override
        public boolean shouldCompress(Fileupload file) {
            return null == file.getBrdata();
        }

        @Override
        public OutputStream getOutputStream(Fileupload file) throws IOException {
            collector = new ByteArrayOutputStream(file.getFiledata().length * 2);
            Encoder.Parameters params = new Encoder.Parameters().setWindow(24).setQuality(11)
                    .setMode(file.getMimetype().contains("text/") ? Encoder.Mode.TEXT : Encoder.Mode.GENERIC);
            return new BrotliOutputStream(collector, params);
        }

        @Override
        public Fileupload setResult(Fileupload file, byte[] compressedData) {
            file.setBrdata(compressedData);
            return file;
        }

        @Override
        public byte[] getResult() {
            return collector.toByteArray();
        }
    }

    public static class Gzip extends FileCompressorJob {

        private ByteArrayOutputStream collector;

        public Gzip(Tenant beans, Fileupload file) {
            super(beans, file);
        }

        @Override
        public String getType() {
            return "gzip";
        }

        @Override
        public boolean shouldCompress(Fileupload file) {
            return null == file.getGzipdata();
        }

        @Override
        public OutputStream getOutputStream(Fileupload file) throws IOException {
            collector = new ByteArrayOutputStream(file.getFiledata().length * 2);
            return new GzipOutputStream(collector, 9);
        }

        @Override
        public Fileupload setResult(Fileupload file, byte[] compressedData) {
            file.setGzipdata(compressedData);
            return file;
        }

        @Override
        public byte[] getResult() {
            return collector.toByteArray();
        }
    }

    public static List<Future> startAllJobs(Tenant ten, Fileupload file) {
        return List.of(ten.getExec().submit(new Gzip(ten, file)),
                ten.getExec().submit(new Brotli(ten, file)),
                ten.getExec().submit(new Zstd(ten, file)));
    }
    private final static Logger LOG = Logger.getLogger(FileCompressorJob.class.getName());
    final Fileupload file;
    final Tenant ten;

    public FileCompressorJob(Tenant ten, Fileupload file) {
        this.ten = ten;
        this.file = file;
    }

    @Override
    public Boolean call() {
        if (!shouldCompress(file)) {
            LOG.log(Level.FINEST, "File {0} already compressed.", file.getFilename());
            return false;
        }

        try (OutputStream out = getOutputStream(file)) {
            out.write(file.getFiledata());
        } catch (IOException ex) {
            Logger.getLogger(FileCompressorJob.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException(ex);
        }
        byte[] compressedData = getResult();
        synchronized (file) {
            if (null != compressedData && compressedData.length < file.getFiledata().length) {
                Fileupload activeFile = ten.getFile().get(file.getFilename());
                ten.getFile().upsert(Arrays.asList(setResult(activeFile, compressedData)));
                ten.getFile().evict();
                ten.getGlobalCache().clear();
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return this.getClass().equals(obj.getClass()) && this.toString().equals(obj.toString());
    }

    @Override
    public int compareTo(FileCompressorJob t) {
        return t.file.getFiledata().length - file.getFiledata().length;
    }

    @Override
    public String toString() {
        return this.getClass().getName() + " " + file.getFilename();
    }
}
