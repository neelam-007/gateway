package com.l7tech.console.panels;

import com.l7tech.policy.assertion.xmlsec.SamlSecurity;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * Dialog to view/edit the properties of a SamlSecurity assertion.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * <p/>
 * User: flascell<br/>
 * Date: Nov 26, 2004<br/>
 */
public class SamlSecurityPropertiesPanel extends JDialog {
    private JButton okbutton;
    private JButton cancelbutton;
    private JButton helpbutton;
    private JComboBox confirmationMethodComboBox;
    private JPanel mainPanel;
    private SamlSecurity subject;

    public SamlSecurityPropertiesPanel(Frame owner, SamlSecurity assertion) {
        super(owner, true);
        subject = assertion;
        initialize();
    }

    private void initialize() {
        setContentPane(mainPanel);
        setTitle("Saml Security Properties");

        okbutton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ok();
            }
        });

        cancelbutton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cancel();
            }
        });

        helpbutton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                help();
            }
        });

        confirmationMethodComboBox.setModel(new DefaultComboBoxModel(new String[]{"Holder-of-key",
                                                                                  "Sender-vouches",
                                                                                  "Any"}));
        confirmationMethodComboBox.setSelectedIndex(subject.getConfirmationMethodType());

    }

    private void ok() {
        subject.setConfirmationMethodType(confirmationMethodComboBox.getSelectedIndex());
        SamlSecurityPropertiesPanel.this.dispose();
    }

    private void help() {
        // todo
    }

    private void cancel() {
        SamlSecurityPropertiesPanel.this.dispose();
    }

    public static void main(String[] args) {
        SamlSecurity assertion = new SamlSecurity();
        assertion.setConfirmationMethodType(SamlSecurity.CONFIRMATION_METHOD_SENDER_VOUCHES);
        SamlSecurityPropertiesPanel me = new SamlSecurityPropertiesPanel(null, assertion);
        me.pack();
        me.show();
        System.exit(0);
    }
}
