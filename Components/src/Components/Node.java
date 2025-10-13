package Components;
//A Graph Node class used to create and model networks
import java.util.ArrayList;

public class Node extends NetworkComponent{
    static long serialVersionUID = 1l; //UID common to all nodes, this ensures that all nodes have a distinct UID
    public long id;     //UID of current node
    public ArrayList neighbors; //list of current nodes neighbors

    public Node(int x, int y){
        super(1);
        this.id = serialVersionUID++;
        this.x = x;
        this.y = y;
    }
}