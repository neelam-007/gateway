/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.console.panels;

import com.l7tech.common.gui.widgets.OptionalCredentialsPanel;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.transport.jms.JmsConnection;
import com.l7tech.common.transport.jms.JmsEndpoint;
import com.l7tech.common.transport.jms.JmsProvider;
import com.l7tech.console.util.Registry;

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
    private OptionalCredentialsPanel optionalCredentialsPanel;

    private JPanel buttonPanel;
    private JButton testButton;
    private JButton addButton;
    private JButton cancelButton;

    private JmsConnection connection = null;
    private JmsEndpoint endpoint = null;
    private ButtonGroup directionButtonGroup;
    private JRadioButton inboundButton;
    private JRadioButton outboundButton;

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

    public JmsQueuePropertiesDialog(Frame parent, JmsConnection connection, JmsEndpoint endpoint) {
        super(parent, true);
        this.connection = connection;
        this.endpoint = endpoint;
        init();
    }

    public JmsQueuePropertiesDialog(Dialog parent, JmsConnection connection, JmsEndpoint endpoint) {
        super(parent, true);
        this.connection = connection;
        this.endpoint = endpoint;
        init();
    }

    private void init() {
        setTitle("New JMS Queue");
        Container c = getContentPane();
        c.setLayout(new GridBagLayout());
        JPanel p = new JPanel(new GridBagLayout());
        c.add(p, new GridBagConstraints());
        p.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        int y = 0;

        p.add(new JLabel("Queue name:"),
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

        p.add(new JLabel("Naming provider URL:"),
              new GridBagConstraints(0, y, 1, 1, 0, 0,
                                     GridBagConstraints.EAST,
                                     GridBagConstraints.NONE,
                                     new Insets(0, 0, 5, 3), 0, 0));

        p.add(getJndiUrlTextField(),
              new GridBagConstraints(1, y++, 1, 1, 0, 0,
                                     GridBagConstraints.WEST,
                                     GridBagConstraints.HORIZONTAL,
                                     new Insets(0, 0, 5, 0), 0, 0));

        p.add(new JLabel("Queue connection factory URL:"),
              new GridBagConstraints(0, y, 1, 1, 0, 0,
                                     GridBagConstraints.EAST,
                                     GridBagConstraints.NONE,
                                     new Insets(0, 0, 5, 0), 0, 0));

        p.add(getQcfNameTextField(),
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
            Utilities.equalizeButtonSizes(new JButton[] { getTestButton(), getSaveButton(), getCancelButton() });
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
                        throw new RuntimeException("Unable to test this JMS endpoint", e1);
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
                    if (addButton != null) // todo Move this code elsewhere
                        throw new IllegalStateException("Save does not do the right thing currently");

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
                    } catch (Exception e1) {
                        throw new RuntimeException("Unable to save changes to this JMS queue", e1);
                    }

                    // Return from dialog
                    JmsQueuePropertiesDialog.this.hide();
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
                ProviderComboBoxItem[] items = new ProviderComboBoxItem[providers.length];
                for (int i = 0; i < providers.length; i++)
                    items[i] = new ProviderComboBoxItem(providers[i]);
                driverComboBox = new JComboBox(items);
                driverComboBox.setSelectedIndex(-1);
                driverComboBox.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        JmsProvider provider = ((ProviderComboBoxItem)getDriverComboBox().getSelectedItem()).getProvider();
                        if (provider == null)
                            return;

                        // Queue connection factory name, defaulting to destination factory name
                        String qcfName = provider.getDefaultQueueFactoryUrl();
                        if (qcfName == null || qcfName.length() < 1)
                            qcfName = provider.getDefaultDestinationFactoryUrl();
                        if (qcfName != null)
                            getQcfNameTextField().setText(qcfName);
                    }
                });

            } catch (Exception e) {
                throw new RuntimeException("Unable to obtain list of installed JMS providers from Gateway", e);
            }
        }
        return driverComboBox;
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

    private OptionalCredentialsPanel getOptionalCredentialsPanel() {
        if (optionalCredentialsPanel == null) {
            optionalCredentialsPanel = new OptionalCredentialsPanel();
        }
        return optionalCredentialsPanel;
    }

    /**
     * Extract information from the view and create a new JmsConnection object.  The new object will not have a
     * valid OID and will not yet have been saved to the database.
     *
     * If the form state is not valid, an error dialog is displayed and null is returned.
     *
     * @return a new JmsConnection with the current settings, or null if one could not be created.  The new connection
     * will not yet have been saved to the database.
     */
    private JmsConnection makeJmsConnectionFromView() {
        if (!validateForm()) {
            JOptionPane.showMessageDialog(JmsQueuePropertiesDialog.this,
                                          "At minimum, the name, queue name, driver, naming URL and factory URL are required.",
                                          "Unable to proceed",
                                          JOptionPane.ERROR_MESSAGE);
            return null;
        }

        JmsProvider provider = ((ProviderComboBoxItem)getDriverComboBox().getSelectedItem()).getProvider();
        JmsConnection conn = provider.createConnection(getNameTextField().getText(),
                                                       getJndiUrlTextField().getText());

        if (optionalCredentialsPanel.isUsernameAndPasswordRequired()) {
            conn.setUsername(optionalCredentialsPanel.getUsername());
            conn.setPassword(new String(optionalCredentialsPanel.getPassword()));
        }

        conn.setQueueFactoryUrl(qcfNameTextField.getText());

        return conn;
    }

    /**
     * Extract information from the view and create a new JmsEndpoint object.  The new object will not have a
     * valid OID and will not yet have been saved to the database.
     *
     * If the form state is not valid, an error dialog is displayed and null is returned.
     *
     * @return a new JmsEndpoint with the current settings, or null if one could not be created.  The new connection
     * will not yet have been saved to the database.
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
        String name = getNameTextField().getText();
        ep.setName(name);
        ep.setDestinationName(name);
        if (getOptionalCredentialsPanel().isUsernameAndPasswordRequired()) {
            ep.setUsername(getOptionalCredentialsPanel().getUsername());
            ep.setPassword(new String(getOptionalCredentialsPanel().getPassword()));
        }
        return ep;
    }

    private void selectDriverForConnection(JmsConnection connection) {
        int numDrivers = getDriverComboBox().getModel().getSize();
        for (int i = 0; i < numDrivers; ++i) {
            ProviderComboBoxItem item = (ProviderComboBoxItem) getDriverComboBox().getModel().getElementAt(i);
            JmsProvider provider = item.getProvider();
            if (provider.getDefaultQueueFactoryUrl() != null &&
                    provider.getDefaultQueueFactoryUrl().equals(connection.getQueueFactoryUrl()) &&
                    provider.getInitialContextFactoryClassname() != null &&
                    provider.getInitialContextFactoryClassname().equals(
                            connection.getInitialContextFactoryClassname())) {
                getDriverComboBox().setSelectedItem(item);
                return;
            }
        }
    }

    /** Configure the gui to conform with the current endpoint and connection. */
    private void initializeView() {
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

        if (connection != null) {
            // configure gui from connection
            selectDriverForConnection(connection);
            getQcfNameTextField().setText(connection.getQueueFactoryUrl());
            boolean useCredentials = connection.getUsername() != null && connection.getUsername().length() > 0;
            getOptionalCredentialsPanel().
                    setUsernameAndPasswordRequired(useCredentials, connection.getUsername(), connection.getPassword());

        } else {
            // No connection is set
            getDriverComboBox().setSelectedIndex(-1);
            getQcfNameTextField().setText("");
            getOptionalCredentialsPanel().setUsernameAndPasswordRequired(false, null, null);
        }
    }

    /** Returns true iff. the form has enough information to construct a JmsConnection. */
    private boolean validateForm() {
        if (getNameTextField().getText().length() < 1)
            return false;
        if (getJndiUrlTextField().getText().length() < 1)
            return false;
        if (getQcfNameTextField().getText().length() < 1)
            return false;
        if (getDriverComboBox().getSelectedItem() == null)
            return false;
        return true;
    }

    /** Adjust components based on the state of the form. */
    private void enableOrDisableComponents() {
        final boolean valid = validateForm();
        addButton.setEnabled(valid);
        testButton.setEnabled(valid);
    }

    private ButtonGroup getDirectionButtonGroup() {
        if (directionButtonGroup == null) {
            directionButtonGroup = new ButtonGroup();
        }
        return directionButtonGroup;
    }

    private JRadioButton getInboundButton() {
        if (inboundButton == null) {
            inboundButton = new JRadioButton("Inbound (Gateway will take messages from this Endpoint)");
            getDirectionButtonGroup().add(inboundButton);
        }
        return inboundButton;
    }

    private JRadioButton getOutboundButton() {
        if (outboundButton == null) {
            outboundButton = new JRadioButton("Outbound");
            getDirectionButtonGroup().add(outboundButton);
        }

        return outboundButton;
    }
}
