package com.l7tech.console.panels;

import com.l7tech.service.PublishedService;
import com.l7tech.credential.PrincipalCredentials;
import com.l7tech.credential.CredentialFormat;
import com.l7tech.identity.User;
import com.l7tech.policy.assertion.credential.CredentialSourceAssertion;
import com.l7tech.policy.assertion.credential.wss.WssBasic;
import com.l7tech.policy.assertion.credential.wss.WssDigest;
import com.l7tech.policy.assertion.credential.wss.WssClientCert;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.credential.http.HttpDigest;
import com.l7tech.policy.assertion.credential.http.HttpClientCert;
import com.l7tech.policy.assertion.RoutingAssertion;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.net.MalformedURLException;


/**
 * <code>EndpointCredentialsPanel</code> that represent a step in the wizard
 * <code>WizardStepPanel</code> that collects endpoint access control attributes.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class EndpointCredentialsPanel extends WizardStepPanel {
    private PublishedService service = new PublishedService();
    private boolean isValid = true;
    private JCheckBox anonymousAccessCheckBox;


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
        mainPanel = new JPanel();
        credentialsAndTransportPanel = new JPanel();

        setLayout(new BorderLayout());

        add(getServiceUrlPanel(), BorderLayout.NORTH);
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.X_AXIS));
        mainPanel.setBorder(new EmptyBorder(new Insets(10, 10, 10, 10)));

        credentialsAndTransportPanel.setLayout(new GridBagLayout());


        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new Insets(0, 0, 20, 0);
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        credentialsAndTransportPanel.add(getCredentialsPanel(), gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        credentialsAndTransportPanel.add(Box.createGlue(), gridBagConstraints);

        mainPanel.add(credentialsAndTransportPanel);

        add(mainPanel, BorderLayout.CENTER);
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
        return isValid;
    }

    /**
     * Provides the wizard with the current data--either
     * the default data or already-modified settings. This is a
     * noop version that subclasses implement.
     *
     * @param settings the object representing wizard panel state
     * @exception IllegalArgumentException if the the data provided
     * by the wizard are not valid.
     */
    public void readSettings(Object settings) throws IllegalArgumentException {
        PublishServiceWizard.ServiceAndAssertion
          collect = (PublishServiceWizard.ServiceAndAssertion)settings;
        if (!anonymousAccessCheckBox.isSelected()) {
            collect.setRoutingAssertion(null);
        }
        User us = new User(); // why is this User?
        us.setLogin(identityTextField.getText());

        PrincipalCredentials pc =
          new PrincipalCredentials(us,
            getCredentials(),
            getCredentialFormat(),
            realmTextField.getText());
        RoutingAssertion ra =
          new RoutingAssertion(serviceUrlTextField.getText(), pc);
        collect.setRoutingAssertion(ra);
    }

    /**
     * get the credentials that were entered.
     *
     * todo: deal with certificates too
     * @return the credentialsd byte array
     */
    private byte[] getCredentials() {
        char[] cpass = identityPasswordField.getPassword();
        return String.valueOf(cpass).getBytes();
    }

    /**
     * map the credential location UI selection to the
     * corresponding <code>CredentialFormat</code>
     * @return
     */
    private CredentialFormat getCredentialFormat() {
        Object o = getAuthenticationMethodComboBox().getSelectedItem();
        if (o != null) {
            CredentialSourceAssertion ca =
              (CredentialSourceAssertion)Components.getCredentialsLocationMap().get(o);
            // map to the credentialformat
            // todo: extract this into the utility routine and find the place for it
            if (ca instanceof HttpBasic ||
              ca instanceof WssBasic)
                return CredentialFormat.BASIC;
            else if (ca instanceof HttpDigest ||
              ca instanceof WssDigest)
                return CredentialFormat.DIGEST;
            else if (ca instanceof HttpClientCert ||
              ca instanceof WssClientCert)
                return CredentialFormat.DIGEST;
        }
        // default
        return CredentialFormat.BASIC;
    }

    /**
     * Provides the wizard panel with the opportunity to update the
     * settings with its current customized state.
     * Rather than updating its settings with every change in the GUI,
     * it should collect them, and then only save them when requested to
     * by this method.
     *
     * This is a noop version that subclasses implement.
     *
     * @exception IllegalArgumentException if the the data provided
     * by the wizard are not valid.
     * @param settings the object representing wizard panel state
     */
    public void storeSettings(Object settings) throws IllegalArgumentException {
        if (!(settings instanceof PublishServiceWizard.ServiceAndAssertion)) {
            throw new IllegalArgumentException();
        }
        try {
            PublishServiceWizard.ServiceAndAssertion
              sa = (PublishServiceWizard.ServiceAndAssertion)settings;
            PublishedService publishedService = sa.getService();
            service.setWsdlUrl(publishedService.getWsdlUrl());
            String text = serviceUrlTextField.getText();
            if (text == null || "".equals(text)) {
                serviceUrlTextField.setText(service.getWsdlUrl());
            }

        } catch (MalformedURLException e) {
        }

    }


    /** @return the wizard step label    */
    public String getStepLabel() {
        return "Service credentials";
    }


    private JPanel getServiceUrlPanel() {
        serviceUrlPanel = new JPanel();
        serviceUrlPanel.setLayout(new BoxLayout(serviceUrlPanel, BoxLayout.X_AXIS));

        JLabel serviceUrlLabel = new JLabel();
        serviceUrlLabel.setText("Service URL");
        serviceUrlLabel.setBorder(new EmptyBorder(new Insets(1, 1, 1, 5)));
        serviceUrlPanel.add(serviceUrlLabel);

        serviceUrlTextField = new JTextField();
        serviceUrlTextField.setText("");
        serviceUrlTextField.setPreferredSize(new Dimension(100, 20));
        serviceUrlPanel.add(serviceUrlTextField);

        JButton buttonDefaultUrl = new JButton();
        buttonDefaultUrl.addActionListener( new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                serviceUrlTextField.setText(service.getWsdlUrl());
            }
        });
        buttonDefaultUrl.setText("Default");
        serviceUrlPanel.add(buttonDefaultUrl);

        return serviceUrlPanel;
    }


    private JComboBox getAuthenticationMethodComboBox() {
        if (authenticationMethodComboBox != null)
            return authenticationMethodComboBox;
        authenticationMethodComboBox =
          Components.getCredentialsLocationComboBox();

        return authenticationMethodComboBox;
    }

    private JPanel getCredentialsPanel() {
        JPanel credentialsPanel = new JPanel();

        credentialsPanel.setLayout(new BoxLayout(credentialsPanel, BoxLayout.Y_AXIS));
        credentialsPanel.setBorder(new EmptyBorder(new Insets(5, 5, 5, 0)));

        JPanel authMethodPanel = new JPanel();
        authMethodPanel.setLayout(new BoxLayout(authMethodPanel, BoxLayout.X_AXIS));
        JLabel credentialsLabel = new JLabel();
        credentialsLabel.setText("Credentials");
        authMethodPanel.add(credentialsLabel);
        authMethodPanel.add(Box.createRigidArea(new Dimension(20, 10)));

        authMethodPanel.add(getAuthenticationMethodComboBox());

        authMethodPanel.add(Box.createRigidArea(new Dimension(20, 10)));

        anonymousAccessCheckBox = new JCheckBox();
        anonymousAccessCheckBox.setText("Anonymous access");
        anonymousAccessCheckBox.setHorizontalTextPosition(SwingConstants.TRAILING);
        anonymousAccessCheckBox.
          addActionListener(new ActionListener() {
              /** Invoked when an action occurs. */
              public void actionPerformed(ActionEvent e) {
                  JCheckBox cb = (JCheckBox)e.getSource();
                  boolean enable = !cb.isSelected();
                  getAuthenticationMethodComboBox().setEnabled(enable);
                  identityTextField.setEnabled(enable);
                  identityPasswordField.setEnabled(enable);
                  realmTextField.setEnabled(enable);
              }
          });
        // default disable
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                anonymousAccessCheckBox.setSelected(false);
            }
        });

        authMethodPanel.add(anonymousAccessCheckBox);
        credentialsPanel.add(authMethodPanel);

        credentialsPanel.add(Box.createRigidArea(new Dimension(20, 10)));

        JPanel realmPanel = new JPanel();
        realmPanel.setLayout(new BoxLayout(realmPanel, BoxLayout.X_AXIS));
        JLabel realmLabel = new JLabel();
        realmLabel.setText("Realm");
        realmPanel.add(realmLabel);

        realmPanel.add(Box.createRigidArea(new Dimension(20, 10)));

        realmTextField = new JTextField();
        realmTextField.setPreferredSize(new Dimension(50, 20));
        realmPanel.add(realmTextField);

        realmPanel.add(Box.createGlue());
        credentialsPanel.add(realmPanel);
        credentialsPanel.add(Box.createRigidArea(new Dimension(20, 10)));


        JPanel identityPanel = new JPanel();
        identityPanel.setLayout(new BoxLayout(identityPanel, BoxLayout.X_AXIS));
        JLabel identityLabel = new JLabel();
        identityLabel.setText("Identity");
        identityPanel.add(identityLabel);

        identityPanel.add(Box.createRigidArea(new Dimension(20, 10)));

        identityTextField = new JTextField();
        identityTextField.setPreferredSize(new Dimension(50, 20));
        identityPanel.add(identityTextField);

        identityPanel.add(Box.createGlue());
        credentialsPanel.add(identityPanel);

        credentialsPanel.add(Box.createRigidArea(new Dimension(20, 10)));


        JPanel passwordPanel = new JPanel();
        passwordPanel.setLayout(new BoxLayout(passwordPanel, BoxLayout.X_AXIS));

        JLabel passwordLabel = new JLabel();
        passwordLabel.setText("Password");
        passwordPanel.add(passwordLabel);

        passwordPanel.add(Box.createRigidArea(new Dimension(20, 10)));

        identityPasswordField = new JPasswordField();
        identityPasswordField.setPreferredSize(new Dimension(50, 20));
        passwordPanel.add(identityPasswordField);
        passwordPanel.add(Box.createGlue());
        credentialsPanel.add(passwordPanel);

        Utilities.equalizeComponentSizes(
          new JComponent[]{credentialsLabel,
                           realmLabel,
                           identityLabel,
                           passwordLabel});


        Utilities.equalizeComponentWidth(
          new JComponent[]{identityPasswordField,
                           realmTextField,
                           identityTextField,
                           getAuthenticationMethodComboBox()});

        return credentialsPanel;
    }

    private JComboBox authenticationMethodComboBox;
    private JTextField identityTextField;
    private JTextField realmTextField;
    private JPasswordField identityPasswordField;
    private JPanel credentialsAndTransportPanel;
    private JPanel serviceUrlPanel;
    private JTextField serviceUrlTextField;
    private JPanel mainPanel;
}
