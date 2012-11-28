package com.l7tech.external.assertions.createroutingstrategy.console;

import com.l7tech.common.io.failover.FailoverStrategyEditor;
import com.l7tech.console.panels.SimplePropertyDialog;
import com.l7tech.gui.SimpleTableModel;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.TableUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.util.Functions;
import com.l7tech.util.NameValuePair;
import com.l7tech.util.Pair;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class RoutingStrategyConfigurationDialog extends FailoverStrategyEditor {
    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.external.assertions.createroutingstrategy.console.resources.CreateRoutingStrategyResources");
    private static final String NAME = "failover.configure.name";
    private static final String VALUE = "failover.configure.value";
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JButton addPropertyButton;
    private JButton deletePropertyButton;
    private JButton editPropertyButton;
    private JTable propertyTable;
    private SimpleTableModel<NameValuePair> propertiesTableModel;

    public RoutingStrategyConfigurationDialog(final Frame parent, String title,  Map<String, String> properties) {
        super(parent, properties);
        setTitle(title);
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

// call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

// call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        initComponents();
    }

    private void initComponents() {
        propertiesTableModel = TableUtil.configureTable(
                propertyTable,
                TableUtil.column(resources.getString(NAME), 50, 100, 100000, property("key"), String.class),
                TableUtil.column(resources.getString(VALUE), 50, 100, 100000, property("value"), String.class)
        );
        propertyTable.getSelectionModel().setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
        propertyTable.getTableHeader().setReorderingAllowed( false );

        addPropertyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                editProperty(null);
            }
        });

        editPropertyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final int viewRow = propertyTable.getSelectedRow();
                if(viewRow > -1) {
                    editProperty(propertiesTableModel.getRowObject(propertyTable.convertRowIndexToModel(viewRow)));
                }
            }
        });

        deletePropertyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final int viewRow = propertyTable.getSelectedRow();
                if (viewRow > -1) {
                    propertiesTableModel.removeRowAt(propertyTable.convertRowIndexToModel(viewRow));
                }
            }
        });

        setData();
    }

    private void editProperty(final NameValuePair nameValuePair) {
        final SimplePropertyDialog dlg = nameValuePair == null ?
                new SimplePropertyDialog((Frame)getParent()) :
                new SimplePropertyDialog((Frame) getParent(), new Pair<String,String>( nameValuePair.getKey(), nameValuePair.getValue() ) );
        dlg.pack();
        Utilities.centerOnParentWindow(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            @Override
            public void run() {
                if (dlg.isConfirmed()) {
                    final Pair<String, String> property = dlg.getData();
                    for (final NameValuePair pair : new ArrayList<NameValuePair>(propertiesTableModel.getRows())) {
                        if (pair.getKey().equals(property.left)) {
                            propertiesTableModel.removeRow(pair);
                        }
                    }
                    if (nameValuePair != null) propertiesTableModel.removeRow(nameValuePair);

                    propertiesTableModel.addRow(new NameValuePair(property.left, property.right));
                }
            }
        });
    }
    
    private void setData() {
        if(properties != null){
            List<NameValuePair> propList = new ArrayList<NameValuePair>(properties.size());
            for(Map.Entry<String, String> entry : properties.entrySet()){
                NameValuePair pair = new NameValuePair(entry.getKey(), entry.getValue());
                propList.add(pair);
            }
            propertiesTableModel.setRows(propList);
        }
    }

    private static Functions.Unary<String,NameValuePair> property(final String propName) {
        return Functions.propertyTransform(NameValuePair.class, propName);
    }

    private void onOK() {
        List<NameValuePair> props = propertiesTableModel.getRows();
        properties.clear();
        for (Iterator<NameValuePair> iterator = props.iterator(); iterator.hasNext(); ) {
            NameValuePair next =  iterator.next();
            properties.put(next.getKey().trim(), next.getValue().trim());
        }
        dispose();
    }

    private void onCancel() {
        dispose();
    }
}
