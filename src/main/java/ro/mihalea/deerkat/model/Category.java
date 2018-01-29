package ro.mihalea.deerkat.model;

import lombok.*;

import java.util.Objects;


/**
 * Categories used to classify the transactions
 */
@Builder
@Data
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Category category = (Category) o;
        return Objects.equals(id, category.id);
    }

    @Override
    public int hashCode() {

        return Objects.hash(super.hashCode(), id);
    }
}
