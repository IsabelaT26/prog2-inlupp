package se.su.inlupp;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MapApplication extends Application {

    @Override
    public void start(Stage stage) {
        MapModel model = new MapModel();
        MapView view = new MapView(model);

        Scene scene = new Scene(view.getRoot(), 1000, 700);

        stage.setTitle("Map Navigator");
        stage.setScene(scene);
        stage.show();
        stage.setOnCloseRequest(event -> {
            if (view.confirmDiscardUnsavedChanges()) {
                event.consume();
            }
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}