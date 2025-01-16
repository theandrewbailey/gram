package libWebsiteTools.imead;

import java.io.Serializable;
import jakarta.persistence.Basic;
import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import libWebsiteTools.UUIDConverter;

/**
 *
 * @author alpha
 */
@Entity
@Cacheable(true)
@Table(name = "localization", schema = "tools")
@NamedQueries({
    @NamedQuery(name = "Localization.findAll", query = "SELECT l FROM Localization l ORDER BY l.localizationPK.localecode ASC, l.localizationPK.key ASC"),
    @NamedQuery(name = "Localization.findByLocalecode", query = "SELECT l FROM Localization l WHERE l.localizationPK.localecode = :localecode ORDER BY l.localizationPK.key ASC"),
    @NamedQuery(name = "Localization.getDistinctLocales", query = "SELECT DISTINCT l.localizationPK.localecode FROM Localization l ORDER BY l.localizationPK.localecode ASC"),
    @NamedQuery(name = "Localization.count", query = "SELECT COUNT(l) FROM Localization l")})
public class Localization implements Serializable {

    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected LocalizationPK localizationPK;
    @Basic
    @Column(name = "value", length = 65000)
    private String value;
    @Basic(optional = false)
    @NotNull
    @Convert(converter = UUIDConverter.class)
    @Column(name = "uuid", nullable = false, columnDefinition = "uuid")
    private UUID uuid;

    public Localization() {
        uuid = UUID.randomUUID();
    }

    public Localization(String localecode, String key, String value) {
        this.localizationPK = new LocalizationPK(key, localecode);
        this.value = value;
        uuid = UUID.randomUUID();
    }

    public LocalizationPK getLocalizationPK() {
        return localizationPK;
    }

    public void setLocalizationPK(LocalizationPK localizationPK) {
        this.localizationPK = localizationPK;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
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
        hash += (localizationPK != null ? localizationPK.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Localization)) {
            return false;
        }
        Localization other = (Localization) object;
        if ((this.localizationPK == null && other.localizationPK != null) || (this.localizationPK != null && !this.localizationPK.equals(other.localizationPK))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "libWebsiteTools.imead.Localization[ localizationPK=" + localizationPK + " ]";
    }

}
