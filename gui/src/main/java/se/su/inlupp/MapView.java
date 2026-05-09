package se.su.inlupp;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.input.MouseEvent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;

import java.util.*;

public class MapView {

    private static final Paint PLACE_CIRCLE_COLOUR = Color.RED;
    private final MapModel model;

    private BorderPane root;
    private Pane mapPane;
    private Button addPlace;
    private LinkedList<Place> selectedPlacesList = new LinkedList<>();
    private Map<Place, Circle> selectedPlacesMap = new HashMap<>();

    public MapView(MapModel model) {
        this.model = model;
        createLayout();
    }

    public BorderPane getRoot() {
        return root;
    }

    // ---------- Layout ----------

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
        addPlace.setOnAction(new AddButtonHandler());

        Button connect = new Button("Connect");
        Button findPath = new Button("Find path");
        Button remove = new Button("Remove");

        ToolBar toolBar = new ToolBar(addPlace, connect, findPath, remove);

        return new VBox(menuBar, toolBar);
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

    // ---------- Add place feature ----------

    private void enterAddPlaceMode() {
        mapPane.setOnMouseClicked(new AddPlaceClickHandler());
        mapPane.setCursor(Cursor.CROSSHAIR);
        addPlace.setDisable(true);
    }

    private void resetAddPlaceMode() {
        mapPane.setCursor(Cursor.DEFAULT);
        addPlace.setDisable(false);
        mapPane.setOnMouseClicked(null);
    }

    private void handleAddPlaceClick(MouseEvent event) {
        double x = event.getX();
        double y = event.getY();

        Optional<String> placeName = askForPlaceName();

        if (placeName.isPresent()) {
            String name = placeName.get().trim();

            if (name.isEmpty()) {
                showError("Place name cannot be empty.");
                resetAddPlaceMode();
                return;
            }

            Place newPlace = new Place(name, x, y);

            try {
                model.addPlace(newPlace);
                drawPlace(newPlace);
            } catch (IllegalArgumentException e) {
                showError(e.getMessage());
            }
        }

        resetAddPlaceMode();
    }

    // ---------- Drawing ----------

    private void drawPlace(Place place) {
        Circle circle = new Circle(place.getX(), place.getY(), 20);
        circle.setFill(PLACE_CIRCLE_COLOUR);

        circle.setOnMousePressed(new SelectHandler(place,circle));

        Text text = new Text(place.getX() - 20, place.getY() - 25, place.getName());
        text.setFill(PLACE_CIRCLE_COLOUR);

        circle.setOnMouseDragged(new DragHandler(place, circle,text));

        Group group = new Group(circle, text);
        //group.setOnMouseDragged(new DragHandler(place, circle));

        mapPane.getChildren().addAll(circle, text);
    }

    //-----Select Place Feature-----

    private void toggleSelection(Place place, Circle circle){
        if(selectedPlacesMap.containsKey(place)){
            selectedPlacesMap.remove(place);
            selectedPlacesList.remove(place);
            circle.setFill(PLACE_CIRCLE_COLOUR);
        } else if (selectedPlacesMap.size()< 2) {
            selectedPlacesMap.put(place,circle);
            selectedPlacesList.add(place);
            circle.setFill(Color.ORANGE);
        }else {
            Place firsSelectedPlace = selectedPlacesList.getFirst();
            Circle firstSelectPlaceCircle = selectedPlacesMap.get(firsSelectedPlace);
            firstSelectPlaceCircle.setFill(PLACE_CIRCLE_COLOUR);
            selectedPlacesMap.remove(firsSelectedPlace);
            selectedPlacesList.removeFirst();
            toggleSelection(place,circle);
            //showError("You can only select two places at a time.");
        }
    }

    // ---------- Dialogs ----------

    private Optional<String> askForPlaceName() {
        TextInputDialog dialog = new TextInputDialog();

        dialog.setTitle("Name");
        dialog.setHeaderText("Enter a name for the place");
        dialog.setContentText("Name:");

        return dialog.showAndWait();
    }

    private void showError(String errorMessage) {
        Alert alert = new Alert(Alert.AlertType.ERROR);

        alert.setTitle("Error");
        alert.setHeaderText("Something went wrong");
        alert.setContentText(errorMessage);

        alert.showAndWait();
    }

    // ---------- Event handlers ----------

    private class AddButtonHandler implements EventHandler<ActionEvent> {
        @Override
        public void handle(ActionEvent actionEvent) {
            enterAddPlaceMode();
        }
    }

    private class AddPlaceClickHandler implements EventHandler<MouseEvent> {
        @Override
        public void handle(MouseEvent event) {
            handleAddPlaceClick(event);
        }
    }

    private class SelectHandler implements EventHandler<MouseEvent> {
        private final Place place;
        private final Circle circle;

        public SelectHandler(Place place, Circle circle) {
            this.place = place;
            this.circle = circle;
        }

        @Override
        public void handle(MouseEvent event) {
            toggleSelection(place, circle);
            event.consume();
        }
    }

    class DragHandler implements EventHandler<MouseEvent> {

        private Place place;
        private Circle circle;
        private Text text;

        public DragHandler(Place place, Circle circle, Text text){
            this.place = place;
            this.circle = circle;
            this.text = text;
        }

        @Override
        public void handle(MouseEvent event) {
            Point2D point = mapPane.sceneToLocal(event.getSceneX(), event.getSceneY());

            double newX = point.getX();
            double newY = point.getY();

            circle.setCenterX(newX);
            circle.setCenterY(newY);
            text.setX(newX - 20);
            text.setY(newY - 25);
            place.setPosition(newX, newY);
        }
    }
}