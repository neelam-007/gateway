/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.external.assertions.idattr.console;

import com.l7tech.common.gui.util.DialogDisplayer;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.gui.util.RunOnChangeListener;
import com.l7tech.console.panels.AssertionPropertiesEditor;
import com.l7tech.console.util.Registry;
import com.l7tech.external.assertions.idattr.IdentityAttributesAssertion;
import static com.l7tech.external.assertions.idattr.IdentityAttributesAssertion.DEFAULT_VAR_PREFIX;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderType;
import com.l7tech.identity.mapping.*;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.variable.VariableMetadata;

import javax.swing.*;
import javax.swing.event.TableModelListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.MessageFormat;
import java.util.*;
import java.util.List;

/**
 * @author alex
 */
public class IdentityAttributesAssertionDialog extends JDialog implements AssertionPropertiesEditor<IdentityAttributesAssertion> {
    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.external.assertions.idattr.console.resources.IdentityAttributesAssertionDialog");

    private JPanel mainPanel;
    private JTable attributeTable;
    private JButton cancelButton;
    private JButton okButton;
    private JButton addButton;
    private JButton editButton;
    private JButton removeButton;
    private JComboBox identityProviderComboBox;
    private JTextField variablePrefixField;

    private final Map<IdentityProviderType, Set<AttributeHeader>> builtinAttributes = Collections.unmodifiableMap(new HashMap<IdentityProviderType, Set<AttributeHeader>>() {
        {
            put(IdentityProviderType.INTERNAL, new HashSet<AttributeHeader>(Arrays.asList(InternalAttributeMapping.getBuiltinAttributes())));
            put(IdentityProviderType.FEDERATED, new HashSet<AttributeHeader>(Arrays.asList(FederatedAttributeMapping.getBuiltinAttributes())));
            put(IdentityProviderType.LDAP, new HashSet<AttributeHeader>(Arrays.asList(LdapAttributeMapping.getBuiltinAttributes())));
        }
    });

    private boolean ok = false;

    private final java.util.List<IdentityMapping> mappings = new ArrayList<IdentityMapping>();

    private final IdentityAttributesAssertion assertion;
    private IdentityProviderConfig previousProvider;
    private final Map<Long, IdentityProviderConfig> configs = new HashMap<Long, IdentityProviderConfig>();
    private final Map<Long, EntityHeader> headers = new HashMap<Long, EntityHeader>();
    private final IdentityMappingTableModel tableModel = new IdentityMappingTableModel();
    private static final String BAD_ATTRIBUTE_MESSAGE = resources.getString("badAttributeMessage");

    public IdentityAttributesAssertionDialog(Dialog owner, IdentityAttributesAssertion assertion) throws HeadlessException {
        super(owner, resources.getString("dialog.title"), true);
        this.assertion = assertion;
        init();
    }

    public IdentityAttributesAssertionDialog(Frame owner, IdentityAttributesAssertion assertion) throws HeadlessException {
        super(owner, resources.getString("dialog.title"), true);
        this.assertion = assertion;
        init();
    }

    private void init() {
        Utilities.setEscKeyStrokeDisposes(this);

        IdentityMapping[] lattrs = assertion.getLookupAttributes();
        if (lattrs == null) lattrs = new IdentityMapping[0];
        mappings.addAll(Arrays.asList(lattrs));
        attributeTable.setModel(tableModel);

        String prefix = assertion.getVariablePrefix();
        if (prefix == null) prefix = DEFAULT_VAR_PREFIX;
        variablePrefixField.setText(prefix);
        variablePrefixField.getDocument().addDocumentListener(new RunOnChangeListener(new Runnable() {
            public void run() {
                enableButtons();
            }
        }));

        EntityHeader[] tempHeaders;
        
        try {
            tempHeaders = Registry.getDefault().getIdentityAdmin().findAllIdentityProviderConfig();
            for (EntityHeader header : tempHeaders) {
                final IdentityProviderConfig config = Registry.getDefault().getIdentityAdmin().findIdentityProviderConfigByID(header.getOid());
                IdentityProviderType type = config.type();
                if (type != IdentityProviderType.INTERNAL) {
                    header.setName(header.getName() + " [" + type.description() + "]");
                }
                this.configs.put(header.getOid(), config);
                this.headers.put(header.getOid(), header);
            }
        } catch (FindException e) {
            throw new RuntimeException("Unable to load identity provider(s)", e);
        }

        long initialOid = assertion.getIdentityProviderOid();
        final IdentityProviderConfig initialConfig = configs.get(initialOid);
        if (initialConfig != null) {
            this.previousProvider = initialConfig;
        } else {
            // Select the first one
            initialOid = tempHeaders[0].getOid();
            this.previousProvider = configs.get(initialOid);
        }

        identityProviderComboBox.setModel(new DefaultComboBoxModel(tempHeaders));
        final EntityHeader header = headers.get(initialOid);
        if (header == null) {
            identityProviderComboBox.setSelectedIndex(0);
        } else {
            identityProviderComboBox.setSelectedItem(header);
        }

        identityProviderComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateConfig();
            }
        });

        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                assertion.setLookupAttributes(mappings.toArray(new IdentityMapping[0]));
                final String text = variablePrefixField.getText();
                assertion.setVariablePrefix(text.equals(DEFAULT_VAR_PREFIX) ? null : text);
                assertion.setIdentityProviderOid(previousProvider.getOid());
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
                final UsersOrGroups uog = UsersOrGroups.USERS;
                if (previousProvider.type() == IdentityProviderType.INTERNAL) {
                    im = new InternalAttributeMapping(ac, uog);
                } else if (previousProvider.type() == IdentityProviderType.LDAP) {
                    im = new LdapAttributeMapping(ac, previousProvider.getOid(), uog);
                } else if (previousProvider.type() == IdentityProviderType.FEDERATED) {
                    im = new FederatedAttributeMapping(ac, previousProvider.getOid(), uog);
                } else {
                    throw new IllegalStateException(MessageFormat.format("Identity Provider #{0} ({1}) is of an unsupported type \"{2}\"", previousProvider.getOid(), previousProvider.getName(), previousProvider.type().description()));
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
                DialogDisplayer.showConfirmDialog(IdentityAttributesAssertionDialog.this,
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

        tableModel.addTableModelListener(new TableModelListener(){
            public void tableChanged(TableModelEvent e) {
                enableButtons();
            }
        });

        attributeTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                enableButtons();
            }
        });

        Utilities.setDoubleClickAction(attributeTable, editButton);

        enableButtons();
        
        add(mainPanel);
    }

    private void updateConfig() {
        EntityHeader which = (EntityHeader) identityProviderComboBox.getSelectedItem();
        if (which == null) throw new IllegalStateException("No provider selected");

        final IdentityProviderConfig newProvider;
        try {
            newProvider = Registry.getDefault().getIdentityAdmin().findIdentityProviderConfigByID(which.getOid());
            if (newProvider == null) {
                DialogDisplayer.showMessageDialog(this, "Identity Provider Deleted", "The selected identity provider has been deleted!", null);
                dispose();
                return;
            }
        } catch (FindException e) {
            throw new RuntimeException(MessageFormat.format("Unable to load Identity Provider #{0} ({1})", which.getOid(), which.getName()), e);
        }

        if (newProvider.getOid() == previousProvider.getOid()) return; // No change

        final FilterResult result = filterUnsupportedAttributes(previousProvider, newProvider);
        synchronized(this) {
            if (result == CANCEL) {
                identityProviderComboBox.setSelectedItem(headers.get(previousProvider.getOid()));
                return;
            }

            if (result instanceof FilterRemove) {
                FilterRemove remove = (FilterRemove) result;
                for (int what : remove.toRemove) {
                    mappings.remove(what);
                }
                tableModel.fireTableDataChanged();
            }

            previousProvider = newProvider;
        }
    }

    private static abstract class FilterResult { }
    private static FilterResult OK = new FilterResult() { };
    private static FilterResult CANCEL = new FilterResult() { };
    private static class FilterRemove extends FilterResult {
        private final List<Integer> toRemove = new ArrayList<Integer>();
    }

    private FilterResult filterUnsupportedAttributes(final IdentityProviderConfig oldProvider,
                                                     final IdentityProviderConfig newProvider)
    {
        final FilterRemove remove = new FilterRemove();
        for (int i = 0; i < mappings.size(); i++) {
            IdentityMapping mapping = mappings.get(i);
            final AttributeHeader header = mapping.getAttributeConfig().getHeader();
            if (oldProvider.type() != newProvider.type()) {
                // Check if the new provider type still supports this attribute
                Set<AttributeHeader> newBuiltins = builtinAttributes.get(newProvider.type());

                if (newBuiltins.contains(header)) continue; // This attribute is supported in this provider

                String name = header.getName();
                if (name == null) name = mapping.getName();
                if (name == null) name = mapping.getCustomAttributeName();
                if (name == null) name = mapping.getAttributeConfig().getName();
                final boolean[] cancel = new boolean[1];
                final int i1 = i;
                DialogDisplayer.showConfirmDialog(
                        this,
                        MessageFormat.format(BAD_ATTRIBUTE_MESSAGE, name, newProvider.getName()),
                        "Unsupported Attribute", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE,
                        new DialogDisplayer.OptionListener() {
                            public void reportResult(int option) {
                                if (option == JOptionPane.OK_OPTION) {
                                    remove.toRemove.add(i1);
                                } else {
                                    cancel[0] = true;
                                }
                            }
                        }
                );
                if (cancel[0]) return CANCEL;
            } else {
                // TODO two providers of the same type can someday have different attribute dictionaries; not today!
            }
        }

        if (remove.toRemove.isEmpty()) {
            return OK;
        } else {
            return remove;
        }
    }

    private void enableButtons() {
        final boolean gotProvider = identityProviderComboBox.getSelectedItem() != null;
        addButton.setEnabled(gotProvider);
        editButton.setEnabled(gotProvider);
        removeButton.setEnabled(gotProvider);
        attributeTable.setEnabled(gotProvider);

        // Nothing is selected
        final boolean sel = gotProvider && attributeTable.getSelectedRow() != -1;
        editButton.setEnabled(sel);
        removeButton.setEnabled(sel);

        final String vp = variablePrefixField.getText();
        okButton.setEnabled(!mappings.isEmpty() && vp != null && vp.trim().length() > 0 && VariableMetadata.isNameValid(vp));
    }

    private boolean edit(IdentityMapping im) {
        UserAttributeMappingDialog dlg = new UserAttributeMappingDialog(this, im, previousProvider, variablePrefixField.getText());
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        dlg.setVisible(true);
        return dlg.isOk();
    }

    public JDialog getDialog() {
        return this;
    }

    public boolean isConfirmed() {
        return ok;
    }

    public void setData(IdentityAttributesAssertion assertion) {
    }

    public IdentityAttributesAssertion getData(IdentityAttributesAssertion assertion) {
        return assertion;
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
