package libWebsiteTools;

import java.io.Serializable;
import java.util.List;

/**
 *
 * @author alpha
 * @param <Entity>
 */
public interface SearchableRepository<Entity extends Serializable> extends Repository<Entity> {

    public static boolean isSearchable(java.lang.Object instance) {
        return SearchableRepository.class.isInstance(instance);
    }

    /**
     * Perform any of re-index operation necessary for search.
     */
    public default void refreshSearch() {
    }

    /**
     * Search for entities like the given entity.
     *
     * @param term
     * @param limit will return this many (or everything if null). can be
     * ignored, but must be noted if so.
     * @return matching entities
     */
    public List<Entity> search(Entity term, Integer limit);

    /**
     * Search entities on the given term.
     *
     * @param term
     * @param limit will return this many (or everything if null). can be
     * ignored, but must be noted if so.
     * @return matching entities
     */
    public List<Entity> search(Object term, Integer limit);
}
