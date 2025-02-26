package gram.bean.database;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Collection;
import jakarta.persistence.Basic;
import jakarta.persistence.Cacheable;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
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
@Cacheable(true)
@Table(name = "article", schema = "gram")
@NamedQueries({
    @NamedQuery(name = "Article.findAll", query = "SELECT a FROM Article a WHERE a.articleid NOT IN :exclude ORDER BY a.posted DESC"),
    @NamedQuery(name = "Article.findByCategory", query = "SELECT a FROM Article a WHERE a.sectionid.name=:category AND a.articleid NOT IN :exclude ORDER BY a.posted DESC"),
    @NamedQuery(name = "Article.count", query = "SELECT COUNT(a) FROM Article a"),
    @NamedQuery(name = "Article.countByCategory", query = "SELECT COUNT(a) FROM Article a WHERE a.sectionid.name=:category")})
public class Article implements Serializable, Comparable<Article> {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "articleid", nullable = false)
    private Integer articleid;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 250)
    @Column(name = "articletitle", nullable = false, length = 250)
    private String articletitle;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 250)
    @Column(name = "etag", nullable = false, length = 250)
    private String etag;
    @Basic(optional = false)
    @NotNull
    @Column(name = "modified", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime modified;
    @Basic(optional = false)
    @NotNull
    @Column(name = "posted", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime posted;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 10000000)
    @Column(name = "postedhtml", nullable = false, length = 10000000)
    private String postedhtml;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 10000000)
    @Column(name = "postedmarkdown", nullable = false, length = 10000000)
    private String postedmarkdown;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 250)
    @Column(name = "postedname", nullable = false, length = 250)
    private String postedname;
    @Basic(optional = false)
    @NotNull
    @Convert(converter = UUIDConverter.class)
    @Column(name = "uuid", nullable = false, columnDefinition = "uuid")
    private UUID uuid;
    @Column(name = "comments")
    private Boolean comments;
    @Size(max = 1000)
    @Column(name = "description", length = 1000)
    private String description;
    @Size(max = 1000)
    @Column(name = "imageurl", length = 1000)
    private String imageurl;
    @Size(max = 65000)
    @Column(name = "summary", length = 65000)
    private String summary;
    @Size(max = 1000)
    @Column(name = "url", length = 1000)
    private String url;
    @Size(max = 250)
    @Column(name = "suggestion", length = 250)
    private String suggestion;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "articleid")
    @OrderBy("posted ASC")
    private Collection<Comment> commentCollection;
    @JoinColumn(name = "sectionid", referencedColumnName = "sectionid", nullable = true)
    @ManyToOne(optional = true)
    private Section sectionid;

    public Article() {
    }

    public Article(Integer articleid) {
        this.articleid = articleid;
    }

    public Article(UUID uuid) {
        this.uuid = uuid;
    }

    public Article(Integer articleid, String articletitle, String etag, OffsetDateTime posted, OffsetDateTime modified, String summary, String imageurl, UUID uuid) {
        this.articleid = articleid;
        this.articletitle = articletitle;
        this.etag = etag;
        this.posted = posted;
        this.modified = modified;
        this.summary = summary;
        this.imageurl = imageurl;
        this.uuid = uuid;
    }

    public Integer getArticleid() {
        return articleid;
    }

    public void setArticleid(Integer articleid) {
        this.articleid = articleid;
    }

    public String getArticletitle() {
        return articletitle;
    }

    public void setArticletitle(String articletitle) {
        this.articletitle = articletitle;
    }

    public String getEtag() {
        return etag;
    }

    public void setEtag(String etag) {
        this.etag = etag;
    }

    public OffsetDateTime getModified() {
        return modified;
    }

    public void setModified(OffsetDateTime modified) {
        this.modified = modified;
    }

    public OffsetDateTime getPosted() {
        return posted;
    }

    public void setPosted(OffsetDateTime posted) {
        this.posted = posted;
    }

    public String getPostedhtml() {
        return postedhtml;
    }

    public void setPostedhtml(String postedhtml) {
        this.postedhtml = postedhtml;
    }

    public String getPostedmarkdown() {
        return postedmarkdown;
    }

    public void setPostedmarkdown(String postedmarkdown) {
        this.postedmarkdown = postedmarkdown;
    }

    public String getPostedname() {
        return postedname;
    }

    public void setPostedname(String postedname) {
        this.postedname = postedname;
    }

    public Boolean getComments() {
        return comments;
    }

    public void setComments(Boolean comments) {
        this.comments = comments;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageurl() {
        return imageurl;
    }

    public void setImageurl(String imageurl) {
        this.imageurl = imageurl;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
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

    public Collection<Comment> getCommentCollection() {
        return commentCollection;
    }

    public void setCommentCollection(Collection<Comment> commentCollection) {
        this.commentCollection = commentCollection;
    }

    public Section getSectionid() {
        return sectionid;
    }

    public void setSectionid(Section sectionid) {
        this.sectionid = sectionid;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public Article setSuggestion(String suggestion) {
        this.suggestion = suggestion;
        return this;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (articleid != null ? articleid.hashCode() : 0);
        hash += (etag != null ? etag.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Article)) {
            return false;
        }
        Article other = (Article) object;
        return !((this.articleid == null && other.articleid != null) || (this.articleid != null && !this.articleid.equals(other.articleid)) || (this.etag != null && !this.etag.equals(other.etag)));
    }

    @Override
    public String toString() {
        return "gram.bean.database.Article, id:" + articleid + ", title: " + articletitle;
    }

    @Override
    public int compareTo(Article other) {
        if (null == other.getArticleid() && null == this.getArticleid()) {
            return 0;
        } else if (null != this.getArticleid() && null == other.getArticleid()) {
            return -1;
        } else if (null == this.getArticleid() && null != other.getArticleid()) {
            return 1;
        }
        return this.getArticleid() - other.getArticleid();
    }
}
