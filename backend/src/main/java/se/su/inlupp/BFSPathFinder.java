package se.su.inlupp;

import java.util.*;

public class BFSPathFinder<T> implements PathFinder<T> {

  @Override
  public Path<T> findPath(Graph<T> graph, T start, T goal) {
    if (!graph.hasNode(start) || !graph.hasNode(goal)) {
      return null;
    }

    Map<T, T> parentMap = buildParentMap(graph, start, goal);

    if (!parentMap.containsKey(goal)) {
      return null;
    }

    List<Edge<T>> path = reconstructPath(graph, start, goal, parentMap);

    return new GraphPath<>(start, path);
  }

  private Map<T, T> buildParentMap(Graph<T> graph, T start, T goal) {
    Queue<T> queue = new LinkedList<>();
    Map<T, T> parentMap = new HashMap<>();

    parentMap.put(start, null);
    queue.add(start);

    while (!queue.isEmpty()) {
      T current = queue.poll();

      if (current.equals(goal)) {
        break;
      }

      for (Edge<T> edge : graph.getEdgesFrom(current)) {
        T neighbor = edge.getDestination();

        if (!parentMap.containsKey(neighbor)) {
          parentMap.put(neighbor, current);
          queue.add(neighbor);
        }
      }
    }

    return parentMap;
  }

  private List<Edge<T>> reconstructPath(Graph<T> graph, T start, T goal, Map<T, T> parentMap) {
    LinkedList<Edge<T>> path = new LinkedList<>();
    T current = goal;

    while (!current.equals(start)) {
      T parent = parentMap.get(current);

      Edge<T> edge = graph.getEdgeBetween(parent, current);
      path.addFirst(edge);

      current = parent;
    }

    return path;
  }
}

