package ro.mihalea.deerkat.ui.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Callback;
import javafx.util.Pair;
import lombok.extern.log4j.Log4j2;
import ro.mihalea.deerkat.classifier.ConfidenceLevel;
import ro.mihalea.deerkat.exception.model.TransactionFieldException;
import ro.mihalea.deerkat.exception.model.TransactionParseException;
import ro.mihalea.deerkat.exception.processor.FileNotFoundException;
import ro.mihalea.deerkat.exception.processor.FileNotReadableException;
import ro.mihalea.deerkat.exception.repository.*;
import ro.mihalea.deerkat.ui.service.TableService;
import ro.mihalea.deerkat.ui.window.AlertFactory;
import ro.mihalea.deerkat.ui.window.ClassifierDialog;
import ro.mihalea.deerkat.model.Category;
import ro.mihalea.deerkat.model.Transaction;
import ro.mihalea.deerkat.repository.CategorySqlRepository;
import ro.mihalea.deerkat.repository.CsvRepository;
import ro.mihalea.deerkat.repository.TransactionSqlRepository;
import ro.mihalea.deerkat.classifier.AbstractClassifier;
import ro.mihalea.deerkat.classifier.CategoryMatch;
import ro.mihalea.deerkat.classifier.FuzzyClassifier;
import ro.mihalea.deerkat.utility.HtmlProcessor;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Controller for {@link ro.mihalea.deerkat.ui.window.MainWindow}.
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
     * SQL repository used to retrieve categories from the database
     */
    private CategorySqlRepository categorySql;

    /**
     * Factory used to create alert dialogs
     */
    private final AlertFactory alertFactory = new AlertFactory();

    /**
     * JavaFX Table View used to display the transactions in a table
     */
    @FXML
    private TableView<Transaction> transactionsTable;

    /**
     * Service used to to manage a table view
     */
    private TableService table;

    /**
     * Table columns used to represent transactions
     */
    @FXML
    private TableColumn<Transaction, LocalDate> tcPostingDate;
    @FXML
    private TableColumn<Transaction, LocalDate> tcTransactionDate;
    @FXML
    private TableColumn<Transaction, String> tcDetails;
    @FXML
    private TableColumn<Transaction, Double> tcAmount;
    @FXML
    private TableColumn<Transaction, Category> tcCategory;

    /**
     * Store the fuzzy classifier here to reduce the number of SQL queries and inject it by dependencies
     */
    private AbstractClassifier classifier;

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
     * Label on the bottom in the status bar used to display messages that aren't that important
     */
    @FXML
    private Label lbStatus;

    /**
     * Initialise the main controller by instantiating the sql database
     */
    public MainController() {
        try {
            log.debug("Starting MainController");
            transactionSql = new TransactionSqlRepository();
            categorySql = new CategorySqlRepository();
            classifier = new FuzzyClassifier();
        } catch (RepositoryConnectionException e) {
            log.error("Failed to initialise a controller", e);
            System.exit(1);
        }
    }

    /**
     * Initialise the table and set the stage
     *
     * @param stage Primary stage used in the window
     */
    public void initialise(Stage stage) {
        this.stage = stage;

        Platform.runLater(() -> alertFactory.setOwner(stage.getScene().getWindow()));

        initaliseTableService();
        initialiseClassifier();
    }

    private void initaliseTableService() {
        table = new TableService(this, transactionsTable, alertFactory, transactionSql, categorySql, classifier);
        table.setColumns(tcPostingDate, tcTransactionDate, tcDetails, tcAmount, tcCategory);
        table.initialise();
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
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("HTML (*.html)", "*.html"));
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            // If a file has been selected, parse it and add it to the table data
            Task<Pair<Integer, Integer>> task = new Task<Pair<Integer, Integer>>() {
                @Override
                protected Pair<Integer, Integer> call() throws Exception {

                    List<Transaction> transactions = processor.parseTransactions(file.getAbsolutePath());

                    // Reset the progress bar and display it
                    pbImport.setVisible(true);

                    // Number of items successfully imported
                    int items = 0;

                    for (Transaction t : transactions) {
                        try {
                            Optional<Long> key = transactionSql.add(t);
                            if (key.isPresent()) {
                                t.setId(key.get());
                                table.add(t);
                                items++;
                            } else {
                                // A key should always be present, but catch this error anyways
                                log.error("Failed to import transaction because database did not return a key for " + t);
                            }
                        } catch (RepositoryCreateException e) {
                            log.debug("Skipping transaction as it is already found in the database: " + t);
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

                    if (items == 0 && total != 0) {
                        alertFactory.createError("Import", "No transactions have been imported. " +
                                "They may already be in the database.").showAndWait();
                    } else {
                        updateStatus(items + " out of " + total + " transactions have been imported");
                    }

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
     * Load the previously imported transaction into the table
     */
    @FXML
    protected void btnPrevious_Action() {
        Alert confirmation = alertFactory.create(
                Alert.AlertType.CONFIRMATION,
                "Clear table",
                "Are you sure you want to discard the current transactions?"
        );

        if (table.isEmpty() ||
                (confirmation.showAndWait().isPresent() && confirmation.getResult() == ButtonType.OK)) {
            try {
                List<Transaction> transactions = transactionSql.getAll(categorySql);
                table.clear();
                table.addAll(transactions);
                exportButton.setDisable(false);
            } catch (RepositoryReadException e) {
                log.error("Failed to import database transactions into the table", e);
                alertFactory.createError("Error", "Failed to load previous transactions");
            }
        }
    }

    /**
     * Export the transaction stored in the table to a CSV file picked by the user.
     */
    @FXML
    protected void exportButton_Action() {
        log.info("User tries to export table data to CSV");

        Alert alert = alertFactory.create(
                Alert.AlertType.CONFIRMATION,
                "Export confirmation",
                "There are still transactions that don't have a category. " +
                        "Are you sure you want to export them as they are?");
        if (!table.hasEmptyCategories() || alert.showAndWait().isPresent() && alert.getResult() == ButtonType.OK) {
            boolean result = initialiseCsvRepository();

            // Due to short-circuiting the exportCsv statement does not execute if 'result' is false
            if (result && exportCsv()) {
                this.updateStatus("Successfully exported the selected transaction to '" +
                        csvRepository.getFilePath().getFileName() +
                        "'"
                );
            }
        }
    }

    /**
     * Export the data imported into csv format
     * @return Returns true if the export was a success
     */
    private boolean exportCsv() {
        // The repository should only be null if the user pressed cancel
        if (csvRepository != null) {
            try {
                // Add all table data to the repository
                csvRepository.addAll(table.getAll());
                return true;
            } catch (RepositoryCreateException e) {
                log.warn("Failed to add items to the csv repository", e);

                alertFactory.createError(
                        "Export error",
                        "Failed to add the transaction to you CSV file"
                ).showAndWait();
            }
        }

        return false;
    }

    /**
     * Open a file chooser to select the location of the export file
     *
     * @return It returns true if the user has selected a file and the repository has been successfully created
     */
    private boolean initialiseCsvRepository() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export to CSV");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Comma separated value (*.csv)", "*.csv"));

        File file = fileChooser.showSaveDialog(stage);

        if (file != null) {
            // Append .csv extension to the file if the user has not already done it
            String name = file.getName();
            if (!name.endsWith(".csv")) {
                file = new File(file.getAbsolutePath() + ".csv");
            }

            try {
                Path path = Paths.get(file.getAbsolutePath());
                if (csvRepository != null && csvRepository.getFilePath().equals(path)) {
                    log.debug("Skipping initialising csv repository with the same path, nuking it instead");
                    csvRepository.nuke();
                } else {
                    csvRepository = new CsvRepository(path);
                    csvRepository.setCategoryRepository(categorySql);
                }

                return true;
            } catch (RepositoryInitialisationException | RepositoryDeleteException e) {
                log.warn("Failed to initialise csv repository at: " + file.getAbsolutePath(), e);

                alertFactory.createError(
                        "Export error",
                        "There was an error while creating your csv file at " + file.getAbsolutePath()
                ).showAndWait();
            }
        } else {
            log.info("User cancelled export");
        }

        return false;
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
     * Inject model data into the classifier
     */
    private void initialiseClassifier() {
        try {
            // Add only transaction that have been categorised
            classifier.addModelList(transactionSql.getAll(categorySql).stream()
                    .filter(t -> t.getCategory() != null)
                    .collect(Collectors.toList()));

        } catch (RepositoryReadException e) {
            log.error("Failed to inject model data into the classifier", e);
            alertFactory.createError(
                    "Initialisation error",
                    "Failed to initialise the classifier. Check the log for more details"
            );
        }
    }

    /**
     * Update the label in the status bar, setting the text and making it red if it's displaying an error
     * @param text Message to be displayed
     * @param error Whether or not the message is meant to be an error
     */
    public void updateStatus(String text, boolean error) {
        lbStatus.setText(text);
        if(error) {
            lbStatus.getStyleClass().add("status-error");
        } else {
            lbStatus.getStyleClass().clear();
        }
    }

    /**
     * Update the label in the status bar to a normal message
     * @param text Message to be displayed
     */
    public void updateStatus(String text) {
        this.updateStatus(text, false);
    }

    /**
     * Return the stage for any services that may use but don't need to store it
     * @return Main stage
     */
    public Stage getStage() {
        return stage;
    }
}
