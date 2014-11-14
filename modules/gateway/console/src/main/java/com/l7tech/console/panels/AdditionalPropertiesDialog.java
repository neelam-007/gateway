package com.l7tech.console.panels;

import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.util.MutablePair;

import javax.swing.*;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.ResourceBundle;

/**
 * To setup additional properties of JDBC Connection, C3P0 Pool Configuration, or Cassandra Connection.
 *
 * @author ghuang
 */
public class AdditionalPropertiesDialog extends JDialog {
    private static final String C3P0_PROPERTY_PREFIX = "c3p0.";
    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.panels.resources.AdditionalPropertiesDialog");

    private JPanel mainPanel;
    private JButton okButton;
    private JButton cancelButton;
    private JTextField propNameTextField;
    private JTextField propValueTextField;
    private JCheckBox setC3p0PoolingCheckBox;

    private MutablePair<String, String> property;
    private boolean confirmed;
    private boolean disableC3P0CheckBox = false;

    public AdditionalPropertiesDialog(Dialog owner, MutablePair<String, String> property) {
        super(owner, resources.getString("dialog.title.additional.props"));
        initialize(property);
    }

    public AdditionalPropertiesDialog(Dialog owner, MutablePair<String, String> property, boolean disableC3P0CheckBox) {
        super(owner, resources.getString("dialog.title.additional.props"));
        // Hide C3P0 check box
        this.disableC3P0CheckBox = disableC3P0CheckBox;
        if (disableC3P0CheckBox) {
            setC3p0PoolingCheckBox.setSelected(false);
            setC3p0PoolingCheckBox.setVisible(false);
        }
        initialize(property);
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    private void initialize(MutablePair<String, String> property) {
        this.property = property;

        setContentPane(mainPanel);
        setModal(true);
        getRootPane().setDefaultButton(okButton);
        Utilities.setEscKeyStrokeDisposes(this);

        DocumentListener docListener = new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                enableOrDisableOkButton();
            }
        });
        propNameTextField.getDocument().addDocumentListener(docListener);
        propValueTextField.getDocument().addDocumentListener(docListener);

        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                confirmed = true;
                viewToModel();
                dispose();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        modelToView();
        enableOrDisableOkButton();
    }

    private void modelToView() {
        if (property == null || property.left == null || property.right == null) {
            throw new IllegalStateException("An additional property object must be initialized first.");
        }
        if (property.left.startsWith(C3P0_PROPERTY_PREFIX)) {
            propNameTextField.setText(property.left.substring(C3P0_PROPERTY_PREFIX.length()));
            if (!disableC3P0CheckBox) {
                setC3p0PoolingCheckBox.setSelected(true);
            }
        } else {
            propNameTextField.setText(property.left);
            if (!disableC3P0CheckBox) {
                setC3p0PoolingCheckBox.setSelected(false);
            }
        }
        propValueTextField.setText(property.right);
    }

    private void viewToModel() {
        if (property == null || property.left == null || property.right == null) {
            throw new IllegalStateException("An additional property object must be initialized first.");
        }
        property.left = (setC3p0PoolingCheckBox.isSelected()? C3P0_PROPERTY_PREFIX : "") + propNameTextField.getText();
        property.right = propValueTextField.getText();
    }

    private void enableOrDisableOkButton() {
        boolean enabled =
            isNonEmptyRequiredTextField(propNameTextField.getText()) &&
            isNonEmptyRequiredTextField(propValueTextField.getText());
        okButton.setEnabled(enabled);
    }

    private boolean isNonEmptyRequiredTextField(String text) {
        return text != null && !text.trim().isEmpty();
    }
}
