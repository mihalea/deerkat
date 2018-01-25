package ro.mihalea.deerkat.fx.ui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import ro.mihalea.deerkat.fx.controller.MainController;

/**
 * Main window used to interface with the user.
 *
 * It has a table holding transactions, and buttons to import and export them.
 */
public class MainWindow extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("fxml/main.fxml"));
        Parent root = loader.load();
        MainController controller = loader.getController();
        controller.initialise(primaryStage);
        primaryStage.setTitle("Deerkat");
        primaryStage.setScene(new Scene(root, 800, 600));
        primaryStage.show();
    }
}
