/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.console.panels;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.event.PolicyListener;
import com.l7tech.console.tree.ServiceNode;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.BridgeRoutingAssertion;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

/**
 * Allows properties of a {@link com.l7tech.policy.assertion.BridgeRoutingAssertion} to be edited.
 */
public class BridgeRoutingAssertionPropertiesDialog extends JDialog {
    private static final Logger log = Logger.getLogger(BridgeRoutingAssertionPropertiesDialog.class.getName());

    private JPanel rootPanel;
    private JRadioButton rbServerCertAutoDisco;
    private JRadioButton rbServerCertManual;
    private JLabel labelServerCertManual;
    private JButton buttonServerCertImport;
    private JRadioButton rbClientCertUseGatewaySsl;
    private JRadioButton rbClientCertManual;
    private JButton buttonClientCertImport;
    private JLabel labelClientCertManual;
    private JRadioButton rbPolicyAutoDisco;
    private JRadioButton rbPolicyManual;
    private JTextArea policyXmlText;
    private JButton buttonCancel;
    private JButton buttonOk;
    private JButton buttonHttpProperties;

    private final HttpRoutingAssertionDialog httpDialog;
    private final BridgeRoutingAssertion assertion;

    public BridgeRoutingAssertionPropertiesDialog(Frame owner, BridgeRoutingAssertion a, ServiceNode sn) {
        super(owner, true);
        setTitle("Bridge Routing Assertion Properties");
        this.assertion = a;

        setContentPane(rootPanel);

        httpDialog = new HttpRoutingAssertionDialog(owner, a, sn);
        httpDialog.setModal(true);
        httpDialog.pack();
        Utilities.centerOnScreen(httpDialog);

        buttonHttpProperties.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                httpDialog.show();
            }
        });

        Utilities.equalizeButtonSizes(new AbstractButton[] { buttonOk, buttonCancel });

        buttonOk.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                copyViewToModel();

                fireEventAssertionChanged(assertion);
                BridgeRoutingAssertionPropertiesDialog.this.dispose();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                httpDialog.dispose();
                BridgeRoutingAssertionPropertiesDialog.this.dispose();
            }
        });

        ActionListener updateEnableStates = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateEnableStates();
            }
        };

        ButtonGroup bg1 = new ButtonGroup();
        bg1.add(rbClientCertManual);
        bg1.add(rbClientCertUseGatewaySsl);
        rbClientCertManual.addActionListener(updateEnableStates);
        rbClientCertUseGatewaySsl.addActionListener(updateEnableStates);

        ButtonGroup bg2 = new ButtonGroup();
        bg2.add(rbPolicyAutoDisco);
        bg2.add(rbPolicyManual);
        rbPolicyAutoDisco.addActionListener(updateEnableStates);
        rbPolicyManual.addActionListener(updateEnableStates);

        ButtonGroup bg3 = new ButtonGroup();
        bg3.add(rbServerCertAutoDisco);
        bg3.add(rbServerCertManual);
        rbServerCertAutoDisco.addActionListener(updateEnableStates);
        rbServerCertManual.addActionListener(updateEnableStates);

        Utilities.enableGrayOnDisabled(policyXmlText);

        copyModelToView();
        updateEnableStates();
    }

    /** Update the GUI state to reflect the policy assertion settings. */
    private void copyModelToView() {
        // Update GUI state
        String policyXml = assertion.getPolicyXml();
        rbPolicyAutoDisco.setSelected(policyXml == null);
        rbPolicyManual.setSelected(policyXml != null);
        policyXmlText.setText(policyXml != null ? policyXml : "");
    }

    /** Update the policy assertion settings to reflect the GUI state. */
    private void copyViewToModel() {
        // Populate policy assertion settings
        if (rbPolicyManual.isSelected())
            assertion.setPolicyXml(policyXmlText.getText());
        else
            assertion.setPolicyXml(null);
    }

    private void updateEnableStates() {
        policyXmlText.setEnabled(rbPolicyManual.isSelected());
    }

    public void addPolicyListener(PolicyListener listener) {
        httpDialog.addPolicyListener(listener);
    }

    /**
     * notfy the listeners
     *
     * @param a the assertion
     */
    private void fireEventAssertionChanged(final Assertion a) {
        httpDialog.fireEventAssertionChanged(assertion);
    }
}
