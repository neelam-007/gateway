/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.external.assertions.snmptrap.console;

import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.console.panels.AssertionPropertiesEditorSupport;
import com.l7tech.external.assertions.snmptrap.SnmpTrapAssertion;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Properties dialog for {@link SnmpTrapAssertion}.
 */
public class SnmpTrapPropertiesDialog extends AssertionPropertiesEditorSupport<SnmpTrapAssertion> {
    private final InputValidator validator;
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

    private boolean confirmed = false;

    public SnmpTrapPropertiesDialog(Window owner, SnmpTrapAssertion assertion) throws HeadlessException {
        super(owner, assertion);
        validator = new InputValidator(this, getTitle());
        this.owner = owner;
        if (assertion == null) throw new NullPointerException();
        init(assertion);
    }

    private void init(SnmpTrapAssertion assertion) {
        setContentPane(rootPanel);

        ButtonGroup bg = new ButtonGroup();
        bg.add(rbDefaultPort);
        bg.add(rbCustomPort);

        Utilities.enableGrayOnDisabled(portField);
        Utilities.equalizeButtonSizes(new AbstractButton[] { okButton, cancelButton });

        validator.attachToButton(okButton, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (check()) {
                    confirmed = true;
                    dispose();
                }
            }
        });
        //validator.disableButtonWhenInvalid(okButton);

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                confirmed = false;
                dispose();
            }
        });

        ActionListener checkAction = new ActionListener() {
            @Override
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
        if (assertion != null) setData(assertion);
    }

    private boolean check() {
        String[] errs = validator.getAllValidationErrors();
        if (errs != null && errs.length > 0) {
            DialogDisplayer.showMessageDialog(owner, errs[0], "Error", JOptionPane.ERROR_MESSAGE, null);
            return false;
        }

        int oid = safeParseInt(oidField.getText(), 1);
        if (oid == 0) {
            DialogDisplayer.showMessageDialog(owner, "Last part of OID must not be zero.", "Invalid OID", JOptionPane.ERROR_MESSAGE, null);
            return false;
        }
        return true;
    }

    @Override
    public SnmpTrapAssertion getData(SnmpTrapAssertion assertion) {
        assertion.setCommunity(communityField.getText());
        assertion.setTargetHostname(hostnameField.getText());
        int port = SnmpTrapAssertion.DEFAULT_PORT;
        if (rbCustomPort.isSelected())
            port = safeParseInt(portField.getText(), port);
        assertion.setTargetPort(port);
        int oid = safeParseInt(oidField.getText(), 1);
        assertion.setOid(oid);
        assertion.setErrorMessage(messageField.getText());
        return assertion;
    }

    private int safeParseInt(String str, int def) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException nfe) {
            return def;
        }
    }

    @Override
    public void setData(SnmpTrapAssertion assertion) {
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

    @Override
    public boolean isConfirmed() {
        return confirmed;
    }

    @Override
    protected void configureView() {
        okButton.setEnabled( !isReadOnly() );
    }
}
