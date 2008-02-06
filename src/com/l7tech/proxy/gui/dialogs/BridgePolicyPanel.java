/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.proxy.gui.dialogs;

import com.l7tech.common.gui.util.DialogDisplayer;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.util.Pair;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Content of SSG property dialog tab for "Bridge Policy".
 */
class BridgePolicyPanel extends JPanel {
    private final Dialog parentDialog;

    private JPanel rootPanel;
    private JCheckBox cbHeaderPassthrough;
    private JCheckBox cbUseSsl;
    private JTable propertiesTable;
    private JButton addButton;
    private JButton removeButton;
    private JButton editButton;

    private Map<String, String> properties = new LinkedHashMap<String, String>();

    public BridgePolicyPanel(final Dialog parent) {
        this.parentDialog = parent;
        setLayout(new BorderLayout());
        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                edit(null, null);
            }
        });

        editButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int row = propertiesTable.getSelectionModel().getMinSelectionIndex();
                if (row < 0) return;

                final Pair<String, String> p = getRow(row);
                if (p == null) return;
                edit(p.left, p.right);
            }
        });

        removeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int row = propertiesTable.getSelectionModel().getMinSelectionIndex();
                if (row < 0) return;
                properties.remove(getRow(row).left);
                tableModel.fireTableDataChanged();
                enableButtons();
            }
        });

        propertiesTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                enableButtons();
            }
        });
        enableButtons();

        propertiesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        propertiesTable.setModel(tableModel);
        add(rootPanel, BorderLayout.CENTER);
    }

    private Pair<String, String> getRow(int rowIndex) {
        Iterator<String> names = properties.keySet().iterator();
        for (int i = 0; i < properties.size() && names.hasNext(); i++) {
            final String name = names.next();
            if (i != rowIndex) continue;
            return new Pair<String, String>(name, properties.get(name));
        }
        return null;
    }

    private final AbstractTableModel tableModel = new AbstractTableModel() {
        public int getRowCount() {
            return properties.size();
        }

        public int getColumnCount() {
            return 2;
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            Pair<String, String> row = getRow(rowIndex);
            if (row == null) return null;
            switch(columnIndex) {
                case 0:  return row.left;
                case 1:  return row.right;
                default: throw new IllegalArgumentException("Invalid column");
            }
        }

        @Override
        public String getColumnName(int column) {
            switch(column) {
                case 0:  return "Name";
                case 1:  return "Value";
                default: throw new IllegalArgumentException("Invalid column");
            }
        }
    };

    private void edit(final String name, final String value) {
        final BridgePolicyPropertyDialog dlg = new BridgePolicyPropertyDialog(parentDialog, name, value);
        dlg.pack();
        Utilities.centerOnParent(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            public void run() {
                if (dlg.isOk()) {
                    properties.remove(name);
                    properties.put(dlg.getName(), dlg.getValue());
                    tableModel.fireTableDataChanged();
                    enableButtons();
                }
            }
        });
    }

    private void enableButtons() {
        int sel = propertiesTable.getSelectionModel().getLeadSelectionIndex();
        editButton.setEnabled(sel >= 0);
        removeButton.setEnabled(sel >= 0);
    }

    boolean isHeaderPassthrough() {
        return cbHeaderPassthrough.isSelected();
    }

    void setHeaderPassthrough(boolean passthrough) {
        cbHeaderPassthrough.setSelected(passthrough);
    }

    boolean isUseSslByDefault() {
        return cbUseSsl.isSelected();
    }

    void setUseSslByDefault(boolean ssl) {
        cbUseSsl.setSelected(ssl);
    }

    Map<String, String> getProperties() {
        return properties;
    }

    void setProperties(Map<String, String> newprops) {
        this.properties = newprops;
    }
}
