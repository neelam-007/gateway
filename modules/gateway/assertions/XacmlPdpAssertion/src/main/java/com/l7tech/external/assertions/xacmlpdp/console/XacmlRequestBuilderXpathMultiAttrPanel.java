package com.l7tech.external.assertions.xacmlpdp.console;

import com.l7tech.external.assertions.xacmlpdp.XacmlRequestBuilderAssertion;
import com.l7tech.external.assertions.xacmlpdp.XacmlAssertionEnums;
import static com.l7tech.external.assertions.xacmlpdp.XacmlRequestBuilderAssertion.MultipleAttributeConfig.FieldName.*;
import com.l7tech.gui.util.Utilities;
import com.l7tech.util.Functions;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.Arrays;

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
    private JTextField idField;
    private JButton idOptionsButton;
    private JTextField dataTypeField;
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

    private XacmlRequestBuilderAssertion.MultipleAttributeConfig multipleAttributeConfig;
    private DefaultTableModel tableModel;
    private JDialog window;

    public XacmlRequestBuilderXpathMultiAttrPanel(XacmlRequestBuilderAssertion.MultipleAttributeConfig multipleAttributeConfig,
                                                  XacmlAssertionEnums.XacmlVersionType version,
                                                  JDialog window)
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
        idField.setText(multipleAttributeConfig.getField(ID).getValue());
        dataTypeField.setText(multipleAttributeConfig.getField(DATA_TYPE).getValue());
        issuerField.setText(multipleAttributeConfig.getField(ISSUER).getValue());

        if(version == XacmlAssertionEnums.XacmlVersionType.V1_0) {
            issueInstantField.setText(multipleAttributeConfig.getField(ISSUE_INSTANT).getValue());
        }

        valueField.setText(multipleAttributeConfig.getField(VALUE).getValue());

        this.window = window;

        init(version);
    }

    public JPanel getPanel() {
        return mainPanel;
    }

    public void init(XacmlAssertionEnums.XacmlVersionType version) {
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
                XacmlRequestBuilderNamespaceDialog dialog = new XacmlRequestBuilderNamespaceDialog(window, null, null);
                Utilities.centerOnScreen(dialog);
                dialog.setVisible(true);

                if(dialog.isConfirmed()) {
                    tableModel.addRow(new String[] {dialog.getPrefix(), dialog.getUri()});
                    if(multipleAttributeConfig.getNamespaces() == null) {
                        multipleAttributeConfig.setNamespaces(new HashMap<String, String>(1));
                    }
                    multipleAttributeConfig.getNamespaces().put(dialog.getPrefix(), dialog.getUri());
                }
            }
        });

        modifyNamespaceButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                if(namespacesTable.getSelectedRow() == -1) {
                    return;
                }

                String prefix = (String)namespacesTable.getValueAt(namespacesTable.getSelectedRow(), 0);
                String uri = (String)namespacesTable.getValueAt(namespacesTable.getSelectedRow(), 1);
                XacmlRequestBuilderNamespaceDialog dialog = new XacmlRequestBuilderNamespaceDialog(window, prefix, uri);
                Utilities.centerOnScreen(dialog);
                dialog.setVisible(true);

                if(dialog.isConfirmed()) {
                    tableModel.setValueAt(dialog.getPrefix(), namespacesTable.getSelectedRow(), 0);
                    tableModel.setValueAt(dialog.getUri(), namespacesTable.getSelectedRow(), 1);

                    if(!prefix.equals(dialog.getPrefix())) {
                        multipleAttributeConfig.getNamespaces().remove(prefix);
                    }
                    multipleAttributeConfig.getNamespaces().put(prefix, dialog.getUri());
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

        xpathBaseField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void changedUpdate(DocumentEvent evt) {
                multipleAttributeConfig.setXpathBase(xpathBaseField.getText().trim());
            }

            @Override
            public void insertUpdate(DocumentEvent evt) {
                multipleAttributeConfig.setXpathBase(xpathBaseField.getText().trim());
            }

            @Override
            public void removeUpdate(DocumentEvent evt) {
                multipleAttributeConfig.setXpathBase(xpathBaseField.getText().trim());
            }
        });

        addDocumentListener(idField, ID);
        addDocumentListener(dataTypeField, DATA_TYPE);
        addDocumentListener(issuerField, ISSUER);
        addDocumentListener(valueField, VALUE);

        if(version != XacmlAssertionEnums.XacmlVersionType.V1_0) {
            issueInstantLabel.setVisible(false);
            issueInstantFieldsPanel.setVisible(false);
        } else {
            addDocumentListener(issueInstantField, ISSUE_INSTANT);
            addActionListener(issueInstantOptionsButton, ISSUE_INSTANT);
        }

        addActionListener(idOptionsButton, ID);
        addActionListener(dataTypeOptionsButton, DATA_TYPE);
        addActionListener(issuerOptionsButton, ISSUER);
        addActionListener(valueOptionsButton, VALUE);

        Utilities.setDoubleClickAction( namespacesTable, modifyNamespaceButton );
        enableOrDisableButtons();
    }

    private void addDocumentListener(final JTextField textField, final XacmlRequestBuilderAssertion.MultipleAttributeConfig.FieldName fieldName) {
        textField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void changedUpdate(DocumentEvent evt) {
                multipleAttributeConfig.getField(fieldName).setValue(textField.getText().trim());
            }

            @Override
            public void insertUpdate(DocumentEvent evt) {
                multipleAttributeConfig.getField(fieldName).setValue(textField.getText().trim());
            }

            @Override
            public void removeUpdate(DocumentEvent evt) {
                multipleAttributeConfig.getField(fieldName).setValue(textField.getText().trim());
            }
        });

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
        return true;
    }

    private void enableOrDisableButtons() {
        boolean enable = namespacesTable.getSelectedRow() > -1;
        modifyNamespaceButton.setEnabled( enable );
        removeNamespaceButton.setEnabled( enable );
    }
}
