package se.su.inlupp;

import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;

import java.util.*;

public class SelectionManager {

    private static final Paint NORMAL_COLOUR = Color.RED;
    private static final Paint SELECTED_COLOUR = Color.ORANGE;

    private final LinkedList<Place> selectedPlaces = new LinkedList<>();
    private final Map<Place, Circle> selectedCircles = new HashMap<>();

    public void toggleSelection(Place place, Circle circle) {
        if (selectedCircles.containsKey(place)) {
            deselect(place);
            return;
        }

        if (selectedPlaces.size() == 2) {
            Place oldestPlace = selectedPlaces.removeFirst();
            Circle oldestCircle = selectedCircles.remove(oldestPlace);
            oldestCircle.setFill(NORMAL_COLOUR);
        }

        selectedPlaces.add(place);
        selectedCircles.put(place, circle);
        circle.setFill(SELECTED_COLOUR);
    }

    public int nrOfPlacesSelected() {
        return selectedPlaces.size();
    }

    public Place getFirstSelectedPlace() {
        return selectedPlaces.get(0);
    }

    public Place getSecondSelectedPlace() {
        return selectedPlaces.get(1);
    }

    public Circle getCircle(Place place){
        return selectedCircles.get(place);
    }

    public void clearSelection() {
        selectedCircles.values().forEach(circle -> circle.setFill(NORMAL_COLOUR));
        selectedCircles.clear();
        selectedPlaces.clear();
    }

    private void deselect(Place place) {
        Circle circle = selectedCircles.remove(place);
        selectedPlaces.remove(place);

        if (circle != null) {
            circle.setFill(NORMAL_COLOUR);
        }
    }
}