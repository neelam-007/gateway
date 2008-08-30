package com.l7tech.external.assertions.messagecontext.console;

import com.l7tech.console.panels.AssertionPropertiesEditorSupport;
import com.l7tech.console.panels.PermissionFlags;
import com.l7tech.console.panels.ConfigureMessageContextMappingDialog;
import com.l7tech.console.util.TopComponents;
import com.l7tech.external.assertions.messagecontext.MessageContextAssertion;
import com.l7tech.gateway.common.security.rbac.EntityType;
import com.l7tech.gateway.common.mapping.MessageContextMapping;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DialogDisplayer;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.AbstractTableModel;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * @Copyright: Layer 7 Tech. Inc.
 * @Author: ghuang
 * @Date: Aug 5, 2008
 */
public class MessageContextAssertionPropertiesDialog extends AssertionPropertiesEditorSupport<MessageContextAssertion> {
    private static final String DIALOG_TITLE = "Message Context Assertion Properties";
    private final PermissionFlags flags;

    private static final int MAX_NUM_OF_TABLE_COLUMNS = 3;
    private JPanel mainPanel;
    private JButton okButton;
    private JButton cancelButton;
    private JButton addButton;
    private JButton removeButton;
    private JButton propertiesButton;
    private JTable mappingTable;

    private MessageContextMappingTableModel messageContextMappingTableModel;
    private MessageContextAssertion assertion;
    private List<MessageContextMapping> mappings = new ArrayList();
    private int updatedRowPosition = 0;
    private boolean ok = false;

    public MessageContextAssertionPropertiesDialog(Frame owner, MessageContextAssertion assertion) {
        //todo: check readonly
        super(owner, DIALOG_TITLE, true);
        this.assertion = assertion;
        mappings.addAll(Arrays.asList(assertion.getMappings()));
        flags = PermissionFlags.get(EntityType.MESSAGE_CONTEXT_MAPPING_KEYS);
        initialize(true);
    }

    public MessageContextAssertionPropertiesDialog(Dialog owner, MessageContextAssertion assertion) {
        //todo: check readonly
        super(owner, DIALOG_TITLE, true);
        this.assertion = assertion;
        mappings.addAll(Arrays.asList(assertion.getMappings()));
        flags = PermissionFlags.get(EntityType.MESSAGE_CONTEXT_MAPPING_KEYS);
        initialize(true);
    }

    private void initialize(boolean readOnly) {
        initMessageContextMappingTable();

        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ok();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ok = false;
                dispose();
            }
        });

        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                final ConfigureMessageContextMappingDialog dlg
                    = new ConfigureMessageContextMappingDialog(TopComponents.getInstance().getTopParent(), null);
                DialogDisplayer.display(dlg, new Runnable() {
                    public void run() {
                        if (dlg.wasOKed()) {
                            updatedRowPosition = mappingTable.getSelectedRow() + 1;
                            mappings.add(updatedRowPosition, dlg.getMapping());
                            ((AbstractTableModel)mappingTable.getModel()).fireTableDataChanged();
                        }
                    }
                });
            }
        });

        removeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int idx = mappingTable.getSelectedRow();
                Object[] options = {"Remove", "Cancel"};
                int result = JOptionPane.showOptionDialog(null,
                    "Are you sure you want to remove this mapping " + mappings.get(idx).getKey() + "?",
                    "Remove Message Context Mapping?", 0, JOptionPane.WARNING_MESSAGE, null, options, options[1]);
                if (result == 0) {
                    mappings.remove(idx);
                    updatedRowPosition = (idx == mappings.size())? (idx - 1) : idx;
                    ((AbstractTableModel)mappingTable.getModel()).fireTableDataChanged();

                }
            }
        });

        propertiesButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                final int idx = mappingTable.getSelectedRow();
                MessageContextMapping currentMapping = mappings.get(idx);
                final ConfigureMessageContextMappingDialog dlg
                    = new ConfigureMessageContextMappingDialog(MessageContextAssertionPropertiesDialog.this, currentMapping);
                DialogDisplayer.display(dlg, new Runnable() {
                    public void run() {
                        if (dlg.wasOKed()) {
                            updatedRowPosition = idx;
                            ((AbstractTableModel)mappingTable.getModel()).fireTableDataChanged();
                        }
                    }
                });
            }
        });

        Utilities.setDoubleClickAction( mappingTable, propertiesButton);
        enableOrDisableButtons();
        getContentPane().add(mainPanel);
    }

    private void initMessageContextMappingTable() {
        mappingTable.setModel(getMessageContextMappingTableModel());
        mappingTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        mappingTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                enableOrDisableButtons();
            }
        });
        Utilities.setDoubleClickAction(mappingTable, propertiesButton);
    }

    private MessageContextMappingTableModel getMessageContextMappingTableModel() {
        if (messageContextMappingTableModel == null) {
            messageContextMappingTableModel = new MessageContextMappingTableModel();
        }
        return messageContextMappingTableModel;
    }

    private class MessageContextMappingTableModel extends AbstractTableModel {
        public int getColumnCount() {
            return MAX_NUM_OF_TABLE_COLUMNS;
        }

        public void fireTableDataChanged() {
            super.fireTableDataChanged();
            enableOrDisableButtons();
            mappingTable.getSelectionModel().setSelectionInterval(updatedRowPosition, updatedRowPosition);
        }

        public int getRowCount() {
            return mappings.size();
        }

        public String getColumnName(int col) {
            switch (col) {
                case 0:
                    return "Type";
                case 1:
                    return "Key";
                case 2:
                    return "Value";
                default:
                    return "?";
            }
        }

        public boolean isCellEditable(int row, int col) {
            return false;
        }

        public Object getValueAt(int row, int col) {
            switch (col) {
                case 0:
                    return mappings.get(row).getMappingType();
                case 1:
                    return mappings.get(row).getKey();
                case 2:
                    return mappings.get(row).getValue();
                default:
                    return "?";
            }
        }
    }

    private void enableOrDisableButtons() {
        boolean addEnable = getMessageContextMappingTableModel().getRowCount() < 5;
        boolean removeEnabled = mappingTable.getSelectedRow() >= 0 && getMessageContextMappingTableModel().getRowCount() > 1;
        boolean editEnable = mappingTable.getSelectedRow() >= 0;

        //enable/disable taking into account the permissions that this user has.
        addButton.setEnabled(flags.canCreateSome() && addEnable);
        propertiesButton.setEnabled(flags.canCreateSome() && editEnable);
        removeButton.setEnabled(flags.canDeleteSome() && removeEnabled);
    }

    private void ok() {
        assertion.setMappings(mappings.toArray(new MessageContextMapping[0]));
        ok = true;
        dispose();
    }

    public boolean isConfirmed() {
        return ok;
    }

    public void setData(MessageContextAssertion assertion) {
        this.assertion = assertion;
    }

    public MessageContextAssertion getData(MessageContextAssertion assertion) {
        return assertion;
    }
}
