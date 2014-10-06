package com.l7tech.console.panels;

import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;
import com.l7tech.util.MutablePair;

import javax.swing.*;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ResourceBundle;

/**
 * User: nilic
 * Date: 7/23/13
 * Time: 2:38 PM
 */
public class ClusterSettingsDialog extends JDialog {
    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.panels.resources.SiteMinderConfigClusterSettings");

    private JPanel mainPanel;
    private JButton okButton;
    private JButton cancelButton;
    private JTextField propNameTextField;
    private JTextField propValueTextField;

    private MutablePair<String, String> property;
    private boolean confirmed;

    public ClusterSettingsDialog(Dialog owner, MutablePair<String, String> property) {
        super(owner, resources.getString("dialog.title.additional.props"));
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
            propNameTextField.setText(property.left);
            propValueTextField.setText(property.right);
    }

    private void viewToModel() {
        if (property == null || property.left == null || property.right == null) {
            throw new IllegalStateException("An additional property object must be initialized first.");
        }
        property.left = propNameTextField.getText();
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
