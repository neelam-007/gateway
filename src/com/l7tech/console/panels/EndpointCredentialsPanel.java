package com.l7tech.console.panels;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import javax.swing.border.EmptyBorder;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.*;


/**
 * <code>EndpointCredentialsPanel</code> that represent a step in the wizard
 * <code>WizardStepPanel</code> that collects endpoint access control attributes.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 * @version 1.0
 */
public class EndpointCredentialsPanel extends WizardStepPanel {
    
    /** Creates new form ServicePanel */
    public EndpointCredentialsPanel() {
        initComponents();
    }
    
    /**
     * This method is called from within the constructor to
     * initialize the form.
     */
    private void initComponents() {
        GridBagConstraints gridBagConstraints;

        serviceUrljPanel = new JPanel();
        panelTitlejLabel = new JLabel();
        jPanel3 = new JPanel();
        jPanel1 = new JPanel();
        credentialsAndTransportjPanel = new JPanel();
        authenticationjPanel = new JPanel();
        credentialsjLabel = new JLabel();
        authenticationMethodjComboBox = new JComboBox();
        credentialsjPanel = new JPanel();
        identityjLabel = new JLabel();
        identityjTextField = new JTextField();
        passwordjLabel = new JLabel();
        jPasswordField1 = new JPasswordField();
        tlsjPanel = new JPanel();
        tlsjCheckBox = new JCheckBox();
        certificatejButton = new JButton();
        rigidAreajPanel = new JPanel();

        setLayout(new BorderLayout());

        serviceUrljPanel.setLayout(new BoxLayout(serviceUrljPanel, BoxLayout.X_AXIS));

        panelTitlejLabel.setText("Credentials/Transport");
        serviceUrljPanel.add(panelTitlejLabel);

        serviceUrljPanel.add(jPanel3);

        add(serviceUrljPanel, BorderLayout.NORTH);

        jPanel1.setLayout(new BoxLayout(jPanel1, BoxLayout.X_AXIS));

        jPanel1.setBorder(new EmptyBorder(new Insets(10, 10, 10, 10)));
        credentialsAndTransportjPanel.setLayout(new GridBagLayout());

        authenticationjPanel.setLayout(new BoxLayout(authenticationjPanel, BoxLayout.X_AXIS));

        authenticationjPanel.setBorder(new EmptyBorder(new Insets(5, 5, 5, 0)));
        credentialsjLabel.setText("Credentials");
        credentialsjLabel.setBorder(new EmptyBorder(new Insets(1, 1, 1, 5)));
        authenticationjPanel.add(credentialsjLabel);

        authenticationMethodjComboBox.setModel(
                new DefaultComboBoxModel(new String[] {
                    "Anonymous (no authentication required)",
                    "Basic authentication (Transport/HTTP headers)",
                    "Digest authentication (Transport/HTTP headers)",
                    "Basic authentication (SOAP message)",
                    "Digest authentication (SOAP message)",
                    "Client certificate (requires SSL)",
                    "Client certificate (SOAP message)" }));
        authenticationjPanel.add(authenticationMethodjComboBox);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        credentialsAndTransportjPanel.add(authenticationjPanel, gridBagConstraints);

        credentialsjPanel.setLayout(new BoxLayout(credentialsjPanel, BoxLayout.X_AXIS));

        credentialsjPanel.setBorder(new EmptyBorder(new Insets(5, 5, 5, 0)));
        identityjLabel.setText("Identity");
        identityjLabel.setBorder(new EmptyBorder(new Insets(0, 0, 0, 5)));
        credentialsjPanel.add(identityjLabel);

        identityjTextField.setText("enter identity");
        identityjTextField.setPreferredSize(new Dimension(100, 20));
        credentialsjPanel.add(identityjTextField);

        passwordjLabel.setText("Password");
        passwordjLabel.setBorder(new EmptyBorder(new Insets(1, 10, 1, 5)));
        credentialsjPanel.add(passwordjLabel);

        jPasswordField1.setPreferredSize(new Dimension(100, 20));
        credentialsjPanel.add(jPasswordField1);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new Insets(0, 0, 20, 0);
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        credentialsAndTransportjPanel.add(credentialsjPanel, gridBagConstraints);

        tlsjPanel.setLayout(new BoxLayout(tlsjPanel, BoxLayout.X_AXIS));

        tlsjPanel.setBorder(new EmptyBorder(new Insets(5, 5, 5, 0)));
        tlsjCheckBox.setText("SSL/TLS");
        tlsjCheckBox.setHorizontalTextPosition(SwingConstants.LEADING);
        tlsjCheckBox.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent evt) {
                tlsjCheckBoxStateChanged(evt);
            }
        });

        tlsjPanel.add(tlsjCheckBox);

        certificatejButton.setText("Client certificate");
        certificatejButton.setEnabled(false);
        certificatejButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                certificatejButtonActionPerformed(evt);
            }
        });

        tlsjPanel.add(certificatejButton);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        credentialsAndTransportjPanel.add(tlsjPanel, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        credentialsAndTransportjPanel.add(rigidAreajPanel, gridBagConstraints);

        jPanel1.add(credentialsAndTransportjPanel);

        add(jPanel1, BorderLayout.CENTER);
    }

    private void tlsjCheckBoxStateChanged(ChangeEvent evt) {
        // Add your handling code here:
        certificatejButton.setEnabled(tlsjCheckBox.isSelected());
    }
    
    private void certificatejButtonActionPerformed(ActionEvent evt) {
        JFileChooser chooser = new JFileChooser();
        int returnVal = chooser.showOpenDialog(this);
        if(returnVal == JFileChooser.APPROVE_OPTION) {
            System.out.println("The file is: " +  chooser.getSelectedFile().getName());
        }
    }
    
    public String getDescription() {
        return "Enter the protected service credentials, credentials policy"
        +" transport requirements, certificate";
    }

    private JComboBox authenticationMethodjComboBox;
    private JPasswordField jPasswordField1;
    private JLabel panelTitlejLabel;
    private JPanel credentialsAndTransportjPanel;
    private JPanel tlsjPanel;
    private JPanel jPanel3;
    private JLabel credentialsjLabel;
    private JPanel credentialsjPanel;
    private JLabel passwordjLabel;
    private JPanel rigidAreajPanel;
    private JButton certificatejButton;
    private JLabel identityjLabel;
    private JPanel authenticationjPanel;
    private JPanel serviceUrljPanel;
    private JCheckBox tlsjCheckBox;
    private JPanel jPanel1;
    private JTextField identityjTextField;
    
}
