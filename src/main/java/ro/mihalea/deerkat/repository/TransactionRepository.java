package ro.mihalea.deerkat.repository;

import lombok.extern.log4j.Log4j2;
import ro.mihalea.deerkat.exception.database.DatabaseConnectionException;
import ro.mihalea.deerkat.exception.database.DatabaseInitialisationException;
import ro.mihalea.deerkat.exception.database.DatabaseStatementException;
import ro.mihalea.deerkat.model.Transaction;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * TransactionRepository is used to interface with the Sqlite database
 * and manage transactions
 */
@Log4j2
public class TransactionRepository {
    private final static String INITIALISATION_FILE = "configuration.sql";

    /**
     * Connection to the database
     */
    private final Connection connection;

    /**
     * Initialise the repository and connect to the local database at the specified file path
     * @param path Location of the database file
     * @throws DatabaseConnectionException Failed to connect to the database
     */
    public TransactionRepository(String path) throws DatabaseConnectionException {
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            this.initialiseDatabase();
            log.info("Successfully connected to the database at {}", path);
        } catch (SQLException | DatabaseStatementException | DatabaseInitialisationException e) {
            throw new DatabaseConnectionException("Failed to connect to the local database", e);
        }
    }

    /**
     * Create tables in the database according to the initialisation file
     */
    private void initialiseDatabase() throws DatabaseStatementException, DatabaseInitialisationException {
        try {
            URL resource = getClass().getClassLoader().getResource(INITIALISATION_FILE);
            // Abort initialisation if the initialisation file could not be found
            if(resource == null) {
                throw new DatabaseInitialisationException("Failed to retrieve the database initialisation file");
            }

            Path initialisationPath = Paths.get(resource.getFile());

            // Read all lines from the file and combine them into a single string
            String content = Files.readAllLines(initialisationPath)
                    .stream()
                    .collect(Collectors.joining("\n","", ""));

            // Split the content based on the ";" which marks a statement's end
            String[] statements = content.split(";");

            // Execute every statement identified
            for(String statementString : statements) {
                Statement statement = connection.createStatement();
                statement.execute(statementString);
                statement.close();
            }


        } catch (IOException e) {
            throw new DatabaseInitialisationException("Failed to read the database initialisation file", e);
        } catch (SQLException e) {
            throw new DatabaseStatementException("Failed to create one of the configuration statements", e);
        }
    }


    /**
     * Add multiple transactions at once to the database
     * @param newTransactions Iterable of new transactions to be added
     */
    public void createAll(Iterable<Transaction> newTransactions) throws DatabaseStatementException {
        for(Transaction transaction : newTransactions) {
            this.create(transaction);
        }
    }

    /**
     * Add a new transaction to the database
     * @param transaction New transaction to be added to the database
     */
    public void create(Transaction transaction) throws DatabaseStatementException {
        try {
            String createString = "INSERT INTO transactions (postingDate, transactionDate, details, amount)" +
                    "VALUES (?, ?, ?, ?)";

            PreparedStatement statement = connection.prepareStatement(createString);

            statement.setDate(1, new Date(transaction.getPostingDate().toEpochDay()));
            statement.setDate(2, new Date(transaction.getTransactionDate().toEpochDay()));
            statement.setString(3, transaction.getDetails());
            statement.setDouble(4, transaction.getAmount());

            statement.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseStatementException("Failed to create transaction", e);
        }
    }

    /**
     * Retrieve all the transaction stored in the database
     * @return List of all transactions
     */
    public List<Transaction> getAll() throws DatabaseStatementException {
        List<Transaction> transactions = new ArrayList<>();
        try {

            String queryString = "SELECT id, postingDate, transactionDate, details, amount FROM transactions";
            Statement statement = connection.createStatement();

            ResultSet resultSet = statement.executeQuery(queryString);
            while(resultSet.next()) {
                int id = resultSet.getInt("id");
                // Transform from SQL Date to a LocalDate by using epoch time
                LocalDate postingDate = LocalDate.ofEpochDay(resultSet.getDate("postingDate").getTime());
                LocalDate transactionDate = LocalDate.ofEpochDay(resultSet.getDate("transactionDate").getTime());
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
            throw new DatabaseStatementException("Failed to retrieve all transactions", e);
        }


        return transactions;
    }
}
