package ro.mihalea.deerkat.model;

import lombok.*;

import java.util.Objects;


/**
 * Categories used to classify the transactions
 */
@Builder
@Getter
@Setter
@ToString
public class Category {

    /**
     * Unique id identifying the category in the database
     */
    private Long id;

    /**
     * Id of the parent category in case of nested categories
     */
    private Long parentId;

    /**
     * Title of the category in user friendly format
     */
    private @NonNull String title;

    private @Builder.Default @NonNull Boolean hidden = false;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Category category = (Category) o;
        return id.equals(category.id);
    }

    @Override
    public int hashCode() {

        return Objects.hash(id, parentId);
    }
}
