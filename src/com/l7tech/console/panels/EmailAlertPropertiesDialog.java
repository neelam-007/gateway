/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.console.panels;

import static com.l7tech.policy.assertion.alert.EmailAlertAssertion.Protocol;
import com.l7tech.common.gui.NumberField;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.gui.util.InputValidator;
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
    private final InputValidator validator = new InputValidator(this, TITLE);
    private JPanel mainPanel;
    private JTextField toAddressesField;
    private JTextField fromAddressField;
    private JTextField hostField;
    private JTextField portField;
    private JTextField subjectField;
    private JTextArea messageField;
    private JButton okButton;
    private JButton cancelButton;
    private JComboBox protocolCombo;
    private JLabel authUsernameLabel;
    private JTextField authUsernameField;
    private JLabel authPasswordLabel;
    private JPasswordField authPasswordField;
    private JTextField ccAddressesField;
    private JTextField bccAddressesField;
    private JCheckBox authenticateCheckBox;

    private final boolean readOnly;
    private final EmailAlertAssertion assertion;
    private boolean confirmed = false;

    /**
     * Creates a new EmailAlertPropertiesDialog object backed by the provided EmailAlertAssertion object.
     *
     * @param owner The window that owns this dialog
     * @param ass The backing EmailAlertAssertion object
     * @throws HeadlessException
     */
    public EmailAlertPropertiesDialog(Dialog owner, EmailAlertAssertion ass, boolean readOnly) throws HeadlessException {
        super(owner, TITLE, true);
        this.readOnly = readOnly;
        this.assertion = ass;
        initialize();
    }

    /**
     * Creates a new EmailAlertPropertiesDialog object backed by the provided EmailAlertAssertion object.
     *
     * @param owner The window that owns this dialog
     * @param ass The backing EmailAlertAssertion object
     * @throws HeadlessException
     */
    public EmailAlertPropertiesDialog(Frame owner, EmailAlertAssertion ass, boolean readOnly) throws HeadlessException {
        super(owner, TITLE, true);
        this.readOnly = readOnly;
        this.assertion = ass;
        initialize();
    }

    /**
     * Initializes this dialog and sets the fields to the values from the EmailAlertAssertion object.
     */
    private void initialize() {
        setContentPane(mainPanel);

        Utilities.equalizeButtonSizes(new AbstractButton[] { okButton, cancelButton });

        validator.attachToButton(okButton, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                viewToModel();
                confirmed = true;
                dispose();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                confirmed = false;
                dispose();
            }
        });

        protocolCombo.setModel(new DefaultComboBoxModel(Protocol.values()));
        protocolCombo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                EmailAlertAssertion.Protocol proto = (EmailAlertAssertion.Protocol) protocolCombo.getSelectedItem();
                if (proto == Protocol.PLAIN || proto == Protocol.STARTTLS) {
                    portField.setText("25");
                } else if (proto == Protocol.SSL) {
                    portField.setText("465");
                }
                updateEnableDisableState();
            }
        });

        authenticateCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateEnableDisableState();
            }
        });

        portField.setDocument(new NumberField(6));
        validator.constrainTextFieldToNumberRange("port", portField, 1, 65535);

        pack();
        Utilities.centerOnScreen(this);
        modelToView();

        updateEnableDisableState();
        final DocumentListener dl = new DocumentListener() {
            public void changedUpdate(DocumentEvent e) { updateEnableDisableState(); }
            public void insertUpdate(DocumentEvent e) { updateEnableDisableState(); }
            public void removeUpdate(DocumentEvent e) { updateEnableDisableState(); }
        };
        hostField.getDocument().addDocumentListener(dl);
        toAddressesField.getDocument().addDocumentListener(dl);
        portField.getDocument().addDocumentListener(dl);
        authUsernameField.getDocument().addDocumentListener(dl);
        authPasswordField.getDocument().addDocumentListener(dl);
        fromAddressField.getDocument().addDocumentListener(dl);
        subjectField.getDocument().addDocumentListener(dl);
        messageField.getDocument().addDocumentListener(dl);
    }

    /**
     * Sets the fields to the values from the EmailAlertAssertion object.
     */
    private void modelToView() {
        hostField.setText(assertion.getSmtpHost());
        portField.setText(Integer.toString(assertion.getSmtpPort()));
        toAddressesField.setText(assertion.getTargetEmailAddress());
        ccAddressesField.setText(assertion.getTargetCCEmailAddress());
        bccAddressesField.setText(assertion.getTargetBCCEmailAddress());
        subjectField.setText(assertion.getSubject());
        messageField.setText(assertion.messageString());
        fromAddressField.setText(assertion.getSourceEmailAddress());
        protocolCombo.setSelectedItem(assertion.getProtocol());
        authenticateCheckBox.setSelected(assertion.isAuthenticate());
        authUsernameField.setText(assertion.getAuthUsername());
        authPasswordField.setText(assertion.getAuthPassword());
    }

    /**
     * Sets the EmailAlertAssertion properties to the values from the fields in this dialog.
     */
    private void viewToModel() {
        assertion.setSmtpHost(hostField.getText());
        assertion.setSmtpPort(safeParseInt(portField.getText(), EmailAlertAssertion.DEFAULT_PORT));
        assertion.setTargetEmailAddress(toAddressesField.getText());
        assertion.setTargetCCEmailAddress(ccAddressesField.getText());
        assertion.setTargetBCCEmailAddress(bccAddressesField.getText());
        assertion.setSubject(subjectField.getText());
        assertion.messageString(messageField.getText());
        assertion.setSourceEmailAddress(fromAddressField.getText());
        assertion.setProtocol((EmailAlertAssertion.Protocol) protocolCombo.getSelectedItem());

        final boolean authenticate = authenticateCheckBox.isSelected();
        assertion.setAuthenticate(authenticate);
        if (authenticate) {
            assertion.setAuthUsername(authUsernameField.getText());
            assertion.setAuthPassword(new String(authPasswordField.getPassword()));
        } else {
            assertion.setAuthUsername(null);
            assertion.setAuthPassword(null);
        }
    }

    /**
     * If the authentication combo box is set to none, then the username and password fields are disabled,
     * otherwise they are enabled. If all of the necessary fields have values, then the OK button is
     * enabled, otherwise it is diabled.
     */
    private void updateEnableDisableState() {
        boolean ok = true;
        if (toAddressesField.getText().length() < 1) ok = false;
        if (hostField.getText().length() < 1) ok = false;
        if (fromAddressField.getText().length() < 1) ok = false;
        if (!isValidInt(portField.getText())) ok = false;
        int port = safeParseInt(portField.getText(), EmailAlertAssertion.DEFAULT_PORT);
        if (port < 1 || port > 65535) ok = false;
        if (authenticateCheckBox.isSelected() && authUsernameField.getText().length() == 0) ok = false;

        authUsernameLabel.setEnabled(authenticateCheckBox.isSelected());
        authUsernameField.setEnabled(authenticateCheckBox.isSelected());
        authPasswordLabel.setEnabled(authenticateCheckBox.isSelected());
        authPasswordField.setEnabled(authenticateCheckBox.isSelected());

        okButton.setEnabled(!readOnly && ok);
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
        f.setVisible(true);
        EmailAlertAssertion ass = new EmailAlertAssertion();
        ass.setSmtpPort(27);
        EmailAlertPropertiesDialog d = new EmailAlertPropertiesDialog(f, ass, false);
        d.setVisible(true);
        d.dispose();
        System.out.println("Got object: " + d.getResult());
        f.dispose();
    }
}
