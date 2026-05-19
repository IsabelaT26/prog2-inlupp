package se.su.inlupp;

import java.io.*;
import java.util.*;

public class MapModel {

    private ListGraph<Place> graph = new ListGraph<>();
    private PathFinder<Place> pathFinder = new BFSPathFinder<>();

    public void addPlace(Place place) {
        if (place.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Place name cannot be empty");
        }
        if (getPlaces().contains(place)){
            throw new IllegalArgumentException(place.getName() + " already exist on map");
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

    public void disconnectPlaces(Place place1, Place place2){
        graph.disconnect(place1,place2);
    }

    public String getConnectionName(Place place1, Place place2){
        return graph.getEdgeBetween(place1,place2).getName();
    }

    public void useDFS() {
        pathFinder = new DFSPathFinder<>();
    }

    public void useBFS() {
        pathFinder = new BFSPathFinder<>();
    }

    public Path<Place> findPath(Place start, Place goal) {
        return pathFinder.findPath(graph, start, goal);
    }


    //save

    public void saveToFile(File file, String backgroundImagePath) throws IOException {
        FileWriter writer = new FileWriter(file);

        writer.write("BACKGROUND\n");
        writer.write(backgroundImagePath + "\n");

        writer.write("PLACES\n");
        for (Place place : graph.getNodes()) {
            writer.write(place.getName());
            writer.write(",");
            writer.write(String.valueOf(place.getX()));
            writer.write(",");
            writer.write(String.valueOf(place.getY()));
            writer.write("\n");
        }
        writer.write("CONNECTIONS\n");
        Set<String> savedConnections = new HashSet<>();
        for (Place place : graph.getNodes()) {
            for (Edge<Place> connection : graph.getEdgesFrom(place)) {
                String from = place.getName();
                String to = connection.getDestination().getName();

                String key1 = from + "--" + to;
                String key2 = to + "--" + from;

                if (!savedConnections.contains(key1) && !savedConnections.contains(key2)) {
                    savedConnections.add(key1);
                    writer.write(from);
                    writer.write(",");
                    writer.write(to);
                    writer.write(",");
                    writer.write(connection.getName());
                    writer.write(",");
                    writer.write(String.valueOf(connection.getWeight()));
                    writer.write("\n");
                }

            }
        }
        writer.close();
    }

    //load

    public String loadFromFile(File file) throws IOException {
        graph = new ListGraph<>();

        BufferedReader reader = new BufferedReader(new FileReader(file));

        String line;
        String backgroundImagePath = "";

        boolean readingPlaces = false;
        boolean readingConnections = false;

        Map<String, Place> loadedPlaces = new HashMap<>();


        while ((line = reader.readLine()) != null) {
            if (line.isBlank()) {
                continue;
            }

            if(line.equals("BACKGROUND")) {
                backgroundImagePath = reader.readLine();
                continue;
            }

            if(line.equals("PLACES")){
                readingPlaces = true;
                readingConnections = false;
                continue;
            }
            if(line.equals("CONNECTIONS")){
                readingPlaces = false;
                readingConnections = true;
                continue;
            }
            if(readingPlaces){
                String[] placeParts = line.split(",");

                if (placeParts.length != 3) {
                    throw new IOException("Invalid place line: " + line);
                }

                String name = placeParts[0];
                double x = Double.parseDouble(placeParts[1]);
                double y = Double.parseDouble(placeParts[2]);

                Place place = new Place(name, x, y);
                loadedPlaces.put(name,place);
                addPlace(place);
            }
            if(readingConnections){
                String[] connectionParts = line.split(",");

                if (connectionParts.length != 4) {
                    throw new IOException("Invalid connection line: " + line);
                }

                String from = connectionParts[0];
                String to = connectionParts[1];
                String name = connectionParts[2];
                int distance = Integer.parseInt(connectionParts[3]);

                Place placeFrom = loadedPlaces.get(from);
                Place placeTo = loadedPlaces.get(to);

                if (placeFrom == null || placeTo == null) {
                    throw new IOException("Connection refers to unknown place: " + line);
                }

                connectPlaces(placeFrom,placeTo, name, distance);
            }
        }
        reader.close();
        return backgroundImagePath;
    }

    //Other useful methods

    public Set<Place> getPlaces(){
        return graph.getNodes();
    }

    public Map<Place, Place> getConnections(){
        Map<Place,Place> connections = new HashMap<>();
        for(Place place : getPlaces()){
            Collection<Edge<Place>> edges = graph.getEdgesFrom(place);
            for(Edge<Place> edge : edges){
                Place neighbor = edge.getDestination();
                connections.put(place,neighbor);
            }
        }
        return connections;
    }

    public void clear(){
        graph = new ListGraph<>();
    }


}
