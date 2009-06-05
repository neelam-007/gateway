package com.l7tech.external.assertions.xacmlpdp.console;

import com.l7tech.gui.util.Utilities;

import javax.swing.*;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * Copyright (C) 2009, Layer 7 Technologies Inc.
 * User: njordan
 * Date: 3-Apr-2009
 * Time: 4:25:55 PM
 * To change this template use File | Settings | File Templates.
 */
public class XacmlRequestBuilderXmlAttrPanel extends JDialog {
    private JPanel mainPanel;
    private JTextField nameField;
    private JTextField valueField;
    private JButton okButton;
    private JButton cancelButton;

    private boolean confirmed = false;

    public XacmlRequestBuilderXmlAttrPanel(Frame owner, String name, String value) {
        super(owner, "XML Attribute", true);
        init(name, value);
    }

    public XacmlRequestBuilderXmlAttrPanel(JDialog owner, String name, String value) {
        super(owner, "XML Attribute", true);
        init(name, value);
    }

    private void init(String name, String value) {
        DocumentListener documentListener = new DocumentListener() {
            public void changedUpdate(DocumentEvent evt) {
                enableDisableComponents();
            }

            public void insertUpdate(DocumentEvent evt) {
                enableDisableComponents();
            }

            public void removeUpdate(DocumentEvent evt) {
                enableDisableComponents();
            }
        };

        nameField.getDocument().addDocumentListener(documentListener);
        valueField.getDocument().addDocumentListener(documentListener);

        nameField.setText(name);
        valueField.setText(value);

        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                confirmed = true;
                setVisible(false);
            }
        });
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                setVisible(false);
            }
        });

        enableDisableComponents();
        setModal(true);
        setContentPane(mainPanel);
        pack();

        Utilities.centerOnParentWindow(this);
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public String getName() {
        return nameField.getText().trim();
    }

    public String getValue() {
        return valueField.getText().trim();
    }

    private void enableDisableComponents() {
        okButton.setEnabled(nameField.getText().trim().length() > 0 && valueField.getText().length() > 0);
    }
}
