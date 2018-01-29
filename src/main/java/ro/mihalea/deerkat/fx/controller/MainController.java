package ro.mihalea.deerkat.fx.controller;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Background;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.Pair;
import lombok.extern.log4j.Log4j2;
import ro.mihalea.deerkat.classifier.ConfidenceLevel;
import ro.mihalea.deerkat.exception.model.TransactionFieldException;
import ro.mihalea.deerkat.exception.model.TransactionParseException;
import ro.mihalea.deerkat.exception.processor.FileNotFoundException;
import ro.mihalea.deerkat.exception.processor.FileNotReadableException;
import ro.mihalea.deerkat.exception.repository.*;
import ro.mihalea.deerkat.fx.ui.AlertFactory;
import ro.mihalea.deerkat.fx.ui.ClassifierDialog;
import ro.mihalea.deerkat.model.Category;
import ro.mihalea.deerkat.model.Transaction;
import ro.mihalea.deerkat.repository.CsvRepository;
import ro.mihalea.deerkat.repository.TransactionSqlRepository;
import ro.mihalea.deerkat.classifier.AbstractClassifier;
import ro.mihalea.deerkat.classifier.CategoryMatch;
import ro.mihalea.deerkat.classifier.FuzzyClassifier;
import ro.mihalea.deerkat.utility.HtmlProcessor;

import java.io.File;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

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
     * Initialise the main controller by instantiating the sql database
     */
    public MainController() {
        try {
            log.debug("Starting MainController");
            transactionSql = new TransactionSqlRepository();

            classifier = new FuzzyClassifier();

            //TODO: Remove this once debugging is done
            transactionSql.nuke();
        } catch (RepositoryConnectionException e) {
            log.error("Failed to initialise a controller", e);
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
                        Optional<Long> key = transactionSql.add(t);
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

        Alert alert = alertFactory.create(
                Alert.AlertType.CONFIRMATION,
                "Export confirmation",
                "There are still transactions that don't have a category. " +
                        "Are you sure you want to export them as they are?");
        if(noEmptyCategories() || alert.showAndWait().isPresent() && alert.getResult() == ButtonType.OK) {
            boolean result = initialiseCsvRepository();

            if(result) {
                exportCsv();
            }
        }




    }

    /**
     * Export the data imported into csv format
     */
    public void exportCsv() {
        // The repository should only be null if the user pressed cancel
        if(csvRepository != null) {
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
    }

    /**
     * Open a file chooser to select the location of the export file
     * @return It returns true if the user has selected a file and the repository has been successfully created
     */
    public boolean initialiseCsvRepository() {
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
                    return true;
                } catch (RepositoryInitialisationException e) {
                    log.warn("Failed to initialise csv repository at: " + file.getAbsolutePath(), e);

                    alertFactory.createError(
                            "Export error",
                            "There was an error while creating your csv file at " + file.getAbsolutePath()
                    ).showAndWait();
                }
            } else {
                log.info("User cancelled export");
            }
        } else {
            return true;
        }

        return false;
    }

    /**
     * Returns whether or not there are still empty transactions that haven't been categorised
     * @return True if all transactions have categories assigned
     */
    public boolean noEmptyCategories() {
        return tableData.stream().filter(td -> td.getCategory() == null).count() <= 0;
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
        initialiseClassifier();
    }

    /**
     * Inject model data into the classifier
     */
    private void initialiseClassifier() {
        try {
            List<Transaction> transactions = transactionSql.getAll();
            classifier.addModelList(transactions);
        } catch (RepositoryReadException e) {
            log.error("Failed to inject model data into the classifier", e);
            alertFactory.createError(
                    "Initialisation error",
                    "Failed to initialise the classifier. Check the log for more details"
            );
        }
    }

    /**
     * Setup table columns and data bindings
     */
    private void initialiseTable() {
        tcPostingDate.setCellValueFactory(new PropertyValueFactory<>("postingDate"));
        tcTransactionDate.setCellValueFactory(new PropertyValueFactory<>("transactionDate"));
        tcDetails.setCellValueFactory(new PropertyValueFactory<>("details"));
        tcAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
        tcCategory.setCellValueFactory(new PropertyValueFactory<>("category"));

        tcCategory.setCellFactory(new Callback<>() {
            @Override
            public TableCell<Transaction, Category> call(TableColumn<Transaction, Category> param) {
                TableCell<Transaction, Category> cell = new TableCell<>() {
                    @Override
                    protected void updateItem(Category item, boolean empty) {
                        super.updateItem(item, empty);


                        getStyleClass().clear();
                        setText("");

                        if(empty || item == null) {
                            if(this.getTableRow().getItem() != null){
                                setText("Set category");
                                getStyleClass().add("no-category");
                            }
                        } else {
                            setText(item.getTitle());
                            Transaction transaction = tableData.get(this.getIndex());

                            if(transaction != null) {
                                switch (transaction.getConfidenceLevel()) {
                                    case NEED_CONFIRMATION:
                                        getStyleClass().add("need-confirmation");
                                        break;
                                    case PRETTY_SURE:
                                        getStyleClass().add("pretty-sure");
                                        break;
                                    case USER_SET:
                                        getStyleClass().add("user-set");
                                        break;
                                }
                            }
                        }
                    }
                };

                cell.setOnMouseClicked(event -> {
                    Transaction transaction = cell.getTableRow().getItem();

                    ClassifierDialog dialog = new ClassifierDialog(classifier, transaction);
                    dialog.showAndWait();
                    Category category = dialog.getResult();
                    if(category != null) {
                        transaction.setCategory(category);
                        transaction.setConfidenceLevel(ConfidenceLevel.USER_SET);
                        classifier.addModelItem(transaction);
                        searchPerfectMatches();
                        transactionsTable.refresh();
                    }
                });

                cell.setOnMouseEntered(event -> stage.getScene().setCursor(Cursor.HAND));
                cell.setOnMouseExited(event -> stage.getScene().setCursor(Cursor.DEFAULT));

                return cell;
            }
        });

        transactionsTable.setItems(tableData);
    }

    private void searchPerfectMatches() {
        boolean updated = false;

        for (Transaction transaction : tableData) {
            if(transaction.getCategory() == null) {
                Optional<CategoryMatch> best = classifier.getBest(transaction);
                if (best.isPresent()) {
                    CategoryMatch match = best.get();
                    if (match.getSimilarity() > AbstractClassifier.AUTOMATIC_MATCH_VALUE) {
                        classifier.addModelItem(transaction);
                        transaction.setCategory(match.getCategory());
                        transaction.setConfidenceLevel(ConfidenceLevel.PRETTY_SURE);
                        updated = true;
                        log.info("Automatically matched {} with {}", transaction, match);
                    } else if (match.getSimilarity() > AbstractClassifier.NEED_CONFIRMATION_VALUE) {
                        transaction.setCategory(match.getCategory());
                        transaction.setConfidenceLevel(ConfidenceLevel.NEED_CONFIRMATION);
                        log.info("Confirmation needed for matching {} with {}", transaction, match);
                    }
                }
            }
        }

        if(updated) {
            transactionsTable.refresh();
        }
    }


}
