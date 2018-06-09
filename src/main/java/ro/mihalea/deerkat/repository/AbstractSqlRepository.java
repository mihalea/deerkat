package ro.mihalea.deerkat.repository;

import lombok.extern.log4j.Log4j2;
import ro.mihalea.deerkat.exception.repository.*;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Log4j2
public abstract class AbstractSqlRepository<DataType> implements IRepository<DataType> {
    /**
     * Path of the configuration directory used to initialise the database
     */
    private final static String INITIALISATION_DIR = "schemas";

    /**
     * Default path of the database that repositories will use
     */
    private final static String DATABASE_FILE = "deerkat.sqlite";

    /**
     * Connection to the repository
     */
    protected static Connection connection;

    /**
     * Default constructor that uses the default database file
     *
     * @throws RepositoryConnectionException
     */
    public AbstractSqlRepository() throws RepositoryConnectionException {
        this(DATABASE_FILE);
    }

    /**
     * Initialise the repository and connect to the local repository at the specified file path
     *
     * @param path Location of the repository file
     * @throws RepositoryConnectionException Failed to connect to the repository
     */
    public AbstractSqlRepository(String path) throws RepositoryConnectionException {
        try {
            // If the database file does not exists the initialisation method needs to be run
            boolean newDatabase = Files.notExists(Paths.get(path));

            if (connection == null) {
                connection = DriverManager.getConnection("jdbc:sqlite:" + path);
                log.info("Successfully connected to the repository at {}", path);
                this.initialiseDatabase(newDatabase);
            }
        } catch (SQLException | RepositoryInitialisationException e) {
            throw new RepositoryConnectionException("Failed to connect to the local repository", e);
        }
    }

    /**
     * Create and modify tables in order to bring the database up to the latest version
     * @param newDatabase Whether the database file is newly created or not
     * @throws RepositoryInitialisationException Failed to initialise repository
     */
    private void initialiseDatabase(boolean newDatabase) throws RepositoryInitialisationException {
        try {
            int dbVersion = newDatabase ? 0 : this.localSchemaVersion();
            int latestVersion = this.latestSchemaVersion();

            log.info("Local database version: " + dbVersion);
            log.info("Latest schema version: " + latestVersion);

            for (int i = dbVersion + 1; i <= latestVersion; i++) {
                initialiseDatabaseVersion(i);
            }
        } catch (RepositoryReadException e) {
            throw new RepositoryInitialisationException("Failed to initialise repository", e);
        }

    }

    /**
     * Get the latest database schema version from the current jar
     *
     * It assumes that no other files are present in the directory and that they are correctly named
     * @return Latest database schema version
     */
    private int latestSchemaVersion() {
        InputStream in = getClass().getClassLoader().getResourceAsStream(INITIALISATION_DIR);
        BufferedReader br = new BufferedReader(new InputStreamReader(in));

        return (int) br.lines().count();
    }

    /**
     * Returns the local database version
     * @return Local database version
     * @throws RepositoryReadException Failed to access the database to verify whether 'settings' exists
     */
    private int localSchemaVersion() throws RepositoryReadException {
        if (!this.tableExists("settings")) {
            return 1;
        } else {
            return Integer.parseInt(getSetting("dbVersion"));
        }
    }

    /**
     * Create and modify tables in the repository according to the initialisation file
     */
    private void initialiseDatabaseVersion(int version) throws RepositoryInitialisationException {
        try {
            String file = String.format("%s/%03d.sql", INITIALISATION_DIR, version);

            log.debug("Initialising database from " + file);
            InputStream stream = getClass().getClassLoader().getResourceAsStream(file);
            // Abort initialisation if the initialisation file could not be found
            if (stream == null) {
                throw new RepositoryInitialisationException("Failed to retrieve the repository initialisation file");
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            String content = reader.lines().collect(Collectors.joining());

            // Split the content based on the ";" which marks a statement's end
            String[] statements = content.split(";");

            // Ignore empty strings that may appear when splitting
            List<String> validStatements = Arrays.stream(statements)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());

            log.debug("Running {} configuration statements", validStatements.size());
            // Execute every statement identified
            for (String statementString : validStatements) {
                log.debug("Executing: " + statementString);
                Statement statement = connection.createStatement();
                statement.executeUpdate(statementString);
                statement.close();
            }
        } catch (SQLException e) {
            throw new RepositoryInitialisationException("Failed to add one of the configuration statements", e);
        }
    }


    /**
     * From a statement run with RETURN_GENERATED_KEY, extract the key as an optional and return it
     *
     * @param statement Statement that had an insert operation and has been executed
     * @return Optional that may contain an item id
     */
    protected Optional<Long> extractId(Statement statement) throws RepositoryCreateException {
        try {
            ResultSet result = statement.getGeneratedKeys();
            Long key;
            if (result != null && result.next()) {
                key = result.getLong(1);

                log.debug("Server returned KEY={}", key);
                return Optional.of(key);
            } else {
                throw new RepositoryCreateException("Server failed to return a primary key for " + statement);
            }
        } catch (SQLException e) {
            throw new RepositoryCreateException("Failed to get a generated key from the statement: " + statement);
        }
    }

    /**
     * Get values from the settings table
     * @param name Name of the property
     * @return Value of the property encoded as a String
     * @throws RepositoryReadException Exception returned when the repository fails to make the query
     */
    protected String getSetting(String name) throws RepositoryReadException {
        try {
            String queryString = "SELECT value FROM settings WHERE property = ?";
            PreparedStatement statement = connection.prepareStatement(queryString);
            statement.setString(1, name);

            ResultSet resultSet = statement.executeQuery();
            return resultSet.getString("value");
        } catch (SQLException e) {
            throw new RepositoryReadException("Failed to read property: " + name, e);
        }
    }

    /**
     * Delete all data from a single table
     * <p>
     * Prepared statements can't be used as table names can't be set as variables, so string concatenation is used
     * here instead.
     *
     * @param tableName Name of the table which is emptied. Should not accept user input as this is not a prepared statement.
     */
    protected void nukeTable(String tableName) throws RepositoryDeleteException {
        try {
            String queryString = "DELETE FROM " + tableName;
            Statement statement = connection.createStatement();
            statement.executeUpdate(queryString);
            log.info("Table '{}' has been nuked", tableName);
        } catch (SQLException e) {
            throw new RepositoryDeleteException("Failed to delete " + tableName + " table", e);
        }
    }

    /**
     * Checks whether a table is already present in the database
     *
     * @param tableName Name of the table
     * @return True if the table exists in the local database
     */
    public boolean tableExists(String tableName) {
        DatabaseMetaData meta = null;
        try {
            meta = connection.getMetaData();
            ResultSet rs = meta.getTables(null, null, tableName, null);
            return rs.next();
        } catch (SQLException e) {
            return false;
        }

    }
}
