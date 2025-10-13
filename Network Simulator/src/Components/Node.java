package Components;
//A Graph Node class used to create and model networks
import java.util.ArrayList;
import java.util.Set;

public class Node extends NetworkComponent{
    public static long serialVersionUID = 1l;  //UID common to all nodes, this ensures that all nodes have a distinct UID
    public final long id;                     //UID of current node
    public Set<Node> neighbors;         //set of current nodes neighbors
    public int x;
    public int y;                       //type of current network component

    public Node(int x, int y){
        super(1);
        this.id = serialVersionUID++;
        this.x = x;
        this.y = y;
    }
}