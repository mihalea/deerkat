package ro.mihalea.deerkat.model;

import lombok.*;
import ro.mihalea.deerkat.classifier.ConfidenceLevel;

import java.time.LocalDate;

@Builder
@Data
public class Transaction {
    /**
     * Unique id that identifies the transaction in the repository
     */
    private Long id;

    /**
     * Date when the transaction was received by the bank
     * FORMAT: "%B %d, %Y"
     */
    private @NonNull LocalDate transactionDate;

    /**
     * Date when the transaction was processed by the bank
     * Format is the same as the transactionDate's
     *
     * @see #transactionDate
     */
    private @NonNull LocalDate postingDate;

    /**
     * Additional details attached to the transaction, such as the merchant, money transfer information and others
     */
    private @NonNull String details;

    /**
     * Amount of money transferred
     */
    private @NonNull Double amount;

    /**
     * Category that the transaction is part of
     */
    private Category category;

    /**
     * Marks whether the confidence in the matched category is not high enough to mark it automatically
     * Default is: ConfidenceLevel.NONE
     */
    @Builder.Default private transient ConfidenceLevel confidenceLevel = ConfidenceLevel.NONE;
}

