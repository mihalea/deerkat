package ro.mihalea.deerkat.classifier;

import ro.mihalea.deerkat.model.Category;
import ro.mihalea.deerkat.model.Transaction;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Classifier using the weighted average of the {@link FuzzyClassifier} and {@link NaiveClassifier}
 */
public class CombinedClassifier extends AbstractClassifier {
    /**
     * Fuzzy classifier used as a main classifier
     */
    FuzzyClassifier fuzzy = new FuzzyClassifier();

    /**
     * Classifier using machine learning to break ties
     */
    NaiveClassifier naive = new NaiveClassifier();

    @Override
    public void learn(Transaction transaction) {
        fuzzy.learn(transaction);
        naive.learn(transaction);
    }

    @Override
    public List<CategoryMatch> classify(Transaction item) {
        // Classify the transaction with both classifiers
        List<CategoryMatch> fuzzyMatches = fuzzy.classify(item);
        List<CategoryMatch> naiveMatches = naive.classify(item);

        // Build lists of all categories returned so they can be joined into a set
        List<Category> fuzzyCategories = fuzzyMatches.stream().map(CategoryMatch::getCategory).collect(Collectors.toList());
        List<Category> naiveCategories = naiveMatches.stream().map(CategoryMatch::getCategory).collect(Collectors.toList());

        // Set containing all possible categories
        Set<Category> uniqueCategories = new HashSet<>();
        uniqueCategories.addAll(fuzzyCategories);
        uniqueCategories.addAll(naiveCategories);

        List<CategoryMatch> matches = new ArrayList<>();
        for(Category category : uniqueCategories) {
            int confidence = 0;

            // If this classifier has found a match, count it with a weight of 75%
            if(fuzzyCategories.contains(category)) {
                Optional<CategoryMatch> opt = fuzzyMatches.stream()
                        .filter(fm -> fm.getCategory().equals(category))
                        .findFirst();
                if(opt.isPresent()) {
                    confidence += 0.75 * opt.get().getConfidence();
                }
            }

            // If this classifier has found a match, count it with a weight of 25%
            if(naiveCategories.contains(category)) {
                Optional<CategoryMatch> opt = naiveMatches.stream()
                        .filter(nm -> nm.getCategory().equals(category))
                        .findFirst();
                if(opt.isPresent()) {
                    confidence += 0.25 * opt.get().getConfidence();
                }
            }

            matches.add(new CategoryMatch(category, confidence));
        }

        // Ignore matches that are below 30% confidence and sort them based on their confidence level
        return matches.stream()
                .filter(cm -> cm.getConfidence() > 30)
                .sorted(Comparator.comparingInt(CategoryMatch::getConfidence))
                .collect(Collectors.toList());
    }

    @Override
    public void reset() {
        fuzzy.reset();
        naive.reset();
    }
}
