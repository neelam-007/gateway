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
    private static final String MATCH_EQUALS = "exactly match";
    private static final String MATCH_STARTSWITH = "begin with";

    private static final String[] MATCH_TYPES = new String[] { MATCH_EQUALS, MATCH_STARTSWITH };
    
    private JButton okButton;
    private JButton cancelButton;
    private JTextField localPathTextField;
    private JTextField soapActionTextField;
    private JTextField namespaceUriTextField;
    private JPanel policyAttachmentKeyPanel;
    private JComboBox cbMatchType;
    private JCheckBox cbLock;

    private PolicyAttachmentKey result = null;

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

        cbMatchType.setModel(new DefaultComboBoxModel(MATCH_TYPES));

        okButton.addActionListener(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                final String nsuri = namespaceUriTextField.getText();
                final String sa = soapActionTextField.getText();
                final String path = localPathTextField.getText();

                result = new PolicyAttachmentKey(nsuri, sa, path);
                result.setBeginsWithMatch(cbMatchType.getSelectedItem().equals(MATCH_STARTSWITH));
                result.setPersistent(cbLock.isSelected());
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
        Utilities.attachDefaultContextMenu(localPathTextField);
        Utilities.attachDefaultContextMenu(soapActionTextField);
        Utilities.attachDefaultContextMenu(namespaceUriTextField);
        Utilities.runActionOnEscapeKey(policyAttachmentKeyPanel, closeAction);
        setPolicyAttachmentKey(null);
        pack();
    }

    /**
     * @return the confirmed exact match {@link PolicyAttachmentKey}, or null if the dialog has not been OK'ed.
     *         The fields of this PAK will be null for fields not designated as "equals" by the user.
     */
    public PolicyAttachmentKey getPolicyAttachmentKey() {
        return result;
    }

    /**
     * @param pak the {@link PolicyAttachmentKey} data to use for exact match, or null to specify no exact-match PAK.
     *        Note that data from an exact match PAK takes precedence over data from any begins-with PAK.
     */
    public void setPolicyAttachmentKey(PolicyAttachmentKey pak) {
        if (pak == null) pak = new PolicyAttachmentKey();
        cbMatchType.setSelectedItem(pak.isBeginsWithMatch() ? MATCH_STARTSWITH : MATCH_EQUALS);
        cbLock.setSelected(pak.isPersistent());
        namespaceUriTextField.setText(cn(pak.getUri()));
        soapActionTextField.setText(cn(pak.getSoapAction()));
        localPathTextField.setText(cn(pak.getProxyUri()));
    }

    private static String cn(String s) {
        return s == null ? "" : s;
    }
}
