package libWebsiteTools.file;

import java.util.List;
import libWebsiteTools.Repository;

/**
 *
 * @author alpha
 */
public interface FileRepository extends Repository<Fileupload> {

    public final String DEFAULT_MIME_TYPE = "application/octet-stream";
    public final String ZSTD_COMPRESSION = "zstddata";
    public final String BR_COMPRESSION = "brdata";
    public final String GZIP_COMPRESSION = "gzipdata";
    public final String NO_COMPRESSION = "filedata";

    /**
     *
     * @param names
     * @return metadata of requested names, or everything for null
     */
    List<Fileupload> getFileMetadata(List<String> names);

    /**
     *
     * @param metadata
     * @param compression
     * @return data
     */
    byte[] getFiledata(Fileupload metadata, String compression);
}
