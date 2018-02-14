package ro.mihalea.deerkat.classifier;

import de.daslaboratorium.machinelearning.classifier.Classification;
import de.daslaboratorium.machinelearning.classifier.Classifier;
import de.daslaboratorium.machinelearning.classifier.bayes.BayesClassifier;
import ro.mihalea.deerkat.model.Category;
import ro.mihalea.deerkat.model.Transaction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class NaiveClassifier extends AbstractClassifier{
    private Classifier<String, Category> bayes = new BayesClassifier<>();

    public NaiveClassifier() {
        bayes.setMemoryCapacity(50000);
    }

    @Override
    public List<CategoryMatch> classify(Transaction item) {
        List<CategoryMatch> list = new ArrayList<>();
        Collection<Classification<String, Category>> classifications =
                ((BayesClassifier<String, Category>)bayes).classifyDetailed(formatTransaction(item));

        for(Classification<String, Category> c : classifications) {
            list.add(new CategoryMatch(c.getCategory(), (int) (c.getProbability() * 100)));
        }

        return list.stream().filter(cm -> cm.getConfidence() > 35).collect(Collectors.toList());
    }

    @Override
    public void learn(Transaction transaction) {
        bayes.learn(transaction.getCategory(), formatTransaction(transaction));
    }

    private List<String> formatTransaction(Transaction transaction) {
        String sanitised = this.sanitiseDetails(transaction.getDetails());
        return Arrays.asList(sanitised.split("\\s"));
    }
}
