package com.l7tech.external.assertions.remotecacheassertion.console;

import com.l7tech.console.util.Registry;
import com.l7tech.external.assertions.remotecacheassertion.RemoteCacheEntity;
import com.l7tech.external.assertions.remotecacheassertion.RemoteCacheEntityAdmin;
import com.l7tech.external.assertions.remotecacheassertion.RemoteCacheEntityAdminImpl;
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

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 10/11/11
 * Time: 2:14 PM
 * To change this template use File | Settings | File Templates.
 */
public class CacheServersDialog extends JDialog {
    private static final Logger logger = Logger.getLogger(CacheServersDialog.class.getName());

    private JPanel mainPanel;
    private SimpleTableModel<RemoteCacheEntity> serversTableModel;
    private TableRowSorter<SimpleTableModel<RemoteCacheEntity>> rowSorter;
    private JTable serversTable;
    private JButton addServerButton;
    private JButton editButton;
    private JButton removeButton;
    private JButton closeButton;
    private JButton manageLibrariesButton;

    private ClusterStatusAdmin clusterStatusAdmin;

    private static final String TITLE = "Cache Servers";

    public CacheServersDialog(Frame parent) {
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
        com.l7tech.gui.util.Utilities.setRowSorter(serversTable, serversTableModel, new int[]{0}, new boolean[]{true}, new Comparator[]{String.CASE_INSENSITIVE_ORDER});
        rowSorter = (TableRowSorter<SimpleTableModel<RemoteCacheEntity>>) serversTable.getRowSorter();
        rowSorter.setSortsOnUpdates(true);

        addServerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                RemoteCacheEntity newRemoteCache = new RemoteCacheEntity();
                CacheServerDialog dialog = new CacheServerDialog(CacheServersDialog.this, newRemoteCache);
                Utilities.centerOnParentWindow(dialog);
                dialog.setVisible(true);
                if (dialog.isConfirmed()) {
                    newRemoteCache = dialog.getData(newRemoteCache);
                    if (newRemoteCache != null) {
                        try {
                            RemoteCacheEntityAdmin remoteCacheAdmin = Registry.getDefault().getExtensionInterface(RemoteCacheEntityAdmin.class, null);
                            remoteCacheAdmin.save(newRemoteCache);

                            loadServers();
                        } catch (SaveException se) {
                            JOptionPane.showMessageDialog(CacheServersDialog.this, "Failed to save new remote cache entry.", "Error", JOptionPane.ERROR_MESSAGE);
                        } catch (UpdateException ue) {
                            JOptionPane.showMessageDialog(CacheServersDialog.this, "Failed to save new remote cache entry.", "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
            }
        });

        editButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                RemoteCacheEntity remoteCache = serversTableModel.getRowObject(rowSorter.convertRowIndexToModel(serversTable.getSelectedRow()));
                CacheServerDialog dialog = new CacheServerDialog(CacheServersDialog.this, remoteCache);
                Utilities.centerOnParentWindow(dialog);
                dialog.setVisible(true);
                if (dialog.isConfirmed()) {
                    remoteCache = dialog.getData(remoteCache);
                    if (remoteCache != null) {
                        try {
                            RemoteCacheEntityAdmin remoteCacheAdmin = Registry.getDefault().getExtensionInterface(RemoteCacheEntityAdmin.class, null);
                            remoteCacheAdmin.save(remoteCache);

                            loadServers();
                        } catch (SaveException se) {
                            JOptionPane.showMessageDialog(CacheServersDialog.this, "Failed to save remote cache entry.", "Error", JOptionPane.ERROR_MESSAGE);
                        } catch (UpdateException ue) {
                            JOptionPane.showMessageDialog(CacheServersDialog.this, "Failed to save remote cache entry.", "Error", JOptionPane.ERROR_MESSAGE);
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

        // Temporary hide the manage libraries button until it works with clustering
        manageLibrariesButton.setVisible(false);
        manageLibrariesButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                com.l7tech.assertion.base.util.classloaders.console.ManageLibrariesDialog dialog = new com.l7tech.assertion.base.util.classloaders.console.ManageLibrariesDialog(CacheServersDialog.this,
                        RemoteCacheEntityAdminImpl.class.getInterfaces()[0].getName());
                dialog.setVisible(true);
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

    private SimpleTableModel<RemoteCacheEntity> buildServersTableModel() {
        return TableUtil.configureTable(
                serversTable,
                TableUtil.column("Enabled", 40, 100, 180, new Functions.Unary<String, RemoteCacheEntity>() {
                    @Override
                    public String call(RemoteCacheEntity remoteCache) {
                        return remoteCache.isEnabled() + "";
                    }
                }, String.class),
                TableUtil.column("Name", 40, 200, 1000000, new Functions.Unary<String, RemoteCacheEntity>() {
                    @Override
                    public String call(RemoteCacheEntity remoteCache) {
                        return remoteCache.getName();
                    }
                }, String.class),
                TableUtil.column("Type", 40, 100, 180, new Functions.Unary<String, RemoteCacheEntity>() {
                    @Override
                    public String call(RemoteCacheEntity remoteCache) {
                        return remoteCache.getType();
                    }
                }, String.class)
        );
    }

    private void loadServers() {
        try {
            RemoteCacheEntityAdmin remoteCacheAdmin = Registry.getDefault().getExtensionInterface(RemoteCacheEntityAdmin.class, null);
            Collection<RemoteCacheEntity> remoteCachesCollection = remoteCacheAdmin.findAll();
            java.util.List<RemoteCacheEntity> remoteCachesList = new ArrayList<RemoteCacheEntity>(remoteCachesCollection);

            serversTableModel.setRows(remoteCachesList);
        } catch (FindException fe) {
            logger.log(Level.WARNING, "Failed to load the cache servers list.");
        }
    }

    private void doRemove() {
        if (serversTable.getSelectedRowCount() == 0) {
            return;
        }

        java.util.List<RemoteCacheEntity> entries = new ArrayList<RemoteCacheEntity>(serversTable.getSelectedRowCount());
        for (int row : serversTable.getSelectedRows()) {
            entries.add(serversTableModel.getRowObject(rowSorter.convertRowIndexToModel(row)));
        }

        if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(this,
                "Are you sure you want to remove the selected remote cache(s)?",
                "Confirm Document(s) Deletion",
                JOptionPane.YES_NO_OPTION)) {
            RemoteCacheEntityAdmin remoteCacheAdmin = Registry.getDefault().getExtensionInterface(RemoteCacheEntityAdmin.class, null);
            for (RemoteCacheEntity remoteCache : entries) {
                try {
                    remoteCacheAdmin.delete(remoteCache);
                } catch (DeleteException de) {
                    JOptionPane.showMessageDialog(CacheServersDialog.this, "Failed to delete remote cache entry (" + remoteCache.getName() + ").", "Error", JOptionPane.ERROR_MESSAGE);
                } catch (FindException fe) {
                    JOptionPane.showMessageDialog(CacheServersDialog.this, "Failed to delete remote cache entry (" + remoteCache.getName() + ").", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }

            loadServers();
        }
    }
}
