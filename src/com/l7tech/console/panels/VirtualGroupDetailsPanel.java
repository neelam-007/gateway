package com.l7tech.console.panels;

import javax.swing.*;
import java.awt.*;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public class VirtualGroupDetailsPanel extends JPanel {

    private JPanel mainPanel;
    private JTextField groupDescTextField;
    private JTextField x509SubjectDNTextField;
    private JTextField emailTextField;
    private VirtualGroupPanel virtualGroupPanel;

    /**
     * Constructor
     */
    public VirtualGroupDetailsPanel(VirtualGroupPanel p) {
        virtualGroupPanel = p;
        initComponents();
        applyFormSecurity();
    }

    private void applyFormSecurity() {
        virtualGroupPanel.securityFormPreparer.prepare(new Component[]{
            groupDescTextField,
            x509SubjectDNTextField,
            emailTextField
        });

    }

    /**
     * This method is called from within the constructor to
     * initialize the dialog.
     */
    private void initComponents() {
        this.add(mainPanel);
    }

    public JTextField getGroupDescTextField() {
        return groupDescTextField;
    }

    public void setGroupDescTextField(JTextField groupDescTextField) {
        this.groupDescTextField = groupDescTextField;
    }

    public JTextField getX509SubjectDNTextField() {
        return x509SubjectDNTextField;
    }

    public void setX509SubjectDNTextField(JTextField x509SubjectDNTextField) {
        this.x509SubjectDNTextField = x509SubjectDNTextField;
    }

    public JTextField getEmailTextField() {
        return emailTextField;
    }

    public void setEmailTextField(JTextField emailTextField) {
        this.emailTextField = emailTextField;
    }

}
