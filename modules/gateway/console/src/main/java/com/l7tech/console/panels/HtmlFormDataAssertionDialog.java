/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.panels;

import com.l7tech.gui.util.ImageCache;
import com.l7tech.policy.assertion.HtmlFormDataAssertion;
import com.l7tech.policy.assertion.HtmlFormDataLocation;
import com.l7tech.policy.assertion.HtmlFormDataType;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Properties dialog for the HTML Form Data Assertion.
 *
 * @author rmak
 * @since SecureSpan 3.7
 * @see HtmlFormDataAssertion
 */
public class HtmlFormDataAssertionDialog extends JDialog implements TableModelListener {

    /**
     * Custom cell renderer for the HTML Form field table. The main customizations
     * are highlighting of any table cell that has invalid value, and displaying
     * of warning text against invalid cell values.
     */
    private class FieldTableCellRenderer extends DefaultTableCellRenderer {
        private final Color DARK_YELLOW = new Color(192, 192, 0);
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            final String warning = _fieldTableModel.getCellWarning(row, column);
            if (warning == null) {
                if (isSelected) {
                   super.setForeground(table.getSelectionForeground());
                   super.setBackground(table.getSelectionBackground());
                }
                else {
                    super.setForeground(table.getForeground());
                    super.setBackground(table.getBackground());
                }
            } else {
                setForeground(Color.RED);
                if (isSelected) {
                    setBackground(DARK_YELLOW);
                } else {
                    setBackground(Color.YELLOW);
                }
            }

            if (isSelected && hasFocus) {
                // Displays the warning text associated with this cell; if any.
                if (warning == null) {
                    // This cell has no warning. But if other cells have warning,
                    // we display a generic text. We can tell if other cells
                    // have warning if the warning icon is displayed.
                    if (_warningLabel.getIcon() == null) {
                        _warningLabel.setText(null);
                    } else {
                        _warningLabel.setText("Invalid value(s) detected; select a highlighted cell for more information");
                    }
                } else {
                    _warningLabel.setText(warning);
                }
            }

            if (value instanceof Number) {
                setHorizontalAlignment(JLabel.RIGHT);
            } else {
                setHorizontalAlignment(JLabel.LEFT);
            }

            return component;
        }
    }

    /**
     * Custom model for the HTML Form field table. The main customizations are
     * row sorting by field name, and generation of warning text against invalid
     * values.
     */
    private class FieldTableModel extends DefaultTableModel {
        /** Current sorting order in each column. */
        private boolean[] _ascending;

        /**
         * Creates the table model and populates it with given fields in sorted order by name.
         * @param fieldSpecs    field specifications to initialize with; field
         *                      properties will be copied (not referenced)
         */
        public FieldTableModel(final HtmlFormDataAssertion.FieldSpec[] fieldSpecs) {
            super(FieldColumn.getNames(), fieldSpecs.length);

            // Populates the table with the initial Form field values.
            int row = 0;
            for (HtmlFormDataAssertion.FieldSpec fieldSpec : fieldSpecs) {
                setValueAt(fieldSpec.getName(), row, FIELD_NAME_COLUMN.index);
                setValueAt(fieldSpec.getDataType(), row, FIELD_DATA_TYPE_COLUMN.index);
                setValueAt(fieldSpec.getMinOccurs(), row, FIELD_MIN_OCCURS_COLUMN.index);
                setValueAt(fieldSpec.getMaxOccurs(), row, FIELD_MAX_OCCURS_COLUMN.index);
                setValueAt(fieldSpec.getAllowedLocation(), row, FIELD_LOCATION_COLUMN.index);
                ++ row;
            }

            _ascending = new boolean[getColumnCount()];
            sort(FIELD_NAME_COLUMN.index);  // Starts out sorted by field names.
        }

        public Class<?> getColumnClass(int columnIndex) {
            return FieldColumn.findByIndex(columnIndex).clazz;
        }

        /**
         * Sort by the given column. Sorting order is toggled from current order.
         *
         * @param columnIndex   column to sort
         */
        public void sort(final int columnIndex) {
            final int rowCount = getRowCount();
            for (int i = 0; i < rowCount; ++ i) {
                for (int j = i + 1; j < rowCount; ++ j) {
                    final String cell_i = getValueAt(i, columnIndex).toString();
                    final String cell_j = getValueAt(j, columnIndex).toString();
                    if (_ascending[columnIndex]) {
                        if (cell_i.compareTo(cell_j) < 0) {
                            swapRows(i, j);
                        }
                    } else {
                        if (cell_i.compareTo(cell_j) > 0) {
                            swapRows(i, j);
                        }
                    }
                }
            }
            _ascending[columnIndex] = !_ascending[columnIndex];
        }

        /**
         * Swaps the cell contents of two rows.
         * @param row1  index of row 1
         * @param row2  index of row 2
         */
        private void swapRows(final int row1, final int row2) {
            for (int column = 0; column < getColumnCount(); ++ column) {
                final Object value_1 = getValueAt(row1, column);
                final Object value_2 = getValueAt(row2, column);
                setValueAt(value_2, row1, column);
                setValueAt(value_1, row2, column);
            }
        }

        /**
         * Generates a warning against a cell value if it is invalid.
         *
         * @param row       row index of the cell
         * @param column    column index of the cell
         * @return a warning string if cell value is invalid;
         *         <code>null</code> if cell value is valid
         */
        private String getCellWarning(final int row, final int column) {
            if (column == FIELD_NAME_COLUMN.index) {
                final String fieldName = (String)getValueAt(row, column);
                if (fieldName.trim().length() == 0) {
                    return "Field name cannot be blank";
                }
                for (int r = 0; r < getRowCount(); ++ r) {
                    if (r != row) {
                        if (getValueAt(r, column).equals(fieldName)) {
                            return "Duplicate field name";
                        }
                    }
                }
            } else if (column == FIELD_DATA_TYPE_COLUMN.index) {
                final HtmlFormDataType dataType = (HtmlFormDataType) getValueAt(row, column);
                if (dataType == HtmlFormDataType.FILE) {
                    if (!_allowPostCheckbox.isSelected()) {
                        return "Data type \"" + HtmlFormDataType.FILE + "\" requires the POST submission method";
                    }
                    final HtmlFormDataLocation location = (HtmlFormDataLocation) getValueAt(row, FIELD_LOCATION_COLUMN.index);
                    if (location == HtmlFormDataLocation.URL) {
                        return "Data type cannot be \"" + HtmlFormDataType.FILE + "\" when location is \"" + HtmlFormDataLocation.URL + "\"";
                    }
                }
            } else if (column == FIELD_MIN_OCCURS_COLUMN.index) {
                final Integer minValue = (Integer) getValueAt(row, column);
                if (minValue == null) {
                    return "Minimum cannot be blank";
                }
                final int minOccurs = minValue.intValue();
                if (minOccurs < 0) {
                    return "Minimum must be >= 0";
                } else {
                    final Integer maxValue = (Integer) getValueAt(row, FIELD_MAX_OCCURS_COLUMN.index);
                    if (maxValue != null) {
                        final int maxOccurs = maxValue.intValue();
                        if (maxOccurs >= 0) {
                            if (minOccurs > maxOccurs) {
                                return "Minimum must be <= maximum";
                            }
                        }
                    }
                }
            } else if (column == FIELD_MAX_OCCURS_COLUMN.index) {
                final Integer maxValue = (Integer) getValueAt(row, column);
                if (maxValue == null) {
                    return "Maximum cannot be blank";
                }
                final int maxOccurs = maxValue.intValue();
                if (maxOccurs < 0) {
                    return "Maximum must be >= 0";
                } else {
                    final Integer minValue = (Integer) getValueAt(row, FIELD_MIN_OCCURS_COLUMN.index);
                    if (minValue != null) {
                        final int minOccurs = minValue.intValue();
                        if (minOccurs >= 0) {
                            if (minOccurs > maxOccurs) {
                                return "Maximum must be >= minimum";
                            }
                        }
                    }
                }
            } else if (column == FIELD_LOCATION_COLUMN.index) {
                final HtmlFormDataLocation location = (HtmlFormDataLocation) getValueAt(row, column);
                if (location == HtmlFormDataLocation.URL) {
                    final HtmlFormDataType dataType = (HtmlFormDataType) getValueAt(row, FIELD_DATA_TYPE_COLUMN.index);
                    if (dataType == HtmlFormDataType.FILE) {
                        return "Location cannot be \"" + HtmlFormDataLocation.URL + "\" when data type is \"" + HtmlFormDataType.FILE + "\"";
                    }
                } else if (location == HtmlFormDataLocation.BODY) {
                    if (!_allowPostCheckbox.isSelected()) {
                        return "Location \"" + HtmlFormDataLocation.BODY + "\" requires the POST submission method";
                    }
                }
            }
            return null;
        }

        /**
         * Determines if any cell value has warning because it is invalid.
         *
         * Note: This implementation computes every cell validation. A more
         * efficient implementation will compute a cell validation only upon
         * insert, change and delete; and then cache the result. But that would
         * take a lot more code.
         *
         * @return <code>true</code> if any cell value has warning
         */
        public boolean hasWarning() {
            for (int row = 0; row < getRowCount(); ++ row) {
                for (int column = 0; column < getColumnCount(); ++ column) {
                    if (getCellWarning(row, column) != null) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    /** Encapsulates miscellaneous properties of a single column in the Form fields table. */
    private static class FieldColumn {
        /** Colum index. */
        public final int index;

        /** Column header display string. */
        public final String name;

        /** Column header tooltip text. */
        public final String toolTip;

        /** Class of cell values in the column. */
        public final Class<?> clazz;

        /** For instance lookup by column index. */
        private static ArrayList<FieldColumn> _byIndex = new ArrayList<FieldColumn>();

        /** For fast checking of duplicate names in instances. */
        private static ArrayList<String> _names = new ArrayList<String>();

        /**
         * Find an instance given its column index.
         *
         * @param index    column index
         * @return the column with the given index
         */
        public static FieldColumn findByIndex(final int index) {
            try {
                return _byIndex.get(index);
            } catch (IndexOutOfBoundsException e) {
                throw new RuntimeException("Internal error: No field column with index " + index + ".");
            }
        }

        /** @return names of all columns */
        public static String[] getNames() {
            return _names.toArray(new String[_names.size()]);
        }

        public FieldColumn(final int index,
                           final String name,
                           final String toolTip,
                           final Class<?> clazz) {
            try {
                if (_byIndex.get(index) != null) {
                    throw new RuntimeException("Internal error: Field column with index " + index + " already exists.");
                } // else index not in use yet (OK)
            } catch (IndexOutOfBoundsException e) {
                // index not in use yet (OK)
            }
            this.index = index;
            this.name = name;
            this.toolTip = toolTip;
            this.clazz = clazz;
            _byIndex.add(index, this);
            _names.add(index, name);
        }
    }

    // Columns of the Form fields table.
    private static final FieldColumn FIELD_NAME_COLUMN = new FieldColumn(0, "Name", "Name of field", String.class);
    private static final FieldColumn FIELD_DATA_TYPE_COLUMN = new FieldColumn(1, "Data Type", "Data type of field value", HtmlFormDataType.class);
    private static final FieldColumn FIELD_MIN_OCCURS_COLUMN = new FieldColumn(2, "Min Occurs", "Minimum number of occurrences", Integer.class);
    private static final FieldColumn FIELD_MAX_OCCURS_COLUMN = new FieldColumn(3, "Max Occurs", "Maximum number of occurrences", Integer.class);
    private static final FieldColumn FIELD_LOCATION_COLUMN = new FieldColumn(4, "Location", "Location in the request message where the field is allowed", HtmlFormDataLocation.class);

    private JPanel _contentPane;
    private JCheckBox _allowGetCheckbox;
    private JCheckBox _allowPostCheckbox;
    private JCheckBox _disallowOtherFieldsCheckBox;
    private JButton _addButton;
    private JButton _removeButton;
    private JButton _okButton;
    private JButton _cancelButton;

    private final ImageIcon WARNING_ICON = new ImageIcon(ImageCache.getInstance().getIcon("com/l7tech/console/resources/Warning16.png"));

    /**
     * This is for displaying warning against invalid value(s) in {@link #_fieldTable}.
     * The Jlabel icon is turned on if any cell has invalid value.
     * The Jlabel text displays specific warnings if an invalid cell is selected;
     * otherwise a generic warning text is displayed.
     */
    private JLabel _warningLabel;

    private JTable _fieldTable;
    private final FieldTableModel _fieldTableModel;
    private final FieldTableCellRenderer _fieldTableCellRenderer = new FieldTableCellRenderer();
    private final HtmlFormDataAssertion _assertion;
    private boolean _modified;
    private boolean readonly;

    public HtmlFormDataAssertionDialog(Frame owner, final HtmlFormDataAssertion assertion, boolean readonly) throws HeadlessException {
        super(owner, "HTML Form Data Properties", true);
        _assertion = assertion;
        this.readonly = readonly;

        _allowGetCheckbox.setSelected(assertion.isAllowGet());
        _allowPostCheckbox.setSelected(assertion.isAllowPost());
        _disallowOtherFieldsCheckBox.setSelected(assertion.isDisallowOtherFields());
        _warningLabel.setIcon(null);
        _warningLabel.setText(null);

        _fieldTableModel = new FieldTableModel(assertion.getFieldSpecs());
        _fieldTableModel.addTableModelListener(this);

        _fieldTable.setModel(_fieldTableModel);
        _fieldTable.setTableHeader(new JTableHeader(_fieldTable.getColumnModel()) {
            // Adds tooltip to column headers.
            public String getToolTipText(MouseEvent event) {
                final TableColumnModel tcm = _fieldTable.getColumnModel();
                final int viewColumnIndex = tcm.getColumnIndexAtX(event.getX());
                final int modelColumnIndex = _fieldTable.convertColumnIndexToModel(viewColumnIndex);
                return FieldColumn.findByIndex(modelColumnIndex).toolTip;
            }
        });
        _fieldTable.getTableHeader().setReorderingAllowed(false);
        _fieldTable.setColumnSelectionAllowed(false);
        _fieldTable.setRowSelectionAllowed(true);
        _fieldTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        _fieldTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                // The remove button is enabled if some row(s) is selected.
                enableRemoveButton();
            }
        });

        // Sets cell renderes and editors.
        final JTextField nameTextField = new JTextField();
        nameTextField.setEditable(true);
        final TableColumn nameColumn = _fieldTable.getColumn(FIELD_NAME_COLUMN.name);
        nameColumn.setCellRenderer(_fieldTableCellRenderer);
        final DefaultCellEditor nameCellEditor = new DefaultCellEditor(nameTextField);
        nameColumn.setCellEditor(nameCellEditor);

        final JComboBox dataTypeCombo = new JComboBox(HtmlFormDataType.values());
        dataTypeCombo.setEditable(false);
        final TableColumn dataTypeColumn = _fieldTable.getColumn(FIELD_DATA_TYPE_COLUMN.name);
        dataTypeColumn.setCellRenderer(_fieldTableCellRenderer);
        final DefaultCellEditor dataTypeCellEditor = new DefaultCellEditor(dataTypeCombo);
        dataTypeCellEditor.setClickCountToStart(2);
        dataTypeColumn.setCellEditor(dataTypeCellEditor);

        final TableColumn minOccursColumn = _fieldTable.getColumn(FIELD_MIN_OCCURS_COLUMN.name);
        minOccursColumn.setCellRenderer(_fieldTableCellRenderer);

        final TableColumn maxOccursColumn = _fieldTable.getColumn(FIELD_MAX_OCCURS_COLUMN.name);
        maxOccursColumn.setCellRenderer(_fieldTableCellRenderer);

        final JComboBox locationCombo = new JComboBox(HtmlFormDataLocation.values());
        locationCombo.setEditable(false);
        final TableColumn locationColumn = _fieldTable.getColumn(FIELD_LOCATION_COLUMN.name);
        locationColumn.setCellRenderer(_fieldTableCellRenderer);
        final DefaultCellEditor locationCellEditor = new DefaultCellEditor(locationCombo);
        locationCellEditor.setClickCountToStart(2);
        locationColumn.setCellEditor(locationCellEditor);

        // Provides sorting by field name. (i.e., no sorting by other columns)
        final JTableHeader hdr = _fieldTable.getTableHeader();
        hdr.addMouseListener(new MouseAdapter(){
            public void mouseClicked(MouseEvent event) {
                final TableColumnModel tcm = _fieldTable.getColumnModel();
                final int viewColumnIndex = tcm.getColumnIndexAtX(event.getX());
                final int modelColumnIndex = _fieldTable.convertColumnIndexToModel(viewColumnIndex);
                if (modelColumnIndex == FIELD_NAME_COLUMN.index) {
                    _fieldTableModel.sort(modelColumnIndex);
                }
            }
        });

        _addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                _fieldTableModel.addRow(new Object[]{"New Field", HtmlFormDataType.ANY, 1, 1, HtmlFormDataLocation.ANYWHERE});
                final int row = _fieldTable.getRowCount() - 1;
                if (_fieldTable.editCellAt(row, FIELD_NAME_COLUMN.index)) {
                    _fieldTable.changeSelection(row, FIELD_NAME_COLUMN.index, false, false);     // so that the view scrolls automatically to make this row visible
                    nameTextField.requestFocusInWindow();   // so that text gets into this cell as soon as user starts typing
                    nameTextField.selectAll();  // so that the temporary text is replaced
                }
            }
        });

        _removeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                final TableCellEditor editor = _fieldTable.getCellEditor();
                if (editor != null) {
                    editor.cancelCellEditing();
                    // Otherwise, removing the row will cause editing to stop
                    // (instead of cancel) and set values for a row index that
                    // doesn't exist any more.
                }
                final int[] rows = _fieldTable.getSelectedRows();
                Arrays.sort(rows);
                for (int i = rows.length - 1; i >= 0; -- i) {
                    _fieldTableModel.removeRow(rows[i]);
                }
                enableOkButton();   // Removal of rows with invalid value may allow the OK button to be re-enabled.
            }
        });

        _allowGetCheckbox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                enableOkButton();
            }
        });

        _allowPostCheckbox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                _fieldTable.repaint();  // Since this could affect the validity of Location values.
                enableOkButton();
            }
        });

        _okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                onOK();
            }
        });

        _cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                onCancel();
            }
        });

        enableRemoveButton();
        enableOkButton();

        setContentPane(_contentPane);
        getRootPane().setDefaultButton(_okButton);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });
    }

    public boolean isAssertionModified() {
        return _modified;
    }

    public void tableChanged(TableModelEvent event) {
        _warningLabel.setText(null);
        _fieldTable.repaint();
        enableOkButton();
    }

    private void onOK() {
        if (_fieldTable.getCellEditor() != null)
            _fieldTable.getCellEditor().stopCellEditing();
        _assertion.setAllowGet(_allowGetCheckbox.isSelected());
        _assertion.setAllowPost(_allowPostCheckbox.isSelected());
        _assertion.setDisallowOtherFields(_disallowOtherFieldsCheckBox.isSelected());

        // Replaces FieldSpec's in assertion by FieldSpec's in table.
        final int numFields = _fieldTable.getRowCount();
        final HtmlFormDataAssertion.FieldSpec[] fieldSpecs = new HtmlFormDataAssertion.FieldSpec[numFields];
        for (int i = 0; i < numFields; ++i) {
            final String name = ((String) _fieldTable.getValueAt(i, FIELD_NAME_COLUMN.index)).trim();
            final HtmlFormDataType dataType = (HtmlFormDataType) _fieldTable.getValueAt(i, FIELD_DATA_TYPE_COLUMN.index);
            final int minOccurs = ((Integer) _fieldTable.getValueAt(i, FIELD_MIN_OCCURS_COLUMN.index)).intValue();
            final int maxOccurs = ((Integer) _fieldTable.getValueAt(i, FIELD_MAX_OCCURS_COLUMN.index)).intValue();
            final HtmlFormDataLocation allowedLocation = (HtmlFormDataLocation) _fieldTable.getValueAt(i, FIELD_LOCATION_COLUMN.index);
            fieldSpecs[i] = new HtmlFormDataAssertion.FieldSpec(
                    name, dataType, minOccurs, maxOccurs, allowedLocation
            );
        }
        _assertion.setFieldSpecs(fieldSpecs);

        _modified = true;
        dispose();
    }

    private void onCancel() {
        _modified = false;
        dispose();
    }

    private void enableRemoveButton() {
        _removeButton.setEnabled(_fieldTable.getSelectedRow() != -1);
    }

    /**
     * Determines if the OK button should be enabled. Also resets the warning
     * icon and text.
     */
    private void enableOkButton() {
        final boolean hasWarning = _fieldTableModel.hasWarning();
        _okButton.setEnabled(!readonly && (_allowGetCheckbox.isSelected() || _allowPostCheckbox.isSelected())
                             && !hasWarning);
        if (hasWarning) {
            _warningLabel.setIcon(WARNING_ICON);
            if (_warningLabel.getText() == null) {
                _warningLabel.setText("Invalid value(s) detected; select a highlighted cell for more information");
            }
        } else {
            _warningLabel.setIcon(null);
        }
    }
}
