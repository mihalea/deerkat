package ro.mihalea.deerkat.classifier.reducer;

import javafx.util.Pair;
import ro.mihalea.deerkat.classifier.CategoryMatch;
import ro.mihalea.deerkat.model.Category;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AverageReducer implements ReducerInterface {
    @Override
    public List<CategoryMatch> reduce(List<CategoryMatch> matches) {
        Map<Category, Pair<Integer, Integer>> map = this.map(matches);

        // Calculate the averages based on the sums
        Map <Category, Integer> averaged = new HashMap<>();
        for(Map.Entry<Category, Pair<Integer, Integer>> entry : map.entrySet()) {
            Pair<Integer, Integer> count = entry.getValue();
            averaged.put(entry.getKey(), count.getKey() / count.getValue() + (count.getValue() - 1));
        }

        // Prepare a list of CategoryMatches for sorting, creating new CategoryMatches from map entries
        List<CategoryMatch> list = averaged.entrySet().stream()
                .map(e -> new CategoryMatch(e.getKey(), e.getValue()))
                .collect(Collectors.toList());

        return this.sort(list);
    }
}
