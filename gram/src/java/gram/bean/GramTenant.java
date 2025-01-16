package gram.bean;

import jakarta.ejb.Local;
import libWebsiteTools.Repository;
import gram.bean.database.Comment;
import gram.bean.database.Section;
import libWebsiteTools.Tenant;

/**
 * Provides access to all blog data for a single tenant.
 *
 * @author alpha
 */
@Local
public interface GramTenant extends Tenant {

    public boolean isFirstTime();

    public ArticleRepository getArts();

    public Repository<Comment> getComms();

    public Repository<Section> getSects();

    public BackupDaemon getBackup();
}
