package src.SJFAlgo;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;

import src.Gui.GanttChartPanel;
import src.Gui.MainApp;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Sample problems for the Shortest Job First (SJF) module, kept in one place so
 * the
 * Sample window and the "Load Sample Data" button always use the same values.
 */
public final class SJFSample extends JPanel {
    private static final Font BOLD_13 = new Font("SansSerif", Font.BOLD, 13);
    private static final String[] AVG_NAMES = { "Completion", "Turnaround", "Waiting" };

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[] { "PID", "AT", "BT", "CT", "TAT", "WT" }, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JLabel descLabel = new JLabel("", SwingConstants.CENTER);
    private final JLabel[] avgLabels = { new JLabel(), new JLabel(), new JLabel() };
    private final GanttChartPanel gantt = new GanttChartPanel("Select a sample above.");
    private final JButton sample1Btn = new JButton("Sample 1: Staggered Arrivals (5 Processes)");
    private final JButton sample2Btn = new JButton("Sample 2: All Arrive at 0 (4 Processes)");

    public SJFSample(MainApp mainApp) {
        setLayout(new BorderLayout(12, 12));
        setBorder(new EmptyBorder(15, 20, 15, 20));
        setBackground(new Color(248, 250, 252));

        JButton backBtn = new JButton("\u2190 Back to Main Menu");
        backBtn.setFont(BOLD_13);
        backBtn.addActionListener(e -> mainApp.showCard(MainApp.MENU_CARD));

        JButton openSimBtn = new JButton("Open Simulator \u2192");
        openSimBtn.setFont(BOLD_13);
        openSimBtn.addActionListener(e -> mainApp.showCard(MainApp.SJF_CARD));

        JLabel header = new JLabel("Shortest Job First (SJF) - Sample Problems", SwingConstants.CENTER);
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
        for (JLabel label : avgLabels) {
            label.setFont(BOLD_13);
            label.setHorizontalAlignment(SwingConstants.CENTER);
            stats.add(label);
        }

        JPanel center = new JPanel(new BorderLayout(8, 8));
        center.setBackground(Color.WHITE);
        center.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(203, 213, 225)),
                "Sample Output & Metrics", TitledBorder.LEFT, TitledBorder.TOP, BOLD_13));
        center.add(new JScrollPane(table), BorderLayout.CENTER);
        center.add(stats, BorderLayout.SOUTH);
        add(center, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(8, 8));
        bottom.setBackground(Color.WHITE);
        bottom.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(203, 213, 225)),
                "Gantt Chart (Time Scale)", TitledBorder.LEFT, TitledBorder.TOP, BOLD_13));
        bottom.setPreferredSize(new Dimension(0, 102));
        bottom.add(gantt, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        showSample(1);
    }

    private void showSample(int which) {
        boolean first = (which == 1);
        styleTabButton(sample1Btn, first);
        styleTabButton(sample2Btn, !first);

        List<SJFScheduler.Job> jobs = first ? staggeredArrivals() : allArriveAtZero();
        descLabel.setText(first
                ? "5 processes with staggered arrivals and an initial IDLE gap (0-1); shortest available burst runs next."
                : "4 processes all arriving at time 0 (P4=3ms, P1=6ms, P3=7ms, P2=8ms); Avg WT = 7.00 ms, Avg TAT = 13.00 ms.");

        List<SJFScheduler.Block> blocks = SJFScheduler.schedule(jobs);
        tableModel.setRowCount(0);
        for (SJFScheduler.Job j : jobs) {
            tableModel.addRow(new Object[] { j.pid, j.arrival, j.burst, j.completion, j.turnaround, j.waiting });
        }
        double[] avg = SJFScheduler.averages(jobs);
        for (int i = 0; i < avgLabels.length; i++) {
            avgLabels[i].setText("Avg " + AVG_NAMES[i] + ": " + String.format("%.2f ms", avg[i]));
        }
        gantt.setBlocks(blocks);
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

    public static List<SJFScheduler.Job> staggeredArrivals() {
        List<SJFScheduler.Job> jobs = new ArrayList<>();
        // PID AT BT Index
        jobs.add(new SJFScheduler.Job("P1", 1, 7, 0));
        jobs.add(new SJFScheduler.Job("P2", 3, 3, 1));
        jobs.add(new SJFScheduler.Job("P3", 6, 2, 2));
        jobs.add(new SJFScheduler.Job("P4", 7, 10, 3));
        jobs.add(new SJFScheduler.Job("P5", 9, 8, 4));
        return jobs;
    }

    public static List<SJFScheduler.Job> allArriveAtZero() {
        List<SJFScheduler.Job> jobs = new ArrayList<>();
        // PID AT BT Index
        jobs.add(new SJFScheduler.Job("P1", 0, 6, 0));
        jobs.add(new SJFScheduler.Job("P2", 0, 8, 1));
        jobs.add(new SJFScheduler.Job("P3", 0, 7, 2));
        jobs.add(new SJFScheduler.Job("P4", 0, 3, 3));
        return jobs;
    }
}
