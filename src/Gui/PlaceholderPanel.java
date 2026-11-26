package src.Gui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

import java.awt.*;

public class PlaceholderPanel extends JPanel {
    public PlaceholderPanel(MainApp mainApp, String moduleName) {
        setLayout(new BorderLayout(20, 20));
        setBorder(new EmptyBorder(20, 25, 20, 25));
        setBackground(new Color(248, 250, 252));

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);

        JButton backBtn = new JButton("\u2190 Back to Main Menu");
        backBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
        backBtn.addActionListener(e -> mainApp.showCard(MainApp.MENU_CARD));

        topBar.add(backBtn, BorderLayout.WEST);
        add(topBar, BorderLayout.NORTH);

        JPanel centerBox = new JPanel(new GridBagLayout());
        centerBox.setBackground(Color.WHITE);
        centerBox.setBorder(BorderFactory.createLineBorder(new Color(203, 213, 225)));

        JLabel msg = new JLabel("<html><div style='text-align:center;'>"
                + "<h2>" + moduleName + "</h2>"
                + "<p>This module is assigned to a group member.<br>"
                + "Replace this panel in <code>MainApp.java</code> once implemented.</p>"
                + "</div></html>");
        msg.setFont(new Font("SansSerif", Font.PLAIN, 15));
        centerBox.add(msg);

        add(centerBox, BorderLayout.CENTER);
    }
}