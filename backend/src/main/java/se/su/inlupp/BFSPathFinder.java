package se.su.inlupp;

import java.util.*;

public class BFSPathFinder<T> implements PathFinder<T> {

  @Override
  public Path<T> findPath(Graph<T> graph, T start, T goal) {
    if(!graph.hasNode(start) || !graph.hasNode(goal)){
      return null;
    }

    Queue<T> queue = new LinkedList<>();
    Map<T, T> parentMap = new HashMap<>();

    parentMap.put(start, null);
    queue.add(start);

    boolean found = false;

    while (!queue.isEmpty() && !found) {
      T current = queue.poll();

      for (Edge<T> edge : graph.getEdgesFrom(current)) {
        T neighbor = edge.getDestination();

        if (!parentMap.containsKey(neighbor)) {
          parentMap.put(neighbor, current);
          queue.add(neighbor);

          if (neighbor.equals(goal)) {
            found = true;
            break;
          }
        }
      }
    }

    // --- STEP 2: Reconstruct path ---
    LinkedList<Edge<T>> path = new LinkedList<>();
    T current = goal;

    while (!current.equals(start)) {
      T parent = parentMap.get(current);

      // No path exists
      if (parent == null) {
        return null;
      }

      // Build edge from parent → current
      Edge<T> edge = graph.getEdgeBetween(parent, current);

      // Add to front (because we're going backwards)
      path.addFirst(edge);

      // Move one step toward the start
      current = parent;
    }

    return new GraphPath<>(start,path);
  }
}

