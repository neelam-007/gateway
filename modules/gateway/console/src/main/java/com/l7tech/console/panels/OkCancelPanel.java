package com.l7tech.console.panels;

import javax.swing.*;

/**
 * Lightweight re-usable panel which contains an OK button and a Cancel button.
 */
public class OkCancelPanel extends JPanel {
    private JButton okButton;
    private JButton cancelButton;
    private JPanel contentPanel;
    private JPanel buttonPanel;

    public JButton getOkButton() {
        return okButton;
    }

    public JButton getCancelButton() {
        return cancelButton;
    }
}
