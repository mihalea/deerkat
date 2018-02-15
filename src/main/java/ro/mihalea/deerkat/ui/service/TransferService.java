package ro.mihalea.deerkat.ui.service;

import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.concurrent.Task;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Pair;
import lombok.extern.log4j.Log4j2;
import ro.mihalea.deerkat.exception.model.TransactionFieldException;
import ro.mihalea.deerkat.exception.model.TransactionParseException;
import ro.mihalea.deerkat.exception.processor.FileNotFoundException;
import ro.mihalea.deerkat.exception.processor.FileNotReadableException;
import ro.mihalea.deerkat.exception.repository.RepositoryCreateException;
import ro.mihalea.deerkat.exception.repository.RepositoryDeleteException;
import ro.mihalea.deerkat.exception.repository.RepositoryInitialisationException;
import ro.mihalea.deerkat.model.Category;
import ro.mihalea.deerkat.model.Transaction;
import ro.mihalea.deerkat.repository.CategorySqlRepository;
import ro.mihalea.deerkat.repository.CsvRepository;
import ro.mihalea.deerkat.repository.TransactionSqlRepository;
import ro.mihalea.deerkat.ui.window.AlertFactory;
import ro.mihalea.deerkat.utility.HtmlProcessor;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service used to provide better encapsulation for importing and exporting transactions
 */
@Log4j2
public class TransferService {
    /**
     * Owner stage needed for instantiating dialogs
     */
    private Stage owner;

    /**
     * Processor used to parse transactions from an HTML file
     */
    private HtmlProcessor processor = new HtmlProcessor();

    /**
     * Factory used to create alerts and display them
     */
    private AlertFactory alertFactory;

    /**
     * Service used to update the progress bar
     */
    private StatusService statusService;

    /**
     * Repository used to export transactions to a csv file
     */
    private CsvRepository csvRepository;

    /**
     * Repository needed for initialising the csvRepository
     * Storing it is necessary as the CsvRepository gets initialised only at the user's input
     */
    private CategorySqlRepository categorySqlRepository;

    /**
     * Instantiate the transfer service, setting the stage owner needed for dialogs and the AlertFactory
     * needed for display messages
     * @param owner Stage owner needed for instantiating file choosers
     * @param alertFactory Alert factory used for displaying important message
     * @param statusService Status service used for displaying update information
     * @param categorySqlRepository Category repository used for initialising the csv repository for category resolution
     */
    public TransferService(Stage owner, AlertFactory alertFactory, StatusService statusService, CategorySqlRepository categorySqlRepository) {
        this.owner = owner;
        this.alertFactory = alertFactory;
        this.statusService = statusService;
        this.categorySqlRepository = categorySqlRepository;
    }

    /**
     * Import transactions from an HTML file containing a bank statement into the sql repository
     * @param repository Transaction repository used to save data on disk
     * @param table Table service used to insert transactions into the ui
     */
    public void importFile(TransactionSqlRepository repository, TableService table, Runnable onSuccess) {
        log.info("User has begun the import action");

        // Open the file chooser dialog and let the user select an html file
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open statement");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("HTML (*.html)", "*.html"));
        File file = fileChooser.showOpenDialog(owner);

        if (file != null) {
            // If a file has been selected, parse it and add it to the table data
            Task<Pair<Integer, Integer>> task = new Task<Pair<Integer, Integer>>() {
                @Override
                protected Pair<Integer, Integer> call() throws Exception {
                    statusService.displayProgress(true);
                    List<Transaction> transactions = processor.parseTransactions(file.getAbsolutePath());

                    // Number of items successfully imported
                    int successful = 0;

                    for (Transaction t : transactions) {
                        try {
                            Optional<Long> key = repository.add(t);
                            if (key.isPresent()) {
                                t.setId(key.get());
                                table.add(t);
                                successful++;
                            } else {
                                // A key should always be present, but catch this error anyways
                                log.error("Failed to import transaction because database did not return a key for " + t);
                            }
                        } catch (RepositoryCreateException e) {
                            log.debug("Skipping transaction as it is already found in the database: " + t);
                        }

                        updateProgress(successful, transactions.size());
                    }

                    return new Pair<>(successful, transactions.size());
                }


                @Override
                protected void succeeded() {
                    statusService.displayProgress(false);

                    onSuccess.run();

                    Pair<Integer, Integer> pair = this.getValue();
                    int successful = pair.getKey();
                    int total = pair.getValue();

                    log.info("Imported {} out of {} transaction from HTML", successful, total);

                    table.requestFocus();

                    if (successful == 0 && total != 0) {
                        alertFactory.createError("Import", "No transactions have been imported. \n" +
                                "They may already be in the database.").showAndWait();
                    } else {
                        statusService.showMessage(successful + " out of " + total + " transactions have been imported");
                    }
                }

                @Override
                protected void failed() {
                    statusService.displayProgress(false);
                    log.error("Failed to import transactions from HTML", this.getException());
                    alertFactory.createError(
                            "Import error",
                            "An error occurred while trying to import your file"
                    ).showAndWait();
                }
            };

            // Start the import thread
            new Thread(task).start();
            statusService.bindProgress(task.progressProperty());

        } else {
            log.info("User has cancelled the import action");
        }
    }

    /**
     * Export transactions to a comma separated value file
     */
    public void export(List<Transaction> transactions) {
        // Due to short-circuiting the exportCsv statement does not execute if initialiseCsvRepository returns false
        if (initialiseCsvRepository() && exportCsv(transactions)) {
            statusService.showMessage("Successfully exported the selected transaction to '" +
                    csvRepository.getFilePath().getFileName() +
                    "'"
            );
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
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Comma separated value (*.csv)", "*.csv"));

        File file = fileChooser.showSaveDialog(owner);

        if (file != null) {
            try {
                Path path = Paths.get(file.getAbsolutePath());
                /*
                If there is already a repository initialised with the same path, skip creating a new
                one and just nuke the existent one
                */
                if (csvRepository != null && csvRepository.getFilePath().equals(path)) {
                    log.debug("Skipping initialising csv repository with the same path, nuking it instead");
                    csvRepository.nuke();
                } else {
                    csvRepository = new CsvRepository(path);
                    csvRepository.setCategoryRepository(categorySqlRepository);
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
     * Export the data currently stored in the table to a csv file
     * @return Returns true if the export was a success
     */
    private boolean exportCsv(List<Transaction> transactions) {
        // The repository should only be null if the user pressed cancel
        if (csvRepository != null) {
            try {
                // Add all table data to the repository
                csvRepository.addAll(transactions);
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
}
