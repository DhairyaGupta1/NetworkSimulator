package UI;

import Components.Node;
import Components.Link;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;

public class CanvasPanel extends JPanel {
    private Node selectedForLink = null;
    private final int NODE_RADIUS = 28;
    private final int GRID = 48;
    private double scale = 1.0;
    private double panX = 0.0;
    private double panY = 0.0;

    private Node dragNode = null;
    private Point2D.Double dragOffset = null;
    private Point dragOriginal = null;
    private Point rubberLineEnd = null;
    private Point selectionStart = null;
    private Rectangle selectionRect = null;
    private final List<Node> clipboard = new ArrayList<>();
    private final List<CopiedLink> clipboardLinks = new ArrayList<>();
    private final java.util.Set<Node> selected = new java.util.HashSet<>();
    private final List<Node> nodes = new ArrayList<>();
    private final List<Link> links = new ArrayList<>();
    private final JLabel statusLabel;

    private boolean panning = false;
    private Point panAnchor = null;
    private double panStartX = 0.0, panStartY = 0.0;

    private class CopiedLink {
        Node a, b;

        CopiedLink(Node a, Node b) {
            this.a = a;
            this.b = b;
        }
    }

    private final java.util.Deque<Command> undoStack = new java.util.ArrayDeque<>();
    private final java.util.Deque<Command> redoStack = new java.util.ArrayDeque<>();

    private interface Command {
        void execute();

        void undo();
    }

    private class AddNodeCommand implements Command {
        private final Node node;

        AddNodeCommand(Node n) {
            node = n;
        }

        public void execute() {
            nodes.add(node);
        }

        public void undo() {
            links.removeIf(l -> l.node1 == node || l.node2 == node);
            nodes.remove(node);
            Components.Node.releaseId(node.id);
        }
    }

    private class RemoveNodeCommand implements Command {
        private final Node node;
        private final List<Link> removedLinks = new ArrayList<>();

        RemoveNodeCommand(Node n) {
            node = n;
        }

        public void execute() {
            for (Link l : new ArrayList<>(links)) {
                if (l.node1 == node || l.node2 == node)
                    removedLinks.add(l);
            }
            links.removeAll(removedLinks);
            nodes.remove(node);
            Components.Node.releaseId(node.id);
        }

        public void undo() {
            boolean reserved = Components.Node.reserveId(node.id);
            if (!reserved)
                node.id = Components.Node.nextId();
            nodes.add(node);
            links.addAll(removedLinks);
        }
    }

    private class MoveNodeCommand implements Command {
        private final Node node;
        private final int oldX, oldY, newX, newY;

        MoveNodeCommand(Node n, int ox, int oy, int nx, int ny) {
            node = n;
            oldX = ox;
            oldY = oy;
            newX = nx;
            newY = ny;
        }

        public void execute() {
            node.x = newX;
            node.y = newY;
        }

        public void undo() {
            node.x = oldX;
            node.y = oldY;
        }
    }

    private class AddLinkCommand implements Command {
        private final Node a, b;
        private Link created;

        AddLinkCommand(Node a, Node b) {
            this.a = a;
            this.b = b;
        }

        public void execute() {
            created = new Link(a, b);
            links.add(created);
        }

        public void undo() {
            links.remove(created);
        }
    }

    private class CompositeCommand implements Command {
        private final List<Command> parts = new ArrayList<>();

        void add(Command c) {
            parts.add(c);
        }

        public void execute() {
            for (Command c : parts)
                c.execute();
        }

        public void undo() {
            for (int i = parts.size() - 1; i >= 0; --i)
                parts.get(i).undo();
        }
    }

    public CanvasPanel(JLabel statusLabel) {
        this.statusLabel = statusLabel;
        setBackground(Color.WHITE);
        setupKeyBindings();
        addMouseWheelListener(e -> {
            if (e.getPreciseWheelRotation() < 0) {
                scale *= 1.12;
            } else {
                scale /= 1.12;
            }
            Point mouse = e.getPoint();
            Point2D.Double before = screenToWorld(mouse.x, mouse.y);
            Point2D.Double after = screenToWorld(mouse.x, mouse.y);
            panX += before.x - after.x;
            panY += before.y - after.y;
            repaint();
        });
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                boolean shift = (e.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) != 0;
                boolean ctrl = (e.getModifiersEx() & MouseEvent.CTRL_DOWN_MASK) != 0;
                Point2D.Double world = screenToWorld(e.getX(), e.getY());
                Node hit = findNodeAtWorld(world.x, world.y);
                if (SwingUtilities.isLeftMouseButton(e)) {
                    if (hit != null) {
                        if (shift) {
                            selectedForLink = hit;
                            rubberLineEnd = new Point(e.getX(), e.getY());
                        } else if (ctrl) {
                            if (selected.contains(hit)) {
                                selected.remove(hit);
                            } else {
                                selected.add(hit);
                            }
                            updateStatus();
                        } else {
                            selected.clear();
                            dragNode = hit;
                            Point2D.Double w = screenToWorld(e.getX(), e.getY());
                            dragOffset = new Point2D.Double(w.x - hit.x, w.y - hit.y);
                            dragOriginal = new Point(hit.x, hit.y);
                            updateStatus();
                        }
                    } else {
                        if (ctrl) {
                            selectionStart = new Point(e.getX(), e.getY());
                            selectionRect = new Rectangle(selectionStart.x, selectionStart.y, 0, 0);
                        } else {
                            Point2D.Double w = screenToWorld(e.getX(), e.getY());
                            int sx = snapToGridWorld((int) Math.round(w.x));
                            int sy = snapToGridWorld((int) Math.round(w.y));
                            AddNodeCommand cmd = new AddNodeCommand(new Node(sx, sy));
                            cmd.execute();
                            undoStack.push(cmd);
                            redoStack.clear();
                        }
                    }
                    repaint();
                } else if (SwingUtilities.isRightMouseButton(e)) {
                    if (hit != null) {
                        if (!selected.isEmpty() && selected.contains(hit)) {
                            CompositeCommand comp = new CompositeCommand();
                            for (Node r : new ArrayList<>(selected)) {
                                comp.add(new RemoveNodeCommand(r));
                            }
                            comp.execute();
                            undoStack.push(comp);
                            redoStack.clear();
                            selected.clear();
                            updateStatus();
                            repaint();
                        } else {
                            RemoveNodeCommand cmd = new RemoveNodeCommand(hit);
                            cmd.execute();
                            undoStack.push(cmd);
                            redoStack.clear();
                            repaint();
                        }
                    }
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (selectedForLink != null) {
                    Node dest = findNodeAt(e.getX(), e.getY());
                    if (dest != null && dest != selectedForLink) {
                        AddLinkCommand cmd = new AddLinkCommand(selectedForLink, dest);
                        cmd.execute();
                        undoStack.push(cmd);
                        redoStack.clear();
                    }
                    selectedForLink = null;
                    rubberLineEnd = null;
                    repaint();
                }
                if (selectionStart != null && selectionRect != null) {
                    selected.clear();
                    for (Node n : nodes) {
                        Point sn = worldToScreen(n.x, n.y);
                        if (selectionRect.contains(sn))
                            selected.add(n);
                    }
                    selectionStart = null;
                    selectionRect = null;
                    updateStatus();
                    repaint();
                    return;
                }
                if (dragNode != null && dragOriginal != null) {
                    if (dragNode.x != dragOriginal.x || dragNode.y != dragOriginal.y) {
                        MoveNodeCommand mv = new MoveNodeCommand(dragNode, dragOriginal.x, dragOriginal.y,
                                dragNode.x, dragNode.y);
                        undoStack.push(mv);
                        redoStack.clear();
                    }
                }
                dragNode = null;
                dragOffset = null;
                dragOriginal = null;
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (selectedForLink != null) {
                    rubberLineEnd = new Point(e.getX(), e.getY());
                    repaint();
                    return;
                }

                if (SwingUtilities.isMiddleMouseButton(e) || (e.getModifiersEx() & MouseEvent.ALT_DOWN_MASK) != 0) {
                    if (!panning) {
                        panning = true;
                        panAnchor = e.getPoint();
                        panStartX = panX;
                        panStartY = panY;
                    }
                    double dx = (e.getX() - panAnchor.x) / scale;
                    double dy = (e.getY() - panAnchor.y) / scale;
                    panX = panStartX - dx;
                    panY = panStartY - dy;
                    repaint();
                    return;
                }

                if (dragNode != null) {
                    Point2D.Double w = screenToWorld(e.getX(), e.getY());
                    double nxd = w.x - dragOffset.x;
                    double nyd = w.y - dragOffset.y;
                    int nx = snapToGridWorld((int) Math.round(nxd));
                    int ny = snapToGridWorld((int) Math.round(nyd));
                    dragNode.x = nx;
                    dragNode.y = ny;
                    repaint();
                    return;
                }

                if (selectionStart != null) {
                    int x = Math.min(selectionStart.x, e.getX());
                    int y = Math.min(selectionStart.y, e.getY());
                    int w = Math.abs(selectionStart.x - e.getX());
                    int h = Math.abs(selectionStart.y - e.getY());
                    selectionRect = new Rectangle(x, y, w, h);
                    repaint();
                }
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                boolean ctrl = (e.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) != 0;
                Node hit = findNodeAt(e.getX(), e.getY());
                if (ctrl && SwingUtilities.isLeftMouseButton(e) && hit == null) {
                    selectionStart = new Point(e.getX(), e.getY());
                    selectionRect = new Rectangle(selectionStart.x, selectionStart.y, 0, 0);
                }
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (selectionRect != null && !selectionRect.contains(e.getPoint())) {
                    selectionRect = null;
                    repaint();
                }
            }
        });
    }

    private Node findNodeAt(int x, int y) {
        Point2D.Double w = screenToWorld(x, y);
        return findNodeAtWorld(w.x, w.y);
    }

    private int snapToGridWorld(int v) {
        return Math.round((float) v / GRID) * GRID;
    }

    private Point2D.Double screenToWorld(int sx, int sy) {
        double wx = (sx - getWidth() / 2.0) / scale + panX;
        double wy = (sy - getHeight() / 2.0) / scale + panY;
        return new Point2D.Double(wx, wy);
    }

    private Point worldToScreen(double wx, double wy) {
        int sx = (int) Math.round((wx - panX) * scale + getWidth() / 2.0);
        int sy = (int) Math.round((wy - panY) * scale + getHeight() / 2.0);
        return new Point(sx, sy);
    }

    private Node findNodeAtWorld(double wx, double wy) {
        for (Node n : nodes) {
            double dx = n.x - wx;
            double dy = n.y - wy;
            if (dx * dx + dy * dy <= (double) NODE_RADIUS * (double) NODE_RADIUS)
                return n;
        }
        return null;
    }

    private void updateStatus() {
        if (!selected.isEmpty()) {
            statusLabel.setText("Selection: " + selected.size() + " node(s)");
        } else if (selectionRect != null) {
            statusLabel.setText("Selection rectangle active");
        } else if (selectedForLink != null) {
            statusLabel.setText("Link mode: select destination");
        } else {
            statusLabel.setText("Ready");
        }
    }

    private void undo() {
        if (undoStack.isEmpty())
            return;
        Command cmd = undoStack.pop();
        cmd.undo();
        redoStack.push(cmd);
        repaint();
    }

    private void redo() {
        if (redoStack.isEmpty())
            return;
        Command cmd = redoStack.pop();
        cmd.execute();
        undoStack.push(cmd);
        repaint();
    }

    private void copySelection() {
        clipboard.clear();
        clipboardLinks.clear();
        java.util.List<Node> toCopy = new ArrayList<>();
        if (selectionRect != null) {
            for (Node n : nodes) {
                Point sn = worldToScreen(n.x, n.y);
                if (selectionRect.contains(sn))
                    toCopy.add(n);
            }
        } else {
            toCopy.addAll(selected);
        }

        java.util.Map<Node, Node> map = new java.util.HashMap<>();
        for (Node n : toCopy) {
            Node copy = new Node(n.x, n.y);
            clipboard.add(copy);
            map.put(n, copy);
        }

        for (Link l : links) {
            if (map.containsKey(l.node1) && map.containsKey(l.node2)) {
                clipboardLinks.add(new CopiedLink(l.node1, l.node2));
            }
        }
        updateStatus();
    }

    private void pasteClipboard() {
        if (clipboard.isEmpty())
            return;
        CompositeCommand comp = new CompositeCommand();
        int offset = GRID;

        java.util.Map<Node, Node> createdMap = new java.util.HashMap<>();
        for (Node cn : clipboard) {
            Node p = new Node(cn.x + offset, cn.y + offset);
            comp.add(new AddNodeCommand(p));
            createdMap.put(cn, p);
        }

        for (CopiedLink l : clipboardLinks) {
            Node a = null, b = null;
            for (Node key : createdMap.keySet()) {
                if (key.x == l.a.x && key.y == l.a.y) {
                    a = createdMap.get(key);
                }
                if (key.x == l.b.x && key.y == l.b.y) {
                    b = createdMap.get(key);
                }
            }
            if (a != null && b != null) {
                comp.add(new AddLinkCommand(a, b));
            }
        }

        comp.execute();
        undoStack.push(comp);
        redoStack.clear();
        repaint();
    }

    private void setupKeyBindings() {
        InputMap im = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, KeyEvent.CTRL_DOWN_MASK), "undo");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, KeyEvent.CTRL_DOWN_MASK), "redo");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_DOWN_MASK), "copy");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.CTRL_DOWN_MASK), "paste");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_X, KeyEvent.CTRL_DOWN_MASK), "cut");

        am.put("undo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                undo();
            }
        });
        am.put("redo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                redo();
            }
        });
        am.put("copy", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                copySelection();
            }
        });
        am.put("paste", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                pasteClipboard();
            }
        });
        am.put("cut", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cutSelection();
            }
        });
    }

    private void cutSelection() {
        copySelection();
        CompositeCommand comp = new CompositeCommand();
        List<Node> toRemove = new ArrayList<>();
        if (selectionRect != null) {
            for (Node n : nodes) {
                Point sn = worldToScreen(n.x, n.y);
                if (selectionRect.contains(sn))
                    toRemove.add(n);
            }
        } else {
            toRemove.addAll(selected);
        }
        for (Node r : toRemove) {
            comp.add(new RemoveNodeCommand(r));
        }
        comp.execute();
        undoStack.push(comp);
        redoStack.clear();
        selectionRect = null;
        selected.clear();
        updateStatus();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g2.setColor(new Color(230, 230, 230));
        Point2D.Double topLeft = screenToWorld(0, 0);
        Point2D.Double bottomRight = screenToWorld(getWidth(), getHeight());
        int startX = (int) Math.floor(topLeft.x / GRID) * GRID;
        int endX = (int) Math.ceil(bottomRight.x / GRID) * GRID;
        for (int x = startX; x <= endX; x += GRID) {
            Point sx = worldToScreen(x, 0);
            g2.drawLine(sx.x, 0, sx.x, getHeight());
        }
        int startY = (int) Math.floor(topLeft.y / GRID) * GRID;
        int endY = (int) Math.ceil(bottomRight.y / GRID) * GRID;
        for (int y = startY; y <= endY; y += GRID) {
            Point sy = worldToScreen(0, y);
            g2.drawLine(0, sy.y, getWidth(), sy.y);
        }

        g2.setStroke(new BasicStroke(2f));
        g2.setColor(new Color(120, 120, 120));
        for (Link l : links) {
            Point a = worldToScreen(l.node1.x, l.node1.y);
            Point b = worldToScreen(l.node2.x, l.node2.y);
            g2.drawLine(a.x, a.y, b.x, b.y);
        }

        if (selectedForLink != null && rubberLineEnd != null) {
            Point a = worldToScreen(selectedForLink.x, selectedForLink.y);
            g2.setColor(new Color(200, 50, 50, 180));
            g2.setStroke(
                    new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[] { 6f }, 0));
            g2.drawLine(a.x, a.y, rubberLineEnd.x, rubberLineEnd.y);
        }

        if (selectionRect != null) {
            g2.setColor(new Color(50, 120, 200, 60));
            g2.fill(selectionRect);
            g2.setColor(new Color(50, 120, 200));
            g2.setStroke(new BasicStroke(1f));
            g2.draw(selectionRect);
        }

        for (Node n : nodes) {
            Point s = worldToScreen(n.x, n.y);
            int r = (int) Math.max(6, Math.round(NODE_RADIUS * scale));
            g2.setColor(new Color(0, 0, 0, 40));
            g2.fillOval(s.x - r + 2, s.y - r + 3, r * 2, r * 2);

            GradientPaint gp = new GradientPaint(s.x - r, s.y - r, new Color(80, 160, 240), s.x + r, s.y + r,
                    new Color(30, 90, 200));
            g2.setPaint(gp);
            g2.fillOval(s.x - r, s.y - r, r * 2, r * 2);

            g2.setColor(new Color(20, 45, 130));
            g2.setStroke(new BasicStroke(Math.max(1f, (float) (2f * scale))));
            g2.drawOval(s.x - r, s.y - r, r * 2, r * 2);

            g2.setColor(Color.WHITE);
            String id = Long.toString(n.id);
            FontMetrics fm = g2.getFontMetrics();
            int tw = fm.stringWidth(id);
            g2.drawString(id, s.x - tw / 2, s.y + fm.getAscent() / 2 - 2);

            if (selected.contains(n)) {
                g2.setColor(new Color(255, 200, 50, 180));
                g2.setStroke(new BasicStroke(Math.max(2f, (float) (3f * scale))));
                g2.drawOval(s.x - r - 4, s.y - r - 4, r * 2 + 8, r * 2 + 8);
            }
        }

        if (selectedForLink != null) {
            Point s = worldToScreen(selectedForLink.x, selectedForLink.y);
            g2.setColor(new Color(200, 50, 50));
            g2.setStroke(new BasicStroke(2f));
            g2.drawOval(s.x - NODE_RADIUS - 4, s.y - NODE_RADIUS - 4, NODE_RADIUS * 2 + 8, NODE_RADIUS * 2 + 8);
        }
    }
}
