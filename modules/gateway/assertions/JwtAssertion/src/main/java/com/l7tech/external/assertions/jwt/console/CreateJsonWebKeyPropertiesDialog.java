package com.l7tech.external.assertions.jwt.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.panels.TargetVariablePanel;
import com.l7tech.external.assertions.jwt.CreateJsonWebKeyAssertion;
import com.l7tech.external.assertions.jwt.JwkKeyInfo;
import com.l7tech.gui.SimpleTableModel;
import com.l7tech.gui.util.TableUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.util.Functions;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import static com.l7tech.util.Functions.propertyTransform;

public class CreateJsonWebKeyPropertiesDialog extends AssertionPropertiesOkCancelSupport<CreateJsonWebKeyAssertion> {
    private JPanel contentPane;
    private JTable keysTable;
    private JButton addButton;
    private JButton deleteButton;
    private JButton editButton;
    private TargetVariablePanel outputVariable;

    private SimpleTableModel<JwkKeyInfo> tableModel;

    public CreateJsonWebKeyPropertiesDialog(final Frame parent, final CreateJsonWebKeyAssertion assertion) {
        super(CreateJsonWebKeyAssertion.class, parent, String.valueOf(assertion.meta().get(AssertionMetadata.PROPERTIES_ACTION_NAME)), true);
        initComponents();
        setData(assertion);
    }

    @Override
    protected void initComponents() {
        super.initComponents();
        getOkButton().setEnabled(false);
        tableModel = TableUtil.configureTable(
                keysTable,
                TableUtil.column("Key ID", 80, 130, 999999, stringProperty("keyId")),
                TableUtil.column("Usage", 40, 80, 80, stringProperty("publicKeyUse"))
        );
        keysTable.setModel(tableModel);
        Utilities.setRowSorter(keysTable, tableModel, new int[]{0,1}, new boolean[]{true, true}, null);

        keysTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        keysTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                final boolean o = keysTable.getSelectedRow() >= 0;
                deleteButton.setEnabled(o);
                editButton.setEnabled(o);
            }
        });

        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final JWKCreateDialog jwkCreateDialog = new JWKCreateDialog(getOwner(), tableModel);
                jwkCreateDialog.pack();
                Utilities.centerOnScreen(jwkCreateDialog);
                jwkCreateDialog.setVisible(true);
                if(jwkCreateDialog.isConfirmed()){
                    tableModel.addRow(jwkCreateDialog.getJwkKeyInfo());
                }
            }
        });

        editButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final int sel = keysTable.getSelectedRow();
                if(sel >= 0){
                    final int index = keysTable.getRowSorter().convertRowIndexToModel(sel);
                    final JwkKeyInfo keyInfo = tableModel.getRowObject(index);
                    final JWKCreateDialog jwkCreateDialog = new JWKCreateDialog(getOwner(), keyInfo, tableModel, isReadOnly());
                    jwkCreateDialog.pack();
                    Utilities.centerOnScreen(jwkCreateDialog);
                    jwkCreateDialog.setVisible(true);
                    if(jwkCreateDialog.isConfirmed()){
                        tableModel.setRowObject(index, jwkCreateDialog.getJwkKeyInfo());
                    }
                }
            }
        });

        deleteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final int sel = keysTable.getSelectedRow();
                if(sel >= 0){
                    final int index = keysTable.getRowSorter().convertRowIndexToModel(sel);
                    tableModel.removeRowAt(index);
                }
            }
        });
        outputVariable.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                getOkButton().setEnabled(!isReadOnly() && outputVariable.isEntryValid());
            }
        });
        Utilities.setDoubleClickAction(keysTable, editButton);
    }

    @Override
    public void setData(CreateJsonWebKeyAssertion assertion) {
        outputVariable.setVariable(assertion.getTargetVariable());
        outputVariable.setAssertion(assertion, getPreviousAssertion());
        tableModel.setRows(assertion.getKeys());
    }

    @Override
    public CreateJsonWebKeyAssertion getData(CreateJsonWebKeyAssertion assertion) throws ValidationException {
        assertion.setKeys(tableModel.getRows());
        assertion.setTargetVariable(outputVariable.getVariable());
        return assertion;
    }

    @Override
    protected JPanel createPropertyPanel() {
        return contentPane;
    }

    private static Functions.Unary<String, JwkKeyInfo> stringProperty(final String propName) {
        return propertyTransform( JwkKeyInfo.class, propName );
    }
}
