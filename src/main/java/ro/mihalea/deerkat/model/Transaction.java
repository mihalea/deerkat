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
     * Details whether the amount was deposited or withdrawn from the account
     *
     * True if the money has been withdrawn
     */
    private @Builder.Default @NonNull Boolean inflow = false;

    /**
     * Category that the transaction is part of
     */
    private Category category;

    /**
     * Confidence level of the current category as an integer from 0..100
     */
    private @Builder.Default Integer confidence = ConfidenceLevel.NONE;
}

