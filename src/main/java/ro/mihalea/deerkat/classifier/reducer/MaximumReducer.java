package ro.mihalea.deerkat.classifier.reducer;

import javafx.util.Pair;
import ro.mihalea.deerkat.classifier.CategoryMatch;
import ro.mihalea.deerkat.model.Category;

import java.util.*;
import java.util.stream.Collectors;

public class MaximumReducer implements ReducerInterface {
    @Override
    public List<CategoryMatch> reduce(List<CategoryMatch> matches) {
        List<CategoryMatch> grouped = matches.stream()
                // Group all CategoryMatches by Category into a map
                .collect(Collectors.groupingBy(CategoryMatch::getCategory))
                // Get the entries of that map
                .entrySet()
                .stream()
                // For every map, get the match with the maximum confidence
                .map(m -> m.getValue().stream().max(Comparator.comparingInt(CategoryMatch::getConfidence)))
                // Make sure all optionals return values
                .filter(Optional::isPresent)
                // Extract the values from them
                .map(Optional::get)
                // Convert the stream to a list
                .collect(Collectors.toList());

        return this.sort(grouped);
    }
}
