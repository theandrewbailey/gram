package gram.bean.database;

import java.io.Serializable;
import java.time.OffsetDateTime;
import jakarta.persistence.Basic;
import jakarta.persistence.Cacheable;
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
@Table(name = "comment", schema = "gram")
@NamedQueries({
    @NamedQuery(name = "Comment.findAll", query = "SELECT c FROM Comment c ORDER BY c.posted DESC"),
    @NamedQuery(name = "Comment.count", query = "SELECT COUNT(c) FROM Comment c")})
public class Comment implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "commentid", nullable = false)
    private Integer commentid;
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
    @Size(min = 1, max = 250)
    @Column(name = "postedname", nullable = false, length = 250)
    private String postedname;
    @Basic(optional = false)
    @NotNull
    @Convert(converter = UUIDConverter.class)
    @Column(name = "uuid", nullable = false, columnDefinition = "uuid")
    private UUID uuid;
    @Column(name = "isapproved")
    private Boolean isapproved;
    @Column(name = "isspam")
    private Boolean isspam;
    @Size(max = 10000000)
    @Column(name = "postedmarkdown", length = 10000000)
    private String postedmarkdown;
    @JoinColumn(name = "articleid", referencedColumnName = "articleid", nullable = false)
    @ManyToOne(optional = false)
    private Article articleid;

    public Comment() {
    }

    public Comment(UUID uuid) {
        this.uuid = uuid;
    }

    public Comment(Integer commentid, OffsetDateTime posted, String postedhtml, String postedname, UUID uuid) {
        this.commentid = commentid;
        this.posted = posted;
        this.postedhtml = postedhtml;
        this.postedname = postedname;
        this.uuid = uuid;
    }

    public Integer getCommentid() {
        return commentid;
    }

    public void setCommentid(Integer commentid) {
        this.commentid = commentid;
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

    public String getPostedname() {
        return postedname;
    }

    public void setPostedname(String postedname) {
        this.postedname = postedname;
    }

    public Boolean getIsapproved() {
        return isapproved;
    }

    public void setIsapproved(Boolean isapproved) {
        this.isapproved = isapproved;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public Boolean getIsspam() {
        return isspam;
    }

    public void setIsspam(Boolean isspam) {
        this.isspam = isspam;
    }

    public String getPostedmarkdown() {
        return postedmarkdown;
    }

    public void setPostedmarkdown(String postedmarkdown) {
        this.postedmarkdown = postedmarkdown;
    }

    public Article getArticleid() {
        return articleid;
    }

    public void setArticleid(Article articleid) {
        this.articleid = articleid;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (commentid != null ? commentid.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Comment)) {
            return false;
        }
        Comment other = (Comment) object;
        return !((this.commentid == null && other.commentid != null) || (this.commentid != null && !this.commentid.equals(other.commentid)));
    }

    @Override
    public String toString() {
        return "gram.bean.database.Comment[ commentid=" + commentid + " ]";
    }

}
