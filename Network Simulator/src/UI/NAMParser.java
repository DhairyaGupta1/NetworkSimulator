package UI;

import java.io.*;
import java.util.*;

/**
 * Parser for NAM (Network Animator) files.
 * Extracts nodes, links, and packet events from NAM trace format.
 */
public class NAMParser {

    public static class NAMNode {
        public int id;
        public double x, y;
        public String label;
        public String shape = "circle";
        public String color = "black";

        public NAMNode(int id, double x, double y) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.label = "n" + id;
        }
    }

    public static class NAMLink {
        public int srcNode;
        public int dstNode;
        public String orientation = "right";
        public String color = "black";

        public NAMLink(int srcNode, int dstNode) {
            this.srcNode = srcNode;
            this.dstNode = dstNode;
        }
    }

    public static class NAMEvent {
        public double time;
        public String type; // "+" (enqueue), "-" (dequeue), "r" (receive), "d" (drop), "h" (hop)
        public int srcNode;
        public int dstNode;
        public String packetType;
        public int size;

        public NAMEvent(double time, String type) {
            this.time = time;
            this.type = type;
        }
    }

    public static class NAMData {
        public Map<Integer, NAMNode> nodes = new HashMap<>();
        public List<NAMLink> links = new ArrayList<>();
        public List<NAMEvent> events = new ArrayList<>();
        public double maxTime = 0;
        public double minX = Double.MAX_VALUE, maxX = Double.MIN_VALUE;
        public double minY = Double.MAX_VALUE, maxY = Double.MIN_VALUE;
    }

    public static NAMData parse(File namFile) throws IOException {
        NAMData data = new NAMData();

        try (BufferedReader reader = new BufferedReader(new FileReader(namFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#"))
                    continue;

                String[] tokens = line.split("\\s+");
                if (tokens.length == 0)
                    continue;

                String cmd = tokens[0];

                try {
                    if (cmd.equals("n")) {
                        // Node: n -t time -s node_id -x x -y y -Z size -z color
                        int nodeId = -1;
                        double x = 0, y = 0;

                        for (int i = 1; i < tokens.length - 1; i++) {
                            if (tokens[i].equals("-s")) {
                                nodeId = Integer.parseInt(tokens[i + 1]);
                            } else if (tokens[i].equals("-x")) {
                                x = Double.parseDouble(tokens[i + 1]);
                            } else if (tokens[i].equals("-y")) {
                                y = Double.parseDouble(tokens[i + 1]);
                            }
                        }

                        if (nodeId >= 0) {
                            NAMNode node = new NAMNode(nodeId, x, y);
                            data.nodes.put(nodeId, node);

                            data.minX = Math.min(data.minX, x);
                            data.maxX = Math.max(data.maxX, x);
                            data.minY = Math.min(data.minY, y);
                            data.maxY = Math.max(data.maxY, y);
                        }

                    } else if (cmd.equals("l")) {
                        // Link: l -t time -s src -d dst -S state -r rate -D delay
                        int src = -1, dst = -1;

                        for (int i = 1; i < tokens.length - 1; i++) {
                            if (tokens[i].equals("-s")) {
                                src = Integer.parseInt(tokens[i + 1]);
                            } else if (tokens[i].equals("-d")) {
                                dst = Integer.parseInt(tokens[i + 1]);
                            }
                        }

                        if (src >= 0 && dst >= 0) {
                            data.links.add(new NAMLink(src, dst));
                        }

                    } else if (cmd.equals("+") || cmd.equals("-") || cmd.equals("r") ||
                            cmd.equals("d") || cmd.equals("h")) {
                        // Event: + -t time -s src -d dst -p pkttype -e size -a attr
                        double time = 0;
                        int src = -1, dst = -1;
                        String pktType = "tcp";
                        int size = 0;

                        for (int i = 1; i < tokens.length - 1; i++) {
                            if (tokens[i].equals("-t")) {
                                time = Double.parseDouble(tokens[i + 1]);
                            } else if (tokens[i].equals("-s")) {
                                src = Integer.parseInt(tokens[i + 1]);
                            } else if (tokens[i].equals("-d")) {
                                dst = Integer.parseInt(tokens[i + 1]);
                            } else if (tokens[i].equals("-p")) {
                                pktType = tokens[i + 1];
                            } else if (tokens[i].equals("-e")) {
                                size = Integer.parseInt(tokens[i + 1]);
                            }
                        }

                        if (src >= 0 && dst >= 0) {
                            NAMEvent event = new NAMEvent(time, cmd);
                            event.srcNode = src;
                            event.dstNode = dst;
                            event.packetType = pktType;
                            event.size = size;
                            data.events.add(event);
                            data.maxTime = Math.max(data.maxTime, time);
                        }
                    }
                } catch (Exception e) {
                    // Skip malformed lines
                }
            }
        }

        // Sort events by time - use stable sort with explicit comparator
        data.events.sort((e1, e2) -> {
            int timeCompare = Double.compare(e1.time, e2.time);
            if (timeCompare != 0)
                return timeCompare;
            // If times are equal, maintain insertion order by comparing hash codes
            return Integer.compare(System.identityHashCode(e1), System.identityHashCode(e2));
        });

        return data;
    }
}
