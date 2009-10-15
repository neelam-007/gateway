package com.l7tech.external.assertions.jdbcquery.console;

import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.console.util.MutablePair;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.external.assertions.jdbcquery.JdbcQueryAssertion;

import javax.swing.*;
import java.awt.event.*;
import java.util.ResourceBundle;
import java.text.MessageFormat;

public class ContextVariableNamingDialog extends JDialog {
    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.external.assertions.jdbcquery.console.resources.ContextVariableNamingDialog");

    private JPanel contentPane;
    private JButton okButton;
    private JButton cancelButton;
    private JTextField columnLabelTextField;
    private JTextField variableNameTextField;

    private boolean confirmed;
    private MutablePair<String, String> namePair;

    public ContextVariableNamingDialog(JDialog owner, MutablePair<String, String> namePair){
        super(owner, resources.getString("dialog.title.context.variable.naming"));
        initialize(namePair);
    }

    private void initialize(MutablePair<String, String> namePair) {
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
        columnLabelTextField.getDocument().addDocumentListener(changeListener);
        variableNameTextField.getDocument().addDocumentListener(changeListener);

        final InputValidator inputValidator = new InputValidator(this, resources.getString("dialog.title.context.variable.naming"));
        inputValidator.constrainTextField(columnLabelTextField, new NonContextVariableTextFieldValidationRule(columnLabelTextField, resources.getString("text.column.label")));
        inputValidator.constrainTextField(variableNameTextField, new NonContextVariableTextFieldValidationRule(variableNameTextField, resources.getString("text.variable.name")));

        inputValidator.attachToButton(okButton, new ActionListener() {
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
        columnLabelTextField.setText(namePair.left);
        variableNameTextField.setText(namePair.right);
    }

    private void viewToModel() {
        if (namePair == null || namePair.left == null || namePair.right == null) {
            throw new IllegalStateException("A namePair object must be initialized first.");
        }
        namePair.left = columnLabelTextField.getText();
        namePair.right = variableNameTextField.getText();
    }

    private void enableOrDisableOkButton() {
        boolean enabled =
            isNonEmptyRequiredTextField(columnLabelTextField.getText()) &&
            isNonEmptyRequiredTextField(variableNameTextField.getText());
        okButton.setEnabled(enabled);
    }

    private boolean isNonEmptyRequiredTextField(String text) {
        return text != null && !text.trim().isEmpty();
    }

    public String getColumnName() {
        return columnLabelTextField.getText();
    }

    public String getVariableSuffixName() {
        return variableNameTextField.getText();
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

    private class NonContextVariableTextFieldValidationRule extends InputValidator.ComponentValidationRule {
        private String textFieldLabel;

        public NonContextVariableTextFieldValidationRule(JTextField textField, String textFieldLabel) {
            super(textField);
            this.textFieldLabel = textFieldLabel;
        }

        @Override
        public String getValidationError() {
            JTextField textField = (JTextField)component;

            if ( Syntax.getReferencedNames(textField.getText()).length > 0) {
                return MessageFormat.format(resources.getString("validation.message.cannot.use.context.var"), textFieldLabel);
            } else if (textFieldLabel.equals(resources.getString("text.variable.name"))) {
                String text = textField.getText();
                if (text != null && text.equals(JdbcQueryAssertion.VARIABLE_COUNT)) {
                    return MessageFormat.format(resources.getString("validation.message.queryresult.count.reserved"), JdbcQueryAssertion.VARIABLE_COUNT);
                }
            }
            return null;
        }
    }
}