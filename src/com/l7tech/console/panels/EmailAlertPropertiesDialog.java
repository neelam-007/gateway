/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.console.panels;

import com.l7tech.common.gui.NumberField;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.policy.assertion.alert.EmailAlertAssertion;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Properties dialog for {@link com.l7tech.policy.assertion.alert.EmailAlertAssertion}.
 */
public class EmailAlertPropertiesDialog extends JDialog {
    public static final String TITLE = "Email Alert Properties";
    private JPanel mainPanel;
    private JTextField addressField;
    private JTextField fromAddressField;
    private JTextField hostField;
    private JTextField portField;
    private JTextField subjectField;
    private JTextArea messageField;
    private JButton okButton;
    private JButton cancelButton;

    private final EmailAlertAssertion assertion;
    private boolean confirmed = false;

    public EmailAlertPropertiesDialog(Dialog owner, EmailAlertAssertion ass) throws HeadlessException {
        super(owner, TITLE, true);
        this.assertion = ass;
        initialize();
    }

    public EmailAlertPropertiesDialog(Frame owner, EmailAlertAssertion ass) throws HeadlessException {
        super(owner, TITLE, true);
        this.assertion = ass;
        initialize();
    }

    private void initialize() {
        setContentPane(mainPanel);

        Utilities.equalizeButtonSizes(new AbstractButton[] { okButton, cancelButton });

        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                viewToModel();
                confirmed = true;
                hide();
                dispose();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                confirmed = false;
                hide();
                dispose();
            }
        });

        portField.setDocument(new NumberField(6));
        Utilities.constrainTextFieldToIntegerRange(portField, 1, 65535);

        final DocumentListener dl = new DocumentListener() {
                    public void changedUpdate(DocumentEvent e) { updateEnableDisableState(); }
                    public void insertUpdate(DocumentEvent e) { updateEnableDisableState(); }
                    public void removeUpdate(DocumentEvent e) { updateEnableDisableState(); }
                };
        hostField.getDocument().addDocumentListener(dl);
        addressField.getDocument().addDocumentListener(dl);
        portField.getDocument().addDocumentListener(dl);

        pack();
        Utilities.centerOnScreen(this);
        modelToView();
    }

    private void modelToView() {
        hostField.setText(assertion.getSmtpHost());
        portField.setText(Integer.toString(assertion.getSmtpPort()));
        addressField.setText(assertion.getTargetEmailAddress());
        subjectField.setText(assertion.getSubject());
        messageField.setText(assertion.getMessage());
        fromAddressField.setText(assertion.getSourceEmailAddress());
    }

    private void viewToModel() {
        assertion.setSmtpHost(hostField.getText());
        assertion.setSmtpPort(safeParseInt(portField.getText(), EmailAlertAssertion.DEFAULT_PORT));
        assertion.setTargetEmailAddress(addressField.getText());
        assertion.setSubject(subjectField.getText());
        assertion.setMessage(messageField.getText());
        assertion.setSourceEmailAddress(fromAddressField.getText());
    }

    private void updateEnableDisableState() {
        boolean ok = true;
        if (addressField.getText().length() < 1) ok = false;
        if (hostField.getText().length() < 1) ok = false;
        if (fromAddressField.getText().length() < 1) ok = false;
        if (!isValidInt(portField.getText())) ok = false;
        int port = safeParseInt(portField.getText(), EmailAlertAssertion.DEFAULT_PORT);
        if (port < 1 || port > 65535) ok = false;

        okButton.setEnabled(ok);
    }

    private int safeParseInt(String str, int def) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException nfe) {
            return def;
        }
    }

    private boolean isValidInt(String text) {
        try {
            Integer.parseInt(text);
            return true;
        } catch (NumberFormatException nfe) {
            return false;
        }
    }

    public EmailAlertAssertion getResult() {
        return confirmed ? assertion : null;
    }

    public static void main(String[] args) {
        Frame f = new JFrame();
        f.show();
        EmailAlertAssertion ass = new EmailAlertAssertion();
        EmailAlertPropertiesDialog d = new EmailAlertPropertiesDialog(f, ass);
        d.show();
        d.dispose();
        System.out.println("Got object: " + d.getResult());
        f.dispose();
    }
}
