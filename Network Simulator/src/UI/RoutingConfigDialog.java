package UI;

import Components.Node;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Dialog for configuring packet flows between nodes
 * Allows users to define multiple traffic flows with source, destination, and protocol
 */
public class RoutingConfigDialog extends JDialog {
    private JTable flowTable;
    private DefaultTableModel tableModel;
    private JComboBox<String> srcNodeCombo;
    private JComboBox<String> dstNodeCombo;
    private JComboBox<String> flowTypeCombo;
    private JTextField startTimeField;
    private JTextField stopTimeField;
    private List<TrafficFlow> flows;
    private Collection<Node> nodes;
    private boolean confirmed = false;

    public static class TrafficFlow {
        public long srcNodeId;
        public long dstNodeId;
        public String flowType; // "TCP", "UDP", "CBR", "FTP", etc.
        public double startTime;
        public double stopTime;

        public TrafficFlow(long src, long dst, String type, double start, double stop) {
            this.srcNodeId = src;
            this.dstNodeId = dst;
            this.flowType = type;
            this.startTime = start;
            this.stopTime = stop;
        }

        @Override
        public String toString() {
            return String.format("N%d → N%d (%s) [%.1fs - %.1fs]", 
                srcNodeId, dstNodeId, flowType, startTime, stopTime);
        }
    }

    public RoutingConfigDialog(JFrame parent, Collection<Node> nodes) {
        super(parent, "Configure Packet Flows", true);
        this.nodes = nodes;
        this.flows = new ArrayList<>();
        
        setLayout(new BorderLayout(10, 10));
        setSize(700, 500);

        // Top panel - Add flow controls
        JPanel topPanel = new JPanel(new GridBagLayout());
        topPanel.setBorder(BorderFactory.createTitledBorder("Add New Flow"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Source node
        gbc.gridx = 0;
        gbc.gridy = 0;
        topPanel.add(new JLabel("Source Node:"), gbc);
        gbc.gridx = 1;
        srcNodeCombo = new JComboBox<>();
        for (Node n : nodes) {
            srcNodeCombo.addItem("Node " + n.id);
        }
        topPanel.add(srcNodeCombo, gbc);

        // Destination node
        gbc.gridx = 2;
        topPanel.add(new JLabel("Destination:"), gbc);
        gbc.gridx = 3;
        dstNodeCombo = new JComboBox<>();
        for (Node n : nodes) {
            dstNodeCombo.addItem("Node " + n.id);
        }
        topPanel.add(dstNodeCombo, gbc);

        // Flow type
        gbc.gridx = 0;
        gbc.gridy = 1;
        topPanel.add(new JLabel("Flow Type:"), gbc);
        gbc.gridx = 1;
        flowTypeCombo = new JComboBox<>(new String[]{
            "TCP/FTP", "TCP/Telnet", "UDP/CBR", "UDP/Exponential"
        });
        topPanel.add(flowTypeCombo, gbc);

        // Start time
        gbc.gridx = 2;
        topPanel.add(new JLabel("Start Time (s):"), gbc);
        gbc.gridx = 3;
        startTimeField = new JTextField("0.5", 10);
        topPanel.add(startTimeField, gbc);

        // Stop time
        gbc.gridx = 0;
        gbc.gridy = 2;
        topPanel.add(new JLabel("Stop Time (s):"), gbc);
        gbc.gridx = 1;
        stopTimeField = new JTextField("9.5", 10);
        topPanel.add(stopTimeField, gbc);

        // Add button
        gbc.gridx = 3;
        gbc.gridy = 2;
        JButton addButton = new JButton("Add Flow →");
        addButton.addActionListener(e -> addFlow());
        topPanel.add(addButton, gbc);

        add(topPanel, BorderLayout.NORTH);

        // Center panel - Flow table
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBorder(BorderFactory.createTitledBorder("Configured Flows"));

        String[] columns = {"Source", "Destination", "Type", "Start (s)", "Stop (s)"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        flowTable = new JTable(tableModel);
        flowTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        flowTable.getTableHeader().setReorderingAllowed(false);

        JScrollPane scrollPane = new JScrollPane(flowTable);
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        JPanel tableButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton removeButton = new JButton("Remove Selected");
        removeButton.addActionListener(e -> removeSelectedFlow());
        JButton clearButton = new JButton("Clear All");
        clearButton.addActionListener(e -> clearAllFlows());
        tableButtonPanel.add(removeButton);
        tableButtonPanel.add(clearButton);
        centerPanel.add(tableButtonPanel, BorderLayout.SOUTH);

        add(centerPanel, BorderLayout.CENTER);

        // Bottom panel - Action buttons
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        JButton okButton = new JButton("Apply & Continue");
        okButton.addActionListener(e -> {
            confirmed = true;
            dispose();
        });

        JButton skipButton = new JButton("Skip (Use Default)");
        skipButton.addActionListener(e -> {
            confirmed = true;
            flows.clear(); // Empty flows = use default first-to-last
            dispose();
        });

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> {
            confirmed = false;
            dispose();
        });

        bottomPanel.add(skipButton);
        bottomPanel.add(okButton);
        bottomPanel.add(cancelButton);
        add(bottomPanel, BorderLayout.SOUTH);

        // Add some default flows as examples
        addDefaultFlows();

        setLocationRelativeTo(parent);
    }

    private void addDefaultFlows() {
        List<Node> nodeList = new ArrayList<>(nodes);
        if (nodeList.size() >= 2) {
            // Add a default flow from first to last node
            flows.add(new TrafficFlow(
                nodeList.get(0).id,
                nodeList.get(nodeList.size() - 1).id,
                "TCP/FTP",
                0.5,
                9.5
            ));
            updateTable();
        }
    }

    private void addFlow() {
        try {
            int srcIdx = srcNodeCombo.getSelectedIndex();
            int dstIdx = dstNodeCombo.getSelectedIndex();
            
            if (srcIdx < 0 || dstIdx < 0) {
                JOptionPane.showMessageDialog(this, 
                    "Please select source and destination nodes", 
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (srcIdx == dstIdx) {
                JOptionPane.showMessageDialog(this, 
                    "Source and destination must be different", 
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            List<Node> nodeList = new ArrayList<>(nodes);
            long srcId = nodeList.get(srcIdx).id;
            long dstId = nodeList.get(dstIdx).id;
            String flowType = (String) flowTypeCombo.getSelectedItem();
            double startTime = Double.parseDouble(startTimeField.getText().trim());
            double stopTime = Double.parseDouble(stopTimeField.getText().trim());

            if (startTime >= stopTime) {
                JOptionPane.showMessageDialog(this, 
                    "Start time must be less than stop time", 
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            flows.add(new TrafficFlow(srcId, dstId, flowType, startTime, stopTime));
            updateTable();

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, 
                "Invalid time values. Please enter numbers.", 
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void removeSelectedFlow() {
        int selectedRow = flowTable.getSelectedRow();
        if (selectedRow >= 0 && selectedRow < flows.size()) {
            flows.remove(selectedRow);
            updateTable();
        }
    }

    private void clearAllFlows() {
        flows.clear();
        updateTable();
    }

    private void updateTable() {
        tableModel.setRowCount(0);
        for (TrafficFlow flow : flows) {
            tableModel.addRow(new Object[]{
                "Node " + flow.srcNodeId,
                "Node " + flow.dstNodeId,
                flow.flowType,
                String.format("%.1f", flow.startTime),
                String.format("%.1f", flow.stopTime)
            });
        }
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public List<TrafficFlow> getFlows() {
        return new ArrayList<>(flows);
    }
}
