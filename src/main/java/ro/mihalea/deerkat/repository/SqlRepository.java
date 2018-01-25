package ro.mihalea.deerkat.repository;

import lombok.extern.log4j.Log4j2;
import ro.mihalea.deerkat.exception.repository.*;
import ro.mihalea.deerkat.model.Transaction;
import ro.mihalea.deerkat.utility.TransactionDateConverter;

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
 * SqlRepository is used to interface with the Sqlite database
 * and manage transactions
 */
@Log4j2
public class SqlRepository implements IRepository<Transaction, Integer>{
    /**
     * Path of the configuration file used to initialise the database
     */
    private final static String INITIALISATION_FILE = "configuration.sql";

    /**
     * Connection to the repository
     */
    private final Connection connection;

    /**
     * Field used to convert to and from SQL dates
     */
    private final TransactionDateConverter converter = new TransactionDateConverter();

    /**
     * Initialise the repository and connect to the local repository at the specified file path
     * @param path Location of the repository file
     * @throws RepositoryConnectionException Failed to connect to the repository
     */
    public SqlRepository(String path) throws RepositoryConnectionException {
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            this.initialiseDatabase();
            log.info("Successfully connected to the repository at {}", path);
        } catch (SQLException | RepositoryInitialisationException e) {
            throw new RepositoryConnectionException("Failed to connect to the local repository", e);
        }
    }

    /**
     * Create tables in the repository according to the initialisation file
     */
    private void initialiseDatabase() throws RepositoryInitialisationException {
        try {
            URL resource = getClass().getClassLoader().getResource(INITIALISATION_FILE);
            // Abort initialisation if the initialisation file could not be found
            if(resource == null) {
                throw new RepositoryInitialisationException("Failed to retrieve the repository initialisation file");
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
            throw new RepositoryInitialisationException("Failed to read the repository initialisation file", e);
        } catch (SQLException e) {
            throw new RepositoryInitialisationException("Failed to add one of the configuration statements", e);
        }
    }

    /**
     * Add a new transaction to the repository
     * @param transaction New transaction to be added to the repository
     */
    public void add(Transaction transaction) throws RepositoryCreateException {
        try {
            String createString = "INSERT INTO transactions (postingDate, transactionDate, details, amount)" +
                    "VALUES (?, ?, ?, ?)";

            PreparedStatement statement = connection.prepareStatement(createString);

            statement.setDate(1, converter.toSQL(transaction.getPostingDate()));
            statement.setDate(2, converter.toSQL(transaction.getTransactionDate()));
            statement.setString(3, transaction.getDetails());
            statement.setDouble(4, transaction.getAmount());

            statement.executeUpdate();
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
            String queryString = "DELETE FROM transactions";
            Statement statement = connection.createStatement();
            statement.execute(queryString);
        } catch (SQLException e) {
            throw new RepositoryDeleteException("Failed to delete transactions table", e);
        }

    }
}
