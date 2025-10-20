package UI;

import javax.swing.*;
import java.awt.*;

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

        JLabel help = new JLabel(
                "Left-click empty: add node | Drag node: move (snap to grid) | Shift+drag: link | Right-click node: delete");
        JPanel south = new JPanel(new BorderLayout());
        south.add(help, BorderLayout.CENTER);
        south.add(statusLabel, BorderLayout.EAST);
        add(south, BorderLayout.SOUTH);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            NetworkEditor ne = new NetworkEditor();
            ne.setVisible(true);
        });
    }
}
