package UI;

import javax.swing.*;
import java.awt.*;
import java.io.*;

public class NAMViewerPanel extends JPanel {
    private JTextArea namContent;
    private JLabel statusLabel;
    private File currentNamFile;

    public NAMViewerPanel() {
        setLayout(new BorderLayout());

        JPanel topPanel = new JPanel(new BorderLayout());
        JLabel title = new JLabel("NAM Animation");
        title.setFont(new Font("Arial", Font.BOLD, 14));
        title.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        topPanel.add(title, BorderLayout.WEST);

        statusLabel = new JLabel("No animation loaded");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        topPanel.add(statusLabel, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        namContent = new JTextArea();
        namContent.setEditable(false);
        namContent.setFont(new Font("Monospaced", Font.PLAIN, 11));
        namContent.setBackground(new Color(250, 250, 250));
        namContent.setForeground(new Color(50, 50, 50));

        JScrollPane scrollPane = new JScrollPane(namContent);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        add(scrollPane, BorderLayout.CENTER);

        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        infoPanel.add(new JLabel("ℹ NAM files can be viewed with NS-2 NAM tool: nam <filename.nam>"));
        add(infoPanel, BorderLayout.SOUTH);

        setPreferredSize(new Dimension(600, 300));
    }

    public void loadNamFile(File namFile) {
        if (namFile == null || !namFile.exists()) {
            statusLabel.setText("NAM file not found");
            namContent.setText("");
            currentNamFile = null;
            return;
        }

        currentNamFile = namFile;

        try {
            StringBuilder content = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(namFile))) {
                String line;
                int lineCount = 0;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                    lineCount++;
                }

                namContent.setText(content.toString());
                namContent.setCaretPosition(0);
                statusLabel.setText("Loaded: " + namFile.getName() + " (" + lineCount + " lines)");
            }
        } catch (IOException e) {
            statusLabel.setText("Error loading NAM file");
            namContent.setText("Error: " + e.getMessage());
        }
    }

    public void clear() {
        namContent.setText("");
        statusLabel.setText("No animation loaded");
        currentNamFile = null;
    }

    public File getCurrentNamFile() {
        return currentNamFile;
    }
}
