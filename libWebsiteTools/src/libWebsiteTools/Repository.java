package libWebsiteTools;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

/**
 * This object defines CRUD (and other) operations over a data Entity. Intended
 * to be used with databases, but not necessarily.
 *
 * @author alpha
 * @param <Entity> the entities that this repository operates on
 */
public interface Repository<Entity> {

    /**
     * Create (or update) the given entities.
     *
     * @param entities
     * @return the newly stored entities
     */
    public Collection<Entity> upsert(Collection<Entity> entities);

    /**
     * Retrieve a single entity.
     *
     * @param id the primary key of the desired entity.
     * @return the desired entity, or null.
     */
    public Entity get(Object id);

    /**
     * Get everything stored, up to limit.
     *
     * @param limit will return this many (or everything if null).
     * @return all stored entities, up to limit.
     */
    public List<Entity> getAll(Integer limit);

    /**
     * Search entities on the given term.
     *
     * @param term
     * @param limit will return this many (or everything if null). can be
     * ignored, but must be noted if so.
     * @return matching entities
     */
    public List<Entity> search(Object term, Integer limit);

    /**
     * Perform any of re-index operation necessary for search.
     */
    public default void refreshSearch() {
    }

    /**
     * Delete a single entity.
     *
     * @param id the primary key of the desired entity.
     * @return the deleted entity, or null.
     */
    public Entity delete(Object id);

    /**
     * Execute an operation on every stored entity. Depending on implementation,
     * may be processed in parallel and in no specific order.
     *
     * @param operation function to perform on all entities.
     * @param transaction should these operations be performed in a transaction?
     */
    public void processArchive(Consumer<Entity> operation, Boolean transaction);

    /**
     * Something big happened, so clear all caches.
     *
     * @return this
     */
    public Repository<Entity> evict();

    /**
     * How many things are stored?
     *
     * @param term count by this search term or group. can be ignored, but must
     * be noted if so.
     * @return count of how many entities are stored.
     */
    public Long count(Object term);
}
