package libWebsiteTools.imead;

import java.io.Serializable;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 *
 * @author alpha
 */
@Embeddable
public class LocalizationPK implements Serializable {

    @Basic
    @Column(name = "key", nullable = false, length = 1000)
    private String key;
    @Basic
    @Column(name = "localecode")
    private String localecode;

    public LocalizationPK() {
    }

    public LocalizationPK(String key, String localecode) {
        this.key = key;
        this.localecode = localecode;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getLocalecode() {
        return localecode;
    }

    public void setLocalecode(String localecode) {
        this.localecode = localecode;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (key != null ? key.hashCode() : 0);
        hash += (localecode != null ? localecode.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof LocalizationPK)) {
            return false;
        }
        LocalizationPK other = (LocalizationPK) object;
        if ((this.key == null && other.key != null) || (this.key != null && !this.key.equals(other.key))) {
            return false;
        }
        if ((this.localecode == null && other.localecode != null) || (this.localecode != null && !this.localecode.equals(other.localecode))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "libWebsiteTools.imead.LocalizationPK[ key=" + key + ", localecode=" + localecode + " ]";
    }
    
}
