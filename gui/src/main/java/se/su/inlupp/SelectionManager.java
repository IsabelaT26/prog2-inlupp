package se.su.inlupp;

import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class SelectionManager {

    private static final Paint SELECTED_COLOUR = Color.CYAN;

    private final LinkedList<Place> selectedPlaces = new LinkedList<>();
    private final Map<Place, PlaceView> selectedPlaceViews = new HashMap<>();

    public void toggleSelection(Place place, PlaceView placeView) {
        if (selectedPlaceViews.containsKey(place)) {
            deselect(place);
            return;
        }

        if (selectedPlaces.size() == 2) {
            Place oldestPlace = selectedPlaces.removeFirst();
            PlaceView oldestView = selectedPlaceViews.remove(oldestPlace);

            if (oldestView != null) {
                oldestView.setNormal();
            }
        }

        selectedPlaces.add(place);
        selectedPlaceViews.put(place, placeView);
        placeView.setSelected(SELECTED_COLOUR);
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


    public void clearSelection() {
        for (PlaceView placeView : selectedPlaceViews.values()) {
            placeView.setNormal();
        }

        selectedPlaceViews.clear();
        selectedPlaces.clear();
    }

    private void deselect(Place place) {
        PlaceView placeView = selectedPlaceViews.remove(place);
        selectedPlaces.remove(place);

        if (placeView != null) {
            placeView.setNormal();
        }
    }
}