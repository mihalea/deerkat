package ro.mihalea.deerkat.classifier;

import lombok.extern.log4j.Log4j2;
import ro.mihalea.deerkat.model.Transaction;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Abstract class that outlines the action that a classifier should take.
 *
 * Classifiers take some model data and based on that make predictions on one item and which category best describes it
 */
@Log4j2
public abstract class AbstractClassifier {
    /**
     * Remove certain words from the list to improve matching
     */
    private final List<String> BLACKLIST = Arrays.asList(
            "uae",
            "abu",
            "dhabi",
            "llc",
            "are"
    );

    /**
     * List of delimiters used to split the transaction details into tokens
     */
    private final String DELIMITERS = Stream.of(
            "-",
            "/",
            ",",
            "\\s"
    ).collect(Collectors.joining("|"));


    /**
     * Add the transaction to the learning database so that future predictions can be made more accurately
     * @param transaction Transaction with a set category
     */
    public abstract void learn(Transaction transaction);

    /**
     * Add multiple transaction to the learning database
     * @param transactions List of transactions with categories set
     */
    public void learn(List<Transaction> transactions) {
        transactions.forEach(this::learn);
    }

    /**
     * Return a sorted list of category matches based on their similarity score
     *
     * Doubles should represent the probability of a certain item that it fits that category
     * @param item Item that should be analysed to propose some categories
     * @return Map of categories and their probability that they match
     */
    public abstract List<CategoryMatch> classify(Transaction item);

    /**
     * Get the the category that matches the current transaction the most if there are any above the cutoff value
     * @param item Transaction for which to find a category
     * @return Best category if any above cutoff
     */
    public Optional<CategoryMatch> getBest(Transaction item) {
        List<CategoryMatch> matches = classify(item);
        if(matches.size() > 0) {
            return Optional.of(matches.get(0));
        } else {
            return Optional.empty();
        }
    }

    /**
     * Transform the title to lowercase and remove any blacklisted words to remove variation in the matches
     * @param details Transaction details
     * @return Sanitized transaction details
     */
    protected String sanitiseDetails(String details) {
        // Convert all characters to lowercase to reduce variation
        details = details.toLowerCase();

        // Remove anything that's not a space or a letter
        details = details.replaceAll("[^a-z\\s]", "");

        // Split into tokens and remove blacklisted words, and single characters
        details = Arrays.stream(details.split(DELIMITERS))
                .filter(s -> s.length() > 1)
                .filter(s -> !BLACKLIST.contains(s))
                .collect(Collectors.joining(" "));


        return details;
    }

    /**
     * Discard all training been done so far
     */
    public abstract void reset();
}
