/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.panels;

import com.l7tech.common.gui.util.DialogDisplayer;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.util.Registry;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderType;
import com.l7tech.identity.mapping.*;
import com.l7tech.policy.assertion.identity.IdentityAssertion;
import com.l7tech.policy.assertion.identity.SpecificUser;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ResourceBundle;
import java.text.MessageFormat;

/**
 * @author alex
 */
public class IdentityAssertionVariablesDialog extends JDialog {
    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.resources.IdentityAssertionVariablesDialog");
    private JPanel mainPanel;
    private JTable attributeTable;
    private JButton cancelButton;
    private JButton okButton;
    private JButton addButton;
    private JButton editButton;
    private JButton removeButton;

    private boolean ok = false;

    private final java.util.List<IdentityMapping> mappings = new ArrayList<IdentityMapping>();

    private final IdentityAssertion assertion;
    private IdentityProviderConfig config;
    private final IdentityMappingTableModel tableModel = new IdentityMappingTableModel();

    public IdentityAssertionVariablesDialog(Dialog owner, IdentityAssertion assertion) throws HeadlessException {
        super(owner, resources.getString("dialog.title"), true);
        this.assertion = assertion;
        init();
    }

    public IdentityAssertionVariablesDialog(Frame owner, IdentityAssertion assertion) throws HeadlessException {
        super(owner, resources.getString("dialog.title"), true);
        this.assertion = assertion;
        init();
    }

    private void init() {
        Utilities.setEscKeyStrokeDisposes(this);
        
        try {
            config = Registry.getDefault().getIdentityAdmin().findIdentityProviderConfigByID(assertion.getIdentityProviderOid());
        } catch (Exception e) {
            throw new RuntimeException("Unable to load Identity Provider #" + assertion.getIdentityProviderOid());
        }
        if (config == null) throw new RuntimeException("Identity Provider #" + assertion.getIdentityProviderOid() + " no longer exists");

        IdentityMapping[] lattrs = assertion.getLookupAttributes();
        if (lattrs == null) lattrs = new IdentityMapping[0];
        mappings.addAll(Arrays.asList(lattrs));
        attributeTable.setModel(tableModel);

        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                assertion.setLookupAttributes(mappings.toArray(new IdentityMapping[0]));
                ok = true;
                dispose();
            }
        });
        
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                IdentityMapping im;
                AttributeConfig ac = new AttributeConfig(new AttributeHeader());
                final UsersOrGroups uog = assertion instanceof SpecificUser ? UsersOrGroups.USERS : UsersOrGroups.BOTH;
                if (config.type() == IdentityProviderType.INTERNAL) {
                    im = new InternalAttributeMapping(ac, uog);
                } else if (config.type() == IdentityProviderType.LDAP) {
                    im = new LdapAttributeMapping(ac, config.getOid(), uog);
                } else if (config.type() == IdentityProviderType.FEDERATED) {
                    im = new FederatedAttributeMapping(ac, config.getOid(), uog);
                } else {
                    throw new IllegalStateException("Identity Provider #" + config.getOid() + " is of an unsupported type");
                }
                
                if (edit(im)) {
                    mappings.add(im);
                    final int last = mappings.size() - 1;
                    tableModel.fireTableRowsInserted(last, last);
                }
            }
        });

        editButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                final int sel = attributeTable.getSelectedRow();
                if (sel == -1) return;
                if (edit(mappings.get(sel))) tableModel.fireTableRowsUpdated(sel, sel);
            }
        });

        removeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                final int sel = attributeTable.getSelectedRow();
                if (sel == -1) return;
                DialogDisplayer.showConfirmDialog(IdentityAssertionVariablesDialog.this,
                        MessageFormat.format(resources.getString("removeButton.confirmMessage"), mappings.get(sel).toString()), 
                        resources.getString("removeButton.confirmTitle"),
                        JOptionPane.YES_NO_OPTION,
                        new DialogDisplayer.OptionListener() {
                            public void reportResult(int option) {
                                if (option == JOptionPane.YES_OPTION) {
                                    mappings.remove(sel);
                                    tableModel.fireTableRowsDeleted(sel, sel);
                                }
                            }
                        });
            }
        });

        attributeTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                enableButtons();
            }
        });

        enableButtons();
        
        add(mainPanel);
    }

    private void enableButtons() {
        // Nothing is selected
        final boolean sel = attributeTable.getSelectedRow() != -1;
        editButton.setEnabled(sel);
        removeButton.setEnabled(sel);
    }

    private boolean edit(IdentityMapping im) {
        UserAttributeMappingDialog dlg = new UserAttributeMappingDialog(this, im, config);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg);
        return dlg.isOk();
    }

    private class IdentityMappingTableModel extends AbstractTableModel {
        public int getRowCount() {
            return mappings.size();
        }

        public int getColumnCount() {
            return 2;
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            IdentityMapping map = mappings.get(rowIndex);
            switch(columnIndex) {
                case 0:
                    return map.getAttributeConfig().getVariableName();
                case 1:
                    return map.toString();
                default:
                    throw new IllegalArgumentException("No such column " + columnIndex);
            }
        }

        @Override
        public String getColumnName(int column) {
            switch(column) {
                case 0:
                    return resources.getString("attributeTable.col1Name");
                case 1:
                    return resources.getString("attributeTable.col2Name");
                default:
                    throw new IllegalArgumentException("No such column " + column);
            }
        }
    }

    public boolean isOk() {
        return ok;
    }
}
