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
        if (place.getName().contains(",")) {
            throw new IllegalArgumentException("Name cannot contain commas.");
        }

        if (getPlaces().contains(place)) {
            throw new IllegalArgumentException(place.getName() + " already exist on map");
        }

        graph.add(place);
    }

    public void removePlace(Place place) {
        try{
            graph.remove(place);
        }catch (NoSuchElementException e){
            throw new IllegalArgumentException("This place doesn't exist in the graph");
        }

    }

    public void connectPlaces(Place place1, Place place2, String pathWayName, int distance) {
        if (place1.equals(place2)) {
            throw new IllegalArgumentException("A place cannot connect to itself");
        }

        if (pathWayName.trim().isEmpty()) {
            throw new IllegalArgumentException("Road name cannot be empty");
        }

        if (pathWayName.contains(",")) {
            throw new IllegalArgumentException("Name cannot contain commas");
        }

        try {
            graph.connect(place1, place2, pathWayName, distance);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Distance must be greater than 0");
        } catch (NoSuchElementException e) {
            throw new IllegalArgumentException("One or both places do not exist in the graph");
        } catch (IllegalStateException e) {
            throw new IllegalArgumentException("These places are already connected");
        }
    }

    public void disconnectPlaces(Place place1, Place place2) {
        try {
            graph.disconnect(place1, place2);
        } catch (IllegalStateException e) {
            throw new IllegalArgumentException("Places are not connected");
        } catch (NoSuchElementException e) {
            throw new IllegalArgumentException("One or both places do not exist in the graph");
        }
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

        String backgroundImagePath = "";

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            boolean readingPlaces = false;
            boolean readingConnections = false;

            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }

                if (line.equals("BACKGROUND")) {
                    backgroundImagePath = reader.readLine();
                    continue;
                }

                if (line.equals("PLACES")) {
                    readingPlaces = true;
                    readingConnections = false;
                    continue;
                }

                if (line.equals("CONNECTIONS")) {
                    readingPlaces = false;
                    readingConnections = true;
                    continue;
                }

                if (readingPlaces) {
                    readPlace(line);
                } else if (readingConnections) {
                    readConnection(line);
                }
            }
        }

        return backgroundImagePath;
    }

    private void readPlace(String line) throws IOException {
        String[] placeParts = line.split(",");

        if (placeParts.length != 3) {
            throw new IOException("Invalid place line: " + line);
        }

        try {
            String name = placeParts[0];
            double x = Double.parseDouble(placeParts[1]);
            double y = Double.parseDouble(placeParts[2]);

            Place place = new Place(name, x, y);
            addPlace(place);
        } catch (NumberFormatException e) {
            throw new IOException("Invalid coordinates in place line: " + line);
        }
    }

    private void readConnection(String line) throws IOException {
        String[] connectionParts = line.split(",");

        if (connectionParts.length != 4) {
            throw new IOException("Invalid connection line: " + line);
        }

        try {
            Place placeFrom = findPlaceByName(connectionParts[0]);
            Place placeTo = findPlaceByName(connectionParts[1]);
            String name = connectionParts[2];
            int distance = Integer.parseInt(connectionParts[3]);

            if (placeFrom == null || placeTo == null) {
                throw new IOException("Connection refers to unknown place: " + line);
            }

            connectPlaces(placeFrom, placeTo, name, distance);
        }catch(NumberFormatException e){
            throw new IOException("Invalid distance in connection line " + line);
        }
    }

    private Place findPlaceByName(String name) {
        for (Place place : graph.getNodes()) {
            if (place.getName().equals(name)) {
                return place;
            }
        }

        return null;
    }

    //Other useful methods

    public Set<Place> getPlaces() {
        return graph.getNodes();
    }

    public List<RoadInfo> getRoads() {
        List<RoadInfo> roads = new ArrayList<>();

        for (Place from : graph.getNodes()) {
            for (Edge<Place> edge : graph.getEdgesFrom(from)) {
                Place to = edge.getDestination();

                if (from.getName().compareTo(to.getName()) < 0) { //what guarantees the same road doesn't appear twice
                    roads.add(new RoadInfo(
                            from,
                            to,
                            edge.getName(),
                            edge.getWeight()
                    ));
                }
            }
        }

        return roads;
    }

    public void clear() {
        graph = new ListGraph<>();
    }


}
