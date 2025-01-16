package libWebsiteTools;

import java.io.IOException;
import java.io.OutputStream;

/**
 *
 * @author alpha
 * @param <OperatesOn>
 * @param <QueryOn>
 */
public interface CompressionAlgorithm<OperatesOn, QueryOn> {

    public String getType();

    public boolean shouldCompress(QueryOn input);

    public OutputStream getOutputStream(OperatesOn input) throws IOException;

    public OperatesOn setResult(OperatesOn input, byte[] compressedData);

    public byte[] getResult();
}
