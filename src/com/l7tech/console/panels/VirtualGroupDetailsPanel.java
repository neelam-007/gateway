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
    private JLabel emailPatternLabel;

    /**
     * Constructor
     */
    public VirtualGroupDetailsPanel(VirtualGroupPanel p) {
        virtualGroupPanel = p;
        initComponents();
        applyFormSecurity();
    }

    private void applyFormSecurity() {
        groupDescTextField.setEditable(virtualGroupPanel.getGroupFlags().canUpdateSome());
        x509SubjectDNTextField.setEditable(virtualGroupPanel.getGroupFlags().canUpdateSome());
        emailTextField.setEditable(virtualGroupPanel.getGroupFlags().canUpdateSome());
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

        // Bugzilla #1090 - disable the fields that cannot be tested in rel 3.0        
        emailTextField.setEnabled(false);
        emailPatternLabel.setEnabled(false);
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
