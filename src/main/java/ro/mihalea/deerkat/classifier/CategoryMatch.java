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
    /**
     * Category that the classifier has found as a match
     */
    private Category category;

    /**
     * Confidence level as an integer from 0..100
     */
    private Integer confidence;
}
