package com.l7tech.console.panels;

import com.l7tech.console.action.DeleteEntityNodeAction;
import com.l7tech.console.logging.ErrorManager;
import com.l7tech.console.module.ServerModuleFileWithSignedBytes;
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
import org.apache.commons.lang.WordUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.*;
import java.util.List;
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
                    enableOrDisable(Collections.<ServerModuleFile>emptyList());
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

        moduleTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

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
                final ServerModuleFile newMod;
                if (entity instanceof ServerModuleFileWithSignedBytes) {
                    newMod = new ServerModuleFileWithSignedBytes((ServerModuleFileWithSignedBytes)entity);
                } else {
                    newMod = new ServerModuleFile();
                    newMod.copyFrom(entity, true, true, true);
                }

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
        deleteButton.addActionListener(
                new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        final Collection<ServerModuleFile> selectedModules = Collections.unmodifiableCollection(getSelectedServerModuleFiles().values());
                        if (selectedModules.isEmpty()) {
                            return;
                        }

                        DialogDisplayer.showSafeConfirmDialog(
                                ServerModuleFileManagerWindow.this,
                                WordUtils.wrap(createDeleteConfirmationMsg(selectedModules), DeleteEntityNodeAction.LINE_CHAR_LIMIT, null, true),
                                ServerModuleFileManagerWindow.resources.getString("delete.confirmer.title"),
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.WARNING_MESSAGE,
                                new DialogDisplayer.OptionListener() {
                                    @Override
                                    public void reportResult(final int option) {
                                        if (option == JOptionPane.YES_OPTION)
                                            asyncDeleteServerModuleFiles(selectedModules);
                                    }
                                });
                    }
                }
        );

        if (canUpload) {
            startRefreshTimer();
        }

        // initial update of server module file table based of the select cluster node
        resetData();
    }

    private String createDeleteConfirmationMsg(final Collection<ServerModuleFile> selected) {
        if (selected.size() > 1) 
            return resources.getString("delete.confirmer.message.multiple");
        return MessageFormat.format(resources.getString("delete.confirmer.message.single"), selected.iterator().next().getName());
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

    private void asyncDeleteServerModuleFiles(final Collection<ServerModuleFile> modules) {
        try {
            final List<Exception> errors = doAsyncTask(
                    resources.getString("delete.async.dialog.message"),
                    new Callable<List<Exception>>() {
                        @Override
                        public List<Exception> call() throws Exception {
                            final List<Exception> errors = new ArrayList<>();
                            for (final ServerModuleFile module : modules) {
                                try {
                                    getClusterStatusAdmin().deleteServerModuleFile(module.getGoid());
                                    serverModuleFilesTableModel.removeRow(module);
                                } catch (final Exception e) {
                                    logger.log(Level.WARNING, "Error deleting Server Module File \"" + module.getName() + "\": " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                                    errors.add(e);
                                }
                            }
                            return Collections.unmodifiableList(errors);
                        }
                    }
            );

            if (errors.size() == 1) {
                final Exception error = errors.get(0);
                if (error instanceof PermissionDeniedException) {
                    // delegate to PermissionDeniedErrorHandler
                    throw (PermissionDeniedException) error;
                } else {
                    showError(resources.getString("error.async.delete.single"), error);
                }
            } else if (errors.size() > 1) {
                showError(resources.getString("error.async.delete.multiple"), null);
            }
        } catch (final InvocationTargetException e) {
            // rethrow if target was DeleteException
            final Throwable cause = e.getTargetException();
            showError(resources.getString("unhandled.error.async.delete"), cause != null ? cause : e, true);
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
                        Goid goid = entity.getGoid();

                        // Save
                        if (Goid.isDefault(goid)) {
                            assert entity instanceof ServerModuleFileWithSignedBytes;
                            final ServerModuleFileWithSignedBytes smf = (ServerModuleFileWithSignedBytes)entity;
                            goid = getClusterStatusAdmin().saveServerModuleFile(
                                    smf.getSignedBytes(),
                                    smf.getName(),
                                    smf.getProperty(ServerModuleFile.PROP_FILE_NAME)
                            );
                        }
                        // Update
                        else {
                            try {
                                getClusterStatusAdmin().updateServerModuleFileName(goid, entity.getName());
                            } catch (final FindException | UpdateException e) {
                                throw new SaveException(e);
                            }
                        }

                        try {
                            entity = getClusterStatusAdmin().findServerModuleFileById(goid, false);
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
        final Map<Goid, ServerModuleFile> selectedModuleFiles = getSelectedServerModuleFiles();
        final Map<Goid, ServerModuleFile> newlySelectedModuleFiles = new HashMap<>(getSelectedServerModuleFiles());
        boolean updateSelection = false;

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
                final ServerModuleFile selectedModuleFile = selectedModuleFiles.get(moduleFile.getGoid());
                if (selectedModuleFile != null) {
                    // the select module was added again, preserve the selection
                    updateSelection = true;
                    newlySelectedModuleFiles.put(moduleFile.getGoid(), moduleFile);
                }
            }
        }

        // finally if updateSelection is set loop through all newlySelectedModuleFiles and reselect them
        if (updateSelection) {
            final ListSelectionModel selectionModel = moduleTable.getSelectionModel();
            if (selectionModel != null) {
                selectionModel.clearSelection();
                for (final ServerModuleFile newlySelectedModuleFile : newlySelectedModuleFiles.values()) {
                    final int newModelRowIndex = serverModuleFilesTableModel.getRowIndex(newlySelectedModuleFile);
                    if (newModelRowIndex > -1) {
                        final int newRowIndex = moduleTable.convertRowIndexToView(newModelRowIndex);
                        if (newRowIndex > -1) {
                            selectionModel.addSelectionInterval(newRowIndex, newRowIndex);
                        }
                    }
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
            // get currently/originally selected rows
            final int[] selectedRows = moduleTable.getSelectedRows();
            // fire data change event
            serverModuleFilesTableModel.fireTableDataChanged();
            // reselect originally selected row
            if (selectedRows != null && selectedRows.length > 0) {
                final ListSelectionModel selectionModel = moduleTable.getSelectionModel();
                if (selectionModel != null) {
                    selectionModel.clearSelection();
                    for (final int rowIndex : selectedRows) {
                        selectionModel.addSelectionInterval(rowIndex, rowIndex);
                    }
                } else {
                    // if there is no selectionModel select the first one
                    final int rowIndex = selectedRows[0];
                    moduleTable.setRowSelectionInterval(rowIndex, rowIndex);
                }
            }
        }
    }

    /**
     * Get all selected {@link ServerModuleFile Server Module Files}, from the Modules Table.
     *
     * @return a read-only map of {@code ServerModuleFile}'s and their {@code Goid}'s of all selected rows,
     * or an empty {@code Map} if no row is selected.
     */
    @NotNull
    private Map<Goid, ServerModuleFile> getSelectedServerModuleFiles() {
        final int[] selectedRows = moduleTable.getSelectedRows();
        final Map<Goid, ServerModuleFile> selectedModules = new HashMap<>(selectedRows.length);
        // loop through selected row
        for (final int rowIndex : selectedRows) {
            final ServerModuleFile selectedModule = moduleFromRowIndex(rowIndex);
            if (selectedModule != null) {
                selectedModules.put(selectedModule.getGoid(), selectedModule);
            }
        }
        // finally return read-only collection
        return Collections.unmodifiableMap(selectedModules);
    }

    /**
     * Utility method for getting the associated {@code ServerModuleFile} of the specified row index.
     *
     * @param rowIndex    the row index.
     * @return The {@code ServerModuleFile} associated for the specified {@code rowIndex}
     * or {@code null} if {@code rowIndex} is an invalid row (i.e. -1).
     */
    @Nullable
    private ServerModuleFile moduleFromRowIndex(final int rowIndex) {
        if (rowIndex >= 0) {
            final int modelIndex = moduleTable.convertRowIndexToModel(rowIndex);
            if (modelIndex >= 0) {
                return serverModuleFilesTableModel.getRowObject(modelIndex);
            }
        }
        return null;
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
            message = message + ":\n" + ExceptionUtils.getMessage(e);
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
     * {@link ServerModuleFile Server Module File}'s.<br/>
     * {@code Delete} button is enabled if at least one of the selected modules can be deleted by the user.
     *
     * @param selectedModules    the currently selected {@link ServerModuleFile Server Module File}'s.  Required and cannot be {@code null}.
     */
    private void enableOrDisable(@NotNull final Collection<ServerModuleFile> selectedModules) {
        final boolean isCurrentNodeSelected = isCurrentNodeSelected();
        propertiesButton.setEnabled(!selectedModules.isEmpty());
        boolean canDelete = !selectedModules.isEmpty() && canUpload && isCurrentNodeSelected;
        if (canDelete) {
            final SecurityProvider securityProvider = getSecurityProvider();
            for (final ServerModuleFile module : selectedModules) {
                canDelete = securityProvider.hasPermission(new AttemptedDeleteSpecific(EntityType.SERVER_MODULE_FILE, module));
                if (canDelete) {
                    // found at least one module that the user can delete, so break (one is enough to enable the delete button)
                    break;
                }
            }

        }
        deleteButton.setEnabled(canDelete);
        uploadButton.setEnabled(canCreate && canUpload && isCurrentNodeSelected);
    }

    /**
     * Enable or Disable buttons based on the user's (currently logged-on) RBAC permissions for currently selected
     * {@link ServerModuleFile Server Module File}'s.
     *
     * @see #enableOrDisable(java.util.Collection)
     */
    private void enableOrDisable() {
        enableOrDisable(Collections.unmodifiableCollection(getSelectedServerModuleFiles().values()));
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
