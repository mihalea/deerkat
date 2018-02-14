package ro.mihalea.deerkat.repository;

import lombok.extern.log4j.Log4j2;
import ro.mihalea.deerkat.exception.repository.*;
import ro.mihalea.deerkat.model.Category;
import ro.mihalea.deerkat.model.Transaction;
import ro.mihalea.deerkat.utility.LocalDateConverter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * CsvRepository is used to create CSV files and to read from them
 */
@Log4j2
public class CsvRepository implements IRepository<Transaction> {
    /**
     * Field used to convert LocalDates to and from String
     */
    private final LocalDateConverter converter = new LocalDateConverter();

    /**
     * Path to the CSV file used for writing and reading
     */
    private final Path filePath;

    /**
     * Table headers in CSV format containing field names as needed by YNAB4
     */
    private final static String HEADERS = "Transaction ID,Date,Posting Date,Payee,Category Id,Category,Memo,Outflow,Inflow" + System.lineSeparator();

    /**
     * Date formated chosen to be compatible with the YNAB4 application
     */
    private final String DATE_FORMAT = "dd/MM/yy";

    /**
     * Category repository used to resolve parent categories' title
     */
    private CategorySqlRepository categoryRepository;

    /**
     * Construct the repository, and nuke the contents
     *
     * @param csvLocation Path to the csv file
     */
    public CsvRepository(Path csvLocation) throws RepositoryInitialisationException {
        this(csvLocation, true);
    }

    /**
     * Initialise the repository path and nuke the contents if needed
     * @param csvLocation Path to the csv file
     * @param delete Flag signalling whether all pre existent data from the repository should be deleted
     * @throws RepositoryInitialisationException
     */
    public CsvRepository(Path csvLocation, boolean delete) throws RepositoryInitialisationException {
        this.filePath = csvLocation;

        // Create a new file containing the table header and overwrite any preexisting files
        try {
            if(delete) {
                this.nuke();
            }
        } catch (RepositoryDeleteException e) {
            throw new RepositoryInitialisationException("Failed to initialise repository", e);
        }

        log.info("Initialised CSV Repository");
    }

    @Override
    public Optional<Transaction> getById(Long id) throws RepositoryReadException, UnimplementedMethodException {
        throw new UnimplementedMethodException("GetById is not implemented");
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
    public List<Transaction> getAll() throws RepositoryReadException {
        try {
            List<Transaction> transactions =  Files.readAllLines(filePath).stream()
                    .skip(1)
                    .map(t -> {
                        try {
                            return this.fromCSV(t);
                        } catch (RepositoryParseException | RepositoryReadException e) {
                            log.error("Failed to parse transaction", e);
                        }

                        return null;
                    })
                    .collect(Collectors.toList());

            log.info("User requested {} transactions from csv", transactions.size());

            return transactions;
        } catch (IOException e) {
            throw new RepositoryReadException("Failed to read repository from " + filePath.toAbsolutePath().toString(), e);
        }
    }

    @Override
    public void update(Transaction transaction) throws RepositoryUpdateException, UnimplementedMethodException {
        throw new UnimplementedMethodException("update is not implemented");
    }

    @Override
    public void nuke() throws RepositoryDeleteException {
        try {
            if (Files.exists(filePath)) {
                // If the file exists check that it is a regular files, and that it's readable and writeable
                if (!Files.isRegularFile(filePath)) {
                    throw new RepositoryDeleteException("The path provided does not lead to a regular file: " + filePath);
                }
                if (!Files.isReadable(filePath)) {
                    throw new RepositoryDeleteException("The path provided does not lead to a readable file: " + filePath);
                }
                if (!Files.isWritable(filePath)) {
                    throw new RepositoryDeleteException("The path provided does not lead to a writeable file: " + filePath);
                }


                Files.delete(filePath);
            }
            Files.write(filePath, HEADERS.getBytes());
            log.info("Created new CSV file at " + filePath.toString());
        } catch (IOException e) {
            throw new RepositoryDeleteException("Failed to nuke the database!", e);
        }
    }

    /**
     * Convert from a line of CSV containing a transaction to a Transaction object
     *
     * @param csv Line of CSV containing a transaction
     * @return Transaction object parsed from csv
     */
    private Transaction fromCSV(String csv) throws RepositoryParseException, RepositoryReadException {
        String[] fields = csv.split(",");

        Transaction.TransactionBuilder builder = Transaction.builder();

        try {
            builder.id(Long.parseLong(fields[0]))
                    .transactionDate(converter.fromString(fields[1], DATE_FORMAT))
                    .postingDate(converter.fromString(fields[2], DATE_FORMAT))
                    .details(fields[3]);

            String categoryString = fields[4];
            if(!categoryString.trim().isEmpty()) {
                long categoryId = Long.parseLong(categoryString);
                categoryRepository.getById(categoryId).ifPresent(builder::category);
            }

            String outflow = fields[7];
            String inflow = fields[8];
            if(!outflow.trim().isEmpty()) {
                builder.amount(Double.parseDouble(outflow));
                builder.inflow(false);
            } else if (!inflow.trim().isEmpty()) {
                builder.amount(Double.parseDouble(inflow));
                builder.inflow(true);
            } else {
                log.error("Both inflow and outflow are empty");
                throw new RepositoryParseException("Failed to parse transaction amount for line: " + csv);
            }


        } catch (NumberFormatException e) {
            log.error("Failed to parse object number for line: " + csv, e);
        }


        return builder.build();
    }

    /**
     * Convert from a Transaction object to a line of CSV
     *
     * @param transaction Transaction to be formatted as CSV
     * @return String in CSV format storing a transaction
     */
    private String toCSV(Transaction transaction) {
        String category = "";
        try {
            if(transaction.getInflow()) {
                category = "Income: Available this month";
            } else if (categoryRepository != null && transaction.getCategory() != null) {
                Category subCategory = transaction.getCategory();
                Optional<Category> parentOptional = categoryRepository.getById(subCategory.getParentId());
                if (parentOptional.isPresent()) {
                    category = parentOptional.get().getTitle() + ": " + subCategory.getTitle();
                }
            }
        } catch (RepositoryReadException e) {
            log.error("Failed to retrieve parent category");
        }

        return transaction.getId() +
                "," +
                String.valueOf(converter.toString(transaction.getTransactionDate(), DATE_FORMAT) +
                "," +
                String.valueOf(converter.toString(transaction.getPostingDate(), DATE_FORMAT)) +
                "," +
                transaction.getDetails() +
                "," +
                (transaction.getCategory() != null ? transaction.getCategory().getId()  : " ") +
                "," +
                category +
                "," +
                String.format(transaction.getInflow() ? ", ,%s" : ",%s, ", transaction.getAmount()) +
                System.lineSeparator());
    }

    public Path getFilePath() {
        return filePath;
    }

    public void setCategoryRepository(CategorySqlRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }
}
