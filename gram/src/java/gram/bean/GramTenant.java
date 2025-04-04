package gram.bean;

import gram.bean.database.ArticleRepository;
import jakarta.ejb.Local;
import libWebsiteTools.Repository;
import gram.bean.database.Comment;
import gram.bean.database.GramIMEADRepository;
import gram.bean.database.Section;
import libWebsiteTools.Tenant;

/**
 * Provides access to all blog data for a single tenant.
 *
 * @author alpha
 */
@Local
public interface GramTenant extends Tenant {

    @Override
    public GramIMEADRepository getImead();

    public ArticleRepository getArts();

    public Repository<Comment> getComms();

    public Repository<Section> getCategories();
}
