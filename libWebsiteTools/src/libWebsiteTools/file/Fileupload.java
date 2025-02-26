package libWebsiteTools.file;

import java.io.Serializable;
import java.time.OffsetDateTime;
import jakarta.persistence.Basic;
import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import libWebsiteTools.UUIDConverter;

/**
 *
 *
 * @author alpha
 */
@Entity
@Cacheable(true)
@Table(name = "fileupload", schema = "tools")
@NamedQueries({
    @NamedQuery(name = "Fileupload.findAll", query = "SELECT f FROM Fileupload f ORDER BY f.filename"),
    @NamedQuery(name = "Fileupload.count", query = "SELECT COUNT(f) FROM Fileupload f"),
    @NamedQuery(name = "Filemetadata.findAll", query = "SELECT" + Fileupload.METADATA_CONSTRUCTOR + "FROM Fileupload f ORDER BY f.filename"),
    @NamedQuery(name = "Filemetadata.findByFilenames", query = "SELECT" + Fileupload.METADATA_CONSTRUCTOR + "FROM Fileupload f WHERE f.filename in :filenames ORDER BY f.filename"),
    @NamedQuery(name = "Filemetadata.search", query = "SELECT" + Fileupload.METADATA_CONSTRUCTOR + "FROM Fileupload f WHERE f.filename like CONCAT('%',:term,'%') OR f.url like CONCAT('%',:term,'%') ORDER BY f.filename")})
public class Fileupload implements Serializable {

    public static final String METADATA_CONSTRUCTOR = " new libWebsiteTools.file.Fileupload(f.filename,f.atime,f.etag,f.mimetype,f.url,f.datasize,f.gzipsize,f.brsize,f.zstdsize,f.uuid) ";
    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 1000)
    @Column(name = "filename", nullable = false, length = 1000)
    private String filename;
    @Basic(optional = false)
    @NotNull
    @Column(name = "atime", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime atime;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 250)
    @Column(name = "etag", nullable = false, length = 250)
    private String etag;
    @Basic(optional = false)
    @NotNull
    @Lob
    @Column(name = "filedata", nullable = false)
    private byte[] filedata;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 100)
    @Column(name = "mimetype", nullable = false, length = 100)
    private String mimetype;
    @Size(max = 65000)
    @Column(name = "url", length = 65000)
    private String url;
    @Basic(optional = false)
    @NotNull
    @Convert(converter = UUIDConverter.class)
    @Column(name = "uuid", nullable = false, columnDefinition = "uuid")
    private UUID uuid;
    @Basic(optional = false)
    @Lob
    @Column(name = "gzipdata")
    private byte[] gzipdata;
    @Basic(optional = false)
    @Lob
    @Column(name = "brdata")
    private byte[] brdata;
    @Basic(optional = false)
    @Lob
    @Column(name = "zstddata")
    private byte[] zstddata;
    @Column(name = "datasize", insertable = false, updatable = false)
    private Integer datasize;
    @Column(name = "gzipsize", insertable = false, updatable = false)
    private Integer gzipsize;
    @Column(name = "brsize", insertable = false, updatable = false)
    private Integer brsize;
    @Column(name = "zstdsize", insertable = false, updatable = false)
    private Integer zstdsize;

    public Fileupload() {
        uuid = UUID.randomUUID();
    }

    public Fileupload(String filename) {
        this.filename = filename;
        uuid = UUID.randomUUID();
    }

    public Fileupload(String filename, OffsetDateTime atime) {
        this.filename = filename;
        this.atime = atime;
        uuid = UUID.randomUUID();
    }

    public Fileupload(String filename, OffsetDateTime atime, String etag, String mimetype, String url, Integer datasize, Integer gzipsize, Integer brsize, Integer zstdsize, UUID uuid) {
        this.filename = filename;
        this.atime = atime;
        this.etag = etag;
        this.mimetype = mimetype;
        this.url = url;
        this.datasize = datasize;
        this.gzipsize = gzipsize;
        this.brsize = brsize;
        this.zstdsize = zstdsize;
        this.uuid = uuid;
    }

    public OffsetDateTime getAtime() {
        return atime;
    }

    public void setAtime(OffsetDateTime atime) {
        this.atime = atime;
    }

    public String getEtag() {
        return etag;
    }

    public void setEtag(String etag) {
        this.etag = etag;
    }

    public byte[] getFiledata() {
        return filedata;
    }

    public void setFiledata(byte[] filedata) {
        this.filedata = filedata;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getMimetype() {
        return mimetype;
    }

    public void setMimetype(String mimetype) {
        this.mimetype = mimetype;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    @Override
    public int hashCode() {
        return filename.hashCode();
    }

    @Override
    public boolean equals(Object object) {
        // Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Fileupload)) {
            return false;
        }
        Fileupload other = (Fileupload) object;
        return !((this.filename == null && other.filename != null) || (this.filename != null && !this.filename.equals(other.filename)));
    }

    @Override
    public String toString() {
        return "libWebsiteTools.file.Fileupload[ filename=" + filename + " ]";
    }

    public byte[] getGzipdata() {
        return gzipdata;
    }

    public void setGzipdata(byte[] gzipdata) {
        this.gzipdata = gzipdata;
    }

    public byte[] getBrdata() {
        return brdata;
    }

    public void setBrdata(byte[] brdata) {
        this.brdata = brdata;
    }

    public byte[] getZstddata() {
        return zstddata;
    }

    public void setZstddata(byte[] zstddata) {
        this.zstddata = zstddata;
    }

    public Integer getDatasize() {
        return datasize;
    }

    public Integer getGzipsize() {
        return gzipsize;
    }

    public Integer getBrsize() {
        return brsize;
    }

    public Integer getZstdsize() {
        return zstdsize;
    }
}
