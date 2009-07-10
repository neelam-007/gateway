package com.l7tech.external.assertions.xacmlpdp.console;

import com.l7tech.external.assertions.xacmlpdp.XacmlRequestBuilderAssertion;
import com.l7tech.external.assertions.xacmlpdp.XacmlAssertionEnums;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.DialogDisplayer;

import javax.swing.*;

/**
 * Copyright (C) 2009, Layer 7 Technologies Inc.
 * User: njordan
 * Date: 2-Apr-2009
 * Time: 5:31:35 PM
 * To change this template use File | Settings | File Templates.
 */
public class XacmlRequestBuilderAttributePanel extends JPanel implements XacmlRequestBuilderNodePanel {
    private JTextField idField;
    private JTextField issuerField;
    private JLabel issueInstantLabel;
    private JTextField issueInstantField;
    private JPanel mainPanel;
    private JComboBox dataTypeComboBox;

    private XacmlRequestBuilderAssertion.Attribute attribute;
    private XacmlAssertionEnums.XacmlVersionType xacmlVersion;

    public XacmlRequestBuilderAttributePanel(XacmlRequestBuilderAssertion.Attribute attribute, XacmlAssertionEnums.XacmlVersionType version) {
        this.attribute = attribute;
        this.xacmlVersion = version;
        idField.setText(attribute.getId());
        issuerField.setText(attribute.getIssuer());

        if(xacmlVersion == XacmlAssertionEnums.XacmlVersionType.V1_0) {
            issueInstantField.setText(attribute.getIssueInstant());
        }

        init();

        dataTypeComboBox.setSelectedItem(attribute.getDataType());
    }

    public JPanel getPanel() {
        return mainPanel;
    }

    public void init() {
        dataTypeComboBox.setModel( new DefaultComboBoxModel( XacmlConstants.XACML_10_DATATYPES.toArray() ) );

        idField.getDocument().addDocumentListener(new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                attribute.setId(idField.getText().trim());
            }
        }));

        dataTypeComboBox.addActionListener(new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                attribute.setDataType( ((String)dataTypeComboBox.getSelectedItem()).trim() );
            }
        }));

        issuerField.getDocument().addDocumentListener(new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                attribute.setIssuer(issuerField.getText().trim());
            }
        }));

        if(xacmlVersion != XacmlAssertionEnums.XacmlVersionType.V1_0) {
            issueInstantLabel.setVisible(false);
            issueInstantField.setVisible(false);
        } else {
            issueInstantField.getDocument().addDocumentListener(new RunOnChangeListener(new Runnable() {
                @Override
                public void run() {
                    attribute.setIssueInstant(issueInstantField.getText().trim());
                }
            }));
        }
    }

    @Override
    public boolean handleDispose() {
        attribute.setDataType(((String)dataTypeComboBox.getEditor().getItem()).trim()); // Access editor directly to get the current text

        if ( attribute.getDataType()==null ||
             attribute.getDataType().isEmpty() ) {
            DialogDisplayer.showMessageDialog( this, "Data Type is required. Please enter a Data Type.", "Validation Error", JOptionPane.ERROR_MESSAGE, null );
            return false;
        }
        return true;
    }
}
