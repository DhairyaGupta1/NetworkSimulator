package UI;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class SimulationResultsWindow extends JFrame {
    private LogViewerPanel logViewer;
    private NAMViewerPanel namViewer;
    private JTabbedPane tabbedPane;

    public SimulationResultsWindow() {
        super("Simulation Results");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(900, 600);
        setLayout(new BorderLayout());

        tabbedPane = new JTabbedPane();

        logViewer = new LogViewerPanel();
        tabbedPane.addTab("Network Logs", logViewer);

        namViewer = new NAMViewerPanel();
        tabbedPane.addTab("NAM Animation", namViewer);

        add(tabbedPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton exportLogsButton = new JButton("Export Logs...");
        exportLogsButton.addActionListener(e -> exportLogs());

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dispose());

        buttonPanel.add(exportLogsButton);
        buttonPanel.add(closeButton);
        add(buttonPanel, BorderLayout.SOUTH);

        setLocationRelativeTo(null);
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
        setVisible(true);
        toFront();
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
