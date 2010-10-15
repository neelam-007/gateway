package com.l7tech.console.panels;

import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.uddi.UDDIKeyedReference;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Set;

public class ManageMetaDataDialog extends JDialog {

    public ManageMetaDataDialog(final Window owner, final Set<UDDIKeyedReference> keyedRefs) {
        super(owner, resources.getString("dialog.title"));
        setContentPane(contentPane);
        this.keyedRefs.addAll(keyedRefs);
        initialize();
    }

    public boolean isWasOked() {
        return wasOked;
    }

    private void initialize(){
        if (getOwner() == null)
            Utilities.setAlwaysOnTop(this, true);

        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                wasOked = true;
                ManageMetaDataDialog.this.dispose();
            }
        });

        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final KeyedReferenceDialog keyedRefDlg =
                        new KeyedReferenceDialog(ManageMetaDataDialog.this, new HashSet<UDDIKeyedReference>(keyedRefs));
                keyedRefDlg.pack();
                keyedRefDlg.setModal(true);
                keyedRefDlg.setSize(500, 150);
                DialogDisplayer.display(keyedRefDlg, new Runnable() {
                    @Override
                    public void run() {
                        if (keyedRefDlg.isConfirmed()) {
                            final UDDIKeyedReference newRef = keyedRefDlg.getKeyedReference();
                            addValueToTable(newRef, false, -1);
                        }

                    }
                });
            }
        });

        removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedRow = keyedReferenceTable.getSelectedRow();
                if(selectedRow < 0) return;

                keyedRefs.remove(selectedRow);
                keyedReferenceModel.fireTableDataChanged();
            }
        });

        editButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final int selectedRow = keyedReferenceTable.getSelectedRow();
                if(selectedRow < 0) return;

                final UDDIKeyedReference selectedRef = keyedRefs.get(selectedRow);
                final KeyedReferenceDialog metaDialog =
                        new KeyedReferenceDialog(ManageMetaDataDialog.this,
                                selectedRef, new HashSet<UDDIKeyedReference>(keyedRefs));
                metaDialog.pack();
                metaDialog.setModal(true);
                metaDialog.setSize(600, 150);
                DialogDisplayer.display(metaDialog, new Runnable() {
                    @Override
                    public void run() {
                        if (metaDialog.isConfirmed()) {
                            addValueToTable(metaDialog.getKeyedReference(), true, selectedRow);
                        }
                    }
                });
            }
        });

        initTable();
        Utilities.setDoubleClickAction(keyedReferenceTable, editButton);
        Utilities.setEscKeyStrokeDisposes(this);

        Utilities.centerOnScreen(this);
    }

    public Set<UDDIKeyedReference> getKeyedReferences(){
        return new HashSet<UDDIKeyedReference>(keyedRefs);
    }

    // - PRIVATE

    private void addValueToTable(final UDDIKeyedReference keyedReference,
                                 final boolean isUpdate,
                                 final int rowToUpdate) {
        if (isUpdate) {
            keyedRefs.remove(rowToUpdate);
        }

        if (keyedRefs.contains(keyedReference)) {
            return;
        }

        if (isUpdate) {
            keyedRefs.add(rowToUpdate, keyedReference);
        } else {
            keyedRefs.add(keyedReference);
        }
        keyedReferenceModel.fireTableDataChanged();
    }

    private void initTable(){
        keyedReferenceModel = new AbstractTableModel() {
            @Override
            public int getRowCount() {
                return keyedRefs.size();
            }

            @Override
            public int getColumnCount() {
                return 3;
            }

            @Override
            public String getColumnName(int column) {
                switch (column){
                    case 0:
                        return resources.getString("tmodelkey");
                    case 1:
                        return resources.getString("keyvalue");
                    case 2:
                        return resources.getString("keyname");
                    default:
                        throw new IllegalStateException("Unknown column");//programming error
                }
            }

            @Override
            public Object getValueAt(int rowIndex, int columnIndex) {
                final UDDIKeyedReference keyedReference = keyedRefs.get(rowIndex);
                switch (columnIndex){
                    case 0:
                        return keyedReference.getTModelKey();
                    case 1:
                        return keyedReference.getKeyValue();
                    case 2:
                        return keyedReference.getKeyName();
                    default:
                        throw new IllegalStateException("Unknown column");//programming error
                }
            }

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return false;
            }

            @Override
            public void fireTableDataChanged() {
                super.fireTableDataChanged();
                enableOrDisableTableButtons();
            }
        };

        keyedReferenceTable.setModel(keyedReferenceModel);
        keyedReferenceTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        keyedReferenceTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                enableOrDisableTableButtons();
            }
        });
        keyedReferenceTable.getTableHeader().setReorderingAllowed(false);
    }

    private void enableOrDisableTableButtons() {
        int selectedRow = keyedReferenceTable.getSelectedRow();

        boolean addEnabled = true;
        boolean editEnabled = selectedRow >= 0;
        boolean removeEnabled = selectedRow >= 0;

        addButton.setEnabled(addEnabled);
        editButton.setEnabled(editEnabled);
        removeButton.setEnabled(removeEnabled);
    }

    private JPanel contentPane;
    private JButton addButton;
    private JButton editButton;
    private JButton removeButton;
    private JButton closeButton;
    private JScrollPane scrollPane;
    private JTable keyedReferenceTable;
    private AbstractTableModel keyedReferenceModel;
    private final java.util.List<UDDIKeyedReference> keyedRefs = new ArrayList<UDDIKeyedReference>();
    private boolean wasOked;

    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.panels.ManageMetaDataDialog");
}
