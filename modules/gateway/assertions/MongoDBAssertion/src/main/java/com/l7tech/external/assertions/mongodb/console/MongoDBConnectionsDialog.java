package com.l7tech.external.assertions.mongodb.console;

import com.l7tech.console.util.Registry;
import com.l7tech.external.assertions.mongodb.entity.MongoDBConnectionEntity;
import com.l7tech.external.assertions.mongodb.entity.MongoDBConnectionEntityAdmin;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.LoggingAudit;
import com.l7tech.gateway.common.cluster.ClusterStatusAdmin;
import com.l7tech.gui.SimpleTableModel;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.TableUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.util.Functions;

import javax.swing.*;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MongoDBConnectionsDialog extends JDialog {
    private static final Logger logger = Logger.getLogger(MongoDBConnectionsDialog.class.getName());

    private JPanel mainPanel;
    private SimpleTableModel<MongoDBConnectionEntity> serversTableModel;
    private TableRowSorter<SimpleTableModel<MongoDBConnectionEntity>> rowSorter;
    private JTable serversTable;
    private JButton addServerButton;
    private JButton editButton;
    private JButton removeButton;
    private JButton closeButton;

    private ClusterStatusAdmin clusterStatusAdmin;

    private static final String TITLE = "MongoDB Connections";

    public MongoDBConnectionsDialog(Frame parent) {
        super(parent, TITLE, true);

        clusterStatusAdmin = Registry.getDefault().getClusterStatusAdmin();

        initComponents();
    }

    private void initComponents() {
        final RunOnChangeListener enableDisableListener = new RunOnChangeListener() {
            @Override
            protected void run() {
                enableDisableComponents();
            }
        };

        Utilities.setEscKeyStrokeDisposes(this);
        serversTableModel = buildServersTableModel();
        serversTable.setModel(serversTableModel);
        serversTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        serversTable.getSelectionModel().addListSelectionListener(enableDisableListener);
        Utilities.setRowSorter(serversTable, serversTableModel, new int[] {0}, new boolean[] {true}, new Comparator[]{String.CASE_INSENSITIVE_ORDER});
        rowSorter = (TableRowSorter<SimpleTableModel<MongoDBConnectionEntity>>)serversTable.getRowSorter();
        rowSorter.setSortsOnUpdates(true);

        addServerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                MongoDBConnectionEntity newMongoDBConnection = new MongoDBConnectionEntity();
                MongoDBConnectionDialog dialog = new MongoDBConnectionDialog(MongoDBConnectionsDialog.this, newMongoDBConnection);
                Utilities.centerOnParentWindow(dialog);
                dialog.setVisible(true);
                if (dialog.isConfirmed()) {
                    newMongoDBConnection = dialog.getData(newMongoDBConnection);
                    if(newMongoDBConnection != null) {
                        try {
                            MongoDBConnectionEntityAdmin connectionEntityAdmin = Registry.getDefault().getExtensionInterface(MongoDBConnectionEntityAdmin.class, null);
                            connectionEntityAdmin.save(newMongoDBConnection);

                            loadServers();
                        } catch(SaveException se) {
                            JOptionPane.showMessageDialog(MongoDBConnectionsDialog.this, "Failed to save new MongoDB connection.", "Error", JOptionPane.ERROR_MESSAGE);
                        } catch(UpdateException ue) {
                            JOptionPane.showMessageDialog(MongoDBConnectionsDialog.this, "Failed to save new MongoDB connection.", "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
            }
        });

        editButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                MongoDBConnectionEntity mongoDBConnectionEntity = serversTableModel.getRowObject(rowSorter.convertRowIndexToModel(serversTable.getSelectedRow()));
                MongoDBConnectionDialog dialog = new MongoDBConnectionDialog(MongoDBConnectionsDialog.this, mongoDBConnectionEntity, true);
                Utilities.centerOnParentWindow(dialog);
                dialog.setVisible(true);
                if(dialog.isConfirmed()) {
                    mongoDBConnectionEntity = dialog.getData(mongoDBConnectionEntity);
                    if(mongoDBConnectionEntity != null) {
                        try {
                            MongoDBConnectionEntityAdmin mongoDBConnectionEntityAdmin = Registry.getDefault().getExtensionInterface(MongoDBConnectionEntityAdmin.class, null);
                            mongoDBConnectionEntityAdmin.update(mongoDBConnectionEntity);
                            loadServers();
                        } catch (Exception ex) {
                            logger.warning("Unable to update mongoDBConnectionEntityAdmin " + ex.getMessage());
                        }
                    }
                }
            }
        });

        removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doRemove();
            }
        });

        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });

        setContentPane(mainPanel);
        pack();

        loadServers();
        enableDisableComponents();
    }

    private void enableDisableComponents() {
        final int[] selectedRows = serversTable.getSelectedRows();
        editButton.setEnabled(selectedRows.length == 1);
        removeButton.setEnabled(selectedRows.length > 0);
    }

    private SimpleTableModel<MongoDBConnectionEntity> buildServersTableModel() {
        return TableUtil.configureTable(
                serversTable,
                TableUtil.column("Connection Name", 40, 200, 1000000, new Functions.Unary<String, MongoDBConnectionEntity>() {
                    @Override
                    public String call(MongoDBConnectionEntity mongoDBConnection) {
                        return mongoDBConnection.getName();
                    }
                }, String.class),
                TableUtil.column("Server", 40, 100, 180, new Functions.Unary<String, MongoDBConnectionEntity>() {
                    @Override
                    public String call(MongoDBConnectionEntity mongoDBConnection) {
                        return mongoDBConnection.getUri();
                    }
                }, String.class),
                TableUtil.column("Database", 40, 100, 180, new Functions.Unary<String, MongoDBConnectionEntity>() {
                    @Override
                    public String call(MongoDBConnectionEntity mongoDBConnection) {
                        return mongoDBConnection.getDatabaseName();
                    }
                }, String.class),
                TableUtil.column("Port", 40, 100, 180, new Functions.Unary<String, MongoDBConnectionEntity>() {
                    @Override
                    public String call(MongoDBConnectionEntity mongoDBConnection) {
                        return mongoDBConnection.getPort();
                    }
                }, String.class),
                TableUtil.column("User Name", 40, 100, 180, new Functions.Unary<String, MongoDBConnectionEntity>() {
                    @Override
                    public String call(MongoDBConnectionEntity mongoDBConnection) {
                        return mongoDBConnection.getUsername();
                    }
                }, String.class)
        );
    }

    private void loadServers() {
        try {
            MongoDBConnectionEntityAdmin mongoDBConnectionEntityAdmin = Registry.getDefault().getExtensionInterface(MongoDBConnectionEntityAdmin.class, null);
            Collection<MongoDBConnectionEntity> mongoDBConnectionEntityCollection= mongoDBConnectionEntityAdmin.findByType();
            java.util.List<MongoDBConnectionEntity> connectionEntities = new ArrayList<MongoDBConnectionEntity>(mongoDBConnectionEntityCollection);

            serversTableModel.setRows(connectionEntities);
        } catch(FindException fe) {
            logger.log(Level.WARNING, "Failed to load the cache servers list.");
        }
    }

    private void doRemove() {
        if(serversTable.getSelectedRowCount() == 0) {
            return;
        }

        java.util.List<MongoDBConnectionEntity> entries = new ArrayList<MongoDBConnectionEntity>(serversTable.getSelectedRowCount());
        for(int row : serversTable.getSelectedRows()) {
            entries.add(serversTableModel.getRowObject(rowSorter.convertRowIndexToModel(row)));
        }

        if(JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(this,
                "Are you sure you want to remove the selected MongoDB connection(s)?",
                "Confirm Document(s) Deletion",
                JOptionPane.YES_NO_OPTION))
        {
            MongoDBConnectionEntityAdmin mongoDBConnectionEntityAdmin = Registry.getDefault().getExtensionInterface(MongoDBConnectionEntityAdmin.class, null);
            for(MongoDBConnectionEntity connectionEntity : entries) {
                try {
                    mongoDBConnectionEntityAdmin.delete(connectionEntity);
                } catch(DeleteException de) {
                    JOptionPane.showMessageDialog(MongoDBConnectionsDialog.this, "Failed to delete MongoDB connection (" + connectionEntity.getName() + ").", "Error", JOptionPane.ERROR_MESSAGE);
                } catch(FindException fe) {
                    JOptionPane.showMessageDialog(MongoDBConnectionsDialog.this, "Failed to delete MongoDB connection (" + connectionEntity.getName() + ").", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }

            loadServers();
        }
    }
}
