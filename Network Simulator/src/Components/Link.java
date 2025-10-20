package Components;

import java.util.HashSet;

public class Link extends NetworkComponent {
    public static long serialVersionUID = 1l;
    public Node node1;
    public Node node2;
    public final long id;

    public Link(Node node1, Node node2) {
        super(2);
        this.id = serialVersionUID++;
        this.node1 = node1;
        if (node1.neighbors == null)
            node1.neighbors = new HashSet<Node>();
        node1.neighbors.add(node2);

        if (node2.neighbors == null)
            node2.neighbors = new HashSet<Node>();
        node2.neighbors.add(node1);
        this.node2 = node2;
    }
}
