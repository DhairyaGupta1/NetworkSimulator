package UI;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import Exporters.ItmTclFrame;
import Exporters.NS2TclGenerator;
import Exporters.NS3ApiClient;
import UI.SimulationConfigDialog.SimulationConfig;
import AI.GeminiPacketGenerator;
import AI.GeminiPacketGenerator.NetworkPacket;

public class NetworkEditor extends JFrame {
    private final CanvasPanel canvas;
    private final JLabel statusLabel = new JLabel("Ready");
    private SimulationResultsWindow resultsWindow;

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

        JMenu simulateMenu = new JMenu("Simulate");
        JMenuItem runSimulation = new JMenuItem("Run Simulation...");
        runSimulation.addActionListener(e -> runSimulation());
        simulateMenu.add(runSimulation);
        menuBar.add(simulateMenu);

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

    private void runSimulation() {
        if (canvas.getNodes().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please create some nodes before running simulation.",
                    "No Network",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        SimulationConfigDialog configDialog = new SimulationConfigDialog(this);
        configDialog.setVisible(true);

        if (!configDialog.isConfirmed()) {
            return;
        }

        SimulationConfig config = configDialog.getConfig();

        RoutingConfigDialog routingDialog = new RoutingConfigDialog(this, canvas.getNodes());
        routingDialog.setVisible(true);

        if (!routingDialog.isConfirmed()) {
            return;
        }

        java.util.List<RoutingConfigDialog.TrafficFlow> flows = routingDialog.getFlows();

        JDialog progressDialog = new JDialog(this, "Running Simulation", true);
        progressDialog.setLayout(new BorderLayout(10, 10));
        JLabel progressLabel = new JLabel("Generating TCL script...", SwingConstants.CENTER);
        progressLabel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        progressDialog.add(progressLabel, BorderLayout.CENTER);
        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressDialog.add(progressBar, BorderLayout.SOUTH);
        progressDialog.setSize(400, 150);
        progressDialog.setLocationRelativeTo(this);

        SwingWorker<NS3ApiClient.SimulationResult, String> worker = new SwingWorker<NS3ApiClient.SimulationResult, String>() {

            @Override
            protected NS3ApiClient.SimulationResult doInBackground() throws Exception {
                publish("Generating NS-2 TCL script with custom flows...");
                File tempTcl = File.createTempFile("network_sim_", ".tcl");
                NS2TclGenerator.generateTcl(tempTcl, canvas.getNodes(), canvas.getLinks(), config, flows);

                publish("Uploading to NS-3 API...");
                NS3ApiClient.SimulationResult result = NS3ApiClient.runSimulation(tempTcl);

                tempTcl.delete();

                return result;
            }

            @Override
            protected void process(java.util.List<String> chunks) {
                if (!chunks.isEmpty()) {
                    progressLabel.setText(chunks.get(chunks.size() - 1));
                }
            }

            @Override
            protected void done() {
                progressDialog.dispose();

                try {
                    NS3ApiClient.SimulationResult result = get();

                    System.out.println("DEBUG: Simulation done. Success=" + result.success);

                    if (result.success) {
                        if (resultsWindow == null) {
                            resultsWindow = new SimulationResultsWindow();
                            System.out.println("DEBUG: Created new SimulationResultsWindow");
                        }

                        String logs = result.traceLogs != null ? result.traceLogs : "No logs generated";

                        resultsWindow.showResults(logs, result.namFile);
                        System.out.println("DEBUG: Results window shown with logs and NAM");

                        if (config.enableDataset) {
                            System.out.println("DEBUG: Starting background packet generation");
                            resultsWindow.showGeneratingDatasetPlaceholder();

                            new Thread(() -> {
                                try {
                                    System.out.println("DEBUG: Calling Gemini API with simulation config...");
                                    java.util.List<NetworkPacket> packets = GeminiPacketGenerator
                                            .generatePacketsFromTopology(
                                                    canvas.getNodes(),
                                                    canvas.getLinks(),
                                                    config.datasetPacketCount,
                                                    config.datasetScenario,
                                                    config);

                                    System.out.println("DEBUG: Packets generated: " + packets.size());

                                    SwingUtilities.invokeLater(() -> {
                                        resultsWindow.setPacketData(packets);
                                        System.out.println("DEBUG: Packet data updated in UI");
                                    });

                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                    SwingUtilities.invokeLater(() -> {
                                        JOptionPane.showMessageDialog(NetworkEditor.this,
                                                "Packet generation failed: " + ex.getMessage(),
                                                "Generation Error",
                                                JOptionPane.ERROR_MESSAGE);
                                    });
                                }
                            }).start();
                        } else {
                            JOptionPane.showMessageDialog(NetworkEditor.this,
                                    "Simulation completed successfully!",
                                    "Simulation Complete",
                                    JOptionPane.INFORMATION_MESSAGE);
                        }

                    } else {
                        JOptionPane.showMessageDialog(NetworkEditor.this,
                                "Simulation failed:\n" + result.errorMessage,
                                "Simulation Error",
                                JOptionPane.ERROR_MESSAGE);
                    }

                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(NetworkEditor.this,
                            "Simulation error: " + ex.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        };

        worker.execute();
        progressDialog.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            NetworkEditor ne = new NetworkEditor();
            ne.setVisible(true);
        });
    }
}
