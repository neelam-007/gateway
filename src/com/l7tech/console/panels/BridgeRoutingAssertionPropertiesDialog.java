/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.console.panels;

import com.l7tech.console.tree.ServiceNode;
import com.l7tech.policy.assertion.BridgeRoutingAssertion;

import javax.swing.*;
import java.awt.*;
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

    public BridgeRoutingAssertionPropertiesDialog(Frame owner, BridgeRoutingAssertion a, ServiceNode sn) {
        super(owner, true);

    }
}
