package ro.mihalea.deerkat.repository;

import ro.mihalea.deerkat.exception.repository.*;
import ro.mihalea.deerkat.model.Transaction;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Basic repository interface used for handling objects
 * @param <ItemType> Type of the data stored in the repository
 */
public interface IRepository<ItemType> {
    /**
     * Add a new object to the database
     * @param itemType Object to be added to the database
     */
    Optional<Long> add(ItemType itemType) throws RepositoryCreateException;

    /**
     * Add multiple object to the database
     * @param list List of objects to be added to the database
     */
    default List<Optional<Long>> addAll(Iterable<ItemType> list) throws RepositoryCreateException {
        List<Optional<Long>> keys = new ArrayList<>();
        for(ItemType item : list) {
            Optional<Long> key = this.add(item);
            keys.add(key);
        }

        return keys;
    }

    /**
     * Get all the objects found in the database
     * @return List of all objects stored
     */
    List<ItemType> getAll() throws RepositoryReadException, UnimplementedMethodException;

    /**
     * Update a transaction in the database
     * @param item Transaction that needs to be updated
     */
    void update(ItemType item) throws RepositoryUpdateException, UnimplementedMethodException;

    /**
     * Method used for deleting all items from the database
     */
    void nuke() throws RepositoryDeleteException;
}
