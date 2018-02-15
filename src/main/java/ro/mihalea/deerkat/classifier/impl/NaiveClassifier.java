package ro.mihalea.deerkat.classifier.impl;

import de.daslaboratorium.machinelearning.classifier.Classification;
import de.daslaboratorium.machinelearning.classifier.Classifier;
import de.daslaboratorium.machinelearning.classifier.bayes.BayesClassifier;
import ro.mihalea.deerkat.classifier.AbstractClassifier;
import ro.mihalea.deerkat.classifier.CategoryMatch;
import ro.mihalea.deerkat.model.Category;
import ro.mihalea.deerkat.model.Transaction;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Classifier that uses machine learning to sort transactions based on bayes' theorem
 *
 * THIS CURRENT IMPLEMENTATION IS A FLOP, CORRECTLY MATCHING LESS THAN 1% OF TRANSACTIONS
 */
public class NaiveClassifier extends AbstractClassifier {
    private Classifier<String, Category> bayes = new BayesClassifier<>();

    public NaiveClassifier() {
        // Remember the last 50k transactions
        bayes.setMemoryCapacity(50000);
    }

    @Override
    public List<CategoryMatch> classify(Transaction item) {
        // Get a list of all matches identified
        List<CategoryMatch> list = new ArrayList<>();
        Collection<Classification<String, Category>> classifications =
                ((BayesClassifier<String, Category>)bayes).classifyDetailed(formatTransaction(item));

        // Transform them a Classification to a CategoryMatch, moving from a floating point to an integer confidence value
        for(Classification<String, Category> c : classifications) {
            list.add(new CategoryMatch(c.getCategory(), (int) (c.getProbability() * 100)));
        }

        //Ignore transactions below 35%
        return list.stream()
                .filter(cm -> cm.getConfidence() > 35)
                .sorted(Comparator.comparingInt(CategoryMatch::getConfidence).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public void learn(Transaction transaction) {
        bayes.learn(transaction.getCategory(), formatTransaction(transaction));
    }

    private List<String> formatTransaction(Transaction transaction) {
        String sanitised = this.sanitiseDetails(transaction.getDetails());
        return Arrays.asList(sanitised.split("\\s"));
    }

    @Override
    public void reset() {
        bayes.reset();
    }
}
