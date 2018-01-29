package ro.mihalea.deerkat.repository;

import lombok.extern.log4j.Log4j2;
import ro.mihalea.deerkat.exception.repository.RepositoryCreateException;
import ro.mihalea.deerkat.exception.repository.RepositoryDeleteException;
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
import java.util.Optional;

/**
 * CsvRepository is used to create CSV files and to read from them
 */
@Log4j2
public class CsvRepository implements IRepository<Transaction> {
    /**
     * Field used to convert LocalDates to and from String
     */
    private final TransactionDateConverter converter = new TransactionDateConverter();

    /**
     * Path to the CSV file used for writing and reading
     */
    private final Path filePath;

    /**
     * Table headers in CSV format containing field names as needed by YNAB4
     */
    private final static String HEADERS = "Date,Payee,Category,Memo,Outflow,Inflow" + System.lineSeparator();

    /**
     * Date formated chosen to be compatible with the YNAB4 application
     */
    private final String DATE_FORMAT = "MM/dd/yy";

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
                this.nuke();
            } catch (RepositoryDeleteException e) {
                throw new RepositoryInitialisationException("Failed to create a new database file", e);
            }
        }

        log.info("Initialised CSV Repository");
    }

    @Override
    public Optional<Long> add(Transaction transaction) throws RepositoryCreateException {
        try {
            Files.write(filePath, this.toCSV(transaction).getBytes(), StandardOpenOption.APPEND);
            log.info("Add transaction: " + transaction);
            return Optional.empty();
        } catch (IOException e) {
            throw new RepositoryCreateException("Failed to write the transaction to the file: " + transaction, e);
        }
    }

    @Override
    public List<Transaction> getAll() throws UnimplementedMethodException {
        throw new UnimplementedMethodException("getAll is not implemented");
    }

    @Override
    public void nuke() throws RepositoryDeleteException {
        try {
            Files.write(filePath, HEADERS.getBytes());
            log.info("Created new CSV file at " + filePath.toString());
        } catch (IOException e) {
            throw new RepositoryDeleteException("Failed to nuke the database!", e);
        }
    }

    /**
     * Convert from a line of CSV containing a transaction to a Transaction object
     * @param csv Line of CSV containing a transaction
     * @return Transaction object parsed from csv
     */
    private Transaction fromCSV(String csv) {
        String[] fields = csv.split(",");

        return Transaction.builder()
                .postingDate(converter.fromString(fields[0], DATE_FORMAT))
                .transactionDate(converter.fromString(fields[1], DATE_FORMAT))
                .details(fields[2])
                .amount(Double.parseDouble(fields[3])).build();
    }

    /**
     * Convert from a Transaction object to a line of CSV
     * @param transaction Transaction to be formatted as CSV
     * @return String in CSV format storing a transaction
     */
    private String toCSV(Transaction transaction) {
        return String.valueOf(converter.toString(transaction.getTransactionDate(), DATE_FORMAT) +
                "," +
                transaction.getDetails() +
                ",,," +
                transaction.getAmount() +
                "," +
                System.lineSeparator());
    }
}
