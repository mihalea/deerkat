package ro.mihalea.deerkat.fx.controller;

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
import ro.mihalea.deerkat.fx.ui.AlertFactory;
import ro.mihalea.deerkat.fx.ui.ClassifierDialog;
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
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
     * SQL repository used to retrieve categories from the database
     */
    private CategorySqlRepository categorySql;

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
     * Open a file chooser and import the file's contents into the table using {@link HtmlProcessor} for parsing
     */
    @FXML
    protected void importButton_Action() {
        log.info("User has begun the import action");

        // Open the file chooser dialog and let the user select an html file
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open statement");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("HTML file (*.html)", "*.html"));
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            // If a file has been selected, parse it and add it to the table data
            Task<Pair<Integer, Integer>> task = new Task<Pair<Integer, Integer>>() {
                @Override
                protected Pair<Integer, Integer> call() throws Exception {

                    List<Transaction> transactions = processor.parseTransactions(file.getAbsolutePath());

                    // Reset the progress bar and display it
                    pbImport.setVisible(true);

                    // Number of items processed sucessfully imported
                    int items = 0;

                    for (Transaction t : transactions) {
                        try {
                            Optional<Long> key = transactionSql.add(t);
                            if (key.isPresent()) {
                                t.setId(key.get());
                                tableData.add(t);
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
                        lbStatus.setText(items + " out of " + total + " transactions have been imported");
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

        if (tableData.size() == 0 ||
                (confirmation.showAndWait().isPresent() && confirmation.getResult() == ButtonType.OK)) {
            try {
                List<Transaction> transactions = transactionSql.getAll(categorySql);
                tableData.clear();
                tableData.addAll(transactions);
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
        if (noEmptyCategories() || alert.showAndWait().isPresent() && alert.getResult() == ButtonType.OK) {
            boolean result = initialiseCsvRepository();

            if (result) {
                exportCsv();
            }
        }
    }

    /**
     * Export the data imported into csv format
     */
    private void exportCsv() {
        // The repository should only be null if the user pressed cancel
        if (csvRepository != null) {
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
     *
     * @return It returns true if the user has selected a file and the repository has been successfully created
     */
    private boolean initialiseCsvRepository() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export to CSV");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV file (*.csv)", "*.csv"));

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
     * Returns whether or not there are still empty transactions that haven't been categorised
     *
     * @return True if all transactions have categories assigned
     */
    private boolean noEmptyCategories() {
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
        initialiseWindowListener();
    }

    private void initialiseWindowListener() {
        stage.addEventHandler(WindowEvent.WINDOW_SHOWN, event -> {
            try {
                List<Transaction> withoutCategory = transactionSql.getAll(categorySql).stream()
                        .filter(t -> t.getCategory() == null)
                        .collect(Collectors.toList());

                if (withoutCategory.size() > 0) {
                    Alert alert = alertFactory.create(
                            Alert.AlertType.CONFIRMATION,
                            "Load previous transactions",
                            "It appears that you've closed the application without categorising all transactions\n" +
                                    "Do you want to continue working on them?");

                    if (alert.showAndWait().isPresent() && alert.getResult() == ButtonType.OK) {
                        tableData.addAll(withoutCategory);
                    }
                }
            } catch (RepositoryReadException e) {
                log.error("Failed to retrieve transactions", e);
            }
        });
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
     * Setup table columns and data bindings
     */
    private void initialiseTable() {
        tableData.addListener((ListChangeListener<Transaction>) c -> {
            if(c.next() && c.getAddedSize() > 0) {
                searchMatches();
            }
        });

        tcPostingDate.setCellValueFactory(new PropertyValueFactory<>("postingDate"));
        tcTransactionDate.setCellValueFactory(new PropertyValueFactory<>("transactionDate"));
        tcDetails.setCellValueFactory(new PropertyValueFactory<>("details"));
        tcAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
        tcCategory.setCellValueFactory(new PropertyValueFactory<>("category"));


        tcCategory.setCellFactory(new Callback<TableColumn<Transaction, Category>, TableCell<Transaction, Category>>() {
            @Override
            public TableCell<Transaction, Category> call(TableColumn<Transaction, Category> param) {
                TableCell<Transaction, Category> cell = new TableCell<Transaction, Category>() {
                    @Override
                    protected void updateItem(Category item, boolean empty) {
                        super.updateItem(item, empty);


                        getStyleClass().clear();
                        setText("");
                        setTooltip(null);

                        if (empty || item == null) {
                            if (this.getTableRow().getItem() != null) {
                                setText("Set category");
                                getStyleClass().add("no-category");
                            }
                        } else {
                            setText(item.getTitle());
                            Transaction transaction = tableData.get(this.getIndex());

                            if (transaction != null) {
                                switch (transaction.getConfidenceLevel()) {
                                    case NEED_CONFIRMATION:
                                        getStyleClass().add("need-confirmation");
                                        setTooltip(new Tooltip("Medium possibility of a correct match"));
                                        break;
                                    case PRETTY_SURE:
                                        getStyleClass().add("pretty-sure");
                                        setTooltip(new Tooltip("High possibility of a correct match"));
                                        break;
                                    case USER_SET:
                                        getStyleClass().add("user-set");
                                        setTooltip(new Tooltip("User-set category"));
                                        break;
                                }
                            }
                        }
                    }
                };

                cell.setOnMouseClicked(event -> {
                    Transaction transaction = (Transaction) cell.getTableRow().getItem();

                    ClassifierDialog dialog = new ClassifierDialog(classifier, transaction);
                    dialog.showAndWait();
                    Category category = dialog.getResult();
                    if (category != null) {
                        transaction.setCategory(category);
                        transaction.setConfidenceLevel(ConfidenceLevel.USER_SET);
                        classifier.addModelItem(transaction);
                        searchMatches();
                        transactionsTable.refresh();

                        try {
                            transactionSql.update(transaction);
                        } catch (RepositoryUpdateException e) {
                            log.error("Failed to update transaction after categorisation: " + transaction, e);
                            lbStatus.setText("Failed to update transaction in the database");
                        }
                    }
                });

                cell.setOnMouseEntered(event -> stage.getScene().setCursor(Cursor.HAND));
                cell.setOnMouseExited(event -> stage.getScene().setCursor(Cursor.DEFAULT));

                return cell;
            }
        });

        transactionsTable.setItems(tableData);
    }

    /**
     * Analyse the current data set and try to categorise them automatically
     */
    private void searchMatches() {
        boolean updated = false;

        // Count the number of transactions found that are a perfect match
        int perfect = 0;
        //Count the number of transactions found may need user confirmation
        int needConfirmation = 0;

        for (Transaction transaction : tableData) {
            if (transaction.getCategory() == null) {
                Optional<CategoryMatch> best = classifier.getBest(transaction);
                if (best.isPresent()) {
                    CategoryMatch match = best.get();
                    try {
                        if (match.getSimilarity() > AbstractClassifier.AUTOMATIC_MATCH_VALUE) {
                            classifier.addModelItem(transaction);
                            transaction.setCategory(match.getCategory());
                            transaction.setConfidenceLevel(ConfidenceLevel.PRETTY_SURE);
                            transactionSql.update(transaction);
                            updated = true;
                            perfect++;
                            log.info("Automatically matched {} with {}", transaction, match);
                        } else if (match.getSimilarity() > AbstractClassifier.NEED_CONFIRMATION_VALUE) {
                            transaction.setCategory(match.getCategory());
                            transaction.setConfidenceLevel(ConfidenceLevel.NEED_CONFIRMATION);
                            transactionSql.update(transaction);
                            log.info("Confirmation needed for matching {} with {}", transaction, match);
                            needConfirmation++;
                            updated = true;
                        }
                    } catch (RepositoryUpdateException e) {
                        log.error("Failed to update transaction in the database: " + transaction, e);
                    }
                }
            }
        }

        // Construct status messages
        if (perfect > 0 || needConfirmation > 0) {
            String message = "";

            if (perfect > 0) {
                message = perfect + " perfect match";
                if (perfect != 1) {
                    message += "es";
                }
            }

            if (perfect > 0 && needConfirmation > 0) {
                message += " and ";
            }

            if (needConfirmation > 0) {
                message += needConfirmation + " possible match";
                if (needConfirmation != 1) {
                    message += "es";
                }
            }

            message += " " + (perfect + needConfirmation == 1 ? "has" : "have") + " been found";

            lbStatus.setText(message);
        }

        if (updated) {
            transactionsTable.refresh();
        }
    }


}
