package ro.mihalea.deerkat.ui.service;

import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import lombok.extern.log4j.Log4j2;

/**
 * Serice used to modify the contents of the status bar
 */
@Log4j2
public class StatusService {
    /**
     * Label used to display text messages
     */
    private final Label label;

    /**
     * Progress bar used to display ongoing action's progress
     */
    private final ProgressBar progressBar;

    /**
     * Creates a status service ready to display messages and progress reports
     * @param label JavaFX Label part of the status bar
     * @param progressBar JavaFX ProgressBar part of the status bar
     */
    public StatusService(Label label, ProgressBar progressBar) {
        this.label = label;
        this.progressBar = progressBar;
    }

    /**
     * Update the text in the status bar to display messages
     * @param text Message to be displayed
     */
    public void showMessage(String text) {
        this.showText(text, false);
    }

    /**
     * Update the text in the status bar to display errors
     * @param text Error message
     */
    public void showError(String text) {
        this.showText(text, true);
    }

    /**
     * Private method used to display text in the status bar and change the font colour based on the context
     * @param text Text to be displayed
     * @param error
     */
    private void showText(String text, boolean error) {
        label.setText(text);

        if(error) {
            label.getStyleClass().add("status-error");
        } else {
            label.getStyleClass().clear();
        }
    }

    /**
     * Set the visibility status of the progress bar
     * @param display True if the progress bar should be visible
     */
    public void displayProgress(boolean display) {
        progressBar.setVisible(display);
    }

    /**
     * Bind to the progress bar to have it automatically update
     * @param progress Property updating the progress bar
     */
    public void bindProgress(ReadOnlyDoubleProperty progress) {
        progressBar.progressProperty().bind(progress);
    }
}
