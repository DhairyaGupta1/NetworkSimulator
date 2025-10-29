package UI;

import javax.swing.*;
import java.awt.*;

public class SimulationConfigDialog extends JDialog {
    private JTextField simTimeField;
    private JTextField bandwidthField;
    private JTextField delayField;
    private JComboBox<String> protocolCombo;
    private JComboBox<String> queueTypeCombo;
    private JComboBox<String> applicationCombo;
    private JTextField packetSizeField;
    private JTextField dataRateField;
    private JCheckBox enableTracingCheck;
    private JCheckBox enableNamCheck;
    private JCheckBox enableDatasetCheck;
    private JTextField datasetPacketCountField;
    private JComboBox<String> datasetScenarioCombo;
    private boolean confirmed = false;

    public SimulationConfigDialog(JFrame parent) {
        super(parent, "Simulation Configuration", true);
        setLayout(new BorderLayout(10, 10));

        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;

        gbc.gridx = 0;
        gbc.gridy = row;
        mainPanel.add(new JLabel("Simulation Time (sec):"), gbc);
        gbc.gridx = 1;
        simTimeField = new JTextField("10.0", 15);
        mainPanel.add(simTimeField, gbc);
        row++;

        gbc.gridx = 0;
        gbc.gridy = row;
        mainPanel.add(new JLabel("Link Bandwidth:"), gbc);
        gbc.gridx = 1;
        bandwidthField = new JTextField("100.0Mb", 15);
        mainPanel.add(bandwidthField, gbc);
        row++;

        gbc.gridx = 0;
        gbc.gridy = row;
        mainPanel.add(new JLabel("Link Delay:"), gbc);
        gbc.gridx = 1;
        delayField = new JTextField("10ms", 15);
        mainPanel.add(delayField, gbc);
        row++;

        gbc.gridx = 0;
        gbc.gridy = row;
        mainPanel.add(new JLabel("Transport Protocol:"), gbc);
        gbc.gridx = 1;
        protocolCombo = new JComboBox<>(new String[] { "TCP", "UDP", "TCP/Reno", "TCP/Newreno", "TCP/Vegas" });
        mainPanel.add(protocolCombo, gbc);
        row++;

        gbc.gridx = 0;
        gbc.gridy = row;
        mainPanel.add(new JLabel("Queue Type:"), gbc);
        gbc.gridx = 1;
        queueTypeCombo = new JComboBox<>(new String[] { "DropTail", "RED", "FQ", "SFQ" });
        mainPanel.add(queueTypeCombo, gbc);
        row++;

        gbc.gridx = 0;
        gbc.gridy = row;
        mainPanel.add(new JLabel("Application:"), gbc);
        gbc.gridx = 1;
        applicationCombo = new JComboBox<>(new String[] { "FTP", "CBR", "Telnet", "Exponential" });
        mainPanel.add(applicationCombo, gbc);
        row++;

        gbc.gridx = 0;
        gbc.gridy = row;
        mainPanel.add(new JLabel("Packet Size (bytes):"), gbc);
        gbc.gridx = 1;
        packetSizeField = new JTextField("1000", 15);
        mainPanel.add(packetSizeField, gbc);
        row++;

        gbc.gridx = 0;
        gbc.gridy = row;
        mainPanel.add(new JLabel("Data Rate (for CBR):"), gbc);
        gbc.gridx = 1;
        dataRateField = new JTextField("1Mb", 15);
        mainPanel.add(dataRateField, gbc);
        row++;

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        enableTracingCheck = new JCheckBox("Enable Trace File (.tr)", true);
        mainPanel.add(enableTracingCheck, gbc);
        gbc.gridwidth = 1;
        row++;

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        enableNamCheck = new JCheckBox("Enable NAM Animation (.nam)", true);
        mainPanel.add(enableNamCheck, gbc);
        row++;

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        JLabel datasetLabel = new JLabel("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        mainPanel.add(datasetLabel, gbc);
        row++;

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        enableDatasetCheck = new JCheckBox("📊 Generate Network Packet Dataset (Optional)", false);
        enableDatasetCheck.setFont(enableDatasetCheck.getFont().deriveFont(Font.BOLD));
        enableDatasetCheck.addActionListener(e -> {
            boolean enabled = enableDatasetCheck.isSelected();
            datasetPacketCountField.setEnabled(enabled);
            datasetScenarioCombo.setEnabled(enabled);
        });
        mainPanel.add(enableDatasetCheck, gbc);
        gbc.gridwidth = 1;
        row++;

        gbc.gridx = 0;
        gbc.gridy = row;
        mainPanel.add(new JLabel("  Packet Count:"), gbc);
        gbc.gridx = 1;
        datasetPacketCountField = new JTextField("200", 15);
        datasetPacketCountField.setEnabled(false);
        mainPanel.add(datasetPacketCountField, gbc);
        row++;

        gbc.gridx = 0;
        gbc.gridy = row;
        mainPanel.add(new JLabel("  Traffic Scenario:"), gbc);
        gbc.gridx = 1;
        datasetScenarioCombo = new JComboBox<>(new String[] {
                "Normal Traffic", "DDoS Attack", "Port Scan",
                "Mixed Traffic", "Web Attack", "Malware Communication"
        });
        datasetScenarioCombo.setSelectedIndex(3); // Default to Mixed Traffic
        datasetScenarioCombo.setEnabled(false);
        mainPanel.add(datasetScenarioCombo, gbc);

        add(mainPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okButton = new JButton("Run Simulation");
        JButton cancelButton = new JButton("Cancel");

        okButton.addActionListener(e -> {
            confirmed = true;
            dispose();
        });

        cancelButton.addActionListener(e -> {
            confirmed = false;
            dispose();
        });

        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(parent);
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public SimulationConfig getConfig() {
        SimulationConfig config = new SimulationConfig();
        config.simTime = Double.parseDouble(simTimeField.getText().trim());
        config.bandwidth = bandwidthField.getText().trim();
        config.delay = delayField.getText().trim();
        config.protocol = (String) protocolCombo.getSelectedItem();
        config.queueType = (String) queueTypeCombo.getSelectedItem();
        config.application = (String) applicationCombo.getSelectedItem();
        config.packetSize = Integer.parseInt(packetSizeField.getText().trim());
        config.dataRate = dataRateField.getText().trim();
        config.enableTracing = enableTracingCheck.isSelected();
        config.enableNam = enableNamCheck.isSelected();
        config.enableDataset = enableDatasetCheck.isSelected();
        if (config.enableDataset) {
            config.datasetPacketCount = Integer.parseInt(datasetPacketCountField.getText().trim());
            config.datasetScenario = (String) datasetScenarioCombo.getSelectedItem();
        }
        return config;
    }

    public static class SimulationConfig {
        public double simTime;
        public String bandwidth;
        public String delay;
        public String protocol;
        public String queueType;
        public String application;
        public int packetSize;
        public String dataRate;
        public boolean enableTracing;
        public boolean enableNam;
        public boolean enableDataset;
        public int datasetPacketCount;
        public String datasetScenario;
    }
}
