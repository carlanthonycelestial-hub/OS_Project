package src.SJFAlgo;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;

import src.Gui.GanttChartPanel;
import src.Gui.MainApp;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SJF extends JPanel {
    private static final int MAX_PROCESSES = 20;
    private static final int MAX_TIME = 10000;
    private static final int DEFAULT_ROWS = 5;
    private static final int COL_PID = 0, COL_AT = 1, COL_BT = 2;
    private static final Font BOLD_13 = new Font("SansSerif", Font.BOLD, 13);
    private static final String[] AVG_NAMES = { "Completion", "Turnaround", "Waiting" };

    private final DefaultTableModel inputModel = new DefaultTableModel(
            new String[] { "PID", "Arrival Time", "Burst Time" }, 0);
    private final DefaultTableModel outputModel = new DefaultTableModel(
            new String[] { "PID", "AT", "BT", "CT", "TAT", "WT" }, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable inputTable = new JTable(inputModel);
    private final GanttChartPanel gantt = new GanttChartPanel();
    private final JLabel[] avgLabels = { new JLabel(), new JLabel(), new JLabel() };

    private static final class InvalidInput extends Exception {
        final int row, col;

        InvalidInput(int row, int col, String message) {
            super(message);
            this.row = row;
            this.col = col;
        }
    }

    public SJF(MainApp mainApp) {
        setLayout(new BorderLayout(12, 12));
        setBorder(new EmptyBorder(15, 20, 15, 20));
        setBackground(new Color(248, 250, 252));

        JLabel header = new JLabel("Shortest Job First (SJF) - Non-Preemptive", SwingConstants.CENTER);
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

        JPanel rowButtons = flowPanel(8, 2,
                button("+ Add Process", this::addSingleRow),
                button("- Remove Selected", this::removeSelectedRow),
                button("Add Multiple", this::promptAddMultipleRows));
        JButton computeBtn = button("Compute SJF", this::runSJF);
        computeBtn.setBackground(new Color(37, 99, 235));
        computeBtn.setForeground(Color.WHITE);
        JPanel actionButtons = flowPanel(10, 4,
                button("Load Sample Data", this::loadSampleData),
                button("Reset / Clear", this::resetAll),
                computeBtn);
        JPanel controls = new JPanel(new GridLayout(2, 1, 5, 5));
        controls.setOpaque(false);
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
        for (int i = 0; i < count; i++)
            inputModel.addRow(new Object[] { "P" + nextPidNumber(), "", "" });
    }

    private void addSingleRow() {
        stopEditing();
        if (inputModel.getRowCount() >= MAX_PROCESSES) {
            JOptionPane.showMessageDialog(this, "The maximum number of processes is " + MAX_PROCESSES + ".",
                    "Limit Reached", JOptionPane.WARNING_MESSAGE);
            return;
        }
        addRows(1);
        selectCell(inputModel.getRowCount() - 1, COL_PID);
    }

    private void removeSelectedRow() {
        stopEditing();
        int selected = inputTable.getSelectedRow();
        if (selected == -1) {
            JOptionPane.showMessageDialog(this,
                    "Please select a row in the table first, then click \"Remove Selected\".",
                    "No Row Selected", JOptionPane.INFORMATION_MESSAGE);
        } else {
            inputModel.removeRow(selected);
        }
    }

    private void promptAddMultipleRows() {
        stopEditing();
        int remaining = MAX_PROCESSES - inputModel.getRowCount();
        if (remaining <= 0) {
            addSingleRow();
            return;
        }
        String input = JOptionPane.showInputDialog(this,
                "How many processes would you like to add? (up to " + remaining + " more)",
                "Add Multiple Processes", JOptionPane.QUESTION_MESSAGE);
        if (input == null)
            return;

        try {
            int count = Integer.parseInt(input.trim());
            if (count < 1 || count > remaining) {
                showError("Please enter a number between 1 and " + remaining
                        + " (the table can hold at most " + MAX_PROCESSES + " processes).");
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
        for (SJFScheduler.Job j : SJFSample.staggeredArrivals())
            inputModel.addRow(new Object[] { j.pid, String.valueOf(j.arrival), String.valueOf(j.burst) });
    }

    private void resetAll() {
        stopEditing();
        inputModel.setRowCount(0);
        addRows(DEFAULT_ROWS);
        outputModel.setRowCount(0);
        gantt.setBlocks(new ArrayList<>());
        showAverages(null);
    }

    private void runSJF() {
        stopEditing();
        try {
            List<SJFScheduler.Job> jobs = readJobs();
            List<SJFScheduler.Block> blocks = SJFScheduler.schedule(jobs);

            outputModel.setRowCount(0);
            for (SJFScheduler.Job j : jobs) {
                outputModel.addRow(new Object[] { j.pid, j.arrival, j.burst, j.completion, j.turnaround, j.waiting });
            }
            showAverages(SJFScheduler.averages(jobs));
            gantt.setBlocks(blocks);
        } catch (InvalidInput ex) {
            if (ex.row >= 0) {
                selectCell(ex.row, ex.col);
                inputTable.requestFocusInWindow();
            }
            showError(ex.getMessage());
        }
    }

    private String cell(int row, int col) {
        Object value = inputModel.getValueAt(row, col);
        return value == null ? "" : value.toString().trim();
    }

    private List<SJFScheduler.Job> readJobs() throws InvalidInput {
        int rows = inputModel.getRowCount();
        if (rows == 0)
            throw new InvalidInput(-1, -1, "Please add at least one process before computing.");
        if (rows > MAX_PROCESSES)
            throw new InvalidInput(-1, -1, "Too many processes. The maximum is " + MAX_PROCESSES + ".");

        List<SJFScheduler.Job> jobs = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (int i = 0; i < rows; i++) {
            String pid = cell(i, COL_PID);
            String where = "Row " + (i + 1) + (pid.isEmpty() ? "" : " (" + pid + ")");

            if (pid.isEmpty())
                throw new InvalidInput(i, COL_PID, where + ": PID is empty.");
            if (!pid.matches("^[Pp]\\d+$")) {
                throw new InvalidInput(i, COL_PID, "Invalid PID '" + pid + "' at row " + (i + 1)
                        + ". PID must be the letter P followed only by numbers (e.g., P1, P2).");
            }
            if (pid.charAt(0) == 'p') {
                pid = "P" + pid.substring(1);
                inputModel.setValueAt(pid, i, COL_PID);
            }
            if (!seen.add(pid)) {
                throw new InvalidInput(i, COL_PID, "Duplicate PID '" + pid + "' at row " + (i + 1)
                        + ". Each process must have a unique PID.");
            }

            int arrival = parseField(i, COL_AT, where, "Arrival Time", 0);
            int burst = parseField(i, COL_BT, where, "Burst Time", 1);
            jobs.add(new SJFScheduler.Job(pid, arrival, burst, i));
        }
        return jobs;
    }

    private int parseField(int row, int col, String where, String name, int min) throws InvalidInput {
        String text = cell(row, col);
        String range = "a whole number between " + min + " and " + MAX_TIME;
        if (text.isEmpty())
            throw new InvalidInput(row, col, where + ": " + name + " is empty.");
        try {
            int value = Integer.parseInt(text);
            if (value >= min && value <= MAX_TIME)
                return value;
        } catch (NumberFormatException ignored) {
        }
        throw new InvalidInput(row, col, where + ": " + name + " must be " + range + ".");
    }
}