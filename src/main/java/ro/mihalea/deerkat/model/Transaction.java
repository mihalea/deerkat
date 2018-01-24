package ro.mihalea.deerkat.model;

import lombok.Data;

import java.time.LocalDate;

public @Data
class Transaction {
    /**
     * Date when the transaction was received by the bank
     * FORMAT: "%B %d, %Y"
     */
    private LocalDate transactionDate;

    /**
     * Date when the transaction was processed by the bank
     * Format is the same as the transactionDate's
     * @see #transactionDate
     */
    private LocalDate postingDate;

    /**
     * Additional details attached to the transaction, such as the merchant, money transfer information and others
     */
    private String details;

    /**
     * Amount of money transferred
     */
    private double amount;
}
