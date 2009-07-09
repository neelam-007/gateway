package com.l7tech.external.assertions.xacmlpdp.console;

import com.l7tech.external.assertions.xacmlpdp.XacmlRequestBuilderAssertion;
import com.l7tech.external.assertions.xacmlpdp.XacmlAssertionEnums;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DialogDisplayer;

import javax.swing.*;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
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

    private XacmlRequestBuilderAssertion.GenericXmlElementHolder genericXmlElementHolder;
    private DefaultTableModel tableModel;
    private JDialog window;

    /**
     *
     * @param genericXmlElementHolder
     * @param xacmlVersion of the request being generated
     * @param window
     */
    public XacmlRequestBuilderXmlContentPanel(
            XacmlRequestBuilderAssertion.GenericXmlElementHolder genericXmlElementHolder,
            XacmlAssertionEnums.XacmlVersionType xacmlVersion,
            JDialog window) {
        this.genericXmlElementHolder = genericXmlElementHolder;
        this.window = window;

        tableModel = new DefaultTableModel(new Object[] {"Name", "Value"}, 0){
            @Override
            public boolean isCellEditable( int row, int column ) {
                return false;
            }
        };
        for(Map.Entry<String, String> entry : genericXmlElementHolder.getAttributes().entrySet()) {
            tableModel.addRow(new Object[] {entry.getKey(), entry.getValue()});
        }
        xmlAttributesTable.setModel(tableModel);
        xmlAttributesTable.getTableHeader().setReorderingAllowed( false );

        contentField.setText(genericXmlElementHolder.getContent());

        boolean instanceOfRepeatTag =
                genericXmlElementHolder instanceof XacmlRequestBuilderAssertion.XmlElementCanRepeatTag;
        if(instanceOfRepeatTag){
            XacmlRequestBuilderAssertion.XmlElementCanRepeatTag repeatTag =
                    (XacmlRequestBuilderAssertion.XmlElementCanRepeatTag) genericXmlElementHolder;
            repeatCheckBox.setSelected(repeatTag.isCanElementHaveSameTypeSibilings());
        }else{
            repeatCheckBox.setSelected(false);    
        }
        init(xacmlVersion == XacmlAssertionEnums.XacmlVersionType.V2_0 && instanceOfRepeatTag);
    }

    public JPanel getPanel() {
        return mainPanel;
    }

    public void init(boolean showSettings) {
        xmlAttributesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        xmlAttributesTable.getSelectionModel().addListSelectionListener( new ListSelectionListener(){
            @Override
            public void valueChanged( ListSelectionEvent e) {
                enableOrDisableButtons();
            }
        } );

        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                XacmlRequestBuilderXmlAttrPanel dialog = new XacmlRequestBuilderXmlAttrPanel(window, "", "");
                dialog.setVisible(true);

                if(dialog.isConfirmed()) {
                    String attrName = dialog.getName();
                    String attrValue = dialog.getValue();
                    
                    if (isDuplicateAttributeName(attrName)) {
                        DialogDisplayer.showMessageDialog(XacmlRequestBuilderXmlContentPanel.this,
                            "The attribute name '" + attrName + "' already exists.  Please use a new attribute name and try again.",
                            "Duplicate Attribute Name", JOptionPane.ERROR_MESSAGE, null);
                        return;
                    }

                    tableModel.addRow(new Object[] {attrName, attrValue});
                    genericXmlElementHolder.getAttributes().put(attrName, attrValue);
                }
            }
        });

        modifyButton.addActionListener(new ActionListener() {
            @Override
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
                    String newName = dialog.getName();
                    String newValue = dialog.getValue();

                    if (!name.equals(newName) && isDuplicateAttributeName(newName)) {
                        DialogDisplayer.showMessageDialog(XacmlRequestBuilderXmlContentPanel.this,
                            "The attribute name '" + newName + "' already exists.  Please use a new attribute name and try again.",
                            "Duplicate Attribute Name", JOptionPane.ERROR_MESSAGE, null);
                        return;
                    }

                    xmlAttributesTable.setValueAt(newName, selectedRow, 0);
                    xmlAttributesTable.setValueAt(newValue, selectedRow, 1);

                    genericXmlElementHolder.getAttributes().remove(name);
                    genericXmlElementHolder.getAttributes().put(newName, newValue);
                }
            }
        });

        removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                int selectedRow = xmlAttributesTable.getSelectedRow();
                if(selectedRow == -1) {
                    return;
                }

                String name = (String)xmlAttributesTable.getValueAt(selectedRow, 0);
                tableModel.removeRow(selectedRow);
                genericXmlElementHolder.getAttributes().remove(name);
            }
        });

        contentField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void changedUpdate(DocumentEvent evt) {
                genericXmlElementHolder.setContent(contentField.getText().trim());
            }

            @Override
            public void insertUpdate(DocumentEvent evt) {
                genericXmlElementHolder.setContent(contentField.getText().trim());
            }

            @Override
            public void removeUpdate(DocumentEvent evt) {
                genericXmlElementHolder.setContent(contentField.getText().trim());
            }
        });

        repeatCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                if(genericXmlElementHolder instanceof XacmlRequestBuilderAssertion.XmlElementCanRepeatTag){
                    XacmlRequestBuilderAssertion.XmlElementCanRepeatTag repeatTag =
                            (XacmlRequestBuilderAssertion.XmlElementCanRepeatTag) genericXmlElementHolder;
                    repeatTag.setCanElementHaveSameTypeSibilings(repeatCheckBox.isSelected());
                }
            }
        });

        settingsPanel.setVisible(showSettings);

        Utilities.setDoubleClickAction( xmlAttributesTable, modifyButton );
        enableOrDisableButtons();
    }

    @Override
    public boolean handleDispose() {
        return true;
    }

    private void enableOrDisableButtons() {
        boolean enable = xmlAttributesTable.getSelectedRow() > -1;
        modifyButton.setEnabled( enable );
        removeButton.setEnabled( enable );
    }

    private boolean isDuplicateAttributeName(String attrName) {
        return genericXmlElementHolder.getAttributes().containsKey(attrName);
    }
}
