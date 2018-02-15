package ro.mihalea.deerkat.ui.service;

import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.stage.WindowEvent;
import javafx.util.Callback;
import lombok.extern.log4j.Log4j2;
import ro.mihalea.deerkat.classifier.AbstractClassifier;
import ro.mihalea.deerkat.classifier.CategoryMatch;
import ro.mihalea.deerkat.classifier.ConfidenceLevel;
import ro.mihalea.deerkat.exception.repository.RepositoryReadException;
import ro.mihalea.deerkat.exception.repository.RepositoryUpdateException;
import ro.mihalea.deerkat.model.Category;
import ro.mihalea.deerkat.model.Transaction;
import ro.mihalea.deerkat.repository.CategorySqlRepository;
import ro.mihalea.deerkat.repository.TransactionSqlRepository;
import ro.mihalea.deerkat.ui.controller.MainController;
import ro.mihalea.deerkat.ui.window.AlertFactory;
import ro.mihalea.deerkat.ui.window.ClassifierDialog;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service used to control a JFX TableView and provide better encapsulation
 */
@Log4j2
public class TableService {
    /**
     * Controller parent of this instance
     */
    private final MainController controller;

    /**
     * JFX TableView used to store transactions
     */
    private final TableView<Transaction> table;

    /**
     * Alert factory used to create and display alerts
     */
    private AlertFactory alertFactory;

    /**
     * Sql repository used to save transactions to the disk
     */
    private TransactionSqlRepository transactionSql;

    /**
     * Transaction classifier used to identity possible categories based on the details
     */
    private AbstractClassifier classifier;

    /**
     * Service used to update the status bar
     */
    private StatusService statusService;

    /**
     * Table view data model storing transactions
     */
    private ObservableList<Transaction> model = FXCollections.observableArrayList();

    /**
     * Columns used to display data from a transaction
     */
    private TableColumn<Transaction, LocalDate> tcPostingDate;
    private TableColumn<Transaction, LocalDate> tcTransactionDate;
    private TableColumn<Transaction, String> tcDetails;
    private TableColumn<Transaction, Double> tcAmount;
    private TableColumn<Transaction, Category> tcCategory;


    /**
     * Create the table service needed to manage a TableView
     *
     * @param controller     MainController used to manage the FXML
     * @param table          TableView received from the loader
     * @param alertFactory   AlertFactory instantiated with the controller as the owner
     * @param transactionSql SQL Repository used to store transactions
     * @param classifier     Classifier used to suggest categories for transactions
     */
    public TableService(MainController controller, TableView<Transaction> table, AlertFactory alertFactory,
                        TransactionSqlRepository transactionSql,
                        AbstractClassifier classifier, StatusService statusService) {
        this.controller = controller;
        this.table = table;
        this.alertFactory = alertFactory;
        this.transactionSql = transactionSql;
        this.classifier = classifier;
        this.statusService = statusService;
    }

    /**
     * Set all the columns from the table
     *
     * @param postingDate     Column containing the posting date
     * @param transactionDate Column containing the transaction date
     * @param details         Column containing the details
     * @param amount          Column containing the amount
     * @param category        Column containing the category
     */
    public void setColumns(TableColumn<Transaction, LocalDate> postingDate, TableColumn<Transaction, LocalDate> transactionDate,
                           TableColumn<Transaction, String> details, TableColumn<Transaction, Double> amount,
                           TableColumn<Transaction, Category> category) {
        this.tcPostingDate = postingDate;
        this.tcTransactionDate = transactionDate;
        this.tcDetails = details;
        this.tcAmount = amount;
        this.tcCategory = category;

    }

    /**
     * Setup and initialise all variables that are need for this instance
     */
    public void initialise() {
        initialiseModel();
        initialiseColumns();
        initialiseKeyListener();
    }

    /**
     * Enable users to open the classifier dialog by pressing enter on a selected transaction
     */
    private void initialiseKeyListener() {
        table.setOnKeyReleased(ke -> {
            if (ke.getCode() == KeyCode.ENTER && !table.getSelectionModel().isEmpty()) {
                Transaction transaction = table.getSelectionModel().getSelectedItem();
                // This should never be null, but have this sanity check
                if (transaction != null) {
                    handleCategoryClick(transaction);
                }
            }
        });
    }

    /**
     * Group varying confidence levels into groups so that they can be sorted by the color hint
     * @param transaction Transaction that needs to be sorted
     * @return ConfindenceLevel values
     */
    private Integer getConfidenceLevel(Transaction transaction) {
        if(transaction.getInflow()) {
            return ConfidenceLevel.USER_SET + 1;
        } else if(transaction.getConfidence() >= ConfidenceLevel.USER_SET) {
            return ConfidenceLevel.USER_SET;
        } else if (transaction.getConfidence() >= ConfidenceLevel.PRETTY_SURE) {
            return ConfidenceLevel.PRETTY_SURE;
        } else if (transaction.getConfidence() >= ConfidenceLevel.NEED_CONFIRMATION) {
            return ConfidenceLevel.NEED_CONFIRMATION;
        } else {
            return ConfidenceLevel.NONE;
        }

    }

    /**
     * Sort the underlying table view model based on confidence levels, category names, dates and details
     */
    private void sortModel() {
        FXCollections.sort(
                model,
                Comparator.comparing(t -> this.getConfidenceLevel((Transaction) t)).reversed()
                        .thenComparing(t -> {
                            Transaction transaction = (Transaction) t;
                            if(transaction.getCategory() == null) {
                                return "";
                            } else {
                                return transaction.getCategory().getTitle();
                            }
                        })
                        .thenComparing(t -> ((Transaction) t).getPostingDate())
                        .thenComparing(t -> ((Transaction) t).getTransactionDate())
                        .thenComparing(t -> ((Transaction) t).getDetails())
        );
        log.debug("Sorting triggered");
    }

    /**
     * Setup listeners for the data model used to trigger automatic category matching
     */
    private void initialiseModel() {
        model.addListener((ListChangeListener<Transaction>) c -> {
            while (c.next()) {
                if(c.wasAdded() || c.wasRemoved() || c.wasAdded()) {
                    sortModel();
                }
                if (c.wasAdded()) {
                    log.debug("One or more items have been added to the table");
                    for (Transaction t : c.getAddedSubList()) {
                        searchMatches(t);
                    }
                }


            }


        });
    }

    /**
     * Setup table columns and data bindings
     */
    private void initialiseColumns() {
        tcPostingDate.setCellValueFactory(new PropertyValueFactory<>("postingDate"));
        tcTransactionDate.setCellValueFactory(new PropertyValueFactory<>("transactionDate"));
        tcDetails.setCellValueFactory(new PropertyValueFactory<>("details"));
        tcAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
        tcCategory.setCellValueFactory(new PropertyValueFactory<>("category"));


        tcCategory.setCellFactory(p -> {
            TableCell<Transaction, Category> cell = new CategoryTableCell();

            cell.setOnMouseClicked(event -> handleCategoryClick((Transaction) cell.getTableRow().getItem()));
            //TODO: Replace this with css :hover
            cell.setOnMouseEntered(event -> controller.getStage().getScene().setCursor(Cursor.HAND));
            cell.setOnMouseExited(event -> controller.getStage().getScene().setCursor(Cursor.DEFAULT));

            return cell;
        });

        table.setItems(model);
    }

    /**
     * Handle the creation of classifier dialogs and update the
     */
    private void handleCategoryClick(Transaction transaction) {
        if (transaction.getInflow()) {
            /*
            Prevent users from changing the category of an inflow transaction
            as they should have no category set because it gets generated at runtime
            */
            alertFactory.create(
                    Alert.AlertType.INFORMATION,
                    "Inflow transaction",
                    "Inflow transactions can't have their category changed"
            ).showAndWait();
        } else {
            ClassifierDialog dialog = new ClassifierDialog(
                    classifier,
                    transaction,
                    controller.getStage().getScene().getWindow()
            );
            dialog.showAndWait();
            Category category = dialog.getResult();
            if (category != null) {
                transaction.setCategory(category);
                transaction.setConfidence(ConfidenceLevel.USER_SET);
                classifier.learn(transaction);
                searchMatches();
                table.refresh();

                try {
                    transactionSql.update(transaction);
                    this.sortModel();
                } catch (RepositoryUpdateException e) {
                    log.error("Failed to update transaction after categorisation: " + transaction, e);
                    statusService.showError("Failed to update transaction in the database");
                }
            }
        }
    }

    /**
     * Produce a category string based on the transaction date for inflow transactions
     *
     * @param transaction Transaction which is an inflow
     * @return String to be used as a category
     */
    public String getInflowCategory(Transaction transaction) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM");
        LocalDate date = transaction.getTransactionDate();
        return "Income for " + formatter.format(date);
    }

    /**
     * Analyse the all transactions stored in the table and try to categorise them automatically
     */
    private void searchMatches() {
        for (Transaction t : model) {
            searchMatches(t);
        }
    }

    /**
     * Analyse a transaction using the {@link AbstractClassifier} and update it accordingly
     */
    private void searchMatches(Transaction transaction) {
        boolean updated = false;

        // Count the number of transactions found that are a perfect match
        int perfect = 0;
        //Count the number of transactions found may need user confirmation
        int needConfirmation = 0;

        if (transaction.getConfidence() < ConfidenceLevel.USER_SET && !transaction.getInflow()) {
            Optional<CategoryMatch> best = classifier.getBest(transaction);
            if (best.isPresent()) {
                CategoryMatch match = best.get();
                try {
                    if (match.getConfidence() >= ConfidenceLevel.NEED_CONFIRMATION) {
                        // Similarity between PRETTY_SURE and MAXIMUM

                        transaction.setConfidence(match.getConfidence());
                        transaction.setCategory(match.getCategory());
                        transactionSql.update(transaction);
                        updated = true;

                        if (match.getConfidence() >= ConfidenceLevel.PRETTY_SURE) {
                            // Similarity between AUTOMATIC_MATCH_VALUE and MAXIMUM
                            // Add this item to the classifier's data model as it's most certainly a good match,
                            // and skip this for NEED_CONFIRMATION matches as I don't want to have the classifier
                            // have the possibility of self training as it may lead to unforeseen effects
                            classifier.learn(transaction);
                            log.info("Automatically matched {} with {}", transaction, match);
                            perfect++;
                        } else {
                            needConfirmation++;
                        }
                    }
                } catch (RepositoryUpdateException e) {
                    log.error("Failed to update transaction in the database: " + transaction, e);
                }
            }

            if (updated) {
                table.refresh();
                displayAutoMatches(needConfirmation, perfect);
            }
        }
    }

    /**
     * Update the status bar to display the results of the automatic matching process if any matches are found
     *
     * @param needConfirmation Number of matches found that have a smaller confidence level
     * @param perfectMatch     Number of matches found that have an almost perfect confidence level
     */
    private void displayAutoMatches(int needConfirmation, int perfectMatch) {
        // Construct status messages
        String message = "";

        if (perfectMatch > 0) {
            message = perfectMatch + " perfect match";
            // Pluralise the noun
            if (perfectMatch != 1) {
                message += "es";
            }
        }

        if (perfectMatch > 0 && needConfirmation > 0) {
            message += " and ";
        }

        if (needConfirmation > 0) {
            message += needConfirmation + " possible match";
            // Pluralise the noun
            if (needConfirmation != 1) {
                message += "es";
            }
        }

        message += " " + (perfectMatch + needConfirmation == 1 ? "has" : "have") + " been found";

        statusService.showMessage(message);
    }

    //region CRUD Operations

    /**
     * Checks whether the table model has any transactions that don't have a category set
     *
     * @return True if there is at least one transaction that doesn't have a category
     */
    public boolean hasEmptyCategories() {
        return model.stream()
                .filter(td -> td.getCategory() == null)
                .filter(td -> !td.getInflow())
                .count() >= 0;
    }

    /**
     * Add a transaction to the table model
     *
     * @param transaction Transaction to be added to the table
     */
    public void add(Transaction transaction) {
        model.add(transaction);
    }

    /**
     * Add a collection of transsactions to the model
     *
     * @param transactions Collection of transactions
     */
    public void addAll(Collection<? extends Transaction> transactions) {
        model.addAll(transactions);
    }

    /**
     * Checks whether the table holds any transactions or not
     *
     * @return True if there is at least one transaction in the table
     */
    public boolean isEmpty() {
        return model.isEmpty();
    }

    /**
     * Clear the table and discard any transactions stored
     */
    public void clear() {
        model.clear();
    }

    /**
     * Returns all transactions currently stored in the table's model
     *
     * @return List of transactions stored in the table's model
     */
    public List<Transaction> getAll() {
        return new ArrayList<>(model);
    }

    /**
     * Returns the currently selected item if a selection is made
     *
     * @return Optional that may contain a selected transaction
     */
    public Optional<Transaction> getSelected() {
        Transaction transaction = table.getSelectionModel().getSelectedItem();
        if (transaction != null) {
            return Optional.of(transaction);
        } else {
            return Optional.empty();
        }
    }
    //endregion

    /**
     * Class used to handle the styling of a table cell containing a category
     */
    private class CategoryTableCell extends TableCell<Transaction, Category> {
        @Override
        protected void updateItem(Category item, boolean empty) {
            super.updateItem(item, empty);


            // Clear any previously set values as otherwise display bugs do occur
            getStyleClass().clear();
            setText("");
            setTooltip(null);

            if (empty || item == null) {
                Transaction transaction = (Transaction) this.getTableRow().getItem();
                if (transaction != null) {
                    if (transaction.getInflow()) {
                        /*
                        For inflow transactions there is no category saved in the object so we have
                        to always generate it based on the date
                        */
                        setText(getInflowCategory(transaction));
                        getStyleClass().add("inflow");
                        setTooltip(new Tooltip("Inflow transaction"));
                    } else {
                        setText("Set category");
                        getStyleClass().add("no-category");
                    }
                }
            } else {
                setText(item.getTitle());
                Transaction transaction = model.get(this.getIndex());

                if (transaction != null) {
                    // Apply different styling classes and tooltips based on the confidence level of the match
                    if (transaction.getConfidence() >= ConfidenceLevel.USER_SET) {
                        getStyleClass().add("user-set");
                        setTooltip(new Tooltip("User-set category"));
                    } else if (transaction.getConfidence() >= ConfidenceLevel.PRETTY_SURE) {
                        getStyleClass().add("pretty-sure");
                        setTooltip(new Tooltip("High possibility of a correct match"));
                    } else if (transaction.getConfidence() >= ConfidenceLevel.NEED_CONFIRMATION) {
                        getStyleClass().add("need-confirmation");
                        setTooltip(new Tooltip("Medium possibility of a correct match"));
                    }
                }
            }
        }
    }

    /**
     * Have the table view request focus
     */
    public void requestFocus() {
        Platform.runLater(table::requestFocus);
    }
}
