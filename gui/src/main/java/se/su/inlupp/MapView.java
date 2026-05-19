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
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

public class MapView {

    private static final Color PLACE_CIRCLE_COLOUR = Color.RED;
    private final MapModel model;

    private BorderPane root;
    private Pane mapPane;
    private Button addPlace = new Button("Add place");
    private Button  connect = new Button("Connect");
    private Button removePlace = new Button("Remove Place");
    private Button findPath = new Button("Find path");
    private Button disconnect = new Button("Disconnect");
    private Button clearPath = new Button("Clear path");

    private MenuItem newItem = new MenuItem("New");
    private MenuItem saveItem = new MenuItem("Save");
    private MenuItem loadItem = new MenuItem("Load");
    private MenuItem loadBackgroundItem = new MenuItem("Load background image");

    private MenuItem dfs = new MenuItem("Depth First Search");;
    private MenuItem  bfs = new MenuItem("Breadth First Search");

    private ImageView imageView;
    private String backgroundImagePath = "/background.jpg";
    private boolean hasUnsavedChanges = false;

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

        newItem.setOnAction(event -> newMap());
        saveItem.setOnAction(event-> saveMap());
        loadItem.setOnAction(event -> loadMap());
        loadBackgroundItem.setOnAction(event -> loadBackgroundImage());

        fileMenu.getItems().addAll(newItem, saveItem, loadItem, loadBackgroundItem);

        Menu pathMenu = new Menu("Path");

        dfs.setOnAction(event -> model.useDFS());
        bfs.setOnAction(event -> model.useBFS());

        pathMenu.getItems().addAll(dfs, bfs);

        MenuBar menuBar = new MenuBar(fileMenu, pathMenu);

        addPlace.setOnAction(event -> enterAddPlaceMode());

        connect.setOnAction(new ConnectButtonHandler());

        disconnect.setOnAction(new DisconnectButtonHandler());

        findPath.setOnAction(new FindPathHandler());

        clearPath.setOnAction(event -> clearPathHighlight());

        removePlace.setOnAction(new RemoveButtonHandler());

        ToolBar toolBar = new ToolBar(addPlace,removePlace, connect, disconnect, findPath, clearPath);

        return new VBox(menuBar, toolBar);
    }

    private Pane createMapPane() {
        mapPane = new Pane();

        Image background = new Image(
                Objects.requireNonNull(
                        MapApplication.class.getResourceAsStream(backgroundImagePath)
                )
        );

        imageView = new ImageView(background);
        imageView.setPreserveRatio(false);

        imageView.fitWidthProperty().bind(mapPane.widthProperty());
        imageView.fitHeightProperty().bind(mapPane.heightProperty());

        mapPane.getChildren().add(imageView);

        return mapPane;
    }

    // ---------- Add place feature ----------

    private void enterAddPlaceMode() {
        mapPane.setOnMouseClicked(this::handleAddPlaceClick);
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

        Group placeView = new Group(circle, text);

        placeViews.put(place, placeView);
        placeCircles.put(place, circle);

        placeView.setOnMousePressed(new SelectHandler(place, placeView));

        mapPane.getChildren().addAll(placeView);
        hasUnsavedChanges = true;
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
        hasUnsavedChanges = true;
    }

    //-----Select Place Feature-----

    private void handlePlaceClicked(Place place, Group placeView) {
        switch (currentMode) {
            case NORMAL:
                return;

            case REMOVE:
                selectionManager.toggleSelection(place,placeView);
                handleRemoveSelection();
                return;

            case FIND_PATH:
                selectionManager.toggleSelection(place, placeView);
                handleFindPathSelection();
                return;

            case CONNECT:
                selectionManager.toggleSelection(place, placeView);
                handleConnectSelection();
                return;
            case DISCONNECT:
                selectionManager.toggleSelection(place, placeView);
                handleDisconnectSelection();
        }
    }

    private void resetModeAfterAction() {
        selectionManager.clearSelection();
        currentMode = Mode.NORMAL;
        connect.setDisable(false);
        disconnect.setDisable(false);
        removePlace.setDisable(false);
        findPath.setDisable(false);
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
                hasUnsavedChanges = true;
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
        hasUnsavedChanges = true;

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

        hasUnsavedChanges = true;
        resetModeAfterAction();
    }

    private void handleFindPathSelection(){
        if (selectionManager.nrOfPlacesSelected() < 2) {
            return;
        }

        Place start = selectionManager.getFirstSelectedPlace();
        Place goal = selectionManager.getSecondSelectedPlace();

        Path<Place> path = model.findPath(start,goal);
        if (path == null){
            showError("Path not found");
            resetModeAfterAction();
            return;
        }

        highlightPath(path);

        resetModeAfterAction();
    }

    private void highlightPath(Path<Place> path) {
        clearPathHighlight();

        List<Place> nodes = path.getNodes();

        for (int i = 0; i < nodes.size() - 1; i++) {
            Place from = nodes.get(i);
            Place to = nodes.get(i + 1);

            for (ConnectionView view : connectionViews) {
                if (view.connects(from, to)) {
                    view.highlight(Color.ORANGE);
                }
            }
        }

        showInfo("Path found","Total distance: " + path.getTotalWeight());
    }

    private void clearPathHighlight() {
        for (ConnectionView view : connectionViews) {
            view.unhighlight(PLACE_CIRCLE_COLOUR);
        }
    }

    // ---------- Save & load ----------

    private void saveMap(){
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Map");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Txt Files", "*.txt")
        );
        File file = fileChooser.showSaveDialog(root.getScene().getWindow());

        if (file == null) {
            return;
        }

        try {
            model.saveToFile(file, backgroundImagePath);
            showInfo("Success!", "Your map has been saved!");
            hasUnsavedChanges = false;
        }catch (IOException e){
            showError("Unable to save, try again!");
        }

    }

    private void loadMap(){
        if (!confirmDiscardUnsavedChanges()) {
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Load map");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Txt Files", "*.txt")
        );
        File file = fileChooser.showOpenDialog(root.getScene().getWindow());

        if (file == null) {
            return;
        }

        try {
            clearMapView();



            setBackgroundImage(model.loadFromFile(file));
            for (Place place : model.getPlaces()) {
                drawPlace(place);
            }
            for (Place place : model.getPlaces()){
                if(model.getConnections().containsKey(place)){
                    drawLine(place,model.getConnections().get(place));
                }
            }
            hasUnsavedChanges = false;
        } catch (FileNotFoundException e) {
            showError("File not found!");
        } catch (IOException e) {
          showError(e.getMessage());
        }
    }

    private void loadBackgroundImage(){
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Load Background");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image files", "*.jpg", "*.jpeg", "*.png")
        );

        File file = fileChooser.showOpenDialog(root.getScene().getWindow());



        if (file == null) {
            return;
        }

        Image background = new Image(file.toURI().toString());
        imageView.setImage(background);
        backgroundImagePath = file.getAbsolutePath();
        hasUnsavedChanges = true;
    }

    private void newMap() {
        if (!confirmDiscardUnsavedChanges()) {
            return;
        }

        model.clear();
        clearMapView();
        setBackgroundImage("/background.jpg");

        hasUnsavedChanges = false;
    }

    private void setBackgroundImage(String path) {
        if (path == null || path.isBlank()) {
            return;
        }

        try {
            Image background;

            File file = new File(path);

            if (file.exists()) {
                background = new Image(file.toURI().toString());
            } else {
                String resourcePath = path.startsWith("/") ? path : "/" + path;

                background = new Image(
                        Objects.requireNonNull(
                                MapApplication.class.getResourceAsStream(resourcePath)
                        )
                );
            }

            imageView.setImage(background);
            backgroundImagePath = path;

        } catch (Exception e) {
            showError("Could not load background image: " + path);
        }
    }

    private void clearMapView() {
        for (Group placeView : placeViews.values()) {
            mapPane.getChildren().remove(placeView);
        }

        for (ConnectionView connectionView : connectionViews) {
            mapPane.getChildren().remove(connectionView.getRoot());
        }

        placeViews.clear();
        placeCircles.clear();
        connectionViews.clear();
        selectionManager.clearSelection();
    }

    public boolean confirmDiscardUnsavedChanges() {
        if (!hasUnsavedChanges) {
            return true;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Unsaved changes");
        alert.setHeaderText("You have unsaved changes.");
        alert.setContentText("Do you want to continue and discard them?");

        Optional<ButtonType> result = alert.showAndWait();

        return result.isPresent() && result.get() == ButtonType.OK;
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
        alert.setContentText(errorMessage + "!");

        alert.showAndWait();
    }

    private void showInfo(String title, String message){
        Alert alert = new Alert(Alert.AlertType.INFORMATION);

        alert.setTitle("Info");
        alert.setHeaderText(title);
        alert.setContentText(message);

        alert.showAndWait();
    }

    // ---------- Event handlers ----------

    private class SelectHandler implements EventHandler<MouseEvent> {
        private final Place place;
        private final Group placeView;

        public SelectHandler(Place place, Group placeView) {
            this.place = place;
            this.placeView = placeView;
        }

        @Override
        public void handle(MouseEvent event) {
            handlePlaceClicked(place, placeView);
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
            hasUnsavedChanges = true;
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

    private class FindPathHandler implements EventHandler<ActionEvent>{

        @Override
        public void handle(ActionEvent event) {
            selectionManager.clearSelection();
            currentMode = Mode.FIND_PATH;
            mapPane.setCursor(Cursor.CROSSHAIR);
            findPath.setDisable(true);
        }
    }
}