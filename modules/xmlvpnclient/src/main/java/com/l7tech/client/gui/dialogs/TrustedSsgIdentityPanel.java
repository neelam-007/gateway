/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.client.gui.dialogs;

import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.client.gui.util.IconManager;

import javax.swing.*;
import java.awt.*;

/**
 * Version of the Identity pane for trusted SSGs.
 */
public class TrustedSsgIdentityPanel extends SsgIdentityPanel {
    private JPanel mainPanel;
    private JTextField usernameTextField;
    private JPasswordField userPasswordField;
    private JButton clientCertButton;
    private JButton ssgCertButton;
    private JCheckBox useClientCredentialCheckBox;
    private JCheckBox useKerberosCredentialsCheckBox;
    private JCheckBox savePasswordCheckBox;
    private final Ssg ssg;

    public TrustedSsgIdentityPanel(Ssg ssg) {
        setLayout(new BorderLayout());
        add(mainPanel);
        this.ssg = ssg;
    }

    public JButton getClientCertButton() {
        return clientCertButton;
    }

    public JButton getSsgCertButton() {
        return ssgCertButton;
    }

    public ImageIcon getGeneralPaneImageIcon() {
        return ssg.isGeneric() ? IconManager.getSmallGenericServiceDiagram()
                               : IconManager.getSmallTrustedSsgDiagram();
    }

    public JCheckBox getUseClientCredentialCheckBox() {
        return useClientCredentialCheckBox;
    }

    public JCheckBox getUseKerberosCredentialCheckbox() {
        return useKerberosCredentialsCheckBox;   
    }

    public JCheckBox getSavePasswordCheckBox() {
        return savePasswordCheckBox;
    }

    public JTextField getUsernameTextField() {
        return usernameTextField;
    }

    public JPasswordField getUserPasswordField() {
        return userPasswordField;
    }
}
