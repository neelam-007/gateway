package com.l7tech.console.panels;

import com.l7tech.policy.assertion.ComparisonAssertion;

import javax.swing.*;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * GUI for {@link com.l7tech.policy.assertion.ComparisonAssertion}
 */
public class ComparisonAssertionDialog extends JDialog {
    private JButton cancelButton;
    private JButton okButton;
    private JTextField expr1Field;
    private JTextField expr2Field;
    private JPanel mainPanel;
    private JComboBox operatorCombo;
    private JCheckBox negateCheckbox;
    private JCheckBox caseCheckbox;

    private boolean assertionModified;
    private JLabel verbLabel;

    public ComparisonAssertionDialog(Frame owner, final ComparisonAssertion assertion) throws HeadlessException {
        super(owner, "Equality", true);

        add(mainPanel);

        operatorCombo.setModel(new DefaultComboBoxModel(ComparisonAssertion.Operator.getValues()));
        operatorCombo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ComparisonAssertion.Operator op = (ComparisonAssertion.Operator)operatorCombo.getSelectedItem();
                if (op.isUnary()) {
                    expr2Field.setEnabled(false);
                } else {
                    expr2Field.setEnabled(true);
                }
                enableButtons();
            }
        });

        operatorCombo.setSelectedItem(assertion.getOperator());
        expr1Field.setText(assertion.getExpression1());
        negateCheckbox.setSelected(assertion.isNegate());
        expr2Field.setText(assertion.getExpression2());
        caseCheckbox.setSelected(assertion.isCaseSensitive());

        enableButtons();

        DocumentListener dl = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { enableButtons(); }
            public void removeUpdate(DocumentEvent e) { enableButtons(); }
            public void changedUpdate(DocumentEvent e) { enableButtons(); }
        };

        expr1Field.getDocument().addDocumentListener(dl);
        expr2Field.getDocument().addDocumentListener(dl);

        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                assertion.setNegate(negateCheckbox.isSelected());
                assertion.setExpression1(expr1Field.getText());
                ComparisonAssertion.Operator op = (ComparisonAssertion.Operator) operatorCombo.getSelectedItem();
                assertion.setOperator(op);
                if (op.isUnary()) {
                    assertion.setExpression2(null);
                } else {
                    assertion.setExpression2(expr2Field.getText());
                }
                assertion.setCaseSensitive(caseCheckbox.isSelected());
                assertionModified = true;
                dispose();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                assertionModified = false;
                dispose();
            }
        });
    }

    private void enableButtons() {
        okButton.setEnabled(
            expr1Field.getText().length() > 0 &&
            (((ComparisonAssertion.Operator)operatorCombo.getSelectedItem()).isUnary() || expr2Field.getText().length() > 0)
        );
    }

    public boolean isAssertionModified() {
        return assertionModified;
    }
}
