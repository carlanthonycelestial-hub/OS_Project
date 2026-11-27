package src.MultipleLevelAlgo;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

import src.Gui.GanttChartPanel;
import src.Gui.MainApp;
import src.MultipleLevelAlgo.MultiLevelScheduler.Result;
import src.Scheduler.CPUScheduler.Job;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/** Multilevel Queue screen: Queue 1 = Round Robin, Queue 2 = FCFS. */
public class MultiLevel extends JPanel {
    private static final int MAX_PROCESSES = 20, MAX_TIME = 10000, DEFAULT_ROWS = 5;
    private static final int COL_AT = 1, COL_BT = 2, COL_QUEUE = 3;
    private static final Font BOLD = new Font("SansSerif", Font.BOLD, 13);
    private static final String[] STAT_NAMES = { "Avg. Completion Time", "Avg. Turnaround Time",
            "Avg. Waiting Time", "Avg. Response Time", "CPU Utilization", "Throughput" };

    private final DefaultTableModel inputModel = new DefaultTableModel(
            new String[] { "PID", "Arrival Time", "Burst Time", "Queue (1=RR, 2=FCFS)" }, 0) {
        @Override
        public boolean isCellEditable(int row, int col) {
            return col != 0; // PID is auto-generated
        }
    };
    private final DefaultTableModel outputModel = new DefaultTableModel(
            new String[] { "PID", "Queue", "AT", "BT", "CT", "TAT", "WT", "RT" }, 0) {
        @Override
        public boolean isCellEditable(int row, int col) {
            return false;
        }
    };
    private final JTable inputTable = new JTable(inputModel);
    private final JSpinner countSpinner = new JSpinner(new SpinnerNumberModel(DEFAULT_ROWS, 1, MAX_PROCESSES, 1));
    private final JTextField quantumField = new JTextField("4", 4);
    private final GanttChartPanel gantt = new GanttChartPanel("Run 'Compute MLQ' to generate the Gantt chart.");
    private final JLabel[] statLabels = new JLabel[STAT_NAMES.length];

    /** Validation failure; row = -1 means the quantum field. */
    private static final class InvalidInput extends Exception {
        final int row, col;

        InvalidInput(int row, int col, String message) {
            super(message);
            this.row = row;
            this.col = col;
        }
    }

    public MultiLevel(MainApp mainApp) {
        setLayout(new BorderLayout(12, 12));
        setBorder(new EmptyBorder(15, 20, 15, 20));

        // ---- top bar ----
        JLabel title = new JLabel("Multilevel Queue (MLQ) CPU Scheduling", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        JPanel top = new JPanel(new BorderLayout());
        top.add(button("\u2190 Back to Main Menu", () -> mainApp.showCard(MainApp.MENU_CARD)), BorderLayout.WEST);
        top.add(title, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        // ---- left: input ----
        inputTable.setRowHeight(25);
        inputTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        // The queue can only be 1 or 2, so offer a drop-down instead of free text.
        inputTable.getColumnModel().getColumn(COL_QUEUE)
                .setCellEditor(new DefaultCellEditor(new JComboBox<>(new String[] { "1", "2" })));
        countSpinner.addChangeListener(e -> syncRows());
        syncRows();

        JPanel settings = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        settings.add(new JLabel("No. of processes:"));
        settings.add(countSpinner);
        settings.add(new JLabel("   Q1 (RR) Time Quantum:"));
        settings.add(quantumField);

        JButton compute = button("Compute MLQ", this::runMultiLevel);
        compute.setBackground(new Color(37, 99, 235));
        compute.setForeground(Color.WHITE);
        quantumField.addActionListener(e -> runMultiLevel()); // Enter in the quantum field runs it
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 4));
        actions.add(button("Load Sample Data", this::loadSampleData));
        actions.add(button("Reset / Clear", this::resetAll));
        actions.add(compute);

        JPanel controls = new JPanel(new GridLayout(2, 1));
        controls.add(settings);
        controls.add(actions);

        JPanel left = new JPanel(new BorderLayout(8, 8));
        left.setBorder(BorderFactory.createTitledBorder("1. Process Input (max " + MAX_PROCESSES + ")"));
        left.add(new JScrollPane(inputTable), BorderLayout.CENTER);
        left.add(controls, BorderLayout.SOUTH);

        // ---- right: results ----
        JTable outputTable = new JTable(outputModel);
        outputTable.setRowHeight(25);
        JPanel stats = new JPanel(new GridLayout(3, 2, 10, 6));
        stats.setBorder(new EmptyBorder(8, 10, 8, 10));
        for (int i = 0; i < statLabels.length; i++) {
            statLabels[i] = new JLabel();
            statLabels[i].setFont(BOLD);
            stats.add(statLabels[i]);
        }
        showStats(null);

        JPanel right = new JPanel(new BorderLayout(8, 8));
        right.setBorder(BorderFactory.createTitledBorder("2. Process Metrics Table"));
        right.add(new JScrollPane(outputTable), BorderLayout.CENTER);
        right.add(stats, BorderLayout.SOUTH);

        JPanel center = new JPanel(new GridLayout(1, 2, 15, 0));
        center.add(left);
        center.add(right);
        add(center, BorderLayout.CENTER);

        // ---- bottom: Gantt ----
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBorder(BorderFactory.createTitledBorder("3. Gantt Chart (Time Scale)"));
        bottom.setPreferredSize(new Dimension(0, 98));
        bottom.add(gantt, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
    }

    // ---------- small UI helpers ----------

    private static JButton button(String text, Runnable action) {
        JButton b = new JButton(text);
        b.setFont(BOLD);
        b.addActionListener(e -> action.run());
        return b;
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Input Validation Error", JOptionPane.ERROR_MESSAGE);
    }

    private void showStats(Result r) {
        String[] values = r == null ? new String[STAT_NAMES.length] : new String[] {
                String.format("%.2f ms", r.avgCompletion), String.format("%.2f ms", r.avgTurnaround),
                String.format("%.2f ms", r.avgWaiting), String.format("%.2f ms", r.avgResponse),
                String.format("%.2f %%", r.utilization), String.format("%.4f proc/ms", r.throughput) };
        for (int i = 0; i < statLabels.length; i++)
            statLabels[i].setText(STAT_NAMES[i] + ": " + (values[i] == null ? "-" : values[i]));
    }

    private void stopEditing() {
        if (inputTable.isEditing())
            inputTable.getCellEditor().stopCellEditing();
    }

    /** Makes the table have exactly as many rows as the spinner says (existing rows are kept). */
    private void syncRows() {
        stopEditing();
        int n = (Integer) countSpinner.getValue();
        while (inputModel.getRowCount() < n)
            inputModel.addRow(new Object[] { "P" + (inputModel.getRowCount() + 1), "", "", "1" });
        while (inputModel.getRowCount() > n)
            inputModel.removeRow(inputModel.getRowCount() - 1);
    }

    private void loadSampleData() {
        String[][] sample = { // AT, BT, Queue
                { "0", "4", "1" }, { "0", "3", "1" }, { "0", "8", "2" }, { "10", "4", "1" }, { "12", "5", "2" } };
        stopEditing();
        quantumField.setText("4");
        countSpinner.setValue(sample.length);
        for (int r = 0; r < sample.length; r++)
            for (int c = 0; c < 3; c++)
                inputModel.setValueAt(sample[r][c], r, c + 1);
    }

    private void resetAll() {
        stopEditing();
        quantumField.setText("4");
        countSpinner.setValue(DEFAULT_ROWS);
        for (int r = 0; r < inputModel.getRowCount(); r++) {
            inputModel.setValueAt("", r, COL_AT);
            inputModel.setValueAt("", r, COL_BT);
            inputModel.setValueAt("1", r, COL_QUEUE);
        }
        outputModel.setRowCount(0);
        gantt.setBlocks(new ArrayList<>());
        showStats(null);
    }

    // ---------- run ----------

    private void runMultiLevel() {
        stopEditing();
        try {
            int quantum = readInt(quantumField.getText(), 1, 1000, -1, -1, "Time Quantum");
            List<Job> jobs = readJobs();
            Result res = MultiLevelScheduler.run(jobs, quantum);

            outputModel.setRowCount(0);
            for (Job j : jobs) {
                outputModel.addRow(new Object[] { j.pid, j.queue == 1 ? "Q1 (RR)" : "Q2 (FCFS)", j.arrival, j.burst,
                        j.completion, j.turnaround, j.waiting, res.response.get(j) });
            }
            showStats(res);
            gantt.setBlocks(res.blocks);
        } catch (InvalidInput ex) {
            if (ex.row >= 0) {
                inputTable.changeSelection(ex.row, ex.col, false, false);
                inputTable.requestFocusInWindow();
            } else {
                quantumField.requestFocusInWindow();
            }
            showError(ex.getMessage());
        }
    }

    private List<Job> readJobs() throws InvalidInput {
        List<Job> jobs = new ArrayList<>();
        for (int i = 0; i < inputModel.getRowCount(); i++) {
            String row = "Row " + (i + 1);
            int at = readInt(cellText(i, COL_AT), 0, MAX_TIME, i, COL_AT, row + " Arrival Time");
            int bt = readInt(cellText(i, COL_BT), 1, MAX_TIME, i, COL_BT, row + " Burst Time");
            int q = readInt(cellText(i, COL_QUEUE), 1, 2, i, COL_QUEUE, row + " Queue (1 = RR, 2 = FCFS)");
            jobs.add(new Job("P" + (i + 1), at, bt, 0, q, i));
        }
        return jobs;
    }

    private String cellText(int row, int col) {
        Object v = inputModel.getValueAt(row, col);
        return v == null ? "" : v.toString().trim();
    }

    /** One parser for every numeric field: whole number in [min, max] or an InvalidInput. */
    private static int readInt(String text, int min, int max, int row, int col, String label) throws InvalidInput {
        text = text.trim();
        if (text.isEmpty())
            throw new InvalidInput(row, col, label + " is empty.");
        try {
            int val = Integer.parseInt(text);
            if (val >= min && val <= max)
                return val;
        } catch (NumberFormatException ignored) {
        }
        throw new InvalidInput(row, col, label + " must be a whole number between " + min + " and " + max + ".");
    }
}
