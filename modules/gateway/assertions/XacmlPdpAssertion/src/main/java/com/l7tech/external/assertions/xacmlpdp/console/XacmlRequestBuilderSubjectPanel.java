package com.l7tech.external.assertions.xacmlpdp.console;

import com.l7tech.external.assertions.xacmlpdp.XacmlRequestBuilderAssertion;

import javax.swing.*;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;

/**
 * Copyright (C) 2009, Layer 7 Technologies Inc.
 * User: njordan
 * Date: 2-Apr-2009
 * Time: 5:01:11 PM
 * To change this template use File | Settings | File Templates.
 */
public class XacmlRequestBuilderSubjectPanel extends JPanel implements XacmlRequestBuilderNodePanel {
    private JTextField subjectCategoryField;
    private JPanel mainPanel;

    private XacmlRequestBuilderAssertion.Subject subject;

    public XacmlRequestBuilderSubjectPanel(XacmlRequestBuilderAssertion.Subject subject) {
        this.subject = subject;
        subjectCategoryField.setText(subject.getSubjectCategory());
        init();
    }

    public JPanel getPanel() {
        return mainPanel;
    }

    public void init() {
        subjectCategoryField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent evt) {
                subject.setSubjectCategory(subjectCategoryField.getText().trim());
            }

            public void insertUpdate(DocumentEvent evt) {
                subject.setSubjectCategory(subjectCategoryField.getText().trim());
            }

            public void removeUpdate(DocumentEvent evt) {
                subject.setSubjectCategory(subjectCategoryField.getText().trim());
            }
        });
    }

    public boolean handleDispose() {
        return true;
    }
}
