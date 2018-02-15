package ro.mihalea.deerkat.model;

import lombok.*;
import org.apache.logging.log4j.util.Strings;

import java.util.Objects;


/**
 * Categories used to classify the transactions
 */
@Builder
@Getter
@Setter
@ToString
public class Category implements Comparable<Category>{

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

    /**
     * Compare current category with another based on their titles
     * @param o Category that it needs comparing against
     * @return Integer value signifying the relationship between the titles, using conventional values (-1, 0, 1)
     */
    @Override
    public int compareTo(Category o) {
        return this.getTitle().compareTo(o.getTitle());
    }
}
