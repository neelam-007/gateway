package com.l7tech.console.panels.saml;

import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.TextListCellRenderer;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.security.saml.SamlConstants;
import com.l7tech.console.beaneditor.BeanListener;
import com.l7tech.policy.assertion.xmlsec.SamlAttributeStatement;
import com.l7tech.util.Functions;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.EnumSet;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import static com.l7tech.policy.assertion.xmlsec.SamlAttributeStatement.Attribute.*;
import static com.l7tech.policy.assertion.xmlsec.SamlAttributeStatement.Attribute.AttributeValueAddBehavior.*;

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
    private JButton cancelButton;

    private JTextField attributeNameField;
    private JTextField attributeNamespaceField;
    private JTextArea attributeValueField;
    private JRadioButton nameFormatUnspecifiedRadioButton;
    private JRadioButton nameFormatURIRefRadioButton;
    private JRadioButton nameFormatBasicRadioButton;
    private JRadioButton nameFormatOtherRadioButton;
    private JTextField attributeNameFormatTextField;
    private JRadioButton anyValueRadio;
    private JRadioButton specificValueRadio;
    private JPanel nameFormatPanel;
    private JPanel mainPanel;
    private JCheckBox repeatIfMultivaluedCheckBox;
    private JComboBox msgElmBehaviorComboBox;

    private final SamlAttributeStatement.Attribute attribute;
    private final boolean issueMode;

    /**
     * @param parent the parent Frame. May be <B>null</B>
     * @param issueMode
     */
    public EditAttributeDialog(JDialog parent, SamlAttributeStatement.Attribute attribute, int samlVersion, boolean issueMode) {
        super(parent, true);
        this.attribute = attribute;
        this.issueMode = issueMode;
        initResources();
        initComponents(samlVersion!=2, samlVersion!=1);
        pack();
        Utilities.centerOnScreen(this);
        Utilities.setEscKeyStrokeDisposes(this);
        setResizable(false);
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
    private void initComponents(boolean enableNamespace, boolean enableNameFormat) {
        setTitle(resources.getString(issueMode ? "issueDialog.title" : "dialog.title"));

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                // user hit window manager close button
                windowAction(CMD_CANCEL);
            }
        });

        msgElmBehaviorComboBox.setModel(
                new DefaultComboBoxModel(
                        EnumSet.of(
                                STRING_CONVERT,
                                ADD_AS_XML).toArray()));

        msgElmBehaviorComboBox.setRenderer(new TextListCellRenderer<AttributeValueAddBehavior>(
                new Functions.Unary<String, AttributeValueAddBehavior>() {
                    @Override
                    public String call(final AttributeValueAddBehavior dataType) {
                        return dataType.getValue();
                    }
                }));

        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(nameFormatUnspecifiedRadioButton);
        buttonGroup.add(nameFormatURIRefRadioButton);
        buttonGroup.add(nameFormatBasicRadioButton);
        buttonGroup.add(nameFormatOtherRadioButton);

        attributeNameField.setText(attribute.getName());
        attributeNamespaceField.setText(attribute.getNamespace());
        attributeValueField.setText(attribute.getValue());
        repeatIfMultivaluedCheckBox.setSelected(attribute.isRepeatIfMulti());
        msgElmBehaviorComboBox.setSelectedItem(attribute.getAddBehavior());

        String nameFormat = attribute.getNameFormat();
        if (nameFormat == null || nameFormat.length()==0 ||
            SamlConstants.ATTRIBUTE_NAME_FORMAT_UNSPECIFIED.equals(nameFormat)) {
            nameFormatUnspecifiedRadioButton.setSelected(true);
            attributeNameFormatTextField.setText(SamlConstants.ATTRIBUTE_NAME_FORMAT_UNSPECIFIED);
        } else if (SamlConstants.ATTRIBUTE_NAME_FORMAT_URIREFERENCE.equals(nameFormat)) {
            nameFormatURIRefRadioButton.setSelected(true);
            attributeNameFormatTextField.setText(SamlConstants.ATTRIBUTE_NAME_FORMAT_URIREFERENCE);
        } else if (SamlConstants.ATTRIBUTE_NAME_FORMAT_BASIC.equals(nameFormat)) {
            nameFormatBasicRadioButton.setSelected(true);
            attributeNameFormatTextField.setText(SamlConstants.ATTRIBUTE_NAME_FORMAT_BASIC);
        } else {
            nameFormatOtherRadioButton.setSelected(true);
            attributeNameFormatTextField.setText(nameFormat);
        }
        
        getRootPane().setDefaultButton(okButton);

        // login button (global variable)
        okButton.setActionCommand(CMD_OK);
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                windowAction(event);
            }
        });

        // cancel button
        cancelButton.setActionCommand(CMD_CANCEL);
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                windowAction(event);
            }
        });

        ButtonGroup radioButtons = new ButtonGroup();
        radioButtons.add(specificValueRadio);
        radioButtons.add(anyValueRadio);

        ActionListener buttonEnabler = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableButtons();
            }
        };

        nameFormatOtherRadioButton.addChangeListener(new ChangeListener(){
            @Override
            public void stateChanged(ChangeEvent e) {
                attributeNameFormatTextField.setEnabled(nameFormatOtherRadioButton.isSelected());
                if (attributeNameFormatTextField.isEnabled()) {
                    attributeNameFormatTextField.setText("");
                    attributeNameFormatTextField.requestFocus();
                }
            }
        });
        ChangeListener nameFormatChangeLister = new ChangeListener(){
            @Override
            public void stateChanged(ChangeEvent e) {
                if (nameFormatUnspecifiedRadioButton.isSelected()) {
                    attributeNameFormatTextField.setText(SamlConstants.ATTRIBUTE_NAME_FORMAT_UNSPECIFIED);
                }
                else if (nameFormatURIRefRadioButton.isSelected()) {
                    attributeNameFormatTextField.setText(SamlConstants.ATTRIBUTE_NAME_FORMAT_URIREFERENCE);
                }
                else if (nameFormatBasicRadioButton.isSelected()) {
                    attributeNameFormatTextField.setText(SamlConstants.ATTRIBUTE_NAME_FORMAT_BASIC);
                }
            }
        };
        nameFormatUnspecifiedRadioButton.addChangeListener(nameFormatChangeLister);
        nameFormatURIRefRadioButton.addChangeListener(nameFormatChangeLister);
        nameFormatBasicRadioButton.addChangeListener(nameFormatChangeLister);


        specificValueRadio.addActionListener(buttonEnabler);
        anyValueRadio.addActionListener(buttonEnabler);

        if (issueMode) {
            anyValueRadio.setEnabled(false);
            anyValueRadio.setVisible(false);
            specificValueRadio.setVisible(false);
            repeatIfMultivaluedCheckBox.setVisible(true);
            msgElmBehaviorComboBox.setVisible(true);
        } else {
            repeatIfMultivaluedCheckBox.setVisible(false);
            msgElmBehaviorComboBox.setVisible(false);
        }

        if (attribute.isAnyValue()) {
            anyValueRadio.setSelected(true);
        } else {
            specificValueRadio.setSelected(true);
            attributeValueField.setText(attribute.getValue());
        }

        enableButtons();

        // enable / disable optional elements
        Utilities.setEnabled(attributeNamespaceField, enableNamespace);
        Utilities.setEnabled(nameFormatPanel, enableNameFormat);
        attributeNameFormatTextField.setEnabled(nameFormatOtherRadioButton.isSelected());

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

        if (cmd == null) return;

        if (cmd.equals(CMD_CANCEL)) {
            fireCancelled();
            dispose();
        } else if (cmd.equals(CMD_OK)) {
            if (validateInput()) {
                attribute.setName(attributeNameField.getText());
                attribute.setNamespace(attributeNamespaceField.getText());

                if (SamlConstants.ATTRIBUTE_NAME_FORMAT_UNSPECIFIED.equals(attributeNameFormatTextField.getText())) {
                    attribute.setNameFormat(null); // unspecified is the default
                } else {
                    attribute.setNameFormat(attributeNameFormatTextField.getText());
                }

                if (anyValueRadio.isSelected()) {
                    attribute.setValue(null);
                    attribute.setAnyValue(true);
                } else {
                    attribute.setAnyValue(false);
                    attribute.setValue(attributeValueField.getText().trim());
                }
                attribute.setRepeatIfMulti(repeatIfMultivaluedCheckBox.isSelected());
                attribute.setAddBehavior((AttributeValueAddBehavior) msgElmBehaviorComboBox.getSelectedItem());
                fireEditAccepted();
                dispose();
            }
        }
    }

    private void fireCancelled() {
        PropertyChangeListener[] listeners = beanListeners.getPropertyChangeListeners();
        for (PropertyChangeListener listener : listeners) {
            ((BeanListener) listener).onEditCancelled(this, attribute);
        }
    }

    private void fireEditAccepted() {
        PropertyChangeListener[] listeners = beanListeners.getPropertyChangeListeners();
        for (PropertyChangeListener listener : listeners) {
            ((BeanListener) listener).onEditAccepted(this, attribute);
        }
    }

    /**
     * validate the username and context
     *
     * @return true validated, false otherwise
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

        if (nameFormatPanel.isEnabled() && nameFormatOtherRadioButton.isSelected()) {
            String nameFormat = attributeNameFormatTextField.getText();

            if (nameFormat == null || "".equals(nameFormat.trim())) {
                JOptionPane.
                showMessageDialog(this,
                                  resources.getString("attributeNameFormatTextField.error.empty"),
                                  resources.getString("attributeNameFormatTextField.error.title"),
                                  JOptionPane.ERROR_MESSAGE);
                attributeNameFormatTextField.requestFocus();
                return false;
            }
        }

        String value = attributeValueField.getText().trim();
        if (specificValueRadio.isSelected() && "".equals(value.trim()) ) {
            JOptionPane.
            showMessageDialog(this,
                              resources.getString("attributeValueField.error.empty"),
                              resources.getString("attributeValueField.error.title"),
                              JOptionPane.ERROR_MESSAGE);
            attributeValueField.requestFocus();
            return false;
        }

        // validate configuration when repeat if Multivalued is on
        if (repeatIfMultivaluedCheckBox.isSelected()) {
            if (!Syntax.isOnlyASingleVariableReferenced(value)) {
                JOptionPane.
                showMessageDialog(this,
                        resources.getString("attributeValueField.error.moreThanOneVariableReferenced"),
                        resources.getString("attributeValueField.error.title"),
                        JOptionPane.ERROR_MESSAGE);
                attributeValueField.requestFocus();

                return false;
            }

        }

        return true;
    }
}
