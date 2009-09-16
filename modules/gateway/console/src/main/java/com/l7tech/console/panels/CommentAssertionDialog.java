package com.l7tech.console.panels;

import com.l7tech.policy.assertion.CommentAssertion;

import javax.swing.*;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class CommentAssertionDialog extends LegacyAssertionPropertyDialog {
    private final CommentAssertion assertion;
    private final boolean readOnly;
    private boolean assertionModified;
    private JTextField commentField;
    private JButton okButton;
    private JButton cancelButton;
    private JPanel mainPanel;

    public CommentAssertionDialog(Frame owner, final CommentAssertion assertion, final boolean readOnly)
            throws HeadlessException
    {
        super(owner, assertion, true);
        this.assertion = assertion;
        this.readOnly = readOnly;

        commentField.setText(assertion.getComment());

        okButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                assertionModified = true;
                assertion.setComment(commentField.getText());
                dispose();
            }
        });

        cancelButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                assertionModified = false;
                dispose();
            }
        });

        commentField.getDocument().addDocumentListener(new DocumentListener(){
            @Override
            public void insertUpdate(DocumentEvent e) { enableButtons(); }
            @Override
            public void removeUpdate(DocumentEvent e) { enableButtons(); }
            @Override
            public void changedUpdate(DocumentEvent e) { enableButtons(); }
        });

        enableButtons();

        add(mainPanel);
    }

    private void enableButtons() {
        okButton.setEnabled(!readOnly && commentField.getText() != null && commentField.getText().length() > 0);
    }

    public CommentAssertion getAssertion() {
        return assertion;
    }

    public boolean isAssertionModified() {
        return assertionModified;
    }
}
