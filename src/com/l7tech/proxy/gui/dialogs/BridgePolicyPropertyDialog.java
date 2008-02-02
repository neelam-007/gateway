/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.proxy.gui.dialogs;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * @author alex
 */
public class BridgePolicyPropertyDialog extends JDialog {
    private JTextField nameTextField;
    private JTextField valueTextField;
    private JButton okButton;
    private JButton cancelButton;
    private JPanel mainPanel;

    private String name;
    private String value;

    private boolean ok;

    public BridgePolicyPropertyDialog(Frame owner, String name, String value) {
        super(owner, "Bridge Policy Property", true);
        initialize(name, value);
    }

    public BridgePolicyPropertyDialog(Dialog owner, String name, String value) {
        super(owner, "Bridge Policy Property", true);
        initialize(name, value);
    }

    private void initialize(String name, String value) {
        this.name = name;
        this.value = value;

        nameTextField.setText(name);
        valueTextField.setText(value);

        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ok = true;
                set(nameTextField.getText(), valueTextField.getText());
                dispose();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ok = false;
                set(null, null);
                dispose();
            }
        });

        add(mainPanel);
    }

    private void set(final String nv, final String vv) {
        this.name = nv;
        this.value = vv;
    }

    public boolean isOk() {
        return ok;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }
}
