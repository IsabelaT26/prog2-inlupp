package se.su.inlupp;

import java.util.*;

public class DFSPathFinder<T> implements PathFinder<T> {

  @Override
  public Path<T> findPath(Graph<T> graph, T start, T goal) {
    if(!graph.hasNode(start) || !graph.hasNode(goal)){
      return null;
    }

    Set<T> visited = new HashSet<>();
    LinkedList<Edge<T>> path = new LinkedList<>();

    if(dfs(start, goal, visited, graph, path)){
      return new GraphPath<>(start, path);
    }

    return null;
  }

  private boolean dfs(T current, T goal, Set<T> visited, Graph<T> graph, List<Edge<T>> path){
    if(current.equals(goal)){
      return true;
    }

    visited.add(current);

    for(Edge<T> edge :graph.getEdgesFrom(current)){
      T neighbor = edge.getDestination();

      if(!visited.contains(neighbor)){
        path.add(edge);
        if (dfs(neighbor, goal, visited, graph, path)){
          return true;
        }
        path.removeLast();
      }
    }

    return false;
  }


}

