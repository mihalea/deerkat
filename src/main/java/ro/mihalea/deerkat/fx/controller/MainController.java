package ro.mihalea.deerkat.fx.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Pair;
import lombok.extern.log4j.Log4j2;
import ro.mihalea.deerkat.exception.model.TransactionFieldException;
import ro.mihalea.deerkat.exception.model.TransactionParseException;
import ro.mihalea.deerkat.exception.processor.FileNotFoundException;
import ro.mihalea.deerkat.exception.processor.FileNotReadableException;
import ro.mihalea.deerkat.exception.repository.RepositoryConnectionException;
import ro.mihalea.deerkat.exception.repository.RepositoryCreateException;
import ro.mihalea.deerkat.exception.repository.RepositoryDeleteException;
import ro.mihalea.deerkat.exception.repository.RepositoryInitialisationException;
import ro.mihalea.deerkat.fx.ui.AlertFactory;
import ro.mihalea.deerkat.model.Transaction;
import ro.mihalea.deerkat.repository.CsvRepository;
import ro.mihalea.deerkat.repository.TransactionSqlRepository;
import ro.mihalea.deerkat.utility.HtmlProcessor;

import java.io.File;
import java.util.List;
import java.util.Optional;

/**
 * Controller for {@link ro.mihalea.deerkat.fx.ui.MainWindow}.
 */
@Log4j2
public class MainController {
    /**
     * Primary stage contained in the window
     */
    private Stage stage;

    /**
     * Processor used to parse HTML files
     */
    private final HtmlProcessor processor = new HtmlProcessor();

    /**
     * Repository used to export items to csv
     */
    private CsvRepository csvRepository;

    /**
     * SQL Repository used to save multiple imports in the database to increase the classifier's accuracy
     */
    private TransactionSqlRepository transactionSql;

    /**
     * Path where the sql repository should save its file
     */
    private final static String DATABASE_PATH = "deerkat.sqlite";

    /**
     * Factory used to create alert dialogs
     */
    private final AlertFactory alertFactory = new AlertFactory();

    /**
     * List of Transactions stored in the table
     */
    private final ObservableList<Transaction> tableData = FXCollections.observableArrayList();

    /**
     * JavaFX Table View used to display the transactions in a table
     */
    @FXML
    private TableView<Transaction> transactionsTable;

    /**
     * Table columns used to represent transactions
     */
    @FXML
    private TableColumn<Transaction, String> tcPostingDate;
    @FXML
    private TableColumn<Transaction, String> tcTransactionDate;
    @FXML
    private TableColumn<Transaction, String> tcDetails;
    @FXML
    private TableColumn<Transaction, String> tcAmount;
    @FXML
    private TableColumn<Transaction, String> tcCategory;

    /**
     * Button used to export table data to csv
     */
    @FXML
    private Button exportButton;

    /**
     * Progress bar used to monitor importing
     */
    @FXML
    private ProgressBar pbImport;

    /**
     * Initialise the main controller by instantiating the sql database
     */
    public MainController() {
        try {
            log.debug("Starting MainController");
            transactionSql = new TransactionSqlRepository(DATABASE_PATH);

            //TODO: Remove this once debugging is done
            transactionSql.nuke();
        } catch (RepositoryConnectionException e) {
            log.error("Failed to initialise MainController", e);
            System.exit(1);
        } catch (RepositoryDeleteException e) {
            e.printStackTrace();
        }
    }

    /**
     * Open a file chooser and import the file's contents into the table using {@link HtmlProcessor} for parsing
     */
    @FXML
    protected void importButton_Action() {
        log.info("User has begun the import action");

        // Open the file chooser dialog and let the user select an html file
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open statement");
        fileChooser.setSelectedExtensionFilter(new FileChooser.ExtensionFilter("HTML file", "html"));
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            // If a file has been selected, parse it and add it to the table data
            Task<Pair<Integer, Integer>> task = new Task<>() {
                @Override
                protected Pair<Integer, Integer> call() throws Exception {

                    List<Transaction> transactions = processor.parseTransactions(file.getAbsolutePath());

                    // Reset the progress bar and display it
                    pbImport.setVisible(true);

                    // Number of items processed sucessfully imported
                    int items = 0;

                    for (Transaction t : transactions) {
                        Optional<Integer> key = transactionSql.add(t);
                        if (key.isPresent()) {
                            t.setId(key.get());
                            tableData.add(t);
                            items++;
                        } else {
                            // A key should always be present, but catch this error anyways
                            log.error("Failed to import transaction because database did not return a key for " + t);
                        }

                        updateProgress(items, transactions.size());
                    }

                    // Enable the export function now that we have items in the database
                    exportButton.setDisable(false);

                    return new Pair<>(items, transactions.size());
                }


                @Override
                protected void succeeded() {
                    Pair<Integer, Integer> pair = this.getValue();
                    int items = pair.getKey();
                    int total = pair.getValue();

                    log.info("Successfully imported {} out of {} transaction from HTML", items, total);
                    alertFactory.create(
                            items == total ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR,
                            "Import finalised",
                            items + " out of " + total + " transactions have been imported"
                    ).showAndWait();

                    // Hide the progress bar again
                    pbImport.setVisible(false);
                }

                @Override
                protected void failed() {
                    try {
                        // I don't really know how to handle this better so this stays for now
                        //TODO: Find out how to handle these exceptions better
                        throw this.getException();
                    } catch (RepositoryCreateException e) {
                        log.warn("Failed to update local SQL database", e);

                        alertFactory.createError(
                                "Database error",
                                "Failed to update the local database with the imported transactions"
                        ).showAndWait();
                    } catch (FileNotFoundException | FileNotReadableException | TransactionFieldException | TransactionParseException e) {
                        log.warn("Failed to import file: " + file.getAbsolutePath(), e);

                        alertFactory.createError(
                                "Failed to import file",
                                "There was an error while importing your file: " + file.getAbsolutePath()
                        ).showAndWait();
                    } catch (Throwable throwable) {
                        log.warn("An unknown error occurred", throwable);

                        alertFactory.createError(
                                "Error",
                                "Failed to import your file"
                        );
                    }
                }
            };

            // Bind the task progress to the progress bar to keep it updated
            pbImport.progressProperty().bind(task.progressProperty());


            // Start the import thread
            new Thread(task).start();

        } else {
            log.info("User has cancelled the import action");
        }
    }

    /**
     * Export the transaction stored in the table to a CSV file picked by the user.
     */
    @FXML
    protected void exportButton_Action() {
        log.info("User tries to export table data to CSV");

        // If the repository is null, the user has not used this option before, create a new database connection
        if (csvRepository == null) {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Export to CSV");
            fileChooser.setSelectedExtensionFilter(new FileChooser.ExtensionFilter("CSV file", "csv"));

            File file = fileChooser.showSaveDialog(stage);

            if (file != null) {
                // Append .csv extension to the file if the user has not already done it
                String name = file.getName();
                if (!name.endsWith(".csv")) {
                    file = new File(file.getAbsolutePath() + " .csv");
                }

                try {
                    csvRepository = new CsvRepository(file.getAbsolutePath());
                } catch (RepositoryInitialisationException e) {
                    log.warn("Failed to initialise csv repository at: " + file.getAbsolutePath(), e);

                    alertFactory.createError(
                            "Export error",
                            "There was an error while creating your csv file at " + file.getAbsolutePath()
                    ).showAndWait();

                    return;
                }
            }
        }

        try {
            // Add all table data to the repository
            csvRepository.addAll(tableData);
        } catch (RepositoryCreateException e) {
            log.warn("Failed to add items to the csv repository", e);

            alertFactory.createError(
                    "Export error",
                    "Failed to add the transaction to you CSV file"
            ).showAndWait();
        }
    }

    /**
     * Debug method used to check correct data associations between the view and the repositories
     */
    @FXML
    protected void transactionsTable_MouseClick() {
        Transaction transaction = transactionsTable.getSelectionModel().getSelectedItem();

        if (transaction != null) {
            log.debug("Clicked on {}", transaction);
        }
    }

    /**
     * Initialise the table and set the stage
     *
     * @param stage Primary stage used in the window
     */
    public void initialise(Stage stage) {
        this.stage = stage;

        initialiseTable();
    }

    /**
     * Setup table columns and data bindings
     */
    private void initialiseTable() {
        tcPostingDate.setCellValueFactory(new PropertyValueFactory<>("postingDate"));
        tcTransactionDate.setCellValueFactory(new PropertyValueFactory<>("transactionDate"));
        tcDetails.setCellValueFactory(new PropertyValueFactory<>("details"));
        tcAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));


        transactionsTable.setItems(tableData);
    }


}
