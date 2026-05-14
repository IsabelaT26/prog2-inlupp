package se.su.inlupp;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Shape;
import javafx.scene.text.Text;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class SelectionManager {

    private static final Paint NORMAL_COLOUR = Color.RED;
    private static final Paint SELECTED_COLOUR = Color.ORANGE;

    private final LinkedList<Place> selectedPlaces = new LinkedList<>();
    private final Map<Place, Group> selectedPlaceViews = new HashMap<>();

    public void toggleSelection(Place place, Group placeView) {
        if (selectedPlaceViews.containsKey(place)) {
            deselect(place);
            return;
        }

        if (selectedPlaces.size() == 2) {
            Place oldestPlace = selectedPlaces.removeFirst();
            Group oldestView = selectedPlaceViews.remove(oldestPlace);
            setPlaceViewColour(oldestView, NORMAL_COLOUR);
        }

        selectedPlaces.add(place);
        selectedPlaceViews.put(place, placeView);
        setPlaceViewColour(placeView, SELECTED_COLOUR);
    }

    public int nrOfPlacesSelected() {
        return selectedPlaces.size();
    }

    public Place getFirstSelectedPlace() {
        return selectedPlaces.getFirst();
    }

    public Place getSecondSelectedPlace() {
        return selectedPlaces.get(1);
    }

    public Group getPlaceView(Place place) {
        return selectedPlaceViews.get(place);
    }

    public void clearSelection() {
        for (Group group : selectedPlaceViews.values()) {
            setPlaceViewColour(group, NORMAL_COLOUR);
        }

        selectedPlaceViews.clear();
        selectedPlaces.clear();
    }

    private void deselect(Place place) {
        Group group = selectedPlaceViews.remove(place);
        selectedPlaces.remove(place);

        if (group != null) {
            setPlaceViewColour(group, NORMAL_COLOUR);
        }
    }

    private void setPlaceViewColour(Group group, Paint colour) {
        if (group == null) {
            return;
        }

        for (Node node : group.getChildren()) {
            if (node instanceof Shape shape) {
                shape.setFill(colour);
            } else if (node instanceof Text text) {
                text.setFill(colour);
            }
        }
    }
}