package libWebsiteTools.file;

import java.util.List;
import libWebsiteTools.Repository;

/**
 *
 * @author alpha
 */
public interface FileRepository extends Repository<Fileupload> {

    String DEFAULT_MIME_TYPE = "application/octet-stream";

    /**
     * 
     * @param term
     * @param limit
     * @return metadata of files like the specified file term
     */
    public List<Fileupload> search(Fileupload term, Integer limit);

    /**
     *
     * @param names
     * @return metadata of requested names, or everything for null
     */
    List<Fileupload> getFileMetadata(List<String> names);
}
