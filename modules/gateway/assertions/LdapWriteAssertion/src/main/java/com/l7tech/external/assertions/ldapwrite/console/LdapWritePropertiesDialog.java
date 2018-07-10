package com.l7tech.external.assertions.ldapwrite.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.panels.TargetVariablePanel;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.external.assertions.ldapwrite.LdapChangetypeEnum;
import com.l7tech.external.assertions.ldapwrite.LdifAttribute;
import com.l7tech.gui.SimpleTableModel;
import com.l7tech.external.assertions.ldapwrite.LdapWriteAssertion;
import com.l7tech.gateway.common.admin.IdentityAdmin;
import com.l7tech.gui.util.TableUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderType;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.util.Functions;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.gui.util.TableUtil.column;


public class LdapWritePropertiesDialog extends AssertionPropertiesOkCancelSupport<LdapWriteAssertion> {

    private static String COLUMN_NAME_ATTRIBUTE_PROP = "attribute.column.name";
    private static String COLUMN_NAME_VALUE_PROP = "value.column.name";
    private static String ERROR_MSG_LDAP_CONNECTOR_EMPTY_PROP = "error.msg.ldap.connector.empty";
    private static String ERROR_MSG_DN_EMPTY_PROP = "error.msg.dn.empty";
    private static String ERROR_MSG_CHANGETYPE_EMPTY_PROP = "error.msg.changetype.empty";

    private static final Logger logger = Logger.getLogger(LdapWritePropertiesDialog.class.getName());

    private JPanel contentPane;
    private JComboBox<IdentityProviderCbItem> ldapConnectorCb;
    private JTextField dnTextBox;
    private JComboBox<LdapChangetypeEnum> changetypeCb;
    private JTable attributeTable;
    private JButton addButton;
    private JButton removeButton;
    private JButton editButton;
    private TargetVariablePanel variablePrefixTextField;
    private JPanel variablePrefixTextPanel;
    private JButton moveUpButton;
    private JButton moveDownButton;

    private SimpleTableModel<LdifAttribute> attributeSimpleTableModel = new SimpleTableModel<>();
    private ArrayList<IdentityProviderCbItem> identityProviderCBItems = new ArrayList<>();
    private ResourceBundle resourceBundle;

    public LdapWritePropertiesDialog(final Window owner,
                                     final LdapWriteAssertion assertion) {

        super(LdapWriteAssertion.class, owner, assertion, true);
        this.add(contentPane);

        initComponents();
        setData(assertion);
    }

    @Override
    public void setData(final LdapWriteAssertion assertion) {

        ldapConnectorCb.setSelectedIndex(-1);
        final Goid id = assertion.getLdapProviderId();

        try {
            // Test to see if the ldapProviderId exists in the gateway. Throw exception if not found.
            Registry.getDefault().getIdentityAdmin().findIdentityProviderConfigByID(id);

            final IdentityProviderCbItem identityProviderCBitem = new IdentityProviderCbItem();
            identityProviderCBitem.id = id;
            ldapConnectorCb.setSelectedItem(identityProviderCBitem);

        } catch (FindException e) {
            logger.log(Level.WARNING, "Could not populate LDAP Provider combobox. The provider no longer exists in the Gateway.");
        }

        final String dn = assertion.getDn();
        if (dn != null) {
            dnTextBox.setText(dn.trim());
        }

        changetypeCb.setSelectedItem(assertion.getChangetype());

        attributeSimpleTableModel.setRows(assertion.getAttributeList());

        variablePrefixTextField.setVariable(assertion.getVariablePrefix());
    }

    @Override
    public LdapWriteAssertion getData(final LdapWriteAssertion assertion) {

        Object selected = ldapConnectorCb.getSelectedItem();
        if (selected == null) {
            String emptyLdapConnectorError = resourceBundle.getString(ERROR_MSG_LDAP_CONNECTOR_EMPTY_PROP);
            throw new ValidationException(emptyLdapConnectorError);
        }
        final IdentityProviderCbItem ci = (IdentityProviderCbItem) selected;
        assertion.setLdapProviderId(ci.id);

        String dn = dnTextBox.getText().trim();
        if (dn.isEmpty()) {
            String dnEmptyError = resourceBundle.getString(ERROR_MSG_DN_EMPTY_PROP);
            throw new ValidationException(dnEmptyError);
        }
        assertion.setDn(dn);

        selected = changetypeCb.getSelectedItem();
        if (selected == null) {
            String changetypeEmptyError = resourceBundle.getString(ERROR_MSG_CHANGETYPE_EMPTY_PROP);
            throw new ValidationException(changetypeEmptyError);
        }
        assertion.setChangetype((LdapChangetypeEnum) selected);

        final List<LdifAttribute> attributeList = attributeSimpleTableModel.getRows();
        assertion.setAttributeList(attributeList);

        if (!variablePrefixTextField.isEntryValid()) {
            throw new ValidationException(variablePrefixTextField.getErrorMessage());
        }

        assertion.setVariablePrefix(variablePrefixTextField.getVariable().trim());

        return assertion;
    }

    @Override
    protected JPanel createPropertyPanel() {
        return contentPane;
    }

    @Override
    protected void initComponents() {

        super.initComponents();

        resourceBundle = ResourceBundle.getBundle( getClass().getName());

        final Object[] writableLdapProviders = getWritableLdapProviders();
        DefaultComboBoxModel defaultComboBoxModel = new DefaultComboBoxModel(writableLdapProviders);
        ldapConnectorCb.setModel(defaultComboBoxModel);

        populateChangetype();

        String attributeColName = resourceBundle.getString(COLUMN_NAME_ATTRIBUTE_PROP);
        String valueColName = resourceBundle.getString(COLUMN_NAME_VALUE_PROP);

        attributeSimpleTableModel = TableUtil.configureTable(attributeTable,
                column(attributeColName, 200, 200, 99999, Functions.propertyTransform(LdifAttribute.class, "key")),
                column(valueColName, 300, 300, 99999, Functions.propertyTransform(LdifAttribute.class, "value"))
        );

        attributeTable.setModel(attributeSimpleTableModel);
        final ListSelectionModel selectionModel = attributeTable.getSelectionModel();

        selectionModel.addListSelectionListener(
                new AttributeTableSelectionHandler());


        moveUpButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final int viewRow = attributeTable.getSelectedRow();
                if (viewRow > 0) {
                    int prevIndex = viewRow - 1;
                    attributeSimpleTableModel.swapRows(prevIndex, viewRow);
                    attributeSimpleTableModel.fireTableDataChanged();
                    attributeTable.setRowSelectionInterval(prevIndex, prevIndex);
                }
            }
        });

        moveDownButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                final int viewRow = attributeTable.getSelectedRow();
                if (viewRow > -1 && viewRow < attributeSimpleTableModel.getRowCount() - 1) {
                    int nextIndex = viewRow + 1;
                    attributeSimpleTableModel.swapRows(viewRow, nextIndex);
                    attributeSimpleTableModel.fireTableDataChanged();
                    attributeTable.setRowSelectionInterval(nextIndex, nextIndex);
                }
            }
        });

        // Add button pressed.
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                final LdifAttribute newAttribute = new LdifAttribute();
                EditorOutput editorOutput = displayLdifStatementEditor(newAttribute);
                if (editorOutput.getOkPressed()) {

                    attributeSimpleTableModel.addRow(editorOutput.getModifiedAttributePair());
                    attributeSimpleTableModel.fireTableDataChanged();
                }
            }
        });


        // Edit button pressed.
        editButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                final int sel = attributeTable.getSelectedRow();
                if (sel == -1) {
                    return;
                }

                final LdifAttribute attributePair = attributeSimpleTableModel.getRowObject(sel);
                EditorOutput editorOutput = displayLdifStatementEditor(attributePair);

                if (editorOutput.getOkPressed()) {
                    attributeSimpleTableModel.setRowObject(sel, editorOutput.getModifiedAttributePair());
                    attributeSimpleTableModel.fireTableDataChanged();
                }
            }
        });

        // Remove button pressed.
        removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                final int sel = attributeTable.getSelectedRow();
                if (sel == -1) return;
                attributeSimpleTableModel.removeRowAt(sel);
                attributeSimpleTableModel.fireTableDataChanged();

                if (sel < attributeSimpleTableModel.getRowCount()) {
                    attributeTable.setRowSelectionInterval(sel, sel);
                }
            }
        });


        variablePrefixTextField = new TargetVariablePanel();
        variablePrefixTextPanel.setLayout(new BorderLayout());
        variablePrefixTextPanel.add(variablePrefixTextField, BorderLayout.CENTER);

        if (attributeTable.getSelectedRow() == -1) {
            editButton.setEnabled(false);
            removeButton.setEnabled(false);
            moveUpButton.setEnabled(false);
            moveDownButton.setEnabled(false);
        }

        Utilities.setDoubleClickAction(attributeTable, editButton);
    }


    private void populateChangetype() {

        changetypeCb.setModel( new DefaultComboBoxModel(LdapChangetypeEnum.values()));
    }


    private Object[] getWritableLdapProviders() {

        final IdentityAdmin identityAdmin = Registry.getDefault().getIdentityAdmin();
        identityProviderCBItems.clear();

        try {
            final EntityHeader[] identityProviderConfigs = identityAdmin.findAllIdentityProviderConfig();
            if (identityProviderConfigs == null || identityProviderConfigs.length < 1) {
                logger.log(Level.WARNING, "Could not populate LDAP Provider combobox. No configuration provided.");
                return new Object[0];
            }
            for (EntityHeader header : identityProviderConfigs) {
                final IdentityProviderConfig cfg = identityAdmin.findIdentityProviderConfigByID(header.getGoid());
                if (cfg.isWritable() && (IdentityProviderType.fromVal(cfg.getTypeVal()) == IdentityProviderType.LDAP)) {
                    IdentityProviderCbItem item = new IdentityProviderCbItem();
                    item.id = header.getGoid();
                    item.name = cfg.getName();
                    identityProviderCBItems.add(item);
                }
            }
        } catch (ObjectModelException e) {
            logger.log(Level.WARNING, "Could not populate LDAP Provider combobox:{0}", e.toString());
        }

        return identityProviderCBItems.toArray();
    }


    private class EditorOutput {
        boolean okPressed = false;
        LdifAttribute modifiedAttributePair;

        public EditorOutput(boolean okPressed, LdifAttribute modifiedAttributePair) {
            this.okPressed = okPressed;
            this.modifiedAttributePair = modifiedAttributePair;
        }

        public boolean getOkPressed() {
            return okPressed;
        }

        public LdifAttribute getModifiedAttributePair() {
            return modifiedAttributePair;
        }
    }


    private EditorOutput displayLdifStatementEditor(LdifAttribute attributePair) {

        final AttributeEditorDialog dlg = new AttributeEditorDialog(TopComponents.getInstance().getTopParent(), attributePair);

        dlg.pack();
        Utilities.centerOnParentWindow(dlg);
        dlg.setVisible(true);

        return new EditorOutput(dlg.isWasOKed(), dlg.getReturnItem());
    }

    private class IdentityProviderCbItem {
        String name;
        Goid id;

        public IdentityProviderCbItem() {
            super();
        }

        @Override
        public String toString() {
            return name;
        }

        @Override
        public boolean equals(Object anObject) {
            if (this == anObject) {
                return true;
            }
            if (anObject == null) {
                return false;
            }

            if (anObject instanceof IdentityProviderCbItem) {
                if (this.id.equals(((IdentityProviderCbItem) anObject).id)) {
                    return true;
                }
            }
            return false;
        }
    }

    private class AttributeTableSelectionHandler implements ListSelectionListener {

        public void valueChanged(ListSelectionEvent e) {

            final ListSelectionModel lsm = (ListSelectionModel) e.getSource();
            if (lsm.isSelectionEmpty()) {
                editButton.setEnabled(false);
                removeButton.setEnabled(false);
                moveUpButton.setEnabled(false);
                moveDownButton.setEnabled(false);

            } else {
                editButton.setEnabled(true);
                removeButton.setEnabled(true);
                moveUpButton.setEnabled(true);
                moveDownButton.setEnabled(true);
            }
        }
    }


}
