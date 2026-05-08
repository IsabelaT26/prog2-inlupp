package se.su.inlupp;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.util.Objects;

public class MapApplication extends Application {

  @Override
  public void start(Stage stage) {
    MapModel model = new MapModel();
    MapView view = new MapView(model);

    Scene scene = new Scene(view.getRoot(), 1000, 700);

    stage.setTitle("Map Navigator");
    stage.setScene(scene);
    stage.show();
  }

  public static void main(String[] args) {
    launch(args);
  }
}