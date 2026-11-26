package src.Gui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Reusable simple result panel for Disk Scheduling algorithms (SCAN, LOOK, etc.).
 * Displays the seek sequence table, summary metrics, and head movement graph.
 */
public class DiskResultPanel extends JPanel {
    private static final Font BOLD_13 = new Font("SansSerif", Font.BOLD, 13);

    public static final class StepData {
        public final int order;
        public final String id;
        public final int from, to, distance;

        public StepData(int order, String id, int from, int to, int distance) {
            this.order = order;
            this.id = id;
            this.from = from;
            this.to = to;
            this.distance = distance;
        }
    }

    private final MainApp mainApp;
    private String backCard = MainApp.MENU_CARD;

    private final JLabel headerTitle = new JLabel("Disk Scheduling Results", SwingConstants.CENTER);
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[] { "Step", "Req ID", "From", "To", "Seek" }, 0) {
        @Override
        public boolean isCellEditable(int r, int c) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);
    private final JLabel totalLbl = new JLabel();
    private final JLabel avgLbl = new JLabel();
    private final JLabel seqLbl = new JLabel();

    private int chartHead;
    private int chartMaxTrack = -1; // -1 means scale based on min/max visited
    private List<StepData> chartSteps = new ArrayList<>();

    private final JPanel chartPanel = new JPanel() {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            drawChart((Graphics2D) g, getWidth(), getHeight());
        }
    };

    public DiskResultPanel(MainApp mainApp) {
        this.mainApp = mainApp;
        setLayout(new BorderLayout(12, 12));
        setBorder(new EmptyBorder(15, 20, 15, 20));
        setBackground(new Color(248, 250, 252));

        // Top Bar with Back Button
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);

        JButton backBtn = new JButton("\u2190 Back");
        backBtn.setFont(BOLD_13);
        backBtn.addActionListener(e -> mainApp.showCard(backCard));

        headerTitle.setFont(new Font("SansSerif", Font.BOLD, 20));
        headerTitle.setForeground(new Color(15, 23, 42));

        topBar.add(backBtn, BorderLayout.WEST);
        topBar.add(headerTitle, BorderLayout.CENTER);
        add(topBar, BorderLayout.NORTH);

        // Center Table & Stats
        table.setRowHeight(25);
        JPanel stats = new JPanel(new GridLayout(3, 1, 6, 6));
        stats.setBackground(new Color(241, 245, 249));
        stats.setBorder(new EmptyBorder(10, 12, 10, 12));

        totalLbl.setFont(BOLD_13);
        avgLbl.setFont(BOLD_13);
        seqLbl.setFont(BOLD_13);
        stats.add(totalLbl);
        stats.add(avgLbl);
        stats.add(seqLbl);

        JPanel centerPanel = new JPanel(new BorderLayout(8, 8));
        centerPanel.setBackground(Color.WHITE);
        centerPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(203, 213, 225)),
                "Seek Sequence & Metrics", TitledBorder.LEFT, TitledBorder.TOP, BOLD_13));
        centerPanel.add(new JScrollPane(table), BorderLayout.CENTER);
        centerPanel.add(stats, BorderLayout.SOUTH);
        add(centerPanel, BorderLayout.CENTER);

        // Bottom Chart
        chartPanel.setBackground(Color.WHITE);
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBackground(Color.WHITE);
        bottom.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(203, 213, 225)),
                "Disk Head Movement (Track Scale)", TitledBorder.LEFT, TitledBorder.TOP, BOLD_13));
        bottom.setPreferredSize(new Dimension(0, 160));
        bottom.add(chartPanel, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        clear();
    }

    /**
     * Display results and switch view to this panel.
     */
    public void displayResult(String title, String returnCard, int head, int maxTrack,
                              List<StepData> steps, int totalMovement, double avgSeek, String seq) {
        this.backCard = returnCard;
        headerTitle.setText(title + " - Results");
        this.chartHead = head;
        this.chartMaxTrack = maxTrack;
        this.chartSteps = new ArrayList<>(steps);

        tableModel.setRowCount(0);
        for (StepData s : steps) {
            tableModel.addRow(new Object[] { s.order, s.id, s.from, s.to, s.distance });
        }

        totalLbl.setText("Total Head Movement: " + totalMovement + " tracks");
        avgLbl.setText("Average Seek Distance: " + String.format("%.2f tracks", avgSeek));
        seqLbl.setText("Seek Sequence: " + seq);
        seqLbl.setToolTipText(seq);

        chartPanel.repaint();
        mainApp.showCard(MainApp.DISK_RESULT_CARD);
    }

    public void clear() {
        tableModel.setRowCount(0);
        totalLbl.setText("Total Head Movement: -");
        avgLbl.setText("Average Seek Distance: -");
        seqLbl.setText("Seek Sequence: -");
        chartSteps.clear();
        chartPanel.repaint();
    }

    private void drawChart(Graphics2D g2, int w, int h) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (chartSteps.isEmpty()) {
            g2.setColor(Color.GRAY);
            g2.setFont(new Font("SansSerif", Font.ITALIC, 12));
            g2.drawString("No disk head movement data available.", 20, 42);
            return;
        }

        int margin = 28;
        int n = chartSteps.size();
        int span = Math.max(w - 2 * margin, 100);
        int top = 24;
        int bot = Math.max(h - 18, top + 20);

        int min, max;
        if (chartMaxTrack > 0) {
            min = 0;
            max = chartMaxTrack;
        } else {
            min = chartHead;
            max = chartHead;
            for (StepData s : chartSteps) {
                min = Math.min(min, Math.min(s.from, s.to));
                max = Math.max(max, Math.max(s.from, s.to));
            }
            if (min == max) max = min + 1;
        }

        // Base horizontal line
        g2.setColor(new Color(148, 163, 184));
        g2.drawLine(margin, 16, margin + span, 16);

        // Movement lines
        g2.setColor(new Color(37, 99, 235));
        g2.setStroke(new BasicStroke(2f));
        for (int i = 0; i < n; i++) {
            StepData s = chartSteps.get(i);
            int x1 = margin + (int) Math.round(((double) (s.from - min) / (max - min)) * span);
            int x2 = margin + (int) Math.round(((double) (s.to - min) / (max - min)) * span);
            int y1 = top + (int) Math.round(((double) i / n) * (bot - top));
            int y2 = top + (int) Math.round(((double) (i + 1) / n) * (bot - top));
            g2.drawLine(x1, y1, x2, y2);
        }

        // Point markers and track labels
        g2.setFont(new Font("SansSerif", Font.BOLD, 10));
        for (int i = 0; i <= n; i++) {
            int track = (i == 0) ? chartHead : chartSteps.get(i - 1).to;
            boolean isBoundary = i > 0 && "END".equals(chartSteps.get(i - 1).id);

            int x = margin + (int) Math.round(((double) (track - min) / (max - min)) * span);
            int y = top + (int) Math.round(((double) i / n) * (bot - top));

            g2.setColor(i == 0 ? new Color(245, 158, 11) : (isBoundary ? new Color(239, 68, 68) : new Color(16, 185, 129)));
            g2.fillOval(x - 4, y - 4, 8, 8);
            g2.setColor(new Color(30, 41, 59));
            g2.drawOval(x - 4, y - 4, 8, 8);

            String txt = String.valueOf(track);
            int tw = g2.getFontMetrics().stringWidth(txt);
            g2.drawString(txt, Math.min(Math.max(x - tw / 2, 4), Math.max(w - tw - 4, 4)), Math.max(y - 6, 12));
        }
    }
}
