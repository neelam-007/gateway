package com.l7tech.external.assertions.ldapquery.console;

import com.l7tech.console.panels.AssertionPropertiesEditorSupport;
import com.l7tech.console.util.Registry;
import com.l7tech.external.assertions.ldapquery.LDAPQueryAssertion;
import com.l7tech.external.assertions.ldapquery.QueryAttributeMapping;
import com.l7tech.gateway.common.admin.IdentityAdmin;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderType;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.ObjectModelException;

import javax.swing.*;
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
    private JSpinner cacheSizeSpinner;
    private JSpinner cachePeriodSpinner;
    private JCheckBox protectAgainstLDAPInjectionCheckBox;
    private JCheckBox allowMultipleSearchResultsCheckBox;
    private JSpinner maximumResultsSpinner;
    private JCheckBox failIfNoResultsCheckBox;
    private JCheckBox failIfTooManyResultsCheckBox;
    private boolean wasOKed = false;
    private LDAPQueryAssertion assertion;
    private java.util.List<QueryAttributeMapping> localMappings = new ArrayList<QueryAttributeMapping>();
    private final MappingTableModel tableModel = new MappingTableModel();
    private final Logger logger = Logger.getLogger(LDAPQueryPropertiesDialog.class.getName());
    private ArrayList<ComboItem> comboStuff = new ArrayList<ComboItem>();

    public LDAPQueryPropertiesDialog(Window owner, LDAPQueryAssertion assertion) throws HeadlessException {
        super(owner, assertion);
        this.assertion = assertion;
        initialize();
    }

    private void initialize() {
        setContentPane(mainPanel);
        mappingTable.setModel(tableModel);
        Utilities.setEscKeyStrokeDisposes( this );

        final InputValidator validator = new InputValidator( this, getTitle() );
        validator.attachToButton(okBut, new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                viewToModel();
                wasOKed = true;
                dispose();
            }
        });
        cancelButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                dispose();
            }
        });

        editButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final int sel = mappingTable.getSelectedRow();
                if (sel == -1) return;
                if (edit(localMappings.get(sel))) tableModel.fireTableRowsUpdated(sel, sel);
            }
        });

        deleteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doDeleteMapping();
            }
        });

        newButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final QueryAttributeMapping attributeMapping = new QueryAttributeMapping("", "");
                if (edit(attributeMapping)) {
                    localMappings.add(attributeMapping);
                    final int last = localMappings.size() - 1;
                    tableModel.fireTableRowsInserted(last, last);
                }
            }
        });

        final RunOnChangeListener enableDisableListener = new RunOnChangeListener(){
            @Override
            protected void run() {
                enableDisableComponents();
            }
        };

        mappingTable.getSelectionModel().addListSelectionListener(enableDisableListener);
        cacheLDAPAttributeValuesCheckBox.addActionListener(enableDisableListener);
        allowMultipleSearchResultsCheckBox.addActionListener(enableDisableListener);

        Utilities.equalizeButtonSizes(new JButton[]{okBut, editButton, deleteButton});

        ldapCombo.setModel(new DefaultComboBoxModel(populateLdapProviders()));
        cacheSizeSpinner.setModel(new SpinnerNumberModel(assertion.getCacheSize(), 0, 100000, 1));
        validator.addRule(new InputValidator.NumberSpinnerValidationRule(cacheSizeSpinner, "Cache size"));
        cachePeriodSpinner.setModel(new SpinnerNumberModel((int)assertion.getCachePeriod(), 0, null, 1));
        validator.addRule(new InputValidator.NumberSpinnerValidationRule(cachePeriodSpinner, "Cache maximum age"));
        maximumResultsSpinner.setModel( new SpinnerNumberModel(0, 0, null, 1) );
        validator.addRule(new InputValidator.NumberSpinnerValidationRule(maximumResultsSpinner, "Maximum results"));
        maximumResultsSpinner.getEditor().setPreferredSize( cachePeriodSpinner.getEditor().getPreferredSize() );

        validator.constrainTextFieldToBeNonEmpty("Search Filter", searchField, new InputValidator.ComponentValidationRule(searchField) {
            @Override
            public String getValidationError() {
                String val = searchField.getText();
                if (val == null || val.trim().length() == 0)
                    return "The search filter cannot be empty";
                else
                    return null;
            }
        });

        validator.addRule(new InputValidator.ComponentValidationRule(ldapCombo) {
            @Override
            public String getValidationError() {
                if (comboStuff.isEmpty())
                    return "An LDAP Provider must be selected";
                else
                    return null;
            }
        });

        Utilities.setDoubleClickAction(mappingTable, editButton);
        modelToView();
        enableDisableComponents();
        validator.validate();
    }

    private void modelToView() {
        // update gui from assertion
        localMappings.clear();
        localMappings.addAll(Arrays.asList(assertion.getQueryMappings()));
        Goid id = assertion.getLdapProviderOid();
        for (ComboItem i : comboStuff) {
            if (i.oid.equals(id)) {
                ldapCombo.setSelectedItem(i);
            }
        }
        searchField.setText(assertion.getSearchFilter());
        searchField.setCaretPosition( 0 );
        protectAgainstLDAPInjectionCheckBox.setSelected(assertion.isSearchFilterInjectionProtected());
        cacheLDAPAttributeValuesCheckBox.setSelected(assertion.isEnableCache());
        cacheSizeSpinner.setValue(assertion.getCacheSize());
        cachePeriodSpinner.setValue(assertion.getCachePeriod());
        failIfNoResultsCheckBox.setSelected(assertion.isFailIfNoResults());
        failIfTooManyResultsCheckBox.setSelected(assertion.isFailIfTooManyResults());
        allowMultipleSearchResultsCheckBox.setSelected(assertion.isAllowMultipleResults());
        maximumResultsSpinner.setValue(assertion.getMaximumResults());
    }

    private void viewToModel() {
        getData(assertion);
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
                    @Override
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
        Goid oid;
        @Override
        public String toString() {
            return name;
        }
    }

    private void enableDisableComponents() {
        final boolean attributeSelected = mappingTable.getSelectedRow() != -1;
        editButton.setEnabled(attributeSelected);
        deleteButton.setEnabled(!isReadOnly() && attributeSelected);

        final boolean multipleResultsEnabled = allowMultipleSearchResultsCheckBox.isSelected();
        maximumResultsSpinner.setEnabled( multipleResultsEnabled );

        final boolean cacheEnabled = cacheLDAPAttributeValuesCheckBox.isSelected();
        cacheSizeSpinner.setEnabled( cacheEnabled );
        cachePeriodSpinner.setEnabled( cacheEnabled );
    }

    private Object[] populateLdapProviders() {
        IdentityAdmin identityAdmin = Registry.getDefault().getIdentityAdmin();
        comboStuff.clear();
        try {
            EntityHeader[] identityProviderConfigs = identityAdmin.findAllIdentityProviderConfig();
            if (identityProviderConfigs == null || identityProviderConfigs.length < 1) {
                // todo, error msg
                okBut.setEnabled(false);
                return new Object[0];
            }
            for (EntityHeader header : identityProviderConfigs) {
                IdentityProviderConfig cfg = identityAdmin.findIdentityProviderConfigByID(header.getGoid());
                if (IdentityProviderType.fromVal(cfg.getTypeVal()) == IdentityProviderType.LDAP) {
                    ComboItem item = new ComboItem();
                    item.oid = header.getGoid();
                    item.name = cfg.getName();
                    comboStuff.add(item);
                }
            }
        } catch (ObjectModelException e) {
            logger.log(Level.SEVERE, "problem reading providers", e);
        }

        if (comboStuff.size() < 1) {
            okBut.setEnabled(false);
        }

        return comboStuff.toArray();
    }

    @Override
    public JDialog getDialog() {
        return this;
    }

    @Override
    public boolean isConfirmed() {
        return wasOKed;
    }

    @Override
    public void setData(LDAPQueryAssertion assertion) {
        this.assertion = assertion;
        modelToView();
    }

    @Override
    public LDAPQueryAssertion getData(LDAPQueryAssertion assertion) {
        Object selected = ldapCombo.getSelectedItem();
        if (selected != null) {
            ComboItem ci = (ComboItem)selected;
            assertion.setLdapProviderOid(ci.oid);
        }
        assertion.setQueryMappings(localMappings.toArray(new QueryAttributeMapping[localMappings.size()]));
        assertion.setSearchFilter(searchField.getText());
        assertion.setSearchFilterInjectionProtected(protectAgainstLDAPInjectionCheckBox.isSelected());
        assertion.setEnableCache(cacheLDAPAttributeValuesCheckBox.isSelected());
        assertion.setCacheSize(((Number)cacheSizeSpinner.getValue()).intValue());
        assertion.setCachePeriod(((Number)cachePeriodSpinner.getValue()).longValue());
        assertion.setFailIfNoResults(failIfNoResultsCheckBox.isSelected());
        assertion.setFailIfTooManyResults(failIfTooManyResultsCheckBox.isSelected());
        assertion.setAllowMultipleResults(allowMultipleSearchResultsCheckBox.isSelected());
        if ( allowMultipleSearchResultsCheckBox.isSelected() ) {
            assertion.setMaximumResults( (Integer) maximumResultsSpinner.getValue() );
        } else {
            assertion.setMaximumResults( 0 );   
        }
        return assertion;
    }

    private boolean edit(QueryAttributeMapping im) {
        AttributeVariableMapDialog dlg = new AttributeVariableMapDialog(this, im, assertion, getPreviousAssertion());
        dlg.pack();
        Utilities.centerOnParentWindow(dlg);
        dlg.setVisible(true);
        return dlg.isWasOKed();
    }

    private class MappingTableModel extends AbstractTableModel {
        @Override
        public int getRowCount() {
            return localMappings.size();
        }
                                                                              
        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
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
