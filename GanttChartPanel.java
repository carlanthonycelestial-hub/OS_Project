package app;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

final class GanttChartPanel extends JPanel {
    private static final Color[] PALETTE = {
            new Color(59, 130, 246), new Color(16, 185, 129), new Color(245, 158, 11),
            new Color(139, 92, 246), new Color(236, 72, 153), new Color(20, 184, 166),
            new Color(249, 115, 22)
    };
    private static final Color IDLE_FILL = new Color(226, 232, 240);
    private static final Color IDLE_TEXT = new Color(100, 116, 139);
    private static final Color OUTLINE = new Color(30, 41, 59);

    private static final Font EMPTY_FONT = new Font("SansSerif", Font.ITALIC, 12);
    private static final Font BLOCK_FONT = new Font("SansSerif", Font.BOLD, 11);
    private static final Font TIME_FONT = new Font("SansSerif", Font.PLAIN, 10);

    private static final int MARGIN = 20, TOP_Y = 10, BAR_HEIGHT = 32, LABEL_GAP = 6;

    private List<SJFScheduler.Block> blocks = new ArrayList<>();

    GanttChartPanel() {
        setBackground(Color.WHITE);
        ToolTipManager.sharedInstance().registerComponent(this);
    }

    void setBlocks(List<SJFScheduler.Block> blocks) {
        this.blocks = blocks;
        repaint();
    }

    private int xAt(int time) {
        int origin = blocks.get(0).start;
        int total = Math.max(blocks.get(blocks.size() - 1).end - origin, 1);
        double pxPerUnit = (double) Math.max(getWidth() - 2 * MARGIN, 100) / total;
        return MARGIN + (int) Math.round((time - origin) * pxPerUnit);
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        if (blocks.isEmpty() || e.getY() < TOP_Y || e.getY() > TOP_Y + BAR_HEIGHT) return null;
        for (SJFScheduler.Block b : blocks) {
            if (e.getX() >= xAt(b.start) && e.getX() <= Math.max(xAt(b.end), xAt(b.start) + 1)) {
                return b.label + ": " + b.start + " \u2192 " + b.end + "  (" + (b.end - b.start) + " units)";
            }
        }
        return null;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (blocks.isEmpty()) {
            g2.setColor(Color.GRAY);
            g2.setFont(EMPTY_FONT);
            g2.drawString("Run 'Compute SJF' to generate the Gantt chart.", 20, 38);
            return;
        }

        g2.setFont(BLOCK_FONT);
        FontMetrics fm = g2.getFontMetrics();
        int colorIdx = 0;

        for (SJFScheduler.Block b : blocks) {
            int x1 = xAt(b.start);
            int width = Math.max(xAt(b.end) - x1, 1);

            g2.setColor(b.idle ? IDLE_FILL : PALETTE[colorIdx++ % PALETTE.length]);
            g2.fillRect(x1, TOP_Y, width, BAR_HEIGHT);

            int textWidth = fm.stringWidth(b.label);
            if (width >= textWidth + 4) {
                g2.setColor(b.idle ? IDLE_TEXT : Color.WHITE);
                int textY = TOP_Y + (BAR_HEIGHT - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(b.label, x1 + (width - textWidth) / 2, textY);
            }

            g2.setColor(OUTLINE);
            g2.drawRect(x1, TOP_Y, width, BAR_HEIGHT);
        }
        drawTimeLabels(g2);
    }

    private void drawTimeLabels(Graphics2D g2) {
        g2.setFont(TIME_FONT);
        g2.setColor(OUTLINE);
        FontMetrics fm = g2.getFontMetrics();
        int y = TOP_Y + BAR_HEIGHT + 14;

        int lastEnd = blocks.get(blocks.size() - 1).end;
        String endStr = String.valueOf(lastEnd);
        int endW = fm.stringWidth(endStr);
        int endLeft = Math.min(getWidth() - endW - 4, xAt(lastEnd) - endW / 2);

        int prevRight = Integer.MIN_VALUE / 2;
        for (SJFScheduler.Block b : blocks) {
            String s = String.valueOf(b.start);
            int w = fm.stringWidth(s);
            int left = Math.max(2, xAt(b.start) - w / 2);
            if (left < prevRight + LABEL_GAP || left + w + LABEL_GAP > endLeft) continue;
            g2.drawString(s, left, y);
            prevRight = left + w;
        }
        g2.drawString(endStr, endLeft, y);
    }
}
