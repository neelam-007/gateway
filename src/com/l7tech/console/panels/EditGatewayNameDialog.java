package com.l7tech.console.panels;

import com.l7tech.cluster.ClusterStatusAdmin;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.console.util.TopComponents;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.rmi.RemoteException;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Logger;

/*
 * A class for providing a dialog window to the users for changing the gateway name.
 *
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

public class EditGatewayNameDialog extends JDialog {
    static final Logger logger = Logger.getLogger(EditGatewayNameDialog.class.getName());

    /** Resource bundle with default locale */
    private ResourceBundle resources = null;

    /**
     * Command string for a cancel action (e.g.,a button or menu item).
     * This string is never presented to the user and should
     * not be internationalized.
     */
    private String CMD_CANCEL = "cmd.cancel";

    /**
     * Command string for a login action (e.g.,a button or menu item).
     * This string is never presented to the user and should
     * not be internationalized.
     */
    private String CMD_OK = "cmd.ok";

    private JButton okButton = null;

    /* new password text field */
    private JTextField newGatewayNameField = null;
    private ClusterStatusAdmin clusterStatusAdmin;
    private String oldGatewayName;
    private String nodeId;

    /**
     * Create a new EditGatewayNameDialog
     *
     * @param parent the parent Frame. May be <B>null</B>
     */
    public EditGatewayNameDialog(Frame parent, ClusterStatusAdmin clusterStatusAdmin, String nodeId, String oldGatewayName) {
        super(parent, true);
        // this.listener = l;
        this.clusterStatusAdmin = clusterStatusAdmin;
        this.nodeId = nodeId;
        this.oldGatewayName = oldGatewayName;
        initResources();
        initComponents();
        pack();
        Utilities.centerOnScreen(this);
    }

    /**
     * Loads locale-specific resources: strings, images, etc
     */
    private void initResources() {
        Locale locale = Locale.getDefault();
        resources = ResourceBundle.getBundle("com.l7tech.console.resources.EditGatewayNameDialog", locale);
    }

    /**
     * This method is called from within the constructor to
     * initialize the dialog.
     */
    private void initComponents() {

        GridBagConstraints constraints = null;

        Container contents = getContentPane();
        contents.setLayout(new GridBagLayout());
        setTitle(resources.getString("dialog.title"));

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent event) {
                // user hit window manager close button
                windowAction(CMD_CANCEL);
            }
        });

        newGatewayNameField = new JTextField(); // needed below
        newGatewayNameField.setText(oldGatewayName);
        // password label
        JLabel passwordLabel = new JLabel();
        passwordLabel.setToolTipText(resources.getString("newGatewayNameField.tooltip"));
        passwordLabel.setText(resources.getString("newGatewayNameField.label"));
        passwordLabel.setLabelFor(newGatewayNameField);
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(11, 12, 0, 0);

        contents.add(passwordLabel, constraints);

        // password field
        newGatewayNameField.setToolTipText(resources.getString("newGatewayNameField.tooltip"));


        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1.0;
        constraints.gridwidth = 2;
        constraints.insets = new Insets(11, 7, 0, 11);
        contents.add(newGatewayNameField, constraints);

        constraints = new GridBagConstraints();
        constraints.gridx = 2;
        constraints.gridy = 2;
        constraints.gridwidth = 2;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.EAST;
        constraints.weightx = 1.0;
        constraints.insets = new Insets(17, 12, 11, 11);
        JPanel buttonPanel = createButtonPanel(); // sets global okButton
        contents.add(buttonPanel, constraints);

        getRootPane().setDefaultButton(okButton);
    } // initComponents()

    /**
     * Creates the panel of buttons that goes along the bottom
     * of the dialog
     *
     * Sets the variable okButton
     *
     */
    private JPanel createButtonPanel() {

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, 0));

        // login button (global variable)
        okButton = new JButton();
        okButton.setText(resources.getString("okButton.label"));
        okButton.setToolTipText(resources.getString("okButton.tooltip"));
        okButton.setActionCommand(CMD_OK);
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                windowAction(event);
            }
        });
        panel.add(okButton);

        // space
        panel.add(Box.createRigidArea(new Dimension(5, 0)));

        // cancel button
        JButton cancelButton = new JButton();
        cancelButton.setText(resources.getString("cancelButton.label"));
        cancelButton.setActionCommand(CMD_CANCEL);
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                windowAction(event);
            }
        });
        panel.add(cancelButton);

        // space
        panel.add(Box.createRigidArea(new Dimension(5, 0)));
        Utilities.equalizeButtonSizes(new JButton[]{cancelButton, okButton});
        return panel;
    } // createButtonPanel()


    /**
     * The user has selected an option. Here we close and dispose
     * the dialog.
     * If actionCommand is an ActionEvent, getCommandString() is
     * called, otherwise toString() is used to get the action command.
     *
     * @param actionCommand
     *               may be null
     */
    private void windowAction(Object actionCommand) {
        String cmd = null;

        if (actionCommand != null) {
            if (actionCommand instanceof ActionEvent) {
                cmd = ((ActionEvent) actionCommand).getActionCommand();
            } else {
                cmd = actionCommand.toString();
            }
        }
        if (cmd == null) {
            // do nothing
        } else if (cmd.equals(CMD_CANCEL)) {
            setVisible(false);
        } else if (cmd.equals(CMD_OK)) {
            if (validateInput()) {
                changeGatewayName(nodeId, newGatewayNameField.getText());
            } else {
                newGatewayNameField.requestFocus();
                return;
            }
        }
    }

    /**
     * validate the username and context
     * @return true validated, false othwerwise
     */
    private boolean validateInput() {
        String newPass = newGatewayNameField.getText();

        if (newPass == null || "".equals(newPass)) {
            JOptionPane.
                    showMessageDialog(this,
                            resources.getString("newGatewayNameField.error.empty"),
                            resources.getString("newGatewayNameField.warning.title"),
                            JOptionPane.ERROR_MESSAGE);
            return false;
        }

        return true;
    }

    /**
     * change gateway name
     *
     * @param newName new gateway name
     */
    private void changeGatewayName(final String nodeId, final String newName) {

        if(clusterStatusAdmin == null) {
            logger.warning("ClusterStatusAdmin service is not available. Cannot rename the node: " + oldGatewayName);

            JOptionPane.
                    showMessageDialog(this,
                            resources.getString("newGatewayNameField.error.connection.lost"),
                            resources.getString("newGatewayNameField.error.title"),
                            JOptionPane.ERROR_MESSAGE);
            dispose();
            return;
        }

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {

                try {
                    clusterStatusAdmin.changeNodeName(nodeId, newName);
                    logger.info("Gateway name changed. Old name: " + oldGatewayName + " , New name: " + newName);

                    // update the status message on the Main Window
                    TopComponents.getInstance().getMainWindow().updateNodeNameInStatusMessage(oldGatewayName, newName);

                } catch (UpdateException e) {
                    logger.warning("Cannot rename the node: " + oldGatewayName);
                    JOptionPane.
                            showMessageDialog(EditGatewayNameDialog.this,
                                    resources.getString("newGatewayNameField.error.update"),
                                    resources.getString("newGatewayNameField.error.title"),
                                    JOptionPane.ERROR_MESSAGE);

                } catch (RemoteException e) {
                    logger.warning("Remote Exception. Cannot rename the node: " + oldGatewayName);
                    JOptionPane.
                            showMessageDialog(EditGatewayNameDialog.this,
                                    resources.getString("newGatewayNameField.error.remote.exception"),
                                    resources.getString("newGatewayNameField.error.title"),
                                    JOptionPane.ERROR_MESSAGE);
                } finally {
                    dispose();
                }

            }
        });
    }

}
