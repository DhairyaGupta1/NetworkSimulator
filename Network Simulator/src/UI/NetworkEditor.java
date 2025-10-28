package UI;

import javax.swing.*;
import java.awt.*;
import Exporters.ItmTclFrame;

public class NetworkEditor extends JFrame {
    private final CanvasPanel canvas;
    private final JLabel statusLabel = new JLabel("Ready");

    public NetworkEditor() {
        super("Network Simulator - Editor");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLayout(new BorderLayout());

        canvas = new CanvasPanel(statusLabel);
        add(canvas, BorderLayout.CENTER);

        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenuItem exportTcl = new JMenuItem("Export to NS-2 TCL...");
        exportTcl.addActionListener(e -> exportToTcl());
        fileMenu.add(exportTcl);
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);

        JLabel help = new JLabel(
                "Left-click empty: add node | Drag node: move (snap to grid) | Shift+drag: link | Right-click node: delete");
        JPanel south = new JPanel(new BorderLayout());
        south.add(help, BorderLayout.CENTER);
        south.add(statusLabel, BorderLayout.EAST);
        add(south, BorderLayout.SOUTH);
    }

    private void exportToTcl() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Export to NS-2 TCL");
        fc.setSelectedFile(new java.io.File("network.tcl"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
            return;

        JTextField simTimeField = new JTextField("10", 10);
        JTextField bandwidthField = new JTextField("100.0Mb", 10);
        JTextField delayField = new JTextField("1ms", 10);

        JPanel panel = new JPanel(new java.awt.GridLayout(3, 2, 5, 5));
        panel.add(new JLabel("Simulation time (sec):"));
        panel.add(simTimeField);
        panel.add(new JLabel("Bandwidth:"));
        panel.add(bandwidthField);
        panel.add(new JLabel("Delay:"));
        panel.add(delayField);

        int result = JOptionPane.showConfirmDialog(this, panel, "TCL Export Parameters",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION)
            return;

        try {
            int simTime = Integer.parseInt(simTimeField.getText().trim());
            String bandwidth = bandwidthField.getText().trim();
            String delay = delayField.getText().trim();

            ItmTclFrame.writeTcl(fc.getSelectedFile(), canvas.getNodes(), canvas.getLinks(), simTime, bandwidth,
                    delay);

            JOptionPane.showMessageDialog(this, "TCL file exported successfully to:\n" + fc.getSelectedFile(),
                    "Export Complete", JOptionPane.INFORMATION_MESSAGE);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid simulation time: " + ex.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Export failed: " + ex.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            NetworkEditor ne = new NetworkEditor();
            ne.setVisible(true);
        });
    }
}
