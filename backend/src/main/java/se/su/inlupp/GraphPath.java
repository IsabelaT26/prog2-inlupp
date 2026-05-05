package se.su.inlupp;

import java.util.*;

public class GraphPath<T> implements Path<T> {

    private final List<Edge<T>> edges;
    private final T startNode;

    public GraphPath(T startNode, List<Edge<T>> edges) {
        this.edges = edges;
        this.startNode = startNode;
    }

    @Override
    public T getStart() {
        return startNode;
    }

    @Override
    public T getEnd() {
        if (edges.isEmpty()) {
            return startNode;
        }
        return edges.get(edges.size() - 1).getDestination();
    }

    @Override
    public int getTotalWeight() {
        int totalWeight = 0;
        for (Edge<T> e : edges) {
            totalWeight += e.getWeight();
        }
        return totalWeight;
    }

    @Override
    public List<Edge<T>> getEdges() {
        return Collections.unmodifiableList(edges);
    }

    @Override
    public List<T> getNodes() {
        List<T> nodes = new ArrayList<>();
        nodes.add(startNode);
        for (Edge<T> e : edges) {
            nodes.add(e.getDestination());
        }
        return nodes;
    }

    @Override
    public Iterator<Edge<T>> iterator() {
        return edges.iterator();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("From ").append(getStart())
                .append(" to ").append(getEnd()).append(":\n");

        for (Edge<T> e : edges) {
            sb.append("  -> ")
                    .append(e.toString());
        }

        sb.append("Total weight: ").append(getTotalWeight());

        return sb.toString();
    }
}
