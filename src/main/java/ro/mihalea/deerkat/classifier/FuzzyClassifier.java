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
    private static final Integer CUT_OFF = 50;
    /**
     * List of model data that will be used to calculate matches
     */
    protected List<Transaction> modelData = new ArrayList<>();

    @Override
    public void learn(Transaction transaction) {
        for (Transaction model : modelData) {
            if(model.getId().equals(transaction.getId())) {
                model.setCategory(transaction.getCategory());
                log.debug("Model item has been updated to {}", model);
                return;
            }
        }

        modelData.add(transaction);
        log.debug("Model item has been added to the classifier: {}", transaction);
    }

    @Override
    public List<CategoryMatch> classify(Transaction transaction) {
        // Extract the details from the model data and sanitize the strings to remove noise
        List<String> detailsList = modelData.stream()
                .map(Transaction::getDetails)
                .map(this::sanitiseDetails)
                .collect(Collectors.toList());


        // Match the current category with previous ones
        List<ExtractedResult> results = FuzzySearch.extractAll(
                this.sanitiseDetails(transaction.getDetails()),
                detailsList
        );

        // Construct a list of category matches based on model data and the scores of the fuzzy search
        List<CategoryMatch> matches = new ArrayList<>();
        for (int i = 0; i < results.size(); i++) {
            matches.add(new CategoryMatch(modelData.get(i).getCategory(), results.get(i).getScore()));
        }

        // Remove any matches that have their score below the cutoff values as they don't help
        List<CategoryMatch> filtered = matches.stream()
                .filter(e -> e.getConfidence() > CUT_OFF)
                .collect(Collectors.toList());

        // Map containing the Category and a pair of integers containing the total score for that category and the number
        // of categories found in that category
        Map<Category, Pair<Integer, Integer>> summed = new HashMap<>();

        // Sum up all the matches and prepare to calculate the averages
        for (CategoryMatch match : filtered) {
            if(summed.containsKey(match.getCategory())) {
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

        // Calculate the averages based on the sums
        Map <Category, Integer> averaged = new HashMap<>();
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
                .sorted(Comparator.comparingInt(CategoryMatch::getConfidence).reversed())
                .collect(Collectors.toList());

        log.debug("Found {} possible categories for {}", sortedAveraged.size(), transaction);

        return sortedAveraged;
    }

    @Override
    public void reset() {
        modelData.clear();
    }
}
