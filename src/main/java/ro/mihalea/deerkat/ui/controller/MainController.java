package ro.mihalea.deerkat.ui.controller;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import lombok.extern.log4j.Log4j2;
import ro.mihalea.deerkat.exception.repository.*;
import ro.mihalea.deerkat.ui.service.StatusService;
import ro.mihalea.deerkat.ui.service.TableService;
import ro.mihalea.deerkat.ui.service.TransferService;
import ro.mihalea.deerkat.ui.window.AlertFactory;
import ro.mihalea.deerkat.model.Category;
import ro.mihalea.deerkat.model.Transaction;
import ro.mihalea.deerkat.repository.CategorySqlRepository;
import ro.mihalea.deerkat.repository.CsvRepository;
import ro.mihalea.deerkat.repository.TransactionSqlRepository;
import ro.mihalea.deerkat.classifier.AbstractClassifier;
import ro.mihalea.deerkat.classifier.FuzzyClassifier;
import ro.mihalea.deerkat.utility.HtmlProcessor;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller for {@link ro.mihalea.deerkat.ui.window.MainWindow}.
 */
@Log4j2
public class MainController {

    //region FXML Variables
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
     * Button used to export table data to csv
     */
    @FXML
    private Button exportButton;

    /**
     * Progress bar used to monitor importing
     */
    @FXML
    private ProgressBar progressBar;

    /**
     * Label on the bottom in the status bar used to display messages that aren't that important
     */
    @FXML
    private Label lbStatus;
    //endregion


    /**
     * Store the fuzzy classifier here to reduce the number of SQL queries and inject it by dependencies
     */
    private AbstractClassifier classifier;

    /**
     * Primary stage contained in the window
     */
    private Stage stage;

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
     * Service used to to manage a table view
     */
    private TableService table;

    /**
     * Service used to import and export transactions
     */
    private TransferService transfer;

    /**
     * Service used to update the status bar text and progress bar
     */
    private StatusService status;

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

        status = new StatusService(lbStatus, progressBar);

        table = new TableService(this, transactionsTable, alertFactory,
                transactionSql, categorySql, classifier, status);
        table.setColumns(tcPostingDate, tcTransactionDate, tcDetails, tcAmount, tcCategory);
        table.initialise();

        transfer = new TransferService(stage, alertFactory, status, categorySql);

        initialiseWindowListener();
        initialiseClassifier();
    }

    /**
     * Add window listeners to the table asking the user whether to reload previous transactions without a category
     */
    private void initialiseWindowListener() {
        stage.addEventHandler(WindowEvent.WINDOW_SHOWN, event -> {
            try {
                // Query all transactions without a category that are also not inflow
                List<Transaction> withoutCategory = transactionSql.getAll(categorySql).stream()
                        .filter(t -> t.getCategory() == null)
                        .filter(t -> !t.getInflow())
                        .collect(Collectors.toList());

                // If any such transactions are found ask the user what he wants to do
                if (withoutCategory.size() > 0) {
                    Alert alert = alertFactory.create(
                            Alert.AlertType.CONFIRMATION,
                            "Load previous transactions",
                            "It appears that you've closed the application without categorising all transactions\n" +
                                    "Do you want to continue working on them?");

                    if (alert.showAndWait().isPresent() && alert.getResult() == ButtonType.OK) {
                        table.addAll(withoutCategory);
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
     * Open a file chooser and import the file's contents into the table using {@link HtmlProcessor} for parsing
     */
    @FXML
    protected void importButton_Action() {
        transfer.importFile(transactionSql, table);
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
                alertFactory.createError("Error", "Failed to load previous transactions").showAndWait();
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
            transfer.export(table.getAll());
        }
    }

    /**
     * Debug method used to check correct data associations between the view and the repositories
     */
    @FXML
    protected void transactionsTable_MouseClick() {
        table.getSelected().ifPresent(t -> log.debug("Clicked on {}", t));
    }

    /**
     * Return the stage for any services that may use but don't need to store it
     * @return Main stage
     */
    public Stage getStage() {
        return stage;
    }
}
