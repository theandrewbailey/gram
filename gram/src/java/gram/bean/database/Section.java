package gram.bean.database;

import java.io.Serializable;
import jakarta.persistence.Basic;
import jakarta.persistence.Cacheable;
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
 * TODO: rename Section to Category
 *
 * @author alpha
 */
@Entity
@Cacheable(true)
@Table(name = "section", schema = "gram")
@NamedQueries({
    @NamedQuery(name = "Category.findAll", query = "SELECT s FROM Section s"),
    @NamedQuery(name = "Category.findByName", query = "SELECT s FROM Section s WHERE s.name = :name"),
    @NamedQuery(name = "Category.count", query = "SELECT COUNT(s) FROM Section s"),
    @NamedQuery(name = "Category.byArticlesPosted", query = "SELECT a.sectionid, min(a.posted), COUNT(a.sectionid.sectionid) FROM Article a GROUP BY a.sectionid ORDER BY min(a.posted)")})
@SuppressWarnings("ValidPrimaryTableName")
public class Section implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "sectionid", nullable = false)
    private Integer sectionid;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 250)
    @Column(name = "name", nullable = false, length = 250)
    private String name;
    @Basic(optional = false)
    @NotNull
    @Convert(converter = UUIDConverter.class)
    @Column(name = "uuid", nullable = false, columnDefinition = "uuid")
    private UUID uuid;

    public Section() {
        uuid = UUID.randomUUID();
    }

    public Section(Integer categoryid) {
        this.sectionid = categoryid;
        uuid = UUID.randomUUID();
    }

    public Section(String name) {
        this.name = name;
        uuid = UUID.randomUUID();
    }

    public Section(Integer categoryid, String name, UUID uuid) {
        this.sectionid = categoryid;
        this.name = name;
        this.uuid = uuid;
    }

    public Integer getSectionid() {
        return sectionid;
    }

    public void setSectionid(Integer sectionid) {
        this.sectionid = sectionid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

//    public Collection<Article> getArticleCollection() {
//        return articleCollection;
//    }
//    public void setArticleCollection(Collection<Article> articleCollection) {
//        this.articleCollection = articleCollection;
//    }
    @Override
    public int hashCode() {
        int hash = 0;
        hash += (sectionid != null ? sectionid.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Section)) {
            return false;
        }
        Section other = (Section) object;
        return !((this.sectionid == null && other.sectionid != null) || (this.sectionid != null && !this.sectionid.equals(other.sectionid)));
    }

    @Override
    public String toString() {
        return "gram.bean.database.Section[ sectionid=" + sectionid + " ]";
    }

}
