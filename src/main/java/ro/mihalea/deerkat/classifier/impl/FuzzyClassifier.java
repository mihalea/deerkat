package ro.mihalea.deerkat.classifier.impl;

import javafx.util.Pair;
import lombok.extern.log4j.Log4j2;
import me.xdrop.fuzzywuzzy.FuzzySearch;
import me.xdrop.fuzzywuzzy.model.ExtractedResult;
import ro.mihalea.deerkat.classifier.AbstractClassifier;
import ro.mihalea.deerkat.classifier.CategoryMatch;
import ro.mihalea.deerkat.classifier.reducer.AverageReducer;
import ro.mihalea.deerkat.classifier.reducer.ReducerInterface;
import ro.mihalea.deerkat.model.Category;
import ro.mihalea.deerkat.model.Transaction;

import java.util.*;
import java.util.stream.Collectors;

@Log4j2
public class FuzzyClassifier extends AbstractClassifier {
    /**
     * List of model data that will be used to calculate matches
     */
    private List<Transaction> modelData = new ArrayList<>();

    /**
     * Reducer used to process a list of matches with duplicate categories into a set with unique categories
     */
    private ReducerInterface reducer;


    /**
     * Default constructor using the best reducer available based on testing
     *
     * Currently it's using the {@link AverageReducer}
     */
    public FuzzyClassifier() {
        this(new AverageReducer());
    }

    /**
     * Initialise the classifier with the specified reducer
     * @param reducer Reducer used to process fuzzy matches
     */
    public FuzzyClassifier(ReducerInterface reducer) {
        this.reducer = reducer;
    }

    @Override
    public void learn(Transaction transaction) {
        for (Transaction model : modelData) {
            // If the transaction is already in the database, update it's category
            if(model.getId().equals(transaction.getId())) {
                model.setCategory(transaction.getCategory());
                log.debug("Model item has been updated to {}", model);
                return;
            }
        }

        // No transaction with the same id found in the database, adding it now
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

        return reducer.reduce(matches);
    }

    @Override
    public void reset() {
        modelData.clear();
    }
}
