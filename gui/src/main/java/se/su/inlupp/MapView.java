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

    // Constants

    private static final Color NORMAL_COLOUR = Color.DARKBLUE;
    private static final Color HIGHLIGHT_COLOUR = Color.GREEN;
    private static final FileChooser.ExtensionFilter TXT_FILTER =
            new FileChooser.ExtensionFilter("Txt Files", "*.txt");

    private static final FileChooser.ExtensionFilter IMAGE_FILTER =
            new FileChooser.ExtensionFilter("Image files", "*.jpg", "*.jpeg", "*.png");
    private static final String DEFAULT_BACKGROUND_PATH = "/background.jpg";

    // Model and helpers

    private final MapModel model;
    private final MapDialog dialogs = new MapDialog();
    private final SelectionManager selectionManager = new SelectionManager();

    // JavaFX layout fields

    private BorderPane root;
    private Pane mapPane;
    private ImageView imageView;

    // Buttons and menu items

    private final Button addPlace = new Button("Add place");
    private final Button connect = new Button("Connect");
    private final Button removePlace = new Button("Remove Place");
    private final Button findPath = new Button("Find path");
    private final Button disconnect = new Button("Disconnect");
    private final Button clearPath = new Button("Clear path");

    private final MenuItem newItem = new MenuItem("New");
    private final MenuItem saveItem = new MenuItem("Save");
    private final MenuItem loadItem = new MenuItem("Load");
    private final MenuItem loadBackgroundItem = new MenuItem("Load background image");

    private final MenuItem dfs = new MenuItem("Depth First Search");
    private final MenuItem bfs = new MenuItem("Breadth First Search");

    // View state

    private String currentBackgroundPath = DEFAULT_BACKGROUND_PATH;

    private final Image defaultBackgroundImage = new Image(
            Objects.requireNonNull(
                    MapApplication.class.getResourceAsStream(DEFAULT_BACKGROUND_PATH)
            )
    );

    private boolean hasUnsavedChanges = false;

    private final List<ConnectionView> connectionViews = new ArrayList<>();
    private final Map<Place, PlaceView> placeViews = new HashMap<>();

    private Mode currentMode = Mode.NORMAL;

    // Constructor and public methods

    public MapView(MapModel model) {
        this.model = model;

        createLayout();
    }

    public BorderPane getRoot() {
        return root;
    }

    public boolean shouldCancelDueToUnsavedChanges() {
        if (!hasUnsavedChanges) {
            return false;
        }

        return !dialogs.confirmDiscardUnsavedChanges();
    }

    // Layout creation

    private void createLayout() {
        root = new BorderPane();

        root.setTop(createTop());
        root.setCenter(createMapPane());
    }

    private VBox createTop() {
        Menu fileMenu = new Menu("File");

        newItem.setOnAction(event -> newMap());
        saveItem.setOnAction(event -> saveMap());
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

        ToolBar toolBar = new ToolBar(addPlace, removePlace, connect, disconnect, findPath, clearPath);

        return new VBox(menuBar, toolBar);
    }

    private Pane createMapPane() {
        mapPane = new Pane();

        imageView = new ImageView(defaultBackgroundImage);
        imageView.setPreserveRatio(false);

        imageView.fitWidthProperty().bind(mapPane.widthProperty());
        imageView.fitHeightProperty().bind(mapPane.heightProperty());

        mapPane.getChildren().add(imageView);

        return mapPane;
    }

    // Menu/button actions

    private void resetToDefaultBackground() {
        imageView.setImage(defaultBackgroundImage);
        currentBackgroundPath = DEFAULT_BACKGROUND_PATH;
    }

    private void newMap() {
        if (shouldCancelDueToUnsavedChanges()) {
            return;
        }

        model.clear();
        clearMapView();
        resetToDefaultBackground();

        hasUnsavedChanges = false;
    }

    private void saveMap() {
        if (model.getPlaces().isEmpty()) {
            dialogs.showError("There is nothing to save. Add at least one place first");
            return;
        }

        File file = chooseFileToSave("Save Map", TXT_FILTER);

        if (file == null) {
            return;
        }

        try {
            model.saveToFile(file, currentBackgroundPath);
            dialogs.showInfo("Success!", "Your map has been saved!");
            hasUnsavedChanges = false;
        } catch (IOException e) {
            dialogs.showError("Unable to save, try again!");
        }
    }

    private void loadMap() {
        if (shouldCancelDueToUnsavedChanges()) {
            return;
        }

        File file = chooseFileToOpen("Load Map", TXT_FILTER);

        if (file == null) {
            return;
        }

        try {
            clearMapView();
            setBackgroundImage(model.loadFromFile(file));

            for (Place place : model.getPlaces()) {
                drawPlace(place);
            }

            for (RoadInfo road : model.getRoads()) {
                drawLine(road);
            }

            hasUnsavedChanges = false;
        } catch (FileNotFoundException e) {
            dialogs.showError("File not found!");
        } catch (IOException e) {
            dialogs.showError(e.getMessage());
        }
    }

    private void loadBackgroundImage() {
        File file = chooseFileToOpen("Load Background", IMAGE_FILTER);

        if (file == null) {
            return;
        }

        Image background = new Image(file.toURI().toString());
        imageView.setImage(background);
        currentBackgroundPath = file.getAbsolutePath();
        hasUnsavedChanges = true;
    }

    private void setBackgroundImage(String path) {
        if (path == null || path.isBlank() || path.equals(DEFAULT_BACKGROUND_PATH)) {
            resetToDefaultBackground();
            return;
        }

        File file = new File(path);

        if (!file.exists()) {
            resetToDefaultBackground();
            dialogs.showError("The saved background image could not be found. The default background was used instead.");
            return;
        }

        Image background = new Image(file.toURI().toString());
        imageView.setImage(background);
        currentBackgroundPath = path;
    }

    private File chooseFileToOpen(String title, FileChooser.ExtensionFilter filter) {
        FileChooser fileChooser = createFileChooser(title, filter);
        return fileChooser.showOpenDialog(root.getScene().getWindow());
    }

    private File chooseFileToSave(String title, FileChooser.ExtensionFilter filter) {
        FileChooser fileChooser = createFileChooser(title, filter);
        return fileChooser.showSaveDialog(root.getScene().getWindow());
    }

    private FileChooser createFileChooser(String title, FileChooser.ExtensionFilter filter) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);
        fileChooser.getExtensionFilters().add(filter);
        return fileChooser;
    }

    // Add place

    private void enterAddPlaceMode() {
        mapPane.setOnMouseClicked(this::handleAddPlaceClick);
        mapPane.setCursor(Cursor.CROSSHAIR);
        addPlace.setDisable(true);
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

    private void resetAddPlaceMode() {
        mapPane.setCursor(Cursor.DEFAULT);
        addPlace.setDisable(false);
        mapPane.setOnMouseClicked(null);
    }

    // Selection and mode handling

    private void handlePlaceClicked(Place place, PlaceView placeView) {
        // if in normal state, the user shouldn't be able to select,
        // we want it to only be able to do that if a specific action is to be performed.
        if (currentMode == Mode.NORMAL) {
            return;
        }

        selectionManager.toggleSelection(place, placeView);

        switch (currentMode) {
            case REMOVE:
                handleRemoveSelection();
                break;

            case FIND_PATH:
                handleFindPathSelection();
                break;

            case CONNECT:
                handleConnectSelection();
                break;

            case DISCONNECT:
                handleDisconnectSelection();
                break;
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

    // Connect/disconnect

    private void handleConnectSelection() {
        if (selectionManager.nrOfPlacesSelected() < 2) {
            return;
        }

        Place place1 = selectionManager.getFirstSelectedPlace();
        Place place2 = selectionManager.getSecondSelectedPlace();

        for (ConnectionView view : connectionViews) {
            if (view.connects(place1, place2)) {
                dialogs.showError(place1.getName() + " and " + place2.getName() + " are already connected!");
                resetModeAfterAction();
                return;
            }
        }

        Optional<RoadInfo> roadInfo = dialogs.askForRoadInfo(place1, place2);

        if (roadInfo.isPresent()) {
            String roadName = roadInfo.get().name();
            int roadDistance = roadInfo.get().distance();

            try {
                model.connectPlaces(place1, place2, roadName, roadDistance);
                drawLine(roadInfo.get());
                hasUnsavedChanges = true;
            } catch (IllegalArgumentException e) {
                dialogs.showError(e.getMessage());
            }
        }

        resetModeAfterAction();
    }

    private void handleDisconnectSelection() {
        if (selectionManager.nrOfPlacesSelected() < 2) {
            return;
        }

        Place place1 = selectionManager.getFirstSelectedPlace();
        Place place2 = selectionManager.getSecondSelectedPlace();

        ConnectionView toRemove = null;

        for (ConnectionView view : connectionViews) {
            if (view.connects(place1, place2)) {
                toRemove = view;
                break;
            }
        }

        if (toRemove == null) {
            dialogs.showError(place1.getName() + " and " + place2.getName() + " do not have a direct connection!");
            resetModeAfterAction();
            return;
        }

        try{
            model.disconnectPlaces(place1, place2);
            connectionViews.remove(toRemove);
            mapPane.getChildren().remove(toRemove.getRoot());
            hasUnsavedChanges = true;
        }catch (IllegalArgumentException e){
           dialogs.showError(e.getMessage());
        }

        resetModeAfterAction();
    }

    // Remove place

    private void handleRemoveSelection() {
        if (selectionManager.nrOfPlacesSelected() != 1) {
            return;
        }

        Place place = selectionManager.getFirstSelectedPlace();

        if (!dialogs.confirmRemovePlace(place.getName())) {
            resetModeAfterAction();
            return;
        }

        removePlaceFromMap(place);
        removeConnectionsFromRemovedPlace(place);

        hasUnsavedChanges = true;
        resetModeAfterAction();
    }

    private void removePlaceFromMap(Place place){
        PlaceView placeView = placeViews.get(place);

        try {
            model.removePlace(place);
            mapPane.getChildren().remove(placeView.getRoot());
            placeViews.remove(place);
        }catch (IllegalArgumentException e){
            dialogs.showError(e.getMessage());
        }


    }

    private void removeConnectionsFromRemovedPlace(Place place){
        Iterator<ConnectionView> iterator = connectionViews.iterator();

        while (iterator.hasNext()) {
            ConnectionView view = iterator.next();

            if (view.comesFrom(place)) {
                mapPane.getChildren().remove(view.getRoot());
                iterator.remove();
            }
        }
    }

    // Path finding

    private void handleFindPathSelection() {
        if (selectionManager.nrOfPlacesSelected() < 2) {
            return;
        }

        Place start = selectionManager.getFirstSelectedPlace();
        Place goal = selectionManager.getSecondSelectedPlace();

        Path<Place> path = model.findPath(start, goal);

        if (path == null) {
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
                placeView.highlight(HIGHLIGHT_COLOUR);
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

        dialogs.showPath(path);
    }

    private void clearPathHighlight() {
        for (ConnectionView view : connectionViews) {
            view.unhighlight(NORMAL_COLOUR);
        }

        for (PlaceView placeView : placeViews.values()) {
            placeView.setNormal();
        }
    }

    // Drawing

    private void drawPlace(Place place) {
        PlaceView placeView = new PlaceView(place, NORMAL_COLOUR);

        placeView.getRoot().setOnMousePressed(new SelectHandler(place, placeView));
        placeView.getRoot().setOnMouseDragged(new DragHandler(placeView));

        placeViews.put(place, placeView);
        mapPane.getChildren().add(placeView.getRoot());

        hasUnsavedChanges = true;
    }

    private void drawLine(RoadInfo roadInfo) {

        PlaceView placeView1 = placeViews.get(roadInfo.from());
        PlaceView placeView2 = placeViews.get(roadInfo.to());

        ConnectionView connectionView = new ConnectionView(
                placeView1,
                placeView2,
                roadInfo.name(),
                NORMAL_COLOUR
        );

        connectionViews.add(connectionView);
        mapPane.getChildren().add(1, connectionView.getRoot());

        hasUnsavedChanges = true;
    }

    // Reset/helper methods

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

    // Event handler classes

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

    private class DisconnectButtonHandler implements EventHandler<ActionEvent> {
        @Override
        public void handle(ActionEvent event) {
            selectionManager.clearSelection();
            currentMode = Mode.DISCONNECT;
            mapPane.setCursor(Cursor.CROSSHAIR);
            disconnect.setDisable(true);
        }
    }

    private class RemoveButtonHandler implements EventHandler<ActionEvent> {
        @Override
        public void handle(ActionEvent event) {
            selectionManager.clearSelection();
            currentMode = Mode.REMOVE;
            mapPane.setCursor(Cursor.CROSSHAIR);
            removePlace.setDisable(true);
        }
    }

    private class FindPathHandler implements EventHandler<ActionEvent> {
        @Override
        public void handle(ActionEvent event) {
            if (model.getRoads().isEmpty()) {
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