package com.l7tech.console.panels.saml;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.action.Actions;
import com.l7tech.console.beaneditor.BeanListener;
import com.l7tech.policy.assertion.xmlsec.SamlAttributeStatement;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Logger;

/**
 * Edits the SAML Attribute: name, namespace, value.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class EditAttributeDialog extends JDialog {
    static final Logger log = Logger.getLogger(EditAttributeDialog.class.getName());

    /**
     * Resource bundle with default locale
     */
    private ResourceBundle resources = null;

    /**
     * listeners
     */
    private PropertyChangeSupport beanListeners = new PropertyChangeSupport(this);

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

    /* attribute name text field */
    private JTextField attributeNameField = null;
    /* attribute namespace text field */
    private JTextField attributeNamespaceField = null;
    /* attribute value text field */
    private JTextField attributeValueField = null;

    private SamlAttributeStatement.Attribute attribute;


    /**
     * Create a new PasswordDialog
     *
     * @param parent the parent Frame. May be <B>null</B>
     */
    public EditAttributeDialog(JDialog parent, SamlAttributeStatement.Attribute attribute) {
        super(parent, true);
        this.attribute = attribute;
        initResources();
        initComponents();
        pack();
        Utilities.centerOnScreen(this);
        Actions.setEscKeyStrokeDisposes(this);
    }

    /**
     * Adds the bean listener to the list of bean listeners.
     *
     * @param listener the bean listener
     */
    public synchronized void addBeanListener(BeanListener listener) {
        beanListeners.addPropertyChangeListener(listener);
    }

    /**
     * Removes the bean listener from the list of
     *
     * @param listener the bean listener
     */
    public synchronized void removeBeanListener(BeanListener listener) {
        beanListeners.removePropertyChangeListener(listener);
    }

    /**
     * Loads locale-specific resources: strings, images, etc
     */
    private void initResources() {
        Locale locale = Locale.getDefault();
        resources = ResourceBundle.getBundle("com.l7tech.console.panels.saml.resources.EditAttributeDialog", locale);
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

        attributeNameField = new JTextField(); // needed below
        attributeNameField.setText(attribute.getName());
        // attribute name label
        JLabel attributeNameLabel = new JLabel();
        attributeNameLabel.setToolTipText(resources.getString("attributeNameField.tooltip"));
        attributeNameLabel.setText(resources.getString("attributeNameField.label"));
        attributeNameLabel.setLabelFor(attributeNameField);
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(11, 12, 0, 0);

        contents.add(attributeNameLabel, constraints);

        // attribute name field
        attributeNameField.setToolTipText(resources.getString("attributeNameField.tooltip"));


        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1.0;
        constraints.gridwidth = 2;
        constraints.insets = new Insets(11, 7, 0, 11);
        contents.add(attributeNameField, constraints);


        attributeNamespaceField = new JTextField();
        attributeNamespaceField.setText(attribute.getNamespace());

        // attribute namespace label
        JLabel attributeNamespaceLabel = new JLabel();
        attributeNamespaceLabel.setToolTipText(resources.getString("attributeNamespaceField.tooltip"));
        attributeNamespaceLabel.setText(resources.getString("attributeNamespaceField.label"));
        attributeNamespaceLabel.setLabelFor(attributeNamespaceField);
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(11, 12, 0, 0);

        contents.add(attributeNamespaceLabel, constraints);

        // attribute namespace field
        attributeNamespaceField.setToolTipText(resources.getString("attributeNamespaceField.tooltip"));

        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1.0;
        constraints.gridwidth = 2;
        constraints.insets = new Insets(11, 7, 0, 11);
        contents.add(attributeNamespaceField, constraints);


        attributeValueField = new JTextField();
        attributeValueField.setText(attribute.getValue());

        // attribute value label
        JLabel attributeValueLabel = new JLabel();
        attributeValueLabel.setToolTipText(resources.getString("attributeValueField.tooltip"));
        attributeValueLabel.setText(resources.getString("attributeValueField.label"));
        attributeValueLabel.setLabelFor(attributeValueField);
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(11, 12, 0, 0);

        contents.add(attributeValueLabel, constraints);

        // attribute namespace field
        attributeValueField.setToolTipText(resources.getString("attributeValueField.tooltip"));

        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 2;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1.0;
        constraints.gridwidth = 2;
        constraints.insets = new Insets(11, 7, 0, 11);
        contents.add(attributeValueField, constraints);

        constraints = new GridBagConstraints();
        constraints.gridx = 2;
        constraints.gridy = 4;
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
     * <p/>
     * Sets the variable okButton
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
     * @param actionCommand may be null
     */
    private void windowAction(Object actionCommand) {
        String cmd = null;

        if (actionCommand != null) {
            if (actionCommand instanceof ActionEvent) {
                cmd = ((ActionEvent)actionCommand).getActionCommand();
            } else {
                cmd = actionCommand.toString();
            }
        }
        if (cmd == null) {
            // do nothing
        } else if (cmd.equals(CMD_CANCEL)) {
            fireCancelled();
            dispose();
        } else if (cmd.equals(CMD_OK)) {
            if (validateInput()) {
                attribute.setName(attributeNameField.getText());
                attribute.setNamespace(attributeNamespaceField.getText());
                attribute.setValue(attributeValueField.getText());
                fireEditAccepted();
                dispose();
            } else {
                return;
            }
        }
    }

    private void fireCancelled() {
        PropertyChangeListener[] listeners = beanListeners.getPropertyChangeListeners();
        for (int i = 0; i < listeners.length; i++) {
            PropertyChangeListener listener = listeners[i];
            ((BeanListener)listener).onEditCancelled(this, attribute);
        }
    }

    private void fireEditAccepted() {
        PropertyChangeListener[] listeners = beanListeners.getPropertyChangeListeners();
        for (int i = 0; i < listeners.length; i++) {
            PropertyChangeListener listener = listeners[i];
            ((BeanListener)listener).onEditAccepted(this, attribute);
        }
    }

    /**
     * validate the username and context
     *
     * @return true validated, false othwerwise
     */
    private boolean validateInput() {
        String name = attributeNameField.getText();

        if (name == null || "".equals(name)) {
            JOptionPane.
            showMessageDialog(this,
                              resources.getString("attributeNameField.error.empty"),
                              resources.getString("attributeNameField.error.title"),
                              JOptionPane.ERROR_MESSAGE);
            attributeNameField.requestFocus();
            return false;
        }

        String value = attributeValueField.getText();
        if (value == null || "".equals(value)) {
            JOptionPane.
            showMessageDialog(this,
                              resources.getString("attributeValueField.error.empty"),
                              resources.getString("attributeValueField.error.title"),
                              JOptionPane.ERROR_MESSAGE);
            attributeValueField.requestFocus();
            return false;
        }
        return true;
    }
}
