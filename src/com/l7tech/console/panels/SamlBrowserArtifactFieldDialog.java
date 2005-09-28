/**
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 */
package com.l7tech.console.panels;

import javax.swing.*;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class SamlBrowserArtifactFieldDialog extends JDialog {
    private JButton cancelButton;
    private JButton okButton;
    private JTextField valueField;
    private JTextField nameField;
    private JPanel mainPanel;

    private boolean modified = false;

    public SamlBrowserArtifactFieldDialog(Dialog owner, boolean modal) throws HeadlessException {
        super(owner, "Form Field Properties", modal);

        DocumentListener docListener = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { updateButtons(); }
            public void removeUpdate(DocumentEvent e) { updateButtons(); }
            public void changedUpdate(DocumentEvent e) { updateButtons(); }
        };
        nameField.getDocument().addDocumentListener(docListener);
        valueField.getDocument().addDocumentListener(docListener);

        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                modified = true;
                dispose();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                modified = false;
                dispose();
            }
        });

        updateButtons();
        add(mainPanel);
    }

    private void updateButtons() {
        okButton.setEnabled(nameField.getText()!=null && nameField.getText().trim().length() > 0);
    }

    public boolean isModified() {
        return modified;
    }

    public JTextField getValueField() {
        return valueField;
    }

    public JTextField getNameField() {
        return nameField;
    }
}
