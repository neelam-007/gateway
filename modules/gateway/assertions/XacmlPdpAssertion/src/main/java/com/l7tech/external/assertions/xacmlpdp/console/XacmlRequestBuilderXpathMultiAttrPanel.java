package com.l7tech.external.assertions.xacmlpdp.console;

import com.l7tech.external.assertions.xacmlpdp.XacmlRequestBuilderAssertion;
import com.l7tech.external.assertions.xacmlpdp.XacmlAssertionEnums;
import static com.l7tech.external.assertions.xacmlpdp.XacmlRequestBuilderAssertion.MultipleAttributeConfig.FieldName.*;
import static com.l7tech.external.assertions.xacmlpdp.XacmlRequestBuilderAssertion.MultipleAttributeConfig.FieldType.CONTEXT_VARIABLE;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.util.Functions;
import com.l7tech.util.ValidationUtils;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.xml.xpath.XpathUtil;
import com.l7tech.xml.xpath.XpathVariableFinder;
import com.l7tech.xml.xpath.NoSuchXpathVariableException;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.PolicyVariableUtils;
import com.l7tech.policy.variable.VariableMetadata;
import com.sun.xacml.attr.DateTimeAttribute;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

import org.jaxen.XPathSyntaxException;
import org.jaxen.JaxenException;
import org.w3c.dom.Document;

/**
 * Copyright (C) 2009, Layer 7 Technologies Inc.
 * @author njordan
 */
public class XacmlRequestBuilderXpathMultiAttrPanel extends JPanel implements XacmlRequestBuilderNodePanel {
    private JPanel mainPanel;

    private JComboBox messageSourceComboBox;
    private JTable namespacesTable;
    private JButton addNamespaceButton;
    private JButton modifyNamespaceButton;
    private JButton removeNamespaceButton;
    private JTextField xpathBaseField;

    private JComboBox idComboBox;
    private JComboBox idExpressionType;

    private JComboBox dataTypeComboBox;
    private JComboBox dataTypeExpressionType;

    private JTextField issuerField;
    private JComboBox issuerExpressionType;

    private JPanel issueInstantFieldsPanel;
    private JLabel issueInstantLabel;
    private JTextField issueInstantField;
    private JComboBox issueInstantExpressionType;

    private JTextField valueField;
    private JComboBox valueExpressionType;
    private JCheckBox falsifyPolicyCheckBox;

    private final XacmlAssertionEnums.XacmlVersionType xacmlVersion;

    private final XacmlRequestBuilderAssertion.MultipleAttributeConfig multipleAttributeConfig;
    private final DefaultTableModel tableModel;
    private final JDialog window;
    private final Document testDocument;
    private final XacmlRequestBuilderAssertion assertion;

    public XacmlRequestBuilderXpathMultiAttrPanel( final XacmlRequestBuilderAssertion.MultipleAttributeConfig multipleAttributeConfig,
                                                   final XacmlAssertionEnums.XacmlVersionType version,
                                                   final Set<String> idOptions,
                                                   final JDialog window,
                                                   final XacmlRequestBuilderAssertion assertion)
    {
        this.multipleAttributeConfig = multipleAttributeConfig;
        this.assertion = assertion;
        xacmlVersion = version;

        messageSourceComboBox.setModel(
                new DefaultComboBoxModel(
                        new Object[] {
                                XacmlAssertionEnums.MessageLocation.DEFAULT_REQUEST.getLocationName(),
                                XacmlAssertionEnums.MessageLocation.DEFAULT_RESPONSE.getLocationName()}));

        if(multipleAttributeConfig.getMessageSource() == XacmlAssertionEnums.MessageLocation.DEFAULT_REQUEST) {
            messageSourceComboBox.setSelectedIndex(0);
        } else if(multipleAttributeConfig.getMessageSource() == XacmlAssertionEnums.MessageLocation.DEFAULT_RESPONSE) {
            messageSourceComboBox.setSelectedIndex(1);
        }

        tableModel = new DefaultTableModel(new String[] {"Prefix", "URI"}, 0){
            @Override
            public boolean isCellEditable( int row, int column ) {
                return false;
            }
        };
        if(multipleAttributeConfig.getNamespaces() != null) {
            for(Map.Entry<String, String> entry : multipleAttributeConfig.getNamespaces().entrySet()) {
                tableModel.addRow(new String[] {entry.getKey(), entry.getValue()});
            }
        }
        namespacesTable.setModel(tableModel);
        namespacesTable.getTableHeader().setReorderingAllowed( false );

        xpathBaseField.setText(multipleAttributeConfig.getXpathBase());
        issuerField.setText(multipleAttributeConfig.getField(ISSUER).getValue());

        if(version != XacmlAssertionEnums.XacmlVersionType.V2_0) {
            issueInstantField.setText(multipleAttributeConfig.getField(ISSUE_INSTANT).getValue());
        }

        valueField.setText(multipleAttributeConfig.getField(VALUE).getValue());

        this.window = window;

        init(idOptions );

        idComboBox.setSelectedItem(multipleAttributeConfig.getField(ID).getValue());
        dataTypeComboBox.setSelectedItem(multipleAttributeConfig.getField(DATA_TYPE).getValue());

        idExpressionType.setSelectedItem(multipleAttributeConfig.getField(ID).getType());
        dataTypeExpressionType.setSelectedItem(multipleAttributeConfig.getField(DATA_TYPE).getType());
        issuerExpressionType.setSelectedItem(multipleAttributeConfig.getField(ISSUER).getType());
        issueInstantExpressionType.setSelectedItem(multipleAttributeConfig.getField(ISSUE_INSTANT).getType());
        valueExpressionType.setSelectedItem(multipleAttributeConfig.getField(VALUE).getType());

        testDocument = XmlUtil.stringAsDocument("<blah xmlns=\"http://bzzt.com\"/>");
    }

    public JPanel getPanel() {
        return mainPanel;
    }

    public void init(final Set<String> idOptions ) {
        idComboBox.setModel( new DefaultComboBoxModel( idOptions.toArray() ) );
        dataTypeComboBox.setModel( new DefaultComboBoxModel( XacmlConstants.XACML_10_DATATYPES.toArray() ) );

        idExpressionType.setModel( new DefaultComboBoxModel( EnumSet.allOf(XacmlRequestBuilderAssertion.MultipleAttributeConfig.FieldType.class).toArray()) );
        dataTypeExpressionType.setModel( new DefaultComboBoxModel( EnumSet.allOf(XacmlRequestBuilderAssertion.MultipleAttributeConfig.FieldType.class).toArray()) );
        issuerExpressionType.setModel( new DefaultComboBoxModel( EnumSet.allOf(XacmlRequestBuilderAssertion.MultipleAttributeConfig.FieldType.class).toArray()) );
        issueInstantExpressionType.setModel( new DefaultComboBoxModel( EnumSet.allOf(XacmlRequestBuilderAssertion.MultipleAttributeConfig.FieldType.class).toArray()) );
        valueExpressionType.setModel( new DefaultComboBoxModel( EnumSet.allOf(XacmlRequestBuilderAssertion.MultipleAttributeConfig.FieldType.class).toArray()) );

        namespacesTable.getSelectionModel().addListSelectionListener( new ListSelectionListener(){
            @Override
            public void valueChanged( ListSelectionEvent e) {
                enableOrDisableButtons();
            }
        } );

        messageSourceComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                String selectedItem = messageSourceComboBox.getSelectedItem().toString();
                if(XacmlAssertionEnums.MessageLocation.DEFAULT_REQUEST.getLocationName().equals(selectedItem)) {
                    multipleAttributeConfig.setMessageSource(XacmlAssertionEnums.MessageLocation.DEFAULT_REQUEST);
                } else if(XacmlAssertionEnums.MessageLocation.DEFAULT_RESPONSE.getLocationName().equals(selectedItem)) {
                    multipleAttributeConfig.setMessageSource(XacmlAssertionEnums.MessageLocation.DEFAULT_RESPONSE);
                }
            }
        });

        addNamespaceButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                boolean done = false;
                String prefix = "";
                String namespace = "";
                while ( !done ) {
                    XacmlRequestBuilderNamespaceDialog dialog = new XacmlRequestBuilderNamespaceDialog(window, prefix, namespace);
                    Utilities.centerOnParentWindow(dialog);
                    dialog.setVisible(true);

                    if( dialog.isConfirmed() ) {
                        prefix = dialog.getPrefix();
                        namespace = dialog.getUri();

                        if ( validateNamespacePrefix( null, prefix ) &&
                             validateNamespace( null, namespace ) ) {
                            done = true;
                            tableModel.addRow(new String[] {prefix, namespace});
                            if(multipleAttributeConfig.getNamespaces() == null) {
                                multipleAttributeConfig.setNamespaces(new HashMap<String, String>(1));
                            }
                            multipleAttributeConfig.getNamespaces().put(prefix, namespace);
                        }
                    } else {
                        done = true;
                    }
                }
            }
        });

        modifyNamespaceButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                if(namespacesTable.getSelectedRow() == -1) {
                    return;
                }

                final String originalPrefix = (String)namespacesTable.getValueAt(namespacesTable.getSelectedRow(), 0);
                String prefix = originalPrefix;
                final String originalNamespace = (String)namespacesTable.getValueAt(namespacesTable.getSelectedRow(), 1);
                String namespace = originalNamespace;
                boolean done = false;
                while ( !done ) {
                    XacmlRequestBuilderNamespaceDialog dialog = new XacmlRequestBuilderNamespaceDialog(window, prefix, namespace);
                    Utilities.centerOnParentWindow(dialog);
                    dialog.setVisible(true);

                    if( dialog.isConfirmed() ) {
                        prefix = dialog.getPrefix();
                        namespace = dialog.getUri();

                        if ( validateNamespacePrefix( originalPrefix, prefix ) &&
                             validateNamespace( originalNamespace, namespace ) ) {
                            done = true;
                            tableModel.setValueAt(prefix, namespacesTable.getSelectedRow(), 0);
                            tableModel.setValueAt(namespace, namespacesTable.getSelectedRow(), 1);

                            multipleAttributeConfig.getNamespaces().remove(originalPrefix);
                            multipleAttributeConfig.getNamespaces().put(prefix, namespace);
                        }
                    } else {
                        done = true;
                    }
                }
            }
        });

        removeNamespaceButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                if(namespacesTable.getSelectedRow() == -1) {
                    return;
                }

                String ns = namespacesTable.getValueAt(namespacesTable.getSelectedRow(), 0).toString();
                multipleAttributeConfig.getNamespaces().remove(ns);
                tableModel.removeRow(namespacesTable.getSelectedRow());
            }
        });

        xpathBaseField.getDocument().addDocumentListener(new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                multipleAttributeConfig.setXpathBase(xpathBaseField.getText().trim());
            }
        }));

        addValueChangeListener(idComboBox, ID);
        addTypeChangeListener(idExpressionType, ID, idComboBox);
        addValueChangeListener(dataTypeComboBox, DATA_TYPE);
        addTypeChangeListener(dataTypeExpressionType, DATA_TYPE, dataTypeComboBox);
        addValueChangeListener(issuerField, ISSUER);
        addTypeChangeListener(issuerExpressionType, ISSUER, issuerField);
        addValueChangeListener(valueField, VALUE);
        addTypeChangeListener(valueExpressionType, VALUE, valueField);

        if(xacmlVersion == XacmlAssertionEnums.XacmlVersionType.V2_0) {
            issueInstantLabel.setVisible(false);
            issueInstantFieldsPanel.setVisible(false);
        } else {
            addValueChangeListener(issueInstantField, ISSUE_INSTANT);
            addTypeChangeListener(issueInstantExpressionType, ISSUE_INSTANT, issueInstantField);
        }

        falsifyPolicyCheckBox.setSelected(multipleAttributeConfig.isFalsifyPolicyEnabled());

        Utilities.setDoubleClickAction( namespacesTable, modifyNamespaceButton );
        enableOrDisableButtons();
    }

    private void addValueChangeListener(final JTextField valueTextField, final XacmlRequestBuilderAssertion.MultipleAttributeConfig.FieldName fieldName) {
        valueTextField.getDocument().addDocumentListener(new RunOnChangeListener(new Runnable() {
                @Override
                public void run() {
                    multipleAttributeConfig.getField(fieldName).setValue(valueTextField.getText().trim());
                }
            }));
    }

    private void addValueChangeListener(final JComboBox valuesComboBox, final XacmlRequestBuilderAssertion.MultipleAttributeConfig.FieldName fieldName) {
        valuesComboBox.addActionListener(new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                multipleAttributeConfig.getField(fieldName).setValue(((String)valuesComboBox.getSelectedItem()).trim());
            }
        }));
    }

    private void addTypeChangeListener(final JComboBox typesComboBox, final XacmlRequestBuilderAssertion.MultipleAttributeConfig.FieldName fieldName, final JComponent valueComponent) {
        typesComboBox.addActionListener(new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                XacmlRequestBuilderAssertion.MultipleAttributeConfig.FieldType previousType = multipleAttributeConfig.getField(fieldName).getType();
                XacmlRequestBuilderAssertion.MultipleAttributeConfig.FieldType newType = (XacmlRequestBuilderAssertion.MultipleAttributeConfig.FieldType) typesComboBox.getSelectedItem();

                // Update the field type
                multipleAttributeConfig.getField(fieldName).setType(newType);

                // The below section is implemented for fixing the bug 7682.
                // To resolve 7682, we have to resolve a special case - when user changes the field value, then change the type
                // from Context Variable to one of other types, the field value will be automatically wrapped by a context variable
                // wrapper "${...}" in multipleAttributeConfig, which will cause validation error for "Absolute XPath" type later.
                // Thus, we need to remove the wrapper in multipleAttributeConfig before the validation, when user changes the type
                // from Context Variable to other type.
                if (previousType == CONTEXT_VARIABLE && previousType != newType) {
                    String textInValueComponent;
                    if (valueComponent instanceof JTextField) {
                        textInValueComponent = ((JTextField)valueComponent).getText();
                    } else if (valueComponent instanceof JComboBox) {
                        textInValueComponent = ((String) ((JComboBox)valueComponent).getSelectedItem()).trim();
                    } else {
                        throw new RuntimeException("Invalid component to accept field value.");
                    }

                    boolean hasContextVariableWrapperInValueComponent = Syntax.getReferencedNames(textInValueComponent).length > 0;
                    if (! hasContextVariableWrapperInValueComponent) {
                        // Althought this text doesn't have the "${}" wrapper, the value in multipleAttributeConfig does
                        // have the wrapper, so we need to remove the wrapper before updating the value.
                        String[] contextVariables = Syntax.getReferencedNames(multipleAttributeConfig.getField(fieldName).getValue());
                        if (contextVariables.length > 1) {
                            throw new IllegalArgumentException("AttributeValue must be a reference to exactly one context variable.");
                        } else if (contextVariables.length == 1) {
                            // Update the field value without the "${}" wrapper (Note: the wrapper has been removed in the above step.)
                            multipleAttributeConfig.getField(fieldName).setValue(contextVariables[0]);
                        } // Otherwise, don't update anything.
                    }
                }
            }
        }));
    }


    @Override
    public boolean handleDispose(final XacmlRequestBuilderDialog builderDialog) {
        //return true if the node is being removed
        if(!multipleAttributeConfig.shouldValidate()) return true;

        multipleAttributeConfig.setFalsifyPolicyEnabled(falsifyPolicyCheckBox.isSelected());

        for (XacmlRequestBuilderAssertion.MultipleAttributeConfig.Field field : multipleAttributeConfig.getFields().values()) {
            if (CONTEXT_VARIABLE == field.getType()) {
                String[] varStrings = Syntax.getReferencedNames(field.getValue());
                if (varStrings.length != 1 || ! field.getValue().equals("${" + varStrings[0] + "}")) {
                    //if more than one variable is returned, or the variable is indexed which was removed by
                    //Syntax.getReferencedNames
                    DialogDisplayer.showMessageDialog(this, "Field '" + field.getName() + "' must be a reference to exactly one context variable.", "Validation Error", JOptionPane.ERROR_MESSAGE, null);
                    return false;
                }
            }
        }

        Set<XacmlRequestBuilderAssertion.MultipleAttributeConfig.FieldName> relativeXpaths = multipleAttributeConfig.getRelativeXPathFieldNames();
        if( ! relativeXpaths.isEmpty() && xpathBaseField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Attribute(s) " +
                Functions.map(relativeXpaths, new Functions.Unary<String, XacmlRequestBuilderAssertion.MultipleAttributeConfig.FieldName>()
                {
                    @Override
                    public String call(XacmlRequestBuilderAssertion.MultipleAttributeConfig.FieldName fieldName) {
                        return fieldName.toString();
                    }
                }) +
                    " declared as relative XPaths, XPath Base is needed.", "Attribute Validation Error", JOptionPane.ERROR_MESSAGE);
            xpathBaseField.grabFocus();
            return false;
        }
        if ( multipleAttributeConfig.getField(ID).getValue()==null ||
             multipleAttributeConfig.getField(ID).getValue().isEmpty() ) {
            DialogDisplayer.showMessageDialog( this, "AttributeId is required.  Please enter an AttributeId.", "Validation Error", JOptionPane.ERROR_MESSAGE, null );
            return false;
        }
        if ( multipleAttributeConfig.getField(DATA_TYPE).getValue()==null ||
             multipleAttributeConfig.getField(DATA_TYPE).getValue().isEmpty() ) {
            DialogDisplayer.showMessageDialog( this, "DataType is required.  Please enter a DataType.", "Validation Error", JOptionPane.ERROR_MESSAGE, null );
            return false;
        }

        final Map<String,String> namespaces = multipleAttributeConfig.getNamespaces();
        if ( !relativeXpaths.isEmpty() ) {
            String baseErrorMessage = validateXPath( xpathBaseField.getText().trim(), namespaces, builderDialog );
            if ( baseErrorMessage != null ) {
                DialogDisplayer.showMessageDialog( this, "Invalid \"XPath Base\" : " + baseErrorMessage, "Validation Error", JOptionPane.ERROR_MESSAGE, null );
                return false;
            }
            for ( XacmlRequestBuilderAssertion.MultipleAttributeConfig.FieldName field : relativeXpaths ) {
                String xpath = multipleAttributeConfig.getField(field).getValue();
                String xpathErrorMessage = validateXPath( xpath, namespaces, builderDialog);
                if ( xpathErrorMessage != null ) {
                    DialogDisplayer.showMessageDialog( this, "Invalid XPath for \""+field+"\" : " + xpathErrorMessage, "Validation Error", JOptionPane.ERROR_MESSAGE, null );
                    return false;
                }
            }
        }
        Set<XacmlRequestBuilderAssertion.MultipleAttributeConfig.FieldName> absoluteXpaths = multipleAttributeConfig.getAbsoluteXPathFieldNames();
        if ( !absoluteXpaths.isEmpty() ) {
            for ( XacmlRequestBuilderAssertion.MultipleAttributeConfig.FieldName field : absoluteXpaths ) {
                String xpath = multipleAttributeConfig.getField(field).getValue();
                String xpathErrorMessage = validateXPath( xpath, namespaces, builderDialog );
                if ( xpathErrorMessage != null ) {
                    DialogDisplayer.showMessageDialog( this, "Invalid XPath for \""+field+"\" : " + xpathErrorMessage, "Validation Error", JOptionPane.ERROR_MESSAGE, null );
                    return false;
                }
            }
        }

        // Validate IssueInstant if the Xacml version is pre 2.0 and the option of IssueInstant is Regular.
        if(xacmlVersion != XacmlAssertionEnums.XacmlVersionType.V2_0) {
            XacmlRequestBuilderAssertion.MultipleAttributeConfig.FieldType issueInstantType = multipleAttributeConfig.getField(ISSUE_INSTANT).getType();
            // Ignore the validation if IssueInstant is specified by Context Variable or XPath.
            if (issueInstantType == XacmlRequestBuilderAssertion.MultipleAttributeConfig.FieldType.REGULAR) {
                String issueInstant = issueInstantField.getText();
                if (issueInstant != null) {
                    issueInstant = issueInstant.trim();
                    // if is is a blank or consists of context variable(s), then ignore validation.
                    if (issueInstant.isEmpty() || Syntax.getReferencedNames(issueInstant).length > 0) return true;

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
        }

        return true;
    }

    private void enableOrDisableButtons() {
        boolean enable = namespacesTable.getSelectedRow() > -1;
        modifyNamespaceButton.setEnabled( enable );
        removeNamespaceButton.setEnabled( enable );
    }

    private boolean validateNamespacePrefix( final String originalPrefix, final String prefix ) {
        boolean valid = false;

        if ( (originalPrefix==null || !originalPrefix.equals( prefix )) && isDuplicateNamespacePrefix( prefix )) {
            JOptionPane.showMessageDialog(window,
                "The namespace prefix '" + prefix + "' already exists.  Please use a new namespace prefix and try again.",
                "Duplicate Namespace Prefix", JOptionPane.ERROR_MESSAGE, null );
        } else if ( !ValidationUtils.isProbablyValidXmlNamespacePrefix( prefix )) {
            JOptionPane.showMessageDialog(window,
                "The namespace prefix '" + prefix + "' is not valid.  Please modify the namespace prefix and try again.",
                "Invalid Namespace Prefix", JOptionPane.ERROR_MESSAGE, null );
        } else {
            valid = true;
        }

        return valid;
    }

    private boolean validateNamespace( final String originalNamespace, final String namespace ) {
        boolean valid = false;

        if ( (originalNamespace==null || !originalNamespace.equals( namespace )) && isDuplicateNamespace( namespace )) {
            JOptionPane.showMessageDialog(window,
                "The namespace '" + namespace + "' is already specified.\n Please check the namespace and try again.",
                "Duplicate Namespace", JOptionPane.ERROR_MESSAGE, null );
        } else {
            valid = true;
        }

        return valid;
    }

    private String validateXPath( final String xpath,
                                  final Map<String,String> namespaces,
                                  final XacmlRequestBuilderDialog builderDialog) {
        String error = null;
        try {
            final Map<String, VariableMetadata> predecessorVariables = assertion.getParent() != null ?
                    PolicyVariableUtils.getVariablesSetByPredecessors( assertion ) :
                    PolicyVariableUtils.getVariablesSetByPredecessorsAndSelf( builderDialog.getPreviousAssertion() );

            XpathUtil.compileAndEvaluate(testDocument, xpath, namespaces, buildXpathVariableFinder(predecessorVariables.keySet()));
        } catch ( XPathSyntaxException e) {
            return ExceptionUtils.getMessage( e );
        } catch ( JaxenException e) {
            return ExceptionUtils.getMessage( e );
        } catch (RuntimeException e) { // sometimes NPE, sometimes NFE
            return "XPath expression error '" + xpath + "'";
        }
        return error;
    }

    /**
     * The only purpose of this method is to validate that any referenced variables from a relative xpath refer
     * to variables defined in the policy so far. The actual value returned is not important. The server side
     * will be required to return the correct value of the requested variable
     * @param variables the variable to validate has been defined somewhere previously in this policy
     * @return
     */
    private XpathVariableFinder buildXpathVariableFinder( final Set<String> variables ) {
        return new XpathVariableFinder(){
            @Override
            public Object getVariableValue( final String namespaceUri,
                                            final String variableName ) throws NoSuchXpathVariableException {
                if ( namespaceUri != null )
                    throw new NoSuchXpathVariableException("Unsupported XPath variable namespace '"+namespaceUri+"'.");

                if ( !variables.contains(variableName) )
                    throw new NoSuchXpathVariableException("Unsupported XPath variable name '"+variableName+"'.");

                return "";
            }
        };
    }

    private boolean isDuplicateNamespacePrefix( final String prefix ) {
        return multipleAttributeConfig.getNamespaces().containsKey(prefix);
    }

    private boolean isDuplicateNamespace( final String namespace ) {
        return multipleAttributeConfig.getNamespaces().containsValue(namespace);
    }
}
