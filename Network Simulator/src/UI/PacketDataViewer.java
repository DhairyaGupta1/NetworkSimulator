package UI;

import AI.GeminiPacketGenerator.NetworkPacket;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.List;

/**
 * Window for displaying generated packet data with export functionality
 */
public class PacketDataViewer extends JFrame {

    private JTable packetTable;
    private DefaultTableModel tableModel;
    private List<NetworkPacket> packets;

    public PacketDataViewer(List<NetworkPacket> packets) {
        super("Generated Packet Data - " + packets.size() + " packets");
        this.packets = packets;

        setSize(1400, 700);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        JPanel topPanel = createStatsPanel();
        add(topPanel, BorderLayout.NORTH);

        JScrollPane tableScroll = createPacketTable();
        add(tableScroll, BorderLayout.CENTER);

        JPanel bottomPanel = createActionPanel();
        add(bottomPanel, BorderLayout.SOUTH);

        setLocationRelativeTo(null);
    }

    private JPanel createStatsPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 1, 5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("📊 Dataset Statistics"));

        long normalCount = packets.stream().filter(p -> "Normal".equals(p.trafficType)).count();
        long attackCount = packets.stream().filter(p -> "Attack".equals(p.trafficType)).count();
        long tcpCount = packets.stream().filter(p -> "TCP".equals(p.protocol)).count();
        long udpCount = packets.stream().filter(p -> "UDP".equals(p.protocol)).count();
        long icmpCount = packets.stream().filter(p -> "ICMP".equals(p.protocol)).count();

        double avgPacketSize = packets.stream().mapToInt(p -> p.packetSize).average().orElse(0);
        double avgLatency = packets.stream().mapToDouble(p -> p.latency).average().orElse(0);

        JLabel line1 = new JLabel(String.format(
                "   Total: %d packets  |  Normal: %d (%.1f%%)  |  Attack: %d (%.1f%%)  |  Avg Size: %.0f bytes  |  Avg Latency: %.3f ms",
                packets.size(),
                normalCount, (normalCount * 100.0 / packets.size()),
                attackCount, (attackCount * 100.0 / packets.size()),
                avgPacketSize,
                avgLatency * 1000));
        line1.setFont(new Font("Monospaced", Font.BOLD, 12));

        JLabel line2 = new JLabel(String.format(
                "   Protocols: TCP=%d (%.1f%%)  UDP=%d (%.1f%%)  ICMP=%d (%.1f%%)",
                tcpCount, (tcpCount * 100.0 / packets.size()),
                udpCount, (udpCount * 100.0 / packets.size()),
                icmpCount, (icmpCount * 100.0 / packets.size())));
        line2.setFont(new Font("Monospaced", Font.PLAIN, 11));

        panel.add(line1);
        panel.add(line2);

        return panel;
    }

    private JScrollPane createPacketTable() {
        String[] columns = {
                "ID", "Time", "Src IP", "Dst IP", "Src Port", "Dst Port",
                "Protocol", "Size", "Type", "Attack", "Latency", "Rate",
                "Flags", "App", "Retrans", "Error%"
        };

        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        for (NetworkPacket pkt : packets) {
            tableModel.addRow(new Object[] {
                    pkt.packetId,
                    String.format("%.3f", pkt.timestamp - packets.get(0).timestamp),
                    pkt.sourceIP,
                    pkt.destIP,
                    pkt.sourcePort,
                    pkt.destPort,
                    pkt.protocol,
                    pkt.packetSize,
                    pkt.trafficType,
                    pkt.attackType != null ? pkt.attackType : "-",
                    String.format("%.3f ms", pkt.latency * 1000),
                    String.format("%.1f pps", pkt.packetRate),
                    pkt.flags,
                    pkt.applicationType,
                    pkt.retransmissions,
                    String.format("%.2f%%", pkt.errorRate * 100)
            });
        }

        packetTable = new JTable(tableModel);
        packetTable.setFont(new Font("Monospaced", Font.PLAIN, 11));
        packetTable.setRowHeight(22);
        packetTable.setAutoCreateRowSorter(true);

        packetTable.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                if (!isSelected) {
                    String type = (String) table.getValueAt(row, 8);
                    if ("Attack".equals(type)) {
                        c.setBackground(new Color(255, 230, 230));
                    } else {
                        c.setBackground(new Color(230, 255, 230));
                    }
                }

                return c;
            }
        });

        JScrollPane scrollPane = new JScrollPane(packetTable);
        scrollPane.setBorder(BorderFactory.createTitledBorder("📦 Packet Details"));

        return scrollPane;
    }

    private JPanel createActionPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));

        JButton showAll = new JButton("Show All");
        showAll.addActionListener(e -> filterPackets(null));

        JButton showNormal = new JButton("✅ Normal Only");
        showNormal.addActionListener(e -> filterPackets("Normal"));

        JButton showAttacks = new JButton("⚠️ Attacks Only");
        showAttacks.addActionListener(e -> filterPackets("Attack"));

        JButton exportCsv = new JButton("💾 Export to CSV");
        exportCsv.setFont(exportCsv.getFont().deriveFont(Font.BOLD, 13f));
        exportCsv.setBackground(new Color(76, 175, 80));
        exportCsv.setForeground(Color.WHITE);
        exportCsv.setFocusPainted(false);
        exportCsv.addActionListener(e -> exportToCsv());

        JButton close = new JButton("Close");
        close.addActionListener(e -> dispose());

        panel.add(new JLabel("Filter: "));
        panel.add(showAll);
        panel.add(showNormal);
        panel.add(showAttacks);
        panel.add(Box.createHorizontalStrut(20));
        panel.add(exportCsv);
        panel.add(close);

        return panel;
    }

    private void filterPackets(String type) {
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);
        packetTable.setRowSorter(sorter);

        if (type != null) {
            sorter.setRowFilter(RowFilter.regexFilter(type, 8));
        } else {
            sorter.setRowFilter(null);
        }
    }

    private void exportToCsv() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Export Packet Data to CSV");
        fc.setSelectedFile(new File("packet_data_" + System.currentTimeMillis() + ".csv"));

        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File outputFile = fc.getSelectedFile();

        try (PrintWriter writer = new PrintWriter(new FileWriter(outputFile))) {
            writer.println(AI.GeminiPacketGenerator.NetworkPacket.getCSVHeader());

            for (NetworkPacket packet : packets) {
                writer.println(packet.toCSV());
            }

            JOptionPane.showMessageDialog(this,
                    String.format("✅ Successfully exported %d packets to:\n%s\n\nFile size: %.2f KB",
                            packets.size(),
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
}
