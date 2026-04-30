package se.su.inlupp;

import java.util.*;

public class ListGraph<T> implements Graph<T> {

  private final Map<T, List<Edge<T>>> adjacencyList = new HashMap<>();

  private class ListEdge implements Edge<T>{

    private int weight;
    private final T destination;
    private final String name;

    public ListEdge(T destination, String name, int weight) {
      this.destination = destination;
      this.name = name;
      this.weight = weight;
    }

    @Override
    public int getWeight() {
      return weight;
    }

    @Override
    public void setWeight(int weight) {
      if (weight < 0){
        throw new IllegalArgumentException("Weight must be bigger than 0!");
      }
      this.weight = weight;
    }

    @Override
    public T getDestination() {
      return destination;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public String toString(){
      return "Edge: " + name + " Destination: " + destination + " Weight: " + weight;
    }

  }

  @Override
  public void add(T node) {
    adjacencyList.putIfAbsent(node, new ArrayList<>());
  }

  @Override
  public void remove(T node) {
    if (!hasNode(node)) {
      throw new NoSuchElementException("Node does not exist: " + node);
    }

    for (Edge<T> edge : adjacencyList.get(node)) {
      T neighbor = edge.getDestination();

      adjacencyList.get(neighbor)
              .removeIf(e -> e.getDestination().equals(node));
    }

    adjacencyList.remove(node);
  }

  @Override
  public boolean hasNode(T node) {
    return adjacencyList.containsKey(node);
  }

  @Override
  public void connect(T node1, T node2, String name, int weight) {
    if(!hasNode(node1) || !hasNode(node2)){
      throw new NoSuchElementException();
    }
    if (weight < 0){
      throw new IllegalArgumentException();
    }
    if(getEdgeBetween(node1, node2) != null){
      throw new IllegalStateException();
    }

    List<Edge<T>> node1Edges = adjacencyList.get(node1);
    List<Edge<T>> node2Edges = adjacencyList.get(node2);

    node1Edges.add(new ListEdge(node2, name, weight));
    node2Edges.add(new ListEdge(node1, name, weight));
  }

  @Override
  public void disconnect(T node1, T node2) {
    if(!hasNode(node1) || !hasNode(node2)){
      throw new NoSuchElementException();
    }
    if(getEdgeBetween(node1, node2) == null){
      throw new IllegalStateException();
    }

    adjacencyList.get(node1)
            .removeIf(e -> e.getDestination().equals(node2));
    adjacencyList.get(node2)
            .removeIf(e -> e.getDestination().equals(node1));


  }

  @Override
  public void setConnectionWeight(T node1, T node2, int weight) {
    if(!hasNode(node1) || !hasNode(node2)){
      throw new NoSuchElementException();
    }
    if(getEdgeBetween(node1, node2) == null){
      throw new NoSuchElementException();
    }
    if (weight < 0){
      throw new IllegalArgumentException();
    }

    Edge<T> node1ToNode2 = getEdgeBetween(node1,node2);
    Edge<T> node2ToNode1 = getEdgeBetween(node2,node1);

    node1ToNode2.setWeight(weight);
    node2ToNode1.setWeight(weight);
  }

  @Override
  public Set<T> getNodes() {
    return Collections.unmodifiableSet(new HashSet<>(adjacencyList.keySet()));
  }

  @Override
  public Collection<Edge<T>> getEdgesFrom(T node) {
    if(!hasNode(node)){
      throw new NoSuchElementException();
    }
    return Collections.unmodifiableCollection(new ArrayList<>(adjacencyList.get(node)));
  }

  @Override
  public Edge<T> getEdgeBetween(T node1, T node2) {
    if(!hasNode(node1) || !hasNode(node2)){
      throw new NoSuchElementException();
    }

    List<Edge<T>> edgesNode1 = adjacencyList.get(node1);
    for (Edge<T> e : edgesNode1) {
      if (e.getDestination().equals(node2)) {
        return e;
      }
    }
    return null;
  }

  @Override
  public Iterator<T> iterator() {
    return adjacencyList.keySet().iterator();
  }

  @Override
  public String toString(){
    StringBuilder sb = new StringBuilder();

    for (T node : getNodes()){
      sb.append(node);
      sb.append(":");
      for (Edge<T> e : getEdgesFrom(node)){
        sb.append("\n");
        sb.append("->");
        sb.append(e.toString());
      }
      sb.append("\n");
    }
    return sb.toString();
  }
}

