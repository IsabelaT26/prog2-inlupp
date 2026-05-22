package se.su.inlupp;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.input.MouseEvent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

public class MapView {

    private static final Color PLACE_CIRCLE_COLOUR = Color.DARKBLUE;
    private static final Color HIGHLIGHT_COLOUR = Color.GREEN;
    private final MapModel model;

    private final MapDialog dialogs = new MapDialog();

    private BorderPane root;
    private Pane mapPane;
    private final Button addPlace = new Button("Add place");
    private final Button  connect = new Button("Connect");
    private final Button removePlace = new Button("Remove Place");
    private final Button findPath = new Button("Find path");
    private final Button disconnect = new Button("Disconnect");
    private final Button clearPath = new Button("Clear path");

    private final MenuItem newItem = new MenuItem("New");
    private final MenuItem saveItem = new MenuItem("Save");
    private final MenuItem loadItem = new MenuItem("Load");
    private final MenuItem loadBackgroundItem = new MenuItem("Load background image");

    private final MenuItem dfs = new MenuItem("Depth First Search");
    private final MenuItem  bfs = new MenuItem("Breadth First Search");

    private ImageView imageView;
    private String backgroundImagePath = "/background.jpg";
    private final Image originalBackgroundImage = new Image(
            Objects.requireNonNull(
                    MapApplication.class.getResourceAsStream("/background.jpg")
            )
    );
    private boolean hasUnsavedChanges = false;

    private final List<ConnectionView> connectionViews = new ArrayList<>();

    private final Map<Place, PlaceView> placeViews = new HashMap<>();

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

        dfs.setOnAction(new DFSHandler());
        bfs.setOnAction(new BFSHandler());

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

        imageView = new ImageView(originalBackgroundImage);
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

        Optional<String> placeName = dialogs.askForPlaceName();

        if (placeName.isPresent()) {
            String name = placeName.get().trim();

            if (name.isEmpty()) {
                dialogs.showError("Place name cannot be empty.");
                resetAddPlaceMode();
                return;
            }

            Place newPlace = new Place(name, x, y);

            try {
                model.addPlace(newPlace);
                drawPlace(newPlace);
            } catch (IllegalArgumentException e) {
                dialogs.showError(e.getMessage());
            }
        }

        resetAddPlaceMode();
    }

    // ---------- Drawing ----------

    private void drawPlace(Place place) {
        PlaceView placeView = new PlaceView(place);
        placeView.getRoot().setOnMousePressed(new SelectHandler(place, placeView));
        placeView.getRoot().setOnMouseDragged(new DragHandler(placeView));

        placeViews.put(place, placeView);
        hasUnsavedChanges = true;

        mapPane.getChildren().add(placeView.getRoot());
    }

    private void drawLine(Place place1, Place place2) {
        PlaceView placeView1 = placeViews.get(place1);
        PlaceView placeView2 = placeViews.get(place2);

        ConnectionView connectionView = new ConnectionView(
                place1,
                place2,
                placeView1.getCircle(),
                placeView2.getCircle(),
                model.getConnectionName(place1, place2),
                PLACE_CIRCLE_COLOUR
        );

        connectionViews.add(connectionView);

        mapPane.getChildren().add(1,connectionView.getRoot());
        hasUnsavedChanges = true;
    }

    //-----Select Place Feature-----

    private void handlePlaceClicked(Place place, PlaceView placeView) {
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
                dialogs.showError(place1.getName() +" and "+ place2.getName() + " are already connected!");
                resetModeAfterAction();
                return;
            }
        }

        Optional<RoadInfo> roadInfo = dialogs.askForRoadInfo();

        if(roadInfo.isPresent()) {
            String roadName = roadInfo.get().name();
            int roadDistance = roadInfo.get().distance();

            try {
                model.connectPlaces(place1, place2, roadName, roadDistance);
                drawLine(place1, place2);
                hasUnsavedChanges = true;
            } catch (IllegalArgumentException e) {
                dialogs.showError(e.getMessage());
            }
        }
        resetModeAfterAction();
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
            dialogs.showError(place1.getName() +" and "+ place2.getName() + " do not have a connection!");
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
            PlaceView placeView = placeViews.get(place);

            boolean removalApproved = dialogs.confirmRemovePlace(place.getName());

            if(!removalApproved){
                resetModeAfterAction();
                return;
            }

            model.removePlace(place);
            mapPane.getChildren().remove(placeView.getRoot());

            placeViews.remove(place);

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
            dialogs.showError("Path not found");
            resetModeAfterAction();
            return;
        }



        resetModeAfterAction();
        highlightPath(path);
    }

    private void highlightPath(Path<Place> path) {
        clearPathHighlight();

        List<Place> nodes = path.getNodes();
        for (Place place : nodes) {
            PlaceView placeView = placeViews.get(place);

            if (placeView != null) {
                placeView.setNormal();
            }
        }

        for (int i = 0; i < nodes.size() - 1; i++) {
            Place from = nodes.get(i);
            Place to = nodes.get(i + 1);


            for (ConnectionView view : connectionViews) {
                if (view.connects(from, to)) {
                    view.highlight(HIGHLIGHT_COLOUR);
                }
            }
        }

        dialogs.showInfo("Path found","Total distance: " + path.getTotalWeight());
    }

    private void clearPathHighlight() {
        for (ConnectionView view : connectionViews) {
            view.unhighlight(PLACE_CIRCLE_COLOUR);
        }

        for (PlaceView placeView : placeViews.values()) {
            placeView.setNormal();
        }
    }

    // ---------- Save & load ----------

    private void saveMap(){
        if(model.getPlaces().isEmpty()){
            dialogs.showError("There is nothing to save. Add at least one place first");
            return;
        }

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
            dialogs.showInfo("Success!", "Your map has been saved!");
            hasUnsavedChanges = false;
        }catch (IOException e){
            dialogs.showError("Unable to save, try again!");
        }

    }

    private void loadMap(){
        if (confirmDiscardUnsavedChanges()) {
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
            dialogs.showError("File not found!");
        } catch (IOException e) {
          dialogs.showError(e.getMessage());
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
        if (confirmDiscardUnsavedChanges()) {
            return;
        }

        model.clear();
        clearMapView();
        setBackgroundImage("/background.jpg");

        hasUnsavedChanges = false;
    }

    private void setBackgroundImage(String path) {
        if (path == null || path.isBlank()) {
            imageView.setImage(originalBackgroundImage);
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
            dialogs.showError("Could not load the saved background image. The default background will be used instead." );
            imageView.setImage(originalBackgroundImage);
        }
    }

    private void clearMapView() {
        for (PlaceView placeView : placeViews.values()) {
            mapPane.getChildren().remove(placeView.getRoot());
        }

        for (ConnectionView connectionView : connectionViews) {
            mapPane.getChildren().remove(connectionView.getRoot());
        }

        placeViews.clear();
        connectionViews.clear();
        selectionManager.clearSelection();
    }

    public boolean confirmDiscardUnsavedChanges() {
        if (!hasUnsavedChanges) {
            return false;
        }

        return !dialogs.confirmDiscardUnsavedChanges();
    }

    // ---------- Event handlers ----------

    private class SelectHandler implements EventHandler<MouseEvent> {
        private final Place place;
        private final PlaceView placeView;

        public SelectHandler(Place place, PlaceView placeView) {
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

        private final PlaceView placeView;

        public DragHandler(PlaceView placeView) {
            this.placeView = placeView;
        }

        @Override
        public void handle(MouseEvent event) {
            Point2D point = mapPane.sceneToLocal(event.getSceneX(), event.getSceneY());

            placeView.moveTo(point.getX(), point.getY());

            hasUnsavedChanges = true;

            event.consume();
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
            if(model.getConnections().isEmpty()){
                dialogs.showError("There are no roads on the map yet. Connect places before finding a path");
                return;
            }
            selectionManager.clearSelection();
            currentMode = Mode.FIND_PATH;
            mapPane.setCursor(Cursor.CROSSHAIR);
            findPath.setDisable(true);
        }
    }

    private class BFSHandler implements EventHandler<ActionEvent> {
        @Override
        public void handle(ActionEvent event) {
            model.useBFS();
            dialogs.showInfo("PathFinding Algorithm changed", "Pathfinding algorithm set to Breadth First Search.");
        }
    }

    private class DFSHandler implements EventHandler<ActionEvent> {
        @Override
        public void handle(ActionEvent event) {
            model.useDFS();
            dialogs.showInfo("PathFinding Algorithm changed", "Pathfinding algorithm set to Depth First Search.");
        }
    }
}