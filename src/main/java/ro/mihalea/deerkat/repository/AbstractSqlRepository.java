package ro.mihalea.deerkat.repository;

import lombok.extern.log4j.Log4j2;
import ro.mihalea.deerkat.exception.repository.RepositoryConnectionException;
import ro.mihalea.deerkat.exception.repository.RepositoryInitialisationException;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Log4j2
public abstract class AbstractSqlRepository<DataType, KeyType> implements IRepository<DataType, KeyType>{
    /**
     * Path of the configuration file used to initialise the database
     */
    private final static String INITIALISATION_FILE = "configuration.sql";

    /**
     * Connection to the repository
     */
    protected final Connection connection;

    /**
     * Initialise the repository and connect to the local repository at the specified file path
     * @param path Location of the repository file
     * @throws RepositoryConnectionException Failed to connect to the repository
     */
    public AbstractSqlRepository(String path) throws RepositoryConnectionException {
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
            log.debug("Initialising database from " + INITIALISATION_FILE);
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

            // Ignore empty strings that may appear when splitting
            List<String> validStatements = Arrays.stream(statements)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());

            log.debug("Found {} configuration statements", validStatements.size());
            // Execute every statement identified
            for(String statementString : validStatements) {
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
}
