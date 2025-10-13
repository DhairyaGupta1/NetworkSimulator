package Components;

import java.util.ArrayList;
import java.util.HashSet;

//denotes a link between two nodes
public class Link extends NetworkComponent{
    static long serialVersionUID = 1l;          //UID for Links
    public Node node1;                          //node connected to one end of the link
    public Node node2;                          //node connected to opposite end of the link
    public final long id;

    public Link(Node node1, Node node2){
        super(2);
        this.id = serialVersionUID++;
        this.node1 = node1;
        if(node1.neighbors == null) node1.neighbors = new HashSet<Node>();  //if node1 has no neighbors, create a set of neighbors;
        node1.neighbors.add(node2); //add node2 as a neighbor of node1

        //same process repeated for node2
        if(node2.neighbors == null) node2.neighbors = new HashSet<Node>();
        node2.neighbors.add(node1);
        this.node2 = node2;
    }
}
