package com.l7tech.external.assertions.ssh.console;

import com.l7tech.console.util.EntityUtils;
import com.l7tech.console.util.Registry;
import com.l7tech.external.assertions.ssh.server.sftppollinglistener.SftpPollingListenerConstants;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.gateway.common.cluster.ClusterStatusAdmin;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.logging.Logger;

/**
 * SFTP polling listeners dialog window.
 */
public class SftpPollingListenersWindow extends JDialog {
    private static final Logger logger = Logger.getLogger(SftpPollingListenersWindow.class.getName());

    private JPanel mainPanel;
    private JTable listenersTable;
    private ListenerConfigurationsTableModel tableModel;
    private JButton addButton;
    private JButton modifyButton;
    private JButton removeButton;
    private JButton closeButton;
    private JButton cloneButton;

    public SftpPollingListenersWindow(Frame owner) {
        super(owner, "Manage SFTP Polling Listeners", true);
        initialize();
    }

    public SftpPollingListenersWindow(Dialog owner) {
        super(owner, "Manage SFTP Polling Listeners", true);
        initialize();
    }

    private void initialize() {
        loadListenerConfigurations();
        tableModel = new ListenerConfigurationsTableModel();
        listenersTable.setModel(tableModel);
        listenersTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listenersTable.setColumnSelectionAllowed(false);

        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final SftpPollingListenerPropertiesDialog dialog = new SftpPollingListenerPropertiesDialog(SftpPollingListenersWindow.this, new SftpPollingListenerDialogSettings(), true);
                Utilities.centerOnScreen(dialog);
                DialogDisplayer.display(dialog, new Runnable() {
                    @Override
                    public void run() {
                        if(dialog.isConfirmed()) {
                            updateListenersList(dialog.getConfiguration());
                        }
                    }
                });
            }
        });

        cloneButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(listenersTable.getSelectedRow() > -1) {
                    int viewRow = listenersTable.getSelectedRow();
                    if(viewRow >= 0) {
                        int modelRow = listenersTable.convertRowIndexToModel(viewRow);
                        SftpPollingListenerDialogSettings i = tableModel.getListenerConfigurations().get(modelRow);
                        if(i != null) {
                            SftpPollingListenerDialogSettings newListener =  new SftpPollingListenerDialogSettings();
                            i.copyPropertiesToResource(newListener);
                            newListener.setName(EntityUtils.getNameForCopy( newListener.getName() ));
                            newListener.setVersion( 0 );

                            final SftpPollingListenerPropertiesDialog dialog =
                                new SftpPollingListenerPropertiesDialog(SftpPollingListenersWindow.this, newListener, true);
                            dialog.selectNameField();
                            Utilities.centerOnScreen(dialog);
                            DialogDisplayer.display(dialog, new Runnable() {
                                @Override
                                public void run() {
                                    if (dialog.isConfirmed()) {
                                        updateListenersList(dialog.getConfiguration());
                                    }
                                }
                            });

                        }
                    }
                }
            }
        });
        modifyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(listenersTable.getSelectedRow() > -1) {
                    int viewRow = listenersTable.getSelectedRow();
                    if(viewRow >= 0) {
                        int modelRow = listenersTable.convertRowIndexToModel(viewRow);
                        SftpPollingListenerDialogSettings i = tableModel.getListenerConfigurations().get(modelRow);
                        if(i != null) {
                            //grab the latest version from the list
                            long connectionOid = i.getResId();

                            //Here need to find the connection using the resId from the cluster property
                            SftpPollingListenerDialogSettings configuration = i;
                            if(configuration == null) {
                                // the listener has been removed some how
                                DialogDisplayer.showMessageDialog(SftpPollingListenersWindow.this, "SFTP polling listener configuration not found.", refreshListenerConfigurations(null));
                            } else {
                                final SftpPollingListenerPropertiesDialog dialog =
                                    new SftpPollingListenerPropertiesDialog(SftpPollingListenersWindow.this, i, false);
                                Utilities.centerOnScreen(dialog);
                                DialogDisplayer.display(dialog, refreshListenerConfigurations(configuration)); //refresh after any changes
                            }
                        }
                    }
                }
            }
        });

        removeButton.addActionListener(new ActionListener() {
            @Override
                public void actionPerformed(ActionEvent e) {
                    int viewRow = listenersTable.getSelectedRow();
                    if (viewRow >= 0) {
                        int modelRow = listenersTable.convertRowIndexToModel(viewRow);
                        SftpPollingListenerDialogSettings i = tableModel.getListenerConfigurations().get(modelRow);
                        if (i != null) {
                            String name = i.getName();

                            Object[] options = {"Remove", "Cancel"};

                            int result = JOptionPane.showOptionDialog(null,
                              "<HTML>Are you sure you want to remove the " +
                              "configuration for the SFTP polling listener " +
                              name + "?<br>" +
                              "<center>This action cannot be undone." +
                              "</center></html>",
                              "Remove SFTP polling listener?",
                              0, JOptionPane.WARNING_MESSAGE,
                              null, options, options[1]);
                            if (result == 0) {
                                try {
                                    deleteResourceFromClusterProperty(i);
                                } catch (Exception e1) {
                                    throw new RuntimeException("Unable to delete SFTP polling listener " + name, e1);
                                }
                                updateListenersList(null);
                            }
                        }
                    }
                }
        });

        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        listenersTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                enableOrDisableButtons();
            }
        });

        enableOrDisableButtons();

        Utilities.setDoubleClickAction(listenersTable, modifyButton);
        Utilities.setRowSorter(listenersTable, tableModel, new int[]{0,1,2}, new boolean[]{true,true,true}, null);
        Utilities.setMinimumSize(this);

        setContentPane(mainPanel);
        pack();
    }

    private class ListenerConfigurationsTableModel extends AbstractTableModel {
        private List<SftpPollingListenerDialogSettings> listenerConfigurations = loadListenerConfigurations();

        @Override
        public int getColumnCount() {
            return 3;
        }

        @Override
        public int getRowCount() {
            if(getListenerConfigurations() == null) {
                return 0;
            } else {
                return getListenerConfigurations().size();
            }
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
                case 0:
                    return "Name";
                case 1:
                    return "Connection URI";
                case 2:
                    return "Enabled";
            }
            return "?";
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            SftpPollingListenerDialogSettings i = listenerConfigurations.get(rowIndex);
            
            switch (columnIndex) {
                case 0:
                    return i.getName();
                case 1:
                    return "sftp://" + i.getUsername() + "@" + i.getHostname() + i.getDirectory();
                case 2:
                    return i.isActive() ? "Yes" : "No";
            }
            return "?";
        }

        public List<SftpPollingListenerDialogSettings> getListenerConfigurations() {
            return listenerConfigurations;
        }

        public void refreshListenerConfigurationsList() {
            listenerConfigurations = loadListenerConfigurations();
        }
    }

    /**
     * Generate a runnable object to refresh listener in the table.
     *
     * @param selectedConfiguration: the listener saved or updated will be highlighted in the table.
     * @return  A runnable that will refresh the configuration list
     */
    private Runnable refreshListenerConfigurations(final SftpPollingListenerDialogSettings selectedConfiguration) {
        return new Runnable() {
            @Override
            public void run() {
                updateListenersList(selectedConfiguration);
            }
        };
    }

    /**
     * Rebuild the listener table model, reloading the list from the server.  If an listener argument is
     * given, the row containing the specified configuration will be selected in the new table.
     */
    private void updateListenersList(SftpPollingListenerDialogSettings selectedConfiguration) {
        // Update the listener configuration list in the table model.
        tableModel.refreshListenerConfigurationsList();

        // Update the GUI of the table.
        tableModel.fireTableDataChanged();

        // Set the selection highlight for the saved/updated configuration.
        if (selectedConfiguration != null) {
            List<SftpPollingListenerDialogSettings> rows = tableModel.getListenerConfigurations();
            for (int i = 0; i < rows.size(); ++i) {
                SftpPollingListenerDialogSettings item = rows.get(i);
                if (item != null && item.getResId() == selectedConfiguration.getResId()) {
                    int viewRow = listenersTable.convertRowIndexToView(i);
                    if (viewRow >= 0) {
                        listenersTable.getSelectionModel().setSelectionInterval(viewRow, viewRow);
                    }
                    break;
                }
            }
        }
    }

    private List<SftpPollingListenerDialogSettings> loadListenerConfigurations() {
        List<SftpPollingListenerDialogSettings> listenerConfigurations = null;

        ClusterStatusAdmin admin = Registry.getDefault().getClusterStatusAdmin();
        ClusterProperty property = null;
        try {
            property = admin.findPropertyByName(SftpPollingListenerConstants.SFTP_POLLING_CONFIGURATION_UI_PROPERTY);
        } catch(FindException fe) {
            logger.warning("Could not find any cluster properties for property >" + SftpPollingListenerConstants.SFTP_POLLING_CONFIGURATION_UI_PROPERTY + "<");
        }

        if(property != null) {
            // load the resources from the property string
            final ClassLoader currentContextClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(SftpPollingListenerPropertiesDialog.class.getClassLoader());

                SftpPollingListenerXmlUtilities xmlUtil = new SftpPollingListenerXmlUtilities();
                listenerConfigurations = xmlUtil.unmarshallFromXMLString(property.getValue());
            } finally {
                Thread.currentThread().setContextClassLoader(currentContextClassLoader);
            }
        }

        return listenerConfigurations;
    }

    private void saveClusterPropertyList(List<SftpPollingListenerDialogSettings> list) {
        ClusterStatusAdmin admin = Registry.getDefault().getClusterStatusAdmin();
        boolean clusterPropExists = false;
        //existing connections in the cluster properties
        ClusterProperty property = getClusterProperty();
        if(property != null)
            clusterPropExists = true;

        //if the list passed in is null, delete the cluster property.
        //otherwise, marshall the list and write the property.
        if(list == null || list.size()==0) {
            if(clusterPropExists){
                try {
                    admin.deleteProperty(property);
                } catch(DeleteException de){
                    logger.warning("Could not delete cluster property " + SftpPollingListenerConstants.SFTP_POLLING_CONFIGURATION_UI_PROPERTY + " most likely does not exist");
                }
            }
        } else {
            String xml = marshallToXMLClassLoaderSafe(list);
            if(property != null) {
                property.setValue(xml);
            } else {
                property = new ClusterProperty(SftpPollingListenerConstants.SFTP_POLLING_CONFIGURATION_UI_PROPERTY, xml);
            }
            try {
                admin.saveProperty(property);
            } catch(Exception e) {
                logger.warning("Could not save the cluster property " + SftpPollingListenerConstants.SFTP_POLLING_CONFIGURATION_UI_PROPERTY);
            }
        }
    }

    private String marshallToXMLClassLoaderSafe(List<SftpPollingListenerDialogSettings> listenerConfigurations) {
          String xml = null;
            final ClassLoader currentContextClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(SftpPollingListenerPropertiesDialog.class.getClassLoader());

                SftpPollingListenerXmlUtilities xmlUtil = new SftpPollingListenerXmlUtilities();
                xml = xmlUtil.marshallToXMLString(listenerConfigurations);
            } finally {
                Thread.currentThread().setContextClassLoader(currentContextClassLoader);
            }
        return xml;
    }

    private ClusterProperty getClusterProperty(){
        ClusterStatusAdmin admin = Registry.getDefault().getClusterStatusAdmin();
        ClusterProperty property = null;
        try {
            property = admin.findPropertyByName(SftpPollingListenerConstants.SFTP_POLLING_CONFIGURATION_UI_PROPERTY);
        } catch(FindException fe) {
            logger.warning("Could not find any cluster properties for property >" + SftpPollingListenerConstants.SFTP_POLLING_CONFIGURATION_UI_PROPERTY + "<");
        }
        return property;
    }

    private void deleteResourceFromClusterProperty(SftpPollingListenerDialogSettings resource) {
        List<SftpPollingListenerDialogSettings> list = loadListenerConfigurations();

        for(int i = 0; i < list.size(); i++) {
            SftpPollingListenerDialogSettings configuration = list.get(i);
            if(configuration.getResId() == resource.getResId()) {
                //remove it from the list.
                list.remove(i);
                break;
            }
        }

        saveClusterPropertyList(list);
    }

    private void enableOrDisableButtons() {
        boolean enabled = listenersTable.getSelectedRow() >= 0;
        removeButton.setEnabled(enabled);
        modifyButton.setEnabled(enabled);
        cloneButton.setEnabled(enabled);
    }
}
