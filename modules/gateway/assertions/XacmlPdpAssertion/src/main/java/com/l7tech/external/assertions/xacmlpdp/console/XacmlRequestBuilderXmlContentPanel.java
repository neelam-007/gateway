package com.l7tech.external.assertions.xacmlpdp.console;

import com.l7tech.external.assertions.xacmlpdp.XacmlRequestBuilderAssertion;

import javax.swing.*;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import javax.swing.table.DefaultTableModel;
import java.util.Map;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * Copyright (C) 2009, Layer 7 Technologies Inc.
 * User: njordan
 * Date: 2-Apr-2009
 * Time: 6:06:40 PM
 * To change this template use File | Settings | File Templates.
 */
public class XacmlRequestBuilderXmlContentPanel extends JPanel implements XacmlRequestBuilderNodePanel {
    private JTable xmlAttributesTable;
    private JButton addButton;
    private JButton modifyButton;
    private JButton removeButton;
    private JTextArea contentField;
    private JCheckBox repeatCheckBox;
    private JPanel settingsPanel;
    private JPanel mainPanel;

    private XacmlRequestBuilderAssertion.XmlTag xmlTag;
    private DefaultTableModel tableModel;
    private JDialog window;

    public XacmlRequestBuilderXmlContentPanel(XacmlRequestBuilderAssertion.XmlTag xmlTag, boolean showSettings, JDialog window) {
        this.xmlTag = xmlTag;
        this.window = window;

        tableModel = new DefaultTableModel(new Object[] {"Name", "Value"}, 0);
        for(Map.Entry<String, String> entry : xmlTag.getAttributes().entrySet()) {
            tableModel.addRow(new Object[] {entry.getKey(), entry.getValue()});
        }
        xmlAttributesTable.setModel(tableModel);

        contentField.setText(xmlTag.getContent());

        repeatCheckBox.setSelected(xmlTag.getRepeat());

        init(showSettings);
    }

    public JPanel getPanel() {
        return mainPanel;
    }

    public void init(boolean showSettings) {
        xmlAttributesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                XacmlRequestBuilderXmlAttrPanel dialog = new XacmlRequestBuilderXmlAttrPanel(window, "", "");
                dialog.setVisible(true);

                if(dialog.isConfirmed()) {
                    tableModel.addRow(new Object[] {dialog.getName(), dialog.getValue()});
                    xmlTag.getAttributes().put(dialog.getName(), dialog.getValue());
                }
            }
        });

        modifyButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                int selectedRow = xmlAttributesTable.getSelectedRow();
                if(selectedRow == -1) {
                    return;
                }

                String name = (String)xmlAttributesTable.getValueAt(selectedRow, 0);
                String value = (String)xmlAttributesTable.getValueAt(selectedRow, 1);

                XacmlRequestBuilderXmlAttrPanel dialog = new XacmlRequestBuilderXmlAttrPanel(window, name, value);
                dialog.setVisible(true);

                if(dialog.isConfirmed()) {
                    xmlAttributesTable.setValueAt(dialog.getName(), selectedRow, 0);
                    xmlAttributesTable.setValueAt(dialog.getValue(), selectedRow, 1);

                    xmlTag.getAttributes().remove(name);
                    xmlTag.getAttributes().put(name, value);
                }
            }
        });

        removeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                int selectedRow = xmlAttributesTable.getSelectedRow();
                if(selectedRow == -1) {
                    return;
                }

                String name = (String)xmlAttributesTable.getValueAt(selectedRow, 0);
                tableModel.removeRow(selectedRow);
                xmlTag.getAttributes().remove(name);
            }
        });

        contentField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent evt) {
                xmlTag.setContent(contentField.getText().trim());
            }

            public void insertUpdate(DocumentEvent evt) {
                xmlTag.setContent(contentField.getText().trim());
            }

            public void removeUpdate(DocumentEvent evt) {
                xmlTag.setContent(contentField.getText().trim());
            }
        });

        repeatCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                xmlTag.setRepeat(repeatCheckBox.isSelected());
            }
        });

        settingsPanel.setVisible(showSettings);
    }

    public boolean handleDispose() {
        return true;
    }
}
