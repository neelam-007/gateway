package com.l7tech.external.assertions.remotecacheassertion.console;

import com.l7tech.console.panels.SimplePropertyDialog;
import com.l7tech.external.assertions.remotecacheassertion.server.RedisRemoteCache;
import com.l7tech.gui.SimpleTableModel;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.TableUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.util.Functions;
import com.l7tech.util.NameValuePair;
import com.l7tech.util.Pair;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;


public class RedisConfigPanel implements RemoteCacheConfigPanel {

    private JPanel mainPanel;
    private JTextField serverUriTextField;
    private JTable additionalPropertiesTable;
    private JButton addButton;
    private JButton editButton;
    private JButton removeButton;
    private JPasswordField passwordField;
    private JCheckBox clusterCheckBox;

    private Dialog parent;
    private InputValidator validator;
    private SimpleTableModel<NameValuePair> additionalPropertiesTableModel;

    private InputValidator.ValidationRule serverUrisValidationRule;


    public RedisConfigPanel(Dialog parent, HashMap<String, String> properties, InputValidator validator) {
        this.parent = parent;
        this.validator = validator;

        initComponents();
        setData(properties);
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }

    private void initComponents() {

        serverUrisValidationRule = new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                if (serverUriTextField.getText().trim().isEmpty()) {
                    return "Server URI can't be empty. Correct format is servername:port,servername2:port2.";
                }

                String[] serverList = serverUriTextField.getText().split(",");
                for (String server : serverList) {
                    if (server.split(":").length != 2) {
                        return "Server URI not specified in correct format. Correct format is servername:port,servername2:port2.";
                    }
                }
                return null;
            }
        };
        validator.addRule(serverUrisValidationRule);

        clusterCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (clusterCheckBox.isSelected()) {
                    passwordField.setEnabled(false);
                } else {
                    passwordField.setEnabled(true);
                }
            }
        });

        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                onAdd();
            }
        });
        editButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                onEdit();
            }
        });
        removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                onRemove();
            }
        });

        additionalPropertiesTableModel = TableUtil.configureTable(
                additionalPropertiesTable,
                TableUtil.column("Name", 50, 400, 100000, property("key"), String.class),
                TableUtil.column("Value", 50, 400, 100000, property("value"), String.class)
        );

        additionalPropertiesTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        additionalPropertiesTable.getTableHeader().setReorderingAllowed(false);
        additionalPropertiesTable.getTableHeader().setReorderingAllowed(false);
        final TableRowSorter<TableModel> sorter = new TableRowSorter<TableModel>(additionalPropertiesTableModel);
        sorter.setSortKeys(Arrays.asList(new RowSorter.SortKey(0, SortOrder.ASCENDING), new RowSorter.SortKey(1, SortOrder.ASCENDING)));
        additionalPropertiesTable.setRowSorter(sorter);
    }

    private void setData(HashMap<String, String> properties) {

        if (StringUtils.isNotBlank(properties.get(RedisRemoteCache.PROPERTY_SERVERS))) {
            serverUriTextField.setText(properties.get(RedisRemoteCache.PROPERTY_SERVERS));
        }
        if (StringUtils.isNotBlank(properties.get(RedisRemoteCache.PROPERTY_IS_CLUSTER))) {
            clusterCheckBox.setSelected(Boolean.parseBoolean(properties.get(RedisRemoteCache.PROPERTY_IS_CLUSTER)));
        }
        if (StringUtils.isNotBlank(properties.get(RedisRemoteCache.PROPERTY_PASSWORD))) {
            passwordField.setText(properties.get(RedisRemoteCache.PROPERTY_PASSWORD));
        }

        final java.util.List<NameValuePair> additionalProperties = new ArrayList<>();

        for (final String propertyName : properties.keySet()) {
            if (!propertyName.startsWith(RedisRemoteCache.PROPERTY_PASSWORD) && !propertyName.startsWith(RedisRemoteCache.PROPERTY_IS_CLUSTER) &&
                    !propertyName.startsWith(RedisRemoteCache.PROPERTY_SERVERS)) {
                additionalProperties.add(new NameValuePair(propertyName, properties.get(propertyName)));
            }
        }

        if (additionalProperties.isEmpty()) {
            additionalPropertiesTableModel.setRows(Collections.<NameValuePair>emptyList());
        } else {
            additionalPropertiesTableModel.setRows(additionalProperties);
        }
    }

    public HashMap<String, String> getData() {
        HashMap<String, String> properties = new HashMap<String, String>();
        properties.put(RedisRemoteCache.PROPERTY_SERVERS, serverUriTextField.getText());
        properties.put(RedisRemoteCache.PROPERTY_PASSWORD, String.valueOf(passwordField.getPassword()));
        properties.put(RedisRemoteCache.PROPERTY_IS_CLUSTER, String.valueOf(clusterCheckBox.isSelected()));

        for (final Map.Entry<String, String> entry : additionalPropertiesTableModel.getRows()) {
            properties.put(entry.getKey(), entry.getValue());
        }

        return properties;
    }

    public void removeValidators() {
        validator.removeRule(serverUrisValidationRule);
    }

    private static Functions.Unary<String, NameValuePair> property(final String propName) {
        return Functions.propertyTransform(NameValuePair.class, propName);
    }

    private void editProperty(final NameValuePair propertyValue) {
        final SimplePropertyDialog dlg = propertyValue == null ?
                new SimplePropertyDialog(parent) :
                new SimplePropertyDialog(parent, new Pair<>(propertyValue.getKey(), propertyValue.getValue()));
        dlg.setTitle("Apache GenericObjectPool Properties");
        dlg.pack();
        Utilities.centerOnParentWindow(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            /** @noinspection unchecked*/
            @Override
            public void run() {
                if (dlg.isConfirmed()) {
                    final Pair<String, String> property = dlg.getData();
                    for (final NameValuePair pair : new ArrayList<>(additionalPropertiesTableModel.getRows())) {
                        if (pair.getKey().equals(property.left)) {
                            additionalPropertiesTableModel.removeRow(pair);
                        }
                    }
                    if (property != null) {
                        additionalPropertiesTableModel.removeRow(propertyValue);
                    }
                    additionalPropertiesTableModel.addRow(new NameValuePair(property.left, property.right));
                }
            }
        });
    }

    private void onRemove() {
        final int viewRow = additionalPropertiesTable.getSelectedRow();
        if (viewRow > -1) {
            additionalPropertiesTableModel.removeRowAt(additionalPropertiesTable.convertRowIndexToModel(viewRow));
        }
    }

    private void onAdd() {
        editProperty(null);
    }

    private void onEdit() {
        final int viewRow = additionalPropertiesTable.getSelectedRow();
        if (viewRow > -1) {
            final int modelRow = additionalPropertiesTable.convertRowIndexToModel(viewRow);
            editProperty(additionalPropertiesTableModel.getRowObject(modelRow));
        }
    }
}