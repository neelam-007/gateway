package com.l7tech.console.panels;

import com.l7tech.policy.assertion.CommentAssertion;

import javax.swing.*;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class CommentAssertionDialog extends JDialog {
    private final CommentAssertion assertion;
    private boolean assertionModified;
    private JTextField commentField;
    private JButton okButton;
    private JButton cancelButton;
    private JPanel mainPanel;

    public CommentAssertionDialog(Frame owner, final CommentAssertion assertion)
            throws HeadlessException
    {
        super(owner, "Comment Assertion Properties", true);
        this.assertion = assertion;

        commentField.setText(assertion.getComment());

        okButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                assertionModified = true;
                assertion.setComment(commentField.getText());
                dispose();
            }
        });

        cancelButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                assertionModified = false;
                dispose();
            }
        });

        commentField.getDocument().addDocumentListener(new DocumentListener(){
            public void insertUpdate(DocumentEvent e) { enableButtons(); }
            public void removeUpdate(DocumentEvent e) { enableButtons(); }
            public void changedUpdate(DocumentEvent e) { enableButtons(); }
        });

        enableButtons();

        add(mainPanel);
    }

    private void enableButtons() {
        okButton.setEnabled(commentField.getText() != null && commentField.getText().length() > 0);
    }

    public CommentAssertion getAssertion() {
        return assertion;
    }

    public boolean isAssertionModified() {
        return assertionModified;
    }
}
