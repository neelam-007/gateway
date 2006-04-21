package com.l7tech.console.panels.saml;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.beaneditor.BeanListener;
import com.l7tech.policy.assertion.xmlsec.SamlAttributeStatement;

import javax.swing.*;
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

    private JButton okButton;

    /* attribute name text field */
    private JTextField attributeNameField;
    /* attribute namespace text field */
    private JTextField attributeNamespaceField;
    /* attribute value text field */
    private JTextField attributeValueField;

    private JButton cancelButton;
    private JRadioButton anyValueRadio;
    private JRadioButton specificValueRadio;
    private JPanel mainPanel;

    private SamlAttributeStatement.Attribute attribute;

    /**
     * @param parent the parent Frame. May be <B>null</B>
     */
    public EditAttributeDialog(JDialog parent, SamlAttributeStatement.Attribute attribute) {
        super(parent, true);
        this.attribute = attribute;
        initResources();
        initComponents();
        pack();
        Utilities.centerOnScreen(this);
        Utilities.setEscKeyStrokeDisposes(this);
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
        setTitle(resources.getString("dialog.title"));

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent event) {
                // user hit window manager close button
                windowAction(CMD_CANCEL);
            }
        });

        attributeNameField.setText(attribute.getName());
        attributeNamespaceField.setText(attribute.getNamespace());
        attributeValueField.setText(attribute.getValue());
        getRootPane().setDefaultButton(okButton);

        // login button (global variable)
        okButton.setActionCommand(CMD_OK);
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                windowAction(event);
            }
        });

        // cancel button
        cancelButton.setActionCommand(CMD_CANCEL);
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                windowAction(event);
            }
        });

        ButtonGroup radioButtons = new ButtonGroup();
        radioButtons.add(specificValueRadio);
        radioButtons.add(anyValueRadio);

        ActionListener buttonEnabler = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                enableButtons();
            }
        };

        specificValueRadio.addActionListener(buttonEnabler);
        anyValueRadio.addActionListener(buttonEnabler);

        if (attribute.isAnyValue()) {
            anyValueRadio.setSelected(true);
        } else {
            specificValueRadio.setSelected(true);
            attributeValueField.setText(attribute.getValue());
        }

        enableButtons();

        add(mainPanel);
    }

    private void enableButtons() {
        attributeValueField.setEnabled(specificValueRadio.isSelected());
    }

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
                if (anyValueRadio.isSelected()) {
                    attribute.setValue(null);
                    attribute.setAnyValue(true);
                } else {
                    attribute.setAnyValue(false);
                    attribute.setValue(attributeValueField.getText());
                }
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

        if (name == null || "".equals(name.trim())) {
            JOptionPane.
            showMessageDialog(this,
                              resources.getString("attributeNameField.error.empty"),
                              resources.getString("attributeNameField.error.title"),
                              JOptionPane.ERROR_MESSAGE);
            attributeNameField.requestFocus();
            return false;
        }

        String value = attributeValueField.getText();
        if (specificValueRadio.isSelected() && (value == null || "".equals(value.trim()) )) {
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
