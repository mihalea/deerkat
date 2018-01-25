package ro.mihalea.deerkat.repository;

import ro.mihalea.deerkat.exception.repository.RepositoryReadException;
import ro.mihalea.deerkat.exception.repository.RepositoryCreateException;
import ro.mihalea.deerkat.exception.repository.UnimplementedMethodException;

import java.util.List;

/**
 * Basic repository interface used for handling objects
 * @param <ItemType> Type of the data stored in the repository
 * @param <KeyType> Type of the id used by the object
 */
public interface IRepository<ItemType, KeyType> {
    /**
     * Add a new object to the database
     * @param itemType Object to be added to the database
     */
    void add(ItemType itemType) throws RepositoryCreateException;

    /**
     * Add multiple object to the database
     * @param list List of objects to be added to the database
     */
    default void addAll(Iterable<ItemType> list) throws RepositoryCreateException {
        for(ItemType item : list) {
            this.add(item);
        }
    }

    /**
     * Get a single object from the database using its ID
     * @param key ID of the requested object
     * @return Object with the same ID
     */
    ItemType getById(KeyType key) throws UnimplementedMethodException;

    /**
     * Get all the objects found in the database
     * @return List of all objects stored
     */
    List<ItemType> getAll() throws RepositoryReadException, UnimplementedMethodException;
}
