package com.l7tech.external.assertions.ssh.console;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.console.logging.ErrorManager;
import com.l7tech.console.util.EntityUtils;
import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.transport.SsgActiveConnector;
import com.l7tech.gateway.common.transport.TransportAdmin;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.*;
import com.l7tech.util.ExceptionUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.external.assertions.ssh.SftpPollingListenerConstants.SFTP_POLLING_DEFAULT_PORT;
import static com.l7tech.gateway.common.transport.SsgActiveConnector.*;
import static com.l7tech.gui.util.DialogDisplayer.display;
import static com.l7tech.gui.util.DialogDisplayer.showMessageDialog;

/**
 * SFTP polling listeners dialog window.
 */
public class SftpPollingListenersWindow extends JDialog {
    private static final Logger logger = Logger.getLogger( SftpPollingListenersWindow.class.getName() );

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

    private void initialize() {
        tableModel = new ListenerConfigurationsTableModel();
        listenersTable.setModel(tableModel);
        listenersTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listenersTable.setColumnSelectionAllowed(false);

        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final SsgActiveConnector connector = new SsgActiveConnector();
                connector.setEnabled( true );
                connector.setProperty( PROPERTIES_KEY_ENABLE_RESPONSE_MESSAGES, Boolean.TRUE.toString() );
                connector.setProperty( PROPERTIES_KEY_OVERRIDE_CONTENT_TYPE, ContentTypeHeader.XML_DEFAULT.getFullValue() );
                connector.setProperty( PROPERTIES_KEY_SFTP_PORT, SFTP_POLLING_DEFAULT_PORT );
                connector.setType( ACTIVE_CONNECTOR_TYPE_SFTP );
                editAndSave(connector, false);
            }
        });

        cloneButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SsgActiveConnector selectedConnector = tableModel.getSelectedConnector(listenersTable);
                if(selectedConnector != null) {
                    final SsgActiveConnector connector =  new SsgActiveConnector(selectedConnector);
                    EntityUtils.updateCopy( connector );
                    editAndSave( connector, true );
                }
            }
        });

        modifyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SsgActiveConnector connector = tableModel.getSelectedConnector(listenersTable);
                if( connector != null) {
                    editAndSave( connector, false );
                } else {
                    // the listener has been removed some how
                    showMessageDialog(SftpPollingListenersWindow.this, "SFTP polling listener configuration not found.", null);
                    updateListenersList( null );
                }
            }
        });

        removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SsgActiveConnector configuration = tableModel.getSelectedConnector(listenersTable);
                if (configuration != null) {
                    String name = configuration.getName();

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
                            deleteConfiguration(configuration);
                        } catch (Exception e1) {
                            throw new RuntimeException("Unable to delete SFTP polling listener " + name, e1);
                        }
                        updateListenersList(null);
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

        Utilities.setEscKeyStrokeDisposes(this);
        Utilities.setDoubleClickAction(listenersTable, modifyButton);
        Utilities.setRowSorter(listenersTable, tableModel, new int[]{0,1,2}, new boolean[]{true,true,true}, null);
        Utilities.setMinimumSize(this);

        setContentPane(mainPanel);
        pack();
    }

    private void editAndSave( final SsgActiveConnector connector,
                              final boolean isClone) {
        final SftpPollingListenerPropertiesDialog dialog =
            new SftpPollingListenerPropertiesDialog(SftpPollingListenersWindow.this, connector);
        if (isClone) dialog.selectNameField();
        Utilities.centerOnParentWindow( dialog );
        display(dialog, new Runnable() {
            @Override
            public void run() {
                if (dialog.isConfirmed()) {
                    final TransportAdmin admin = getTransportAdmin();
                    if ( admin != null ) {
                        boolean duplicate = false;
                        try {
                            admin.saveSsgActiveConnector( connector );
                        } catch ( DuplicateObjectException e ) {
                            duplicate = true;
                        } catch ( SaveException e ) {
                            ErrorManager.getDefault().notify( Level.WARNING, e, "Error saving Polling Listener" );
                        } catch ( UpdateException e ) {
                            if ( ExceptionUtils.causedBy( e, DuplicateObjectException.class ) ) {
                                duplicate = true;
                            } else {
                                ErrorManager.getDefault().notify( Level.WARNING, e, "Error saving Polling Listener" );
                            }
                        }

                        if ( duplicate ) {
                            showMessageDialog( SftpPollingListenersWindow.this,
                                    "Listener already exists",
                                    "Unable to save listener '" + connector.getName() + "'\n" +
                                    "because an existing listener is already using the name.",
                                    null,
                                    new Runnable() {
                                        @Override
                                        public void run() {
                                            updateListenersList( connector );
                                            editAndSave( connector, false );
                                        }
                                    } );
                        } else {
                            updateListenersList( connector );
                        }
                    }
                }
            }
        });
    }

    private class ListenerConfigurationsTableModel extends AbstractTableModel {
        private static final long serialVersionUID = 1L;
        private List<SsgActiveConnector> listenerConfigurations = loadListenerConfigurations();

        @Override
        public int getColumnCount() {
            return 3;
        }

        @Override
        public int getRowCount() {
            if( getConnectors() == null) {
                return 0;
            } else {
                return getConnectors().size();
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
            final SsgActiveConnector connector = listenerConfigurations.get(rowIndex);

            switch (columnIndex) {
                case 0:
                    return connector.getName();
                case 1:
                    return "sftp://" + connector.getProperty(PROPERTIES_KEY_SFTP_USERNAME) + "@" +
                            connector.getProperty(PROPERTIES_KEY_SFTP_HOST) +
                            connector.getProperty(PROPERTIES_KEY_SFTP_DIRECTORY);
                case 2:
                    return connector.isEnabled() ? "Yes" : "No";
            }
            return "?";
        }

        public List<SsgActiveConnector> getConnectors() {
            return listenerConfigurations;
        }

        public SsgActiveConnector getSelectedConnector(JTable listenersTable) {
            SsgActiveConnector selectedConnector = null;
            int viewRow = listenersTable.getSelectedRow();
            if(viewRow > -1) {
                int modelRow = listenersTable.convertRowIndexToModel(viewRow);
                selectedConnector = tableModel.getConnectors().get(modelRow);
            }
            return selectedConnector;
        }

        public void refreshListenerConfigurationsList() {
            listenerConfigurations = loadListenerConfigurations();
        }
    }

    /**
     * Rebuild the listener table model, reloading the list from the server.  If an listener argument is
     * given, the row containing the specified configuration will be selected in the new table.
     * @param selectedConfiguration the selected configuration
     */
    private void updateListenersList(@Nullable SsgActiveConnector selectedConfiguration) {
        // Update the listener configuration list in the table model.
        tableModel.refreshListenerConfigurationsList();

        // Update the GUI of the table.
        tableModel.fireTableDataChanged();

        // Set the selection highlight for the saved/updated configuration.
        if (selectedConfiguration != null) {
            List<SsgActiveConnector> modelRows = tableModel.getConnectors();
            for (int i = 0; i < modelRows.size(); ++i) {
                SsgActiveConnector modelItem = modelRows.get(i);
                if (modelItem != null && modelItem.getGoid().equals(selectedConfiguration.getGoid())) {
                    int viewRow = listenersTable.convertRowIndexToView(i);
                    if (viewRow >= 0) {
                        listenersTable.getSelectionModel().setSelectionInterval(viewRow, viewRow);
                    }
                }
            }
        }
    }

    private TransportAdmin getTransportAdmin() {
        final Registry registry = Registry.getDefault();
        if (!registry.isAdminContextPresent()) {
            logger.warning("Admin context not present.");
            return null;
        }
        return registry.getTransportAdmin();
    }

    /*
     * Load list of listener configurations from the server.
     */
    private List<SsgActiveConnector> loadListenerConfigurations() {
        List<SsgActiveConnector> listenerConfigurations = Collections.emptyList();
        try {
            final TransportAdmin transportAdmin = getTransportAdmin();
            if ( transportAdmin != null ) {
                listenerConfigurations = new ArrayList<SsgActiveConnector>(
                        transportAdmin.findSsgActiveConnectorsByType( ACTIVE_CONNECTOR_TYPE_SFTP ));
            }
        } catch (FindException e) {
            throw new RuntimeException( e );
        }

        return listenerConfigurations;
    }

    private void deleteConfiguration( final SsgActiveConnector configuration ) throws FindException, DeleteException {
        final TransportAdmin transportAdmin = getTransportAdmin();
        if (transportAdmin != null) {
            transportAdmin.deleteSsgActiveConnector( configuration.getGoid() );
        }
    }

    private void enableOrDisableButtons() {
        boolean enabled = listenersTable.getSelectedRow() >= 0;
        removeButton.setEnabled(enabled);
        modifyButton.setEnabled(enabled);
        cloneButton.setEnabled(enabled);
    }
}
