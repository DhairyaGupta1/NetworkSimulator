package Components;

import java.util.Set;

public class Node extends NetworkComponent {
    public static long serialVersionUID = 1l;
    private static final java.util.PriorityQueue<Long> freeIds = new java.util.PriorityQueue<>();
    public long id;
    public Set<Node> neighbors;
    public int x;
    public int y;

    public Node(int x, int y) {
        super(1);
        this.id = nextId();
        this.x = x;
        this.y = y;
    }

    public Node(int x, int y, long id) {
        super(1);
        this.id = id;
        this.x = x;
        this.y = y;
        if (serialVersionUID <= id)
            serialVersionUID = id + 1;
    }

    public static synchronized long nextId() {
        Long v = freeIds.poll();
        if (v != null)
            return v;
        return serialVersionUID++;
    }

    public static synchronized void releaseId(long id) {
        if (id <= 0)
            return;
        freeIds.add(id);
    }

    public static synchronized boolean reserveId(long id) {
        if (id <= 0)
            return false;
        // If id is available in freeIds, remove it and reserve
        if (freeIds.remove(id))
            return true;
        if (serialVersionUID <= id) {
            serialVersionUID = id + 1;
            return true;
        }
        return false;
    }
}