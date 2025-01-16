package libWebsiteTools.security;

import java.io.Serializable;
import java.time.OffsetDateTime;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import libWebsiteTools.UUIDConverter;

/**
 *
 * @author alpha
 */
@Entity
@Table(name = "exceptionevent", schema = "tools")
@NamedQueries({
    @NamedQuery(name = "Exceptionevent.findAll", query = "SELECT e FROM Exceptionevent e ORDER BY e.atime DESC"),
    @NamedQuery(name = "Exceptionevent.findByExceptioneventid", query = "SELECT e FROM Exceptionevent e WHERE e.exceptioneventid = :exceptioneventid"),
    @NamedQuery(name = "Exceptionevent.clean", query = "DELETE FROM Exceptionevent e WHERE e.atime < :past")})
public class Exceptionevent implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "exceptioneventid", nullable = false)
    private Integer exceptioneventid;
    @Basic(optional = false)
    @NotNull
    @Column(name = "atime", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime atime;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 10000000)
    @Column(name = "description", nullable = false, length = 10000000)
    private String description;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 65000)
    @Column(name = "title", nullable = false, length = 65000)
    private String title;
    @Basic(optional = false)
    @NotNull
    @Convert(converter = UUIDConverter.class)
    @Column(name = "uuid", nullable = false, columnDefinition = "uuid")
    private UUID uuid;

    public Exceptionevent() {
    }

    public Exceptionevent(Integer exceptioneventid) {
        this.exceptioneventid = exceptioneventid;
    }

    public Exceptionevent(Integer exceptioneventid, OffsetDateTime atime, String description, String title, UUID uuid) {
        this.exceptioneventid = exceptioneventid;
        this.atime = atime;
        this.description = description;
        this.title = title;
        this.uuid = uuid;
    }

    public Integer getExceptioneventid() {
        return exceptioneventid;
    }

    public void setExceptioneventid(Integer exceptioneventid) {
        this.exceptioneventid = exceptioneventid;
    }

    public OffsetDateTime getAtime() {
        return atime;
    }

    public void setAtime(OffsetDateTime atime) {
        this.atime = atime;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (exceptioneventid != null ? exceptioneventid.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Exceptionevent)) {
            return false;
        }
        Exceptionevent other = (Exceptionevent) object;
        return !((this.exceptioneventid == null && other.exceptioneventid != null) || (this.exceptioneventid != null && !this.exceptioneventid.equals(other.exceptioneventid)));
    }

    @Override
    public String toString() {
        return "libWebsiteTools.db.Exceptionevent[ exceptioneventid=" + exceptioneventid + " ]";
    }
}
