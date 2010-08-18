package com.l7tech.external.assertions.saml2attributequery.console;

import com.l7tech.gui.util.Utilities;
import com.l7tech.external.assertions.saml2attributequery.SamlToLdapMap;

import javax.swing.*;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 6-Feb-2009
 * Time: 8:35:50 PM
 * To change this template use File | Settings | File Templates.
 */
public class SamlToLdapAttributeMapDialog extends JDialog {
    private JButton okButton;
    private JButton cancelButton;
    private JTable mapTable;
    private JButton addButton;
    private JButton removeButton;
    private JButton editButton;
    private JPanel mainPanel;
    private JTextField mappingNameField;
    private boolean confirmed = false;

    public SamlToLdapAttributeMapDialog(JDialog owner, boolean modal, String name, SamlToLdapMap map, boolean readOnly) {
        super(owner, "Validate Digital Signature", modal);
        setContentPane(mainPanel);

        mapTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        updateView(name, map);
        
        mappingNameField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent evt) {
                okButton.setEnabled(isDataValid());
            }

            public void insertUpdate(DocumentEvent evt) {
                okButton.setEnabled(isDataValid());
            }

            public void removeUpdate(DocumentEvent evt) {
                okButton.setEnabled(isDataValid());
            }
        });

        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                SamlLdapMappingEntryDialog dialog = new SamlLdapMappingEntryDialog(SamlToLdapAttributeMapDialog.this, true, null, null, null);
                dialog.setVisible(true);
                if(dialog.isConfirmed()) {
                    ((DefaultTableModel)mapTable.getModel()).addRow(new Object[] {dialog.getSamlAttribute(), dialog.getNameFormat(), dialog.getLdapAttribute()});
                }

                okButton.setEnabled(isDataValid());
            }
        });

        removeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                ((DefaultTableModel)mapTable.getModel()).removeRow(mapTable.getSelectedRow());

                okButton.setEnabled(isDataValid());
            }
        });

        editButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                DefaultTableModel model = (DefaultTableModel)mapTable.getModel();
                int selectedRow = mapTable.getSelectedRow();

                String samlAttribute = (String)model.getValueAt(selectedRow, 0);
                String nameFormat = (String)model.getValueAt(selectedRow, 1);
                String ldapAttribute = (String)model.getValueAt(selectedRow, 2);

                SamlLdapMappingEntryDialog dialog = new SamlLdapMappingEntryDialog(SamlToLdapAttributeMapDialog.this, true, samlAttribute, ldapAttribute, nameFormat);
                dialog.setVisible(true);

                if(dialog.isConfirmed()) {
                    model.setValueAt(dialog.getSamlAttribute(), selectedRow, 0);
                    model.setValueAt(dialog.getNameFormat(), selectedRow, 1);
                    model.setValueAt(dialog.getLdapAttribute(), selectedRow, 2);
                }

                okButton.setEnabled(isDataValid());
            }
        });

        Utilities.equalizeButtonSizes(new JButton[] { okButton, cancelButton });

        okButton.setEnabled( !readOnly && isDataValid() );
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (!isDataValid())
                    return;
                confirmed = true;

                dispose();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                confirmed = false;
                dispose();
            }
        });

        pack();

        getRootPane().setDefaultButton(okButton);
        Utilities.runActionOnEscapeKey(getRootPane(), new AbstractAction() {
            public void actionPerformed(ActionEvent e) { cancelButton.doClick(); }
        });

        Utilities.centerOnScreen(this);
    }

    private void updateView(String name, SamlToLdapMap map) {
        mappingNameField.setText(name == null ? "" : name);

        mapTable.setModel(new DefaultTableModel(map.asObjectArray(), new Object[] {"SAML Attribute Name", "Name Format", "LDAP Attribute Name"}));
    }

    private boolean isDataValid() {
        return mappingNameField.getText().trim().length() > 0 && mapTable.getModel().getRowCount() > 0;
    }

    public String getMappingName() {
        return mappingNameField.getText().trim();
    }

    public SamlToLdapMap getMap() {
        SamlToLdapMap map = new SamlToLdapMap();

        for(int i = 0;i < mapTable.getModel().getRowCount();i++) {
            map.add((String)mapTable.getModel().getValueAt(i, 0), (String)mapTable.getModel().getValueAt(i, 1), (String)mapTable.getModel().getValueAt(i, 2));
        }

        return map;
    }

    /** @return true if Ok button was pressed. */
    public boolean isConfirmed() {
        return confirmed;
    }
}
