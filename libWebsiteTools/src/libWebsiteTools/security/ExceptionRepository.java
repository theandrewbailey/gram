package libWebsiteTools.security;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import libWebsiteTools.Repository;

/**
 *
 * @author alpha
 */
public interface ExceptionRepository extends Repository<Exceptionevent> {

    @Override
    public Long count(Object term);

    @Override
    public Exceptionevent delete(Object id);

    @Override
    public ExceptionRepository evict();

    @Override
    public Exceptionevent get(Object id);

    @Override
    public List<Exceptionevent> getAll(Integer limit);

    @Override
    public void processArchive(Consumer<Exceptionevent> operation, Boolean transaction);

    @Override
    public List<Exceptionevent> search(Object term, Integer limit);

    @Override
    public Collection<Exceptionevent> upsert(Collection<Exceptionevent> entities);

    public CertUtil getCerts();

    public boolean inHoneypot(String ip);

    public void logException(HttpServletRequest req, String title, String desc, Throwable t);

    public boolean putInHoneypot(String ip);

}
