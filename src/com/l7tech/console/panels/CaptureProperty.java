/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Aug 16, 2005<br/>
 */
package com.l7tech.console.panels;

import com.l7tech.cluster.ClusterProperty;

import javax.swing.*;
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
    private Map propertyNamesToDescriptions;

    public CaptureProperty(JDialog parent, String title, String description, ClusterProperty property, Map suggestedValues) {
        super(parent, true);
        this.title = title;
        this.description = description;
        this.property = property;
        this.propertyNamesToDescriptions = suggestedValues;
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
        } else {
            if (propertyNamesToDescriptions == null || propertyNamesToDescriptions.isEmpty()) {
                keyComboBox.setModel(new DefaultComboBoxModel());
                keyComboBox.setEnabled(true);
                keyComboBox.setEditable(true);
            } else {
                keyComboBox.setModel(new DefaultComboBoxModel(propertyNamesToDescriptions.keySet().toArray(new String[0])));
                keyComboBox.setEnabled(true);
                keyComboBox.setEditable(true);
                ItemListener itemListener = new ItemListener() {
                    public void itemStateChanged(ItemEvent e) {
                        String description = (String) propertyNamesToDescriptions.get(keyComboBox.getSelectedItem());
                        if (description == null) description = "";
                        descField.setText(description);
                    }
                };
                keyComboBox.addItemListener(itemListener);
                keyComboBox.setSelectedIndex(0);
                itemListener.itemStateChanged(null); // init desc
            }
        }
        valueField.setText(property.getValue());
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (newKey() == null || newKey().length() < 1 || newValue() == null || newValue().length() < 1) {
                    JOptionPane.showMessageDialog(CaptureProperty.this,
                                                  "Key and Value cannot be empty",
                                                  "Invalid Property Key or Value",
                                                  JOptionPane.ERROR_MESSAGE);
                } else {
                    property.setName(newKey());
                    property.setValue(newValue());
                    oked = true;
                    dispose();
                }
            }
        });
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
