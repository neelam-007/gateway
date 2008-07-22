/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.gui.widgets;

import javax.swing.*;

/**
 *
 *
 * @author rmak
 * @since SecureSpan 5.0
 */
public class BetterComboBoxTest_JComboBoxFrame extends JFrame {
    private JPanel _contentPane;
    private JComboBox _comboBox;

    public BetterComboBoxTest_JComboBoxFrame() {
        setTitle("JComboBox");
        setContentPane(_contentPane);

        pack();
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    }

    public JComboBox getComboBox() {
        return _comboBox;
    }
}
