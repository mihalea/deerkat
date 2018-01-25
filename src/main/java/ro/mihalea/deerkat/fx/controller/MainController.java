package ro.mihalea.deerkat.fx.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
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
import ro.mihalea.deerkat.model.Transaction;
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
     * Open a file chooser and import the file's contents into the table using {@link HtmlProcessor} for parsing
     * @param event Event sent by JavaFX
     */
    @FXML
    protected void importButtonAction(ActionEvent event) {
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
                tableData.clear();
                tableData.addAll(transactions);
            } catch (FileNotFoundException | FileNotReadableException | TransactionFieldException | TransactionParseException e) {
                log.warn("Failed to import file: " + file.getAbsolutePath(), e);
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Failed to import file");
                alert.setHeaderText(null);
                alert.setContentText("There was an error while importing your file: " + file.getAbsolutePath());
                alert.showAndWait();
            }
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
