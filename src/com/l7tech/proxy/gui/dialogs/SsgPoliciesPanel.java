/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.gui.dialogs;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.proxy.datamodel.Policy;
import com.l7tech.proxy.datamodel.PolicyAttachmentKey;
import com.l7tech.proxy.datamodel.PolicyManager;
import com.l7tech.proxy.gui.Gui;
import com.l7tech.proxy.gui.policy.PolicyTreeCellRenderer;
import com.l7tech.proxy.gui.policy.PolicyTreeModel;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.tree.TreeModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The Service Policies panel for the {@link SsgPropertyDialog}.
 */
class SsgPoliciesPanel extends JPanel {
    private static final Logger log = Logger.getLogger(SsgPoliciesPanel.class.getName());

    private PolicyManager policyCache; // transient policies
    private PolicyAttachmentKey lastSelectedPolicy = null;

    //   View for Service Policies pane
    private JTree policyTree;
    private JTable policyTable;
    private ArrayList displayPolicies;
    private DisplayPolicyTableModel displayPolicyTableModel;
    private JButton buttonFlushPolicies;
    private JButton importButton;
    private JButton exportButton;
    private JButton changeButton;
    private JButton deleteButton;

    public SsgPoliciesPanel() {
        init();
    }

    public void setPolicyCache(PolicyManager policyCache) {
        this.policyCache = policyCache;
        updatePolicyPanel();
    }

    private void init() {
        int y = 0;
        setLayout(new GridBagLayout());
        JPanel pane = this;
        setBorder(BorderFactory.createEmptyBorder());

        pane.add(new JLabel("<HTML><h4>Service Policies Being Cached by Bridge</h4></HTML>"),
                 new GridBagConstraints(0, y, 1, 1, 1.0, 0.0,
                                        GridBagConstraints.NORTHWEST,
                                        GridBagConstraints.HORIZONTAL,
                                        new Insets(14, 6, 0, 6), 0, 0));

        buttonFlushPolicies = new JButton("Clear Policy Cache");
        buttonFlushPolicies.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                policyCache.clearPolicies();
                updatePolicyPanel();
            }
        });
        pane.add(Box.createGlue(),
                 new GridBagConstraints(1, y, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.EAST,
                                        GridBagConstraints.HORIZONTAL,
                                        new Insets(0, 0, 0, 6), 0, 0));
        pane.add(buttonFlushPolicies,
                 new GridBagConstraints(2, y++, 2, 1, 0.0, 0.0,
                                        GridBagConstraints.EAST,
                                        GridBagConstraints.NONE,
                                        new Insets(14, 6, 0, 6), 0, 0));

        pane.add(new JLabel("Web Services with Cached Policies:"),
                 new GridBagConstraints(0, y, 2, 1, 0.0, 0.0,
                                        GridBagConstraints.WEST,
                                        GridBagConstraints.HORIZONTAL,
                                        new Insets(6, 6, 0, 6), 0, 0));
        pane.add(Box.createGlue(),
                 new GridBagConstraints(2, y++, 1, 1, 1.0, 0.0,
                                        GridBagConstraints.WEST,
                                        GridBagConstraints.HORIZONTAL,
                                        new Insets(6, 6, 0, 6), 0, 0));

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
        policyTable.getColumnModel().getColumn(3).setHeaderValue("Lock");
        policyTable.getColumnModel().getColumn(3).setCellRenderer(policyTable.getDefaultRenderer(Boolean.class));
        policyTable.getColumnModel().getColumn(3).setCellEditor(policyTable.getDefaultEditor(Boolean.class));
        policyTable.getColumnModel().getColumn(3).setMaxWidth(35);
        policyTable.getColumnModel().getColumn(3).setMinWidth(35);
        policyTable.getTableHeader().setReorderingAllowed(false);
        JScrollPane policyTableSp = new JScrollPane(policyTable);
        policyTableSp.setPreferredSize(new Dimension(120, 120));
        pane.add(policyTableSp,
                 new GridBagConstraints(0, y, 3, 1, 100.0, 1.0,
                                        GridBagConstraints.CENTER,
                                        GridBagConstraints.BOTH,
                                        new Insets(0, 6, 3, 3), 0, 0));
        JPanel policyButtons = new JPanel();
        policyButtons.setLayout(new BoxLayout(policyButtons, BoxLayout.Y_AXIS));
        policyButtons.add(getChangeButton());
        policyButtons.add(getExportButton());
        policyButtons.add(getDeleteButton());
        policyButtons.add(Box.createGlue());
        policyButtons.add(getImportButton());
        Utilities.equalizeButtonSizes(new AbstractButton[] { getImportButton(),
                                                             getExportButton(),
                                                             getChangeButton(),
                                                             getDeleteButton() });
        pane.add(policyButtons,
                 new GridBagConstraints(3, y++, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.NORTHWEST,
                                        GridBagConstraints.VERTICAL,
                                        new Insets(0, 0, 3, 6), 0, 0));

        pane.add(new JLabel("Associated Policy:"),
                 new GridBagConstraints(0, y++, GridBagConstraints.REMAINDER, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER,
                                        GridBagConstraints.BOTH,
                                        new Insets(4, 6, 0, 6), 0, 0));

        policyTree = new JTree((TreeModel)null);
        policyTree.setCellRenderer(new PolicyTreeCellRenderer());
        JScrollPane policyTreeSp = new JScrollPane(policyTree);
        policyTreeSp.setPreferredSize(new Dimension(120, 120));
        pane.add(policyTreeSp,
                 new GridBagConstraints(0, y++, GridBagConstraints.REMAINDER, 1, 100.0, 100.0,
                                        GridBagConstraints.CENTER,
                                        GridBagConstraints.BOTH,
                                        new Insets(2, 6, 6, 6), 3, 3));

        policyTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                displaySelectedPolicy();
            }
        });

    }

    public JButton getImportButton() {
        if (importButton == null) {
            importButton = new JButton("Import");
            importButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    JFileChooser fc = Utilities.createJFileChooser();
                    fc.setFileFilter(createPolicyFileFilter());
                    fc.setDialogType(JFileChooser.OPEN_DIALOG);

                    if (JFileChooser.APPROVE_OPTION == fc.showOpenDialog(getRootPane())) {
                        File got = fc.getSelectedFile();
                        FileInputStream policyIs = null;
                        try {
                            policyIs = new FileInputStream(got);
                            Assertion rootAssertion = WspReader.parse(policyIs);
                            policyIs.close();
                            policyIs = null;

                            // TODO filter out assertions that aren't implemented by the SSB
                            Policy newPolicy = new Policy(rootAssertion, null);
                            newPolicy.setPersistent(true);
                            PolicyAttachmentKeyDialog pakDlg = new PolicyAttachmentKeyDialog(Gui.getInstance().getFrame(),
                                                                                             "Import Policy: Policy Attachment Key",
                                                                                             true);
                            pakDlg.setPolicyAttachmentKey(getSelectedPolicy());
                            Utilities.centerOnScreen(pakDlg);
                            pakDlg.show();
                            PolicyAttachmentKey pak = pakDlg.getPolicyAttachmentKey();
                            if (pak != null) {
                                policyCache.setPolicy(pak, newPolicy);
                                updatePolicyPanel();
                            }
                        } catch (NullPointerException nfe) {
                        } catch (IOException e) {
                            log.log(Level.WARNING, "Error importing policy", e);
                            JOptionPane.showMessageDialog(getRootPane(),
                              "Unable to import the specified file: " + e.getMessage(),
                              "Unable to read file",
                              JOptionPane.ERROR_MESSAGE);
                        } finally {
                            if (policyIs != null) try { policyIs.close(); } catch (IOException e) {}
                        }
                    }
                }
            });
        }
        return importButton;
    }

    public JButton getExportButton() {
        if (exportButton == null) {
            exportButton = new JButton("Export");
            exportButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    PolicyAttachmentKey pak = getSelectedPolicy();
                    if (pak == null) return;
                    Policy policy = policyCache.getPolicy(pak);
                    if (policy == null) return;

                    JFileChooser fc = Utilities.createJFileChooser();
                    fc.setFileFilter(createPolicyFileFilter());
                    fc.setDialogType(JFileChooser.SAVE_DIALOG);

                    if (JFileChooser.APPROVE_OPTION == fc.showSaveDialog(getRootPane())) {
                        File got = fc.getSelectedFile();
                        FileOutputStream os = null;
                        try {
                            os = new FileOutputStream(got);
                            WspWriter.writePolicy(policy.getAssertion(), os);
                            os.close();
                            os = null;
                        } catch (NullPointerException nfe) {
                        } catch (IOException e) {
                            log.log(Level.WARNING, "Error exporting policy", e);
                            JOptionPane.showMessageDialog(getRootPane(),
                              "Unable to export to the specified file: " + e.getMessage(),
                              "Unable to export file",
                              JOptionPane.ERROR_MESSAGE);
                        } finally {
                            if (os != null) try { os.close(); } catch (IOException e) { }
                        }
                    }
                }
            });
        }
        return exportButton;
    }

    private FileFilter createPolicyFileFilter() {
        FileFilter filter = new FileFilter() {
            public boolean accept(File f) {
                if (f.isDirectory())
                    return true;
                String name = f.getName();
                int dot = name.lastIndexOf('.');
                if (dot < 0)
                    return false;
                String ext = name.substring(dot);
                return ext.equalsIgnoreCase(".xml");
            }

            public String getDescription() {
                return "Policy document (*.xml)";
            }
        };
        return filter;
    }

    public JButton getChangeButton() {
        if (changeButton == null) {
            changeButton = new JButton("Edit");
            changeButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    PolicyAttachmentKey pak = getSelectedPolicy();
                    if (pak == null) return;
                    Policy policy = policyCache.getPolicy(pak);
                    if (policy == null) return;
                    PolicyAttachmentKeyDialog pakDlg = new PolicyAttachmentKeyDialog(Gui.getInstance().getFrame(),
                                                                                     "Edit Policy Attachment Key",
                                                                                     true);
                    pakDlg.setPolicyAttachmentKey(pak);
                    Utilities.centerOnScreen(pakDlg);
                    pakDlg.show();
                    PolicyAttachmentKey newPak = pakDlg.getPolicyAttachmentKey();
                    if (newPak == null || newPak.equals(pak)) return;
                    policyCache.flushPolicy(pak);
                    policyCache.setPolicy(newPak, policy);
                    lastSelectedPolicy = newPak;
                    updatePolicyPanel();
                }
            });
        }
        return changeButton;
    }

    public JButton getDeleteButton() {
        if (deleteButton == null) {
            deleteButton = new JButton("Delete");
            deleteButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    PolicyAttachmentKey pak = getSelectedPolicy();
                    if (pak == null) return;
                    policyCache.flushPolicy(pak);
                    updatePolicyPanel();
                }
            });
        }
        return deleteButton;
    }

    private class DisplayPolicyTableModel extends AbstractTableModel {
        public int getRowCount() {
            return displayPolicies.size();
        }

        public int getColumnCount() {
            return 4;
        }

        public boolean isCellEditable(int rowIndex, int columnIndex) {
            switch (columnIndex) {
                case 3:
                    return true;
            }
            return false;
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            switch (columnIndex) {
                case 0:
                    return ((PolicyAttachmentKey)displayPolicies.get(rowIndex)).getUri();
                case 1:
                    return ((PolicyAttachmentKey)displayPolicies.get(rowIndex)).getSoapAction();
                case 2:
                    return ((PolicyAttachmentKey)displayPolicies.get(rowIndex)).getProxyUri();
                case 3:
                    PolicyAttachmentKey pak = ((PolicyAttachmentKey)displayPolicies.get(rowIndex));
                    Policy p = policyCache.getPolicy(pak);
                    return p == null ? null : Boolean.valueOf(p.isPersistent());
            }
            log.log(Level.WARNING, "SsgPropertyDialog: policyTable: invalid columnIndex: " + columnIndex);
            return null;
        }

        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            switch (columnIndex) {
                case 3:
                    Boolean p = (Boolean)aValue;
                    if (p != null) {
                        PolicyAttachmentKey pak = ((PolicyAttachmentKey)displayPolicies.get(rowIndex));
                        Policy policy = policyCache.getPolicy(pak);
                        if (policy != null) {
                            policy.setPersistent(p.booleanValue());
                            if (!policy.isPersistent())
                                policyCache.flushPolicy(pak); // clear any shadowed value
                            policyCache.setPolicy(pak, policy);
                            updatePolicyPanel();
                        }
                    }
                    break;
            }
        }
    }

    private PolicyAttachmentKey getSelectedPolicy() {
        int row = policyTable.getSelectedRow();
        if (row >= 0 && row < displayPolicies.size())
            return (PolicyAttachmentKey)displayPolicies.get(row);
        return null;
    }

    private void displaySelectedPolicy() {
        // do this?    if (e.getValueIsAdjusting()) return;
        Policy policy = null;
        int row = policyTable.getSelectedRow();
        if (row >= 0 && row < displayPolicies.size()) {
            lastSelectedPolicy = (PolicyAttachmentKey)displayPolicies.get(row);
            if (lastSelectedPolicy != null)
                policy = policyCache.getPolicy(lastSelectedPolicy);
        }
        policyTree.setModel((policy == null || policy.getClientAssertion() == null) ? null : new PolicyTreeModel(policy.getClientAssertion()));
        int erow = 0;
        while (erow < policyTree.getRowCount()) {
            policyTree.expandRow(erow++);
        }
        getChangeButton().setEnabled(policy != null);
        getExportButton().setEnabled(policy != null);
        getDeleteButton().setEnabled(policy != null && !policy.isPersistent());
    }

    /** Update the policy display panel with information from the Ssg bean. */
    public void updatePolicyPanel() {
        PolicyAttachmentKey lastPak = lastSelectedPolicy;
        displayPolicies.clear();
        displayPolicies = new ArrayList(policyCache.getPolicyAttachmentKeys());
        displayPolicyTableModel.fireTableDataChanged();
        if (lastPak != null) {
            for (int i = 0; i < displayPolicies.size(); i++) {
                PolicyAttachmentKey pak = (PolicyAttachmentKey)displayPolicies.get(i);
                if (lastPak == pak)
                    policyTable.getSelectionModel().setSelectionInterval(i, i);
            }
        }
        displaySelectedPolicy();
    }
}
