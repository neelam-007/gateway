package com.l7tech.external.assertions.xacmlpdp.console;

import javax.swing.*;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * Copyright (C) 2009, Layer 7 Technologies Inc.
 * User: njordan
 * Date: 9-Apr-2009
 * Time: 8:58:01 PM
 * To change this template use File | Settings | File Templates.
 */
public class XacmlRequestBuilderNamespaceDialog extends JDialog{
    private JTextField prefixField;
    private JTextField uriField;
    private JButton okButton;
    private JButton cancelButton;
    private JPanel mainPanel;

    private boolean confirmed = false;

    public XacmlRequestBuilderNamespaceDialog(Frame owner, String prefix, String uri) {
        super(owner, "Namespace", true);
        initComponents(prefix, uri);
    }

    public XacmlRequestBuilderNamespaceDialog(Dialog owner, String prefix, String uri) {
        super(owner, "Namespace", true);
        initComponents(prefix, uri);
    }

    private void initComponents(String prefix, String uri) {
        prefixField.setText((prefix == null) ? "" : prefix);
        uriField.setText((uri == null) ? "" : uri);

        DocumentListener documentListener = new DocumentListener() {
            public void changedUpdate(DocumentEvent evt) {
                enableDisableButtons();
            }

            public void insertUpdate(DocumentEvent evt) {
                enableDisableButtons();
            }

            public void removeUpdate(DocumentEvent evt) {
                enableDisableButtons();
            }
        };

        prefixField.getDocument().addDocumentListener(documentListener);
        uriField.getDocument().addDocumentListener(documentListener);

        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                confirmed = true;
                XacmlRequestBuilderNamespaceDialog.this.dispose();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                XacmlRequestBuilderNamespaceDialog.this.dispose();
            }
        });

        setContentPane(mainPanel);
        pack();
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    private void enableDisableButtons() {
        okButton.setEnabled(prefixField.getText().trim().length() > 0 && uriField.getText().trim().length() > 0);
    }

    public String getPrefix() {
        return prefixField.getText().trim();
    }

    public String getUri() {
        return uriField.getText().trim();
    }
}
