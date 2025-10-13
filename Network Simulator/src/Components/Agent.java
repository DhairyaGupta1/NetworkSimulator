package Components;

//Agent is the network component that determines what protocol a Node uses to send packets
public class Agent extends  NetworkComponent{
    public static long serialVersionUID = 1l;
    public String type;
    public long id;

    //converts int to corresponding agent type
    public void convertToAgent(int choice){
        switch(choice){
            case 1:
                type = "TCP";
                break;
            case 2:
                type = "UDP";
                break;
            default:
                System.out.println("Error, The choice does not correspond to any agent!");
        }
    }
    public Agent(){
        super(3);
        this.id = serialVersionUID++;
    }
}
