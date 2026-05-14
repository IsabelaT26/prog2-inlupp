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
    private Button connect;
    private Button removePlace;
    private Button findPath;
    private Button disconnect;

    private final List<ConnectionView> connectionViews = new ArrayList<>();

    private final Map<Place, Group> placeViews = new HashMap<>();
    private final Map<Place, Circle> placeCircles = new HashMap<>();

    private final SelectionManager selectionManager = new SelectionManager();
    private Mode currentMode = Mode.NORMAL;

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

        connect = new Button("Connect");
        connect.setOnAction(new ConnectButtonHandler());

        disconnect = new Button("Disconnect");
        disconnect.setOnAction(new DisconnectButtonHandler());

        findPath = new Button("Find path");

        removePlace = new Button("Remove Place");
        removePlace.setOnAction(new RemoveButtonHandler());

        ToolBar toolBar = new ToolBar(addPlace, connect, disconnect, findPath, removePlace);

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


        Text text = new Text(place.getX() - 20, place.getY() - 25, place.getName());
        text.setFill(PLACE_CIRCLE_COLOUR);

        circle.setOnMouseDragged(new DragHandler(place, circle,text));

        Group circleAndName = new Group(circle, text);

        placeViews.put(place, circleAndName);
        placeCircles.put(place, circle);

        circleAndName.setOnMousePressed(new SelectHandler(place, circleAndName));

        mapPane.getChildren().addAll(circleAndName);
    }

    private void drawLine(Place place1, Place place2) {
        Circle circle1 = placeCircles.get(place1);
        Circle circle2 = placeCircles.get(place2);

        ConnectionView connectionView = new ConnectionView(
                place1,
                place2,
                circle1,
                circle2,
                model.getConnectionName(place1, place2),
                PLACE_CIRCLE_COLOUR
        );

        connectionViews.add(connectionView);

        mapPane.getChildren().add(1,connectionView.getRoot());
    }

    //-----Select Place Feature-----

    private void handlePlaceClicked(Place place, Group circleAndName) {
        switch (currentMode) {
            case NORMAL:
                return;

            case REMOVE:
                selectionManager.toggleSelection(place,circleAndName);
                handleRemoveSelection();
                return;

            case FIND_PATH:
                // later path logic
                return;

            case CONNECT:
                selectionManager.toggleSelection(place, circleAndName);
                handleConnectSelection();
                return;
            case DISCONNECT:
                selectionManager.toggleSelection(place, circleAndName);
                handleDisconnectSelection();
                return;
        }
    }

    private void resetModeAfterAction() {
        selectionManager.clearSelection();
        currentMode = Mode.NORMAL;
        connect.setDisable(false);
        disconnect.setDisable(false);
        removePlace.setDisable(false);
        mapPane.setCursor(Cursor.DEFAULT);
    }

    private void handleConnectSelection() {
        if (selectionManager.nrOfPlacesSelected() < 2) {
            return;
        }

        Place place1 = selectionManager.getFirstSelectedPlace();
        Place place2 = selectionManager.getSecondSelectedPlace();

        for(ConnectionView view : connectionViews){
            if(view.connects(place1, place2)){
                showError(place1.getName() +" and "+ place2.getName() + " are already connected!");
                resetModeAfterAction();
                return;
            }
        }

        Optional<RoadInfo> roadInfo = askForRoadInfo();

        if(roadInfo.isPresent()) {
            String roadName = roadInfo.get().name();
            int roadDistance = roadInfo.get().distance();

            try {
                model.connectPlaces(place1, place2, roadName, roadDistance);
                drawLine(place1, place2);
            } catch (IllegalArgumentException e) {
                showError(e.getMessage());
            }finally {
                resetModeAfterAction();
            }
        }
    }

    private void handleDisconnectSelection(){
        if (selectionManager.nrOfPlacesSelected() < 2) {
            return;
        }

        Place place1 = selectionManager.getFirstSelectedPlace();
        Place place2 = selectionManager.getSecondSelectedPlace();


        ConnectionView toRemove = null;

        for(ConnectionView view : connectionViews){
            if(view.connects(place1, place2)){
                toRemove = view;
                break;
            }
        }

        if(toRemove == null){
            showError(place1.getName() +" and "+ place2.getName() + " do not have a connection!");
            resetModeAfterAction();
            return;
        }
        connectionViews.remove(toRemove);
        mapPane.getChildren().remove(toRemove.getRoot());
        model.disconnectPlaces(place1, place2);

        resetModeAfterAction();
    }

    private void handleRemoveSelection(){
        if (selectionManager.nrOfPlacesSelected() == 1) {
            Place place = selectionManager.getFirstSelectedPlace();
            Group placeView = placeViews.get(place);

            model.removePlace(place);
            mapPane.getChildren().remove(placeView);

            placeViews.remove(place);
            placeCircles.remove(place);

            Iterator<ConnectionView> iterator = connectionViews.iterator();

            while (iterator.hasNext()) {
                ConnectionView view = iterator.next();

                if (view.comesFrom(place)) {
                    mapPane.getChildren().remove(view.getRoot());
                    iterator.remove();
                }
            }
        }

        resetModeAfterAction();
    }

    // ---------- Dialogs ----------

    private Optional<String> askForPlaceName() {
        TextInputDialog dialog = new TextInputDialog();

        dialog.setTitle("Name");
        dialog.setHeaderText("Enter a name for the place");
        dialog.setContentText("Name:");

        return dialog.showAndWait();
    }

    private Optional<RoadInfo> askForRoadInfo() {

        Dialog<RoadInfo> dialog = new Dialog<>();

        dialog.setTitle("Road Info");
        dialog.setHeaderText("Enter road information");

        // Input fields
        TextField nameField = new TextField();
        TextField distanceField = new TextField();

        // Layout
        VBox box = new VBox(
                10,
                new Label("Road name:"),
                nameField,
                new Label("Distance:"),
                distanceField
        );

        dialog.getDialogPane().setContent(box);

        // Buttons
        ButtonType okButton = new ButtonType(
                "OK",
                ButtonBar.ButtonData.OK_DONE
        );

        dialog.getDialogPane().getButtonTypes().addAll(
                okButton,
                ButtonType.CANCEL
        );

        // Save result into RoadInfo record
        dialog.setResultConverter(button -> {

            if (button == okButton) {

                try {
                    String name = nameField.getText();
                    int distance = Integer.parseInt(distanceField.getText());

                    return new RoadInfo(name, distance);

                } catch (NumberFormatException e) {

                    showError("Distance must be a number.");
                    connect.setDisable(false);
                }
            }

            return null;
        });

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
        private final Group circleAndName;

        public SelectHandler(Place place, Group circleAndName) {
            this.place = place;
            this.circleAndName = circleAndName;
        }

        @Override
        public void handle(MouseEvent event) {
            handlePlaceClicked(place, circleAndName);
            event.consume();
        }
    }

    private class DragHandler implements EventHandler<MouseEvent> {

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

    private class ConnectButtonHandler implements EventHandler<ActionEvent> {
        @Override
        public void handle(ActionEvent event) {
            selectionManager.clearSelection();
            currentMode = Mode.CONNECT;
            mapPane.setCursor(Cursor.CROSSHAIR);
            connect.setDisable(true);
        }
    }

    private class DisconnectButtonHandler implements EventHandler<ActionEvent>{
        @Override
        public void handle(ActionEvent event) {
            selectionManager.clearSelection();
            currentMode = Mode.DISCONNECT;
            mapPane.setCursor(Cursor.CROSSHAIR);
            disconnect.setDisable(true);
        }
    }

    private class RemoveButtonHandler implements EventHandler<ActionEvent>{
        @Override
        public void handle(ActionEvent event) {
            selectionManager.clearSelection();
            currentMode = Mode.REMOVE;
            mapPane.setCursor(Cursor.CROSSHAIR);
            removePlace.setDisable(true);
        }
    }
}