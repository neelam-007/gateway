/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.console.panels;

import com.l7tech.common.gui.NumberField;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.policy.assertion.alert.SnmpTrapAssertion;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Properties dialog for {@link SnmpTrapAssertion}.
 */
public class SnmpTrapPropertiesDialog extends JDialog {
    public static final String TITLE = "SNMP Properties";

    private JPanel rootPanel;
    private JRadioButton rbDefaultPort;
    private JTextField portField;
    private JRadioButton rbCustomPort;
    private JTextField oidField;
    private JTextField messageField;
    private JTextField communityField;
    private JTextField hostnameField;
    private JButton okButton;
    private JButton cancelButton;

    private final SnmpTrapAssertion assertion;
    private boolean confirmed = false;

    public SnmpTrapPropertiesDialog(Dialog owner, SnmpTrapAssertion assertion) throws HeadlessException {
        super(owner, TITLE, true);
        if (assertion == null) throw new NullPointerException();
        this.assertion = assertion;
        init();
    }

    public SnmpTrapPropertiesDialog(Frame owner, SnmpTrapAssertion assertion) throws HeadlessException {
        super(owner, TITLE, true);
        if (assertion == null) throw new NullPointerException();
        this.assertion = assertion;
        init();
    }

    private void init() {
        setContentPane(rootPanel);

        ButtonGroup bg = new ButtonGroup();
        bg.add(rbDefaultPort);
        bg.add(rbCustomPort);

        Utilities.enableGrayOnDisabled(portField);
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

        ActionListener checkAction = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateEnableDisableState();
            }
        };
        rbDefaultPort.addActionListener(checkAction);
        rbCustomPort.addActionListener(checkAction);
        hostnameField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) { updateEnableDisableState(); }
            public void insertUpdate(DocumentEvent e) { updateEnableDisableState(); }
            public void removeUpdate(DocumentEvent e) { updateEnableDisableState(); }
        });        

        portField.setDocument(new NumberField(6));
        oidField.setDocument(new NumberField(9));

        pack();
        Utilities.centerOnScreen(this);
        modelToView();
    }

    private void viewToModel() {
        assertion.setCommunity(communityField.getText());
        assertion.setTargetHostname(hostnameField.getText());
        int port = SnmpTrapAssertion.DEFAULT_PORT;
        if (rbCustomPort.isSelected())
            port = safeParseInt(portField.getText(), port);
        assertion.setTargetPort(port);
        assertion.setOid(safeParseInt(oidField.getText(), 0));
        assertion.setErrorMessage(messageField.getText());
    }

    private void updateEnableDisableState() {
        portField.setEnabled(rbCustomPort.isSelected());

        boolean ok = true;
        if (hostnameField.getText().length() < 1) ok = false;
        if (rbCustomPort.isSelected() && !isValidInt(portField.getText())) ok = false;
        int port = safeParseInt(portField.getText(), SnmpTrapAssertion.DEFAULT_PORT);
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

    private void modelToView() {
        portField.setText(Integer.toString(assertion.getTargetPort()));
        final boolean dp = assertion.getTargetPort() == SnmpTrapAssertion.DEFAULT_PORT;
        rbDefaultPort.setSelected(dp);
        rbCustomPort.setSelected(!dp);
        hostnameField.setText(assertion.getTargetHostname());
        messageField.setText(assertion.getErrorMessage());
        oidField.setText(Integer.toString(assertion.getOid()));
        communityField.setText(assertion.getCommunity());
        updateEnableDisableState();
    }

    /** @return the edited assertion if Ok was pressed, or null if the dialog was canceled or closed. */
    public SnmpTrapAssertion getResult() {
        return confirmed ? assertion : null;
    }

    public static void main(String[] args) {
        Frame f = new JFrame();
        f.show();
        SnmpTrapAssertion ass = new SnmpTrapAssertion("foo.bar.com", 0, "s3cr3t", 8787, "ERROR ERROR!");
        SnmpTrapPropertiesDialog d = new SnmpTrapPropertiesDialog(f, ass);
        d.show();
        d.dispose();
        System.out.println("Got object: " + d.getResult());
        f.dispose();
    }
}
