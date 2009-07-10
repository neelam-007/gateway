package com.l7tech.external.assertions.xacmlpdp.console;

import com.l7tech.external.assertions.xacmlpdp.XacmlRequestBuilderAssertion;
import com.l7tech.external.assertions.xacmlpdp.XacmlAssertionEnums;
import static com.l7tech.external.assertions.xacmlpdp.XacmlRequestBuilderAssertion.MultipleAttributeConfig.FieldName.*;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.util.Functions;
import com.l7tech.util.ValidationUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

/**
 * Copyright (C) 2009, Layer 7 Technologies Inc.
 * User: njordan
 * Date: 2-Apr-2009
 * Time: 8:36:28 PM
 * To change this template use File | Settings | File Templates.
 */
public class XacmlRequestBuilderXpathMultiAttrPanel extends JPanel implements XacmlRequestBuilderNodePanel {
    private JComboBox messageSourceComboBox;
    private JTextField xpathBaseField;
    private JButton idOptionsButton;
    private JButton dataTypeOptionsButton;
    private JTextField issuerField;
    private JButton issuerOptionsButton;
    private JLabel issueInstantLabel;
    private JTextField issueInstantField;
    private JButton issueInstantOptionsButton;
    private JPanel issueInstantFieldsPanel;
    private JTextField valueField;
    private JButton valueOptionsButton;
    private JTable namespacesTable;
    private JButton addNamespaceButton;
    private JButton modifyNamespaceButton;
    private JButton removeNamespaceButton;
    private JPanel mainPanel;
    private JComboBox dataTypeComboBox;
    private JComboBox idComboBox;

    private XacmlRequestBuilderAssertion.MultipleAttributeConfig multipleAttributeConfig;
    private DefaultTableModel tableModel;
    private JDialog window;

    public XacmlRequestBuilderXpathMultiAttrPanel( final XacmlRequestBuilderAssertion.MultipleAttributeConfig multipleAttributeConfig,
                                                   final XacmlAssertionEnums.XacmlVersionType version,
                                                   final Set<String> idOptions,
                                                   final JDialog window )
    {
        this.multipleAttributeConfig = multipleAttributeConfig;

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

        if(version == XacmlAssertionEnums.XacmlVersionType.V1_0) {
            issueInstantField.setText(multipleAttributeConfig.getField(ISSUE_INSTANT).getValue());
        }

        valueField.setText(multipleAttributeConfig.getField(VALUE).getValue());

        this.window = window;

        init(version, idOptions );

        idComboBox.setSelectedItem(multipleAttributeConfig.getField(ID).getValue());
        dataTypeComboBox.setSelectedItem(multipleAttributeConfig.getField(DATA_TYPE).getValue());
    }

    public JPanel getPanel() {
        return mainPanel;
    }

    public void init( final XacmlAssertionEnums.XacmlVersionType version,
                      final Set<String> idOptions ) {
        idComboBox.setModel( new DefaultComboBoxModel( idOptions.toArray() ) );
        dataTypeComboBox.setModel( new DefaultComboBoxModel( XacmlConstants.XACML_10_DATATYPES.toArray() ) );

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

                        if ( validateNamespacePrefix( null, prefix ) ) {
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
                String namespace = (String)namespacesTable.getValueAt(namespacesTable.getSelectedRow(), 1);
                boolean done = false;
                while ( !done ) {
                    XacmlRequestBuilderNamespaceDialog dialog = new XacmlRequestBuilderNamespaceDialog(window, prefix, namespace);
                    Utilities.centerOnParentWindow(dialog);
                    dialog.setVisible(true);

                    if( dialog.isConfirmed() ) {
                        prefix = dialog.getPrefix();
                        namespace = dialog.getUri();

                        if ( validateNamespacePrefix( originalPrefix, prefix ) ) {
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

        addChangeListener(idComboBox, ID);
        addChangeListener(dataTypeComboBox, DATA_TYPE);
        addChangeListener(issuerField, ISSUER);
        addChangeListener(valueField, VALUE);

        if(version != XacmlAssertionEnums.XacmlVersionType.V1_0) {
            issueInstantLabel.setVisible(false);
            issueInstantFieldsPanel.setVisible(false);
        } else {
            addChangeListener(issueInstantField, ISSUE_INSTANT);
            addActionListener(issueInstantOptionsButton, ISSUE_INSTANT);
        }

        addActionListener(idOptionsButton, ID);
        addActionListener(dataTypeOptionsButton, DATA_TYPE);
        addActionListener(issuerOptionsButton, ISSUER);
        addActionListener(valueOptionsButton, VALUE);

        Utilities.setDoubleClickAction( namespacesTable, modifyNamespaceButton );
        enableOrDisableButtons();
    }

    private void addChangeListener(final JTextField textField, final XacmlRequestBuilderAssertion.MultipleAttributeConfig.FieldName fieldName) {
        textField.getDocument().addDocumentListener(new RunOnChangeListener(new Runnable() {
                @Override
                public void run() {
                    multipleAttributeConfig.getField(fieldName).setValue(textField.getText().trim());
                }
            }));
    }

    private void addChangeListener(final JComboBox field, final XacmlRequestBuilderAssertion.MultipleAttributeConfig.FieldName fieldName) {
        field.addActionListener(new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                multipleAttributeConfig.getField(fieldName).setValue(((String)field.getSelectedItem()).trim());
            }
        }));
    }

    private void addActionListener(JButton button, final XacmlRequestBuilderAssertion.MultipleAttributeConfig.FieldName fieldName) {
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                XacmlRequestBuilderMultiAttrOptionsDialog dialog = new XacmlRequestBuilderMultiAttrOptionsDialog(window, fieldName.toString(), multipleAttributeConfig.getField(fieldName));
                Utilities.centerOnScreen(dialog);
                dialog.setVisible(true);

                if(dialog.isConfirmed()) {
                    multipleAttributeConfig.getField(fieldName).setIsXpath(dialog.getXpathIsExpression());
                    multipleAttributeConfig.getField(fieldName).setIsRelative(dialog.getRelativeToXpath());
                }
            }
        });

    }

    @Override
    public boolean handleDispose() {
        // Access editor directly to get the current text
        multipleAttributeConfig.getField(ID).setValue(((String)idComboBox.getEditor().getItem()).trim());
        multipleAttributeConfig.getField(DATA_TYPE).setValue(((String)dataTypeComboBox.getEditor().getItem()).trim()); 

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
        if ( multipleAttributeConfig.getField(DATA_TYPE).getValue()==null ||
             multipleAttributeConfig.getField(DATA_TYPE).getValue().isEmpty() ) {
            DialogDisplayer.showMessageDialog( this, "Data Type is required. Please enter a Data Type.", "Validation Error", JOptionPane.ERROR_MESSAGE, null );
            return false;
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

    private boolean isDuplicateNamespacePrefix( final String prefix ) {
        return multipleAttributeConfig.getNamespaces().containsKey(prefix);
    }

}
