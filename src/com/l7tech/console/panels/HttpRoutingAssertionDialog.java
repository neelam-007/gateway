package com.l7tech.console.panels;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.xml.Wsdl;
import com.l7tech.console.event.PolicyEvent;
import com.l7tech.console.event.PolicyListener;
import com.l7tech.console.tree.ServiceNode;
import com.l7tech.console.action.Actions;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.HttpRoutingAssertion;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EventListener;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.rmi.RemoteException;
import java.net.URL;
import java.net.MalformedURLException;


/**
 * <code>HttpRoutingAssertionDialog</code> is the protected service
 * policy edit dialog.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class HttpRoutingAssertionDialog extends JDialog {
    static final Logger log = Logger.getLogger(LogonDialog.class.getName());
    private HttpRoutingAssertion assertion;
    private JButton cancelButton;
    private JPanel buttonPanel;
    private JButton okButton;
    private EventListenerList listenerList = new EventListenerList();
    private ServiceNode service;

    private JComboBox authenticationMethodComboBox;
    private JTextField identityTextField;
    private JTextField realmTextField;
    private JPasswordField identityPasswordField;
    private JCheckBox taiCredentialChaining;

    private JPanel credentialsPanel;
    private JPanel serviceUrlPanel;
    private JTextField serviceUrlTextField;
    private JPanel mainPanel;

    private JRadioButton passwordMethod;
    private JRadioButton samlMethod;
    private JSpinner expirySpinner;
    // private JCheckBox memebershipStatementCheck; // Bugzilla 1269, reenable for group membership attribute statement
    private JLabel expirySpinLabel;
    private JLabel identityLabel;
    private JLabel passwordLabel;
    private JLabel realmLabel;

    /**
     * Creates new form ServicePanel
     */
    public HttpRoutingAssertionDialog(Frame owner, HttpRoutingAssertion a, ServiceNode sn) {
        super(owner, true);
        setTitle("HTTP(S) Routing Properties");
        assertion = a;
        this.service = sn;
        initComponents();
        initFormData();
    }

    /**
     * add the PolicyListener
     * 
     * @param listener the PolicyListener
     */
    public void addPolicyListener(PolicyListener listener) {
        listenerList.add(PolicyListener.class, listener);
    }

    /**
     * remove the the PolicyListener
     * 
     * @param listener the PolicyListener
     */
    public void removePolicyListener(PolicyListener listener) {
        listenerList.remove(PolicyListener.class, listener);
    }

    /**
     * notfy the listeners
     * 
     * @param a the assertion
     */
    private void fireEventAssertionChanged(final Assertion a) {
        SwingUtilities.invokeLater(
          new Runnable() {
              public void run() {
                  int[] indices = new int[a.getParent().getChildren().indexOf(a)];
                  PolicyEvent event = new
                    PolicyEvent(this, new AssertionPath(a.getPath()), indices, new Assertion[]{a});
                  EventListener[] listeners = listenerList.getListeners(PolicyListener.class);
                  for (int i = 0; i < listeners.length; i++) {
                      ((PolicyListener)listeners[i]).assertionsChanged(event);
                  }
              }
          });
    }


    /**
     * This method is called from within the constructor to
     * initialize the form.
     */
    private void initComponents() {
        GridBagConstraints gridBagConstraints;
        mainPanel = new JPanel();
        credentialsPanel = new JPanel();

        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());
        getServiceUrlPanel().setBorder(BorderFactory.createEmptyBorder(10, 5, 5, 10));
        contentPane.add(getServiceUrlPanel(), BorderLayout.NORTH);
        Actions.setEscKeyStrokeDisposes(this);
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(new EmptyBorder(new Insets(10, 10, 10, 10)));


        credentialsPanel.setLayout(new GridBagLayout());
        credentialsPanel.setBorder(BorderFactory.createTitledBorder("Service Authentication"));
        ButtonGroup methodGroup = new ButtonGroup();

        passwordMethod = new JRadioButton("Identity Authentication");
        methodGroup.add(passwordMethod);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new Insets(0, 0, 20, 10);
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        credentialsPanel.add(passwordMethod, gridBagConstraints);

        JSeparator js = new JSeparator(JSeparator.VERTICAL);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.VERTICAL;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new Insets(2, 5, 2, 5);
        credentialsPanel.add(js, gridBagConstraints);


        samlMethod = new JRadioButton("SAML Security");
        methodGroup.add(samlMethod);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new Insets(0, 0, 20, 0);
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        credentialsPanel.add(samlMethod, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new Insets(0, 0, 20, 10);
        gridBagConstraints.anchor = GridBagConstraints.NORTH;
        credentialsPanel.add(getCredentialsPanel(), gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = GridBagConstraints.NORTH;
        gridBagConstraints.insets = new Insets(0, 0, 20, 10);
        credentialsPanel.add(getSamlAssertionPanel(), gridBagConstraints);

 /*       gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = GridBagConstraints.VERTICAL;
        credentialsPanel.add(Box.createVerticalGlue(), gridBagConstraints);

        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = GridBagConstraints.VERTICAL;
        credentialsPanel.add(Box.createVerticalGlue(), gridBagConstraints);*/

        mainPanel.add(credentialsPanel);

        // Add buttonPanel
        mainPanel.add(getButtonPanel());
        contentPane.add(mainPanel, BorderLayout.CENTER);
        
        // listeners
        samlMethod.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                updateEnableDisable();
            }
        });
        passwordMethod.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                updateEnableDisable();
            }
        });
        try {
            if (!service.getPublishedService().isSoap()) {
                samlMethod.setEnabled(false);
            }
        } catch (FindException e) {
            log.log(Level.WARNING, "cannot get corresponding service", e);
        } catch (RemoteException e) {
            log.log(Level.WARNING, "cannot get corresponding service", e);
        }
    }

    private void updateEnableDisable() {
        boolean saml = false;
        boolean password = true;
        if (samlMethod.isSelected()) {
            saml = true;
        }
        password = !saml;

        expirySpinner.setEnabled(saml);
        expirySpinLabel.setEnabled(saml);
        // memebershipStatementCheck.setEnabled(saml); //bugzilla 1269

        identityTextField.setEnabled(password);
        identityLabel.setEnabled(password);
        realmTextField.setEnabled(password);
        realmLabel.setEnabled(password);
        identityPasswordField.setEnabled(password);
        passwordLabel.setEnabled(password);
    }

    /**
     * get the credentials that were entered.
     * <p/>
     * todo: deal with certificates too
     * 
     * @return the credentialsd byte array
     */
    private byte[] getCredentials() {
        char[] cpass = identityPasswordField.getPassword();
        return String.valueOf(cpass).getBytes();
    }

    private JPanel getServiceUrlPanel() {
        if (serviceUrlPanel != null)
            return serviceUrlPanel;

        serviceUrlPanel = new JPanel();
        serviceUrlPanel.setLayout(new BoxLayout(serviceUrlPanel, BoxLayout.X_AXIS));

        JLabel serviceUrlLabel = new JLabel();
        serviceUrlLabel.setText("Service URL:");
        serviceUrlLabel.setBorder(new EmptyBorder(new Insets(1, 1, 1, 5)));
        serviceUrlPanel.add(serviceUrlLabel);

        serviceUrlTextField = new JTextField();
        serviceUrlTextField.setPreferredSize(new Dimension(300, 20));
        serviceUrlPanel.add(serviceUrlTextField);

        JButton buttonDefaultUrl = new JButton();
        buttonDefaultUrl.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (service != null) {

                    try {
                        Wsdl wsdl = service.getPublishedService().parsedWsdl();
                        String serviceURI = null;
                        if (wsdl != null) {
                            serviceURI = wsdl.getServiceURI();
                            serviceUrlTextField.setText(serviceURI);
                        } else {
                            log.log(Level.INFO, "Can't retrieve WSDL from the published service");
                        }
                    } catch (java.rmi.RemoteException re) {
                        log.log(Level.INFO, "HttpRoutingAssertionDialog", re);
                    } catch (FindException fe) {
                        log.log(Level.INFO, "HttpRoutingAssertionDialog", fe);
                    } catch (javax.wsdl.WSDLException we) {
                        log.log(Level.INFO, "HttpRoutingAssertionDialog", we);
                    }
                } else {
                    log.log(Level.INFO, "Can't find the service");
                }
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
          CredentialsLocation.getCredentialsLocationComboBox();

        return authenticationMethodComboBox;
    }

    private JPanel getCredentialsPanel() {
        JPanel credentialsPanel = new JPanel();

        credentialsPanel.setLayout(new BoxLayout(credentialsPanel, BoxLayout.Y_AXIS));
        credentialsPanel.setBorder(new EmptyBorder(new Insets(5, 5, 5, 0)));

        JPanel identityPanel = new JPanel();
        identityPanel.setLayout(new BoxLayout(identityPanel, BoxLayout.X_AXIS));
        identityLabel = new JLabel();
        identityLabel.setText("User Name:");
        identityPanel.add(identityLabel);
        identityPanel.add(Box.createRigidArea(new Dimension(20, 10)));

        identityTextField = new JTextField();
        identityLabel.setLabelFor(identityTextField);
        identityTextField.setPreferredSize(new Dimension(50, 20));
        identityPanel.add(identityTextField);

        identityPanel.add(Box.createGlue());
        credentialsPanel.add(identityPanel);
        credentialsPanel.add(Box.createRigidArea(new Dimension(20, 10)));


        JPanel passwordPanel = new JPanel();
        passwordPanel.setLayout(new BoxLayout(passwordPanel, BoxLayout.X_AXIS));

        passwordLabel = new JLabel();
        passwordLabel.setText("Password:");
        passwordPanel.add(passwordLabel);

        passwordPanel.add(Box.createRigidArea(new Dimension(20, 10)));

        identityPasswordField = new JPasswordField();
        passwordLabel.setLabelFor(identityPasswordField);
        identityPasswordField.setPreferredSize(new Dimension(50, 20));
        passwordPanel.add(identityPasswordField);
        passwordPanel.add(Box.createGlue());
        credentialsPanel.add(passwordPanel);
        credentialsPanel.add(Box.createRigidArea(new Dimension(20, 10)));

        JPanel realmPanel = new JPanel();
        realmPanel.setLayout(new BoxLayout(realmPanel, BoxLayout.X_AXIS));
        realmLabel = new JLabel();
        realmLabel.setText("Realm:");
        realmPanel.add(realmLabel);

        realmPanel.add(Box.createRigidArea(new Dimension(20, 10)));

        realmTextField = new JTextField();
        realmLabel.setLabelFor(realmTextField);
        realmTextField.setPreferredSize(new Dimension(50, 20));
        realmPanel.add(realmTextField);
        realmPanel.add(Box.createGlue());

        credentialsPanel.add(realmPanel);

        JPanel taiPanel = new JPanel();
        taiPanel.setLayout(new BoxLayout(taiPanel, BoxLayout.X_AXIS));

        taiCredentialChaining = new JCheckBox("TAI Identity Pass");
        taiCredentialChaining.setPreferredSize(new Dimension(50, 20));
        taiPanel.add(taiCredentialChaining);
        taiPanel.add(Box.createHorizontalGlue());

        credentialsPanel.add(Box.createVerticalStrut(20));
        credentialsPanel.add(taiPanel);

        Utilities.equalizeComponentSizes(
          new JComponent[]{realmLabel,
                           identityLabel,
                           passwordLabel
          });


        Utilities.equalizeComponentWidth(
          new JComponent[]{identityPasswordField,
                           realmTextField,
                           identityTextField,
                           getAuthenticationMethodComboBox(),
                           taiCredentialChaining});

        return credentialsPanel;
    }


    private JPanel getSamlAssertionPanel() {
        JPanel samlPanel = new JPanel();
        samlPanel.setLayout(new BoxLayout(samlPanel, BoxLayout.Y_AXIS));
        samlPanel.setBorder(new EmptyBorder(new Insets(5, 5, 5, 0)));

        JPanel expiresPanel = new JPanel();
        expiresPanel.setLayout(new BoxLayout(expiresPanel, BoxLayout.X_AXIS));

        expirySpinner = new JSpinner();
        Integer value = new Integer(5);
        Integer min = new Integer(1);
        Integer max = new Integer(120);
        Integer step = new Integer(1);
        SpinnerNumberModel spinModel = new SpinnerNumberModel(value, min, max, step);
        expirySpinner.setModel(spinModel);
        expirySpinLabel = new JLabel("Ticket Expiry (in minutes):");
        expirySpinLabel.setLabelFor(expirySpinner);

        expiresPanel.add(expirySpinLabel);
        expiresPanel.add(expirySpinner);
        expiresPanel.add(Box.createGlue());

        samlPanel.add(expiresPanel);
        samlPanel.add(Box.createRigidArea(new Dimension(20, 10)));

        JPanel includeGroupsPanel = new JPanel();
        includeGroupsPanel.setLayout(new BoxLayout(includeGroupsPanel, BoxLayout.X_AXIS));
        //memebershipStatementCheck = new JCheckBox("Group Membership Statement"); // Bugzilla 1269
        // includeGroupsPanel.add(memebershipStatementCheck);
        includeGroupsPanel.add(Box.createGlue());
        samlPanel.add(includeGroupsPanel);
        samlPanel.add(Box.createGlue());
        return samlPanel;
    }


    /**
     * Returns buttonPanel
     */
    private JPanel getButtonPanel() {
        if (buttonPanel == null) {
            buttonPanel = new JPanel();
            buttonPanel.setLayout(new GridBagLayout());

            Component hStrut = Box.createHorizontalStrut(8);

            // add components
            buttonPanel.add(hStrut,
              new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0,
                GridBagConstraints.CENTER,
                GridBagConstraints.BOTH,
                new Insets(0, 0, 0, 0), 0, 0));

            buttonPanel.add(getOKButton(),
              new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER,
                GridBagConstraints.NONE,
                new Insets(5, 5, 5, 5), 0, 0));

            buttonPanel.add(getCancelButton(),
              new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER,
                GridBagConstraints.NONE,
                new Insets(5, 5, 5, 5), 0, 0));

            JButton buttons[] = new JButton[]
            {
                getOKButton(),
                getCancelButton()
            };
            Utilities.equalizeButtonSizes(buttons);
        }
        return buttonPanel;
    }


    /**
     * Returns okButton
     */
    private JButton getOKButton() {
        // If button not already created
        if (null == okButton) {
            // Create button
            okButton = new JButton("OK");

            // Register listener
            okButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    // check url before accepting
                    String url = serviceUrlTextField.getText();
                    boolean bad = false;
                    if (url == null || url.length() < 1) {
                        url = "<empty>";
                        bad = true;
                    }
                    try {
                        new URL(url);
                    } catch (MalformedURLException e1) {
                        bad = true;
                    }
                    if (bad) {
                        JOptionPane.showMessageDialog(okButton, "URL value " + url + " is not valid.");
                    } else {
                        assertion.setProtectedServiceUrl(url);
                        assertion.setLogin(identityTextField.getText());
                        assertion.setPassword(new String(getCredentials()));
                        assertion.setRealm(realmTextField.getText());
                        final Integer sv = (Integer)expirySpinner.getValue();
                        assertion.setSamlAssertionExpiry(sv.intValue());
                        //assertion.setGroupMembershipStatement(memebershipStatementCheck.isSelected()); // Bugzilla 1269
                        assertion.setAttachSamlSenderVouches(samlMethod.isSelected());
                        assertion.setTaiCredentialChaining(taiCredentialChaining.isSelected());
                        fireEventAssertionChanged(assertion);
                        HttpRoutingAssertionDialog.this.dispose();
                    }
                }
            });
        }

        // Return button
        return okButton;
    }

    /**
     * Returns cancelButton
     */
    private JButton getCancelButton() {
        // If button not already created
        if (null == cancelButton) {

            // Create button
            cancelButton = new JButton("Cancel");

            // Register listener
            cancelButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    HttpRoutingAssertionDialog.this.dispose();
                }
            });
        }
        // Return button
        return cancelButton;
    }

    private void initFormData() {
        identityTextField.setText(assertion.getLogin());

        identityPasswordField.setText(assertion.getPassword());
        realmTextField.setText(assertion.getRealm());
        serviceUrlTextField.setText(assertion.getProtectedServiceUrl());
        taiCredentialChaining.setSelected(assertion.isTaiCredentialChaining());

        //memebershipStatementCheck.setSelected(assertion.isGroupMembershipStatement()); // Bugzilla 1269
        int expiry = assertion.getSamlAssertionExpiry();
        if (expiry == 0) {
            expiry = 5;
        }
        expirySpinner.setValue(new Integer(expiry));
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                boolean saml = assertion.isAttachSamlSenderVouches();
                samlMethod.setSelected(saml);
                passwordMethod.setSelected(!saml);
            }
        });
    }
}
