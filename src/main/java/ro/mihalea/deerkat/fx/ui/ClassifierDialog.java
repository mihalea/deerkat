package ro.mihalea.deerkat.fx.ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import ro.mihalea.deerkat.fx.controller.ClassifierController;
import ro.mihalea.deerkat.model.Category;
import ro.mihalea.deerkat.model.Transaction;
import ro.mihalea.deerkat.classifier.AbstractClassifier;

import java.io.IOException;

/**
 * Dialog which allows the user to set a category for a transaction based on a classifier's suggestions
 */
public class ClassifierDialog extends Dialog<Category> {
    /**
     * Initialise the dialog with the transaction that needs to be classified
     * @param transaction Transaction that needs to be classified
     */
    public ClassifierDialog(AbstractClassifier classifier, Transaction transaction) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("fxml/classifier.fxml"));
            Parent root = loader.load();
            ClassifierController controller = loader.getController();
            getDialogPane().setContent(root);
            getDialogPane().getStylesheets().add("css/bootstrap.css");
            getDialogPane().getStylesheets().add("css/classifier.css");

            ButtonType okay = new ButtonType("Set category", ButtonBar.ButtonData.OK_DONE);
            ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

            getDialogPane().getButtonTypes().add(okay);
            getDialogPane().getButtonTypes().add(cancel);

            getDialogPane().lookupButton(okay).getStyleClass().add("primary");
            getDialogPane().lookupButton(okay).setDisable(true);


            controller.initialise(classifier, getDialogPane(), okay, transaction);

            setResultConverter(buttonType -> {
                if(buttonType == ButtonType.OK) {
                    return controller.getSelectedCategory();
                } else {
                    return null;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
