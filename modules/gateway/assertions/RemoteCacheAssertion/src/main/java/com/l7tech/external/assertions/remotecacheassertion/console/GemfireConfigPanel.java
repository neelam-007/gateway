package com.l7tech.external.assertions.remotecacheassertion.console;

import com.l7tech.console.panels.SimplePropertyDialog;
import com.l7tech.external.assertions.remotecacheassertion.server.GemfireRemoteCache;
import com.l7tech.gui.SimpleTableModel;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.TableUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.util.Functions;
import com.l7tech.util.Pair;

import javax.swing.*;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: nilic
 * Date: 5/31/13
 * Time: 10:11 AM
 * To change this template use File | Settings | File Templates.
 */
public class GemfireConfigPanel implements RemoteCacheConfigPanel {

    private JPanel mainPanel;
    private JTextField cacheNameField;
    private JTextField serverUriTextField;
    private JTabbedPane tabbedPanel;
    private JTable environmentPropertiesTable;
    private JButton addEnvironmentButton;
    private JButton editEnvironmentButton;
    private JButton removeEnvironmentButton;
    private JRadioButton locatorRadioButton;
    private JRadioButton serverRadioButton;

    private Dialog parent;
    private InputValidator validator;
    private SimpleTableModel<NameValuePair> environmentPropertiesTableModel;

    private InputValidator.ValidationRule cacheNameValidationRule;
    private InputValidator.ValidationRule serverUrisValidationRule;

    public GemfireConfigPanel(Dialog parent, HashMap<String, String> properties, InputValidator validator) {
        this.parent = parent;
        this.validator = validator;

        initComponents();
        setData(properties);
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }

    private void initComponents() {
        cacheNameValidationRule = validator.constrainTextFieldToBeNonEmpty("cache name", cacheNameField, null);

        serverUrisValidationRule = new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {

            if (serverUriTextField.getText().trim().isEmpty()){
                return "Server URI can't be empty. Correct format is servername:port,servername2:port2.";
            }

            String[] serverList = serverUriTextField.getText().split(",");
            for(String server : serverList) {
               if (server.split(":").length != 2) {
                return "Server URI not specified in correct format. Correct format is servername:port,servername2:port2.";
               }
            }
            return null;
            }
        };
        validator.addRule(serverUrisValidationRule);

        addEnvironmentButton.addActionListener( new ActionListener(){
            @Override
            public void actionPerformed( final ActionEvent e ) {
                onEnvironmentAdd();
            }
        } );
        editEnvironmentButton.addActionListener( new ActionListener(){
            @Override
            public void actionPerformed( final ActionEvent e ) {
                onEnvironmentEdit();
            }
        } );
        removeEnvironmentButton.addActionListener( new ActionListener(){
            @Override
            public void actionPerformed( final ActionEvent e ) {
                onEnvironmentRemove();
            }
        } );

        environmentPropertiesTableModel = TableUtil.configureTable(
                environmentPropertiesTable,
                TableUtil.column("Name", 50, 400, 100000, property("key"), String.class),
                TableUtil.column("Value", 50, 400, 100000, property("value"), String.class)
        );

        environmentPropertiesTable.getSelectionModel().setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
        environmentPropertiesTable.getTableHeader().setReorderingAllowed( false );
        environmentPropertiesTable.getTableHeader().setReorderingAllowed(false);
        final TableRowSorter<TableModel> sorter = new TableRowSorter<TableModel>( environmentPropertiesTableModel );
        sorter.setSortKeys( Arrays.asList(new RowSorter.SortKey(0, SortOrder.ASCENDING), new RowSorter.SortKey(1, SortOrder.ASCENDING)) );
        environmentPropertiesTable.setRowSorter(sorter);

        locatorRadioButton.setEnabled(true);
        serverRadioButton.setEnabled(true);
    }

    private void setData(HashMap<String, String> properties) {

        cacheNameField.setText(properties.get(GemfireRemoteCache.PROPERTY_CACHE_NAME) == null ? "" : properties.get(GemfireRemoteCache.PROPERTY_CACHE_NAME));

        if(properties.get(GemfireRemoteCache.PROPERTY_SERVERS) != null) {
            serverUriTextField.setText(properties.get(GemfireRemoteCache.PROPERTY_SERVERS));
        }
        final java.util.List<NameValuePair> environmentProperties = new ArrayList<NameValuePair>();

        for ( final String propertyName : properties.keySet()) {
            if (!propertyName.startsWith( GemfireRemoteCache.PROPERTY_CACHE_NAME ) &&
                    !propertyName.startsWith(GemfireRemoteCache.PROPERTY_SERVERS) &&
                    !propertyName.startsWith(GemfireRemoteCache.PROPERTY_CACHE_OPTION)) {
                environmentProperties.add( new NameValuePair( propertyName, properties.get(propertyName) ) );
            }
        }

        if (environmentProperties.isEmpty()){
            environmentPropertiesTableModel.setRows( Collections.<NameValuePair>emptyList() );
        } else {
            environmentPropertiesTableModel.setRows( environmentProperties );
        }
        
        if (properties.get(GemfireRemoteCache.PROPERTY_CACHE_OPTION) == null || properties.get(GemfireRemoteCache.PROPERTY_CACHE_OPTION).equals("locator")){
            locatorRadioButton.setSelected(true);
        } else if (properties.get(GemfireRemoteCache.PROPERTY_CACHE_OPTION).equals("server")) {
            serverRadioButton.setSelected(true);
        }
    }

    public HashMap<String, String> getData() {
        HashMap<String, String> properties = new HashMap<String, String>();
        properties.put(GemfireRemoteCache.PROPERTY_CACHE_NAME, cacheNameField.getText());
        properties.put(GemfireRemoteCache.PROPERTY_SERVERS, serverUriTextField.getText());

        for ( final Map.Entry<String,String> entry : environmentPropertiesTableModel.getRows() ) {
            properties.put(entry.getKey(), entry.getValue());
        }

        if (locatorRadioButton.isSelected()){
            properties.put(GemfireRemoteCache.PROPERTY_CACHE_OPTION, "locator");
        } else if (serverRadioButton.isSelected()){
            properties.put(GemfireRemoteCache.PROPERTY_CACHE_OPTION, "server");
        }

        return properties;
    }

    public void removeValidators() {
        validator.removeRule(cacheNameValidationRule);
        validator.removeRule(serverUrisValidationRule);
    }

    private static Functions.Unary<String,NameValuePair> property(final String propName) {
        return Functions.propertyTransform(NameValuePair.class, propName);
    }

    private static class NameValuePair extends AbstractMap.SimpleEntry<String,String>{
        NameValuePair( final String key, final String value ) {
            super( key, value );
        }
    }

    private void editEnvironmentProperty( final NameValuePair nameValuePair ) {
        final SimplePropertyDialog dlg = nameValuePair == null ?
                new SimplePropertyDialog(parent) :
                new SimplePropertyDialog(parent, new Pair<String,String>( nameValuePair.getKey(), nameValuePair.getValue() ) );
        dlg.pack();
        Utilities.centerOnParentWindow(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            /** @noinspection unchecked*/
            @Override
            public void run() {
                if (dlg.isConfirmed()) {
                    final Pair<String, String> property = dlg.getData();
                    for (final NameValuePair pair : new ArrayList<NameValuePair>(environmentPropertiesTableModel.getRows())) {
                        if (pair.getKey().equals(property.left)) {
                            environmentPropertiesTableModel.removeRow(pair);
                        }
                    }
                    if (nameValuePair != null) environmentPropertiesTableModel.removeRow(nameValuePair);

                    if (property.left.equals(GemfireRemoteCache.PROPERTY_LOG_FILE)) {
                        DialogDisplayer.showMessageDialog(parent, "GemFire Property Message", "You can't set external log file in Gateway", null);
                    } else {
                        environmentPropertiesTableModel.addRow(new NameValuePair(property.left, property.right));
                    }
                }
            }
        });

    }

    private void onEnvironmentRemove() {
        final int viewRow = environmentPropertiesTable.getSelectedRow();
        if ( viewRow > -1 ) {
            environmentPropertiesTableModel.removeRowAt( environmentPropertiesTable.convertRowIndexToModel(viewRow) );
        }
    }

    private void onEnvironmentAdd() {
        editEnvironmentProperty( null );
    }

    private void onEnvironmentEdit() {
        final int viewRow = environmentPropertiesTable.getSelectedRow();
        if ( viewRow > -1 ) {
            final int modelRow = environmentPropertiesTable.convertRowIndexToModel(viewRow);
            editEnvironmentProperty( environmentPropertiesTableModel.getRowObject(modelRow  ) );
        }
    }
}
