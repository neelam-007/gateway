/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.gui.dialogs;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.proxy.datamodel.PolicyAttachmentKey;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * Dialog that obtains information about a PolicyAttachmentKey.
 */
public class PolicyAttachmentKeyDialog extends JDialog {
    private JButton okButton;
    private JButton cancelButton;
    private JTextField localPathTextField;
    private JTextField soapActionTextField;
    private JTextField namespaceUriTextField;
    private PolicyAttachmentKey result = null;
    private JPanel policyAttachmentKeyPanel;

    public PolicyAttachmentKeyDialog() throws HeadlessException {
        initialize();
    }

    public PolicyAttachmentKeyDialog(Frame owner, String title, boolean modal) throws HeadlessException {
        super(owner, title, modal);
        initialize();
    }

    public PolicyAttachmentKeyDialog(Dialog owner, String title, boolean modal) throws HeadlessException {
        super(owner, title, modal);
        initialize();
    }

    private void initialize() {
        setContentPane(policyAttachmentKeyPanel);

        okButton.addActionListener(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                result = new PolicyAttachmentKey(namespaceUriTextField.getText(),
                                                 soapActionTextField.getText(),
                                                 localPathTextField.getText());
                hide();
                dispose();
            }
        });
        getRootPane().setDefaultButton(okButton);

        final AbstractAction closeAction = new AbstractAction() {
                    public void actionPerformed(ActionEvent e) {
                        result = null;
                        hide();
                        dispose();
                    }
                };
        cancelButton.addActionListener(closeAction);
        Utilities.runActionOnEscapeKey(policyAttachmentKeyPanel, closeAction);
        pack();
    }

    /** @return the confirmed {@link PolicyAttachmentKey}, or null if the dialog has not been OK'ed. */
    public PolicyAttachmentKey getPolicyAttachmentKey() {
        return result;
    }

    /** @param pak the {@link PolicyAttachmentKey} data to load into the view. */
    public void setPolicyAttachmentKey(PolicyAttachmentKey pak) {
        namespaceUriTextField.setText(pak == null ? "" : pak.getUri());
        soapActionTextField.setText(pak == null ? "" : pak.getSoapAction());
        localPathTextField.setText(pak == null ? "" : pak.getProxyUri());
    }
}
