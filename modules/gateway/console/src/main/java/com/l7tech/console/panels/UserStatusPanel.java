package com.l7tech.console.panels;

import javax.swing.*;
import java.awt.*;

/**
 * Panel which displays a user's LogonInfo status and a button to change the status.
 */
public class UserStatusPanel extends JPanel {
    private JButton statusButton;
    private JPanel contentPanel;
    private JLabel statusLabel;
    private JLabel dynamicStatusLabel;

    public UserStatusPanel() {
        super();
        setLayout(new BorderLayout());
        add(contentPanel, BorderLayout.CENTER);
    }

    public JButton getStatusButton() {
        return statusButton;
    }

    public JLabel getStatusLabel() {
        return statusLabel;
    }

    public JLabel getDynamicStatusLabel() {
        return dynamicStatusLabel;
    }
}
