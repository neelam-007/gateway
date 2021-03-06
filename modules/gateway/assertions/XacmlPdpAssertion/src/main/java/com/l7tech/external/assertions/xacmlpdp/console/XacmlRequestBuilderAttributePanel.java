package com.l7tech.external.assertions.xacmlpdp.console;

import com.l7tech.external.assertions.xacmlpdp.XacmlRequestBuilderAssertion;
import com.l7tech.external.assertions.xacmlpdp.XacmlAssertionEnums;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.policy.variable.Syntax;
import com.sun.xacml.attr.DateTimeAttribute;

import javax.swing.*;
import java.util.Set;

/**
 * Copyright (C) 2009, Layer 7 Technologies Inc.
 * @author njordan
 */
public class XacmlRequestBuilderAttributePanel extends JPanel implements XacmlRequestBuilderNodePanel {
    private JTextField issuerField;
    private JLabel issueInstantLabel;
    private JTextField issueInstantField;
    private JPanel mainPanel;
    private JComboBox dataTypeComboBox;
    private JComboBox idComboBox;

    private XacmlRequestBuilderAssertion.Attribute attribute;
    private XacmlAssertionEnums.XacmlVersionType xacmlVersion;

    public XacmlRequestBuilderAttributePanel( final XacmlRequestBuilderAssertion.Attribute attribute,
                                              final XacmlAssertionEnums.XacmlVersionType version,
                                              final Set<String> idOptions ) {
        this.attribute = attribute;
        this.xacmlVersion = version;
        issuerField.setText(attribute.getIssuer());

        if(xacmlVersion != XacmlAssertionEnums.XacmlVersionType.V2_0) {
            issueInstantField.setText(attribute.getIssueInstant());
        }

        init( idOptions );

        idComboBox.setSelectedItem(attribute.getId());
        dataTypeComboBox.setSelectedItem(attribute.getDataType());
    }

    public JPanel getPanel() {
        return mainPanel;
    }

    public void init( final Set<String> idOptions ) {
        idComboBox.setModel( new DefaultComboBoxModel( idOptions.toArray() ) );
        dataTypeComboBox.setModel( new DefaultComboBoxModel( XacmlConstants.XACML_10_DATATYPES.toArray() ) );

        idComboBox.addActionListener(new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                attribute.setId( ((String)idComboBox.getSelectedItem()).trim() );
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

        if(xacmlVersion == XacmlAssertionEnums.XacmlVersionType.V2_0) {
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
    public boolean handleDispose(final XacmlRequestBuilderDialog builderDialog) {
        //return true if the node is being removed
        if(!attribute.shouldValidate()) return true;

        // Access editors directly to get the current text
        attribute.setId(((String)idComboBox.getEditor().getItem()).trim());
        attribute.setDataType(((String)dataTypeComboBox.getEditor().getItem()).trim());

        if (attribute.getId() == null || attribute.getId().isEmpty()) {
            DialogDisplayer.showMessageDialog( this, "AttributeId is required.  Please enter an AttributeId.", "Validation Error", JOptionPane.ERROR_MESSAGE, null );
            idComboBox.grabFocus();
            return false;
        }

        if ( attribute.getDataType()==null ||
             attribute.getDataType().isEmpty() ) {
            DialogDisplayer.showMessageDialog( this, "DataType is required.  Please enter a DataType.", "Validation Error", JOptionPane.ERROR_MESSAGE, null );
            dataTypeComboBox.grabFocus();
            return false;
        }

        // Validate IssueInstant if the Xacml version is pre 2.0.
        if(xacmlVersion != XacmlAssertionEnums.XacmlVersionType.V2_0) {
            String issueInstant = issueInstantField.getText();
            if (issueInstant != null) {
                issueInstant = issueInstant.trim();
                // if is is a blank or consists of context variable(s), then ignore validation.
                if (issueInstant.isEmpty() || Syntax.getReferencedNames(issueInstant, false).length > 0) return true;

                // Check if it is a valid datetime with a format "yyyy-MM-dd'T'HH:mm:ss[Z]"
                try {
                    DateTimeAttribute.getInstance(issueInstant);
                } catch (Exception e) {
                    DialogDisplayer.showMessageDialog(this, "IssueInstant must be either a blank or a valid datetime with a format \"yyyy-MM-dd'T'HH:mm:ss[Z]\".",
                        "Validation Error", JOptionPane.ERROR_MESSAGE, null);
                    return false;
                }
            }
        }

        return true;
    }
}
