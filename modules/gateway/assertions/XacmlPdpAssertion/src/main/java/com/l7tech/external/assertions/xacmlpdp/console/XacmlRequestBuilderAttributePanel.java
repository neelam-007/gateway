package com.l7tech.external.assertions.xacmlpdp.console;

import com.l7tech.external.assertions.xacmlpdp.XacmlRequestBuilderAssertion;

import javax.swing.*;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * Copyright (C) 2009, Layer 7 Technologies Inc.
 * User: njordan
 * Date: 2-Apr-2009
 * Time: 5:31:35 PM
 * To change this template use File | Settings | File Templates.
 */
public class XacmlRequestBuilderAttributePanel extends JPanel implements XacmlRequestBuilderNodePanel {
    private JTextField idField;
    private JTextField dataTypeField;
    private JTextField issuerField;
    private JLabel issueInstantLabel;
    private JTextField issueInstantField;
    private JPanel mainPanel;

    private XacmlRequestBuilderAssertion.Attribute attribute;
    private XacmlRequestBuilderAssertion.XacmlVersionType xacmlVersion;

    public XacmlRequestBuilderAttributePanel(XacmlRequestBuilderAssertion.Attribute attribute, XacmlRequestBuilderAssertion.XacmlVersionType version) {
        this.attribute = attribute;
        this.xacmlVersion = version;
        idField.setText(attribute.getId());
        dataTypeField.setText(attribute.getDataType());
        issuerField.setText(attribute.getIssuer());

        if(xacmlVersion == XacmlRequestBuilderAssertion.XacmlVersionType.V1_0) {
            issueInstantField.setText(attribute.getIssueInstant());
        }

        init();
    }

    public JPanel getPanel() {
        return mainPanel;
    }

    public void init() {
        idField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent evt) {
                attribute.setId(idField.getText().trim());
            }

            public void insertUpdate(DocumentEvent evt) {
                attribute.setId(idField.getText().trim());
            }

            public void removeUpdate(DocumentEvent evt) {
                attribute.setId(idField.getText().trim());
            }
        });

        dataTypeField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent evt) {
                attribute.setDataType(dataTypeField.getText().trim());
            }

            public void insertUpdate(DocumentEvent evt) {
                attribute.setDataType(dataTypeField.getText().trim());
            }

            public void removeUpdate(DocumentEvent evt) {
                attribute.setDataType(dataTypeField.getText().trim());
            }
        });

        issuerField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent evt) {
                attribute.setIssuer(issuerField.getText().trim());
            }

            public void insertUpdate(DocumentEvent evt) {
                attribute.setIssuer(issuerField.getText().trim());
            }

            public void removeUpdate(DocumentEvent evt) {
                attribute.setIssuer(issuerField.getText().trim());
            }
        });

        if(xacmlVersion != XacmlRequestBuilderAssertion.XacmlVersionType.V1_0) {
            issueInstantLabel.setVisible(false);
            issueInstantField.setVisible(false);
        } else {
            issueInstantField.getDocument().addDocumentListener(new DocumentListener() {
                public void changedUpdate(DocumentEvent evt) {
                    attribute.setIssueInstant(issueInstantField.getText().trim());
                }

                public void insertUpdate(DocumentEvent evt) {
                    attribute.setIssueInstant(issueInstantField.getText().trim());
                }

                public void removeUpdate(DocumentEvent evt) {
                    attribute.setIssueInstant(issueInstantField.getText().trim());
                }
            });
        }
    }

    public boolean handleDispose() {
        return true;
    }
}
