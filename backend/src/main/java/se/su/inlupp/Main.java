package se.su.inlupp;

public class Main {

    public static void main(String[] args) {

        ListGraph<String> graph = new ListGraph<>();

        // 🔹 Add nodes
        graph.add("A");
        graph.add("B");
        graph.add("C");

        System.out.println("Nodes added.");

        // 🔹 Connect nodes
        graph.connect("A", "B", "AB", 5);
        graph.connect("A", "C", "AC", 3);

        System.out.println("Connections created.");

        //🔹 Connect nodes wrong
        try {
            graph.connect("A", "B", "AB", 5);
        }catch (Exception e){
            System.out.println("Error trying to connect :" + e);
        }

        try {
            graph.connect("B", "C", "BC", -1);
        }catch (Exception e){
            System.out.println("Error trying to connect :" + e);
        }


        // 🔹 Check connections
        System.out.println("Edge A -> B: " + graph.getEdgeBetween("A", "B"));
        System.out.println("Edge B -> A: " + graph.getEdgeBetween("B", "A"));
        System.out.println("Edge A -> C: " + graph.getEdgeBetween("A", "C"));

//        // 🔹 Remove node
//        graph.remove("A");
//        System.out.println("Removed node A.");
//
//        // 🔹 Check if edges are removed properly
//        try {
//            System.out.println("Edge B -> A after removal: " + graph.getEdgeBetween("B", "A"));
//        } catch (Exception e) {
//            System.out.println("Error after removal (expected if node gone): " + e);
//        }
        //🔹 disconnect

//        graph.disconnect("A","B");
//
//        System.out.println("Disconnected A and B");


        // 🔹 Set weight

        graph.setConnectionWeight("A", "B", 2);

        System.out.println("Weigth between A and be changed from 5 to 2");

        System.out.println("Edge A -> B: " + graph.getEdgeBetween("A", "B"));
        System.out.println("Edge B -> A: " + graph.getEdgeBetween("B", "A"));
        System.out.println("Edge A -> C: " + graph.getEdgeBetween("A", "C"));

//        // 🔹 GetEdgeFrom()
//        System.out.println(graph.getEdgesFrom("A"));
//
//
//
//        // 🔹 Check remaining structure
//        System.out.println("Node B still exists: " + graph.hasNode("B"));
//        System.out.println("Node C still exists: " + graph.hasNode("C"));

       System.out.println(graph);
        System.out.println("Test complete.");
    }


}
