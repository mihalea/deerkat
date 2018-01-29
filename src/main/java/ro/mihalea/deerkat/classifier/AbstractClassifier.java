package ro.mihalea.deerkat.classifier;

import ro.mihalea.deerkat.model.Transaction;

import java.util.*;

/**
 * Abstract class that outlines the action that a classifier should take.
 *
 * Classifiers take some model data and based on that make predictions on one item and which category best describes it
 */
public abstract class AbstractClassifier {
    /**
     * PERCENTAGE FROM WHICH TO HIDE MATCHES
     */
    protected final static int CUTOFF_VALUE = 75;

    /**
     * Value after which an automatic match is made
     */
    public final static int AUTOMATIC_MATCH_VALUE = 95;

    /**
     * Value after which the category is still set automatically but it will be displayed differently to the user
     */
    public final static int NEED_CONFIRMATION_VALUE = 50;

    /**
     * Remove certain words from the list to improve matching
     */
    protected final String[] BLACKLIST = new String[] {
            "uae",
            "abu dhabi",
            "llc",
            "are"
    };

    /**
     * List of model data that will be used to calculate matches
     */
    protected List<Transaction> modelData = new ArrayList<>();

    /**
     * Add a List to the model data to improve predictions
     * @param data List of model data to be used
     */
    public void addModelList(List<Transaction> data) {
        data.forEach(this::addModelItem);
    }

    /**
     * Add a single item to the model data to improve predictions, or update an item if one with the same id is found
     * @param data Data added to the model
     */
    public void addModelItem(Transaction data) {
        for (Transaction model : modelData) {
            if(model.getId().equals(data.getId())) {
                model.setCategory(data.getCategory());
                return;
            }
        }

        modelData.add(data);
    }

    /**
     * Return a sorted list of category matches based on their similarity score
     *
     * Doubles should represent the probability of a certain item that it fits that category
     * @param item Item that should be analysed to propose some categories
     * @return Map of categories and their probability that they match
     */
    public abstract List<CategoryMatch> getMatches(Transaction item);

    /**
     * Get the the category that matches the current transaction the most if there are any above the cutoff value
     * @param item Transaction for which to find a category
     * @return Best category if any above cutoff
     */
    public Optional<CategoryMatch> getBest(Transaction item) {
        List<CategoryMatch> matches = getMatches(item);
        if(matches.size() > 0) {
            return Optional.of(matches.get(0));
        } else {
            return Optional.empty();
        }
    }

    /**
     * Transform the title to lowercase and remove any blacklisted words to remove variation in the matches
     * @param title Category title
     * @return Sanitized category title
     */
    protected String sanitizeTitle(String title) {
        title = title.toLowerCase();

        for (String word : BLACKLIST) {
            title = title.replace(word, "");
        }

        title = title.replaceAll("[^a-z]", "");

        return title;
    }
}
