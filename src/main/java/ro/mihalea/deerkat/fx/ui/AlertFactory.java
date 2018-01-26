package ro.mihalea.deerkat.fx.ui;

import javafx.scene.control.Alert;

/**
 * Factory class used to facilitate the creations of dialogs with JavaFX
 */
public class AlertFactory {
    /**
     * Create a new alert with the specified alert type, title and description and return it
     * @param alertType Describes the appearence of the dialog
     * @param title Title of the window
     * @param description Main body of text displayed to the user
     * @return New alert ready to be displayed
     */
    public Alert create(Alert.AlertType alertType, String title, String description) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(description);
        return alert;
    }

    /**
     * Create an error dialog with the given title and description and return it
     * @param title Title of the window
     * @param description Main body of text displayed to the user
     * @return New error alert ready to be displayed
     */
    public Alert createError(String title, String description) {
        return this.create(Alert.AlertType.ERROR, title, description);
    }
}
