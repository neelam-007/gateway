package com.l7tech.console.panels;

import com.l7tech.console.logging.ErrorManager;
import com.l7tech.console.security.SecurityProvider;
import com.l7tech.console.util.*;
import com.l7tech.gateway.common.cluster.ClusterNodeInfo;
import com.l7tech.gateway.common.cluster.ClusterStatusAdmin;
import com.l7tech.gateway.common.module.ModuleState;
import com.l7tech.gateway.common.module.ServerModuleFile;
import com.l7tech.gateway.common.module.ServerModuleFileState;
import com.l7tech.gateway.common.security.rbac.*;
import com.l7tech.gui.SimpleTableModel;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.TableUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.PleaseWaitDialog;
import com.l7tech.objectmodel.*;
import com.l7tech.util.*;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.ResourceBundle;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.gui.util.TableUtil.column;
import static com.l7tech.util.Functions.propertyTransform;

/**
 * Dialog responsible for managing {@link ServerModuleFile}'s
 */
public class ServerModuleFileManagerWindow extends JDialog {
    private static final long serialVersionUID = 385196421868644404L;
    private static final Logger logger = Logger.getLogger(ServerModuleFileManagerWindow.class.getName());
    private static final ResourceBundle resources = ResourceBundle.getBundle(ServerModuleFileManagerWindow.class.getName());
    private static final int REFRESH_INTERVAL_MILLIS = 5000;
    private static final long WAIT_BEFORE_DISPLAY_ASYNC_OPERATION_DIALOG = 500L;
    private static final int SIZE_COLUMN_INDEX = 3;
    private static final int SIZE_COLUMN_ALIGNMENT = DefaultTableCellRenderer.TRAILING;

    private JPanel contentPane;
    private JTable moduleTable;
    private JButton uploadButton;
    private JButton deleteButton;
    private JButton propertiesButton;
    private JButton closeButton;
    private JPanel uploadDisabledWarningPanel;
    private JComboBox<ClusterNodeInfo> clusterNodeCombo;
    private JLabel readOnlyThisIsLabel;

    /// List of Server Module Files from gateway
    private final Collection<ServerModuleFile> serverModuleFiles = new ArrayList<>();
    private final CollectionUpdateConsumer<ServerModuleFile, FindException> serverModuleFilesUpdateConsumer;
    private final SimpleTableModel<ServerModuleFile> serverModuleFilesTableModel;

    // List of cluster nodes fetched from gateway
    private final Collection<ClusterNodeInfo> clusterNodes = new ArrayList<>();
    private final CollectionUpdateConsumer<ClusterNodeInfo, FindException> clusterNodesUpdateConsumer;
    private final DefaultComboBoxModel<ClusterNodeInfo> clusterNodesComboModel;

    private Timer refreshTimer;

    private ClusterStatusAdmin clusterStatusAdmin;
    private SecurityProvider securityProvider;
    private ClusterNodeInfo currentClusterNode;

    private final boolean canCreate;
    private final boolean canUpload;

    /**
     * Convenience CellRenderer to display server module bytes into human-readable {@code String}.<br/>
     * Also the alignment is set to {@link #SIZE_COLUMN_ALIGNMENT}.
     */
    @SuppressWarnings("serial")
    private static class SizeCellRenderer extends DefaultTableCellRenderer {
        /**
         * Sets the alignment to right.
         */
        public SizeCellRenderer() {
            super();
            setHorizontalAlignment(SIZE_COLUMN_ALIGNMENT);
        }

        @Override
        protected void setValue(Object value) {
            if (value instanceof Long) {
                value = ServerModuleFile.humanReadableBytes((Long)value);
            }
            super.setValue(value);
        }
    }

    /**
     * Constructor
     */
    public ServerModuleFileManagerWindow(final Window parent) {
        super(parent, resources.getString("dialog.title"), JDialog.DEFAULT_MODALITY_TYPE);

        setContentPane(contentPane);
        setModal(true);

        // determine whether the user create new ServerModuleFile entities
        canCreate = getSecurityProvider().hasPermission(new AttemptedCreate(EntityType.SERVER_MODULE_FILE));
        canUpload = ServerModuleFileClusterPropertiesReader.getInstance().isModulesUploadEnabled();
        uploadDisabledWarningPanel.setVisible(!canUpload);

        // set dispose on close and when ESC is pressed
        //
        closeButton.addActionListener(Utilities.createDisposeAction(this));
        Utilities.setEscAction(this, closeButton);
        this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        // crate enable/disable action listener
        //
        final RunOnChangeListener enableOrDisableListener = new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                enableOrDisable();
            }
        }) {
            @Override
            public void tableChanged(final TableModelEvent e) {
                if (TableModelEvent.DELETE == e.getType()) {
                    // need custom handling for delete event as it can get triggered before row selection is cleared
                    enableOrDisable(null);
                } else {
                    run();
                }
            }
        };

        // create cluster nodes combo
        //
        clusterNodesUpdateConsumer = new CollectionUpdateConsumer<ClusterNodeInfo, FindException>(null) {
            @Override
            protected CollectionUpdate<ClusterNodeInfo> getUpdate(final int oldVersionID) throws FindException {
                return getClusterStatusAdmin().getClusterNodesUpdate(oldVersionID);
            }
        };
        //noinspection serial
        clusterNodesComboModel = new DefaultComboBoxModel<ClusterNodeInfo>() {
            /**
             * Adds an element in alphabetical order.
             *
             * @param element    a {@link ClusterNodeInfo} object to add
             */
            @Override
            public void addElement(final ClusterNodeInfo element) {
                int i = 0;
                while (i < getSize() && element.compareTo(getElementAt(i)) >= 0) {
                    ++i;
                }
                insertElementAt(element, i);
            }
        };
        try {
            clusterNodesUpdateConsumer.update(clusterNodes, clusterNodesComboModel);
            clusterNodeCombo.setModel(clusterNodesComboModel);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
        clusterNodesComboModel.setSelectedItem(getCurrentClusterNode());
        clusterNodeCombo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                resetData();
            }
        });


        // create Server Module File table
        //
        serverModuleFilesUpdateConsumer = new CollectionUpdateConsumer<ServerModuleFile, FindException>(null) {
            @Override
            protected CollectionUpdate<ServerModuleFile> getUpdate(final int oldVersionID) throws FindException {
                return getClusterStatusAdmin().getServerModuleFileUpdate(oldVersionID);
            }
        };
        serverModuleFilesTableModel = TableUtil.configureTable(moduleTable,
                column( resources.getString("modules.column.name"), 30, 140, 99999, propertyTransform( ServerModuleFile.class, "name" ) ),
                column( resources.getString("modules.column.file-name"), 30, 250, 99999, propFinder( ServerModuleFile.PROP_FILE_NAME ) ),
                column( resources.getString("modules.column.type"), 30, 150, 150, propertyTransform( ServerModuleFile.class, "moduleType") ),
                column( resources.getString("modules.column.size"), 20, 80, 150, dataBytesPropToLong(), Long.class ),
                column( resources.getString("modules.column.status"), 20, 80, 150,
                        new Functions.Unary<String, ServerModuleFile>() {
                            @Override
                            public String call(final ServerModuleFile serverModuleFile) {
                                return getStateForSelectedNode(serverModuleFile);
                            }
                        }
                )
        );
        serverModuleFilesTableModel.addTableModelListener(enableOrDisableListener);
        Utilities.setRowSorter(moduleTable, serverModuleFilesTableModel, new int[]{0}, new boolean[]{true}, new Comparator[]{String.CASE_INSENSITIVE_ORDER});
        moduleTable.getSelectionModel().addListSelectionListener(enableOrDisableListener);
        // Override the Size cell renderer in order to display human-readable bytes
        //
        moduleTable.getColumnModel().getColumn(SIZE_COLUMN_INDEX).setCellRenderer(new SizeCellRenderer());

        // create entity crud
        //
        final EntityCrudController<ServerModuleFile> crud = new EntityCrudController<>();
        crud.setEntityTable(moduleTable);
        crud.setEntityTableModel(serverModuleFilesTableModel);
        crud.setEntityCreator(new EntityCreator<ServerModuleFile>() {
            @Override
            public ServerModuleFile createNewEntity() {
                try {
                    return new ServerModuleFileChooser(ServerModuleFileManagerWindow.this).choose();
                } catch (IOException e) {
                    showError(resources.getString("error.open.module"), e, true);
                }
                return null;
            }
        });
        crud.setEntityDeleter(new EntityDeleter<ServerModuleFile>() {
            @Override
            public void deleteEntity(@NotNull final ServerModuleFile entity) throws DeleteException {
                asyncDeleteModuleFile(entity);
            }
        });
        crud.setEntityDeleteConfirmer(createDialogBasedEntityDeleteConfirmer());
        crud.setEntitySaver(new EntitySaver<ServerModuleFile>() {
            @Override
            public ServerModuleFile saveEntity(final ServerModuleFile entity) throws SaveException {
                return asyncSaveModuleFile(entity);
            }
        });
        crud.setEntityEditor(new EntityEditor<ServerModuleFile>() {
            @Override
            public void displayEditDialog(final ServerModuleFile entity, @NotNull final Functions.UnaryVoidThrows<ServerModuleFile, SaveException> afterEditListener) {
                final ServerModuleFile newMod = new ServerModuleFile();
                newMod.copyFrom(entity, true, true, true);

                final boolean create = PersistentEntity.DEFAULT_GOID.equals(entity.getGoid());
                final AttemptedOperation operation = create
                        ? new AttemptedCreateSpecific(EntityType.SERVER_MODULE_FILE, entity)
                        : new AttemptedUpdate(EntityType.SERVER_MODULE_FILE, entity);
                final boolean readOnly = !canUpload || !getSecurityProvider().hasPermission(operation) || !isCurrentNodeSelected();

                final ServerModuleFilePropertiesDialog dlg = new ServerModuleFilePropertiesDialog(
                        ServerModuleFileManagerWindow.this,
                        newMod,
                        create ? StringUtils.EMPTY : getStateMessageForSelectedNode(newMod),
                        readOnly
                );
                dlg.pack();
                Utilities.centerOnParentWindow(dlg);
                DialogDisplayer.display(dlg, new Runnable() {
                    @Override
                    public void run() {
                        if (!dlg.isConfirmed())
                            return;
                        try {
                            newMod.setName(dlg.getModuleName());
                            afterEditListener.call(newMod);
                        } catch (final SaveException e) {
                            showError(resources.getString("error.save"), e);
                        }
                    }
                });
            }
        });

        // set action listener for the buttons
        //
        uploadButton.addActionListener(crud.createCreateAction());
        propertiesButton.addActionListener(crud.createEditAction());
        Utilities.setDoubleClickAction(moduleTable, propertiesButton);
        deleteButton.addActionListener(crud.createDeleteAction());

        if (canUpload) {
            startRefreshTimer();
        }

        // initial update of server module file table based of the select cluster node
        resetData();
    }

    /**
     * Utility method for executing long-lasting task (like uploading large server module file) asynchronous,
     * with a "Please Wait..." dialog.<br/>
     * If the task did complete before {@link #WAIT_BEFORE_DISPLAY_ASYNC_OPERATION_DIALOG defined time} the dialog will not be displayed.
     *
     * @param taskInfo     the progress dialog info message.
     * @param task         the {@link Callable task} to execute.  Required and cannot be {@code null}.
     * @return Whatever was returned from {@code task} or {@code null} if the task was canceled.
     * @throws InvocationTargetException if the invoked {@code task} method failed to be executed.
     */
    private <T> T doAsyncTask(
            final String taskInfo,
            @NotNull final Callable<T> task
    ) throws InvocationTargetException {
        final JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);

        final PleaseWaitDialog cancelDialog = new PleaseWaitDialog(this, taskInfo, progressBar);
        cancelDialog.pack();
        cancelDialog.setModal(true);
        Utilities.centerOnParentWindow(cancelDialog);

        try {
            return Utilities.doWithDelayedCancelDialog(task, cancelDialog, WAIT_BEFORE_DISPLAY_ASYNC_OPERATION_DIALOG);
        } catch (final InterruptedException e) {
            logger.finer("Saving/Deleting Server Module File was cancelled.");
        }

        return null;
    }

    /**
     * Asynchronously delete the specified {@code moduleFile}.
     *
     * @param moduleFile    {@link ServerModuleFile} entity to delete.
     * @throws DeleteException if module can't be deleted, or DB error updating.
     */
    private void asyncDeleteModuleFile(@NotNull final ServerModuleFile moduleFile) throws DeleteException {
        try {
            doAsyncTask(
                    resources.getString("delete.async.dialog.message"),
                    new Callable<Void>() {
                        @Override
                        public Void call() throws Exception {
                            getClusterStatusAdmin().deleteServerModuleFile(moduleFile.getGoid());
                            return null;
                        }
                    }
            );
        } catch (final InvocationTargetException e) {
            // rethrow if target was DeleteException
            final Throwable cause = e.getTargetException();
            if (cause instanceof DeleteException) {
                throw (DeleteException)cause;
            }
            showError(resources.getString("unhandled.error.async.delete"), e, true);
        }
    }

    /**
     * Asynchronously save the specified {@code moduleFile}.
     *
     * @param moduleFile    {@link ServerModuleFile} entity to save.
     * @throws SaveException if a new entity cannot be saved.
     */
    private ServerModuleFile asyncSaveModuleFile(@NotNull final ServerModuleFile moduleFile) throws SaveException {
        try {
            return doAsyncTask(
                    resources.getString("save.async.dialog.message"),
                    new Callable<ServerModuleFile>() {
                        @Override
                        public ServerModuleFile call() throws SaveException {
                            ServerModuleFile entity = moduleFile;
                            try {
                                final Goid id = getClusterStatusAdmin().saveServerModuleFile(entity);
                                entity.setGoid(id);
                            } catch (FindException | UpdateException e) {
                                throw new SaveException(e);
                            }

                            try {
                                entity = getClusterStatusAdmin().findServerModuleFileById(entity.getGoid(), false);
                            } catch (FindException e) {
                                entity.setData(null);
                                entity.setModuleSha256(null);
                                entity.setProperty(ServerModuleFile.PROP_SIZE, null);
                                entity.setProperty(ServerModuleFile.PROP_FILE_NAME, null);
                                entity.setProperty(ServerModuleFile.PROP_ASSERTIONS, null);
                                /* FALL-THROUGH and do without up-to-date sha-256 and size */
                            }

                            return entity;
                        }
                    }
            );
        } catch (final InvocationTargetException e) {
            // rethrow if target was SaveException
            final Throwable cause = e.getTargetException();
            if (cause instanceof SaveException) {
                throw (SaveException)cause;
            }
            showError(resources.getString("unhandled.error.async.save"), e, true);
        }

        return null;
    }

    /**
     * Starts a swing Timer responsible for refreshing both cluster nodes combo and server module files table.
     */
    private void startRefreshTimer() {
        if (refreshTimer == null) {
            refreshTimer = new Timer(REFRESH_INTERVAL_MILLIS, new ActionListener() {
                // flag for refresh currently active
                final AtomicBoolean refreshing = new AtomicBoolean(false);

                @Override
                public void actionPerformed(final ActionEvent e) {
                    try {
                        // create a SwingWorker
                        final com.l7tech.gui.util.SwingWorker refreshWorker = new com.l7tech.gui.util.SwingWorker () {
                            @Override
                            public void finished() {
                                super.finished();
                            }

                            @Override
                            public Object construct() {
                                if (refreshing.getAndSet(true)) {
                                    logger.fine("Concurrent refresh requested, skipping update.");
                                    return null;
                                }
                                try {
                                    // update cluster nodes
                                    clusterNodesUpdateConsumer.update(clusterNodes, clusterNodesComboModel);
                                    // update server module files
                                    updateServerModuleFileTable(serverModuleFilesUpdateConsumer.update(serverModuleFiles));
                                } catch (final RuntimeException e) {
                                    ErrorManager.getDefault().notify(Level.WARNING, e, resources.getString("error.refresh.runtime.error"));
                                    refreshTimer.stop();
                                    dispose();
                                } catch (final FindException e) {
                                    ErrorManager.getDefault().notify(Level.WARNING, e, resources.getString("error.refresh.find.error"));
                                    refreshTimer.stop();
                                    dispose();
                                } finally {
                                    refreshing.set(false);
                                }
                                return null;
                            }
                        };
                        refreshWorker.start();
                    } catch (final RuntimeException ex) {
                        ErrorManager.getDefault().notify(Level.WARNING, ex, resources.getString("error.refresh.runtime.error"));
                        refreshTimer.stop();
                        dispose();
                    }
                }
            });
        }
        refreshTimer.restart();
    }

    /**
     * Updates Server Module Files Table data based on the specified added and removed collecation pair.
     * </p>
     * Special case:<br/>
     * Since {@link CollectionUpdate} and {@link CollectionUpdateConsumer} doesn't natively support entity changes
     * (only new and removed entities are handled), {@link com.l7tech.gateway.common.module.ServerModuleFileStateDifferentiator ServerModuleFileStateDifferentiator}
     * is introduced to detect any {@link ServerModuleFile} entity properties and {@link ServerModuleFileState state} changes.<br/>
     * In this case, removed collection will contain the old entity(s), where as the added collection will contain
     * the same entity(s) (same id's) but with the new values.
     *
     * @param addedRemoved    a pair holding added and removed {@link ServerModuleFile} entities from the Gateway.
     */
    private void updateServerModuleFileTable(@NotNull final Pair<Collection<ServerModuleFile>, Collection<ServerModuleFile>> addedRemoved) {
        // get added and removed ServerModuleFile's
        final Collection<ServerModuleFile> added = addedRemoved.left;
        final Collection<ServerModuleFile> removed = addedRemoved.right;

        // special case
        // Since CollectionUpdate & CollectionUpdateConsumer doesn't support entity changes, only new and removed entities,
        // ServerModuleFileStateDifferentiator is introduced to detect any ServerModuleFile entity properties and state changes.
        // In this case, removed collection will contain the old entity(s), where as the added collection will contain
        // the same entity(s) (same id's) but with new values.
        // In this case we need to preserve selection.
        final ServerModuleFile selectedModuleFile = getSelectedServerModuleFile();
        ServerModuleFile newlySelectedModuleFile = null;

        // Removes removed items from model.
        if (removed != null) {
            for (final ServerModuleFile moduleFile : removed) {
                serverModuleFilesTableModel.removeRow(moduleFile);
            }
        }
        // Adds added items to model.
        if (added != null) {
            for (final ServerModuleFile moduleFile : added) {
                final int modelRowIndex = serverModuleFilesTableModel.getRowIndex(moduleFile);
                if (modelRowIndex == -1) {
                    serverModuleFilesTableModel.addRow(moduleFile);
                } else {
                    serverModuleFilesTableModel.setRowObject(modelRowIndex, moduleFile);
                }
                if (selectedModuleFile != null && selectedModuleFile.getGoid() != null && selectedModuleFile.getGoid().equals(moduleFile.getGoid())) {
                    // the select module was added again, preserve the selection
                    newlySelectedModuleFile = moduleFile;
                }
            }
        }

        if (newlySelectedModuleFile != null) {
            final int newModelRowIndex = serverModuleFilesTableModel.getRowIndex(newlySelectedModuleFile);
            if (newModelRowIndex > -1) {
                final int newRowIndex = moduleTable.convertRowIndexToView(newModelRowIndex);
                if (newRowIndex > -1) {
                    moduleTable.setRowSelectionInterval(newRowIndex, newRowIndex);
                }
            }
        }
    }

    /**
     * Resets Server Module Files Table data.<br/>
     * Typically after new node is selected and during dialog display.
     */
    private void resetData() {
        if (refreshTimer != null) {
            refreshTimer.stop();
        }

        try {
            if (serverModuleFilesUpdateConsumer != null) {
                updateServerModuleFileTable(serverModuleFilesUpdateConsumer.update(serverModuleFiles));
            }
        } catch (final Exception e) {
            throw new RuntimeException(e);
        } finally {
            readOnlyThisIsLabel.setVisible(canUpload && !isCurrentNodeSelected());
            fireTableDataChanged();
            enableOrDisable();
        }

        if (refreshTimer != null) {
            refreshTimer.start();
        }
    }

    /**
     * Utility method for firing table data changed event and preserving selection.
     */
    private void fireTableDataChanged() {
        if (serverModuleFilesTableModel != null) {
            // get currently/originally selected row
            final int rowIndex = moduleTable.getSelectedRow();
            // fire data change event
            serverModuleFilesTableModel.fireTableDataChanged();
            // reselect originally selected row
            if (rowIndex > -1) {
                moduleTable.setRowSelectionInterval(rowIndex, rowIndex);
            }
        }
    }

    /**
     * Get currently selected {@link ServerModuleFile Server Module File}, from the Modules Table,
     * or {@code null} if no module is selected.
     */
    @Nullable
    private ServerModuleFile getSelectedServerModuleFile() {
        ServerModuleFile selected = null;
        final int rowIndex = moduleTable.getSelectedRow();
        if (rowIndex >= 0) {
            final int modelIndex = moduleTable.convertRowIndexToModel(rowIndex);
            if (modelIndex >= 0) {
                selected = serverModuleFilesTableModel.getRowObject(modelIndex);
            }
        }
        return selected;
    }

    /**
     * Convenience method for getting selected cluster node.
     */
    @Nullable
    public ClusterNodeInfo getSelectedClusterNode() {
        return (ClusterNodeInfo) clusterNodesComboModel.getSelectedItem();
    }

    /**
     * Convenience method for determining whether selected node (from the combo) is the currently logon node.
     */
    private boolean isCurrentNodeSelected() {
        final ClusterNodeInfo selectedClusterNode = getSelectedClusterNode();
        final String selectedNodeId = (selectedClusterNode != null ? selectedClusterNode.getNodeIdentifier() : null);
        return getCurrentClusterNode().getNodeIdentifier().equals(selectedNodeId);
    }

    /**
     * Get our cached {@link ClusterStatusAdmin}
     */
    @NotNull
    private ClusterStatusAdmin getClusterStatusAdmin() {
        if (clusterStatusAdmin == null) {
            clusterStatusAdmin = Registry.getDefault().getClusterStatusAdmin();
        }
        return clusterStatusAdmin;
    }

    /**
     * Get our cached {@link SecurityProvider}
     */
    @NotNull
    private SecurityProvider getSecurityProvider() {
        if (securityProvider == null) {
            securityProvider = Registry.getDefault().getSecurityProvider();
        }
        return securityProvider;
    }

    /**
     * Get our cached {@link ClusterNodeInfo current cluster node}
     */
    @NotNull
    private ClusterNodeInfo getCurrentClusterNode() {
        if (currentClusterNode == null) {
            currentClusterNode = getClusterStatusAdmin().getSelfNode();
        }
        return currentClusterNode;
    }

    /**
     * Create a default delete confirmer.
     *
     * @return a new EntityDeleteConfirmer instance.  Never {@code null}.
     */
    @NotNull
    private EntityDeleteConfirmer<ServerModuleFile> createDialogBasedEntityDeleteConfirmer() {
        return new EntityDeleteConfirmer<ServerModuleFile>() {
            @Override
            public void displayDeleteDialog(@NotNull final ServerModuleFile entity, @NotNull final Functions.UnaryVoid<ServerModuleFile> afterDeleteListener) {
                DialogDisplayer.showOptionDialog(
                        ServerModuleFileManagerWindow.this,
                        ServerModuleFileManagerWindow.resources.getString("delete.confirmer.message"),
                        ServerModuleFileManagerWindow.resources.getString("delete.confirmer.title"),
                        JOptionPane.DEFAULT_OPTION,
                        JOptionPane.WARNING_MESSAGE,
                        null,
                        new Object[]{
                                ServerModuleFileManagerWindow.resources.getString("delete.confirmer.remove.button.caption"),
                                ServerModuleFileManagerWindow.resources.getString("delete.confirmer.cancel.button.caption")
                        },
                        null,
                        new DialogDisplayer.OptionListener() {
                            @Override
                            public void reportResult(int option) {
                                if (option == 0) {
                                    afterDeleteListener.call(entity);
                                } else {
                                    afterDeleteListener.call(null);
                                }
                            }
                        });
            }
        };
    }

    /**
     * Utility method for extracting the {@link ServerModuleFileState module state} for the currently selected
     * Cluster node {@link #getSelectedClusterNode()}.<br/>
     * If state error message is set, then the method will return the error message, otherwise the state itself will be return.
     * <p/>
     * If there is no state object for the specified module, then {@link ModuleState#UPLOADED} is returned.
     *
     * @param moduleFile    The {@link ServerModuleFile module}.
     * @see #getSelectedClusterNode()
     */
    @NotNull
    private String getStateMessageForSelectedNode(@Nullable final ServerModuleFile moduleFile) {
        if (moduleFile != null) {
            final ClusterNodeInfo selectedClusterNode = getSelectedClusterNode();
            if (selectedClusterNode != null) {
                final String selectedClusterNodeId = selectedClusterNode.getNodeIdentifier();
                if (moduleFile.getStates() != null && StringUtils.isNotBlank(selectedClusterNodeId)) {
                    final ServerModuleFileState moduleState = moduleFile.getStateForNode(selectedClusterNodeId);
                    if (moduleState != null) {
                        if (StringUtils.isNotBlank(moduleState.getErrorMessage())) {
                            //noinspection ConstantConditions
                            return moduleState.getErrorMessage();
                        } else {
                            return moduleState.getState().toString();
                        }
                    } else {
                        // no state for this node, return UPLOADED, as module is in the database
                        return ModuleState.UPLOADED.toString();
                    }
                }
            }
        }
        return StringUtils.EMPTY;
    }

    /**
     * Utility method for extracting the {@link ServerModuleFileState module state} for the currently selected
     * Cluster node {@link #getSelectedClusterNode()}.
     * <p/>
     * If there is no state object for the specified module, then {@link ModuleState#UPLOADED} is returned.
     *
     * @param moduleFile    The {@link ServerModuleFile module}.
     * @see #getSelectedClusterNode()
     */
    @NotNull
    private String getStateForSelectedNode(@Nullable final ServerModuleFile moduleFile) {
        if (moduleFile != null) {
            final ClusterNodeInfo selectedClusterNode = getSelectedClusterNode();
            if (selectedClusterNode != null) {
                final String selectedClusterNodeId = selectedClusterNode.getNodeIdentifier();
                if (moduleFile.getStates() != null && StringUtils.isNotBlank(selectedClusterNodeId)) {
                    final ServerModuleFileState moduleState = moduleFile.getStateForNode(selectedClusterNodeId);
                    if (moduleState != null) {
                        return moduleState.getState().toString();
                    } else {
                        // no state for this node, return UPLOADED, as module is in the database
                        return ModuleState.UPLOADED.toString();
                    }
                }
            }
        }
        return StringUtils.EMPTY;
    }

    /**
     * Convenient method without specifying the {@code logError} flag.
     *
     * @see #showError(String, Throwable, boolean)
     */
    private void showError(@NotNull String message, @Nullable final Throwable e) {
        showError(message, e, false);
    }

    /**
     * Utility method for displaying an error that happen.
     *
     * @param message     The error message to display.
     * @param e           The exception that occurred.
     * @param logError    Specify whether to log the error as well.
     */
    private void showError(@NotNull String message, @Nullable final Throwable e, boolean logError) {
        if (e != null) {
            message = message + ": " + ExceptionUtils.getMessage(e);
        }

        if (logError) {
            logger.log(Level.WARNING, message, ExceptionUtils.getDebugException(e));
        }

        DialogDisplayer.showMessageDialog(this, message, resources.getString("error.title"), JOptionPane.ERROR_MESSAGE, null);
    }

    /**
     * Utility method for creating a property extractor callback for the specified property.
     *
     * @param propertyName    the property name to extract.
     * @return a unary function object, which will extract the specified property using java reflection.
     */
    @NotNull
    private static Functions.Unary<Object, ServerModuleFile> propFinder( @NotNull final String propertyName ) {
        return new Functions.Unary<Object, ServerModuleFile>() {
            @Override
            public Object call( ServerModuleFile mod ) {
                return mod.getProperty( propertyName );
            }
        };
    }

    /**
     * Utility method for extracting {@link ServerModuleFile#PROP_SIZE} and converting it to {@link Long} for proper sorting.
     *
     * @return a {@link Long} representing the module databytes, or {@code null} if {@link ServerModuleFile#PROP_SIZE}
     * cannot be converted to {@link Long}.
     */
    @Nullable
    private static Functions.Unary<Long, ServerModuleFile> dataBytesPropToLong() {
        return new Functions.Unary<Long, ServerModuleFile>() {
            @Override
            public Long call(final ServerModuleFile mod) {
                try {
                    return Long.parseLong(mod.getProperty(ServerModuleFile.PROP_SIZE));
                } catch (final NumberFormatException e) {
                    return null;
                }
            }
        };
    }

    /**
     * Enable or Disable buttons based on the user's (currently logged-on) RBAC permissions for the specified
     * {@link ServerModuleFile Server Module File}.
     *
     * @param selected    the currently selected {@link ServerModuleFile Server Module File}.  Optional.
     */
    private void enableOrDisable(@Nullable final ServerModuleFile selected) {
        final boolean isCurrentNodeSelected = isCurrentNodeSelected();
        propertiesButton.setEnabled(selected != null);
        deleteButton.setEnabled(selected != null && canUpload && isCurrentNodeSelected && getSecurityProvider().hasPermission(new AttemptedDeleteSpecific(EntityType.SERVER_MODULE_FILE, selected)));
        uploadButton.setEnabled(canCreate && canUpload && isCurrentNodeSelected);
    }

    /**
     * Enable or Disable buttons based on the user's (currently logged-on) RBAC permissions for currently selected
     * {@link ServerModuleFile Server Module File}.
     *
     * @see #enableOrDisable(com.l7tech.gateway.common.module.ServerModuleFile)
     */
    private void enableOrDisable() {
        enableOrDisable(getSelectedServerModuleFile());
    }

    /**
     * Make sure the refresh timer is stopped when disposing this dialog.
     */
    @Override
    public void dispose() {
        if (refreshTimer != null) {
            refreshTimer.stop();
        }
        super.dispose();
    }
}
