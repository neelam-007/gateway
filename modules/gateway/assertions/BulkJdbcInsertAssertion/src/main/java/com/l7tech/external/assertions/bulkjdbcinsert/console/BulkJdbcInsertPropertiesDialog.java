package com.l7tech.external.assertions.bulkjdbcinsert.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.util.Registry;
import com.l7tech.external.assertions.bulkjdbcinsert.BulkJdbcInsertAssertion;
import com.l7tech.gateway.common.jdbc.JdbcAdmin;
import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.FindException;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;

/**
 * Created by moiyu01 on 15-09-25.
 */
public class BulkJdbcInsertPropertiesDialog extends AssertionPropertiesOkCancelSupport<BulkJdbcInsertAssertion> {
    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.external.assertions.bulkjdbcinsert.console.resources.BulkJdbcInsertPropertiesDialog");
    private static final int MAX_TABLE_COLUMN_NUM = 4;


    private JPanel mainPanel;
    private JComboBox connectionComboBox;
    private JTextField tableNameTextField;
    private JTable mappingTable;
    private JButton addButton;
    private JButton editButton;
    private JButton removeButton;
    private JTextField recordDelimiterTextField;
    private JTextField fieldDelimiterTextField;
    private JCheckBox quotedCheckBox;
    private JComboBox decompressionComboBox;
    private JSpinner batchSizeSpinner;
    private JLabel connectionNameLabel;
    private JTextField escapeQuoteTextField;
    private JLabel decompressionLabel;
    private JLabel quoteLabel;
    private JTextField quoteTextField;
    private JLabel escapeQuoteLabel;
    private JLabel tableNameLabel;
    private JLabel fieldDelimiterLabel;
    private final Map<String,String> connToDriverMap = new HashMap<String, String>();
    private List<BulkJdbcInsertAssertion.ColumnMapper> mapperList = new ArrayList<>();
    private ColumnMappingTableModel mappingTableModel;

    InputValidator inputValidator;

    public BulkJdbcInsertPropertiesDialog(final Frame parent, final BulkJdbcInsertAssertion assertion) {
        super(BulkJdbcInsertAssertion.class, parent, assertion, true);
        initComponents();
    }

    @Override
    protected void initComponents() {
        super.initComponents();

        inputValidator = new InputValidator(this, getTitle());
        inputValidator.addRule(new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                return StringUtils.isBlank(((JTextField)connectionComboBox.getEditor().getEditorComponent()).getText())? resources.getString("connection.name.error"):null;
            }
        });

        decompressionComboBox.setModel(new DefaultComboBoxModel(BulkJdbcInsertAssertion.Compression.values()));
        inputValidator.ensureComboBoxSelection(decompressionLabel.getText(), decompressionComboBox);

        final RunOnChangeListener connectionListener = new RunOnChangeListener(new Runnable() {
            public void run() {
                enableOrDisableComponents();
            }
        });

        inputValidator.constrainTextFieldToBeNonEmpty(tableNameLabel.getText(),tableNameTextField, null);
        inputValidator.constrainTextFieldToMaxChars(fieldDelimiterLabel.getText(),fieldDelimiterTextField,1,null);

        quotedCheckBox.addActionListener(connectionListener);

        ((JTextField)connectionComboBox.getEditor().getEditorComponent()).getDocument().addDocumentListener(connectionListener);
        connectionComboBox.addItemListener(connectionListener);
        //Initialize mapping table
        mappingTableModel = new ColumnMappingTableModel();
        mappingTable.setModel(mappingTableModel);
        mappingTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        mappingTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                enableOrDisableTableButtons();
            }
        });

        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                BulkJdbcInsertAssertion.ColumnMapper mapper = new BulkJdbcInsertAssertion.ColumnMapper();
                BulkJdbcInsertTableMapperDialog dlg = new BulkJdbcInsertTableMapperDialog(BulkJdbcInsertPropertiesDialog.this, "Add New Mapping", mapper);
                dlg.pack();
                Utilities.centerOnScreen(dlg);
                dlg.setVisible(true);
                if (dlg.isConfirmed()) {
                    mapperList.add(mapper);
                    mappingTableModel.fireTableDataChanged();
                }
            }
        });

        editButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final int selectedIndex = mappingTable.getSelectedRow();
                if (selectedIndex >= 0) {
                    BulkJdbcInsertAssertion.ColumnMapper mapper = mapperList.get(selectedIndex);
                    BulkJdbcInsertTableMapperDialog dlg = new BulkJdbcInsertTableMapperDialog(BulkJdbcInsertPropertiesDialog.this, "Edit Mapping", mapper);
                    dlg.pack();
                    Utilities.centerOnScreen(dlg);
                    dlg.setVisible(true);
                    if (dlg.isConfirmed()) {
                        mapperList.set(selectedIndex, mapper);
                        mappingTableModel.fireTableDataChanged();
                    }
                }
            }
        });

        removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final int selectedIndex = mappingTable.getSelectedRow();
                if (selectedIndex >= 0) {
                    Object[] options = {resources.getString("button.remove"), resources.getString("button.cancel")};
                    int result = JOptionPane.showOptionDialog(BulkJdbcInsertPropertiesDialog.this, resources.getString("confirmation.remove.mapping"),
                            resources.getString("dialog.title.remove.mapping"), 0, JOptionPane.WARNING_MESSAGE, null, options, options[1]);
                    if(0 == result) {
                        mapperList.remove(selectedIndex);
                        mappingTableModel.fireTableDataChanged();
                    }
                }
            }
        });

        inputValidator.constrainTextFieldToMaxChars(quoteLabel.getText(),quoteTextField, 1, null);
        inputValidator.constrainTextFieldToMaxChars(escapeQuoteLabel.getText(), escapeQuoteTextField, 1, null);

        mappingTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent
                    (JTable table, Object value, boolean selected, boolean focused, int row, int column) {
                setEnabled(table.isEnabled());
                return super.getTableCellRendererComponent(table, value, selected, focused, row, column);
            }
        });

        batchSizeSpinner.setModel(new SpinnerNumberModel(1, 1, JdbcAdmin.UPPER_BOUND_MAX_RECORDS, 1));

        enableOrDisableComponents();

        inputValidator.attachToButton(getOkButton(), super.createOkAction());
    }

    private void enableOrDisableComponents() {
        boolean quoted = quotedCheckBox.isSelected();
        quoteLabel.setEnabled(quoted);
        quoteTextField.setEnabled(quoted);
        escapeQuoteTextField.setEnabled(quoted);
        escapeQuoteLabel.setEnabled(quoted);
        enableOrDisableTableButtons();
    }

    private void enableOrDisableTableButtons() {
        boolean isSelected = mappingTable.getSelectedRow() >=0;
        editButton.setEnabled(isSelected);
        removeButton.setEnabled(isSelected);
    }

    @Override
    public void setData(BulkJdbcInsertAssertion assertion) {
        populateConnectionCombobox();
        final String connName = assertion.getConnectionName();
        if (connName != null) {
            connectionComboBox.setSelectedItem(connName);
        } else {
            // default selection is no selection
            connectionComboBox.setSelectedItem(null);
        }
        tableNameTextField.setText(assertion.getTableName());
        quotedCheckBox.setSelected(assertion.isQuoted());
        recordDelimiterTextField.setText(assertion.getRecordDelimiter());
        fieldDelimiterTextField.setText(assertion.getFieldDelimiter());
        quoteTextField.setText(assertion.getQuoteChar());
        escapeQuoteTextField.setText(assertion.getEscapeQuote());
        if(assertion.getColumnMapperList() != null) {
            mapperList.addAll(assertion.getColumnMapperList());
        }
        decompressionComboBox.setSelectedItem(assertion.getCompression());
        batchSizeSpinner.setValue(assertion.getBatchSize());
        enableOrDisableComponents();
    }

    @Override
    public BulkJdbcInsertAssertion getData(BulkJdbcInsertAssertion assertion) throws ValidationException {
        assertion.setConnectionName(( connectionComboBox.getSelectedItem()).toString());
        assertion.setTableName(tableNameTextField.getText());
        assertion.setQuoted(quotedCheckBox.isSelected());
        assertion.setRecordDelimiter(recordDelimiterTextField.getText());
        assertion.setFieldDelimiter(fieldDelimiterTextField.getText());
        assertion.setEscapeQuote(escapeQuoteTextField.getText());
        assertion.setQuoteChar(quoteTextField.getText());
        assertion.setColumnMapperList(mapperList);
        assertion.setCompression((BulkJdbcInsertAssertion.Compression)decompressionComboBox.getSelectedItem());
        assertion.setBatchSize((Integer)batchSizeSpinner.getValue());
        return assertion;
    }

    @Override
    protected JPanel createPropertyPanel() {
        return mainPanel;
    }

    @Override
    protected ActionListener createOkAction() {
        return new RunOnChangeListener();
    }

    private void populateConnectionCombobox() {
        java.util.List<JdbcConnection> connectionList;
        JdbcAdmin admin = getJdbcConnectionAdmin();
        if (admin == null) {
            return;
        } else {
            try {
                connectionList = admin.getAllJdbcConnections();
            } catch (FindException e) {
                //logger.warning("Error getting JDBC connection names");
                return;
            }
        }

        // Sort all default driver classes
        Collections.sort(connectionList);

        connectionComboBox.removeAllItems();

        // Add an empty driver class at the first position of the list
        connectionComboBox.addItem("");

        for (JdbcConnection conn: connectionList) {
            final String connName = conn.getName();
            connToDriverMap.put(connName, conn.getDriverClass());
            connectionComboBox.addItem(connName);
        }

    }

    private JdbcAdmin getJdbcConnectionAdmin() {
        Registry reg = Registry.getDefault();
        if (!reg.isAdminContextPresent()) {
            //logger.warning("Cannot get JDBC Connection Admin due to no Admin Context present.");
            return null;
        }
        return reg.getJdbcConnectionAdmin();
    }

    private class ColumnMappingTableModel extends AbstractTableModel {
        @Override
        public int getColumnCount() {
            return MAX_TABLE_COLUMN_NUM;
        }

        @Override
        public void fireTableDataChanged() {
            super.fireTableDataChanged();
            enableOrDisableTableButtons();
        }

        @Override
        public int getRowCount() {
            return mapperList.size();
        }

        @Override
        public String getColumnName(int col) {
            switch (col) {
                case 0:
                    return resources.getString("column.label.name");
                case 1:
                    return resources.getString("column.label.order");
                case 2:
                    return resources.getString("column.label.transformation");
                case 3:
                    return resources.getString("column.label.transformParam");
                default:
                    throw new IndexOutOfBoundsException("Out of the maximum column number, " + MAX_TABLE_COLUMN_NUM + ".");
            }
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            return false;
        }

        @Override
        public Object getValueAt(int row, int col) {
            BulkJdbcInsertAssertion.ColumnMapper columnMapper =  mapperList.get(row);

            switch (col) {
                case 0:
                    return columnMapper.getName();
                case 1:
                    return columnMapper.getOrder();
                case 2:
                    return columnMapper.getTransformation();
                case 3:
                    return columnMapper.getTransformParam();
                default:
                    throw new IndexOutOfBoundsException("Out of the maximum column number, " + MAX_TABLE_COLUMN_NUM + ".");
            }
        }
    }

}
