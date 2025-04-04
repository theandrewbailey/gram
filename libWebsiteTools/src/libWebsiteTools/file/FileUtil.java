package libWebsiteTools.file;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;
import libWebsiteTools.security.HashUtil;
import libWebsiteTools.tag.AbstractInput;
import libWebsiteTools.Tenant;

/**
 *
 * @author alpha
 */
public class FileUtil {

    private final static Logger LOG = Logger.getLogger(FileUtil.class.getName());

    public static List<Fileupload> getFilesFromRequest(HttpServletRequest req, String fieldname) throws IOException, ServletException {
        List<Part> fileparts = AbstractInput.getParts(req, fieldname);
        List<Fileupload> files = new ArrayList<>(fileparts.size());
        for (Part filepart : fileparts) {
            byte[] tehFile = new byte[(int) (filepart.getSize())];
            if (0 == tehFile.length) {
                throw new FileNotFoundException();
            }
            String fileName = filepart.getHeader("content-disposition").split("filename=\"")[1];
            String dir = getParam(req, "directory");
            dir = dir == null ? "" : dir;
            if (0 != dir.length() && !dir.endsWith("/")) {
                dir += "/";
            }
            fileName = dir + fileName.substring(0, fileName.length() - 1);
            try (DataInputStream dis = new DataInputStream(filepart.getInputStream())) {
                dis.readFully(tehFile);
            }
            Fileupload file = new Fileupload();
            file.setAtime(OffsetDateTime.now());
            file.setEtag(HashUtil.getSHA256Hash(tehFile));
            file.setFiledata(tehFile);
            file.setFilename(fileName);
            if (file.getFilename().endsWith(".js")) {
                file.setMimetype("text/javascript");
            } else if (file.getFilename().endsWith(".css")) {
                file.setMimetype("text/css");
            } else {
                file.setMimetype(filepart.getContentType());
            }
            files.add(file);
        }
        return files;
    }

    public static Fileupload loadFile(Tenant ten, String filename, String type, InputStream filedata) throws IOException {
        Fileupload f = null;
        if (null == ten.getFile().get(filename)) {
            f = new Fileupload(filename, OffsetDateTime.now());
            f.setFiledata(FileUtil.getByteArray(filedata));
            f.setEtag(HashUtil.getSHA256Hash(f.getFiledata()));
            f.setMimetype(type);
            f.setUrl(BaseFileServlet.getImmutableURL("", f));
        }
        return f;
    }

    public static byte[] runProcess(String command, byte[] stdin, int expectedOutputSize) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream(expectedOutputSize);
        ByteArrayOutputStream error = new ByteArrayOutputStream(4000);
        Process encoder = Runtime.getRuntime().exec(command);
        if (null != stdin) {
            try (OutputStream out = encoder.getOutputStream()) {
                out.write(stdin);
            }
        }
        LOG.log(Level.FINE, "running command: {0}", command);
        while (true) {
            try {
                try (InputStream input = encoder.getInputStream()) {
                    byte content[] = new byte[expectedOutputSize];
                    int readCount = 0;
                    while (-1 != (readCount = input.read(content))) {
                        output.write(content, 0, readCount);
                    }
                }
                try (InputStream input = encoder.getErrorStream()) {
                    byte content[] = new byte[4000];
                    int readCount = 0;
                    while (-1 != (readCount = input.read(content))) {
                        error.write(content, 0, readCount);
                    }
                }
                int exitcode = encoder.waitFor();
                if (exitcode == 0) {
                } else {
                    LOG.log(Level.WARNING, "Command exited with {0}:\n{1}", new Object[]{exitcode, new String(error.toByteArray())});
                }
                break;
            } catch (InterruptedException | IOException ix) {
                throw new RuntimeException("Problem while running command: " + command, ix);
            }
        }
        encoder.destroy();
        return output.toByteArray();
    }

    /**
     * retrieves a string from a multipart upload request
     *
     * @param req
     * @param param
     * @return value | null
     */
    public static String getParam(HttpServletRequest req, String param) {
        try {
            return new BufferedReader(new InputStreamReader(AbstractInput.getPart(req, param).getInputStream())).readLine();
        } catch (IOException | ServletException e) {
            return null;
        }
    }

    /**
     * useful for getting file contents out of a zip
     *
     * @param in
     * @return
     * @throws IOException
     */
    public static byte[] getByteArray(InputStream in) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[65536];
        int read;
        while ((read = in.read(buf)) != -1) {
            baos.write(buf, 0, read);
        }
        return baos.toByteArray();
    }
}
