package ro.mihalea.deerkat.classifier;

import javafx.util.Pair;
import lombok.extern.log4j.Log4j2;
import me.xdrop.fuzzywuzzy.FuzzySearch;
import me.xdrop.fuzzywuzzy.model.ExtractedResult;
import ro.mihalea.deerkat.model.Category;
import ro.mihalea.deerkat.model.Transaction;

import java.util.*;
import java.util.stream.Collectors;

@Log4j2
public class FuzzyClassifier extends AbstractClassifier{
    @Override
    public List<CategoryMatch> getMatches(Transaction item) {
        // Extract the details from the model data and sanitize the strings to remove noise
        List<String> detailsList = modelData.stream()
                .map(Transaction::getDetails)
                .map(this::sanitizeTitle)
                .collect(Collectors.toList());


        // Match the current category with previous ones
        List<ExtractedResult> results = FuzzySearch.extractAll(
                this.sanitizeTitle(item.getDetails()),
                detailsList
        );

        // Construct a list of category matches based on model data and the scores of the fuzzy search
        List<CategoryMatch> matches = new ArrayList<>();
        for (int i = 0; i < results.size(); i++) {
            matches.add(new CategoryMatch(modelData.get(i).getCategory(), results.get(i).getScore()));
        }

        // Remove any matches that have their score below the cutoff values as they don't help
        List<CategoryMatch> filtered = matches.stream()
                .filter(e -> e.getSimilarity() > CUTOFF_VALUE)
                .collect(Collectors.toList());

        // Map containing the Category and a pair of integers containing the total score for that category and the number
        // of categories found in that category
        Map<Category, Pair<Integer, Integer>> summed = new LinkedHashMap<>();

        // Sum up all the matches and prepare to calculate the averages
        for (CategoryMatch match : filtered) {
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

        // Calculate the averages based on the sums
        Map <Category, Integer> averaged = new LinkedHashMap<>();
        for(Map.Entry<Category, Pair<Integer, Integer>> entry : summed.entrySet()) {
            Pair<Integer, Integer> count = entry.getValue();
            averaged.put(entry.getKey(), count.getKey() / count.getValue());
        }

        // Prepare a list of CategoryMatches for sorting
        List<CategoryMatch> sortedAveraged = new ArrayList<>();
        for(Map.Entry<Category, Integer> entry : averaged.entrySet()) {
            sortedAveraged.add(new CategoryMatch(entry.getKey(), entry.getValue()));
        }

        // Sort the list based on the similarity score
        sortedAveraged = sortedAveraged.stream()
                .sorted(Comparator.comparingInt(CategoryMatch::getSimilarity))
                .collect(Collectors.toList());

        log.debug("Found {} possible categories for {}", sortedAveraged.size(), item);

        return sortedAveraged;
    }
}
