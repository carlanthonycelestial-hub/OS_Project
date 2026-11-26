package src.PriorityAlgo;

import src.Gui.GanttChartPanel;
import src.Gui.MainApp;
import src.SJFAlgo.SJFScheduler;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Sample problems for the Priority module, kept in one place so the
 * Sample window and the "Load Sample Data" button always use the same values.
 */
public final class PrioritySample extends JPanel {
    private static final Font BOLD_13 = new Font("SansSerif", Font.BOLD, 13);
    private static final String[] AVG_NAMES = { "Completion", "Turnaround", "Waiting" };

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[] { "PID", "AT", "BT", "PRIO", "CT", "TAT", "WT" }, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JLabel descLabel = new JLabel("", SwingConstants.CENTER);
    private final JLabel[] avgLabels = { new JLabel(), new JLabel(), new JLabel() };
    private final GanttChartPanel gantt = new GanttChartPanel("Select a sample above.");
    private final JButton sample1Btn = new JButton("Sample 1: Idle Gap & Ties (10 Processes)");
    private final JButton sample2Btn = new JButton("Sample 2: All Arrive at 0 (5 Processes)");

    public PrioritySample(MainApp mainApp) {
        setLayout(new BorderLayout(12, 12));
        setBorder(new EmptyBorder(15, 20, 15, 20));
        setBackground(new Color(248, 250, 252));

        JButton backBtn = new JButton("\u2190 Back to Main Menu");
        backBtn.setFont(BOLD_13);
        backBtn.addActionListener(e -> mainApp.showCard(MainApp.MENU_CARD));

        JButton openSimBtn = new JButton("Open Simulator \u2192");
        openSimBtn.setFont(BOLD_13);
        openSimBtn.addActionListener(e -> mainApp.showCard(MainApp.PRIORITY_CARD));

        JLabel header = new JLabel("Priority (Preemptive) - Sample Problems", SwingConstants.CENTER);
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

        List<PriorityScheduler.Job> jobs = first ? withIdleGapAndTies() : allArriveAtZero();
        descLabel.setText(first
                ? "10 processes with an idle gap (10-12), priority ties broken by arrival, and same-arrival ties broken by row order."
                : "5 processes all arriving at time 0 (P1-P5), ordered purely by priority (lower number = higher priority).");

        List<SJFScheduler.Block> blocks = PriorityScheduler.schedule(jobs);
        tableModel.setRowCount(0);
        for (PriorityScheduler.Job j : jobs) {
            tableModel.addRow(new Object[] {
                    j.pid, j.arrival, j.burst, j.priority, j.completion, j.turnaround, j.waiting
            });
        }
        double[] avg = PriorityScheduler.averages(jobs);
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

    /**
     * Priority sample with 10 processes showing:
     * - priority ties broken by arrival: P2 before P3 (both priority 1), P6 before
     * P10 (both priority 2);
     * - an idle gap: nothing is ready from 10 to 13;
     * - a priority + arrival tie broken by row order/PID: P5 and P6 both arrive at
     * 13 with priority 2.
     *
     * @return a new list each call, so callers can change it freely
     */
    public static List<PriorityScheduler.Job> withIdleGapAndTies() {
        List<PriorityScheduler.Job> processes = new ArrayList<>();
        // PID AT BT Prio Index
        processes.add(new PriorityScheduler.Job("P1", 0, 3, 3, 0));
        processes.add(new PriorityScheduler.Job("P2", 1, 4, 1, 1));
        processes.add(new PriorityScheduler.Job("P3", 2, 2, 1, 2));
        processes.add(new PriorityScheduler.Job("P4", 3, 1, 2, 3));
        processes.add(new PriorityScheduler.Job("P5", 13, 3, 2, 4));
        processes.add(new PriorityScheduler.Job("P6", 13, 2, 2, 5));
        processes.add(new PriorityScheduler.Job("P7", 14, 2, 0, 6));
        processes.add(new PriorityScheduler.Job("P8", 15, 1, 4, 7));
        processes.add(new PriorityScheduler.Job("P9", 20, 2, 3, 8));
        processes.add(new PriorityScheduler.Job("P10", 16, 3, 2, 9));
        return processes;
    }

    /**
     * Second Priority sample: the textbook problem where all five processes
     * arrive at time 0 (burst times in milliseconds). Shown in the menu's Sample
     * window.
     *
     * Expected Gantt chart: P2 0-1 | P5 1-6 | P1 6-16 | P3 16-18 | P4 18-19
     * Average waiting time = 41 / 5 = 8.20; average turnaround time = 60 / 5 =
     * 12.00
     *
     * @return a new list each call, so callers can change it freely
     */
    public static List<PriorityScheduler.Job> allArriveAtZero() {
        List<PriorityScheduler.Job> processes = new ArrayList<>();
        // PID AT BT Prio Index
        processes.add(new PriorityScheduler.Job("P1", 0, 10, 3, 0));
        processes.add(new PriorityScheduler.Job("P2", 0, 1, 1, 1));
        processes.add(new PriorityScheduler.Job("P3", 0, 2, 4, 2));
        processes.add(new PriorityScheduler.Job("P4", 0, 1, 5, 3));
        processes.add(new PriorityScheduler.Job("P5", 0, 5, 2, 4));
        return processes;
    }
}
