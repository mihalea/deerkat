package ro.mihalea.deerkat.classifier;

import javafx.util.Pair;
import me.xdrop.fuzzywuzzy.FuzzySearch;
import me.xdrop.fuzzywuzzy.model.ExtractedResult;
import ro.mihalea.deerkat.model.Category;
import ro.mihalea.deerkat.model.Transaction;

import java.util.*;
import java.util.stream.Collectors;

public class FuzzyClassifier extends AbstractClassifier{
    @Override
    public List<CategoryMatch> getMatches(Transaction item) {
        List<String> detailsList = modelData.stream()
                .map(Transaction::getDetails)
                .map(this::sanitizeTitle)
                .collect(Collectors.toList());


        List<ExtractedResult> results = FuzzySearch.extractAll(
                this.sanitizeTitle(item.getDetails()),
                detailsList
        );

        List<CategoryMatch> matches = new ArrayList<>();
        for (int i = 0; i < results.size(); i++) {
            matches.add(new CategoryMatch(modelData.get(i).getCategory(), results.get(i).getScore()));
        }

        List<CategoryMatch> sorted = matches.stream()
                .filter(e -> e.getSimilarity() > CUTOFF_VALUE)
                .collect(Collectors.toList());

        Map<Category, Pair<Integer, Integer>> summed = new LinkedHashMap<>();

        for (CategoryMatch match : sorted) {
            if(summed.containsKey(match.getCategory())) {
                Pair<Integer, Integer> count = summed.get(match.getCategory());
                summed.put(
                        match.getCategory(),
                        new Pair<>(
                                count.getKey() + match.getSimilarity(),
                                count.getValue() + 1
                        )
                );
            } else {
                summed.put(
                        match.getCategory(),
                        new Pair<>(
                                match.getSimilarity(),
                                1
                        ));
            }
        }

        Map <Category, Integer> averaged = new LinkedHashMap<>();

        for(Map.Entry<Category, Pair<Integer, Integer>> entry : summed.entrySet()) {
            Pair<Integer, Integer> count = entry.getValue();
            averaged.put(entry.getKey(), count.getKey() / count.getValue());
        }

        List<CategoryMatch> sortedAveraged = new ArrayList<>();
        for(Map.Entry<Category, Integer> entry : averaged.entrySet()) {
            sortedAveraged.add(new CategoryMatch(entry.getKey(), entry.getValue()));
        }

        sortedAveraged = sortedAveraged.stream()
                .sorted(Comparator.comparingInt(CategoryMatch::getSimilarity))
                .collect(Collectors.toList());

        return sortedAveraged;
    }
}
