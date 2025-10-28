package UI;

import javax.swing.*;
import java.awt.*;

public class LogViewerPanel extends JPanel {
    private JTextArea logArea;
    private JScrollPane scrollPane;

    public LogViewerPanel() {
        setLayout(new BorderLayout());

        JLabel title = new JLabel("Network Logs");
        title.setFont(new Font("Arial", Font.BOLD, 14));
        title.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        add(title, BorderLayout.NORTH);

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        logArea.setBackground(new Color(30, 30, 30));
        logArea.setForeground(new Color(200, 200, 200));
        logArea.setCaretColor(Color.WHITE);

        scrollPane = new JScrollPane(logArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        add(scrollPane, BorderLayout.CENTER);

        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton clearButton = new JButton("Clear Logs");
        clearButton.addActionListener(e -> clearLogs());
        controlPanel.add(clearButton);
        add(controlPanel, BorderLayout.SOUTH);

        setPreferredSize(new Dimension(600, 300));
    }

    public void setLogs(String logs) {
        logArea.setText(logs);
        logArea.setCaretPosition(0);
    }

    public void appendLog(String log) {
        logArea.append(log + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    public void clearLogs() {
        logArea.setText("");
    }

    public String getLogs() {
        return logArea.getText();
    }
}
