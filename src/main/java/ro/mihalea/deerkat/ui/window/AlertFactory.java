package ro.mihalea.deerkat.ui.window;

import javafx.scene.control.Alert;
import javafx.stage.Modality;
import javafx.stage.Window;
import lombok.extern.log4j.Log4j2;

/**
 * Factory class used to facilitate the creations of dialogs with JavaFX
 */
@Log4j2
public class AlertFactory {
    /**
     * Owner window which is used to center the dialog
     */
    private Window owner;


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

        alert.initModality(Modality.APPLICATION_MODAL);
        alert.initOwner(owner);

        //Log the message but strip any new lines from it
        log.debug("Created new alert: ('" + title + "', '" + description.replace("\n", "") + "'");
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

    public void setOwner(Window owner) {
        this.owner = owner;
    }
}
