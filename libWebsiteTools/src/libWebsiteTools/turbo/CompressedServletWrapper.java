package libWebsiteTools.turbo;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

/**
 *
 * @author alpha
 */
public class CompressedServletWrapper extends HttpServletResponseWrapper implements Closeable {

    public static CompressedServletWrapper getInstance(HttpServletRequest req, HttpServletResponse res) throws IOException {
        CompressedServletWrapper wrap = new CompressedServletWrapper(CompressedOutput.getInstance(req), res);
        wrap.getOutputStream().getOutputStream(res); // initialize OutputStream
        return wrap;
    }

    private final CompressedOutput compressedOut;
    private PrintWriter printWriter;

    public CompressedServletWrapper(CompressedOutput type, HttpServletResponse res) throws IOException {
        super(res);
        compressedOut = type;
    }

    @Override
    public void close() throws IOException {
        if (printWriter != null) {
            printWriter.close();
        }
        compressedOut.close();
    }

    @Override
    public void addHeader(String name, String value) {
        ((HttpServletResponse) getResponse()).addHeader(name, value);
    }

    @Override
    public void setHeader(String name, String value) {
        ((HttpServletResponse) getResponse()).setHeader(name, value);
    }

    @Override
    public void flushBuffer() throws IOException {
        if (printWriter != null) {
            printWriter.flush();
        }
        compressedOut.flush();
    }

    @Override
    public CompressedOutput getOutputStream() throws IOException {
        return compressedOut;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        if (printWriter == null) {
            printWriter = new PrintWriter(new OutputStreamWriter(getOutputStream(), getResponse().getCharacterEncoding()));
        }
        return printWriter;
    }

    @Override
    public void setContentLength(int len) {
    }
}
