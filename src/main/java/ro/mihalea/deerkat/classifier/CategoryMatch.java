package ro.mihalea.deerkat.classifier;

import lombok.*;
import ro.mihalea.deerkat.model.Category;

@AllArgsConstructor
@Getter
@ToString
public class CategoryMatch {
    private Category category;
    private Integer similarity;
}
