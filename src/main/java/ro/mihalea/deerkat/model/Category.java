package ro.mihalea.deerkat.model;

import lombok.*;


/**
 * Categories used to classify the transactions
 */
@Builder
@Data
public class Category {

    /**
     * Unique id identifying the category in the database
     */
    private int id;

    /**
     * Id of the parent category in case of nested categories
     */
    private int parentId;

    /**
     * Title of the category in user friendly format
     */
    private @NonNull String title;
}
