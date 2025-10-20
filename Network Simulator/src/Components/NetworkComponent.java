package Components;

public class NetworkComponent {
    public static final int NODE = 1;
    public static final int LINK = 2;
    public static final int AGENT = 3;

    public static int type;

    public NetworkComponent(int type) {
        NetworkComponent.type = type;
    }

}
