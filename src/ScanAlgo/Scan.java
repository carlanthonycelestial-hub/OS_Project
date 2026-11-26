package src.ScanAlgo;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;

import src.Gui.MainApp;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Scan extends JPanel {
    private static final int MAX_REQUESTS = 20, MAX_LIMIT = 10000, DEFAULT_ROWS = 5;
    private static final Font BOLD_13 = new Font("SansSerif", Font.BOLD, 13);

    private final DefaultTableModel inputModel = new DefaultTableModel(
            new String[] { "Request ID", "Track / Cylinder" }, 0);
    private final DefaultTableModel outputModel = new DefaultTableModel(
            new String[] { "Step", "Req ID", "From", "To", "Seek" }, 0) {
        @Override
        public boolean isCellEditable(int r, int c) {
            return false;
        }
    };
    private final JTable inputTable = new JTable(inputModel);
    private final JTextField headField = new JTextField(5);
    private final JTextField maxTrackField = new JTextField(5);
    private final JComboBox<String> directionBox = new JComboBox<>(
            new String[] { "Right (Increasing)", "Left (Decreasing)" });
    private final JLabel totalLbl = new JLabel(), avgLbl = new JLabel(), seqLbl = new JLabel();

    private int chartHead, chartMaxTrack;
    private List<ScanScheduler.Step> chartSteps = new ArrayList<>();
    private final JPanel chartPanel = new JPanel() {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            drawChart((Graphics2D) g, getWidth(), getHeight());
        }
    };

    private static final class InvalidInput extends Exception {
        final int row, col;
        final JTextField field;

        InvalidInput(int row, int col, JTextField field, String msg) {
            super(msg);
            this.row = row;
            this.col = col;
            this.field = field;
        }
    }

    private final MainApp mainApp;

    public Scan(MainApp mainApp) {
        this.mainApp = mainApp;
        setLayout(new BorderLayout(12, 12));
        setBorder(new EmptyBorder(15, 20, 15, 20));
        setBackground(new Color(248, 250, 252));

        JLabel header = new JLabel("SCAN Disk Scheduling", SwingConstants.CENTER);
        header.setFont(new Font("SansSerif", Font.BOLD, 20));
        header.setForeground(new Color(15, 23, 42));
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);
        topBar.add(btn("\u2190 Back to Main Menu", () -> mainApp.showCard(MainApp.MENU_CARD)), BorderLayout.WEST);
        topBar.add(header, BorderLayout.CENTER);
        add(topBar, BorderLayout.NORTH);

        inputTable.setRowHeight(25);
        inputTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        addRows(DEFAULT_ROWS);

        JPanel headConfig = new JPanel();
        headConfig.setLayout(new BoxLayout(headConfig, BoxLayout.Y_AXIS));
        headConfig.setOpaque(false);
        headConfig.add(flow(FlowLayout.LEFT, 6, 2, lbl("Initial Head:"), headField, lbl("Max Track:"), maxTrackField));
        headConfig.add(flow(FlowLayout.LEFT, 6, 2, lbl("Direction:"), directionBox));

        JButton computeBtn = btn("Compute SCAN", this::runScan);
        computeBtn.setBackground(new Color(37, 99, 235));
        computeBtn.setForeground(Color.WHITE);

        JPanel controls = new JPanel();
        controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));
        controls.setOpaque(false);
        controls.add(flow(FlowLayout.CENTER, 4, 4,
                btn("+ Add Request", this::addSingleRow),
                btn("- Remove Selected", this::removeSelectedRow),
                btn("Add Multiple", this::promptAddMultipleRows)));
        controls.add(flow(FlowLayout.CENTER, 4, 4,
                btn("Load Sample Data", this::loadSampleData),
                btn("Reset / Clear", this::resetAll),
                computeBtn));

        JPanel left = titled("1. Disk Request Input (max " + MAX_REQUESTS + ")");
        left.add(headConfig, BorderLayout.NORTH);
        left.add(new JScrollPane(inputTable), BorderLayout.CENTER);
        left.add(controls, BorderLayout.SOUTH);

        JTable outputTable = new JTable(outputModel);
        outputTable.setRowHeight(25);
        JPanel stats = new JPanel(new GridLayout(3, 1, 6, 6));
        stats.setBackground(new Color(241, 245, 249));
        stats.setBorder(new EmptyBorder(10, 12, 10, 12));
        for (JLabel l : new JLabel[] { totalLbl, avgLbl, seqLbl }) {
            l.setFont(BOLD_13);
            stats.add(l);
        }
        showSummary(null, null, null);

        JPanel right = titled("2. Seek Sequence & Metrics");
        right.add(new JScrollPane(outputTable), BorderLayout.CENTER);
        right.add(stats, BorderLayout.SOUTH);

        JPanel center = new JPanel(new GridLayout(1, 2, 15, 0));
        center.setOpaque(false);
        center.add(left);
        center.add(right);
        add(center, BorderLayout.CENTER);

        chartPanel.setBackground(Color.WHITE);
        JPanel bottom = titled("3. Disk Head Movement (Track Scale)");
        bottom.setPreferredSize(new Dimension(0, 130));
        bottom.add(chartPanel, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
    }

    private void drawChart(Graphics2D g2, int w, int h) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (chartSteps.isEmpty()) {
            g2.setColor(Color.GRAY);
            g2.setFont(new Font("SansSerif", Font.ITALIC, 12));
            g2.drawString("Run 'Compute SCAN' to visualize the disk head movement.", 20, 42);
            return;
        }
        int max = Math.max(chartMaxTrack, 1), margin = 28, n = chartSteps.size();
        int span = Math.max(w - 2 * margin, 100), top = 24, bot = Math.max(h - 18, top + 20);

        g2.setColor(new Color(148, 163, 184));
        g2.drawLine(margin, 16, margin + span, 16);
        g2.setColor(new Color(37, 99, 235));
        g2.setStroke(new BasicStroke(2f));
        for (int i = 0; i < n; i++) {
            ScanScheduler.Step s = chartSteps.get(i);
            int x1 = margin + (int) Math.round(((double) s.from / max) * span);
            int x2 = margin + (int) Math.round(((double) s.to / max) * span);
            int y1 = top + (int) Math.round(((double) i / n) * (bot - top));
            int y2 = top + (int) Math.round(((double) (i + 1) / n) * (bot - top));
            g2.drawLine(x1, y1, x2, y2);
        }
        g2.setFont(new Font("SansSerif", Font.BOLD, 10));
        for (int i = 0; i <= n; i++) {
            int track = (i == 0) ? chartHead : chartSteps.get(i - 1).to;
            boolean isBoundary = i > 0 && "END".equals(chartSteps.get(i - 1).id);
            int x = margin + (int) Math.round(((double) track / max) * span);
            int y = top + (int) Math.round(((double) i / n) * (bot - top));
            g2.setColor(
                    i == 0 ? new Color(245, 158, 11) : isBoundary ? new Color(239, 68, 68) : new Color(16, 185, 129));
            g2.fillOval(x - 4, y - 4, 8, 8);
            g2.setColor(new Color(30, 41, 59));
            g2.drawOval(x - 4, y - 4, 8, 8);
            String txt = String.valueOf(track);
            int tw = g2.getFontMetrics().stringWidth(txt);
            g2.drawString(txt, Math.min(Math.max(x - tw / 2, 4), Math.max(w - tw - 4, 4)), Math.max(y - 6, 12));
        }
    }

    private static JLabel lbl(String text) {
        JLabel l = new JLabel(text);
        l.setFont(BOLD_13);
        return l;
    }

    private static JButton btn(String text, Runnable action) {
        JButton b = new JButton(text);
        b.setFont(BOLD_13);
        b.setMargin(new Insets(4, 6, 4, 6));
        b.addActionListener(e -> action.run());
        return b;
    }

    private static JPanel flow(int align, int hgap, int vgap, JComponent... items) {
        JPanel p = new JPanel(new FlowLayout(align, hgap, vgap));
        p.setOpaque(false);
        for (JComponent c : items)
            p.add(c);
        return p;
    }

    private static JPanel titled(String title) {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(203, 213, 225)), title, TitledBorder.LEFT, TitledBorder.TOP,
                BOLD_13));
        return p;
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Input Validation Error", JOptionPane.ERROR_MESSAGE);
    }

    private void stopEditing() {
        if (inputTable.isEditing())
            inputTable.getCellEditor().stopCellEditing();
    }

    private void selectCell(int row, int col) {
        inputTable.changeSelection(row, col, false, false);
        inputTable.scrollRectToVisible(inputTable.getCellRect(row, col, true));
    }

    private void showSummary(Integer total, Double avg, String seq) {
        totalLbl.setText("Total Head Movement: " + (total == null ? "-" : total + " tracks"));
        avgLbl.setText("Average Seek Distance: " + (avg == null ? "-" : String.format("%.2f tracks", avg)));
        seqLbl.setText("Seek Sequence: " + (seq == null ? "-" : seq));
        seqLbl.setToolTipText(seq);
    }

    private void addRows(int count) {
        int max = 0;
        for (int i = 0; i < inputModel.getRowCount(); i++) {
            String id = cell(i, 0);
            if (id.matches("^[Rr]\\d{1,9}$"))
                max = Math.max(max, Integer.parseInt(id.substring(1)));
        }
        for (int i = 1; i <= count; i++)
            inputModel.addRow(new Object[] { "R" + (max + i), "" });
    }

    private void addSingleRow() {
        stopEditing();
        if (inputModel.getRowCount() >= MAX_REQUESTS) {
            showError("The maximum number of requests is " + MAX_REQUESTS + ".");
            return;
        }
        addRows(1);
        selectCell(inputModel.getRowCount() - 1, 0);
    }

    private void removeSelectedRow() {
        stopEditing();
        int row = inputTable.getSelectedRow();
        if (row == -1)
            showError("Please select a row in the table first.");
        else
            inputModel.removeRow(row);
    }

    private void promptAddMultipleRows() {
        stopEditing();
        int rem = MAX_REQUESTS - inputModel.getRowCount();
        if (rem <= 0) {
            addSingleRow();
            return;
        }
        String in = JOptionPane.showInputDialog(this, "How many requests to add? (1-" + rem + ")", "Add Multiple",
                JOptionPane.QUESTION_MESSAGE);
        if (in == null)
            return;
        try {
            int n = Integer.parseInt(in.trim());
            if (n < 1 || n > rem) {
                showError("Enter a number between 1 and " + rem + ".");
                return;
            }
            addRows(n);
            selectCell(inputModel.getRowCount() - 1, 0);
        } catch (NumberFormatException ex) {
            showError("Invalid input. Please enter a whole number.");
        }
    }

    private void loadSampleData() {
        stopEditing();
        // Sample: Head 53, max track 199, moving Left
        headField.setText("53");
        maxTrackField.setText("199");
        directionBox.setSelectedIndex(1); // Left
        inputModel.setRowCount(0);
        String[][] sample = { {"R1","98"}, {"R2","183"}, {"R3","37"}, {"R4","122"}, {"R5","14"}, {"R6","124"}, {"R7","65"}, {"R8","67"} };
        for (String[] row : sample)
            inputModel.addRow(row);
    }

    private void resetAll() {
        stopEditing();
        headField.setText("");
        maxTrackField.setText("");
        directionBox.setSelectedIndex(0);
        inputModel.setRowCount(0);
        addRows(DEFAULT_ROWS);
        outputModel.setRowCount(0);
        chartSteps = new ArrayList<>();
        chartPanel.repaint();
        showSummary(null, null, null);
    }

    private void runScan() {
        stopEditing();
        try {
            int maxTrack = parseInt(maxTrackField.getText().trim(), 1, MAX_LIMIT, -1, -1, maxTrackField, "Max Track");
            int head = parseInt(headField.getText().trim(), 0, maxTrack, -1, -1, headField, "Initial Head");
            List<ScanScheduler.Request> requests = readRequests(maxTrack);
            List<ScanScheduler.Step> steps = ScanScheduler.schedule(head, maxTrack,
                    directionBox.getSelectedIndex() == 0, requests);

            outputModel.setRowCount(0);
            StringBuilder seq = new StringBuilder().append(head);
            for (ScanScheduler.Step s : steps) {
                outputModel.addRow(new Object[] { s.order, s.id, s.from, s.to, s.distance });
                seq.append(" \u2192 ").append(s.to);
            }
            showSummary(ScanScheduler.totalSeek(steps), ScanScheduler.averageSeek(steps, requests.size()),
                    seq.toString());
            chartHead = head;
            chartMaxTrack = maxTrack;
            chartSteps = steps;
            chartPanel.repaint();

            // Transfer to DiskResultPanel
            java.util.List<src.Gui.DiskResultPanel.StepData> resultSteps = new ArrayList<>();
            for (ScanScheduler.Step s : steps) {
                resultSteps.add(new src.Gui.DiskResultPanel.StepData(s.order, s.id, s.from, s.to, s.distance));
            }
            mainApp.getDiskResultPanel().displayResult(
                    "SCAN Disk Scheduling",
                    MainApp.SCAN_CARD,
                    head,
                    maxTrack,
                    resultSteps,
                    ScanScheduler.totalSeek(steps),
                    ScanScheduler.averageSeek(steps, requests.size()),
                    seq.toString()
            );
        } catch (InvalidInput ex) {
            if (ex.row >= 0) {
                selectCell(ex.row, ex.col);
                inputTable.requestFocusInWindow();
            } else if (ex.field != null)
                ex.field.requestFocusInWindow();
            showError(ex.getMessage());
        }
    }

    private String cell(int r, int c) {
        Object v = inputModel.getValueAt(r, c);
        return v == null ? "" : v.toString().trim();
    }

    private List<ScanScheduler.Request> readRequests(int maxTrack) throws InvalidInput {
        int rows = inputModel.getRowCount();
        if (rows == 0 || rows > MAX_REQUESTS)
            throw new InvalidInput(-1, -1, null, "Requests must be between 1 and " + MAX_REQUESTS + ".");
        List<ScanScheduler.Request> list = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < rows; i++) {
            String id = cell(i, 0);
            if (!id.matches("^[Rr]\\d+$"))
                throw new InvalidInput(i, 0, null,
                        "Row " + (i + 1) + ": Request ID must be R followed by numbers (e.g., R1).");
            if (id.charAt(0) == 'r') {
                id = "R" + id.substring(1);
                inputModel.setValueAt(id, i, 0);
            }
            if (!seen.add(id))
                throw new InvalidInput(i, 0, null, "Row " + (i + 1) + ": Duplicate Request ID '" + id + "'.");
            list.add(new ScanScheduler.Request(id,
                    parseInt(cell(i, 1), 0, maxTrack, i, 1, null, "Row " + (i + 1) + " Track"), i));
        }
        return list;
    }

    private int parseInt(String text, int min, int max, int row, int col, JTextField field, String label)
            throws InvalidInput {
        if (text.isEmpty())
            throw new InvalidInput(row, col, field, label + " is empty.");
        try {
            int val = Integer.parseInt(text);
            if (val >= min && val <= max)
                return val;
        } catch (NumberFormatException ignored) {
        }
        throw new InvalidInput(row, col, field, label + " must be a whole number between " + min + " and " + max + ".");
    }
}
