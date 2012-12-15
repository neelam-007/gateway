package com.l7tech.console.panels;

import com.l7tech.gui.SimpleTableModel;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.TableUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.OkCancelDialog;
import com.l7tech.policy.assertion.MapValueAssertion;
import com.l7tech.util.Functions;
import com.l7tech.util.NameValuePair;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;

public class MapValueAssertionPropertiesDialog extends AssertionPropertiesOkCancelSupport<MapValueAssertion> {
    private JPanel contentPane;
    private JTable mappingsTable;
    private JButton moveUpButton;
    private JButton moveDownButton;
    private JButton addMappingButton;
    private JButton removeMappingButton;
    private JButton editMappingButton;
    private JTextField valueToMapTextField;
    private TargetVariablePanel outputVariableField;

    private SimpleTableModel<NameValuePair> mappingsTableModel;
    
    public MapValueAssertionPropertiesDialog(Window owner, final MapValueAssertion assertion) {
        super(assertion.getClass(), owner, assertion, true);
        initComponents();
        setData(assertion);
    }

    @Override
    public void setData(MapValueAssertion assertion) {
        valueToMapTextField.setText(assertion.getInputExpr());
        outputVariableField.setVariable(assertion.getOutputVar());
        NameValuePair[] mappings = assertion.getMappings();        
        mappingsTableModel.setRows(new ArrayList<NameValuePair>(mappings == null ? Collections.<NameValuePair>emptyList() : Arrays.asList(mappings)));
        outputVariableField.setAssertion(assertion, getPreviousAssertion());
    }

    @Override
    public MapValueAssertion getData(MapValueAssertion assertion) throws ValidationException {
        assertion.setInputExpr(valueToMapTextField.getText());
        assertion.setOutputVar(outputVariableField.getVariable());
        final List<NameValuePair> rows = mappingsTableModel.getRows();
        assertion.setMappings(rows.toArray(new NameValuePair[rows.size()]));
        return assertion;
    }

    @Override
    protected JPanel createPropertyPanel() {
        mappingsTableModel = TableUtil.configureTable(mappingsTable,
                TableUtil.column("Pattern", 100, 250, 99999, Functions.propertyTransform(NameValuePair.class, "key")),
                TableUtil.column("Result", 100, 250, 99999, Functions.propertyTransform(NameValuePair.class, "value")));
        mappingsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        moveUpButton.addActionListener(TableUtil.createMoveUpAction(mappingsTable, mappingsTableModel));
        moveDownButton.addActionListener(TableUtil.createMoveDownAction(mappingsTable, mappingsTableModel));

        addMappingButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {                
                showEditMappingDialog("Add Mapping", new NameValuePair(), new Functions.UnaryVoid<NameValuePair>() {
                    @Override
                    public void call(NameValuePair nameValuePair) {                        
                        mappingsTableModel.addRow(nameValuePair);
                    }
                });
            }
        });
        
        editMappingButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final int rowIndex = mappingsTable.getSelectedRow();
                final NameValuePair mapping = mappingsTableModel.getRowObject(rowIndex);
                if (mapping != null) showEditMappingDialog("Edit Mapping", mapping, new Functions.UnaryVoid<NameValuePair>() {
                    @Override
                    public void call(NameValuePair nameValuePair) {
                        mappingsTableModel.setRowObject(rowIndex, nameValuePair);                        
                    }
                });
            }
        });
        
        removeMappingButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {                
                mappingsTableModel.removeRowAt(mappingsTable.getSelectedRow());
            }
        });

        outputVariableField.setValueWillBeRead(false);
        outputVariableField.setValueWillBeWritten(true);

        return contentPane;
    }

    private void showEditMappingDialog(final String title, NameValuePair initialValue, final Functions.UnaryVoid<NameValuePair> actionIfConfirmed) {
        final NameValuePanel panel = new NameValuePanel(initialValue);
        panel.setNameLabelText("Pattern");
        panel.setValueLabelText("Result");
        final OkCancelDialog<NameValuePair> dlg = new OkCancelDialog<NameValuePair>(this, title, true, panel);
        dlg.pack();
        Utilities.centerOnParentWindow(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            @Override
            public void run() {
                if (dlg.wasOKed()) {
                    NameValuePair value = dlg.getValue();
                    if (value != null)
                        actionIfConfirmed.call(value);
                }
            }
        });
    }
}
