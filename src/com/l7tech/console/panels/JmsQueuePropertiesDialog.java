/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.console.panels;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.gui.widgets.OptionalCredentialsPanel;
import com.l7tech.common.transport.jms.JmsConnection;
import com.l7tech.common.transport.jms.JmsEndpoint;
import com.l7tech.common.transport.jms.JmsProvider;
import com.l7tech.common.util.Locator;
import com.l7tech.console.action.Actions;
import com.l7tech.console.security.RoleFormPreparer;
import com.l7tech.console.security.SecurityProvider;
import com.l7tech.console.util.Registry;
import com.l7tech.identity.Group;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.RemoteException;

/**
 * Dialog for configuring a JMS Queue (ie, a [JmsConnection, JmsEndpoint] pair).
 */
public class JmsQueuePropertiesDialog extends JDialog {
    private JTextField nameTextField;
    private JComboBox driverComboBox;
    private JTextField jndiUrlTextField; // Naming provider URL
    private JTextField qcfNameTextField; // Queue connection factory name
    private JTextField icfNameTextField; // Initial context factory name
    private OptionalCredentialsPanel optionalCredentialsPanel;

    private JPanel buttonPanel;
    private JButton testButton;
    private JButton addButton;
    private JButton cancelButton;

    private ButtonGroup directionButtonGroup;
    private JRadioButton inboundButton;
    private JRadioButton outboundButton;

    private JmsConnection connection = null;
    private JmsEndpoint endpoint = null;
    private boolean wasClosedByOkButton = false;
    private boolean outboundOnly = false;
    private RoleFormPreparer securityFormPreparer;

    private static class ProviderComboBoxItem {
        private JmsProvider provider;

        private ProviderComboBoxItem(JmsProvider provider) {
            this.provider = provider;
        }

        public JmsProvider getProvider() {
            return provider;
        }

        public String toString() {
            return provider.getName();
        }
    }

    private JmsQueuePropertiesDialog(Frame parent) {
        super(parent, true);
    }

    private JmsQueuePropertiesDialog(Dialog parent) {
        super(parent, true);
    }

    /**
     * Create a new JmsQueuePropertiesDialog, configured to adjust the JMS Queue defined by the union of the
     * specified connection and endpoint.  The connection and endpoint may be null, in which case a new
     * Queue will be created by the dialog.  After show() returns, check isCanceled() to see whether the user
     * OK'ed the changes.  If so, call getConnection() and getEndpoint() to read them.  If the dialog completes
     * successfully, the (possibly-new) connection and endpoint will already have been saved to the database.
     *
     * @param parent       the parent window for the new dialog.
     * @param connection   the JMS connection to edit, or null to create a new one for this Queue.
     * @param endpoint     the JMS endpoint to edit, or null to create a new one for this Queue.
     * @param outboundOnly if true, the direction will be locked and defaulted to Outbound only.
     * @return the new instance
     */
    public static JmsQueuePropertiesDialog createInstance(Window parent, JmsConnection connection, JmsEndpoint endpoint, boolean outboundOnly) {
        JmsQueuePropertiesDialog that;
        if (parent instanceof Frame)
            that = new JmsQueuePropertiesDialog((Frame)parent);
        else if (parent instanceof Dialog)
            that = new JmsQueuePropertiesDialog((Dialog)parent);
        else
            throw new IllegalArgumentException("parent must be derived from either Frame or Dialog");
        final SecurityProvider provider = (SecurityProvider)Locator.getDefault().lookup(SecurityProvider.class);
        if (provider == null) {
            throw new IllegalStateException("Could not instantiate security provider");
        }
        that.securityFormPreparer = new RoleFormPreparer(provider, new String[]{Group.ADMIN_GROUP_NAME});


        that.connection = connection;
        that.endpoint = endpoint;
        that.setOutboundOnly(outboundOnly);

        that.init();

        return that;
    }

    private void setOutboundOnly(boolean outboundOnly) {
        this.outboundOnly = outboundOnly;
    }

    private boolean isOutboundOnly() {
        return outboundOnly;
    }

    /**
     * Check how the dialog was closed.  Return value is only guaranteed to be valid after show() has returned.
     *
     * @return false iff. the dialog completed successfully via the "Save" button; otherwise true.
     */
    public boolean isCanceled() {
        return !wasClosedByOkButton;
    }

    /**
     * Obtain the connection that was edited or created.  Return value is only guaranteed to be valid if isCanceled()
     * is false.
     *
     * @return the possibly-new JMS connection, which may have been replaced by a new instance read back from the database
     */
    public JmsConnection getConnection() {
        return connection;
    }

    /**
     * Obtain the endpoint that was edited or created.  Return value is only guaranteed to be valid if isCanceled()
     * is false.
     *
     * @return the possibly-new JMS endpoint, which may have been replaced by a new instance read back from the database
     */
    public JmsEndpoint getEndpoint() {
        return endpoint;
    }

    private void init() {
        setTitle(connection == null ? "Add JMS Queue" : "JMS Queue Properties");
        Container c = getContentPane();
        c.setLayout(new GridBagLayout());
        JPanel p = new JPanel(new GridBagLayout());
        c.add(p, new GridBagConstraints());
        p.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        int y = 0;

        p.add(new JLabel("Queue Name:"),
          new GridBagConstraints(0, y, 1, 1, 0, 0,
            GridBagConstraints.EAST,
            GridBagConstraints.NONE,
            new Insets(0, 0, 5, 3), 0, 0));

        p.add(getNameTextField(),
          new GridBagConstraints(1, y++, 1, 1, 0, 0,
            GridBagConstraints.WEST,
            GridBagConstraints.HORIZONTAL,
            new Insets(0, 0, 5, 0), 0, 0));

        p.add(new JLabel("Driver:"),
          new GridBagConstraints(0, y, 1, 1, 0, 0,
            GridBagConstraints.EAST,
            GridBagConstraints.NONE,
            new Insets(0, 0, 5, 3), 0, 0));

        p.add(getDriverComboBox(),
          new GridBagConstraints(1, y++, 1, 1, 0, 0,
            GridBagConstraints.WEST,
            GridBagConstraints.NONE,
            new Insets(0, 0, 5, 0), 0, 0));

        p.add(new JLabel("Naming Provider URL:"),
          new GridBagConstraints(0, y, 1, 1, 0, 0,
            GridBagConstraints.EAST,
            GridBagConstraints.NONE,
            new Insets(0, 0, 5, 3), 0, 0));

        p.add(getJndiUrlTextField(),
          new GridBagConstraints(1, y++, 1, 1, 0, 0,
            GridBagConstraints.WEST,
            GridBagConstraints.HORIZONTAL,
            new Insets(0, 0, 5, 0), 0, 0));

        p.add(new JLabel("Queue Connection Factory URL:"),
          new GridBagConstraints(0, y, 1, 1, 0, 0,
            GridBagConstraints.EAST,
            GridBagConstraints.NONE,
            new Insets(0, 0, 5, 0), 0, 0));

        p.add(getQcfNameTextField(),
          new GridBagConstraints(1, y++, 1, 1, 0, 0,
            GridBagConstraints.WEST,
            GridBagConstraints.HORIZONTAL,
            new Insets(0, 0, 5, 0), 0, 0));

        p.add(new JLabel("Initial Context Factory Class:"),
          new GridBagConstraints(0, y, 1, 1, 0, 0,
            GridBagConstraints.EAST,
            GridBagConstraints.NONE,
            new Insets(0, 0, 5, 0), 0, 0));

        p.add(getIcfNameTextField(),
          new GridBagConstraints(1, y++, 1, 1, 0, 0,
            GridBagConstraints.WEST,
            GridBagConstraints.HORIZONTAL,
            new Insets(0, 0, 5, 0), 0, 0));

        p.add(new JLabel("Direction:"),
          new GridBagConstraints(0, y, 1, 1, 0, 0,
            GridBagConstraints.EAST,
            GridBagConstraints.NONE,
            new Insets(6, 0, 5, 0), 0, 0));

        p.add(getOutboundButton(),
          new GridBagConstraints(1, y++, 1, 1, 0, 0,
            GridBagConstraints.WEST,
            GridBagConstraints.HORIZONTAL,
            new Insets(6, 0, 0, 0), 0, 0));

        p.add(getInboundButton(),
          new GridBagConstraints(1, y++, 1, 1, 0, 0,
            GridBagConstraints.WEST,
            GridBagConstraints.HORIZONTAL,
            new Insets(0, 0, 6, 0), 0, 0));

        p.add(getOptionalCredentialsPanel(),
          new GridBagConstraints(1, y++, 1, 1, 0, 0,
            GridBagConstraints.WEST,
            GridBagConstraints.HORIZONTAL,
            new Insets(0, 0, 5, 0), 0, 0));

        p.add(getButtonPanel(),
          new GridBagConstraints(0, y++, 2, 1, 10.0, 0,
            GridBagConstraints.EAST,
            GridBagConstraints.HORIZONTAL,
            new Insets(0, 0, 0, 0), 0, 0));

        pack();
        initializeView();
        enableOrDisableComponents();
        applyFormSecurity();
        Actions.setEscKeyStrokeDisposes(this);

    }


    private JPanel getButtonPanel() {
        if (buttonPanel == null) {
            buttonPanel = new JPanel();
            buttonPanel.setLayout(new GridBagLayout());
            buttonPanel.add(Box.createGlue(),
              new GridBagConstraints(0, 0, 1, 1, 10.0, 10.0,
                GridBagConstraints.EAST,
                GridBagConstraints.HORIZONTAL,
                new Insets(0, 0, 0, 0), 0, 0));
            buttonPanel.add(getTestButton(),
              new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.EAST,
                GridBagConstraints.NONE,
                new Insets(0, 0, 0, 5), 0, 0));
            buttonPanel.add(getSaveButton(),
              new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.EAST,
                GridBagConstraints.NONE,
                new Insets(0, 0, 0, 5), 0, 0));
            buttonPanel.add(getCancelButton(),
              new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.EAST,
                GridBagConstraints.NONE,
                new Insets(0, 0, 0, 5), 0, 0));
            Utilities.equalizeButtonSizes(new JButton[]{getTestButton(), getSaveButton(), getCancelButton()});
        }
        return buttonPanel;
    }

    private JButton getTestButton() {
        if (testButton == null) {
            testButton = new JButton("Test Settings");
            testButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    try {
                        JmsConnection newConnection = makeJmsConnectionFromView();
                        if (newConnection == null)
                            return;

                        JmsEndpoint newEndpoint = makeJmsEndpointFromView();
                        if (newEndpoint == null)
                            return;

                        Registry.getDefault().getJmsManager().testEndpoint(newConnection, newEndpoint);
                        JOptionPane.showMessageDialog(JmsQueuePropertiesDialog.this,
                          "The Gateway has verified the existence of this JMS Queue.",
                          "JMS Connection Successful",
                          JOptionPane.INFORMATION_MESSAGE);
                    } catch (RemoteException e1) {
                        throw new RuntimeException("Unable to test this JMS Queue", e1);
                    } catch (Exception e1) {
                        JOptionPane.showMessageDialog(JmsQueuePropertiesDialog.this,
                          "The Gateway was unable to find this JMS Queue:\n" +
                          e1.getMessage(),
                          "JMS Connection Settings",
                          JOptionPane.ERROR_MESSAGE);
                    }
                }
            });
        }
        return testButton;
    }

    private JButton getCancelButton() {
        if (cancelButton == null) {
            cancelButton = new JButton("Cancel");
            cancelButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    JmsQueuePropertiesDialog.this.hide();
                }
            });
        }
        return cancelButton;
    }

    private JButton getSaveButton() {
        if (addButton == null) {
            addButton = new JButton("Save");
            addButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    JmsConnection newConnection = makeJmsConnectionFromView();
                    if (newConnection == null)
                        return;

                    JmsEndpoint newEndpoint = makeJmsEndpointFromView();
                    if (newEndpoint == null)
                        return;

                    try {
                        long oid = Registry.getDefault().getJmsManager().saveConnection(newConnection);
                        newConnection.setOid(oid);
                        newEndpoint.setConnectionOid(newConnection.getOid());
                        oid = Registry.getDefault().getJmsManager().saveEndpoint(newEndpoint);
                        newEndpoint.setOid(oid);

                        connection = Registry.getDefault().getJmsManager().findConnectionByPrimaryKey(newConnection.getOid());
                        endpoint = Registry.getDefault().getJmsManager().findEndpointByPrimaryKey(newEndpoint.getOid());
                    } catch (Exception e1) {
                        throw new RuntimeException("Unable to save changes to this JMS Queue", e1);
                    }

                    // Return from dialog
                    JmsQueuePropertiesDialog.this.hide();
                    wasClosedByOkButton = true;
                }
            });
        }
        return addButton;
    }

    private JTextField getNameTextField() {
        if (nameTextField == null) {
            nameTextField = new JTextField();
            nameTextField.getDocument().addDocumentListener(formPreener);
        }
        return nameTextField;
    }

    private JComboBox getDriverComboBox() {
        if (driverComboBox == null) {
            try {
                JmsProvider[] providers = Registry.getDefault().getJmsManager().getProviderList();
                ProviderComboBoxItem[] items;

                // If we already have a connection, and it was using non-default provider settings,
                // preserve it's old settings in a "Custom" provider
                boolean usingCustom = false;
                if (connection != null) {
                    usingCustom = true;
                    for (int i = 0; i < providers.length; ++i) {
                        JmsProvider provider = providers[i];
                        if (providerMatchesConnection(provider, connection)) {
                            usingCustom = false;
                            break;
                        }
                    }
                }

                if (usingCustom) {
                    items = new ProviderComboBoxItem[providers.length + 1];

                    JmsProvider customProvider = null;
                    customProvider = new JmsProvider();
                    customProvider.setName("(Custom)");
                    customProvider.setDefaultDestinationFactoryUrl(connection.getDestinationFactoryUrl());
                    customProvider.setDefaultQueueFactoryUrl(connection.getQueueFactoryUrl());
                    customProvider.setDefaultTopicFactoryUrl(connection.getTopicFactoryUrl());
                    customProvider.setInitialContextFactoryClassname(connection.getInitialContextFactoryClassname());

                    items[0] = new ProviderComboBoxItem(customProvider);

                    for (int i = 0; i < providers.length; i++)
                        items[i + 1] = new ProviderComboBoxItem(providers[i]);
                } else {
                    // No "custom" provider required
                    items = new ProviderComboBoxItem[providers.length];
                    for (int i = 0; i < providers.length; i++)
                        items[i] = new ProviderComboBoxItem(providers[i]);
                }

                driverComboBox = new JComboBox(items);
                driverComboBox.setSelectedIndex(-1);
                driverComboBox.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        final ProviderComboBoxItem providerItem = (ProviderComboBoxItem)getDriverComboBox().getSelectedItem();
                        if (providerItem == null)
                            return;
                        JmsProvider provider = providerItem.getProvider();

                        // Queue connection factory name, defaulting to destination factory name
                        String qcfName = provider.getDefaultQueueFactoryUrl();
                        if (qcfName == null || qcfName.length() < 1)
                            qcfName = provider.getDefaultDestinationFactoryUrl();
                        if (qcfName != null)
                            getQcfNameTextField().setText(qcfName);

                        String icfName = provider.getInitialContextFactoryClassname();
                        if (icfName != null)
                            getIcfNameTextField().setText(icfName);
                    }
                });

            } catch (Exception e) {
                throw new RuntimeException("Unable to obtain list of installed JMS providers from Gateway", e);
            }
        }
        return driverComboBox;
    }

    // Return true if the specified JmsProvider provides the exact same DefaultQueueFactoryUrl and
    // InitialContextFactoryClassname as the specified connection.  Neither parameter may be null.
    private boolean providerMatchesConnection(JmsProvider provider, JmsConnection connection) {
        return provider.getDefaultQueueFactoryUrl() != null &&
          provider.getDefaultQueueFactoryUrl().equals(connection.getQueueFactoryUrl()) &&
          provider.getInitialContextFactoryClassname() != null &&
          provider.getInitialContextFactoryClassname().equals(connection.getInitialContextFactoryClassname());
    }

    private FormPreener formPreener = new FormPreener();

    private class FormPreener implements DocumentListener {
        public void insertUpdate(DocumentEvent e) { changed(); }

        public void removeUpdate(DocumentEvent e) { changed(); }

        public void changedUpdate(DocumentEvent e) { changed(); }

        private void changed() {
            enableOrDisableComponents();
        }
    }

    private JTextField getJndiUrlTextField() {
        if (jndiUrlTextField == null) {
            jndiUrlTextField = new JTextField();
            jndiUrlTextField.getDocument().addDocumentListener(formPreener);
        }
        return jndiUrlTextField;
    }

    private JTextField getQcfNameTextField() {
        if (qcfNameTextField == null) {
            qcfNameTextField = new JTextField();
            qcfNameTextField.getDocument().addDocumentListener(formPreener);
        }
        return qcfNameTextField;
    }

    private JTextField getIcfNameTextField() {
        if (icfNameTextField == null) {
            icfNameTextField = new JTextField();
            icfNameTextField.getDocument().addDocumentListener(formPreener);
        }
        return icfNameTextField;
    }

    private OptionalCredentialsPanel getOptionalCredentialsPanel() {
        if (optionalCredentialsPanel == null) {
            optionalCredentialsPanel = new OptionalCredentialsPanel();
        }
        return optionalCredentialsPanel;
    }

    /**
     * Extract information from the view and create a new JmsConnection object.  The new object will not have a
     * valid OID and will not yet have been saved to the database.
     * <p/>
     * If the form state is not valid, an error dialog is displayed and null is returned.
     *
     * @return a new JmsConnection with the current settings, or null if one could not be created.  The new connection
     *         will not yet have been saved to the database.
     */
    private JmsConnection makeJmsConnectionFromView() {
        if (!validateForm()) {
            JOptionPane.showMessageDialog(JmsQueuePropertiesDialog.this,
              "At minimum, the name, queue name, naming URL and factory URL are required.",
              "Unable to proceed",
              JOptionPane.ERROR_MESSAGE);
            return null;
        }

        JmsConnection conn;
        if (connection != null) {
            conn = new JmsConnection();
            conn.copyFrom(connection);
        } else {
            final ProviderComboBoxItem providerItem = ((ProviderComboBoxItem)getDriverComboBox().getSelectedItem());
            if (providerItem == null) {
                conn = new JmsConnection();
            } else {
                JmsProvider provider = providerItem.getProvider();
                conn = provider.createConnection(getNameTextField().getText(),
                  getJndiUrlTextField().getText());
            }
        }

        if (getOptionalCredentialsPanel().isUsernameAndPasswordRequired()) {
            conn.setUsername(getOptionalCredentialsPanel().getUsername());
            conn.setPassword(new String(getOptionalCredentialsPanel().getPassword()));
        } else {
            conn.setUsername(null);
            conn.setPassword(null);
        }

        conn.setJndiUrl(getJndiUrlTextField().getText());
        conn.setInitialContextFactoryClassname(getIcfNameTextField().getText());
        conn.setQueueFactoryUrl(getQcfNameTextField().getText());
        return conn;
    }

    /**
     * Extract information from the view and create a new JmsEndpoint object.  The new object will not have a
     * valid OID and will not yet have been saved to the database.
     * <p/>
     * If the form state is not valid, an error dialog is displayed and null is returned.
     *
     * @return a new JmsEndpoint with the current settings, or null if one could not be created.  The new connection
     *         will not yet have been saved to the database.
     */
    private JmsEndpoint makeJmsEndpointFromView() {
        if (!validateForm()) {
            JOptionPane.showMessageDialog(JmsQueuePropertiesDialog.this,
              "The queue name must be provided.",
              "Unable to proceed",
              JOptionPane.ERROR_MESSAGE);
            return null;
        }

        JmsEndpoint ep = new JmsEndpoint();
        if (endpoint != null)
            ep.copyFrom(endpoint);
        String name = getNameTextField().getText();
        ep.setName(name);
        ep.setDestinationName(name);
        ep.setMessageSource(getInboundButton().isSelected());

        if (getOptionalCredentialsPanel().isUsernameAndPasswordRequired()) {
            ep.setUsername(getOptionalCredentialsPanel().getUsername());
            ep.setPassword(new String(getOptionalCredentialsPanel().getPassword()));
        } else {
            ep.setUsername(null);
            ep.setPassword(null);
        }

        // Preserve old OID, if we have one
/*
        if (endpoint != null) {
            ep.setOid(endpoint.getOid());
            ep.setVersion( endpoint.getVersion() );
        }
*/

        return ep;
    }

    private void selectDriverForConnection(JmsConnection connection) {
        int numDrivers = getDriverComboBox().getModel().getSize();
        for (int i = 0; i < numDrivers; ++i) {
            ProviderComboBoxItem item = (ProviderComboBoxItem)getDriverComboBox().getModel().getElementAt(i);
            JmsProvider provider = item.getProvider();
            if (providerMatchesConnection(provider, connection)) {
                getDriverComboBox().setSelectedItem(item);
                return;
            }
        }
    }

    /**
     * Configure the gui to conform with the current endpoint and connection.
     */
    private void initializeView() {
        if (connection != null) {
            // configure gui from connection
            selectDriverForConnection(connection);
            getQcfNameTextField().setText(connection.getQueueFactoryUrl());
            getJndiUrlTextField().setText(connection.getJndiUrl());
            getIcfNameTextField().setText(connection.getInitialContextFactoryClassname());
            boolean useCredentials = connection.getUsername() != null && connection.getUsername().length() > 0;
            getOptionalCredentialsPanel().
              setUsernameAndPasswordRequired(useCredentials, connection.getUsername(), connection.getPassword());

        } else {
            // No connection is set
            getDriverComboBox().setSelectedIndex(-1);
            getQcfNameTextField().setText("");
            getIcfNameTextField().setText("");
            getJndiUrlTextField().setText("");
            getOptionalCredentialsPanel().setUsernameAndPasswordRequired(false, null, null);
        }

        if (endpoint != null) {
            // Configure gui from endpoint
            getNameTextField().setText(endpoint.getDestinationName());
            getInboundButton().setSelected(endpoint.isMessageSource());
        } else {
            // No endpoint is set
            getNameTextField().setText("");
            getInboundButton().setSelected(false);
        }
        getOutboundButton().setSelected(!getInboundButton().isSelected());


    }

    /**
     * Returns true iff. the form has enough information to construct a JmsConnection.
     */
    private boolean validateForm() {
        if (getNameTextField().getText().length() < 1)
            return false;
        if (getJndiUrlTextField().getText().length() < 1)
            return false;
        if (getQcfNameTextField().getText().length() < 1)
            return false;
        if (getIcfNameTextField().getText().length() < 1)
            return false;
        return true;
    }

    /**
     * Adjust components based on the state of the form.
     */
    private void enableOrDisableComponents() {
        final boolean valid = validateForm();
        addButton.setEnabled(valid);
        testButton.setEnabled(valid);
    }

    private void applyFormSecurity() {
        // list components that are subject to security (they require the full admin role)
        securityFormPreparer.prepare(new Component[]{
            addButton,
            getInboundButton(),
            getOutboundButton(),
            getDriverComboBox(),
            getQcfNameTextField(),
            getIcfNameTextField(),
            getJndiUrlTextField(),
            getNameTextField()

        });
        securityFormPreparer.prepare(optionalCredentialsPanel.getComponents());
    }

    private ButtonGroup getDirectionButtonGroup() {
        if (directionButtonGroup == null) {
            directionButtonGroup = new ButtonGroup();
        }
        return directionButtonGroup;
    }

    private JRadioButton getInboundButton() {
        if (inboundButton == null) {
            inboundButton = new JRadioButton("Inbound - Gateway will drain messages from Queue");
            inboundButton.setEnabled(!isOutboundOnly());
            getDirectionButtonGroup().add(inboundButton);
        }
        return inboundButton;
    }

    private JRadioButton getOutboundButton() {
        if (outboundButton == null) {
            outboundButton = new JRadioButton("Outbound - Gateway can route messages to Queue");
            outboundButton.setEnabled(!isOutboundOnly());
            getDirectionButtonGroup().add(outboundButton);
        }

        return outboundButton;
    }

}
