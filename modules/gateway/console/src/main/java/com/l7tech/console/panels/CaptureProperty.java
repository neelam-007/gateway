package com.l7tech.console.panels;

import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.gateway.common.cluster.ClusterPropertyDescriptor;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gui.util.DocumentSizeFilter;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.FindException;
import com.l7tech.console.util.Registry;
import com.l7tech.util.Functions.Unary;
import com.l7tech.util.TextUtils;

import javax.swing.*;
import javax.swing.text.AbstractDocument;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
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
    private JComboBox keyComboBox;
    private JTextArea descField;
    private JButton cancelButton;
    private JButton okButton;

    private String description;
    private Unary<Boolean,String> validator;
    private final ClusterProperty property;
    private String title;
    private boolean oked = false;
    private Collection<ClusterPropertyDescriptor> descriptors;
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
        descField.setText(description);
        descField.setCaretPosition(0);
        if(property.getName() != null) {
            keyComboBox.setModel(new DefaultComboBoxModel(new String[]{property.getName()}));
            keyComboBox.setSelectedIndex(0);
            keyComboBox.setEnabled(false);
            keyComboBox.setEditable(false);
            valueField.setText(property.getValue());
            valueField.setCaretPosition(0);
            final ClusterPropertyDescriptor propertyDescriptor =
                    getClusterPropertyDescriptorByName(descriptors, (String) keyComboBox.getSelectedItem());
            validator = propertyDescriptor == null ? null : new Unary<Boolean, String>() {
                @Override
                public Boolean call( final String s ) {
                    return propertyDescriptor.isValid( s );
                }
            };
        } else {
            valueField.setText("");
            if (descriptors == null || descriptors.isEmpty()) {
                keyComboBox.setModel(new DefaultComboBoxModel());
                keyComboBox.setEnabled(true);
                keyComboBox.setEditable(true);
            } else {
                keyComboBox.setModel(new DefaultComboBoxModel(getNames(descriptors)));
                keyComboBox.setEnabled(true);
                keyComboBox.setEditable(true);
                ItemListener itemListener = new ItemListener() {
                    public void itemStateChanged(ItemEvent e) {
                        final ClusterPropertyDescriptor propertyDescriptor =
                                getClusterPropertyDescriptorByName(descriptors, (String) keyComboBox.getSelectedItem());
                        // Get and set description
                        String description = propertyDescriptor == null? "" : propertyDescriptor.getDescription();
                        descField.setText(description);
                        descField.setCaretPosition(0);

                        // Get and set value
                        String initialValue = propertyDescriptor == null? "" : propertyDescriptor.getDefaultValue();
                        String currentValue = getCurrentPropValue((String) keyComboBox.getSelectedItem());
                        valueField.setText(currentValue == null? initialValue : currentValue);
                        validator = propertyDescriptor == null ? null : new Unary<Boolean, String>() {
                            @Override
                            public Boolean call( final String s ) {
                                return propertyDescriptor.isValid( s );
                            }
                        };
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

    /**
     * Retrieve all cluster properties existing in the Global Cluster Properties table.
     * @return a list of existing cluster properties
     */
    private Collection<ClusterProperty> propulateExistingClusterProps() {
        Collection<ClusterProperty> existingProperties = new ArrayList<ClusterProperty>();
        try {
            Collection<ClusterProperty> allProperties = Registry.getDefault().getClusterStatusAdmin().getAllProperties();
            for (ClusterProperty property : allProperties) {
                if (!property.isHiddenProperty())
                    existingProperties.add(property);
            }

        } catch (FindException e) {
            logger.log(Level.SEVERE, "exception getting properties", e);
        }
        return existingProperties;
    }

    /**
     * Get the value of the clsuter property whose name is propName.
     * @param propName: the name of the cluster propery
     * @return the value of the cluster property
     */
    private String getCurrentPropValue(String propName) {
        String value = null;
        for (ClusterProperty prop: propulateExistingClusterProps()) {
            if (prop.getName().equals(propName)) {
                value = prop.getValue();
                break;
            }
        }
        return value;
    }

    private String[] getNames( final Collection<ClusterPropertyDescriptor> properties ) {
        List<String> names = new ArrayList<String>();

        for ( ClusterPropertyDescriptor descriptor : properties ) {
            if ( descriptor.isVisible() )
                names.add( descriptor.getName() );
        }

        return names.toArray(new String[names.size()]);
    }

    private ClusterPropertyDescriptor getClusterPropertyDescriptorByName(
            final Collection<ClusterPropertyDescriptor> properties,
            final String name ) {
        ClusterPropertyDescriptor property = null;

        if ( properties != null ) {
            for ( ClusterPropertyDescriptor descriptor : properties ) {
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
        return (String) keyComboBox.getSelectedItem();
    }

    public String newValue() {
        return valueField.getText();
    }
}
