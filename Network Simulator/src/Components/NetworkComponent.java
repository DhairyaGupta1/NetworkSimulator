package Components;
//base class extended by all network devices and links
public class NetworkComponent {
    public static final int NODE = 1;   //type 1 means that the component is a node device
    public static final int LINK = 2;   //type 2 means that the component is a link between nodes
    public static int type;
    public int x;
    public int y;           //type of current network component

    public NetworkComponent(int type){
        this.type = type;
    }

}
