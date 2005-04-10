package com.l7tech.console.panels;

import com.l7tech.policy.assertion.HttpFormPost;

import javax.swing.*;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * Used by {@link HttpFormPostDialog} to edit
 * {@link com.l7tech.policy.assertion.HttpFormPost.FieldInfo} objects
 */
public class HttpPostFormFieldInfoDialog extends JDialog {
    private JPanel mainPanel;
    private JButton cancelButton;
    private JButton okButton;
    private JTextField contentTypeField;
    private JTextField fieldNameField;

    private final HttpFormPost.FieldInfo fieldInfo;
    private boolean changed;

    public HttpPostFormFieldInfoDialog(Frame owner, HttpFormPost.FieldInfo fi) throws HeadlessException {
        super(owner, "Edit Field Info", true);
        this.fieldInfo = fi;
        fieldNameField.setText(fi.getFieldname());
        contentTypeField.setText(fi.getContentType());
        enableButtons();

        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                fieldInfo.setFieldname(fieldNameField.getText());
                fieldInfo.setContentType(contentTypeField.getText());
                changed = true;
                dispose();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                changed = false;
                dispose();
            }
        });

        DocumentListener docListener = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                enableButtons();
            }

            public void removeUpdate(DocumentEvent e) {
                enableButtons();
            }

            public void changedUpdate(DocumentEvent e) {
                enableButtons();
            }
        };
        fieldNameField.getDocument().addDocumentListener(docListener);
        contentTypeField.getDocument().addDocumentListener(docListener);

        add(mainPanel);
    }

    private void enableButtons() {
        boolean ok = fieldNameField.getText() != null && fieldNameField.getText().length() > 0;
        ok = ok && contentTypeField.getText() != null && contentTypeField.getText().length() > 0;
        okButton.setEnabled(ok);
    }

    public HttpFormPost.FieldInfo getFieldInfo() {
        return fieldInfo;
    }

    public boolean isChanged() {
        return changed;
    }
}
