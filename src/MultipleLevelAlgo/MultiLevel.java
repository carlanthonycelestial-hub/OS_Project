package src.MultipleLevelAlgo;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;

import src.Gui.GanttChartPanel;
import src.Gui.MainApp;
import src.SJFAlgo.SJFScheduler.Block;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MultiLevel extends JPanel {
    private static final int MAX_PROCESSES = 20;
    private static final int MAX_TIME = 10000;
    private static final int DEFAULT_ROWS = 5;
    private static final int COL_PID = 0, COL_AT = 1, COL_BT = 2, COL_QUEUE = 3;
    private static final Font BOLD_13 = new Font("SansSerif", Font.BOLD, 13);
    private static final String[] AVG_NAMES = { "Completion", "Turnaround", "Waiting" };

    private final DefaultTableModel inputModel = new DefaultTableModel(
            new String[] { "PID", "Arrival Time", "Burst Time", "Queue (1=RR, 2=FCFS)" }, 0);
    private final DefaultTableModel outputModel = new DefaultTableModel(
            new String[] { "PID", "Queue", "AT", "BT", "CT", "TAT", "WT" }, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable inputTable = new JTable(inputModel);
    private final JTextField quantumField = new JTextField("4", 4);
    private final GanttChartPanel gantt = new GanttChartPanel("Run 'Compute MLQ' to generate the Gantt chart.");
    private final JLabel[] avgLabels = { new JLabel(), new JLabel(), new JLabel() };

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
        setBackground(new Color(248, 250, 252));

        JLabel header = new JLabel("Multilevel Queue (MLQ) CPU Scheduling", SwingConstants.CENTER);
        header.setFont(new Font("SansSerif", Font.BOLD, 20));
        header.setForeground(new Color(15, 23, 42));
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);
        topBar.add(button("\u2190 Back to Main Menu", () -> mainApp.showCard(MainApp.MENU_CARD)), BorderLayout.WEST);
        topBar.add(header, BorderLayout.CENTER);
        add(topBar, BorderLayout.NORTH);

        inputTable.setRowHeight(25);
        inputTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        addRows(DEFAULT_ROWS);

        JPanel quantumBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        quantumBar.setOpaque(false);
        JLabel qLbl = new JLabel("Q1 (RR) Time Quantum: ");
        qLbl.setFont(BOLD_13);
        quantumBar.add(qLbl);
        quantumBar.add(quantumField);

        JPanel rowButtons = flowPanel(8, 2,
                button("+ Add Process", this::addSingleRow),
                button("- Remove Selected", this::removeSelectedRow),
                button("Add Multiple", this::promptAddMultipleRows));
        JButton computeBtn = button("Compute MLQ", this::runMultiLevel);
        computeBtn.setBackground(new Color(37, 99, 235));
        computeBtn.setForeground(Color.WHITE);
        JPanel actionButtons = flowPanel(10, 4,
                button("Load Sample Data", this::loadSampleData),
                button("Reset / Clear", this::resetAll),
                computeBtn);
        JPanel controls = new JPanel(new GridLayout(3, 1, 4, 4));
        controls.setOpaque(false);
        controls.add(quantumBar);
        controls.add(rowButtons);
        controls.add(actionButtons);

        JPanel left = titledPanel("1. Process Input (max " + MAX_PROCESSES + ")");
        left.add(new JScrollPane(inputTable), BorderLayout.CENTER);
        left.add(controls, BorderLayout.SOUTH);

        JTable outputTable = new JTable(outputModel);
        outputTable.setRowHeight(25);
        JPanel stats = new JPanel(new GridLayout(3, 1, 6, 6));
        stats.setBackground(new Color(241, 245, 249));
        stats.setBorder(new EmptyBorder(10, 12, 10, 12));
        for (JLabel label : avgLabels) {
            label.setFont(BOLD_13);
            stats.add(label);
        }
        showAverages(null);

        JPanel right = titledPanel("2. Process Metrics Table");
        right.add(new JScrollPane(outputTable), BorderLayout.CENTER);
        right.add(stats, BorderLayout.SOUTH);

        JPanel center = new JPanel(new GridLayout(1, 2, 15, 0));
        center.setOpaque(false);
        center.add(left);
        center.add(right);
        add(center, BorderLayout.CENTER);

        JPanel bottom = titledPanel("3. Gantt Chart (Time Scale)");
        bottom.setPreferredSize(new Dimension(0, 98));
        bottom.add(gantt, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
    }

    private static JButton button(String text, Runnable action) {
        JButton b = new JButton(text);
        b.setFont(BOLD_13);
        b.addActionListener(e -> action.run());
        return b;
    }

    private static JPanel flowPanel(int hgap, int vgap, JComponent... items) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, hgap, vgap));
        p.setOpaque(false);
        for (JComponent c : items)
            p.add(c);
        return p;
    }

    private static JPanel titledPanel(String title) {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(203, 213, 225)),
                title, TitledBorder.LEFT, TitledBorder.TOP, BOLD_13));
        return p;
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Input Validation Error", JOptionPane.ERROR_MESSAGE);
    }

    private void showAverages(double[] avg) {
        for (int i = 0; i < avgLabels.length; i++) {
            avgLabels[i].setText("Average " + AVG_NAMES[i] + " Time: "
                    + (avg == null ? "-" : String.format("%.2f", avg[i])) + "ms");
        }
    }

    private void stopEditing() {
        if (inputTable.isEditing())
            inputTable.getCellEditor().stopCellEditing();
    }

    private void selectCell(int row, int col) {
        inputTable.changeSelection(row, col, false, false);
        inputTable.scrollRectToVisible(inputTable.getCellRect(row, col, true));
    }

    private int nextPidNumber() {
        int max = 0;
        for (int i = 0; i < inputModel.getRowCount(); i++) {
            String pid = cell(i, COL_PID);
            if (pid.matches("^[Pp]\\d{1,9}$"))
                max = Math.max(max, Integer.parseInt(pid.substring(1)));
        }
        return max + 1;
    }

    private void addRows(int count) {
        int start = nextPidNumber();
        for (int i = 0; i < count; i++) {
            inputModel.addRow(new Object[] { "P" + (start + i), "", "", "1" });
        }
    }

    private void addSingleRow() {
        stopEditing();
        if (inputModel.getRowCount() >= MAX_PROCESSES) {
            showError("The maximum number of processes is " + MAX_PROCESSES + ".");
            return;
        }
        addRows(1);
        selectCell(inputModel.getRowCount() - 1, COL_PID);
    }

    private void removeSelectedRow() {
        stopEditing();
        int row = inputTable.getSelectedRow();
        if (row == -1) {
            showError("Please select a row in the table first.");
            return;
        }
        inputModel.removeRow(row);
    }

    private void promptAddMultipleRows() {
        stopEditing();
        int remaining = MAX_PROCESSES - inputModel.getRowCount();
        if (remaining <= 0) {
            addSingleRow();
            return;
        }
        String in = JOptionPane.showInputDialog(this,
                "How many processes would you like to add? (1-" + remaining + ")",
                "Add Multiple Processes", JOptionPane.QUESTION_MESSAGE);
        if (in == null)
            return;
        try {
            int count = Integer.parseInt(in.trim());
            if (count < 1 || count > remaining) {
                showError("Please enter a number between 1 and " + remaining + ".");
                return;
            }
            addRows(count);
            selectCell(inputModel.getRowCount() - 1, COL_PID);
        } catch (NumberFormatException ex) {
            showError("Invalid input. Please enter a whole number.");
        }
    }

    private void loadSampleData() {
        stopEditing();
        inputModel.setRowCount(0);
        quantumField.setText("4");
        // Sample: Mix of Queue 1 (Round Robin) and Queue 2 (FCFS)
        String[][] sample = {
                { "P1", "0", "4", "1" },
                { "P2", "0", "3", "1" },
                { "P3", "0", "8", "2" },
                { "P4", "10", "4", "1" },
                { "P5", "12", "5", "2" }
        };
        for (String[] row : sample) {
            inputModel.addRow(row);
        }
    }

    private void resetAll() {
        stopEditing();
        inputModel.setRowCount(0);
        quantumField.setText("4");
        addRows(DEFAULT_ROWS);
        outputModel.setRowCount(0);
        gantt.setBlocks(new ArrayList<>());
        showAverages(null);
    }

    private void runMultiLevel() {
        stopEditing();
        try {
            int tq = parseQuantum();
            List<MultiLevelScheduler.Job> jobs = readInputs();
            List<Block> blocks = MultiLevelScheduler.schedule(jobs, tq);

            outputModel.setRowCount(0);
            for (MultiLevelScheduler.Job j : jobs) {
                outputModel.addRow(new Object[] {
                        j.pid,
                        "Q" + j.queue + (j.queue == 1 ? " (RR)" : " (FCFS)"),
                        j.arrival,
                        j.burst,
                        j.completion,
                        j.turnaround,
                        j.waiting
                });
            }

            showAverages(MultiLevelScheduler.averages(jobs));
            gantt.setBlocks(blocks);
        } catch (InvalidInput ex) {
            if (ex.row >= 0) {
                selectCell(ex.row, ex.col);
                inputTable.requestFocusInWindow();
            } else {
                quantumField.requestFocusInWindow();
            }
            showError(ex.getMessage());
        }
    }

    private int parseQuantum() throws InvalidInput {
        String text = quantumField.getText().trim();
        if (text.isEmpty()) {
            throw new InvalidInput(-1, -1, "Time Quantum cannot be empty.");
        }
        try {
            int val = Integer.parseInt(text);
            if (val > 0 && val <= 1000) return val;
        } catch (NumberFormatException ignored) {}
        throw new InvalidInput(-1, -1, "Time Quantum must be a whole number between 1 and 1000.");
    }

    private String cell(int r, int c) {
        Object v = inputModel.getValueAt(r, c);
        return v == null ? "" : v.toString().trim();
    }

    private List<MultiLevelScheduler.Job> readInputs() throws InvalidInput {
        int rows = inputModel.getRowCount();
        if (rows == 0 || rows > MAX_PROCESSES)
            throw new InvalidInput(-1, -1, "Number of processes must be between 1 and " + MAX_PROCESSES + ".");

        List<MultiLevelScheduler.Job> jobs = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (int i = 0; i < rows; i++) {
            String pid = cell(i, COL_PID);
            if (!pid.matches("^[Pp]\\d+$")) {
                throw new InvalidInput(i, COL_PID,
                        "Row " + (i + 1) + ": Process ID must be P followed by numbers (e.g., P1).");
            }
            if (pid.charAt(0) == 'p') {
                pid = "P" + pid.substring(1);
                inputModel.setValueAt(pid, i, COL_PID);
            }
            if (!seen.add(pid)) {
                throw new InvalidInput(i, COL_PID, "Row " + (i + 1) + ": Duplicate PID '" + pid + "'.");
            }

            int at = parseNonNeg(cell(i, COL_AT), i, COL_AT, "Row " + (i + 1) + " Arrival Time");
            int bt = parsePos(cell(i, COL_BT), i, COL_BT, "Row " + (i + 1) + " Burst Time");
            int q = parseQueue(cell(i, COL_QUEUE), i, COL_QUEUE, "Row " + (i + 1) + " Queue");

            jobs.add(new MultiLevelScheduler.Job(pid, at, bt, q, i));
        }
        return jobs;
    }

    private int parseNonNeg(String text, int row, int col, String label) throws InvalidInput {
        if (text.isEmpty())
            throw new InvalidInput(row, col, label + " is empty.");
        try {
            int val = Integer.parseInt(text);
            if (val >= 0 && val <= MAX_TIME)
                return val;
        } catch (NumberFormatException ignored) {
        }
        throw new InvalidInput(row, col, label + " must be a whole number between 0 and " + MAX_TIME + ".");
    }

    private int parsePos(String text, int row, int col, String label) throws InvalidInput {
        if (text.isEmpty())
            throw new InvalidInput(row, col, label + " is empty.");
        try {
            int val = Integer.parseInt(text);
            if (val >= 1 && val <= MAX_TIME)
                return val;
        } catch (NumberFormatException ignored) {
        }
        throw new InvalidInput(row, col, label + " must be a whole number between 1 and " + MAX_TIME + ".");
    }

    private int parseQueue(String text, int row, int col, String label) throws InvalidInput {
        if (text.isEmpty())
            throw new InvalidInput(row, col, label + " is empty.");
        try {
            int val = Integer.parseInt(text);
            if (val == 1 || val == 2)
                return val;
        } catch (NumberFormatException ignored) {
        }
        throw new InvalidInput(row, col, label + " must be 1 (Queue 1: RR) or 2 (Queue 2: FCFS).");
    }
}
