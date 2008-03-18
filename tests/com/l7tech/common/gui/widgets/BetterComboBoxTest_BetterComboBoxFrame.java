/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.common.gui.widgets;

import javax.swing.*;

/**
 *
 *
 * @author rmak
 * @since SecureSpan 5.0
 */
public class BetterComboBoxTest_BetterComboBoxFrame extends JFrame {
    private JPanel _contentPane;
    private JComboBox _comboBox;

    public BetterComboBoxTest_BetterComboBoxFrame() {
        setTitle("BetterComboBox");
        setContentPane(_contentPane);

        pack();
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    }

    public JComboBox getComboBox() {
        return _comboBox;
    }
}
