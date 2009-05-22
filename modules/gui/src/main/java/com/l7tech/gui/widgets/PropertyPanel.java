package com.l7tech.gui.widgets;

import com.l7tech.util.Pair;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DialogDisplayer;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.*;
import java.util.*;

/**
 * GUI Widget for property editing
 */
public class PropertyPanel extends JPanel {

    //- PUBLIC

    public PropertyPanel() {
        setLayout(new BorderLayout());

        binding.addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                edit(null, null);
            }
        });

        binding.editButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int row = binding.propertiesTable.getSelectionModel().getMinSelectionIndex();
                if (row < 0) return;

                final Pair<String, String> p = getRow(row);
                if (p == null) return;
                edit(p.left, p.right);
            }
        });

        binding.removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int row = binding.propertiesTable.getSelectionModel().getMinSelectionIndex();
                if (row < 0) return;
                properties.remove(getRow(row).left);
                tableModel.fireTableDataChanged();
                enableButtons();
            }
        });

        binding.propertiesTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                enableButtons();
            }
        });

        enableButtons();
        binding.propertiesTable.getTableHeader().setReorderingAllowed(false);
        binding.propertiesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        binding.propertiesTable.setModel(tableModel);
        add(binding.mainPanel, BorderLayout.CENTER);
    }

    public void setTitle( final String title ) {
        Border border = binding.propertiesPanel.getBorder();
        if ( border instanceof TitledBorder ) {
            TitledBorder titledBorder = (TitledBorder) border;
            titledBorder.setTitle( title );
        }
    }

    public void setPropertyEditTitle( final String title ) {
        this.propertyEditTitle = title;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties( final Map<String, String> newprops ) {
        this.properties = newprops;
    }

    public static final class UiBinding {
        private JTable propertiesTable;
        private JButton addButton;
        private JButton removeButton;
        private JButton editButton;
        private JPanel mainPanel;
        private JPanel propertiesPanel;
    }

    //- PRIVATE

    // This allows this component to be used outside of this module by
    // hiding the fact that it is an IDEA form component.
    private UiBinding binding = new UiBinding();

    private Map<String, String> properties = new LinkedHashMap<String, String>();
    private String propertyEditTitle = "Edit Property";

    private Pair<String, String> getRow( final int rowIndex ) {
        Iterator<String> names = properties.keySet().iterator();
        for (int i = 0; i < properties.size() && names.hasNext(); i++) {
            final String name = names.next();
            if (i != rowIndex) continue;
            return new Pair<String, String>(name, properties.get(name));
        }
        return null;
    }

    private final AbstractTableModel tableModel = new AbstractTableModel() {
        @Override
        public int getRowCount() {
            return properties.size();
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public Object getValueAt( final int rowIndex, final int columnIndex ) {
            Pair<String, String> row = getRow(rowIndex);
            if (row == null) return null;
            switch(columnIndex) {
                case 0:  return row.left;
                case 1:  return row.right;
                default: throw new IllegalArgumentException("Invalid column");
            }
        }

        @Override
        public String getColumnName( final int column ) {
            switch(column) {
                case 0:  return "Name";
                case 1:  return "Value";
                default: throw new IllegalArgumentException("Invalid column");
            }
        }
    };

    private void edit( final String name, final String value ) {
        final PropertyEditDialog dlg = new PropertyEditDialog(SwingUtilities.getWindowAncestor(binding.mainPanel), propertyEditTitle, name, value);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            @Override
            public void run() {
                if (dlg.isOk()) {
                    properties.remove(name);
                    properties.put(dlg.getPropertyName(), dlg.getPropertyValue());
                    tableModel.fireTableDataChanged();
                    enableButtons();
                }
            }
        });
    }

    private void enableButtons() {
        int sel = binding.propertiesTable.getSelectionModel().getLeadSelectionIndex();
        binding.editButton.setEnabled(sel >= 0);
        binding.removeButton.setEnabled(sel >= 0);
    }
}
