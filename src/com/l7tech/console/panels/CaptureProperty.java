/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Aug 16, 2005<br/>
 */
package com.l7tech.console.panels;

import com.l7tech.cluster.ClusterProperty;
import com.l7tech.policy.variable.ExpandVariables;
import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.common.gui.util.DocumentSizeFilter;
import com.l7tech.common.gui.util.Utilities;

import javax.swing.*;
import javax.swing.text.AbstractDocument;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import java.util.Map;

/**
 * Simple dialog to capture a property (key+value)
 *
 * @author flascelles@layer7-tech.com
 */
public class CaptureProperty extends JDialog {
    private JPanel mainPanel;
    private JTextArea valueField;
    private JComboBox keyComboBox;
    private JTextArea descField;
    private JButton cancelButton;
    private JButton okButton;

    private String description;
    private final ClusterProperty property;
    private String title;
    private boolean oked = false;
    private Map propertyNamesToDescriptionAndDefault;
    private boolean isEditable;

    public CaptureProperty(JDialog parent, String title, String description, ClusterProperty property, Map suggestedValues, boolean isEditable) {
        super(parent, true);
        this.title = title;
        this.description = description;
        this.property = property;
        this.propertyNamesToDescriptionAndDefault = suggestedValues;
        this.isEditable = isEditable;
        initialize();
    }

    private void initialize() {
        setContentPane(mainPanel);
        setTitle(title);
        descField.setText(description);
        if(property.getName() != null) {
            keyComboBox.setModel(new DefaultComboBoxModel(new String[]{property.getName()}));
            keyComboBox.setSelectedIndex(0);
            keyComboBox.setEnabled(false);
            keyComboBox.setEditable(false);
            valueField.setText(property.getValue());
        } else {
            valueField.setText("");
            if (propertyNamesToDescriptionAndDefault == null || propertyNamesToDescriptionAndDefault.isEmpty()) {
                keyComboBox.setModel(new DefaultComboBoxModel());
                keyComboBox.setEnabled(true);
                keyComboBox.setEditable(true);
            } else {
                keyComboBox.setModel(new DefaultComboBoxModel(propertyNamesToDescriptionAndDefault.keySet().toArray(new String[0])));
                keyComboBox.setEnabled(true);
                keyComboBox.setEditable(true);
                ItemListener itemListener = new ItemListener() {
                    public void itemStateChanged(ItemEvent e) {
                        String[] values = (String[]) propertyNamesToDescriptionAndDefault.get(keyComboBox.getSelectedItem());
                        String description = values==null ? null : values[0];
                        if (description == null) description = "";
                        descField.setText(description);
                        String initialValue = values==null ? null : values[1];
                        if (initialValue == null) initialValue = "";
                        valueField.setText(initialValue);
                    }
                };
                keyComboBox.addItemListener(itemListener);
                keyComboBox.setSelectedIndex(0);
                itemListener.itemStateChanged(null); // init desc
            }
        }
        ((AbstractDocument)((JTextField)keyComboBox.getEditor().getEditorComponent()).getDocument())
                .setDocumentFilter(new DocumentSizeFilter(128));
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (validateUserInput(newKey(), newValue())) {
                    property.setName(newKey());
                    property.setValue(newValue());
                    oked = true;
                    dispose();
                }
            }
        });

        enableReadOnlyIfNeeded();
        Utilities.setEscKeyStrokeDisposes(this);
    }

    private void enableReadOnlyIfNeeded() {
        keyComboBox.setEditable(isEditable);
        valueField.setEditable(isEditable);
    }

    private boolean validateUserInput(String key, String value) {
        // check for empty value or key
        if (key == null || key.length() < 1 || value == null || value.length() < 1) {
            JOptionPane.showMessageDialog(CaptureProperty.this,
                                          "Key and Value cannot be empty",
                                          "Invalid Property Key or Value",
                                          JOptionPane.ERROR_MESSAGE);
            return false;
        }

        // check for properties that have been assigned context variables but that dont support it
        if (hasContextVariables(value) && !keySupportsContextVars(key)) {
                JOptionPane.showMessageDialog(CaptureProperty.this,
                                          "Property value for " + key + " cannot contain context variables",
                                          "Invalid Property Value",
                                          JOptionPane.ERROR_MESSAGE);
            return false;
        }

        // add your validations here
        return true;
    }

    private boolean keySupportsContextVars(String key) {
        if (key.startsWith(AssertionMessages.OVERRIDE_PREFIX)) {
            return false;
        }

        // add here the patterns of other cluster properties that dont support context variables

        return true;
    }

    private boolean hasContextVariables(String s) {
        ExpandVariables.getReferencedNames(s);
        String[] res = ExpandVariables.getReferencedNames(s);
        return res != null && res.length > 0;
    }

    public boolean wasOked() {
        return oked;
    }

    public ClusterProperty getProperty() {
        return property;
    }

    public String newKey() {
        return (String) keyComboBox.getSelectedItem();
    }

    public String newValue() {
        return valueField.getText();
    }
}
