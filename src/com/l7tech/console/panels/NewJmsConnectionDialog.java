/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.console.panels;

import com.l7tech.common.gui.widgets.OptionalCredentialsPanel;
import com.l7tech.common.transport.jms.JmsConnection;
import com.l7tech.common.transport.jms.JmsProvider;
import com.l7tech.console.util.Registry;
import com.l7tech.objectmodel.FindException;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.jms.JMSException;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Dialog that pops up when "New JMS Connection..." button is clicked.
 */
public class NewJmsConnectionDialog extends JDialog {
    private JTextField nameTextField;
    private String defaultName = "";
    private JComboBox driverComboBox;
    private String driverName = "";
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
        super(owner, true);
        init();
    }

    public NewJmsConnectionDialog(Frame owner) {
        super(owner, true);
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
        setTitle("New JMS Connection");
        Container c = getContentPane();
        c.setLayout(new GridBagLayout());
        JPanel p = new JPanel(new GridBagLayout());
        c.add(p, new GridBagConstraints());
        p.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        int y = 0;

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

        p.add(new JLabel("Connection name:"),
              new GridBagConstraints(0, y, 1, 1, 0, 0,
                                     GridBagConstraints.EAST,
                                     GridBagConstraints.NONE,
                                     new Insets(0, 0, 5, 3), 0, 0));

        p.add(getNameTextField(),
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
        enableOrDisableComponents();
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
                        if (provider == null) {
                            driverName = "";
                            return;
                        }

                        // Queue connection factory name, defaulting to destination factory name
                        String qcfName = provider.getDefaultQueueFactoryUrl();
                        if (qcfName == null || qcfName.length() < 1)
                            qcfName = provider.getDefaultDestinationFactoryUrl();
                        if (qcfName != null)
                            getQcfNameTextField().setText(qcfName);

                        String curName = getNameTextField().getText();
                        boolean wasDefaultName = defaultName.equals(curName) || curName.length() < 1;
                        driverName = provider.getName();
                        updateDefaultName();
                        if (wasDefaultName)
                            getNameTextField().setText(defaultName);
                    }
                });

            } catch (Exception e) {
                throw new RuntimeException("Unable to obtain list of installed JMS providers from Gateway", e);
            }
        }
        return driverComboBox;
    }

    private static Pattern findHost = Pattern.compile("^[^:]*:/?/?/?(?:[^\\@/]*\\@)?([a-zA-Z0-9._\\-]*).*", Pattern.DOTALL);

    // Make a default name for this connection based on available information
    private void updateDefaultName() {
        String d = (driverName == null || driverName.length() < 1) ? "unknown" : driverName;
        String s = "unknown";
        String urlStr = getJndiUrlTextField().getText();
        Matcher matcher= findHost.matcher(urlStr);
        if (matcher.matches() && matcher.group(1).length() > 0)
            s = matcher.group(1);
        defaultName = d + " on " + s;
    }

    private FormPreener formPreener = new FormPreener();
    private class FormPreener implements DocumentListener {
        public void insertUpdate(DocumentEvent e) { changed(e); }
        public void removeUpdate(DocumentEvent e) { changed(e); }
        public void changedUpdate(DocumentEvent e) { changed(e); }
        private void changed(DocumentEvent e) {
            if (!e.getDocument().equals(getNameTextField().getDocument())) {
                String curName = getNameTextField().getText();
                boolean wasDefaultName = defaultName.equals(curName) || curName.length() < 1;
                updateDefaultName();
                if (wasDefaultName && !curName.equals(defaultName))
                    getNameTextField().setText(defaultName);
            }
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
            JOptionPane.showMessageDialog(NewJmsConnectionDialog.this,
                                          "At minimum, the name, driver, naming URL and factory URL are required.",
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

    private JButton getTestButton() {
        if (testButton == null) {
            testButton = new JButton("Test Settings");
            testButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    JmsConnection conn = makeJmsConnectionFromView();
                    if (conn == null)
                        return;

                    try {
                        Registry.getDefault().getJmsManager().testConnection(conn);
                        JOptionPane.showMessageDialog(NewJmsConnectionDialog.this,
                                                      "The Gateway successfully established this JMS connection.",
                                                      "JMS Connection Successful",
                                                      JOptionPane.INFORMATION_MESSAGE);
                    } catch (RemoteException e1) {
                        throw new RuntimeException("Unable to test this JMS connection", e1);
                    } catch (Exception e1) {
                        JOptionPane.showMessageDialog(NewJmsConnectionDialog.this,
                                                      "The Gateway was unable to establish this JMS connection:\n" +
                                                      e1.getMessage(),
                                                      "JMS Connection Settings",
                                                      JOptionPane.ERROR_MESSAGE);
                    }
                }
            });
        }
        return testButton;
    }

    private JButton getSaveButton() {
        if (saveButton == null) {
            saveButton = new JButton("Save Connection");
            saveButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    JmsConnection conn = makeJmsConnectionFromView();
                    if (conn == null)
                        return;

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

    /** Adjust components based on the state of the form. */
    private void enableOrDisableComponents() {
        saveButton.setEnabled(validateForm());
        testButton.setEnabled(validateForm());
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
