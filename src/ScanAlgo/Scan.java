package src.ScanAlgo;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;

import src.Gui.DiskResultPanel.StepData;
import src.Gui.MainApp;
import src.ScanAlgo.ScanScheduler.Request;
import src.ScanAlgo.ScanScheduler.Step;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * SCAN (Elevator) Disk Scheduling input panel.
 * Collects requests, runs SCAN, and displays output in DiskResultPanel.
 */
public class Scan extends JPanel {
    private static final int MAX_REQ = 20;
    private static final int MAX_CYLINDER = 10000;
    private static final Font BOLD_FONT = new Font("SansSerif", Font.BOLD, 13);

    private final MainApp mainApp;
    private final DefaultTableModel inputModel = new DefaultTableModel(
            new String[] { "Request ID", "Track / Cylinder" }, 0);
    private final JTable inputTable = new JTable(inputModel);
    private final JTextField headField = new JTextField("53", 5);
    private final JTextField maxTrackField = new JTextField("199", 5);
    private final JComboBox<String> directionBox = new JComboBox<>(
            new String[] { "Right (Increasing)", "Left (Decreasing)" });

    public Scan(MainApp mainApp) {
        this.mainApp = mainApp;
        setLayout(new BorderLayout(12, 12));
        setBorder(new EmptyBorder(15, 20, 15, 20));
        setBackground(new Color(248, 250, 252));

        // Top Header
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);
        JButton backBtn = new JButton("\u2190 Back to Main Menu");
        backBtn.setFont(BOLD_FONT);
        backBtn.addActionListener(e -> mainApp.showCard(MainApp.MENU_CARD));

        JLabel title = new JLabel("SCAN Disk Scheduling", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        title.setForeground(new Color(15, 23, 42));

        topBar.add(backBtn, BorderLayout.WEST);
        topBar.add(title, BorderLayout.CENTER);
        add(topBar, BorderLayout.NORTH);

        // Input Table Setup
        inputTable.setRowHeight(25);
        inputTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        addRows(5);

        // Configuration Bar (Initial Head, Max Track, Direction)
        JPanel headBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        headBar.setOpaque(false);
        headBar.add(createLabel("Initial Head:"));
        headBar.add(headField);
        headBar.add(createLabel("Max Track:"));
        headBar.add(maxTrackField);
        headBar.add(createLabel("Direction:"));
        headBar.add(directionBox);

        // Action Buttons
        JButton computeBtn = new JButton("Compute SCAN");
        computeBtn.setFont(BOLD_FONT);
        computeBtn.setBackground(new Color(37, 99, 235));
        computeBtn.setForeground(Color.WHITE);
        computeBtn.addActionListener(e -> runScan());

        JPanel rowBtnBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 4));
        rowBtnBar.setOpaque(false);
        rowBtnBar.add(createButton("+ Add Request", this::addSingleRow));
        rowBtnBar.add(createButton("- Remove Selected", this::removeSelectedRow));
        rowBtnBar.add(createButton("Add Multiple", this::promptAddMultiple));

        JPanel actionBtnBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 4));
        actionBtnBar.setOpaque(false);
        actionBtnBar.add(createButton("Load Sample Data", this::loadSampleData));
        actionBtnBar.add(createButton("Reset / Clear", this::resetAll));
        actionBtnBar.add(computeBtn);

        JPanel controls = new JPanel(new GridLayout(2, 1, 4, 4));
        controls.setOpaque(false);
        controls.add(rowBtnBar);
        controls.add(actionBtnBar);

        // Main Center Panel
        JPanel mainBox = new JPanel(new BorderLayout(8, 8));
        mainBox.setBackground(Color.WHITE);
        mainBox.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(203, 213, 225)),
                "Disk Request Input (max " + MAX_REQ + ")",
                TitledBorder.LEFT, TitledBorder.TOP, BOLD_FONT));

        mainBox.add(headBar, BorderLayout.NORTH);
        mainBox.add(new JScrollPane(inputTable), BorderLayout.CENTER);
        mainBox.add(controls, BorderLayout.SOUTH);

        add(mainBox, BorderLayout.CENTER);
    }

    private void runScan() {
        if (inputTable.isEditing()) inputTable.getCellEditor().stopCellEditing();

        try {
            int maxTrack = parseNumber(maxTrackField.getText().trim(), 1, MAX_CYLINDER, "Max Track");
            int head = parseNumber(headField.getText().trim(), 0, maxTrack, "Initial Head");
            List<Request> requests = readRequests(maxTrack);
            boolean movingRight = (directionBox.getSelectedIndex() == 0);

            // Compute schedule using ScanScheduler
            List<Step> steps = ScanScheduler.schedule(head, maxTrack, movingRight, requests);
            int total = ScanScheduler.totalSeek(steps);
            double avg = ScanScheduler.averageSeek(steps, requests.size());

            // Build sequence string (e.g., 53 -> 37 -> 14 -> 0...)
            StringBuilder seq = new StringBuilder().append(head);
            List<StepData> resultSteps = new ArrayList<>();
            for (Step s : steps) {
                resultSteps.add(new StepData(s.order, s.id, s.from, s.to, s.distance));
                seq.append(" \u2192 ").append(s.to);
            }

            // Transfer to DiskResultPanel
            mainApp.getDiskResultPanel().displayResult(
                    "SCAN Disk Scheduling",
                    MainApp.SCAN_CARD,
                    head,
                    maxTrack,
                    resultSteps,
                    total,
                    avg,
                    seq.toString()
            );

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Input Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private List<Request> readRequests(int maxTrack) throws Exception {
        int rows = inputModel.getRowCount();
        if (rows == 0) throw new Exception("Please enter at least one request.");
        if (rows > MAX_REQ) throw new Exception("Maximum allowed requests is " + MAX_REQ + ".");

        List<Request> list = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (int i = 0; i < rows; i++) {
            String id = cell(i, 0);
            if (!id.matches("^[Rr]\\d+$")) {
                throw new Exception("Row " + (i + 1) + ": Request ID must be like 'R1', 'R2'.");
            }
            id = "R" + id.substring(1);
            inputModel.setValueAt(id, i, 0);

            if (!seen.add(id)) {
                throw new Exception("Row " + (i + 1) + ": Duplicate Request ID '" + id + "'.");
            }

            int track = parseNumber(cell(i, 1), 0, maxTrack, "Row " + (i + 1) + " Track");
            list.add(new Request(id, track, i));
        }
        return list;
    }

    private int parseNumber(String text, int min, int max, String label) throws Exception {
        if (text.isEmpty()) throw new Exception(label + " cannot be empty.");
        try {
            int val = Integer.parseInt(text);
            if (val >= min && val <= max) return val;
        } catch (NumberFormatException ignored) {}
        throw new Exception(label + " must be between " + min + " and " + max + ".");
    }

    private void addRows(int count) {
        int nextNum = inputModel.getRowCount() + 1;
        for (int i = 0; i < count; i++) {
            inputModel.addRow(new Object[] { "R" + (nextNum + i), "" });
        }
    }

    private void addSingleRow() {
        if (inputModel.getRowCount() >= MAX_REQ) {
            JOptionPane.showMessageDialog(this, "Limit reached (" + MAX_REQ + ").");
            return;
        }
        addRows(1);
    }

    private void removeSelectedRow() {
        int row = inputTable.getSelectedRow();
        if (row != -1) inputModel.removeRow(row);
    }

    private void promptAddMultiple() {
        int remaining = MAX_REQ - inputModel.getRowCount();
        if (remaining <= 0) return;
        String val = JOptionPane.showInputDialog(this, "How many to add? (1-" + remaining + ")");
        if (val == null) return;
        try {
            int n = Integer.parseInt(val.trim());
            if (n >= 1 && n <= remaining) addRows(n);
        } catch (NumberFormatException ignored) {}
    }

    private void loadSampleData() {
        headField.setText("53");
        maxTrackField.setText("199");
        directionBox.setSelectedIndex(1); // Left
        inputModel.setRowCount(0);
        String[][] sample = {
            {"R1","98"}, {"R2","183"}, {"R3","37"}, {"R4","122"},
            {"R5","14"}, {"R6","124"}, {"R7","65"}, {"R8","67"}
        };
        for (String[] r : sample) inputModel.addRow(r);
    }

    private void resetAll() {
        headField.setText("53");
        maxTrackField.setText("199");
        directionBox.setSelectedIndex(1);
        inputModel.setRowCount(0);
        addRows(5);
    }

    private String cell(int r, int c) {
        Object v = inputModel.getValueAt(r, c);
        return v == null ? "" : v.toString().trim();
    }

    private JLabel createLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(BOLD_FONT);
        return l;
    }

    private JButton createButton(String text, Runnable action) {
        JButton b = new JButton(text);
        b.setFont(BOLD_FONT);
        b.addActionListener(e -> action.run());
        return b;
    }
}
