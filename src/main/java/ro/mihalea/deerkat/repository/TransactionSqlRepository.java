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
public class TransactionSqlRepository extends AbstractSqlRepository<Transaction>{
    /**
     * Field used to convert to and from SQL dates
     */
    private final TransactionDateConverter converter = new TransactionDateConverter();

    /**
     * Initialise the repository and connect to the local repository at the default file path
     *
     * @throws RepositoryConnectionException Failed to connect to the repository
     */
    public TransactionSqlRepository() throws RepositoryConnectionException {
        super();
    }

    /**
     * Add a new transaction to the repository
     * @param transaction New transaction to be added to the repository
     */
    public Optional<Long> add(Transaction transaction) throws RepositoryCreateException {
        try {
            String createString = "INSERT INTO transactions (id, postingDate, transactionDate, details, amount)" +
                    "VALUES (?, ?, ?, ?, ?)";

            PreparedStatement statement = connection.prepareStatement(createString, Statement.RETURN_GENERATED_KEYS);

            statement.setObject(1, transaction.getId());
            statement.setDate(2, converter.toSQL(transaction.getPostingDate()));
            statement.setDate(3, converter.toSQL(transaction.getTransactionDate()));
            statement.setString(4, transaction.getDetails());
            statement.setDouble(5, transaction.getAmount());

            statement.executeUpdate();
            log.debug("Transaction added to repository: " + transaction);

            return transaction.getId() != null ? Optional.of(transaction.getId()) : this.extractId(statement);
        } catch (SQLException e) {
            throw new RepositoryCreateException("Failed to add the transaction to the database: " + transaction, e);
        }
    }

    @Override
    public void update(Transaction item) throws RepositoryUpdateException {
        try {
            String updateString = "UPDATE transactions  SET postingDate = ?, transactionDate = ?, " +
                    "details = ?, amount = ?, categoryId = ? " +
                    "WHERE " +
                    "id = ?";

            PreparedStatement statement = connection.prepareStatement(updateString);

            statement.setDate(1, converter.toSQL(item.getPostingDate()));
            statement.setDate(2, converter.toSQL(item.getTransactionDate()));
            statement.setString(3, item.getDetails());
            statement.setDouble(4, item.getAmount());
            statement.setLong(5, item.getId());
            statement.setObject(6, item.getCategory() != null ? item.getCategory().getId() : null);

            statement.executeUpdate();
            log.debug("Transaction has been updated: " + item);
        } catch (SQLException e) {
            throw new RepositoryUpdateException("Failed to update the transaction :" + item, e);
        }
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
            int count = 0;
            while(resultSet.next()) {
                Long id = resultSet.getLong("id");
                // Transform from SQL Date to a LocalDate by using epoch time
                LocalDate postingDate = converter.fromSQL(resultSet.getDate("postingDate"));
                LocalDate transactionDate = converter.fromSQL(resultSet.getDate("transactionDate"));
                String details = resultSet.getString("details");
                Double amount = resultSet.getDouble("amount");

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
                    count++;
                }
            }

            log.info("Database returned {} transactions", count);
        } catch (SQLException e) {
            throw new RepositoryReadException("Failed to retrieve all transactions", e);
        }


        return transactions;
    }

    @Override
    public void nuke() throws RepositoryDeleteException {
        this.nukeTable("transactions");
    }


}
