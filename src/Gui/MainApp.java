package src.Gui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

import src.LookAlgo.Look;
import src.MultipleLevelAlgo.MultiLevel;
import src.PriorityAlgo.Priority;
import src.SJFAlgo.SJF;
import src.ScanAlgo.Scan;

import java.awt.*;

public class MainApp extends JFrame {
    private final CardLayout cardLayout;
    private final JPanel mainContainer;

    public static final String MENU_CARD = "MENU";
    public static final String SJF_CARD = "SJF";
    public static final String MLQ_CARD = "MLQ";
    public static final String PRIORITY_CARD = "PRIORITY";
    public static final String SCAN_CARD = "SCAN";
    public static final String LOOK_CARD = "LOOK";
    public static final String DISK_RESULT_CARD = "DISK_RESULT";

    private final DiskResultPanel diskResultPanel;

    public MainApp() {
        setTitle("OS Scheduling Simulator - CPU & Disk Scheduling");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(950, 700);
        setMinimumSize(new Dimension(850, 620));
        setLocationRelativeTo(null);

        cardLayout = new CardLayout();
        mainContainer = new JPanel(cardLayout);

        diskResultPanel = new DiskResultPanel(this);

        mainContainer.add(createMainMenuPanel(), MENU_CARD);
        mainContainer.add(new SJF(this), SJF_CARD);
        mainContainer.add(new MultiLevel(this), MLQ_CARD);
        mainContainer.add(new Priority(this), PRIORITY_CARD);
        mainContainer.add(new Scan(this), SCAN_CARD);
        mainContainer.add(new Look(this), LOOK_CARD);
        mainContainer.add(diskResultPanel, DISK_RESULT_CARD);

        add(mainContainer);
        showCard(MENU_CARD);
    }

    public DiskResultPanel getDiskResultPanel() {
        return diskResultPanel;
    }

    public void showCard(String cardName) {
        cardLayout.show(mainContainer, cardName);
    }

    private JPanel createMainMenuPanel() {
        JPanel wrapper = new JPanel(new BorderLayout(20, 20));
        wrapper.setBorder(new EmptyBorder(30, 50, 30, 50));
        wrapper.setBackground(new Color(245, 247, 250));

        JLabel titleLabel = new JLabel("Operating Systems Scheduling Simulator", SwingConstants.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 26));
        titleLabel.setForeground(new Color(30, 41, 59));

        JLabel subtitleLabel = new JLabel("Select an algorithm below to begin", SwingConstants.CENTER);
        subtitleLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        subtitleLabel.setForeground(new Color(100, 116, 139));

        JPanel headerPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        headerPanel.setOpaque(false);
        headerPanel.add(titleLabel);
        headerPanel.add(subtitleLabel);
        wrapper.add(headerPanel, BorderLayout.NORTH);

        JPanel groupsPanel = new JPanel(new GridLayout(1, 2, 30, 0));
        groupsPanel.setOpaque(false);

        JPanel cpuPanel = createGroupCard("CPU Scheduling Algorithms");
        cpuPanel.add(createMenuButton("Shortest Job First (SJF)", SJF_CARD, false));
        cpuPanel.add(Box.createVerticalStrut(15));
        cpuPanel.add(createMenuButton("Multilevel Queue", MLQ_CARD, false));
        cpuPanel.add(Box.createVerticalStrut(15));
        cpuPanel.add(createMenuButton("Priority (Preemptive)", PRIORITY_CARD, false));

        JPanel diskPanel = createGroupCard("Disk Scheduling Algorithms");
        diskPanel.add(createMenuButton("SCAN", SCAN_CARD, false));
        diskPanel.add(Box.createVerticalStrut(15));
        diskPanel.add(createMenuButton("LOOK", LOOK_CARD, false));
        diskPanel.add(Box.createVerticalGlue());

        groupsPanel.add(cpuPanel);
        groupsPanel.add(diskPanel);

        groupsPanel.setBorder(new EmptyBorder(10, 0, 0, 0));
        wrapper.add(groupsPanel, BorderLayout.CENTER);

        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        footerPanel.setOpaque(false);

        JButton aboutBtn = new JButton("About");
        aboutBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        aboutBtn.setPreferredSize(new Dimension(140, 40));
        aboutBtn.addActionListener(e -> showAboutDialog());

        JButton exitBtn = new JButton("Exit");
        exitBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        exitBtn.setPreferredSize(new Dimension(140, 40));
        exitBtn.setBackground(new Color(220, 38, 38));
        exitBtn.setForeground(Color.WHITE);
        exitBtn.setFocusPainted(false);
        exitBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(
                    this, "Are you sure you want to exit?", "Confirm Exit", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                System.exit(0);
            }
        });

        footerPanel.add(aboutBtn);
        footerPanel.add(exitBtn);
        wrapper.add(footerPanel, BorderLayout.SOUTH);

        return wrapper;
    }

    private JPanel createGroupCard(String groupTitle) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(203, 213, 225), 1, true),
                new EmptyBorder(25, 25, 25, 25)));

        JLabel label = new JLabel(groupTitle);
        label.setFont(new Font("SansSerif", Font.BOLD, 18));
        label.setForeground(new Color(15, 23, 42));
        label.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(label);
        panel.add(Box.createVerticalStrut(25));
        return panel;
    }

    private JButton createMenuButton(String text, String targetCard, boolean isPrimary) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.BOLD, 15));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        btn.setPreferredSize(new Dimension(260, 50));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setFocusPainted(false);

        if (isPrimary) {
            btn.setBackground(new Color(37, 99, 235));
            btn.setForeground(Color.WHITE);
        } else {
            btn.setBackground(new Color(241, 245, 249));
            btn.setForeground(new Color(30, 41, 59));
        }

        btn.addActionListener(e -> showCard(targetCard));
        return btn;
    }

    private void showAboutDialog() {
        String aboutMessage = "OS CPU & Disk Scheduling Simulator\n\n"
                + "Group Number: [Insert Group Number]\n\n"
                + "Group Members:\n"
                + "\u2022 [Member 1 Zach] - Shortest Job First (SJF)\n"
                + "\u2022 [Member 2 Roman] - Multilevel Queue\n"
                + "\u2022 [Member 3 Kent] - Priority (Preemptive)\n"
                + "\u2022 [Member 4 Pierre] - SCAN Disk Scheduling\n"
                + "\u2022 [Member 5 Carl] - LOOK Disk Scheduling";
        JOptionPane.showMessageDialog(this, aboutMessage, "About Group Project", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {
        }
        SwingUtilities.invokeLater(() -> new MainApp().setVisible(true));
    }
}
