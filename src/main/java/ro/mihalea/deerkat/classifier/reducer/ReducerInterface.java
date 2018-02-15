package ro.mihalea.deerkat.classifier.reducer;

import javafx.util.Pair;
import ro.mihalea.deerkat.classifier.CategoryMatch;
import ro.mihalea.deerkat.model.Category;

import java.util.*;
import java.util.stream.Collectors;

public interface ReducerInterface {
    /**
     * Confidence level below which matches are ignored
     */
    Integer CUT_OFF = 62;

    /**
     * Reduce a list of matches with multiple instances of the same categories to a list of unique categories
     *
     * @param matches List of matches
     * @return List of matches with unique categories
     */
    List<CategoryMatch> reduce(List<CategoryMatch> matches);

    /**
     * Count the appearances of every category and map them with their total score, ignoring those matching below the
     * cut off value
     *
     * @param matches List of matches
     * @return Map of categories and their scores
     */
    default Map<Category, Pair<Integer, Integer>> map(List<CategoryMatch> matches) {
        // Remove any matches that have their score below the cutoff values as they don't help
        List<CategoryMatch> filtered = matches.stream()
                .filter(e -> e.getConfidence() > CUT_OFF)
                .collect(Collectors.toList());

        // Map containing the Category and a pair of integers containing the total score for that category and the number
        // of categories found in that category
        Map<Category, Pair<Integer, Integer>> summed = new HashMap<>();

        // Sum up all the matches and prepare to calculate the averages
        for (CategoryMatch match : filtered) {
            if (summed.containsKey(match.getCategory())) {
                Pair<Integer, Integer> count = summed.get(match.getCategory());
                summed.put(
                        match.getCategory(),
                        new Pair<>(
                                count.getKey() + match.getConfidence(),
                                count.getValue() + 1
                        )
                );
            } else {
                summed.put(
                        match.getCategory(),
                        new Pair<>(
                                match.getConfidence(),
                                1
                        ));
            }
        }

        return summed;
    }

    /**
     * Sort the matches in increasing order based on their confidence level
     * @param matches
     * @return
     */
    default List<CategoryMatch> sort(List<CategoryMatch> matches) {
        // Sort the list based on the confidence score
        return matches.stream()
                .sorted(Comparator.comparingInt(CategoryMatch::getConfidence).reversed())
                .collect(Collectors.toList());
    }
}
