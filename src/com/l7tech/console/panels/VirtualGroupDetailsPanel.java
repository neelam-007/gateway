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
    private JLabel emailPatternLabel;

    private final boolean canUpdate;

    /**
     * Constructor
     */
    public VirtualGroupDetailsPanel(boolean canUpdate) {
        this.canUpdate = canUpdate;
        initComponents();
        applyFormSecurity();
    }

    private void applyFormSecurity() {
        groupDescTextField.setEditable(canUpdate);
        x509SubjectDNTextField.setEditable(canUpdate);
        emailTextField.setEditable(canUpdate);
    }

    /**
     * This method is called from within the constructor to
     * initialize the dialog.
     */
    private void initComponents() {
        this.setLayout(new GridBagLayout());
        this.add(mainPanel, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0,
                GridBagConstraints.CENTER,
                GridBagConstraints.BOTH,
                new Insets(0, 0, 0, 0), 0, 0));
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
