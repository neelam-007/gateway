package com.l7tech.external.assertions.xacmlpdp.console;

import com.l7tech.external.assertions.xacmlpdp.XacmlRequestBuilderAssertion;
import com.l7tech.external.assertions.xacmlpdp.XacmlAssertionEnums;
import com.l7tech.gui.util.Utilities;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
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

    private XacmlRequestBuilderAssertion.XpathMultiAttr xpathMultiAttr;
    private DefaultTableModel tableModel;
    private JDialog window;

    public XacmlRequestBuilderXpathMultiAttrPanel(XacmlRequestBuilderAssertion.XpathMultiAttr xpathMultiAttr,
                                                  XacmlAssertionEnums.XacmlVersionType version,
                                                  JDialog window)
    {
        this.xpathMultiAttr = xpathMultiAttr;

        messageSourceComboBox.setModel(new DefaultComboBoxModel(new Object[] {"Default Request", "Default Response"}));
        if(xpathMultiAttr.getMessageSource() == XacmlRequestBuilderAssertion.XpathMultiAttr.MessageSource.REQUEST) {
            messageSourceComboBox.setSelectedIndex(0);
        } else if(xpathMultiAttr.getMessageSource() == XacmlRequestBuilderAssertion.XpathMultiAttr.MessageSource.RESPONSE) {
            messageSourceComboBox.setSelectedIndex(1);
        }

        tableModel = new DefaultTableModel(new String[] {"Prefix", "URI"}, 0);
        if(xpathMultiAttr.getNamespaces() != null) {
            for(Map.Entry<String, String> entry : xpathMultiAttr.getNamespaces().entrySet()) {
                tableModel.addRow(new String[] {entry.getKey(), entry.getValue()});
            }
        }
        namespacesTable.setModel(tableModel);

        xpathBaseField.setText(xpathMultiAttr.getXpathBase());
        idField.setText(xpathMultiAttr.getIdField().getValue());
        dataTypeField.setText(xpathMultiAttr.getDataTypeField().getValue());
        issuerField.setText(xpathMultiAttr.getIssuerField().getValue());

        if(version == XacmlAssertionEnums.XacmlVersionType.V1_0) {
            issueInstantField.setText(xpathMultiAttr.getIssueInstantField().getValue());
        }

        valueField.setText(xpathMultiAttr.getValueField().getValue());

        this.window = window;

        init(version);
    }

    public JPanel getPanel() {
        return mainPanel;
    }

    public void init(XacmlAssertionEnums.XacmlVersionType version) {
        messageSourceComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                if("Default Request".equals(messageSourceComboBox.getSelectedItem())) {
                    xpathMultiAttr.setMessageSource(XacmlRequestBuilderAssertion.XpathMultiAttr.MessageSource.REQUEST);
                } else if("Default Response".equals(messageSourceComboBox.getSelectedItem())) {
                    xpathMultiAttr.setMessageSource(XacmlRequestBuilderAssertion.XpathMultiAttr.MessageSource.RESPONSE);
                }
            }
        });

        addNamespaceButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                XacmlRequestBuilderNamespaceDialog dialog = new XacmlRequestBuilderNamespaceDialog(window, null, null);
                Utilities.centerOnScreen(dialog);
                dialog.setVisible(true);

                if(dialog.isConfirmed()) {
                    tableModel.addRow(new String[] {dialog.getPrefix(), dialog.getUri()});
                    if(xpathMultiAttr.getNamespaces() == null) {
                        xpathMultiAttr.setNamespaces(new HashMap<String, String>(1));
                    }
                    xpathMultiAttr.getNamespaces().put(dialog.getPrefix(), dialog.getUri());
                }
            }
        });

        modifyNamespaceButton.addActionListener(new ActionListener() {
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
                        xpathMultiAttr.getNamespaces().remove(prefix);
                    }
                    xpathMultiAttr.getNamespaces().put(prefix, dialog.getUri());
                }
            }
        });

        removeNamespaceButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                if(namespacesTable.getSelectedRow() == -1) {
                    return;
                }

                String ns = namespacesTable.getValueAt(namespacesTable.getSelectedRow(), 0).toString();
                xpathMultiAttr.getNamespaces().remove(ns);
                tableModel.removeRow(namespacesTable.getSelectedRow());
            }
        });

        xpathBaseField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent evt) {
                xpathMultiAttr.setXpathBase(xpathBaseField.getText().trim());
            }

            public void insertUpdate(DocumentEvent evt) {
                xpathMultiAttr.setXpathBase(xpathBaseField.getText().trim());
            }

            public void removeUpdate(DocumentEvent evt) {
                xpathMultiAttr.setXpathBase(xpathBaseField.getText().trim());
            }
        });

        idField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent evt) {
                xpathMultiAttr.getIdField().setValue(idField.getText().trim());
            }

            public void insertUpdate(DocumentEvent evt) {
                xpathMultiAttr.getIdField().setValue(idField.getText().trim());
            }

            public void removeUpdate(DocumentEvent evt) {
                xpathMultiAttr.getIdField().setValue(idField.getText().trim());
            }
        });

        dataTypeField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent evt) {
                xpathMultiAttr.getDataTypeField().setValue(dataTypeField.getText().trim());
            }

            public void insertUpdate(DocumentEvent evt) {
                xpathMultiAttr.getDataTypeField().setValue(dataTypeField.getText().trim());
            }

            public void removeUpdate(DocumentEvent evt) {
                xpathMultiAttr.getDataTypeField().setValue(dataTypeField.getText().trim());
            }
        });

        issuerField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent evt) {
                xpathMultiAttr.getIssuerField().setValue(issuerField.getText().trim());
            }

            public void insertUpdate(DocumentEvent evt) {
                xpathMultiAttr.getIssuerField().setValue(issuerField.getText().trim());
            }

            public void removeUpdate(DocumentEvent evt) {
                xpathMultiAttr.getIssuerField().setValue(issuerField.getText().trim());
            }
        });

        if(version != XacmlAssertionEnums.XacmlVersionType.V1_0) {
            issueInstantLabel.setVisible(false);
            issueInstantFieldsPanel.setVisible(false);
        } else {
            issueInstantField.getDocument().addDocumentListener(new DocumentListener() {
                public void changedUpdate(DocumentEvent evt) {
                    xpathMultiAttr.getIssueInstantField().setValue(issueInstantField.getText().trim());
                }

                public void insertUpdate(DocumentEvent evt) {
                    xpathMultiAttr.getIssueInstantField().setValue(issueInstantField.getText().trim());
                }

                public void removeUpdate(DocumentEvent evt) {
                    xpathMultiAttr.getIssueInstantField().setValue(issueInstantField.getText().trim());
                }
            });

            issueInstantOptionsButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    XacmlRequestBuilderMultiAttrOptionsDialog dialog = new XacmlRequestBuilderMultiAttrOptionsDialog(window, "Issue Instant", xpathMultiAttr.getIssueInstantField());
                    Utilities.centerOnScreen(dialog);
                    dialog.setVisible(true);

                    if(dialog.isConfirmed()) {
                        xpathMultiAttr.getIssueInstantField().setIsXpath(dialog.getXpathIsExpression());
                        xpathMultiAttr.getIssueInstantField().setIsRelative(dialog.getRelativeToXpath());
                    }
                }
            });
        }

        valueField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent evt) {
                xpathMultiAttr.getValueField().setValue(valueField.getText().trim());
            }

            public void insertUpdate(DocumentEvent evt) {
                xpathMultiAttr.getValueField().setValue(valueField.getText().trim());
            }

            public void removeUpdate(DocumentEvent evt) {
                xpathMultiAttr.getValueField().setValue(valueField.getText().trim());
            }
        });

        idOptionsButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                XacmlRequestBuilderMultiAttrOptionsDialog dialog = new XacmlRequestBuilderMultiAttrOptionsDialog(window, "ID", xpathMultiAttr.getIdField());
                Utilities.centerOnScreen(dialog);
                dialog.setVisible(true);

                if(dialog.isConfirmed()) {
                    xpathMultiAttr.getIdField().setIsXpath(dialog.getXpathIsExpression());
                    xpathMultiAttr.getIdField().setIsRelative(dialog.getRelativeToXpath());
                }
            }
        });

        dataTypeOptionsButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                XacmlRequestBuilderMultiAttrOptionsDialog dialog = new XacmlRequestBuilderMultiAttrOptionsDialog(window, "Data Type", xpathMultiAttr.getDataTypeField());
                Utilities.centerOnScreen(dialog);
                dialog.setVisible(true);

                if(dialog.isConfirmed()) {
                    xpathMultiAttr.getDataTypeField().setIsXpath(dialog.getXpathIsExpression());
                    xpathMultiAttr.getDataTypeField().setIsRelative(dialog.getRelativeToXpath());
                }
            }
        });

        issuerOptionsButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                XacmlRequestBuilderMultiAttrOptionsDialog dialog = new XacmlRequestBuilderMultiAttrOptionsDialog(window, "Issuer", xpathMultiAttr.getIssuerField());
                Utilities.centerOnScreen(dialog);
                dialog.setVisible(true);

                if(dialog.isConfirmed()) {
                    xpathMultiAttr.getIssuerField().setIsXpath(dialog.getXpathIsExpression());
                    xpathMultiAttr.getIssuerField().setIsRelative(dialog.getRelativeToXpath());
                }
            }
        });

        valueOptionsButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                XacmlRequestBuilderMultiAttrOptionsDialog dialog = new XacmlRequestBuilderMultiAttrOptionsDialog(window, "Value", xpathMultiAttr.getValueField());
                Utilities.centerOnScreen(dialog);
                dialog.setVisible(true);

                if(dialog.isConfirmed()) {
                    xpathMultiAttr.getValueField().setIsXpath(dialog.getXpathIsExpression());
                    xpathMultiAttr.getValueField().setIsRelative(dialog.getRelativeToXpath());
                }
            }
        });
    }

    public boolean handleDispose() {
        return true;
    }
}
