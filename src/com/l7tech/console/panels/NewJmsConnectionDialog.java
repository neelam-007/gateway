/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.console.panels;

import com.l7tech.common.gui.widgets.OptionalCredentialsPanel;
import com.l7tech.common.transport.jms.JmsProvider;
import com.l7tech.common.transport.jms.JmsConnection;
import com.l7tech.console.util.Registry;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.VersionException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.RemoteException;

/**
 * Dialog that pops up when "New JMS Connection..." button is clicked.
 */
public class NewJmsConnectionDialog extends JDialog {
    private JTextField nameTextField;
    private JComboBox driverComboBox;
    private JTextField jndiUrlTextField; // Naming provider URL
    private JTextField qcfNameTextField; // Queue connection factory name
    private OptionalCredentialsPanel optionalCredentialsPanel;
    private JButton testButton;
    private JPanel buttonPanel;
    private JButton saveButton;
    private JButton cancelButton;

    private JmsConnection newConnection = null;

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

    public NewJmsConnectionDialog(Dialog owner) {
        super(owner);
        init();
    }

    public NewJmsConnectionDialog(Frame owner) {
        super(owner);
        init();
    }

    /**
     * Call after show() returns to collect the newly created JmsConnection, if any.
     *
     * @return The newly created JmsConnection, or null if one wasn't created.
     */
    public JmsConnection getNewJmsConnection() {
        return newConnection;
    }

    private void init() {
        setTitle("New JMS Connection...");
        Container c = getContentPane();
        c.setLayout(new GridBagLayout());
        JPanel p = new JPanel(new GridBagLayout());
        c.add(p, new GridBagConstraints());
        p.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        int y = 0;

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

        p.add(new JLabel("Name:"),
              new GridBagConstraints(0, y, 1, 1, 0, 0,
                                     GridBagConstraints.EAST,
                                     GridBagConstraints.NONE,
                                     new Insets(0, 0, 5, 3), 0, 0));

        p.add(getNameTextField(),
              new GridBagConstraints(1, y++, 1, 1, 0, 0,
                                     GridBagConstraints.WEST,
                                     GridBagConstraints.HORIZONTAL,
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
                                     new Insets(0, 0, 5, 3), 0, 0));

        p.add(getQcfNameTextField(),
              new GridBagConstraints(1, y++, 1, 1, 0, 0,
                                     GridBagConstraints.WEST,
                                     GridBagConstraints.HORIZONTAL,
                                     new Insets(0, 0, 5, 0), 0, 0));

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
    }

    private JPanel getButtonPanel() {
        if (buttonPanel == null) {
            buttonPanel = new JPanel(new GridBagLayout());

            buttonPanel.add(Box.createHorizontalGlue(),
                            new GridBagConstraints(0, 0, 1, 1, 1.0, 0,
                                                   GridBagConstraints.EAST,
                                                   GridBagConstraints.HORIZONTAL,
                                                   new Insets(0, 0, 0, 0), 0, 0));

            buttonPanel.add(getTestButton(),
                            new GridBagConstraints(1, 0, 1, 1, 0, 0,
                                                   GridBagConstraints.EAST,
                                                   GridBagConstraints.NONE,
                                                   new Insets(0, 5, 0, 0), 0, 0));

            buttonPanel.add(getSaveButton(),
                            new GridBagConstraints(2, 0, 1, 1, 0, 0,
                                                   GridBagConstraints.EAST,
                                                   GridBagConstraints.NONE,
                                                   new Insets(0, 5, 0, 0), 0, 0));

            buttonPanel.add(getCancelButton(),
                            new GridBagConstraints(3, 0, 1, 1, 0, 0,
                                                   GridBagConstraints.EAST,
                                                   GridBagConstraints.NONE,
                                                   new Insets(0, 5, 0, 0), 0, 0));
        }
        return buttonPanel;
    }

    private JTextField getNameTextField() {
        if (nameTextField == null) {
            nameTextField = new JTextField();
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
            } catch (RemoteException e) {
                throw new RuntimeException("Unable to obtain list of installed JMS providers from Gateway", e);
            }
        }
        return driverComboBox;
    }

    private JTextField getJndiUrlTextField() {
        if (jndiUrlTextField == null) {
            jndiUrlTextField = new JTextField();
        }
        return jndiUrlTextField;
    }

    private JTextField getQcfNameTextField() {
        if (qcfNameTextField == null) {
            qcfNameTextField = new JTextField();
        }
        return qcfNameTextField;
    }

    private OptionalCredentialsPanel getOptionalCredentialsPanel() {
        if (optionalCredentialsPanel == null) {
            optionalCredentialsPanel = new OptionalCredentialsPanel();
        }
        return optionalCredentialsPanel;
    }

    private JButton getTestButton() {
        if (testButton == null) {
            testButton = new JButton("Test Settings");
        }
        return testButton;
    }

    private JButton getSaveButton() {
        if (saveButton == null) {
            saveButton = new JButton("Save Connection");
            saveButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (!validateForm()) {
                        JOptionPane.showMessageDialog(NewJmsConnectionDialog.this,
                                                      "At minimum, the name, driver, naming URL and factory URL are required.",
                                                      "Unable to proceed",
                                                      JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    JmsProvider provider = ((ProviderComboBoxItem)getDriverComboBox().getSelectedItem()).getProvider();
                    JmsConnection conn = provider.createConnection(getNameTextField().getText(),
                                                                   getJndiUrlTextField().getText());
                    try {
                        long oid = Registry.getDefault().getJmsManager().saveConnection(conn);
                        conn.setOid(oid);
                    } catch (Exception e1) {
                        throw new RuntimeException("Unable to create JMS connection", e1);
                    }

                    newConnection = conn;
                    NewJmsConnectionDialog.this.hide();
                }
            });
        }
        return saveButton;
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

    private JButton getCancelButton() {
        if (cancelButton == null) {
            cancelButton = new JButton("Cancel");
            cancelButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    NewJmsConnectionDialog.this.hide();
                }
            });
        }
        return cancelButton;
    }
}
