/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.gui.dialogs;

import com.l7tech.proxy.datamodel.Policy;
import com.l7tech.proxy.datamodel.PolicyAttachmentKey;
import com.l7tech.proxy.datamodel.PolicyManager;
import com.l7tech.proxy.gui.policy.PolicyTreeCellRenderer;
import com.l7tech.proxy.gui.policy.PolicyTreeModel;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.tree.TreeModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The Service Policies panel for the {@link SsgPropertyDialog}.
 */
class SsgPoliciesPanel extends JPanel {
    private static final Logger log = Logger.getLogger(SsgPoliciesPanel.class.getName());

    PolicyManager policyManager;

    //   View for Service Policies pane
    JTree policyTree;
    JTable policyTable;
    ArrayList displayPolicies;
    DisplayPolicyTableModel displayPolicyTableModel;
    JButton buttonFlushPolicies;
    boolean policyFlushRequested = false;

    public SsgPoliciesPanel() {
        init();
    }

    void setPolicyManager(PolicyManager pm) {
        this.policyManager = pm;
    }

    private void init() {
        int y = 0;
        setLayout(new GridBagLayout());
        JPanel pane = this;
        setBorder(BorderFactory.createEmptyBorder());

        pane.add(new JLabel("<HTML><h4>Service Policies Being Cached by Bridge</h4></HTML>"),
                 new GridBagConstraints(0, y++, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.NORTHWEST,
                                        GridBagConstraints.BOTH,
                                        new Insets(14, 6, 6, 6), 3, 3));

        pane.add(new JLabel("Web Services with Cached Policies:"),
                 new GridBagConstraints(0, y++, 2, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER,
                                        GridBagConstraints.BOTH,
                                        new Insets(6, 6, 0, 6), 3, 3));

        buttonFlushPolicies = new JButton("Clear Policy Cache");
        buttonFlushPolicies.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                policyFlushRequested = true;
                updatePolicyPanel();
            }
        });
        pane.add(buttonFlushPolicies,
                 new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.EAST,
                                        GridBagConstraints.NONE,
                                        new Insets(14, 6, 0, 6), 0, 0));

        displayPolicies = new ArrayList();
        displayPolicyTableModel = new DisplayPolicyTableModel();
        policyTable = new JTable(displayPolicyTableModel);
        policyTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        policyTable.setCellSelectionEnabled(false);
        policyTable.setRowSelectionAllowed(true);
        policyTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        policyTable.setAutoCreateColumnsFromModel(true);
        policyTable.getColumnModel().getColumn(0).setHeaderValue("Body Namespace");
        policyTable.getColumnModel().getColumn(1).setHeaderValue("SOAPAction");
        policyTable.getColumnModel().getColumn(2).setHeaderValue("Proxy URI");
        policyTable.getTableHeader().setReorderingAllowed(false);
        JScrollPane policyTableSp = new JScrollPane(policyTable);
        policyTableSp.setPreferredSize(new Dimension(120, 120));
        pane.add(policyTableSp,
                 new GridBagConstraints(0, y++, 2, 1, 1.0, 1.0,
                                        GridBagConstraints.CENTER,
                                        GridBagConstraints.BOTH,
                                        new Insets(0, 6, 3, 6), 0, 0));

        pane.add(new JLabel("Associated Policy:"),
                 new GridBagConstraints(0, y++, 2, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER,
                                        GridBagConstraints.BOTH,
                                        new Insets(4, 6, 0, 6), 0, 0));

        policyTree = new JTree((TreeModel)null);
        policyTree.setCellRenderer(new PolicyTreeCellRenderer());
        JScrollPane policyTreeSp = new JScrollPane(policyTree);
        policyTreeSp.setPreferredSize(new Dimension(120, 120));
        pane.add(policyTreeSp,
                 new GridBagConstraints(0, y++, 2, 1, 100.0, 100.0,
                                        GridBagConstraints.CENTER,
                                        GridBagConstraints.BOTH,
                                        new Insets(2, 6, 6, 6), 3, 3));

        policyTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                displaySelectedPolicy();
            }
        });
    }

    private class DisplayPolicyTableModel extends AbstractTableModel {
        public int getRowCount() {
            return displayPolicies.size();
        }

        public int getColumnCount() {
            return 3;
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            switch (columnIndex) {
                case 0:
                    return ((PolicyAttachmentKey)displayPolicies.get(rowIndex)).getUri();
                case 1:
                    return ((PolicyAttachmentKey)displayPolicies.get(rowIndex)).getSoapAction();
                case 2:
                    return ((PolicyAttachmentKey)displayPolicies.get(rowIndex)).getProxyUri();
            }
            log.log(Level.WARNING, "SsgPropertyDialog: policyTable: invalid columnIndex: " + columnIndex);
            return null;
        }
    }

    private void displaySelectedPolicy() {
        // do this?    if (e.getValueIsAdjusting()) return;
        Policy policy = null;
        int row = policyTable.getSelectedRow();
        if (row >= 0 && row < displayPolicies.size())
            try {
                policy = policyManager.getPolicy((PolicyAttachmentKey)displayPolicies.get(row));
            } catch (IOException e) {
                log.log(Level.WARNING, "Unable to read policy: " + e.getMessage(), e); // TODO this should be an error dialog probably
                policy = null;
            }
        policyTree.setModel((policy == null || policy.getClientAssertion() == null) ? null : new PolicyTreeModel(policy.getClientAssertion()));
        int erow = 0;
        while (erow < policyTree.getRowCount()) {
            policyTree.expandRow(erow++);
        }
    }

    /** Update the policy display panel with information from the Ssg bean. */
    void updatePolicyPanel() {
        displayPolicies.clear();
        if (!policyFlushRequested)
            displayPolicies = new ArrayList(policyManager.getPolicyAttachmentKeys());
        displayPolicyTableModel.fireTableDataChanged();
        displaySelectedPolicy();
    }
}
