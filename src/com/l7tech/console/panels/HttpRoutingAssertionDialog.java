package com.l7tech.console.panels;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.gui.widgets.IpListPanel;
import com.l7tech.common.xml.Wsdl;
import com.l7tech.console.action.Actions;
import com.l7tech.console.event.PolicyEvent;
import com.l7tech.console.event.PolicyListener;
import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.HttpRoutingAssertion;
import com.l7tech.policy.assertion.RoutingAssertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.assertion.xmlsec.SecurityHeaderAddressable;
import com.l7tech.service.PublishedService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <code>HttpRoutingAssertionDialog</code> is the protected service
 * policy edit dialog.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class HttpRoutingAssertionDialog extends JDialog {
    private static final Logger log = Logger.getLogger(HttpRoutingAssertionDialog.class.getName());
    private HttpRoutingAssertion assertion;
    private JButton cancelButton;
    private JPanel buttonPanel;
    private JButton okButton;
    private EventListenerList listenerList = new EventListenerList();
    private PublishedService service;

    private JComboBox authenticationMethodComboBox;
    private JTextField identityTextField;
    private JTextField realmTextField;
    private JPasswordField identityPasswordField;
    private JCheckBox taiCredentialChaining;

    private JPanel serviceUrlPanel;
    private JTextField serviceUrlTextField;

    private JCheckBox cookiePropagationCheckBox;

    private JRadioButton passwordMethod;
    private JRadioButton samlMethod;
    private JSpinner expirySpinner;
    // private JCheckBox memebershipStatementCheck; // Bugzilla 1269, reenable for group membership attribute statement
    private JLabel expirySpinLabel;
    private JLabel identityLabel;
    private JLabel passwordLabel;
    private JLabel realmLabel;

    private JPanel xmlSecurityHeaderPanel;
    private JRadioButton promoteActorRadio;
    private JRadioButton removeSecHeaderRadio;
    private JRadioButton passthroughSecHeaderRadio;
    private JComboBox promoteActorCombo;
    private IpListPanel ipListPanel;

    /**
     * Creates new form ServicePanel
     */
    public HttpRoutingAssertionDialog(Frame owner, HttpRoutingAssertion a, PublishedService service) {
        super(owner, true);
        setTitle("HTTP(S) Routing Properties");
        assertion = a;
        this.service = service;
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
    void fireEventAssertionChanged(final Assertion a) {
        SwingUtilities.invokeLater(
          new Runnable() {
              public void run() {
                  if (a == null) return;
                  if (a.getParent() == null || a.getParent().getChildren() == null) return;
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
        JPanel mainPanel = new JPanel();
        JPanel cookiePanel = new JPanel();
        JPanel credentialsPanel = new JPanel();

        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());
        getServiceUrlPanel().setBorder(BorderFactory.createEmptyBorder(10, 5, 5, 10));
        contentPane.add(getServiceUrlPanel(), BorderLayout.NORTH);
        Actions.setEscKeyStrokeDisposes(this);
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(new EmptyBorder(new Insets(10, 10, 10, 10)));

        cookiePanel.setLayout(new GridBagLayout());
        cookiePanel.setBorder(BorderFactory.createTitledBorder("Cookie Policy"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new Insets(0, 0, 20, 10);
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        cookiePanel.add(getCookiePolicyPanel(), gridBagConstraints);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new Insets(0, 0, 20, 10);
        gridBagConstraints.weightx = 100.0;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.EAST;
        cookiePanel.add(Box.createGlue(), gridBagConstraints);
        mainPanel.add(cookiePanel);

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

        mainPanel.add(credentialsPanel);

        // add xml security header promotion/passthrough controls
        mainPanel.add(getSecHeaderPanel());

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
        if (!service.isSoap()) {
            samlMethod.setEnabled(false);
            promoteActorRadio.setEnabled(false);
            removeSecHeaderRadio.setEnabled(false);
            passthroughSecHeaderRadio.setEnabled(false);
            promoteActorCombo.setEnabled(false);
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

    private JPanel getSecHeaderPanel() {
        if (xmlSecurityHeaderPanel == null) {

            xmlSecurityHeaderPanel = new JPanel();
            xmlSecurityHeaderPanel.setBorder(BorderFactory.createTitledBorder("Current WSS header handling"));
            xmlSecurityHeaderPanel.setLayout(new BoxLayout(xmlSecurityHeaderPanel, BoxLayout.Y_AXIS));

            ActionListener disenableCombo = new ActionListener() {
                                                public void actionPerformed(ActionEvent e) {
                                                    if (promoteActorRadio.isSelected()) {
                                                        promoteActorCombo.setEnabled(true);
                                                    } else {
                                                        promoteActorCombo.setEnabled(false);
                                                    }
                                                }
                                            };


            removeSecHeaderRadio = new JRadioButton("Remove processed Security header from request before routing");
            removeSecHeaderRadio.addActionListener(disenableCombo);
            JPanel temp = new JPanel();
            temp.setLayout(new BorderLayout());
            temp.add(removeSecHeaderRadio, BorderLayout.NORTH);
            xmlSecurityHeaderPanel.add(temp);

            passthroughSecHeaderRadio = new JRadioButton("Leave current Security header in request before routing");
            passthroughSecHeaderRadio.addActionListener(disenableCombo);
            temp = new JPanel();
            temp.setLayout(new BorderLayout());
            temp.add(passthroughSecHeaderRadio, BorderLayout.NORTH);
            xmlSecurityHeaderPanel.add(temp);

            JPanel actorPromotionPanel = new JPanel();
            actorPromotionPanel.setLayout(new BorderLayout());
            promoteActorRadio = new JRadioButton("Promote other Security header as default before routing");
            promoteActorCombo = new JComboBox();
            promoteActorCombo.setEditable(true);
            actorPromotionPanel.add(promoteActorRadio, BorderLayout.NORTH);
            actorPromotionPanel.add(promoteActorCombo, BorderLayout.SOUTH);
            promoteActorRadio.addActionListener(disenableCombo);
            xmlSecurityHeaderPanel.add(actorPromotionPanel);
            ButtonGroup bg = new ButtonGroup();
            bg.add(removeSecHeaderRadio);
            bg.add(passthroughSecHeaderRadio);
            bg.add(promoteActorRadio);
        }
        return xmlSecurityHeaderPanel;
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

    private IpListPanel getIpListPanel() {
        if (ipListPanel != null)
            return ipListPanel;

        ipListPanel = new IpListPanel();
        return ipListPanel;
    }

    private JPanel getServiceUrlPanel() {
        if (serviceUrlPanel != null)
            return serviceUrlPanel;

        serviceUrlPanel = new JPanel();
        serviceUrlPanel.setLayout(new BoxLayout(serviceUrlPanel, BoxLayout.Y_AXIS));
        JPanel urlBarPanel = new JPanel();
        urlBarPanel.setLayout(new BoxLayout(urlBarPanel, BoxLayout.X_AXIS));
        serviceUrlPanel.add(urlBarPanel);
        serviceUrlPanel.add(getIpListPanel());

        JLabel serviceUrlLabel = new JLabel();
        serviceUrlLabel.setText("Service URL:");
        serviceUrlLabel.setBorder(new EmptyBorder(new Insets(1, 1, 1, 5)));
        urlBarPanel.add(serviceUrlLabel);

        serviceUrlTextField = new JTextField();
        serviceUrlTextField.setPreferredSize(new Dimension(300, 20));
        urlBarPanel.add(serviceUrlTextField);

        JButton buttonDefaultUrl = new JButton();
        buttonDefaultUrl.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (service != null) {

                    try {
                        Wsdl wsdl = service.parsedWsdl();
                        String serviceURI = null;
                        if (wsdl != null) {
                            serviceURI = wsdl.getServiceURI();
                            serviceUrlTextField.setText(serviceURI);
                        } else {
                            log.log(Level.INFO, "Can't retrieve WSDL from the published service");
                        }
                    } catch (javax.wsdl.WSDLException we) {
                        log.log(Level.INFO, "HttpRoutingAssertionDialog", we);
                    }
                } else {
                    log.log(Level.INFO, "Can't find the service");
                }
            }
        });
        buttonDefaultUrl.setText("Default");
        urlBarPanel.add(buttonDefaultUrl);

        return urlBarPanel;
    }

    private JPanel getCookiePolicyPanel() {
        JPanel cookiePanel = new JPanel();

        cookiePanel.setLayout(new BoxLayout(cookiePanel, BoxLayout.X_AXIS));
        cookiePanel.setBorder(new EmptyBorder(new Insets(5, 5, 5, 0)));

        cookiePropagationCheckBox = new JCheckBox();
        cookiePropagationCheckBox.setText("Enable HTTP Cookies");
        cookiePanel.add(cookiePropagationCheckBox);

        return cookiePanel;
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

                        if (getIpListPanel().isAddressesEnabled()) {
                            assertion.setCustomIpAddresses(getIpListPanel().getAddresses());
                            assertion.setFailoverStrategyName(getIpListPanel().getFailoverStrategyName());
                        } else {
                            assertion.setCustomIpAddresses(null);
                            assertion.setFailoverStrategyName(getIpListPanel().getFailoverStrategyName());
                        }

                        if (promoteActorRadio.isSelected()) {
                            String currentVal = (String)promoteActorCombo.getSelectedItem();
                            if (currentVal != null && currentVal.length() > 0) {
                                assertion.setXmlSecurityActorToPromote(currentVal);
                                assertion.setCurrentSecurityHeaderHandling(RoutingAssertion.PROMOTE_OTHER_SECURITY_HEADER);
                            } else {
                                JOptionPane.showMessageDialog(okButton, "The security actor to promote must be set.");
                                return;
                            }
                        } else if (removeSecHeaderRadio.isSelected()) {
                            assertion.setCurrentSecurityHeaderHandling(RoutingAssertion.REMOVE_CURRENT_SECURITY_HEADER);
                            assertion.setXmlSecurityActorToPromote(null);
                        } else if (passthroughSecHeaderRadio.isSelected()) {
                            assertion.setCurrentSecurityHeaderHandling(RoutingAssertion.LEAVE_CURRENT_SECURITY_HEADER_AS_IS);
                            assertion.setXmlSecurityActorToPromote(null);
                        }

                        assertion.setCopyCookies(cookiePropagationCheckBox.isSelected());

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
        getIpListPanel().setAddressesEnabled(assertion.getCustomIpAddresses() != null);
        getIpListPanel().setAddresses(assertion.getCustomIpAddresses());
        getIpListPanel().setFailoverStrategyName(assertion.getFailoverStrategyName());

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

        // read actor promotion information
        java.util.List existingActors = listExistingXmlSecurityRecipientContextFromPolicy();
        for (Iterator iterator = existingActors.iterator(); iterator.hasNext();) {
            String s = (String) iterator.next();
            ((DefaultComboBoxModel)promoteActorCombo.getModel()).addElement(s);
        }
        // todo set initial values based on new routing setting
        if (assertion.getCurrentSecurityHeaderHandling() == RoutingAssertion.REMOVE_CURRENT_SECURITY_HEADER) {
            promoteActorRadio.setSelected(false);
            promoteActorCombo.setEnabled(false);
            removeSecHeaderRadio.setSelected(true);
            passthroughSecHeaderRadio.setSelected(false);
        } else if (assertion.getCurrentSecurityHeaderHandling() == RoutingAssertion.LEAVE_CURRENT_SECURITY_HEADER_AS_IS) {
            promoteActorRadio.setSelected(false);
            promoteActorCombo.setEnabled(false);
            removeSecHeaderRadio.setSelected(false);
            passthroughSecHeaderRadio.setSelected(true);
        } else {
            promoteActorRadio.setSelected(true);
            removeSecHeaderRadio.setSelected(false);
            passthroughSecHeaderRadio.setSelected(false);
            promoteActorCombo.setEnabled(true);
            promoteActorCombo.getModel().setSelectedItem(assertion.getXmlSecurityActorToPromote());
        }

        cookiePropagationCheckBox.setSelected(assertion.isCopyCookies());
    }

    /**
     * @return a list of string objects; one for each different actor referenced from this policy
     */
    private java.util.List listExistingXmlSecurityRecipientContextFromPolicy() {
        ArrayList output = new ArrayList();
        // get to root of policy
        Assertion root = assertion;
        while (root.getParent() != null) {
            root = root.getParent();
        }
        populateXmlSecurityRecipientContext(root, output);

        return output;
    }

    private void populateXmlSecurityRecipientContext(Assertion toInspect, java.util.List receptacle) {
        if (toInspect instanceof CompositeAssertion) {
            CompositeAssertion ca = (CompositeAssertion)toInspect;
            for (Iterator i = ca.children(); i.hasNext();) {
                Assertion a = (Assertion)i.next();
                populateXmlSecurityRecipientContext(a, receptacle);
            }
        } else if (toInspect instanceof SecurityHeaderAddressable) {
            SecurityHeaderAddressable xsecass = (SecurityHeaderAddressable)toInspect;
            if (!xsecass.getRecipientContext().localRecipient()) {
                String existingactor = xsecass.getRecipientContext().getActor();
                if (!receptacle.contains(existingactor)) {
                    receptacle.add(existingactor);
                }
            }
        }
    }
}
