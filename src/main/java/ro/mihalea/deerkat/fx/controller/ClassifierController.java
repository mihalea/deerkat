package ro.mihalea.deerkat.fx.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import lombok.extern.log4j.Log4j2;
import ro.mihalea.deerkat.exception.repository.RepositoryConnectionException;
import ro.mihalea.deerkat.exception.repository.RepositoryReadException;
import ro.mihalea.deerkat.model.Category;
import ro.mihalea.deerkat.model.Transaction;
import ro.mihalea.deerkat.repository.CategorySqlRepository;
import ro.mihalea.deerkat.classifier.AbstractClassifier;
import ro.mihalea.deerkat.classifier.CategoryMatch;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Controller for {@link ro.mihalea.deerkat.fx.ui.ClassifierDialog}
 */
@Log4j2
public class ClassifierController {
    /**
     * Label that display transaction date
     */
    @FXML
    private Label lbDate;

    /**
     * Label that displays the details of the transaction
     */
    @FXML
    private Label lbDetails;

    /**
     * Label that displays the amount of the transaction
     */
    @FXML
    private Label lbAmount;

    /**
     * List view containing all possible categories
     */
    @FXML
    private ListView<Category> lvAll;

    /**
     * List view containing recommended categories obtained from a classifier
     */
    @FXML
    private ListView<CategoryMatch> lvRecommended;

    /**
     * Label above the recommended list
     */
    @FXML
    private Label lbRecommended;

    /**
     * List holding categories that may be recommended to the user
     */
    private ObservableList<CategoryMatch> recommendedCategories = FXCollections.observableArrayList();

    /**
     * Map containing all categories that have been recommended by the classifier and the probability that they match
     */
    private List<CategoryMatch> categoryProbabilities;

    /**
     * Data model used by the list view holding all subcategories
     */
    private ObservableList<Category> allCategories = FXCollections.observableArrayList();

    /**
     * Repository used to retrieve all possible categories
     */
    private final CategorySqlRepository repository;

    /**
     * List of all categories available
     */
    private List<Category> categories;


    /**
     * Stores the user selected category, and if null it signifies that the user has cancelled the action
     */
    private Category selectedCategory;

    /**
     * Initialise the controller by instantiating the repository and updating the categories
     */
    public ClassifierController() throws RepositoryConnectionException {
        repository = new CategorySqlRepository();
    }

    public Category getSelectedCategory() {
        return selectedCategory;
    }

    /**
     * Retrieve all possible categories and update the UI
     */
    public void initialise(AbstractClassifier classifier, DialogPane dialogPane, ButtonType button, Transaction transaction) {
        try {
            updateTransactionLabels(transaction);
            initialiseCells();
            initialiseSelectionModel(dialogPane, button);
            initialiseClassifier(classifier, transaction);

        } catch (RepositoryReadException e) {
            log.error("Failed to load categories into dialog", e);
        }
    }

    /**
     * Analyse the current transaction and update the UI with the best matching categories
     * @param classifier
     */
    private void initialiseClassifier(AbstractClassifier classifier, Transaction transaction) {
        categoryProbabilities = classifier.getMatches(transaction);
        recommendedCategories.addAll(categoryProbabilities);

        if(categoryProbabilities.size() <= 0) {
            lbRecommended.setManaged(false);
            lvRecommended.setManaged(false);
        } else {
            lvRecommended.requestFocus();
            lvRecommended.getSelectionModel().selectFirst();
            lvRecommended.getFocusModel().focus(0);
        }

        lvRecommended.setCellFactory(cell -> new ListCell<>() {
            @Override
            protected void updateItem(CategoryMatch item, boolean empty) {
                super.updateItem(item, empty);

                if(!empty && item != null) {
                        String padded = String.format("%3d", item.getSimilarity());
                        setText(padded + "% - " + item.getCategory().getTitle());
                }
            }
        });
    }

    /**
     * Add events for both list views that update the selected category
     * @param dialogPane Parent dialog pane
     * @param button Confirmation button from the dialog
     */
    private void initialiseSelectionModel(DialogPane dialogPane, ButtonType button) {
        // The two ListView should be mutually exclusive, so selecting an item in one should deselect all items in the other
        lvAll.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> {
                    selectedCategory = lvAll.getSelectionModel().getSelectedItem();
                    lvRecommended.getSelectionModel().clearSelection();
                    dialogPane.lookupButton(button).setDisable(false);
                });
        lvRecommended.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> {
                    selectedCategory = lvRecommended.getSelectionModel().getSelectedItem().getCategory();
                    lvAll.getSelectionModel().clearSelection();
                    dialogPane.lookupButton(button).setDisable(false);
                });
    }

    /**
     * Retrieve categories from the transaction, inject them into their respective list view and update the cell factory
     */
    private void initialiseCells() throws RepositoryReadException {
        categories = repository.getAll();

        // Add only subcategories
        allCategories.addAll(categories.stream().filter(c -> c.getParentId() != null).collect(Collectors.toList()));

        lvAll.setItems(allCategories);
        lvRecommended.setItems(recommendedCategories);

        // Add a custom cell renderer to retrieve the parent's title
        lvAll.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Category item, boolean empty) {
                super.updateItem(item, empty);

                if(!empty && item != null) {

                    // Obtaining parent category
                    Optional<Category> parent = categories.stream()
                            .filter(c -> c.getId().equals(item.getParentId()))
                            .findFirst();
                    if(!parent.isPresent()) {
                        log.error("Failed to obtain parent category for :" + item);
                        setText("ERROR");
                    } else {
                        setText(parent.get().getTitle()+ " > " + item.getTitle());
                    }


                }
            }
        });
    }

    /**
     * Update labels holding transaction information with data from the current transaction
     * @param transaction Current transaction
     */
    private void updateTransactionLabels(Transaction transaction) {
        lbDate.setText(transaction.getTransactionDate().toString());
        lbDetails.setText(transaction.getDetails());
        lbAmount.setText(Double.toString(transaction.getAmount()) + " AED");
    }
}
