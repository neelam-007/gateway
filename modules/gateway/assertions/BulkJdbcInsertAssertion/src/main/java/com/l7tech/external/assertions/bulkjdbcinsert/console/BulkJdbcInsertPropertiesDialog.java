package com.l7tech.external.assertions.bulkjdbcinsert.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.util.Registry;
import com.l7tech.external.assertions.bulkjdbcinsert.BulkJdbcInsertAssertion;
import com.l7tech.gateway.common.jdbc.JdbcAdmin;
import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.gui.SimpleTableModel;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.TableUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.util.Functions;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigInteger;
import java.util.*;

/**
 * Created by moiyu01 on 15-09-25.
 */
public class BulkJdbcInsertPropertiesDialog extends AssertionPropertiesOkCancelSupport<BulkJdbcInsertAssertion> {
    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.external.assertions.bulkjdbcinsert.console.resources.BulkJdbcInsertPropertiesDialog");
    private static final BigInteger MAX_INT_VALUE = BigInteger.valueOf(Integer.MAX_VALUE);
    private static final int MAX_DISPLAYABLE_MESSAGE_LENGTH = 80;

    private JPanel mainPanel;
    private JComboBox connectionComboBox;
    private JTextField tableNameTextField;
    private JTable mappingTable;
    private JButton addButton;
    private JButton editButton;
    private JButton removeButton;
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
    private JComboBox recordDelimiterComboBox;
    private JButton upButton;
    private JButton downButton;
    private final Map<String,String> connToDriverMap = new HashMap<String, String>();
    private SimpleTableModel<BulkJdbcInsertAssertion.ColumnMapper> mappingTableModel;

    InputValidator inputValidator;

    public BulkJdbcInsertPropertiesDialog(final Frame parent, final BulkJdbcInsertAssertion assertion) {
        super(BulkJdbcInsertAssertion.class, parent, assertion, true);
        initComponents(parent);
    }


    protected void initComponents(final Frame parent) {
        super.initComponents();

        recordDelimiterComboBox.setModel(new DefaultComboBoxModel(BulkJdbcInsertAssertion.recordDelimiterMap.keySet().toArray(new String[0])));
        recordDelimiterComboBox.setSelectedItem("CRLF");

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

        inputValidator.addRule(new InputValidator.ComponentValidationRule(tableNameTextField) {
            @Override
            public String getValidationError() {
                if(StringUtils.isBlank(tableNameTextField.getText())) {
                    return resources.getString("table.name.empty.error");
                }
                return null;
            }
        });

        inputValidator.constrainTextFieldToBeNonEmpty(fieldDelimiterLabel.getText(), fieldDelimiterTextField, new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                if(Syntax.getReferencedNames(fieldDelimiterTextField.getText()).length == 0) {
                    if(fieldDelimiterTextField.getText().length() > 1) {
                        return String.format(resources.getString("field.length.error"), fieldDelimiterLabel.getText());
                    }
                }
                return null;
            }
        });
        //inputValidator.constrainTextFieldToMaxChars(fieldDelimiterLabel.getText(), fieldDelimiterTextField, 1, null);

        quotedCheckBox.addActionListener(connectionListener);

        ((JTextField)connectionComboBox.getEditor().getEditorComponent()).getDocument().addDocumentListener(connectionListener);
        connectionComboBox.addItemListener(connectionListener);
        //Initialize mapping table
        mappingTableModel = TableUtil.configureTable(
                mappingTable,
                TableUtil.column(resources.getString("column.label.name"), 50, 100, 100000, property("name"), String.class),
                TableUtil.column(resources.getString("column.label.order"), 50, 100, 100000, property("order"), String.class),
                TableUtil.column(resources.getString("column.label.transformation"), 50, 150, 100000, property("transformation"), String.class),
                TableUtil.column(resources.getString("column.label.transformParam"), 50, 200, 100000, property("transformParam"), String.class)
        );
        mappingTable.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        mappingTable.getTableHeader().setReorderingAllowed(false);
        mappingTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                int index = mappingTable.getSelectedRow();
                boolean selectedMany = mappingTable.getSelectedRowCount() > 1;
                upButton.setEnabled(index > 0 && !selectedMany);
                downButton.setEnabled(index >= 0 && index < mappingTableModel.getRowCount() - 1 && !selectedMany);
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
                    mappingTableModel.addRow(mapper);
                    mappingTableModel.fireTableDataChanged();
                }
            }
        });

        editButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final int selectedIndex = mappingTable.getSelectedRow();
                if (selectedIndex >= 0) {
                    BulkJdbcInsertAssertion.ColumnMapper mapper = mappingTableModel.getRowObject(selectedIndex);
                    BulkJdbcInsertTableMapperDialog dlg = new BulkJdbcInsertTableMapperDialog(BulkJdbcInsertPropertiesDialog.this, "Edit Mapping", mapper);
                    dlg.pack();
                    Utilities.centerOnScreen(dlg);
                    dlg.setVisible(true);
                    if (dlg.isConfirmed()) {
                        mappingTableModel.setRowObject(selectedIndex, mapper);
                        mappingTableModel.fireTableDataChanged();
                    }
                }
            }
        });

        Utilities.setDoubleClickAction(mappingTable, editButton);

        removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final int[] viewRows = mappingTable.getSelectedRows();
                if (viewRows.length > 0) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < viewRows.length; i++) {
                        //limit the length to max displayable so it won't go over
                        if(sb.length() >= MAX_DISPLAYABLE_MESSAGE_LENGTH) {
                            sb.append(" ...");
                            break;
                        }
                        sb.append(mappingTableModel.getRowObject(viewRows[i]).getName());
                        if (i != viewRows.length - 1) {
                            sb.append(",");
                        }
                    }
                    Object[] options = {resources.getString("button.remove"), resources.getString("button.cancel")};
                    int result = JOptionPane.showOptionDialog(BulkJdbcInsertPropertiesDialog.this, resources.getString("confirmation.remove.mapping"),
                            resources.getString("dialog.title.remove.mapping"), 0, JOptionPane.WARNING_MESSAGE, null, options, options[1]);
                    if (result == 0) {
                        for (int i = 0; i < viewRows.length; i++) {
                            mappingTableModel.removeRowAt(mappingTable.getSelectedRow());
                        }
                    }
                }
            }
        });

        upButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final int viewRow = mappingTable.getSelectedRow();
                if (viewRow > 0) {
                    int prevIndex = viewRow - 1;
                    mappingTableModel.swapRows(prevIndex, viewRow);
                    mappingTable.changeSelection(prevIndex, 0, false, false);
                }
                enableOrDisableTableButtons();
            }
        });
        downButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final int viewRow = mappingTable.getSelectedRow();
                if (viewRow > -1 && viewRow < mappingTableModel.getRowCount() - 1) {
                    int nextIndex = viewRow + 1;
                    mappingTableModel.swapRows(viewRow, nextIndex);
                    mappingTable.changeSelection(nextIndex, 0, false, false);
                }
                enableOrDisableTableButtons();
            }
        });

        inputValidator.addRule(new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                if(Syntax.getReferencedNames(quoteTextField.getText()).length == 0) {
                    if(quoteTextField.getText().length() > 1) {
                        return String.format(resources.getString("field.length.error"), quoteLabel.getText());
                    }
                }
                return null;
            }
        });
        inputValidator.addRule(new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                if(Syntax.getReferencedNames(escapeQuoteTextField.getText()).length == 0) {
                    if(escapeQuoteTextField.getText().length() > 1) {
                        return String.format(resources.getString("field.length.error"), escapeQuoteLabel.getText());
                    }
                }
                return null;
            }
        });

        mappingTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent
                    (JTable table, Object value, boolean selected, boolean focused, int row, int column) {
                setEnabled(table.isEnabled());
                return super.getTableCellRendererComponent(table, value, selected, focused, row, column);
            }
        });

        batchSizeSpinner.setModel(new SpinnerBigIntegerModel(BigInteger.ONE, BigInteger.ONE, MAX_INT_VALUE, BigInteger.ONE));
        enableOrDisableComponents();

        inputValidator.attachToButton(getOkButton(), super.createOkAction());
    }

    private void enableOrDisableComponents() {
        boolean quoted = quotedCheckBox.isSelected();
        quoteLabel.setEnabled(quoted);
        quoteTextField.setEnabled(quoted);
        enableOrDisableTableButtons();
    }

    private void enableOrDisableTableButtons() {
        boolean isSelected = mappingTable.getSelectedRow() >=0;
        boolean selectedMany = mappingTable.getSelectedRowCount() > 1;
        editButton.setEnabled(isSelected && !selectedMany);
        removeButton.setEnabled(isSelected);
        upButton.setEnabled(isSelected && !selectedMany);
        downButton.setEnabled(isSelected && !selectedMany);
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
        recordDelimiterComboBox.setSelectedItem(assertion.getRecordDelimiter());
        fieldDelimiterTextField.setText(assertion.getFieldDelimiter());
        quoteTextField.setText(assertion.getQuoteChar());
        escapeQuoteTextField.setText(assertion.getEscapeQuote());
        if(assertion.getColumnMapperList() != null) {
            for(BulkJdbcInsertAssertion.ColumnMapper mapper : assertion.getColumnMapperList()) {
                //clone Mapper Objects
                mappingTableModel.addRow((BulkJdbcInsertAssertion.ColumnMapper)mapper.clone());
            }
        }
        decompressionComboBox.setSelectedItem(assertion.getCompression());
        batchSizeSpinner.setValue(BigInteger.valueOf(assertion.getBatchSize()));
        enableOrDisableComponents();
    }

    @Override
    public BulkJdbcInsertAssertion getData(BulkJdbcInsertAssertion assertion) throws ValidationException {
        assertion.setConnectionName(( connectionComboBox.getSelectedItem()).toString());
        assertion.setTableName(tableNameTextField.getText().trim());
        assertion.setQuoted(quotedCheckBox.isSelected());
        if(recordDelimiterComboBox.getSelectedIndex() == -1) {
            assertion.setRecordDelimiter("CRLF");
        }
        else {
            assertion.setRecordDelimiter(recordDelimiterComboBox.getSelectedItem().toString());
        }
        assertion.setFieldDelimiter(fieldDelimiterTextField.getText());
        assertion.setEscapeQuote(escapeQuoteTextField.getText());
        assertion.setQuoteChar(quoteTextField.getText());
        if(mappingTableModel.getRowCount() > 0) {
            assertion.setColumnMapperList(mappingTableModel.getRows());
        }
        else {
            assertion.setColumnMapperList(Collections.EMPTY_LIST);
        }
        assertion.setCompression((BulkJdbcInsertAssertion.Compression)decompressionComboBox.getSelectedItem());
        assertion.setBatchSize(((BigInteger) batchSizeSpinner.getValue()).intValue());
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

    private static Functions.Unary<String, BulkJdbcInsertAssertion.ColumnMapper> property(final String propName) {
        return Functions.propertyTransform(BulkJdbcInsertAssertion.ColumnMapper.class, propName);
    }

    private static class SpinnerBigIntegerModel extends SpinnerNumberModel {

        public SpinnerBigIntegerModel(BigInteger value, BigInteger minimum, BigInteger maximum, BigInteger stepSize) {
            super(value, minimum, maximum, stepSize);
        }
        /**
         * Returns the next number in the sequence.
         *
         * @return <code>value + stepSize</code> or <code>maximum</code> if the sum
         *     exceeds <code>maximum</code>.
         *
         * @see SpinnerModel#getNextValue
         * @see #getPreviousValue
         * @see #setStepSize
         */
        @Override
        public Object getNextValue() {
            BigInteger currentVal = (BigInteger)getValue();
            BigInteger nextVal = currentVal.add((BigInteger)getStepSize());
            return nextVal.min((BigInteger)getMaximum());
        }


        /**
         * Returns the previous number in the sequence.
         *
         * @return <code>value - stepSize</code>, or
         *     <code>minimum</code> if the sum is less
         *     than <code>minimum</code>.
         *
         * @see SpinnerModel#getPreviousValue
         * @see #getNextValue
         * @see #setStepSize
         */
        @Override
        public Object getPreviousValue() {
            BigInteger currentVal = (BigInteger)getValue();
            BigInteger previousVal = currentVal.subtract((BigInteger)getStepSize());
            return previousVal.max((BigInteger)getMinimum());
        }
    }

}
