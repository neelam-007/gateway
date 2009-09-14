package com.l7tech.external.assertions.ldapquery.console;

import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.console.panels.AssertionPropertiesEditorSupport;
import com.l7tech.console.util.Registry;
import com.l7tech.external.assertions.ldapquery.LDAPQueryAssertion;
import com.l7tech.external.assertions.ldapquery.QueryAttributeMapping;
import com.l7tech.gateway.common.admin.IdentityAdmin;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderType;
import com.l7tech.objectmodel.EntityHeader;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Dialog to set properties of the LDAPQueryAssertion assertion
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Nov 6, 2007<br/>
 */
public class LDAPQueryPropertiesDialog extends AssertionPropertiesEditorSupport<LDAPQueryAssertion> {
    private JPanel mainPanel;
    private JButton okBut;
    private JButton cancelButton;
    private JComboBox ldapCombo;
    private JTextField searchField;
    private JTable mappingTable;
    private JButton deleteButton;
    private JButton newButton;
    private JButton editButton;
    private JCheckBox cacheLDAPAttributeValuesCheckBox;
    private JSpinner cachePeriodSpinner;
    private JCheckBox failIfNoResultsCheckBox;
    private boolean wasOKed = false;
    private LDAPQueryAssertion assertion;
    private java.util.List<QueryAttributeMapping> localMappings = new ArrayList<QueryAttributeMapping>();
    private final MappingTableModel tableModel = new MappingTableModel();
    private final Logger logger = Logger.getLogger(LDAPQueryPropertiesDialog.class.getName());
    private ArrayList<ComboItem> comboStuff = new ArrayList<ComboItem>();
    private InputValidator validator;

    public LDAPQueryPropertiesDialog(Window owner, LDAPQueryAssertion assertion) throws HeadlessException {
        super(owner, assertion);
        this.assertion = assertion;
        initialize();
    }

    private void initialize() {
        setContentPane(mainPanel);
        mappingTable.setModel(tableModel);

        validator = new InputValidator(this, getTitle());
        validator.attachToButton(okBut, new ActionListener(){
            public void actionPerformed(ActionEvent actionEvent) {
                viewToModel();
                wasOKed = true;
                dispose();
            }
        });
        cancelButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent actionEvent) {
                dispose();
            }
        });

        editButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                final int sel = mappingTable.getSelectedRow();
                if (sel == -1) return;
                if (edit(localMappings.get(sel))) tableModel.fireTableRowsUpdated(sel, sel);
            }
        });

        deleteButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doDeleteMapping();
            }
        });

        newButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                QueryAttributeMapping newitem = new QueryAttributeMapping("", "");

                if (edit(newitem)) {
                    localMappings.add(newitem);
                    final int last = localMappings.size() - 1;
                    tableModel.fireTableRowsInserted(last, last);
                }
            }
        });

        mappingTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                enableButtons();
            }
        });

		 cacheLDAPAttributeValuesCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                enableDisableCache();
            }
        });


        Utilities.equalizeButtonSizes(new JButton[]{okBut, editButton, deleteButton});
        enableButtons();

        ldapCombo.setModel(new DefaultComboBoxModel(populateLdapProviders()));


        validator.disableButtonWhenInvalid(okBut);
        validator.constrainTextFieldToBeNonEmpty("Search Filter", searchField, new InputValidator.ComponentValidationRule(searchField) {
            public String getValidationError() {
                String val = searchField.getText();
                if (val == null || val.trim().length() == 0)
                    return "The search filter cannot be empty";
                else
                    return null;
            }
        });

        validator.addRule(new InputValidator.ComponentValidationRule(ldapCombo) {
            public String getValidationError() {
                if (comboStuff.isEmpty())
                    return "An LDAP Provider must be selected";
                else
                    return null;
            }
        });

        Utilities.setDoubleClickAction(mappingTable, editButton);
        modelToView();
        enableDisableCache();
        validator.validate();
    }

    private void modelToView() {
        // update gui from assertion
        localMappings.clear();
        localMappings.addAll(Arrays.asList(assertion.getQueryMappings()));
        long id = assertion.getLdapProviderOid();
        for (ComboItem i : comboStuff) {
            if (i.oid == id) {
                ldapCombo.setSelectedItem(i);
            }
        }
        searchField.setText(assertion.getSearchFilter());
        cacheLDAPAttributeValuesCheckBox.setSelected(assertion.isEnableCache());
        cachePeriodSpinner.setValue(assertion.getCachePeriod());
        failIfNoResultsCheckBox.setSelected(assertion.isFailIfNoResults());
    }

    private void viewToModel() {
        Object selected = ldapCombo.getSelectedItem();
        if (selected != null) {
            ComboItem ci = (ComboItem)selected;
            assertion.setLdapProviderOid(ci.oid);
        }
        assertion.setQueryMappings(localMappings.toArray(new QueryAttributeMapping[localMappings.size()]));
        assertion.setSearchFilter(searchField.getText());
        assertion.setEnableCache(cacheLDAPAttributeValuesCheckBox.isSelected());
        assertion.setCachePeriod(Long.parseLong(cachePeriodSpinner.getValue().toString()));
        assertion.setFailIfNoResults(failIfNoResultsCheckBox.isSelected());
    }

    private void doDeleteMapping() {
        final int sel = mappingTable.getSelectedRow();
        if (sel == -1) return;
        final QueryAttributeMapping found = localMappings.get(sel);
        DialogDisplayer.showConfirmDialog(this,
                        MessageFormat.format("Are you sure you want to remove the mapping for \"{0}\"", found.getAttributeName()),
                "Confirm Deletion",
                JOptionPane.YES_NO_OPTION,
                new DialogDisplayer.OptionListener() {
                    public void reportResult(int option) {
                        if (option == JOptionPane.YES_OPTION) {
                            localMappings.remove(sel);
                            tableModel.fireTableRowsDeleted(sel, sel);
                        }
                    }
                });
    }

    public class ComboItem {
        String name;
        long oid;
        public String toString() {
            return name;
        }
    }

    private void enableDisableCache() {
        if (cacheLDAPAttributeValuesCheckBox.isSelected()) {
            cachePeriodSpinner.setEnabled(true);
        } else {
            cachePeriodSpinner.setEnabled(false);
        }
    }

    private Object[] populateLdapProviders() {
        IdentityAdmin idadmin = Registry.getDefault().getIdentityAdmin();
        comboStuff.clear();
        try {
            EntityHeader[] allproviders = idadmin.findAllIdentityProviderConfig();
            if (allproviders == null || allproviders.length < 1) {
                // todo, error msg
                okBut.setEnabled(false);
                return new Object[0];
            }
            for (EntityHeader header : allproviders) {
                IdentityProviderConfig cfg = idadmin.findIdentityProviderConfigByID(header.getOid());
                if (IdentityProviderType.fromVal(cfg.getTypeVal()) == IdentityProviderType.LDAP) {
                    ComboItem item = new ComboItem();
                    item.oid = header.getOid();
                    item.name = cfg.getName();
                    comboStuff.add(item);
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "problem reading providers", e);
        }

        if (comboStuff.size() < 1) {
            okBut.setEnabled(false);
        }

        return comboStuff.toArray();
    }

    private void enableButtons() {
        final boolean sel = mappingTable.getSelectedRow() != -1;
        editButton.setEnabled(sel);
        deleteButton.setEnabled(sel);
    }

//    private void ok() {
//        wasOKed = true;
//        dispose();
//    }

    public JDialog getDialog() {
        return this;
    }

    public boolean isConfirmed() {
        return wasOKed;
    }

    public void setData(LDAPQueryAssertion assertion) {
        this.assertion = assertion;
        modelToView();
    }

    public LDAPQueryAssertion getData(LDAPQueryAssertion assertion) {
        Object selected = ldapCombo.getSelectedItem();
        if (selected != null) {
            ComboItem ci = (ComboItem)selected;
            assertion.setLdapProviderOid(ci.oid);
        }
        assertion.setQueryMappings(localMappings.toArray(new QueryAttributeMapping[localMappings.size()]));
        assertion.setSearchFilter(searchField.getText());
        assertion.setEnableCache(cacheLDAPAttributeValuesCheckBox.isSelected());
        assertion.setCachePeriod(Long.parseLong(cachePeriodSpinner.getValue().toString()));
        assertion.setFailIfNoResults(failIfNoResultsCheckBox.isSelected());
        return assertion;
    }

    private boolean edit(QueryAttributeMapping im) {
        AttributeVariableMapDialog dlg = new AttributeVariableMapDialog(this, im);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        dlg.setVisible(true);
        return dlg.isWasOKed();
    }

    private class MappingTableModel extends AbstractTableModel {
        public int getRowCount() {
            return localMappings.size();
        }
                                                                              
        public int getColumnCount() {
            return 2;
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            QueryAttributeMapping map = localMappings.get(rowIndex);
            switch(columnIndex) {
                case 0:
                    return map.getAttributeName();
                case 1:
                    return map.getMatchingContextVariableName();
                default:
                    throw new IllegalArgumentException("No such column " + columnIndex);
            }
        }

        @Override
        public String getColumnName(int column) {
            switch(column) {
                case 0:
                    return "LDAP Attribute";
                case 1:
                    return "Context Variable Name";
                default:
                    throw new IllegalArgumentException("No such column " + column);
            }
        }
    }
}
