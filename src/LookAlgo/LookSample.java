package src.LookAlgo;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;

import src.Gui.MainApp;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Sample problems for the LOOK Disk Scheduling module, kept in one place so the
 * Sample window and the "Load Sample Data" button always use the same values.
 */
public final class LookSample extends JPanel {
    private static final Font BOLD_13 = new Font("SansSerif", Font.BOLD, 13);

    public static final class Dataset {
        public final int initialHead;
        public final boolean movingRight;
        public final List<LookScheduler.Request> requests;

        Dataset(int initialHead, boolean movingRight, int... tracks) {
            this.initialHead = initialHead;
            this.movingRight = movingRight;
            this.requests = new ArrayList<>(tracks.length);
            for (int i = 0; i < tracks.length; i++) {
                this.requests.add(new LookScheduler.Request("R" + (i + 1), tracks[i], i));
            }
        }
    }

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[] { "Step", "Req ID", "From", "To", "Seek" }, 0) {
        @Override
        public boolean isCellEditable(int r, int c) {
            return false;
        }
    };
    private final JLabel descLabel = new JLabel("", SwingConstants.CENTER);
    private final JLabel totalLbl = new JLabel("", SwingConstants.CENTER);
    private final JLabel avgLbl = new JLabel("", SwingConstants.CENTER);
    private final JLabel seqLbl = new JLabel("", SwingConstants.CENTER);
    private final JButton sample1Btn = new JButton("Sample 1: Moving Right (Head 53)");
    private final JButton sample2Btn = new JButton("Sample 2: Moving Left (Head 50)");

    private int chartHead;
    private List<LookScheduler.Step> chartSteps = new ArrayList<>();
    private final JPanel chartPanel = new JPanel() {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            drawChart((Graphics2D) g, getWidth(), getHeight());
        }
    };

    public LookSample(MainApp mainApp) {
        setLayout(new BorderLayout(12, 12));
        setBorder(new EmptyBorder(15, 20, 15, 20));
        setBackground(new Color(248, 250, 252));

        JButton backBtn = new JButton("\u2190 Back to Main Menu");
        backBtn.setFont(BOLD_13);
        backBtn.addActionListener(e -> mainApp.showCard(MainApp.MENU_CARD));

        JButton openSimBtn = new JButton("Open Simulator \u2192");
        openSimBtn.setFont(BOLD_13);
        openSimBtn.addActionListener(e -> mainApp.showCard(MainApp.LOOK_CARD));

        JLabel header = new JLabel("LOOK Disk Scheduling - Sample Problems", SwingConstants.CENTER);
        header.setFont(new Font("SansSerif", Font.BOLD, 20));
        header.setForeground(new Color(15, 23, 42));

        JPanel topBar = new JPanel(new BorderLayout(10, 0));
        topBar.setOpaque(false);
        topBar.add(backBtn, BorderLayout.WEST);
        topBar.add(header, BorderLayout.CENTER);
        topBar.add(openSimBtn, BorderLayout.EAST);

        sample1Btn.setFont(BOLD_13);
        sample2Btn.setFont(BOLD_13);
        sample1Btn.setFocusPainted(false);
        sample2Btn.setFocusPainted(false);
        sample1Btn.addActionListener(e -> showSample(1));
        sample2Btn.addActionListener(e -> showSample(2));

        JPanel switcher = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 4));
        switcher.setOpaque(false);
        switcher.add(sample1Btn);
        switcher.add(sample2Btn);

        descLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        descLabel.setForeground(new Color(71, 85, 105));

        JPanel northPanel = new JPanel(new BorderLayout(0, 6));
        northPanel.setOpaque(false);
        northPanel.add(topBar, BorderLayout.NORTH);
        northPanel.add(switcher, BorderLayout.CENTER);
        northPanel.add(descLabel, BorderLayout.SOUTH);
        add(northPanel, BorderLayout.NORTH);

        JTable table = new JTable(tableModel);
        table.setRowHeight(25);

        JPanel stats = new JPanel(new GridLayout(1, 3, 12, 0));
        stats.setBackground(new Color(241, 245, 249));
        stats.setBorder(new EmptyBorder(10, 12, 10, 12));
        for (JLabel label : new JLabel[] { totalLbl, avgLbl, seqLbl }) {
            label.setFont(BOLD_13);
            stats.add(label);
        }

        JPanel center = new JPanel(new BorderLayout(8, 8));
        center.setBackground(Color.WHITE);
        center.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(203, 213, 225)),
                "Sample Seek Sequence & Metrics", TitledBorder.LEFT, TitledBorder.TOP, BOLD_13));
        center.add(new JScrollPane(table), BorderLayout.CENTER);
        center.add(stats, BorderLayout.SOUTH);
        add(center, BorderLayout.CENTER);

        chartPanel.setBackground(Color.WHITE);
        JPanel bottom = new JPanel(new BorderLayout(8, 8));
        bottom.setBackground(Color.WHITE);
        bottom.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(203, 213, 225)),
                "Disk Head Movement (Track Scale)", TitledBorder.LEFT, TitledBorder.TOP, BOLD_13));
        bottom.setPreferredSize(new Dimension(0, 130));
        bottom.add(chartPanel, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        showSample(1);
    }

    private void showSample(int which) {
        boolean first = (which == 1);
        styleTabButton(sample1Btn, first);
        styleTabButton(sample2Btn, !first);

        Dataset data = first ? classicRight() : movingLeftExample();
        descLabel.setText(first
                ? "Head starts at 53 moving Right; reverses at the furthest request (183) without going to a disk boundary."
                : "Head starts at 50 moving Left; reverses at the lowest request (16) and services remaining tracks ascending.");

        List<LookScheduler.Step> steps = LookScheduler.schedule(data.initialHead, data.movingRight, data.requests);
        tableModel.setRowCount(0);
        StringBuilder seq = new StringBuilder().append(data.initialHead);
        for (LookScheduler.Step s : steps) {
            tableModel.addRow(new Object[] { s.order, s.id, s.from, s.to, s.distance });
            seq.append(" \u2192 ").append(s.to);
        }
        totalLbl.setText("Total Head Movement: " + LookScheduler.totalSeek(steps) + " tracks");
        avgLbl.setText("Avg Seek: " + String.format("%.2f tracks", LookScheduler.averageSeek(steps)));
        seqLbl.setText("Seq: " + seq);
        seqLbl.setToolTipText(seq.toString());

        chartHead = data.initialHead;
        chartSteps = steps;
        chartPanel.repaint();
    }

    private void drawChart(Graphics2D g2, int w, int h) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (chartSteps.isEmpty())
            return;
        int min = chartHead, max = chartHead, margin = 28, n = chartSteps.size();
        for (LookScheduler.Step s : chartSteps) {
            min = Math.min(min, Math.min(s.from, s.to));
            max = Math.max(max, Math.max(s.from, s.to));
        }
        if (min == max)
            max = min + 1;
        int span = Math.max(w - 2 * margin, 100), top = 24, bot = Math.max(h - 18, top + 20);

        g2.setColor(new Color(148, 163, 184));
        g2.drawLine(margin, 16, margin + span, 16);
        g2.setColor(new Color(37, 99, 235));
        g2.setStroke(new BasicStroke(2f));
        for (int i = 0; i < n; i++) {
            LookScheduler.Step s = chartSteps.get(i);
            int x1 = margin + (int) Math.round(((double) (s.from - min) / (max - min)) * span);
            int x2 = margin + (int) Math.round(((double) (s.to - min) / (max - min)) * span);
            int y1 = top + (int) Math.round(((double) i / n) * (bot - top));
            int y2 = top + (int) Math.round(((double) (i + 1) / n) * (bot - top));
            g2.drawLine(x1, y1, x2, y2);
        }
        g2.setFont(new Font("SansSerif", Font.BOLD, 10));
        for (int i = 0; i <= n; i++) {
            int track = (i == 0) ? chartHead : chartSteps.get(i - 1).to;
            int x = margin + (int) Math.round(((double) (track - min) / (max - min)) * span);
            int y = top + (int) Math.round(((double) i / n) * (bot - top));
            g2.setColor(i == 0 ? new Color(245, 158, 11) : new Color(16, 185, 129));
            g2.fillOval(x - 4, y - 4, 8, 8);
            g2.setColor(new Color(30, 41, 59));
            g2.drawOval(x - 4, y - 4, 8, 8);
            String txt = String.valueOf(track);
            int tw = g2.getFontMetrics().stringWidth(txt);
            g2.drawString(txt, Math.min(Math.max(x - tw / 2, 4), Math.max(w - tw - 4, 4)), Math.max(y - 6, 12));
        }
    }

    private static void styleTabButton(JButton btn, boolean active) {
        if (active) {
            btn.setBackground(new Color(37, 99, 235));
            btn.setForeground(Color.WHITE);
        } else {
            btn.setBackground(new Color(241, 245, 249));
            btn.setForeground(new Color(30, 41, 59));
        }
    }

    public static Dataset classicRight() {
        return new Dataset(53, true, 98, 183, 37, 122, 14, 124, 65, 67);
    }

    public static Dataset movingLeftExample() {
        return new Dataset(50, false, 82, 170, 43, 140, 24, 16, 190);
    }
}
