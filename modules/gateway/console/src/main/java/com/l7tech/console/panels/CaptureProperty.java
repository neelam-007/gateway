package com.l7tech.console.panels;

import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.gateway.common.cluster.ClusterPropertyDescriptor;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.FindException;
import com.l7tech.console.util.Registry;
import com.l7tech.util.Functions.Unary;
import com.l7tech.util.TextUtils;

import javax.swing.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Simple dialog to capture a property (key+value)
 *
 * @author flascelles@layer7-tech.com
 */
public class CaptureProperty extends JDialog {
    private final Logger logger = Logger.getLogger(CaptureProperty.class.getName());

    private JPanel mainPanel;
    private JTextArea valueField;
    private SimpleEditableSearchComboBox keyComboBox;
    private JTextArea descField;
    private JButton cancelButton;
    private JButton okButton;

    private String description;
    private Unary<Boolean,String> validator;
    private final ClusterProperty property;
    private String title;
    private boolean oked = false;
    private Collection<ClusterPropertyDescriptor> descriptors;
    private Collection<ClusterProperty> properties;
    private boolean isEditable;

    public CaptureProperty(JDialog parent, String title, String description, ClusterProperty property, Collection<ClusterPropertyDescriptor> descriptors, boolean isEditable) {
        super(parent, true);

        this.title = title;
        this.description = description;
        this.property = property;
        this.descriptors = descriptors;
        this.isEditable = isEditable;

        initialize();
    }

    private void initialize() {
        setContentPane(mainPanel);
        setTitle(title);

        keyComboBox.setEditableDocumentSize(128);
        keyComboBox.addItemSelectedListener((e) -> {
            String selectedItem = (String) e.getItem();
            ClusterPropertyDescriptor selectedDescriptor = getClusterPropertyDescriptorByName(descriptors, selectedItem);

            if (keyComboBox.hasResults()) {
                String firstMatch = keyComboBox.getFirstSearchResult();
                if (firstMatch.toLowerCase().equals(selectedItem.toLowerCase())) {
                    selectedDescriptor = getClusterPropertyDescriptorByName(descriptors, firstMatch);
                }
            }

            populateSelectedProperty(selectedDescriptor);
        });

        if (property.getName() != null) {
            initializeForExistingProperty();
        } else {
            initializeForNewProperty();
        }

        cancelButton.addActionListener(e -> dispose());

        okButton.addActionListener(e -> {
            if (validateUserInput(newKey(), newValue())) {
                property.setName(newKey());
                property.setValue(newValue());
                property.setProperty(ClusterProperty.DESCRIPTION_PROPERTY_KEY, newDescription());
                oked = true;
                dispose();
            }
        });

        enableReadOnlyIfNeeded();
        Utilities.setEscKeyStrokeDisposes(this);
    }

    private void initializeForNewProperty() {
        keyComboBox.updateSearchableItems(getClusterPropertyNames(descriptors));
        populateSelectedPropertyFields(description, "", true);
        descField.requestFocus();
    }

    private void initializeForExistingProperty() {
        final ClusterPropertyDescriptor selectedDescriptor = getClusterPropertyDescriptorByName(descriptors, property.getName());
        final boolean userDefined = (selectedDescriptor == null);

        if (userDefined) {
            keyComboBox.updateSearchableItems(Collections.singletonList(property.getName()));
            descField.requestFocus();
        } else {
            validator = (s) -> selectedDescriptor.isValid(s);
            keyComboBox.updateSearchableItems(Collections.singletonList(selectedDescriptor.getName()));
            valueField.requestFocus();
        }

        keyComboBox.setEditableText(property.getName());
        keyComboBox.setEnabled(false);

        populateSelectedPropertyFields(description, property.getValue(), userDefined);
    }

    private void populateSelectedProperty(final ClusterPropertyDescriptor selectedDescriptor) {
        boolean userDefined = (selectedDescriptor == null);

        if (userDefined) {
            populateSelectedPropertyFields("", "", userDefined);
            validator = null;
        } else {
            populateSelectedPropertyFields(selectedDescriptor.getDescription(),
                    getClusterPropertyValue(getExistingClusterProperties(), selectedDescriptor), userDefined);
            validator = (s) -> selectedDescriptor.isValid(s);
        }
    }

    private void populateSelectedPropertyFields(String description, String value, boolean userDefined) {
        descField.setText(description);
        descField.setCaretPosition(0);
        descField.setEnabled(userDefined);
        descField.setEditable(userDefined);
        descField.setOpaque(userDefined);

        valueField.setText(value);
        valueField.setCaretPosition(0);
    }

    /**
     * Retrieve all cluster properties existing in the Global Cluster Properties table.
     * @return a list of existing cluster properties
     */
    private Collection<ClusterProperty> getExistingClusterProperties() {
        if (properties == null) {
            try {
                properties = Registry.getDefault().getClusterStatusAdmin().getAllProperties();
            } catch (FindException e) {
                logger.log(Level.SEVERE, "exception getting properties", e);
            }
        }

        return properties;
    }

    /**
     * Get the value of the cluster property.
     * @param properties: cluster properties
     * @param name: the name of the cluster property
     * @return the value of the cluster property
     */
    private String getClusterPropertyValue(final Collection<ClusterProperty> properties, String name) {
        String value = null;

        if (properties != null) {
            for (ClusterProperty prop : properties) {
                if (prop.getName().equals(name)) {
                    value = prop.getValue();
                    break;
                }
            }
        }

        return value;
    }

    /**
     * Get the value of the cluster property.
     * @param properties: cluster properties
     * @param descriptor: the cluster property descriptor
     * @return the value of the cluster property
     */
    private String getClusterPropertyValue(final Collection<ClusterProperty> properties, ClusterPropertyDescriptor descriptor) {
        String value = getClusterPropertyValue(properties, descriptor.getName());
        return value == null ? descriptor.getDefaultValue() : value;
    }

    /**
     * Get the list of visible cluster property names
     * @param descriptors collection of descriptors
     * @return list of visible cluster property names
     */
    private List<String> getClusterPropertyNames(final Collection<ClusterPropertyDescriptor> descriptors) {
        List<String> names = new ArrayList<String>();

        if (descriptors != null) {
            for (ClusterPropertyDescriptor descriptor : descriptors) {
                if (descriptor.isVisible()) {
                    names.add(descriptor.getName());
                }
            }
        }

        return names;
    }

    /**
     * Get the cluster property descriptor by name
     * @param descriptors collection of descriptors
     * @param name name of the cluster property
     * @return cluster property descriptor
     */
    private ClusterPropertyDescriptor getClusterPropertyDescriptorByName(
            final Collection<ClusterPropertyDescriptor> descriptors,
            final String name ) {
        ClusterPropertyDescriptor property = null;

        if ( descriptors != null && name != null ) {
            for ( ClusterPropertyDescriptor descriptor : descriptors ) {
                if ( name.equals(descriptor.getName()) ) {
                    property = descriptor;
                    break;
                }
            }
        }

        return property;
    }

    private void enableReadOnlyIfNeeded() {
        keyComboBox.setEditable(isEditable);
        valueField.setEditable(isEditable);
        descField.setEditable(isEditable);
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

        // Validate the value
        if ( validator != null ) {
            if (!validator.call( value )) {
                JOptionPane.showMessageDialog(CaptureProperty.this,
                        Utilities.getTextDisplayComponent( "The value '"+TextUtils.truncStringMiddle( value, 128 )+"' is not valid.\n\n" + descField.getText() ),
                        "Invalid Property Value",
                        JOptionPane.ERROR_MESSAGE);

                return false;
            }
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
        String[] res = Syntax.getReferencedNames(s);
        return res != null && res.length > 0;
    }

    public boolean wasOked() {
        return oked;
    }

    public ClusterProperty getProperty() {
        return property;
    }

    public String newKey() {
        return keyComboBox.getEditableText();
    }

    public String newValue() {
        return valueField.getText();
    }

    /**
     * Returns the description in the description field.
     * @return
     */
    public String newDescription(){
        return descField.getText();
    }
}
