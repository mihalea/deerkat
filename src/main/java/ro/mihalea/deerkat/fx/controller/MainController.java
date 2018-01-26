package ro.mihalea.deerkat.fx.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import lombok.extern.log4j.Log4j2;
import ro.mihalea.deerkat.exception.model.TransactionFieldException;
import ro.mihalea.deerkat.exception.model.TransactionParseException;
import ro.mihalea.deerkat.exception.processor.FileNotFoundException;
import ro.mihalea.deerkat.exception.processor.FileNotReadableException;
import ro.mihalea.deerkat.exception.repository.RepositoryCreateException;
import ro.mihalea.deerkat.exception.repository.RepositoryInitialisationException;
import ro.mihalea.deerkat.fx.ui.AlertFactory;
import ro.mihalea.deerkat.model.Transaction;
import ro.mihalea.deerkat.repository.CsvRepository;
import ro.mihalea.deerkat.utility.HtmlProcessor;

import java.io.File;
import java.util.List;

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
    private HtmlProcessor processor = new HtmlProcessor();

    /**
     * Repository used to export items to csv
     */
    private CsvRepository csvRepository;

    /**
     * Factory used to create alert dialogs
     */
    private AlertFactory alertFactory = new AlertFactory();

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
     * Open a file chooser and import the file's contents into the table using {@link HtmlProcessor} for parsing
     */
    @FXML
    protected void importButton_Click() {
        log.info("User tries to import a statement");

        // Open the file chooser dialog and let the user select an html file
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open statement");
        fileChooser.setSelectedExtensionFilter(new FileChooser.ExtensionFilter("HTML file", "html"));
        File file = fileChooser.showOpenDialog(stage);

        if(file != null) {
            // If a file has been selected, parse it and add it to the table data
            try {
                List<Transaction> transactions = processor.parseTransactions(file.getAbsolutePath());
                tableData.addAll(transactions);
                exportButton.setDisable(false);
            } catch (FileNotFoundException | FileNotReadableException | TransactionFieldException | TransactionParseException e) {
                log.warn("Failed to import file: " + file.getAbsolutePath(), e);

                alertFactory.createError(
                        "Failed to import file",
                        "There was an error while importing your file: " + file.getAbsolutePath()
                ).showAndWait();
            }
        }
    }

    /**
     * Export the transaction stored in the table to a CSV file picked by the user.
     */
    @FXML
    protected void exportButton_Click() {
        log.info("User tries to export table data to CSV");

        // If the repository is null, the user has not used this option before, create a new database connection
        if(csvRepository == null) {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Export to CSV");
            fileChooser.setSelectedExtensionFilter(new FileChooser.ExtensionFilter("CSV file", "csv"));

            File file = fileChooser.showSaveDialog(stage);

            if(file != null) {
                // Append .csv extension to the file if the user has not already done it
                String name = file.getName();
                if(!name.endsWith(".csv")) {
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
     * Initialise the table and set the stage
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
