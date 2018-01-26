package ro.mihalea.deerkat.repository;

import lombok.extern.log4j.Log4j2;
import ro.mihalea.deerkat.exception.repository.*;
import ro.mihalea.deerkat.model.Transaction;
import ro.mihalea.deerkat.utility.TransactionDateConverter;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * TransactionSqlRepository is used to interface with the Sqlite database
 * and manage transactions
 */
@Log4j2
public class TransactionSqlRepository extends AbstractSqlRepository<Transaction, Integer>{
    /**
     * Field used to convert to and from SQL dates
     */
    private final TransactionDateConverter converter = new TransactionDateConverter();

    /**
     * Initialise the repository and connect to the local repository at the specified file path
     *
     * @param path Location of the repository file
     * @throws RepositoryConnectionException Failed to connect to the repository
     */
    public TransactionSqlRepository(String path) throws RepositoryConnectionException {
        super(path);
    }

    /**
     * Add a new transaction to the repository
     * @param transaction New transaction to be added to the repository
     */
    public Optional<Integer> add(Transaction transaction) throws RepositoryCreateException {
        try {
            String createString = "INSERT INTO transactions (postingDate, transactionDate, details, amount)" +
                    "VALUES (?, ?, ?, ?)";

            PreparedStatement statement = connection.prepareStatement(createString, Statement.RETURN_GENERATED_KEYS);

            statement.setDate(1, converter.toSQL(transaction.getPostingDate()));
            statement.setDate(2, converter.toSQL(transaction.getTransactionDate()));
            statement.setString(3, transaction.getDetails());
            statement.setDouble(4, transaction.getAmount());

            statement.executeUpdate();
            log.debug("Transaction added to repository: " + transaction);

            ResultSet result = statement.getGeneratedKeys();
            int key;
            if(result != null && result.next()) {
                key = result.getInt(1);
                log.debug("Server returned KEY={} for {}", key, transaction);
                return Optional.of(key);
            } else {
                throw new RepositoryCreateException("Server failed to return a primary key for " + transaction);
            }
        } catch (SQLException e) {
            throw new RepositoryCreateException("Failed to add the transaction to the database: " + transaction, e);
        }
    }

    @Override
    public Transaction getById(Integer key) throws UnimplementedMethodException {
        throw new UnimplementedMethodException("GetById is not implemented");
    }

    /**
     * Retrieve all the transaction stored in the repository
     * @return List of all transactions
     */
    public List<Transaction> getAll() throws RepositoryReadException {
        List<Transaction> transactions = new ArrayList<>();
        try {

            String queryString = "SELECT id, postingDate, transactionDate, details, amount FROM transactions";
            Statement statement = connection.createStatement();

            ResultSet resultSet = statement.executeQuery(queryString);
            log.info("Query returned {} rows", resultSet.getFetchSize());
            while(resultSet.next()) {
                int id = resultSet.getInt("id");
                // Transform from SQL Date to a LocalDate by using epoch time
                LocalDate postingDate = converter.fromSQL(resultSet.getDate("postingDate"));
                LocalDate transactionDate = converter.fromSQL(resultSet.getDate("transactionDate"));
                String details = resultSet.getString("details");
                double amount = resultSet.getDouble("amount");

                // Build a new transaction using the generated builder and the fields above
                Transaction transaction = Transaction.builder()
                        .id(id)
                        .postingDate(postingDate)
                        .transactionDate(transactionDate)
                        .details(details)
                        .amount(amount)
                        .build();

                if(transaction != null) {
                    transactions.add(transaction);
                }
            }
        } catch (SQLException e) {
            throw new RepositoryReadException("Failed to retrieve all transactions", e);
        }


        return transactions;
    }

    @Override
    public void nuke() throws RepositoryDeleteException {
        try {
            String queryString = "DELETE FROM transactions; DELETE FROM categories;";
            Statement statement = connection.createStatement();
            statement.executeUpdate(queryString);
            log.info("Database has been nuked");
        } catch (SQLException e) {
            throw new RepositoryDeleteException("Failed to delete transactions table", e);
        }

    }
}
