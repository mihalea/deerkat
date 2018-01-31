package ro.mihalea.deerkat.ui.window;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.Window;
import ro.mihalea.deerkat.ui.controller.ClassifierController;
import ro.mihalea.deerkat.model.Category;
import ro.mihalea.deerkat.model.Transaction;
import ro.mihalea.deerkat.classifier.AbstractClassifier;

import java.io.IOException;
import java.util.ListResourceBundle;

/**
 * Dialog which allows the user to set a category for a transaction based on a classifier's suggestions
 */
public class ClassifierDialog extends Dialog<Category> {
    /**
     * Initialise the dialog with the transaction that needs to be classified
     * @param transaction Transaction that needs to be classified
     */
    public ClassifierDialog(AbstractClassifier classifier, Transaction transaction, Window window) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("fxml/classifier.fxml"));
            Parent root = loader.load();
            ClassifierController controller = loader.getController();
            getDialogPane().setContent(root);

            // Add custom stylesheets to the dialog
            getDialogPane().getStylesheets().add("css/bootstrap.css");
            getDialogPane().getStylesheets().add("css/classifier.css");


            // Add confirmation and cancellation buttons
            ButtonType okay = new ButtonType("Set category", ButtonBar.ButtonData.OK_DONE);
            ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

            getDialogPane().getButtonTypes().add(okay);
            getDialogPane().getButtonTypes().add(cancel);

            // Transform them into normal buttons
            Button btnOkay = (Button) getDialogPane().lookupButton(okay);
            Button btnCancel = (Button) getDialogPane().lookupButton(cancel);

            // Make the confirmation button stand out
            btnOkay.getStyleClass().add("primary");
            btnOkay.setDefaultButton(true);
            btnOkay.setDisable(true);
            btnCancel.setCancelButton(true);

            controller.initialise(classifier, getDialogPane(), okay, transaction, btnOkay, btnCancel);

            Stage stage = (Stage) this.getDialogPane().getScene().getWindow();
            stage.getIcons().add(new Image(getClass().getClassLoader().getResourceAsStream("icons/deerkat.png")));
            stage.initOwner(window);

            setResultConverter(buttonType -> {
                if(buttonType.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
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
