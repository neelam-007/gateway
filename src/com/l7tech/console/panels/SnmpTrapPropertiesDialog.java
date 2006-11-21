/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.console.panels;

import com.l7tech.common.gui.util.InputValidator;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.policy.assertion.alert.SnmpTrapAssertion;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Properties dialog for {@link SnmpTrapAssertion}.
 */
public class SnmpTrapPropertiesDialog extends JDialog {
    public static final String TITLE = "SNMP Properties";

    private final InputValidator validator = new InputValidator(this, TITLE);
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

    private Window owner;

    private final SnmpTrapAssertion assertion;
    private boolean confirmed = false;

    public SnmpTrapPropertiesDialog(Dialog owner, SnmpTrapAssertion assertion) throws HeadlessException {
        super(owner, TITLE, true);
        this.owner = owner;
        if (assertion == null) throw new NullPointerException();
        this.assertion = assertion;
        init();
    }

    public SnmpTrapPropertiesDialog(Frame owner, SnmpTrapAssertion assertion) throws HeadlessException {
        super(owner, TITLE, true);
        this.owner = owner;
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

        validator.attachToButton(okButton, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (viewToModel()) {
                    confirmed = true;
                    dispose();
                }
            }
        });
        //validator.disableButtonWhenInvalid(okButton);

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                confirmed = false;
                dispose();
            }
        });

        ActionListener checkAction = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                portField.setEnabled(rbCustomPort.isSelected());
                validator.validate();
            }
        };
        rbDefaultPort.addActionListener(checkAction);
        rbCustomPort.addActionListener(checkAction);
        validator.constrainTextFieldToBeNonEmpty("host name", hostnameField, null);

        validator.constrainTextFieldToNumberRange("port", portField, 1, 65535);
        validator.constrainTextFieldToNumberRange("OID", oidField, 0, Integer.MAX_VALUE);

        pack();
        Utilities.centerOnScreen(this);
        modelToView();
    }

    private boolean viewToModel() {
        assertion.setCommunity(communityField.getText());
        assertion.setTargetHostname(hostnameField.getText());
        int port = SnmpTrapAssertion.DEFAULT_PORT;
        if (rbCustomPort.isSelected())
            port = safeParseInt(portField.getText(), port);
        assertion.setTargetPort(port);
        int oid = safeParseInt(oidField.getText(), 1);
        if (oid == 0) {
            JOptionPane.showMessageDialog(owner, "Last part of OID must not be zero.", "Invalid OID", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        assertion.setOid(oid);
        assertion.setErrorMessage(messageField.getText());
        return true;
    }

    private int safeParseInt(String str, int def) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException nfe) {
            return def;
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
        portField.setEnabled(rbCustomPort.isSelected());
        validator.validate();
    }

    /** @return the edited assertion if Ok was pressed, or null if the dialog was canceled or closed. */
    public SnmpTrapAssertion getResult() {
        return confirmed ? assertion : null;
    }
}
