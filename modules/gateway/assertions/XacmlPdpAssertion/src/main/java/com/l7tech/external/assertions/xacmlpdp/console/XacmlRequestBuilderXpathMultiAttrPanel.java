package com.l7tech.external.assertions.xacmlpdp.console;

import com.l7tech.external.assertions.xacmlpdp.XacmlRequestBuilderAssertion;
import com.l7tech.external.assertions.xacmlpdp.XacmlAssertionEnums;
import com.l7tech.gui.util.Utilities;

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
        idField.setText(multipleAttributeConfig.getIdField().getValue());
        dataTypeField.setText(multipleAttributeConfig.getDataTypeField().getValue());
        issuerField.setText(multipleAttributeConfig.getIssuerField().getValue());

        if(version == XacmlAssertionEnums.XacmlVersionType.V1_0) {
            issueInstantField.setText(multipleAttributeConfig.getIssueInstantField().getValue());
        }

        valueField.setText(multipleAttributeConfig.getValueField().getValue());

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

        idField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void changedUpdate(DocumentEvent evt) {
                multipleAttributeConfig.getIdField().setValue(idField.getText().trim());
            }

            @Override
            public void insertUpdate(DocumentEvent evt) {
                multipleAttributeConfig.getIdField().setValue(idField.getText().trim());
            }

            @Override
            public void removeUpdate(DocumentEvent evt) {
                multipleAttributeConfig.getIdField().setValue(idField.getText().trim());
            }
        });

        dataTypeField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void changedUpdate(DocumentEvent evt) {
                multipleAttributeConfig.getDataTypeField().setValue(dataTypeField.getText().trim());
            }

            @Override
            public void insertUpdate(DocumentEvent evt) {
                multipleAttributeConfig.getDataTypeField().setValue(dataTypeField.getText().trim());
            }

            @Override
            public void removeUpdate(DocumentEvent evt) {
                multipleAttributeConfig.getDataTypeField().setValue(dataTypeField.getText().trim());
            }
        });

        issuerField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void changedUpdate(DocumentEvent evt) {
                multipleAttributeConfig.getIssuerField().setValue(issuerField.getText().trim());
            }

            @Override
            public void insertUpdate(DocumentEvent evt) {
                multipleAttributeConfig.getIssuerField().setValue(issuerField.getText().trim());
            }

            @Override
            public void removeUpdate(DocumentEvent evt) {
                multipleAttributeConfig.getIssuerField().setValue(issuerField.getText().trim());
            }
        });

        if(version != XacmlAssertionEnums.XacmlVersionType.V1_0) {
            issueInstantLabel.setVisible(false);
            issueInstantFieldsPanel.setVisible(false);
        } else {
            issueInstantField.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void changedUpdate(DocumentEvent evt) {
                    multipleAttributeConfig.getIssueInstantField().setValue(issueInstantField.getText().trim());
                }

                @Override
                public void insertUpdate(DocumentEvent evt) {
                    multipleAttributeConfig.getIssueInstantField().setValue(issueInstantField.getText().trim());
                }

                @Override
                public void removeUpdate(DocumentEvent evt) {
                    multipleAttributeConfig.getIssueInstantField().setValue(issueInstantField.getText().trim());
                }
            });

            issueInstantOptionsButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    XacmlRequestBuilderMultiAttrOptionsDialog dialog = new XacmlRequestBuilderMultiAttrOptionsDialog(window, "Issue Instant", multipleAttributeConfig.getIssueInstantField());
                    Utilities.centerOnScreen(dialog);
                    dialog.setVisible(true);

                    if(dialog.isConfirmed()) {
                        multipleAttributeConfig.getIssueInstantField().setIsXpath(dialog.getXpathIsExpression());
                        multipleAttributeConfig.getIssueInstantField().setIsRelative(dialog.getRelativeToXpath());
                    }
                }
            });
        }

        valueField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void changedUpdate(DocumentEvent evt) {
                multipleAttributeConfig.getValueField().setValue(valueField.getText().trim());
            }

            @Override
            public void insertUpdate(DocumentEvent evt) {
                multipleAttributeConfig.getValueField().setValue(valueField.getText().trim());
            }

            @Override
            public void removeUpdate(DocumentEvent evt) {
                multipleAttributeConfig.getValueField().setValue(valueField.getText().trim());
            }
        });

        idOptionsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                XacmlRequestBuilderMultiAttrOptionsDialog dialog = new XacmlRequestBuilderMultiAttrOptionsDialog(window, "ID", multipleAttributeConfig.getIdField());
                Utilities.centerOnScreen(dialog);
                dialog.setVisible(true);

                if(dialog.isConfirmed()) {
                    multipleAttributeConfig.getIdField().setIsXpath(dialog.getXpathIsExpression());
                    multipleAttributeConfig.getIdField().setIsRelative(dialog.getRelativeToXpath());
                }
            }
        });

        dataTypeOptionsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                XacmlRequestBuilderMultiAttrOptionsDialog dialog = new XacmlRequestBuilderMultiAttrOptionsDialog(window, "Data Type", multipleAttributeConfig.getDataTypeField());
                Utilities.centerOnScreen(dialog);
                dialog.setVisible(true);

                if(dialog.isConfirmed()) {
                    multipleAttributeConfig.getDataTypeField().setIsXpath(dialog.getXpathIsExpression());
                    multipleAttributeConfig.getDataTypeField().setIsRelative(dialog.getRelativeToXpath());
                }
            }
        });

        issuerOptionsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                XacmlRequestBuilderMultiAttrOptionsDialog dialog = new XacmlRequestBuilderMultiAttrOptionsDialog(window, "Issuer", multipleAttributeConfig.getIssuerField());
                Utilities.centerOnScreen(dialog);
                dialog.setVisible(true);

                if(dialog.isConfirmed()) {
                    multipleAttributeConfig.getIssuerField().setIsXpath(dialog.getXpathIsExpression());
                    multipleAttributeConfig.getIssuerField().setIsRelative(dialog.getRelativeToXpath());
                }
            }
        });

        valueOptionsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                XacmlRequestBuilderMultiAttrOptionsDialog dialog = new XacmlRequestBuilderMultiAttrOptionsDialog(window, "Value", multipleAttributeConfig.getValueField());
                Utilities.centerOnScreen(dialog);
                dialog.setVisible(true);

                if(dialog.isConfirmed()) {
                    multipleAttributeConfig.getValueField().setIsXpath(dialog.getXpathIsExpression());
                    multipleAttributeConfig.getValueField().setIsRelative(dialog.getRelativeToXpath());
                }
            }
        });

        Utilities.setDoubleClickAction( namespacesTable, modifyNamespaceButton );
        enableOrDisableButtons();
    }

    @Override
    public boolean handleDispose() {
        return true;
    }

    private void enableOrDisableButtons() {
        boolean enable = namespacesTable.getSelectedRow() > -1;
        modifyNamespaceButton.setEnabled( enable );
        removeNamespaceButton.setEnabled( enable );
    }
}
