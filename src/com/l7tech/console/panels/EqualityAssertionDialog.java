package com.l7tech.console.panels;

import com.l7tech.policy.assertion.EqualityAssertion;

import javax.swing.*;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * GUI for {@link com.l7tech.policy.assertion.EqualityAssertion}
 */
public class EqualityAssertionDialog extends JDialog {
    private JButton cancelButton;
    private JButton okButton;
    private JTextField expr1Field;
    private JTextField expr2Field;
    private JPanel mainPanel;
    private boolean assertionModified;

    public EqualityAssertionDialog(Frame owner, final EqualityAssertion assertion) throws HeadlessException {
        super(owner, "Equality", true);

        add(mainPanel);

        expr1Field.setText(assertion.getExpression1());
        expr2Field.setText(assertion.getExpression2());

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
                assertion.setExpression1(expr1Field.getText());
                assertion.setExpression2(expr2Field.getText());
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
        okButton.setEnabled(expr1Field.getText().length() > 0 && expr2Field.getText().length() > 0);
    }

    public boolean isAssertionModified() {
        return assertionModified;
    }
}
