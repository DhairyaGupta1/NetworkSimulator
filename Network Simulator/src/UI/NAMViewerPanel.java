package UI;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;

public class NAMViewerPanel extends JPanel {
    private AnimationCanvas canvas;
    private JLabel statusLabel;
    private JButton playButton, pauseButton, stopButton, rewindButton;
    private JSlider timeSlider;
    private JLabel timeLabel;
    private File currentNamFile;
    private NAMParser.NAMData namData;

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

        canvas = new AnimationCanvas();
        add(canvas, BorderLayout.CENTER);

        JPanel controlPanel = new JPanel(new BorderLayout());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        rewindButton = new JButton("⏮ Rewind");
        playButton = new JButton("▶ Play");
        pauseButton = new JButton("⏸ Pause");
        stopButton = new JButton("⏹ Stop");

        JButton exportNamButton = new JButton("💾 Export NAM");
        JButton screenshotButton = new JButton("📸 Screenshot");
        JButton recordButton = new JButton("🎥 Record GIF");

        rewindButton.addActionListener(e -> canvas.rewind());
        playButton.addActionListener(e -> canvas.play());
        pauseButton.addActionListener(e -> canvas.pause());
        stopButton.addActionListener(e -> canvas.stop());
        exportNamButton.addActionListener(e -> exportNamFile());
        screenshotButton.addActionListener(e -> takeScreenshot());
        recordButton.addActionListener(e -> recordAnimation());

        pauseButton.setEnabled(false);
        stopButton.setEnabled(false);

        buttonPanel.add(rewindButton);
        buttonPanel.add(playButton);
        buttonPanel.add(pauseButton);
        buttonPanel.add(stopButton);
        buttonPanel.add(Box.createHorizontalStrut(15)); // Spacing
        buttonPanel.add(exportNamButton);
        buttonPanel.add(screenshotButton);
        buttonPanel.add(recordButton);

        JPanel sliderPanel = new JPanel(new BorderLayout());
        timeLabel = new JLabel("Time: 0.00s / 0.00s");
        timeLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        timeSlider = new JSlider(0, 1000, 0);
        timeSlider.addChangeListener(e -> {
            if (!timeSlider.getValueIsAdjusting() && namData != null) {
                double t = (timeSlider.getValue() / 1000.0) * namData.maxTime;
                canvas.setTime(t);
            }
        });
        sliderPanel.add(timeLabel, BorderLayout.WEST);
        sliderPanel.add(timeSlider, BorderLayout.CENTER);

        controlPanel.add(buttonPanel, BorderLayout.NORTH);
        controlPanel.add(sliderPanel, BorderLayout.SOUTH);
        add(controlPanel, BorderLayout.SOUTH);

        setPreferredSize(new Dimension(700, 500));
    }

    public void loadNamFile(File namFile) {
        if (namFile == null || !namFile.exists()) {
            statusLabel.setText("NAM file not found");
            canvas.clear();
            currentNamFile = null;
            namData = null;
            return;
        }

        currentNamFile = namFile;

        try {
            namData = NAMParser.parse(namFile);
            canvas.setNAMData(namData);

            statusLabel.setText(String.format("Loaded: %s (%d nodes, %d links, %d events)",
                    namFile.getName(), namData.nodes.size(), namData.links.size(), namData.events.size()));

            timeLabel.setText(String.format("Time: 0.00s / %.2fs", namData.maxTime));

        } catch (IOException e) {
            statusLabel.setText("Error loading NAM file");
            JOptionPane.showMessageDialog(this, "Error parsing NAM file: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void clear() {
        canvas.clear();
        statusLabel.setText("No animation loaded");
        currentNamFile = null;
        namData = null;
        timeLabel.setText("Time: 0.00s / 0.00s");
    }

    public File getCurrentNamFile() {
        return currentNamFile;
    }

    private void exportNamFile() {
        if (currentNamFile == null) {
            JOptionPane.showMessageDialog(this,
                    "No NAM file loaded to export.",
                    "No File", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Export NAM File");
        fc.setSelectedFile(new File("network_animation.nam"));

        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                File dest = fc.getSelectedFile();
                java.nio.file.Files.copy(currentNamFile.toPath(), dest.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                JOptionPane.showMessageDialog(this,
                        "NAM file exported successfully to:\n" + dest.getAbsolutePath(),
                        "Export Complete", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                        "Failed to export NAM file: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void takeScreenshot() {
        if (namData == null) {
            JOptionPane.showMessageDialog(this,
                    "No animation loaded to capture.",
                    "No Animation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Save Screenshot");
        fc.setSelectedFile(new File("animation_screenshot.png"));

        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                File outputFile = fc.getSelectedFile();
                java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(
                        canvas.getWidth(), canvas.getHeight(), java.awt.image.BufferedImage.TYPE_INT_RGB);
                Graphics2D g2 = image.createGraphics();
                canvas.paint(g2);
                g2.dispose();

                javax.imageio.ImageIO.write(image, "png", outputFile);
                JOptionPane.showMessageDialog(this,
                        "Screenshot saved to:\n" + outputFile.getAbsolutePath(),
                        "Screenshot Saved", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                        "Failed to save screenshot: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void recordAnimation() {
        if (namData == null) {
            JOptionPane.showMessageDialog(this,
                    "No animation loaded to record.",
                    "No Animation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JOptionPane.showMessageDialog(this,
                "GIF Recording:\n\n" +
                        "1. Click OK to start recording\n" +
                        "2. Play the animation (it will auto-capture frames)\n" +
                        "3. Recording stops at the end\n\n" +
                        "Note: For high-quality recordings, use screen recording software\n" +
                        "like OBS Studio or Windows Game Bar (Win+G)",
                "Recording Info", JOptionPane.INFORMATION_MESSAGE);

        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Save Animation Frames");
        fc.setSelectedFile(new File("animation_frames"));
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File frameDir = fc.getSelectedFile();
            if (!frameDir.exists()) {
                frameDir.mkdirs();
            }

            canvas.startFrameCapture(frameDir);
            JOptionPane.showMessageDialog(this,
                    "Frame capture started!\n" +
                            "Frames will be saved to: " + frameDir.getAbsolutePath() + "\n\n" +
                            "Use a tool like FFmpeg to convert frames to GIF:\n" +
                            "ffmpeg -i frame_%04d.png -vf fps=20 output.gif",
                    "Recording Started", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private class AnimationCanvas extends JPanel {
        private NAMParser.NAMData data;
        private double currentTime = 0;
        private javax.swing.Timer animationTimer;
        private List<Packet> activePackets = new ArrayList<>();
        private int eventIndex = 0;

        private boolean capturingFrames = false;
        private File frameCaptureDir = null;
        private int frameNumber = 0;

        private class Packet {
            double startTime;
            int srcNode, dstNode;
            double progress;
            double visualProgress;
            Color color;
            String type;
            String label;

            Packet(int src, int dst, String type, double time) {
                this.srcNode = src;
                this.dstNode = dst;
                this.type = type;
                this.startTime = time;
                this.progress = 0;
                this.visualProgress = 0;
                this.label = "P" + (activePackets.size() + 1);

                if (type.contains("tcp"))
                    color = new Color(0, 120, 215);
                else if (type.contains("udp"))
                    color = new Color(0, 180, 100);
                else if (type.contains("ack"))
                    color = new Color(255, 140, 0);
                else
                    color = new Color(150, 150, 150);
            }

            void updateVisualProgress() {

                double diff = progress - visualProgress;
                visualProgress += diff * 0.5;
            }
        }

        public AnimationCanvas() {
            setBackground(new Color(245, 245, 250));
            setPreferredSize(new Dimension(600, 400));
            setDoubleBuffered(true);

            animationTimer = new javax.swing.Timer(50, e -> {
                if (data != null && currentTime < data.maxTime) {
                    currentTime += 0.05;
                    updateAnimation();

                    int visibleCount = 0;
                    for (Packet pkt : activePackets) {
                        if (pkt.visualProgress < 1.0) {
                            pkt.updateVisualProgress();
                            visibleCount++;
                        }
                    }

                    if (visibleCount > 50 && animationTimer.getDelay() < 80) {
                        animationTimer.setDelay(80);
                    } else if (visibleCount <= 50 && animationTimer.getDelay() > 50) {
                        animationTimer.setDelay(50);
                    }

                    repaint();

                    timeLabel.setText(String.format("Time: %.2fs / %.2fs", currentTime, data.maxTime));
                    timeSlider.setValue((int) ((currentTime / data.maxTime) * 1000));
                } else {
                    pause();
                }
            });
        }

        public void setNAMData(NAMParser.NAMData data) {
            this.data = data;
            this.currentTime = 0;
            this.eventIndex = 0;
            this.activePackets.clear();
            this.capturingFrames = false;
            this.frameNumber = 0;
            repaint();
        }

        public void startFrameCapture(File directory) {
            this.frameCaptureDir = directory;
            this.capturingFrames = true;
            this.frameNumber = 0;
        }

        public void play() {
            if (data != null) {
                animationTimer.start();
                playButton.setEnabled(false);
                pauseButton.setEnabled(true);
                stopButton.setEnabled(true);
            }
        }

        public void pause() {
            animationTimer.stop();
            playButton.setEnabled(true);
            pauseButton.setEnabled(false);
        }

        public void stop() {
            animationTimer.stop();
            currentTime = 0;
            eventIndex = 0;
            activePackets.clear();
            repaint();
            playButton.setEnabled(true);
            pauseButton.setEnabled(false);
            stopButton.setEnabled(false);
            if (data != null) {
                timeLabel.setText(String.format("Time: 0.00s / %.2fs", data.maxTime));
                timeSlider.setValue(0);
            }
        }

        public void rewind() {
            stop();
        }

        public void setTime(double time) {
            if (data == null)
                return;
            currentTime = Math.max(0, Math.min(time, data.maxTime));

            activePackets.clear();
            eventIndex = 0;
            while (eventIndex < data.events.size() && data.events.get(eventIndex).time <= currentTime) {
                processEvent(data.events.get(eventIndex));
                eventIndex++;
            }

            updatePacketPositions();
            repaint();

            timeLabel.setText(String.format("Time: %.2fs / %.2fs", currentTime, data.maxTime));
        }

        public void clear() {
            data = null;
            currentTime = 0;
            activePackets.clear();
            eventIndex = 0;
            repaint();
        }

        private void updateAnimation() {
            while (eventIndex < data.events.size() && data.events.get(eventIndex).time <= currentTime) {
                processEvent(data.events.get(eventIndex));
                eventIndex++;
            }

            updatePacketPositions();

            activePackets.removeIf(p -> p.progress >= 0.95);

            if (activePackets.size() > 100) {
                while (activePackets.size() > 100) {
                    activePackets.remove(0);
                }
            }
        }

        private void processEvent(NAMParser.NAMEvent event) {
            if (event.type.equals("h") || event.type.equals("+")) {
                if (activePackets.size() < 150) {
                    Packet pkt = new Packet(event.srcNode, event.dstNode, event.packetType, event.time);
                    activePackets.add(pkt);
                }
            }
        }

        private void updatePacketPositions() {
            for (Packet pkt : activePackets) {
                double elapsed = currentTime - pkt.startTime;
                pkt.progress = Math.min(1.0, elapsed / 1.5);
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            if (data == null || data.nodes.isEmpty()) {
                g.setColor(Color.GRAY);
                g.setFont(new Font("Arial", Font.BOLD, 16));
                String msg = "No animation data - Run a simulation to see packets flow!";
                FontMetrics fm = g.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(msg)) / 2;
                int y = getHeight() / 2;
                g.drawString(msg, x, y);
                return;
            }

            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED); // Optimize for speed
            g2.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_SPEED);

            double margin = 60;
            double width = getWidth() - 2 * margin;
            double height = getHeight() - 2 * margin - 100; // Space for legend

            double dataWidth = Math.max(data.maxX - data.minX, 100);
            double dataHeight = Math.max(data.maxY - data.minY, 100);

            double scale = Math.min(width / dataWidth, height / dataHeight) * 0.9;

            g2.setStroke(new BasicStroke(3));

            java.util.Map<String, Integer> linkPacketCount = new java.util.HashMap<>();
            int visiblePacketCount = 0;
            for (Packet pkt : activePackets) {
                if (pkt.visualProgress < 0.95) {
                    String linkKey = Math.min(pkt.srcNode, pkt.dstNode) + "-" + Math.max(pkt.srcNode, pkt.dstNode);
                    linkPacketCount.put(linkKey, linkPacketCount.getOrDefault(linkKey, 0) + 1);
                    visiblePacketCount++;
                }
            }

            boolean heavyLoad = visiblePacketCount > 50;
            int arrowCount = heavyLoad ? 1 : 3;

            for (NAMParser.NAMLink link : data.links) {
                NAMParser.NAMNode n1 = data.nodes.get(link.srcNode);
                NAMParser.NAMNode n2 = data.nodes.get(link.dstNode);
                if (n1 != null && n2 != null) {
                    int x1 = (int) (margin + (n1.x - data.minX) * scale);
                    int y1 = (int) (margin + (n1.y - data.minY) * scale);
                    int x2 = (int) (margin + (n2.x - data.minX) * scale);
                    int y2 = (int) (margin + (n2.y - data.minY) * scale);

                    String linkKey = Math.min(link.srcNode, link.dstNode) + "-" + Math.max(link.srcNode, link.dstNode);
                    boolean hasPackets = linkPacketCount.containsKey(linkKey);

                    if (hasPackets) {
                        if (!heavyLoad) {
                            float pulse = (float) (0.7 + 0.3 * Math.sin(currentTime * 3));
                            g2.setColor(new Color(50, 150, 255, (int) (100 * pulse)));
                            g2.setStroke(new BasicStroke(6));
                            g2.drawLine(x1, y1, x2, y2);
                        }

                        g2.setColor(new Color(70, 170, 255));
                        g2.setStroke(new BasicStroke(3));
                    } else {
                        g2.setColor(new Color(150, 150, 150));
                    }
                    g2.drawLine(x1, y1, x2, y2);

                    if (hasPackets) {
                        int packetCount = linkPacketCount.get(linkKey);

                        for (int i = 0; i < arrowCount; i++) {
                            double offset = (currentTime * 0.3 + i * (1.0 / arrowCount)) % 1.0;
                            drawAnimatedArrow(g2, x1, y1, x2, y2, offset, new Color(70, 170, 255));
                        }

                        if (!heavyLoad) {
                            g2.setFont(new Font("Arial", Font.BOLD, 10));
                            g2.setColor(new Color(255, 100, 50));
                            int mx = (x1 + x2) / 2;
                            int my = (y1 + y2) / 2;
                            String countLabel = packetCount + "p";
                            FontMetrics fm = g2.getFontMetrics();

                            int labelWidth = fm.stringWidth(countLabel);
                            g2.setColor(new Color(255, 255, 255, 220));
                            g2.fillRoundRect(mx - labelWidth / 2 - 3, my - 16, labelWidth + 6, 14, 4, 4);

                            g2.setColor(new Color(255, 100, 50));
                            g2.drawString(countLabel, mx - labelWidth / 2, my - 6);
                        }
                    } else {
                        drawArrow(g2, x1, y1, x2, y2, new Color(100, 100, 100));
                    }
                }
            }

            int packetCount = 0;

            Color glowColor, borderColor = Color.WHITE;
            BasicStroke packetStroke = new BasicStroke(1.5f);

            for (Packet pkt : activePackets) {
                if (pkt.visualProgress >= 0.95)
                    continue;

                NAMParser.NAMNode src = data.nodes.get(pkt.srcNode);
                NAMParser.NAMNode dst = data.nodes.get(pkt.dstNode);
                if (src == null || dst == null)
                    continue;

                int x1 = (int) (margin + (src.x - data.minX) * scale);
                int y1 = (int) (margin + (src.y - data.minY) * scale);
                int x2 = (int) (margin + (dst.x - data.minX) * scale);
                int y2 = (int) (margin + (dst.y - data.minY) * scale);

                int px = (int) (x1 + (x2 - x1) * pkt.visualProgress);
                int py = (int) (y1 + (y2 - y1) * pkt.visualProgress);

                if (heavyLoad) {
                    g2.setColor(pkt.color);
                    g2.fillOval(px - 6, py - 6, 12, 12);
                } else {
                    glowColor = new Color(pkt.color.getRed(), pkt.color.getGreen(), pkt.color.getBlue(), 80);
                    g2.setColor(glowColor);
                    g2.fillOval(px - 12, py - 12, 24, 24); // Glow

                    g2.setColor(pkt.color);
                    g2.fillOval(px - 8, py - 8, 16, 16); // Packet
                    g2.setColor(borderColor);
                    g2.setStroke(packetStroke);
                    g2.drawOval(px - 8, py - 8, 16, 16);

                    if (visiblePacketCount < 30) {
                        g2.setFont(new Font("Arial", Font.BOLD, 8));
                        String pktLabel = pkt.label;
                        FontMetrics fm = g2.getFontMetrics();
                        g2.drawString(pktLabel, px - fm.stringWidth(pktLabel) / 2, py + 3);
                    }
                }

                packetCount++;
            }

            for (NAMParser.NAMNode node : data.nodes.values()) {
                int x = (int) (margin + (node.x - data.minX) * scale);
                int y = (int) (margin + (node.y - data.minY) * scale);

                g2.setColor(new Color(0, 0, 0, 40));
                g2.fillOval(x - 27, y - 25, 54, 54);

                g2.setColor(new Color(70, 130, 180));
                g2.fillOval(x - 25, y - 25, 50, 50);

                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(2.5f));
                g2.drawOval(x - 25, y - 25, 50, 50);

                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Arial", Font.BOLD, 16));
                FontMetrics fm = g2.getFontMetrics();
                String label = node.label != null && !node.label.isEmpty() ? node.label : ("N" + node.id);
                int lx = x - fm.stringWidth(label) / 2;
                int ly = y + fm.getAscent() / 2 - 2;
                g2.drawString(label, lx, ly);

                g2.setColor(Color.BLACK);
                g2.setFont(new Font("Arial", Font.BOLD, 11));
                fm = g2.getFontMetrics();
                String desc = node.label != null && !node.label.isEmpty() ? node.label : ("Node " + node.id);
                lx = x - fm.stringWidth(desc) / 2;
                g2.drawString(desc, lx, y + 38);
            }

            int legendY = getHeight() - 80;
            g2.setColor(new Color(255, 255, 255, 230));
            g2.fillRoundRect(20, legendY, 400, 70, 10, 10);
            g2.setColor(Color.BLACK);
            g2.setStroke(new BasicStroke(2));
            g2.drawRoundRect(20, legendY, 400, 70, 10, 10);

            g2.setFont(new Font("Arial", Font.BOLD, 14));
            g2.drawString("Packet Legend:", 30, legendY + 20);

            g2.setColor(new Color(0, 120, 215));
            g2.fillOval(30, legendY + 30, 16, 16);
            g2.setColor(Color.BLACK);
            g2.setFont(new Font("Arial", Font.PLAIN, 11));
            g2.drawString("TCP", 52, legendY + 43);

            g2.setColor(new Color(0, 180, 100));
            g2.fillOval(95, legendY + 30, 16, 16);
            g2.setColor(Color.BLACK);
            g2.drawString("UDP", 117, legendY + 43);

            g2.setFont(new Font("Arial", Font.BOLD, 13));
            g2.setColor(new Color(220, 50, 50));
            String perfMode = heavyLoad ? " [TURBO]" : "";
            g2.drawString("Active: " + packetCount + perfMode, 170, legendY + 20);

            if (heavyLoad) {
                g2.setFont(new Font("Arial", Font.PLAIN, 10));
                g2.setColor(new Color(255, 140, 0));
                g2.drawString("⚡ Performance mode active", 170, legendY + 38);
            }

            g2.setFont(new Font("Arial", Font.PLAIN, 11));
            g2.setColor(Color.DARK_GRAY);
            g2.drawString("Events: " + eventIndex + "/" + data.events.size(), 170, legendY + 55);

            if (capturingFrames) {
                g2.setFont(new Font("Arial", Font.BOLD, 11));
                g2.setColor(Color.RED);
                g2.fillOval(350, legendY + 10, 12, 12);
                g2.setColor(Color.BLACK);
                g2.drawString("REC", 368, legendY + 20);
            }

            if (capturingFrames && frameCaptureDir != null) {
                try {
                    java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(
                            getWidth(), getHeight(), java.awt.image.BufferedImage.TYPE_INT_RGB);
                    Graphics2D g2d = image.createGraphics();
                    paint(g2d);
                    g2d.dispose();

                    File frameFile = new File(frameCaptureDir, String.format("frame_%04d.png", frameNumber++));
                    javax.imageio.ImageIO.write(image, "png", frameFile);

                    if (currentTime >= data.maxTime) {
                        capturingFrames = false;
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    capturingFrames = false;
                }
            }
        }

        private void drawArrow(Graphics2D g2, int x1, int y1, int x2, int y2, Color color) {
            double angle = Math.atan2(y2 - y1, x2 - x1);
            int arrowSize = 18;

            int mx = (int) (x1 + (x2 - x1) * 0.7);
            int my = (int) (y1 + (y2 - y1) * 0.7);

            int[] xPoints = new int[3];
            int[] yPoints = new int[3];

            xPoints[0] = mx;
            yPoints[0] = my;

            xPoints[1] = (int) (mx - arrowSize * Math.cos(angle - Math.PI / 6));
            yPoints[1] = (int) (my - arrowSize * Math.sin(angle - Math.PI / 6));

            xPoints[2] = (int) (mx - arrowSize * Math.cos(angle + Math.PI / 6));
            yPoints[2] = (int) (my - arrowSize * Math.sin(angle + Math.PI / 6));

            g2.setColor(color);
            g2.fillPolygon(xPoints, yPoints, 3);

            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(2.5f));
            g2.drawPolygon(xPoints, yPoints, 3);
        }

        private void drawAnimatedArrow(Graphics2D g2, int x1, int y1, int x2, int y2, double position, Color color) {
            double angle = Math.atan2(y2 - y1, x2 - x1);
            int arrowSize = 14;

            int mx = (int) (x1 + (x2 - x1) * position);
            int my = (int) (y1 + (y2 - y1) * position);

            int[] xPoints = new int[3];
            int[] yPoints = new int[3];

            xPoints[0] = mx;
            yPoints[0] = my;

            xPoints[1] = (int) (mx - arrowSize * Math.cos(angle - Math.PI / 6));
            yPoints[1] = (int) (my - arrowSize * Math.sin(angle - Math.PI / 6));
=
            xPoints[2] = (int) (mx - arrowSize * Math.cos(angle + Math.PI / 6));
            yPoints[2] = (int) (my - arrowSize * Math.sin(angle + Math.PI / 6));

            int alpha = (int) (220 * (1.0 - Math.abs(position - 0.5) * 0.4));
            g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
            g2.fillPolygon(xPoints, yPoints, 3);

            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(2f)); 
            g2.drawPolygon(xPoints, yPoints, 3);
        }
    }
}