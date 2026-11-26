package src.Gui;

import javax.swing.*;
import src.Scheduler.CPUScheduler.Block;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public final class GanttChartPanel extends JPanel {
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

    private List<Block> blocks = new ArrayList<>();
    private final String emptyMessage;

    public GanttChartPanel() {
        this("Run compute to generate the Gantt chart.");
    }

    public GanttChartPanel(String emptyMessage) {
        this.emptyMessage = emptyMessage;
        setBackground(Color.WHITE);
        ToolTipManager.sharedInstance().registerComponent(this);
    }

    public void setBlocks(List<Block> blocks) {
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
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (blocks.isEmpty()) {
            g2.setFont(EMPTY_FONT);
            g2.setColor(Color.GRAY);
            g2.drawString(emptyMessage, MARGIN, TOP_Y + BAR_HEIGHT / 2 + 5);
            return;
        }

        g2.setFont(BLOCK_FONT);
        FontMetrics fm = g2.getFontMetrics();
        int colorIdx = 0;

        for (Block b : blocks) {
            int x1 = xAt(b.start);
            int x2 = xAt(b.end);
            int width = Math.max(x2 - x1, 1);

            if (b.idle) {
                g2.setColor(IDLE_FILL);
                g2.fillRect(x1, TOP_Y, width, BAR_HEIGHT);
                g2.setColor(OUTLINE);
                g2.drawRect(x1, TOP_Y, width, BAR_HEIGHT);
                g2.setColor(IDLE_TEXT);
            } else {
                g2.setColor(PALETTE[colorIdx % PALETTE.length]);
                colorIdx++;
                g2.fillRect(x1, TOP_Y, width, BAR_HEIGHT);
                g2.setColor(OUTLINE);
                g2.drawRect(x1, TOP_Y, width, BAR_HEIGHT);
                g2.setColor(Color.WHITE);
            }

            int sw = fm.stringWidth(b.label);
            if (sw <= width - 4) {
                int textX = x1 + (width - sw) / 2;
                int textY = TOP_Y + (BAR_HEIGHT + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(b.label, textX, textY);
            }
        }

        g2.setFont(TIME_FONT);
        FontMetrics tfm = g2.getFontMetrics();
        g2.setColor(OUTLINE);

        int timeY = TOP_Y + BAR_HEIGHT + LABEL_GAP + tfm.getAscent();
        for (int i = 0; i < blocks.size(); i++) {
            Block b = blocks.get(i);
            if (i == 0) {
                drawTimeMark(g2, tfm, b.start, xAt(b.start), timeY);
            }
            drawTimeMark(g2, tfm, b.end, xAt(b.end), timeY);
        }
    }

    private void drawTimeMark(Graphics2D g2, FontMetrics fm, int time, int x, int y) {
        String str = String.valueOf(time);
        int w = fm.stringWidth(str);
        g2.drawString(str, x - w / 2, y);
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        if (blocks.isEmpty())
            return null;
        int x = e.getX();
        int y = e.getY();
        if (y < TOP_Y || y > TOP_Y + BAR_HEIGHT)
            return null;

        for (Block b : blocks) {
            int x1 = xAt(b.start);
            int x2 = xAt(b.end);
            if (x >= x1 && x <= x2) {
                int duration = b.end - b.start;
                return String.format("%s [%d - %d] (duration: %d ms)", b.label, b.start, b.end, duration);
            }
        }
        return null;
    }
}
