package UI;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.List;
import AI.GeminiPacketGenerator.NetworkPacket;

public class SimulationResultsWindow extends JFrame {
    private LogViewerPanel logViewer;
    private NAMViewerPanel namViewer;
    private JPanel packetDataPanel;
    private JTable packetTable;
    private DefaultTableModel packetTableModel;
    private JLabel packetStatsLabel;
    private JTabbedPane tabbedPane;
    private List<NetworkPacket> currentPackets;
    private boolean isGeneratingDataset = false;
    private int packetDataTabIndex = -1;
    private JPanel generatingBanner;

    public SimulationResultsWindow() {
        super("Simulation Results");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1200, 700);
        setLayout(new BorderLayout());

        tabbedPane = new JTabbedPane();

        tabbedPane.addChangeListener(e -> {
            if (isGeneratingDataset && tabbedPane.getSelectedIndex() == packetDataTabIndex) {
                SwingUtilities.invokeLater(() -> {
                    if (packetDataTabIndex > 0) {
                        tabbedPane.setSelectedIndex(0);
                    }
                    JOptionPane.showMessageDialog(this,
                            "🤖 AI dataset is still generating...\n\nPlease check back in a moment!",
                            "Dataset Generation In Progress",
                            JOptionPane.INFORMATION_MESSAGE);
                });
            }
        });

        logViewer = new LogViewerPanel();
        tabbedPane.addTab("📋 Network Logs", logViewer);

        namViewer = new NAMViewerPanel();
        tabbedPane.addTab("🎬 NAM Animation", namViewer);

        packetDataPanel = createPacketDataPanel();

        generatingBanner = createGeneratingBanner();

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(generatingBanner, BorderLayout.NORTH);
        mainPanel.add(tabbedPane, BorderLayout.CENTER);

        add(mainPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton exportLogsButton = new JButton("Export Logs...");
        exportLogsButton.addActionListener(e -> exportLogs());

        JButton exportPacketsButton = new JButton("💾 Export Packet Data...");
        exportPacketsButton.addActionListener(e -> exportPacketData());
        exportPacketsButton.setEnabled(false);
        exportPacketsButton.setName("exportPacketsButton");

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dispose());

        buttonPanel.add(exportLogsButton);
        buttonPanel.add(exportPacketsButton);
        buttonPanel.add(closeButton);
        add(buttonPanel, BorderLayout.SOUTH);

        setLocationRelativeTo(null);
    }

    private JPanel createGeneratingBanner() {
        JPanel banner = new JPanel(new BorderLayout());
        banner.setBackground(new Color(255, 243, 205));
        banner.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 2, 0, new Color(255, 193, 7)),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)));

        JLabel messageLabel = new JLabel(" Generating AI-powered packet dataset... Please wait.");
        messageLabel.setFont(new Font("Arial", Font.BOLD, 13));
        messageLabel.setForeground(new Color(102, 60, 0));

        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setPreferredSize(new Dimension(200, 20));

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        leftPanel.setOpaque(false);
        leftPanel.add(messageLabel);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rightPanel.setOpaque(false);
        rightPanel.add(progressBar);

        banner.add(leftPanel, BorderLayout.WEST);
        banner.add(rightPanel, BorderLayout.EAST);
        banner.setVisible(false);

        return banner;
    }

    public void showGeneratingDatasetPlaceholder() {
        generatingBanner.setVisible(true);

        if (tabbedPane.indexOfTab("📊 Packet Data") == -1) {
            tabbedPane.addTab("📊 Packet Data", packetDataPanel);
            packetDataTabIndex = tabbedPane.getTabCount() - 1;
        }

        isGeneratingDataset = true;

        packetStatsLabel.setText(
                "<html><b>Generating AI-powered packet dataset...</b><br>Please wait while Gemini API creates realistic network packets.</html>");
        packetTableModel.setRowCount(0);

        // Add a placeholder row
        packetTableModel.addRow(new Object[] {
                "...", "Generating...", "...", "...", "...", "...",
                "...", "...", "...", "...", "...", "...", "..."
        });
    }

    private JPanel createPacketDataPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel statsPanel = new JPanel(new BorderLayout());
        statsPanel.setBorder(BorderFactory.createTitledBorder("📊 Dataset Statistics"));
        packetStatsLabel = new JLabel("No packet data available");
        packetStatsLabel.setFont(new Font("Monospaced", Font.PLAIN, 11));
        packetStatsLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        statsPanel.add(packetStatsLabel, BorderLayout.CENTER);
        panel.add(statsPanel, BorderLayout.NORTH);

        String[] columns = {
                "ID", "Time", "Src IP", "Dst IP", "Src Port", "Dst Port",
                "Protocol", "Size", "Type", "Attack", "Payload", "Latency", "Rate", "App"
        };

        packetTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        packetTable = new JTable(packetTableModel);
        packetTable.setFont(new Font("Monospaced", Font.PLAIN, 10));
        packetTable.setRowHeight(20);
        packetTable.setAutoCreateRowSorter(true);

        packetTable.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                if (!isSelected) {
                    String type = (String) table.getValueAt(row, 8); // Type column (was 8, still 8)
                    if ("Attack".equals(type)) {
                        c.setBackground(new Color(255, 230, 230)); // Light red
                    } else {
                        c.setBackground(new Color(230, 255, 230)); // Light green
                    }
                }

                return c;
            }
        });

        JScrollPane scrollPane = new JScrollPane(packetTable);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Generated Packets"));
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton showAll = new JButton("Show All");
        showAll.addActionListener(e -> filterPackets(null));

        JButton showNormal = new JButton("Normal Only");
        showNormal.addActionListener(e -> filterPackets("Normal"));

        JButton showAttacks = new JButton("Attacks Only");
        showAttacks.addActionListener(e -> filterPackets("Attack"));

        filterPanel.add(new JLabel("Filter: "));
        filterPanel.add(showAll);
        filterPanel.add(showNormal);
        filterPanel.add(showAttacks);
        panel.add(filterPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void filterPackets(String type) {
        javax.swing.table.TableRowSorter<DefaultTableModel> sorter = new javax.swing.table.TableRowSorter<>(
                packetTableModel);
        packetTable.setRowSorter(sorter);

        if (type != null) {
            sorter.setRowFilter(javax.swing.RowFilter.regexFilter(type, 8));
        } else {
            sorter.setRowFilter(null);
        }
    }

    public void setLogs(String logs) {
        logViewer.setLogs(logs);
    }

    public void setNamFile(File namFile) {
        namViewer.loadNamFile(namFile);
    }

    public void showResults(String logs, File namFile) {
        setLogs(logs);
        setNamFile(namFile);

        revalidate();
        repaint();

        setVisible(true);
        toFront();

        System.out.println("DEBUG: showResults completed. Window visible=" + isVisible());
        System.out.println("DEBUG: TabbedPane tab count=" + tabbedPane.getTabCount());
        System.out.println("DEBUG: LogViewer text length=" + logViewer.getLogs().length());
    }

    public void setPacketData(List<NetworkPacket> packets) {
        this.currentPackets = packets;

        isGeneratingDataset = false;
        generatingBanner.setVisible(false);

        if (packets != null && !packets.isEmpty()) {
            boolean tabExists = false;
            for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                if (tabbedPane.getTitleAt(i).contains("Packet Data")) {
                    tabExists = true;
                    break;
                }
            }

            if (!tabExists) {
                tabbedPane.addTab("📊 Packet Data", packetDataPanel);
            }

            for (Component c : ((JPanel) getContentPane().getComponent(1)).getComponents()) {
                if (c instanceof JButton && "exportPacketsButton".equals(c.getName())) {
                    c.setEnabled(true);
                }
            }

            long normalCount = packets.stream().filter(p -> "Normal".equals(p.trafficType)).count();
            long attackCount = packets.stream().filter(p -> "Attack".equals(p.trafficType)).count();
            long tcpCount = packets.stream().filter(p -> "TCP".equals(p.protocol)).count();
            long udpCount = packets.stream().filter(p -> "UDP".equals(p.protocol)).count();
            long icmpCount = packets.stream().filter(p -> "ICMP".equals(p.protocol)).count();

            double avgPacketSize = packets.stream().mapToInt(p -> p.packetSize).average().orElse(0);
            double avgLatency = packets.stream().mapToDouble(p -> p.latency).average().orElse(0);

            String stats = String.format(
                    "<html><body style='padding:5px'>" +
                            "<b>Total:</b> %d packets  |  " +
                            "<b>Normal:</b> %d (%.1f%%)  |  " +
                            "<b>Attack:</b> %d (%.1f%%)  |  " +
                            "<b>Avg Size:</b> %.0f bytes  |  " +
                            "<b>Avg Latency:</b> %.3f ms<br>" +
                            "<b>Protocols:</b> TCP=%d (%.1f%%)  UDP=%d (%.1f%%)  ICMP=%d (%.1f%%)" +
                            "</body></html>",
                    packets.size(),
                    normalCount, (normalCount * 100.0 / packets.size()),
                    attackCount, (attackCount * 100.0 / packets.size()),
                    avgPacketSize,
                    avgLatency * 1000,
                    tcpCount, (tcpCount * 100.0 / packets.size()),
                    udpCount, (udpCount * 100.0 / packets.size()),
                    icmpCount, (icmpCount * 100.0 / packets.size()));
            packetStatsLabel.setText(stats);

            // Optimized batch table population - MUCH faster for large datasets!
            // Disable auto-sorting during bulk update
            packetTable.setAutoCreateRowSorter(false);
            
            // Build all rows in a Vector for setDataVector (fastest method)
            java.util.Vector<java.util.Vector<Object>> dataVector = new java.util.Vector<>();
            double baseTimestamp = packets.get(0).timestamp;
            
            for (NetworkPacket pkt : packets) {
                java.util.Vector<Object> row = new java.util.Vector<>();
                row.add(pkt.packetId);
                row.add(String.format("%.3f", pkt.timestamp - baseTimestamp));
                row.add(pkt.sourceIP);
                row.add(pkt.destIP);
                row.add(pkt.sourcePort);
                row.add(pkt.destPort);
                row.add(pkt.protocol);
                row.add(pkt.packetSize);
                row.add(pkt.trafficType);
                row.add(pkt.attackType != null ? pkt.attackType : "-");
                row.add(pkt.payload != null ? pkt.payload : "");
                row.add(String.format("%.2f ms", pkt.latency * 1000));
                row.add(String.format("%.1f pps", pkt.packetRate));
                row.add(pkt.applicationType);
                dataVector.add(row);
            }
            
            // Column names
            java.util.Vector<String> columnNames = new java.util.Vector<>();
            columnNames.add("Packet ID");
            columnNames.add("Time (s)");
            columnNames.add("Source IP");
            columnNames.add("Dest IP");
            columnNames.add("Src Port");
            columnNames.add("Dst Port");
            columnNames.add("Protocol");
            columnNames.add("Size");
            columnNames.add("Type");
            columnNames.add("Attack");
            columnNames.add("Payload");
            columnNames.add("Latency");
            columnNames.add("Rate");
            columnNames.add("App");
            
            // Update table in ONE operation (triggers single UI update)
            packetTableModel.setDataVector(dataVector, columnNames);
            
            // Re-enable sorting after data is loaded
            packetTable.setAutoCreateRowSorter(true);

            tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);

            revalidate();
            repaint();

            System.out.println("DEBUG: Packet data tab added and selected");

            correlateLogsWithPackets(packets);
        }
    }

    private void correlateLogsWithPackets(List<NetworkPacket> packets) {
        StringBuilder correlation = new StringBuilder();
        correlation.append("\n\n");
        correlation.append("═══════════════════════════════════════════════════════════════\n");
        correlation.append("PACKET DATASET CORRELATION\n");
        correlation.append("═══════════════════════════════════════════════════════════════\n\n");
        correlation.append("Generated ").append(packets.size())
                .append(" AI-powered network packets based on this topology:\n\n");

        java.util.Map<String, List<NetworkPacket>> packetsBySource = packets.stream()
                .collect(java.util.stream.Collectors.groupingBy(p -> p.sourceIP));

        correlation.append("Traffic Summary by Node:\n");
        correlation.append("─────────────────────────────────────────────────────────────\n");

        packetsBySource.forEach((srcIP, srcPackets) -> {
            long attacks = srcPackets.stream().filter(p -> "Attack".equals(p.trafficType)).count();
            correlation.append(String.format("  %s: %d packets sent (%d attacks)\n",
                    srcIP, srcPackets.size(), attacks));
        });

        correlation.append("\nTop 10 Packet Flow Samples:\n");
        correlation.append("─────────────────────────────────────────────────────────────\n");

        for (int i = 0; i < Math.min(10, packets.size()); i++) {
            NetworkPacket p = packets.get(i);
            correlation.append(String.format("  [Packet %s] %.3fs: %s:%d → %s:%d (%s, %db, %s)\n",
                    p.packetId, p.timestamp, p.sourceIP, p.sourcePort, p.destIP, p.destPort,
                    p.protocol, p.packetSize, p.attackType != null ? p.attackType : "Normal"));
        }

        if (packets.size() > 10) {
            correlation.append(String.format("  ... and %d more packets (see Packet Data tab)\n", packets.size() - 10));
        }

        correlation.append("\n");
        correlation.append("═══════════════════════════════════════════════════════════════\n");
        correlation.append(" TIP: Switch to 'Packet Data' tab for full dataset analysis\n");
        correlation.append("═══════════════════════════════════════════════════════════════\n");

        logViewer.appendLog(correlation.toString());
    }

    private void exportPacketData() {
        if (currentPackets == null || currentPackets.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "No packet data to export",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Export Packet Data to CSV");
        fc.setSelectedFile(new File("packet_data_" + System.currentTimeMillis() + ".csv"));

        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File outputFile = fc.getSelectedFile();

        try (PrintWriter writer = new PrintWriter(new FileWriter(outputFile))) {
            writer.println(AI.GeminiPacketGenerator.NetworkPacket.getCSVHeader());

            for (NetworkPacket packet : currentPackets) {
                writer.println(packet.toCSV());
            }

            JOptionPane.showMessageDialog(this,
                    String.format("✅ Successfully exported %d packets to:\n%s\n\nFile size: %.2f KB",
                            currentPackets.size(),
                            outputFile.getAbsolutePath(),
                            outputFile.length() / 1024.0),
                    "Export Complete",
                    JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Export failed: " + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportLogs() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Export Logs");
        fc.setSelectedFile(new File("simulation_logs.txt"));

        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (java.io.PrintWriter writer = new java.io.PrintWriter(fc.getSelectedFile())) {
                writer.print(logViewer.getLogs());
                JOptionPane.showMessageDialog(this,
                        "Logs exported successfully!",
                        "Export Complete",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                        "Failed to export logs: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
