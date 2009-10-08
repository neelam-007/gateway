package com.l7tech.external.assertions.jdbcquery.console;

import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;
import com.l7tech.console.util.Pair;

import javax.swing.*;
import java.awt.event.*;
import java.util.ResourceBundle;

public class ContextVariableNamingDialog extends JDialog {
    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.external.assertions.jdbcquery.console.resources.ContextVariableNamingDialog");

    private JPanel contentPane;
    private JButton okButton;
    private JButton cancelButton;
    private JTextField queryResultTextField;
    private JTextField contextVariableTextField;

    private boolean confirmed;
    private Pair<String, String> namePair;

    public ContextVariableNamingDialog(JDialog owner, Pair<String, String> namePair){
        super(owner, resources.getString("dialog.title.context.variable.naming"));
        initialize(namePair);
    }

    private void initialize(Pair<String, String> namePair) {
        this.namePair = namePair;

        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(cancelButton);
        pack();
        Utilities.centerOnScreen(this);
        Utilities.setEscKeyStrokeDisposes(this);

        final RunOnChangeListener changeListener = new RunOnChangeListener(new Runnable() {
            public void run() {
                enableOrDisableOkButton();
            }
        });
        // todo: test field length verification
        queryResultTextField.getDocument().addDocumentListener(changeListener);
        contextVariableTextField.getDocument().addDocumentListener(changeListener);

        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doOk();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doCancel();
            }
        });

        modelToView();
        enableOrDisableOkButton();
    }

    private void modelToView() {
        if (namePair == null || namePair.left == null || namePair.right == null) {
            throw new IllegalStateException("A namePair object must be initialized first.");
        }
        queryResultTextField.setText(namePair.left);
        contextVariableTextField.setText(namePair.right);
    }

    private void viewToModel() {
        if (namePair == null || namePair.left == null || namePair.right == null) {
            throw new IllegalStateException("A namePair object must be initialized first.");
        }
        namePair.left = queryResultTextField.getText();
        namePair.right = contextVariableTextField.getText();
    }

    private void enableOrDisableOkButton() {
        boolean enabled =
            isNonEmptyRequiredTextField(queryResultTextField.getText()) &&
            isNonEmptyRequiredTextField(contextVariableTextField.getText());
        okButton.setEnabled(enabled);
    }

    private boolean isNonEmptyRequiredTextField(String text) {
        return text != null && !text.trim().isEmpty();
    }

    public String getColumnName() {
        return queryResultTextField.getText();
    }

    public String getVariableSuffixName() {
        return contextVariableTextField.getText();
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    private void doOk() {
        confirmed = true;
        viewToModel();
        dispose();
    }

    private void doCancel() {
        dispose();
    }
}