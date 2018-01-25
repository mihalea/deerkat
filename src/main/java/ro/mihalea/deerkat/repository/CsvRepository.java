package ro.mihalea.deerkat.repository;

import lombok.extern.log4j.Log4j2;
import ro.mihalea.deerkat.exception.repository.RepositoryCreateException;
import ro.mihalea.deerkat.exception.repository.RepositoryInitialisationException;
import ro.mihalea.deerkat.exception.repository.UnimplementedMethodException;
import ro.mihalea.deerkat.model.Transaction;
import ro.mihalea.deerkat.utility.TransactionDateConverter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import java.util.List;

/**
 * CsvRepository is used to create CSV files and to read from them
 */
@Log4j2
public class CsvRepository implements IRepository<Transaction, Integer> {
    /**
     * Field used to convert LocalDates to and from String
     */
    private final TransactionDateConverter converter = new TransactionDateConverter();

    /**
     * Path to the CSV file used for writing and reading
     */
    private final Path filePath;

    /**
     * Table headers in CSV format containing field names
     */
    private final static String HEADERS = "Posting Date, Transaction Date, Details, Amount" + System.lineSeparator();

    /**
     * Construct the repository and check that the path is valid
     * @param csvLocation Path to the csv file
     */
    public CsvRepository(String csvLocation) throws RepositoryInitialisationException {
        filePath = Paths.get(csvLocation);

        if(Files.exists(filePath)) {
            // If the file exists check that it is a regular files, and that it's readable and writeable
            if (!Files.isRegularFile(filePath)) {
                throw new RepositoryInitialisationException("The path provided does not lead to a regular file: " + csvLocation);
            }
            if (!Files.isReadable(filePath)) {
                throw new RepositoryInitialisationException("The path provided does not lead to a readable file: " + csvLocation);
            }
            if (!Files.isWritable(filePath)) {
                throw new RepositoryInitialisationException("The path provided does not lead to a writeable file: " + csvLocation);
            }
        } else {
            // If the file does not exist try to create it
            try {
                Files.write(filePath, HEADERS.getBytes());
                log.info("Created new CSV file at " + csvLocation);
            } catch (IOException e) {
                throw new RepositoryInitialisationException("Failed to created the CSV file", e);
            }
        }

        log.info("Initialised CSV Repository");
    }

    @Override
    public void add(Transaction transaction) throws RepositoryCreateException {
        try {
            Files.write(filePath, this.toCSV(transaction).getBytes(), StandardOpenOption.APPEND);
            log.info("Add transaction: " + transaction);
        } catch (IOException e) {
            throw new RepositoryCreateException("Failed to write the transaction to the file: " + transaction, e);
        }
    }

    @Override
    public Transaction getById(Integer key) throws UnimplementedMethodException {
        throw new UnimplementedMethodException("GetById is not implemented");
    }

    @Override
    public List<Transaction> getAll() throws UnimplementedMethodException {
        throw new UnimplementedMethodException("getAll is not implemented");
    }

    /**
     * Convert from a line of CSV containing a transaction to a Transaction object
     * @param csv Line of CSV containing a transaction
     * @return Transaction object parsed from csv
     */
    private Transaction fromCSV(String csv) {
        String[] fields = csv.split(",");

        return Transaction.builder()
                .postingDate(converter.fromString(fields[0]))
                .transactionDate(converter.fromString(fields[1]))
                .details(fields[2])
                .amount(Double.parseDouble(fields[3])).build();
    }

    /**
     * Convert from a Transaction object to a line of CSV
     * @param transaction Transaction to be formatted as CSV
     * @return String in CSV format storing a transaction
     */
    private String toCSV(Transaction transaction) {
        return String.valueOf(converter.toString(transaction.getPostingDate()) +
                "," +
                converter.toString(transaction.getTransactionDate()) +
                "," +
                transaction.getDetails() +
                "," +
                transaction.getAmount() +
                System.lineSeparator());
    }
}
