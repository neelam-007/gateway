package com.l7tech.console.panels;

import com.l7tech.service.PublishedService;

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
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
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
        mainjPanel = new JPanel();
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

        rigidAreajPanel = new JPanel();

        setLayout(new BorderLayout());

        serviceUrljPanel.setLayout(new BoxLayout(serviceUrljPanel, BoxLayout.X_AXIS));

        panelTitlejLabel.setText("Credentials/Transport");
        serviceUrljPanel.add(panelTitlejLabel);

        serviceUrljPanel.add(jPanel3);

        add(serviceUrljPanel, BorderLayout.NORTH);

        mainjPanel.setLayout(new BoxLayout(mainjPanel, BoxLayout.X_AXIS));

        mainjPanel.setBorder(new EmptyBorder(new Insets(10, 10, 10, 10)));
        credentialsAndTransportjPanel.setLayout(new GridBagLayout());

        authenticationjPanel.setLayout(new BoxLayout(authenticationjPanel, BoxLayout.X_AXIS));

        authenticationjPanel.setBorder(new EmptyBorder(new Insets(5, 5, 5, 0)));
        credentialsjLabel.setText("Credentials");
        credentialsjLabel.setBorder(new EmptyBorder(new Insets(1, 1, 1, 5)));
        authenticationjPanel.add(credentialsjLabel);

        authenticationMethodjComboBox.setModel(
                new DefaultComboBoxModel(new String[] {
                    "Anonymous (no authentication required)",
                    "Basic authentication (Transport/HTTP headers)"
                    }));
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

        mainjPanel.add(credentialsAndTransportjPanel);

        add(mainjPanel, BorderLayout.CENTER);
    }

    /** @return the wizard step description as string  */
    public String getDescription() {
        return "Enter the protected service credentials and credentials policy";
    }

    /**
     * Test whether the step is finished and it is safe to proceed to the next
     * one.
     * If the step is valid, the "Next" (or "Finish") button will be enabled.
     *
     * @return true if the panel is valid, false otherwis
     */
    public boolean isValid() {
        return true;
    }

    /** @return the wizard step label    */
    public String getStepLabel() {
        return "Service credentials";
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

    private JLabel identityjLabel;
    private JPanel authenticationjPanel;
    private JPanel serviceUrljPanel;

    private JPanel mainjPanel;
    private JTextField identityjTextField;
    
}
