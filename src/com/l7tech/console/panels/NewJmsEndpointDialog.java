/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.console.panels;

import com.l7tech.common.gui.widgets.OptionalCredentialsPanel;
import com.l7tech.common.gui.widgets.WrappingLabel;
import com.l7tech.common.transport.jms.JmsConnection;
import com.l7tech.common.transport.jms.JmsEndpoint;
import com.l7tech.console.util.Registry;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.RemoteException;

/**
 * Dialog for registering a new JMS Endpoint with a given connection.
 */
public class NewJmsEndpointDialog extends JDialog {
    private JmsEndpoint newJmsEndpoint = null;
    private JmsConnection connection;
    private JTextField nameTextField;
    private JButton addButton;
    private OptionalCredentialsPanel optionalCredentialsPanel;
    private JPanel buttonPanel;
    private JButton testButton;
    private JButton cancelButton;

    public NewJmsEndpointDialog(Frame parent, JmsConnection connection) {
        super(parent, true);
        this.connection = connection;
        init();
    }

    public NewJmsEndpointDialog(Dialog parent, JmsConnection connection) {
        super(parent, true);
        this.connection = connection;
        init();
    }

    private void init() {
        setTitle("Add JMS Endpoint");
        Container c = getContentPane();
        c.setLayout(new GridBagLayout());
        JPanel p = new JPanel(new GridBagLayout());
        c.add(p, new GridBagConstraints());
        p.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        int y = 0;

        p.add(new WrappingLabel("Please enter the name of a queue visible through this JMS connection.  " +
                                "This queue must already exist on the target JMS server.",
                                3),
              new GridBagConstraints(0, y++, 2, 1, 0, 0,
                                     GridBagConstraints.WEST,
                                     GridBagConstraints.BOTH,
                                     new Insets(0, 0, 5, 3), 0, 0));

        p.add(new JLabel("Endpoint name:"),
              new GridBagConstraints(0, y, 1, 1, 0, 0,
                                     GridBagConstraints.EAST,
                                     GridBagConstraints.NONE,
                                     new Insets(0, 0, 5, 3), 0, 0));

        p.add(getNameTextField(),
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
            buttonPanel = new JPanel();
            buttonPanel.setLayout(new GridBagLayout());
            buttonPanel.add(Box.createGlue(),
                            new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0,
                                                   GridBagConstraints.EAST,
                                                   GridBagConstraints.HORIZONTAL,
                                                   new Insets(0, 0, 0, 0), 0, 0));
            buttonPanel.add(getTestButton(),
                            new GridBagConstraints(1, 0, 1, 1, 1.0, 1.0,
                                                   GridBagConstraints.EAST,
                                                   GridBagConstraints.NONE,
                                                   new Insets(0, 0, 0, 0), 0, 0));
            buttonPanel.add(getAddButton(),
                            new GridBagConstraints(2, 0, 1, 1, 1.0, 1.0,
                                                   GridBagConstraints.EAST,
                                                   GridBagConstraints.NONE,
                                                   new Insets(0, 0, 0, 0), 0, 0));
            buttonPanel.add(getCancelButton(),
                            new GridBagConstraints(3, 0, 1, 1, 1.0, 1.0,
                                                   GridBagConstraints.EAST,
                                                   GridBagConstraints.NONE,
                                                   new Insets(0, 0, 0, 0), 0, 0));
        }
        return buttonPanel;
    }

    private JButton getTestButton() {
        if (testButton == null) {
            testButton = new JButton("Test Settings");
            testButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    JmsEndpoint endpoint = makeJmsEndpointFromView();
                    if (endpoint == null)
                        return;

                    try {
                        Registry.getDefault().getJmsManager().testEndpoint(connection, endpoint);
                        JOptionPane.showMessageDialog(NewJmsEndpointDialog.this,
                                                      "The Gateway has verified the existence of this JMS Endpoint.",
                                                      "JMS Connection Successful",
                                                      JOptionPane.INFORMATION_MESSAGE);
                    } catch (RemoteException e1) {
                        throw new RuntimeException("Unable to test this JMS endpoint", e1);
                    } catch (Exception e1) {
                        JOptionPane.showMessageDialog(NewJmsEndpointDialog.this,
                                                      "The Gateway was unable to find this JMS Endpoint:\n" +
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
                    NewJmsEndpointDialog.this.hide();
                }
            });
        }
        return cancelButton;
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
            JOptionPane.showMessageDialog(NewJmsEndpointDialog.this,
                                          "The queue name must be provided.",
                                          "Unable to proceed",
                                          JOptionPane.ERROR_MESSAGE);
            return null;
        }

        JmsEndpoint ep = new JmsEndpoint();
        String name = getNameTextField().getText();
        ep.setName(name);
        ep.setDestinationName(name);
        ep.setConnectionOid(this.connection.getOid());
        if (getOptionalCredentialsPanel().isUsernameAndPasswordRequired()) {
            ep.setUsername(getOptionalCredentialsPanel().getUsername());
            ep.setPassword(new String(getOptionalCredentialsPanel().getPassword()));
        }
        return ep;
    }

    private JButton getAddButton() {
        if (addButton == null) {
            addButton = new JButton("Add Endpoint");
            addButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    JmsEndpoint ep = makeJmsEndpointFromView();
                    if (ep == null)
                        return;

                    try {
                        long oid = Registry.getDefault().getJmsManager().saveEndpoint(ep);
                        ep.setOid(oid);
                    } catch (Exception e1) {
                        throw new RuntimeException("Unable to save changes to this JMS connection", e1);
                    }

                    // Return from dialog
                    newJmsEndpoint = ep;
                    NewJmsEndpointDialog.this.hide();
                }
            });
        }
        return addButton;
    }

    private OptionalCredentialsPanel getOptionalCredentialsPanel() {
        if (optionalCredentialsPanel == null) {
            optionalCredentialsPanel = new OptionalCredentialsPanel();
        }
        return optionalCredentialsPanel;
    }

    private void enableOrDisableComponents() {
        boolean valid = validateForm();
        getAddButton().setEnabled(valid);
        getTestButton().setEnabled(valid);
    }

    private boolean validateForm() {
        return getNameTextField().getText().length() > 0;
    }

    private JTextField getNameTextField() {
        if (nameTextField == null) {
            nameTextField = new JTextField();
            nameTextField.getDocument().addDocumentListener(new DocumentListener() {
                public void insertUpdate(DocumentEvent e) { changed(); }
                public void removeUpdate(DocumentEvent e) { changed(); }
                public void changedUpdate(DocumentEvent e) { changed(); }
                private void changed() {
                    enableOrDisableComponents();
                }
            });
        }
        return nameTextField;
    }

    /**
     * Get the newly-created JMS endpoint, or null if one was not created.
     * The new endpoint will have already been saved back to the database.
     * @return  The new, already-saved JmsEndpoint, or null if one was not created after all.
     */
    public JmsEndpoint getNewJmsEndpoint() {
        return newJmsEndpoint;
    }
}
