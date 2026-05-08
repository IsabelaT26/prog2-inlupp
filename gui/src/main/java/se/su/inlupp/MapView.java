package se.su.inlupp;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Cursor;
import javafx.scene.input.MouseEvent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

import java.util.Objects;

public class MapView {

    private final MapModel model;
    private BorderPane root;
    private Pane mapPane;
    private Button addPlace;

    public MapView(MapModel model) {
        this.model = model;
        createLayout();
    }

    public BorderPane getRoot() {
        return root;
    }

    private void createLayout() {
        root = new BorderPane();

        root.setTop(createTop());
        root.setCenter(createMapPane());
    }

    private VBox createTop() {
        Menu fileMenu = new Menu("File");

        MenuItem newItem = new MenuItem("New");
        MenuItem saveItem = new MenuItem("Save");
        MenuItem loadItem = new MenuItem("Load");
        MenuItem exitItem = new MenuItem("Exit");

        fileMenu.getItems().addAll(newItem, saveItem, loadItem, exitItem);

        Menu pathMenu = new Menu("Path");

        MenuItem dfs = new MenuItem("Depth First Search");
        MenuItem bfs = new MenuItem("Breadth First Search");

        pathMenu.getItems().addAll(dfs, bfs);

        MenuBar menuBar = new MenuBar(fileMenu, new Menu("Edit"), pathMenu);

        addPlace = new Button("Add place");
        addPlace.setOnAction(new addButtonHandler());
        Button connect = new Button("Connect");
        Button findPath = new Button("Find path");
        Button remove = new Button("Remove");

        ToolBar toolBar = new ToolBar(addPlace, connect, findPath, remove);

        VBox top = new VBox(menuBar, toolBar);
        return top;
    }

    private class addButtonHandler implements EventHandler<ActionEvent> {
        @Override
        public void handle(ActionEvent actionEvent) {
            mapPane.setOnMouseClicked(new addPlaceClickHandler());

            mapPane.setCursor(Cursor.CROSSHAIR);

            addPlace.setDisable(true);
        }
    }

    class addPlaceClickHandler implements EventHandler<MouseEvent> {
        public void handle(MouseEvent event) {
            double x = event.getX();
            double y = event.getY();

            Place newPlace = new Place("Default name", x, y);
            model.addPlace(newPlace);

            //draw place
            Circle circle = new Circle(x, y, 20);
            Label label = new Label(newPlace.getName());
            mapPane.getChildren().addAll(circle, label);

            mapPane.setCursor(Cursor.DEFAULT);

            addPlace.setDisable(false);
            mapPane.setOnMouseClicked(null);
        }
    }

    private Pane createMapPane() {
        mapPane = new Pane();

        Image background = new Image(
                Objects.requireNonNull(
                        MapApplication.class.getResourceAsStream("/background.jpg")
                )
        );

        ImageView imageView = new ImageView(background);
        imageView.setPreserveRatio(false);

        imageView.fitWidthProperty().bind(mapPane.widthProperty());
        imageView.fitHeightProperty().bind(mapPane.heightProperty());

        mapPane.getChildren().add(imageView);

        return mapPane;
    }


}
