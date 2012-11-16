package com.l7tech.external.assertions.mqnative.console;

import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;
import com.l7tech.util.MutablePair;
import com.l7tech.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

public class MqNativeAdvancedPropertiesPanel extends JPanel {
    final private JDialog owner;
    final private List<String> selectableProperties;

    private JPanel mainPanel;
    private JCheckBox passThroughCheckBox;
    private JButton addButton;
    private JButton editButton;
    private JButton removeButton;
    private JTable advancedPropertiesTable;
    private JLabel customizedPropertiesLabel;
    private AdvancedPropertiesTableModel advancedPropertiesTableModel = new AdvancedPropertiesTableModel();

    public MqNativeAdvancedPropertiesPanel(final JDialog owner, final List<String> selectableProperties) {
        this.owner = owner;
        this.selectableProperties = selectableProperties;

        RunOnChangeListener enableDisableListener = new RunOnChangeListener() {
            @Override
            public void run() {
                enableOrDisableComponents();
            }
        };

        advancedPropertiesTable.setModel( advancedPropertiesTableModel );
        advancedPropertiesTable.getSelectionModel().addListSelectionListener( enableDisableListener );
        advancedPropertiesTable.getTableHeader().setReorderingAllowed( false );

        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                addAdvancedProperty(advancedPropertiesTable, advancedPropertiesTableModel);
            }
        });
        editButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                editAdvancedProperty(advancedPropertiesTable, advancedPropertiesTableModel);
            }
        });
        editButton.setEnabled(false);
        removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                removeAdvancedProperty(advancedPropertiesTable, advancedPropertiesTableModel);
            }
        });
        removeButton.setEnabled(false);

        Utilities.setDoubleClickAction(advancedPropertiesTable, editButton);
    }

    public boolean isPassThrough() {
        return passThroughCheckBox.isSelected();
    }

    public void setPassThrough(boolean isPassThrough) {
        passThroughCheckBox.setSelected(isPassThrough);
    }

    public void setAdvancedPropertiesTableModel(final Map<String,String> properties) {
        advancedPropertiesTableModel.fromMap(properties);
    }

    public Map<String,String> getAdvancedPropertiesTableModelAsMap() {
        return advancedPropertiesTableModel.toMap();
    }

    public void setTitleAndLabels(final String title, final String passThroughCheckBoxText, final String customizedPropertiesLabelText) {
        TitledBorder titleBorder = BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), title);
        titleBorder.setTitleJustification(TitledBorder.LEFT);
        mainPanel.setBorder(titleBorder);
        passThroughCheckBox.setText(passThroughCheckBoxText);
        customizedPropertiesLabel.setText(customizedPropertiesLabelText);
    }

    private void enableOrDisableComponents() {
        final boolean enableAdvancedPropertyContextualControls = advancedPropertiesTable.getSelectedRow() >= 0;
        editButton.setEnabled( enableAdvancedPropertyContextualControls );
        removeButton.setEnabled( enableAdvancedPropertyContextualControls );
    }

    private void addAdvancedProperty(final JTable advancedPropertiesTable, final AdvancedPropertiesTableModel advancedTableModel) {
        final MqNativeAdvancedPropertiesDialog dialog = new MqNativeAdvancedPropertiesDialog(owner, null, selectableProperties, advancedTableModel.toMap());
        dialog.setTitle("Customize");
        dialog.pack();
        Utilities.centerOnParentWindow(dialog);
        DialogDisplayer.display(dialog, new Runnable() {
            @Override
            public void run() {
                if (!dialog.isCanceled()) {
                    updatePropertiesList(advancedPropertiesTable, advancedTableModel, dialog.getTheProperty(), false);
                    dialog.dispose();
                }
            }
        });
    }

    private void editAdvancedProperty(final JTable advancedPropertiesTable, final AdvancedPropertiesTableModel advancedTableModel) {
        int viewRow = advancedPropertiesTable.getSelectedRow();
        if (viewRow < 0) return;

        final String name = (String) advancedTableModel.getValueAt(viewRow, 0);
        final String value = (String) advancedTableModel.getValueAt(viewRow, 1);

        final MqNativeAdvancedPropertiesDialog dialog = new MqNativeAdvancedPropertiesDialog(owner, new MutablePair<String,
                String>(name, value), selectableProperties, advancedTableModel.toMap());
        dialog.setTitle( "Customize" );
        dialog.pack();
        Utilities.centerOnParentWindow( dialog );
        DialogDisplayer.display( dialog, new Runnable() {
            @Override
            public void run() {
                if ( !dialog.isCanceled() ) {
                    updatePropertiesList(advancedPropertiesTable, advancedTableModel, dialog.getTheProperty(), false);
                    dialog.dispose();
                }
            }
        } );
    }

    private void removeAdvancedProperty(JTable advancedPropertiesTable, AdvancedPropertiesTableModel advancedTableModel) {
        int viewRow = advancedPropertiesTable.getSelectedRow();
        if (viewRow < 0) return;

        String name = (String) advancedTableModel.getValueAt(viewRow, 0);
        String value = (String) advancedTableModel.getValueAt(viewRow, 1);
        updatePropertiesList(advancedPropertiesTable, advancedTableModel, new Pair<String, String>(name, value), true);
    }

    private void updatePropertiesList(JTable advancedPropertiesTable, AdvancedPropertiesTableModel advancedTableModel,
                                      final Pair<String, String> selectedProperty, boolean deleted) {
        ArrayList<String> keyset = new ArrayList<String>();
        int currentRow;

        if (deleted) {
            keyset.addAll(advancedTableModel.advancedPropertiesMap.keySet());
            currentRow = keyset.indexOf(selectedProperty.left);
            advancedTableModel.advancedPropertiesMap.remove(selectedProperty.left);
        } else {
            advancedTableModel.advancedPropertiesMap.put(selectedProperty.left, selectedProperty.right);
            keyset.addAll(advancedTableModel.advancedPropertiesMap.keySet());
            currentRow = keyset.indexOf(selectedProperty.left);
        }

        // Refresh the table
        advancedTableModel.fireTableDataChanged();

        // Refresh the selection highlight
        if (currentRow == advancedTableModel.advancedPropertiesMap.size()) currentRow--; // If the previous deleted row was the last row
        if (currentRow >= 0) advancedPropertiesTable.getSelectionModel().setSelectionInterval(currentRow, currentRow);
    }

    /**
     * Inner class AdvancedPropertiesTableModel
     */
    private static class AdvancedPropertiesTableModel extends AbstractTableModel {
        private static final int MAX_TABLE_COLUMN_NUM = 2;
        private Map<String, String> advancedPropertiesMap = new TreeMap<String,String>();

        @Override
        public int getColumnCount() {
            return MAX_TABLE_COLUMN_NUM;
        }

        @Override
        public int getRowCount() {
            return advancedPropertiesMap.size();
        }

        @Override
        public String getColumnName(int col) {
            switch (col) {
                case 0:
                    return "Name";
                case 1:
                    return "Value";
                default:
                    throw new IndexOutOfBoundsException("Out of the maximum column number, " + MAX_TABLE_COLUMN_NUM + ".");
            }
        }

        @Override
        public Object getValueAt(int row, int col) {
            String name = (String) advancedPropertiesMap.keySet().toArray()[row];

            switch (col) {
                case 0:
                    return name;
                case 1:
                    return advancedPropertiesMap.get(name);
                default:
                    throw new IndexOutOfBoundsException("Out of the maximum column number, " + MAX_TABLE_COLUMN_NUM + ".");
            }
        }

        @NotNull
        private Map<String,String> toMap() {
            return new HashMap<String,String>( advancedPropertiesMap );
        }

        private void fromMap( @Nullable final Map<String,String> properties ) {
            advancedPropertiesMap = new TreeMap<String, String>();

            if( properties != null ) {
                advancedPropertiesMap.putAll( properties );
            }
        }
    }
}
