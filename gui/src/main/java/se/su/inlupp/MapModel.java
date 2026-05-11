package se.su.inlupp;

public class MapModel {

    private final ListGraph<Place> graph = new ListGraph<>();

    public void addPlace(Place place) {
        if (place.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Place name cannot be empty");
        }

        graph.add(place);
    }

    public void removePlace(Place place){
        graph.remove(place);
    }

    public void connectPlaces(Place place1, Place place2, String pathWayName, int distance){
        if (pathWayName.trim().isEmpty()) {
            throw new IllegalArgumentException("Road name cannot be empty");
        }
        if (distance < 0){
            throw new IllegalArgumentException("Distance must be greater than 0");
        }

        graph.connect(place1,place2, pathWayName, distance);
    }
    //find path


    //switch bfs/dfs

    //save

    //load


}
