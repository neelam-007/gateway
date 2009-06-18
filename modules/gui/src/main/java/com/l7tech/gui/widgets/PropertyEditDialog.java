/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.gui.widgets;

import com.l7tech.gui.util.RunOnChangeListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * Edit JDialog for property name/value pairs.
 *
 * <p>Both the name and value are editable.</p>
 *
 * @author alex
 */
public class PropertyEditDialog extends JDialog {

    //- PUBLIC

    /**
     * Create a new property editor with the given title and values.
     *
     * @param owner The parent window
     * @param title The title for the dialog
     * @param propertyName The name of the property
     * @param propertyValue The value of the property
     */
    public PropertyEditDialog( final Window owner,
                               final String title,
                               final String propertyName,
                               final String propertyValue ) {
        super( owner, title, PropertyEditDialog.DEFAULT_MODALITY_TYPE );
        this.propertyName = propertyName;
        this.propertyValue = propertyValue;

        nameTextField.setText(propertyName);
        valueTextField.setText(propertyValue);

        nameTextField.getDocument().addDocumentListener(new RunOnChangeListener(new Runnable() {
            public void run() {
                enableOrDisableOkButton();
            }
        }));

        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ok = true;
                set(nameTextField.getText(), valueTextField.getText());
                dispose();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ok = false;
                set(null, null);
                dispose();
            }
        });

        enableOrDisableOkButton();

        add(mainPanel);
    }

    /**
     * Check if the dialog was closed with the "OK" button
     *
     * @return true if OK'd
     */
    public boolean isOk() {
        return ok;
    }

    /**
     * Get the property name, or null if cancelled.
     *
     * @return The name or null
     */
    public String getPropertyName() {
        return propertyName;
    }

    /**
     * Get the property value, or null if cancelled.
     *
     * @return The value or null
     */
    public String getPropertyValue() {
        return propertyValue;
    }

    //- PRIVATE

    private JTextField nameTextField;
    private JTextField valueTextField;
    private JButton okButton;
    private JButton cancelButton;
    private JPanel mainPanel;

    private String propertyName;
    private String propertyValue;
    private boolean ok;

    private void set( final String nv, final String vv ) {
        this.propertyName = nv;
        this.propertyValue = vv;
    }

    /**
     * Check if OK button should be enabled or not.
     */
    private void enableOrDisableOkButton() {
        String propName = nameTextField.getText();
        // Not allow empty name:
        boolean okEnabled = (propName != null && propName.trim().length() > 0);
        okButton.setEnabled(okEnabled);
    }
}