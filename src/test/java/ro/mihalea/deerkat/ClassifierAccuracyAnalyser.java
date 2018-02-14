package ro.mihalea.deerkat;

import org.junit.Before;
import org.junit.Test;
import ro.mihalea.deerkat.classifier.*;
import ro.mihalea.deerkat.model.Transaction;
import ro.mihalea.deerkat.repository.CategorySqlRepository;
import ro.mihalea.deerkat.repository.CsvRepository;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ClassifierAccuracyAnalyser {
    /**
     * Number of iterations that each classifier gets run through
     */
    private final static int ITERATIONS = 250;

    /**
     * List of transactions used to check the accuracy of the classifier
     */
    private List<Transaction> modelData;

    /**
     * Repository used to load model data
     */
    private CsvRepository transactionRepository;

    /**
     * Repository used to load available transactions
     */
    private CategorySqlRepository categoryRepository;

    @Before
    public void setUp() throws Exception {
        // Load the categories from the default resource file
        String db_location = ClassifierAccuracyAnalyser.class.getClassLoader().getResource("deerkat.sqlite").getPath();
        categoryRepository = new CategorySqlRepository(db_location);

        // Load the transactions from the default model data resource file
        String csv_location = ClassifierAccuracyAnalyser.class.getClassLoader().getResource("model_data.csv").getPath();
        Path path = Paths.get(csv_location);
        transactionRepository = new CsvRepository(path, false);
        transactionRepository.setCategoryRepository(categoryRepository);

        modelData = transactionRepository.getAll().stream().filter(t -> t.getCategory() != null).collect(Collectors.toList());


    }

    @Test
    public void randomSampling() throws Exception {
        if(modelData.size() <= 2) {
            throw new Exception("Not enough model data");
        }

        this.analyseClassifier(new FuzzyClassifier(), modelData);
    }

    private void analyseClassifier(AbstractClassifier classifier, List<Transaction> data) {
        // Half of the transactions will be used to train the classifiers, while the other half will be used for verifying their accuracy
        int trainingSamples = data.size() / 2;

        int noMatch = 0;
        int anyMatch = 0;
        int badMatch = 0;
        int goodMatch = 0;
        double averageAccuracy = 0;
        double goodAccuracy = 0;
        double badAccuracy = 0;

        for (int i = 0; i < ITERATIONS; i++) {
            // Shuffle the list so that a random sample can be chosen
            Collections.shuffle(modelData);

            List<Transaction> training = modelData.subList(0, trainingSamples);
            List<Transaction> crosscheck = modelData.subList(trainingSamples, modelData.size() - 1);

            classifier.reset();
            classifier.learn(training);

            for (Transaction transaction : crosscheck) {
                List<CategoryMatch> matchList = classifier.classify(transaction);

                if (matchList.size() == 0) {
                    noMatch += 1;
                } else {
                    CategoryMatch best = matchList.get(0);

                    anyMatch += 1;
                    averageAccuracy += best.getConfidence();

                    if (!best.getCategory().equals(transaction.getCategory())) {
                        badMatch += 1;
                        badAccuracy += best.getConfidence();
                    } else {
                        goodMatch += 1;
                        goodAccuracy += best.getConfidence();
                    }
                }
            }
        }

        averageAccuracy /= anyMatch;
        goodAccuracy /= goodMatch;
        badAccuracy /= badMatch;

        System.out.println("\n" +
                "ACCURACY ANALYSIS FOR <" + classifier.getClass().getSimpleName() + ">\n" +
                "\n" +
                "Total transactions: " + data.size() + "\n" +
                "\n" +
                "  No match: " + noMatch + "\n" +
                "Good match: " + goodMatch + "\n" +
                " Bad match: " + badMatch + "\n" +
                "\n" +
                "Average accuracy: " + String.format("%.2f", averageAccuracy) + " %\n" +
                "   Good accuracy: " + String.format("%.2f", goodAccuracy) + " %\n" +
                "    Bad accuracy: " + String.format("%.2f", badAccuracy) + " %\n" +
                "\n" +
                "Match accuracy: " + String.format("%.2f", 1d * goodMatch / (anyMatch + noMatch) * 100) + " %\n" +
                "\n");
    }
}