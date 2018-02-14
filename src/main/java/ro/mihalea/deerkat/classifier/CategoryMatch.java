package ro.mihalea.deerkat.classifier;

import lombok.*;
import ro.mihalea.deerkat.model.Category;

@AllArgsConstructor
@Getter
@ToString
/**
 * Represent the relationship between a category and its likeliness of being accuratez
 */
public class CategoryMatch {
    private Category category;
    private Integer confidence;
}
