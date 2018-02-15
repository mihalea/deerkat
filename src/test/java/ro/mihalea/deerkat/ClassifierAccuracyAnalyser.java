package ro.mihalea.deerkat;

import org.junit.Before;
import org.junit.Test;
import ro.mihalea.deerkat.classifier.*;
import ro.mihalea.deerkat.classifier.impl.CombinedClassifier;
import ro.mihalea.deerkat.classifier.impl.FuzzyClassifier;
import ro.mihalea.deerkat.classifier.impl.NaiveClassifier;
import ro.mihalea.deerkat.classifier.reducer.AverageReducer;
import ro.mihalea.deerkat.classifier.reducer.MaximumReducer;
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
    private final static int ITERATIONS = 5;

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
    public void fuzzyWithAverage() {
        System.out.println("\n==> Running accuracy analysis for FuzzyClassifier with AverageReducer");
        this.analyseClassifier(new FuzzyClassifier(new AverageReducer()), modelData);
    }

    @Test
    public void fuzzyWithMaximum() {
        System.out.println("\n==> Running accuracy analysis for FuzzyClassifier with MaximumReducer");
        this.analyseClassifier(new FuzzyClassifier(new MaximumReducer()), modelData);
    }

    @Test
    public void naive() {
        System.out.println("\n==> Running accuracy analysis for NaiveClassifier");
        this.analyseClassifier(new NaiveClassifier(), modelData);
    }

    @Test
    public void combined() {
        System.out.println("\n==> Running accuracy analysis for CombinedClassifier");
        this.analyseClassifier(new CombinedClassifier(), modelData);
    }

    private void analyseClassifier(AbstractClassifier classifier, List<Transaction> data) {
        // Half of the transactions will be used to train the classifiers, while the other half will be used for verifying their accuracy
        int trainingSamples = data.size() / 2;

        int noMatch = 0;
        int anyMatch = 0;
        int badMatch = 0;
        int goodMatch = 0;
        int deltaCount = 0;
        double averageAccuracy = 0;
        double goodAccuracy = 0;
        double badAccuracy = 0;
        double averageDelta = 0;

        for (int i = 0; i < ITERATIONS; i++) {
            // Shuffle the list so that a random sample can be chosen
            Collections.shuffle(data);

            List<Transaction> training = data.subList(0, trainingSamples);
            List<Transaction> crosscheck = data.subList(trainingSamples, data.size() - 1);

            classifier.reset();
            classifier.learn(training);

            for (Transaction transaction : crosscheck) {
                List<CategoryMatch> matchList = classifier.classify(transaction);

                if (matchList.size() == 0) {
                    noMatch += 1;
//                    System.out.println(" NO MATCH: " + transaction.getDetails());
                } else {
                    CategoryMatch best = matchList.get(0);

                    anyMatch += 1;
                    averageAccuracy += best.getConfidence();

                    if (!best.getCategory().equals(transaction.getCategory())) {
                        badMatch += 1;
                        badAccuracy += best.getConfidence();

                        System.out.println("BAD MATCH: " + transaction.getDetails());

                        for(CategoryMatch match : matchList) {
                            if(match.getCategory().equals(transaction.getCategory())) {
                                deltaCount += 1;
                                averageDelta += best.getConfidence() - match.getConfidence();
                            }
                        }
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
        averageDelta /= deltaCount;

        System.out.println("\n" +
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
                "Avg delta: " + String.format("%.2f", averageDelta) + "\n" +
                "\n" +
                "   Match accuracy: " + String.format("%.2f", 1d * goodMatch / (anyMatch) * 100) + " %\n" +
                "Match probability: " + String.format("%.2f", 1d * goodMatch / (anyMatch + noMatch) * 100) + " %\n" +
                "\n");
    }
}