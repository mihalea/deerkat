package ro.mihalea.deerkat.ui.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.text.Font;
import lombok.extern.log4j.Log4j2;
import ro.mihalea.deerkat.exception.repository.RepositoryConnectionException;
import ro.mihalea.deerkat.exception.repository.RepositoryReadException;
import ro.mihalea.deerkat.exception.repository.RepositoryUpdateException;
import ro.mihalea.deerkat.model.Category;
import ro.mihalea.deerkat.model.Transaction;
import ro.mihalea.deerkat.repository.CategorySqlRepository;
import ro.mihalea.deerkat.classifier.AbstractClassifier;
import ro.mihalea.deerkat.classifier.CategoryMatch;
import ro.mihalea.deerkat.ui.window.AlertFactory;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller for {@link ro.mihalea.deerkat.ui.window.ClassifierDialog}
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
     * Checkbox used to show hidden categories
     */
    @FXML
    private CheckBox cbHidden;

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
     * Factory used to create alerts
     */
    private AlertFactory alertFactory = new AlertFactory();

    /**
     * Pseudo class used to mark hidden categories
     */
    private PseudoClass pseudo = PseudoClass.getPseudoClass("hidden");


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
    public void initialise(AbstractClassifier classifier, DialogPane dialogPane, ButtonType button, Transaction transaction, Button btnOkay, Button btnCancel) {
        try {
            Platform.runLater(() -> alertFactory.setOwner(dialogPane.getScene().getWindow()));


            updateTransactionLabels(transaction);
            loadFont();
            initialiseKeyListeners(btnOkay, btnCancel);
            initialiseCells();
            initialiseSelectionModel(dialogPane, button);
            initialiseClassifier(classifier, transaction);
        } catch (RepositoryReadException e) {
            log.error("Failed to load categories into dialog", e);
        }
    }

    /**
     * Set up key listeners on the table views
     * @param btnOkay Accept button
     * @param btnCancel Cancel button
     */
    private void initialiseKeyListeners(Button btnOkay, Button btnCancel) {
        lvRecommended.setOnKeyReleased(ke -> handleActionKeys(ke, btnOkay, btnCancel));
        lvAll.setOnKeyReleased(ke -> {
            handleActionKeys(ke, btnOkay, btnCancel);


            if(ke.getCode() == KeyCode.DELETE && !lvAll.getSelectionModel().isEmpty()) {
                Category category = lvAll.getSelectionModel().getSelectedItem();
                if(category != null) {
                    try {
                        category.setHidden(!category.getHidden());
                        repository.update(category);

                        log.info("Category's 'hidden' is now {}: {}", category.getHidden(), category);
                        if(category.getHidden() && !cbHidden.isSelected()) {
                            allCategories.remove(category);
                        }

                        lvAll.refresh();
                    } catch (RepositoryUpdateException e) {
                        log.error("Failed to hide category " + category, e);
                        alertFactory.createError(
                                "Failed to hide",
                                "An error occurred while trying to hide the category"
                        ).showAndWait();
                    }
                }
            }
        });
    }

    /**
     * Handle key events pertaining to the ENTER and ESCAPE keys which are used to accept or cancel the dialog
     * @param keyEvent Key event triggered
     * @param btnOkay Accept button
     * @param btnCancel Cancel button
     */
    private void handleActionKeys(KeyEvent keyEvent, Button btnOkay, Button btnCancel) {
        Object source = keyEvent.getSource();
        ListView listView = null;
        if(source instanceof ListView) {
            listView = (ListView) source;
        }

        // Avoid accepting input if not selection is made
        if(keyEvent.getCode() == KeyCode.ENTER && listView != null && !listView.getSelectionModel().isEmpty()) {
            btnOkay.fire();
        } else if (keyEvent.getCode() == KeyCode.ESCAPE) {
            btnCancel.fire();
        }
    }

    /**
     * Load a monospaced font used for displaying the recommended categories
     */
    private void loadFont() {
        // Note: the double parameter (10) should control the size but appears to be doing nothing
        Font.loadFont(getClass().getClassLoader().getResource("fonts/SourceCodePro.ttf").toExternalForm(), 10d);
    }

    /**
     * Analyse the current transaction and update the UI with the best matching categories
     *
     * @param classifier Classifier used to give predictions on the transaction category
     */
    private void initialiseClassifier(AbstractClassifier classifier, Transaction transaction) {
        categoryProbabilities = classifier.getMatches(transaction);
        recommendedCategories.addAll(categoryProbabilities);

        if (categoryProbabilities.size() <= 0) {
            lbRecommended.setManaged(false);
            lvRecommended.setManaged(false);
            Platform.runLater(() -> {
                lvAll.requestFocus();
                lvRecommended.getSelectionModel().selectFirst();
            });
        } else {
            Platform.runLater(() -> {
                lvRecommended.requestFocus();
                lvRecommended.getSelectionModel().selectFirst();
            });
        }

        lvRecommended.setCellFactory(cell -> new ListCell<CategoryMatch>() {
            @Override
            protected void updateItem(CategoryMatch item, boolean empty) {
                super.updateItem(item, empty);

                if (!empty && item != null) {
                    String padded = String.format("%3d", item.getSimilarity());
                    setText(padded + "% - " + item.getCategory().getTitle());
                }
            }
        });
    }

    /**
     * Add events for both list views that update the selected category
     *
     * @param dialogPane Parent dialog pane
     * @param button     Confirmation button from the dialog
     */
    private void initialiseSelectionModel(DialogPane dialogPane, ButtonType button) {
        // The two ListView should be mutually exclusive, so selecting an item in one should deselect all items in the other
        lvAll.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> {
                    // Check that a selection is already made to prevent bouncing between the two listeners
                    if (lvAll.getSelectionModel().getSelectedItem() != null) {
                        selectedCategory = lvAll.getSelectionModel().getSelectedItem();
                        clearSelection(lvRecommended);
                        dialogPane.lookupButton(button).setDisable(false);
                    }
                });
        lvRecommended.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> {
                    // Check that a selection is already made to prevent bouncing between the two listeners
                    if (lvRecommended.getSelectionModel().getSelectedItem() != null) {
                        selectedCategory = lvRecommended.getSelectionModel().getSelectedItem().getCategory();
                        clearSelection(lvAll);
                        dialogPane.lookupButton(button).setDisable(false);
                    }
                });
    }

    private void clearSelection(ListView listView) {
        listView.getSelectionModel().clearSelection(listView.getSelectionModel().getSelectedIndex());
    }

    /**
     * Retrieve categories from the transaction, inject them into their respective list view and update the cell factory
     */
    private void initialiseCells() throws RepositoryReadException {
        categories = repository.getAll();

        // List only non-hidden sub categories
        List<Category> subCategories = categories.stream()
                .filter(c -> c.getParentId() != null)
                .filter(c -> !c.getHidden())
                .collect(Collectors.toList());

        // Add only subcategories
        allCategories.addAll(subCategories);

        lvAll.setItems(allCategories);
        lvRecommended.setItems(recommendedCategories);

        // Add a custom cell renderer to retrieve the parent's title
        lvAll.setCellFactory(param -> new ListCell<Category>() {
            @Override
            protected void updateItem(Category item, boolean empty) {
                super.updateItem(item, empty);

                if (!empty && item != null) {

                    // Obtaining parent category
                    Optional<Category> parent = categories.stream()
                            .filter(c -> c.getId().equals(item.getParentId()))
                            .findFirst();
                    if (!parent.isPresent()) {
                        log.error("Failed to obtain parent category for :" + item);
                        setText("ERROR");
                    } else {
                        pseudoClassStateChanged(pseudo, item.getHidden());
                        setText(parent.get().getTitle() + " > " + item.getTitle());
                    }


                } else {
                    setText("");
                    pseudoClassStateChanged(pseudo, false);
                }
            }
        });
    }

    /**
     * Update labels holding transaction information with data from the current transaction
     *
     * @param transaction Current transaction
     */
    private void updateTransactionLabels(Transaction transaction) {
        lbDate.setText(transaction.getTransactionDate().toString());
        lbDetails.setText(transaction.getDetails());
        lbAmount.setText(Double.toString(transaction.getAmount()) + " AED");
    }

    @FXML
    private void cbHidden_Action() {
        Category selected = lvAll.getSelectionModel().getSelectedItem();
        if(cbHidden.isSelected()) {
            try {
                allCategories.clear();
                allCategories.addAll(repository.getAll().stream().filter(c -> c.getParentId() != null).collect(Collectors.toList()));
                log.info("Showing hidden categories");
                if(selected != null) {
                    lvAll.getSelectionModel().select(selected);
                    log.debug("{} was selected", selected);
                }
            } catch (RepositoryReadException e) {
                log.error("Failed to show hidden categories", e);
            }
        } else {
            List<Category> hidden = allCategories.filtered(Category::getHidden);
            allCategories.removeAll(hidden);
            if(selected != null && allCategories.contains(selected)) {
                lvAll.getSelectionModel().select(selected);
            }
            log.info("Showing only non-hidden categories");
        }

        lvAll.refresh();
    }
}
