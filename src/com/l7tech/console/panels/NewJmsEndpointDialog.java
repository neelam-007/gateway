/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.console.panels;

import com.l7tech.common.transport.jms.JmsConnection;
import com.l7tech.common.transport.jms.JmsEndpoint;
import com.l7tech.common.gui.widgets.WrappingLabel;
import com.l7tech.common.gui.widgets.OptionalCredentialsPanel;
import com.l7tech.console.util.Registry;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.VersionException;

import javax.swing.*;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.rmi.RemoteException;
import java.util.Set;
import java.util.Iterator;

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

    private JmsEndpoint makeJmsEndpointFromView() {
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

    private JButton getAddButton() {
        if (addButton == null) {
            addButton = new JButton("Add Endpoint");
            addButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (!validateForm()) {
                        JOptionPane.showMessageDialog(NewJmsEndpointDialog.this,
                                                      "The queue name must be provided.",
                                                      "Unable to proceed",
                                                      JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    JmsEndpoint ep = makeJmsEndpointFromView();

                    Set eps = connection.getEndpoints();
                    for (Iterator i = eps.iterator(); i.hasNext();) {
                        Object o = (Object) i.next();
                        System.out.println("In connection's endpoint set: " + o.getClass() + ": " + o);
                    }

                    // Hook it up
                    connection.getEndpoints().add(ep);
                    ep.setConnection(connection);
                    try {
                        long oid = Registry.getDefault().getJmsManager().saveConnection(connection);
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

    public JmsEndpoint getNewJmsEndpoint() {
        return newJmsEndpoint;
    }
}
