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
 * @author alpha
 */
@Entity
@Cacheable(true)
@Table(name = "category", schema = "gram")
@NamedQueries({
    @NamedQuery(name = "Category.findAll", query = "SELECT c FROM Category c"),
    @NamedQuery(name = "Category.findByName", query = "SELECT c FROM Category c WHERE c.name = :name"),
    @NamedQuery(name = "Category.count", query = "SELECT COUNT(c) FROM Category c"),
    @NamedQuery(name = "Category.byArticlesPosted", query = "SELECT a.categoryid, min(a.posted), COUNT(a.categoryid.categoryid) FROM Article a GROUP BY a.categoryid ORDER BY min(a.posted)")})
public class Category implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "categoryid", nullable = false)
    private Integer categoryid;
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
//    private String url;

    public Category() {
        uuid = UUID.randomUUID();
    }

    public Category(Integer categoryid) {
        this.categoryid = categoryid;
        uuid = UUID.randomUUID();
    }

    public Category(String name) {
        this.name = name;
        uuid = UUID.randomUUID();
    }

    public Category(Integer categoryid, String name, UUID uuid) {
        this.categoryid = categoryid;
        this.name = name;
        this.uuid = uuid;
    }

    public Integer getCategoryid() {
        return categoryid;
    }

    public void setCategoryid(Integer categoryid) {
        this.categoryid = categoryid;
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
        hash += (categoryid != null ? categoryid.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Category)) {
            return false;
        }
        Category other = (Category) object;
        return !((this.categoryid == null && other.categoryid != null) || (this.categoryid != null && !this.categoryid.equals(other.categoryid)));
    }

    @Override
    public String toString() {
        return "gram.bean.database.Category[ categoryid=" + categoryid + " ]";
    }

//    public String getUrl() {
//        return url;
//    }
//    public void setUrl(String url) {
//        this.url = url;
//    }
}
