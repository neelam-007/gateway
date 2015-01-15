package com.l7tech.console.panels;

import com.l7tech.console.security.SecurityProvider;
import com.l7tech.console.util.*;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.gateway.common.cluster.ClusterPropertyDescriptor;
import com.l7tech.gateway.common.cluster.ClusterStatusAdmin;
import com.l7tech.gateway.common.module.ServerModuleFile;
import com.l7tech.gateway.common.module.ServerModuleFileState;
import com.l7tech.gateway.common.security.rbac.*;
import com.l7tech.gui.SimpleTableModel;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.TableUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.*;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import java.awt.*;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.gui.util.TableUtil.column;
import static com.l7tech.util.Functions.propertyTransform;

public class ServerModuleFileManagerWindow extends JDialog {
    private static final long serialVersionUID = 385196421868644404L;
    private static final Logger logger = Logger.getLogger(ServerModuleFileManagerWindow.class.getName());
    private static final ResourceBundle resources = ResourceBundle.getBundle(ServerModuleFileManagerWindow.class.getName());
    private static final String CLUSTER_PROP_UPLOAD_ENABLE = "serverModuleFile.upload.enable";

    private JPanel contentPane;
    private JTable moduleTable;
    private JButton uploadButton;
    private JButton deleteButton;
    private JButton propertiesButton;
    private JButton closeButton;
    private JPanel uploadDisabledWarningPanel;

    final private SimpleTableModel<ServerModuleFile> moduleTableModel;
    final private SecurityProvider securityProvider;
    final private boolean canCreate;
    final private boolean canUpload;

    public ServerModuleFileManagerWindow(final Window parent) {
        super(parent, resources.getString("dialog.title"), JDialog.DEFAULT_MODALITY_TYPE);
        setContentPane(contentPane);
        setModal(true);

        // determine whether the user create new ServerModuleFile entities
        securityProvider = Registry.getDefault().getSecurityProvider();
        canCreate = securityProvider.hasPermission(new AttemptedCreate(EntityType.SERVER_MODULE_FILE));

        canUpload = isModulesUploadEnabled();
        uploadDisabledWarningPanel.setVisible(!canUpload);

        closeButton.addActionListener(Utilities.createDisposeAction(this));
        Utilities.setEscAction(this, closeButton);

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

        moduleTableModel = TableUtil.configureTable(moduleTable,
                column( resources.getString("modules.column.name"), 30, 140, 99999, propertyTransform( ServerModuleFile.class, "name" ) ),
                column( resources.getString("modules.column.file-name"), 30, 250, 99999, propFinder( ServerModuleFile.PROP_FILE_NAME ) ),
                column( resources.getString("modules.column.type"), 30, 150, 150, propertyTransform( ServerModuleFile.class, "moduleType") ),
                column( resources.getString("modules.column.hash"), 50, 400, 99999, propertyTransform( ServerModuleFile.class, "moduleSha256") ),
                column( resources.getString("modules.column.size"), 20, 80, 150, propertyTransform( ServerModuleFile.class, "humanReadableFileSize") ),
                column( resources.getString("modules.column.status"), 20, 80, 150,
                        new Functions.Unary<String, ServerModuleFile>() {
                            @Override
                            public String call(final ServerModuleFile serverModuleFile) {
                                return getStateForCurrentNode(serverModuleFile);
                            }
                        }
                )
        );
        moduleTableModel.addTableModelListener(enableOrDisableListener);
        Utilities.setRowSorter(moduleTable, moduleTableModel, new int[]{0}, new boolean[]{true}, new Comparator[]{String.CASE_INSENSITIVE_ORDER});

        reloadModuleTable();

        moduleTable.getSelectionModel().addListSelectionListener(enableOrDisableListener);

        final EntityCrudController<ServerModuleFile> crud = new EntityCrudController<>();
        crud.setEntityTable(moduleTable);
        crud.setEntityTableModel(moduleTableModel);
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
                Registry.getDefault().getClusterStatusAdmin().deleteServerModuleFile(entity.getGoid());
            }
        });
        crud.setEntityDeleteConfirmer(createDialogBasedEntityDeleteConfirmer());
        crud.setEntitySaver(new EntitySaver<ServerModuleFile>() {
            @Override
            public ServerModuleFile saveEntity(ServerModuleFile entity) throws SaveException {
                try {
                    final Goid id = Registry.getDefault().getClusterStatusAdmin().saveServerModuleFile(entity);
                    entity.setGoid(id);
                } catch (FindException | UpdateException e) {
                    throw new SaveException(e);
                }

                try {
                    entity = Registry.getDefault().getClusterStatusAdmin().findServerModuleFileById(entity.getGoid(), false);
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
                final boolean readOnly = !canUpload || !securityProvider.hasPermission(operation);

                final ServerModuleFilePropertiesDialog dlg = new ServerModuleFilePropertiesDialog(
                        ServerModuleFileManagerWindow.this,
                        newMod,
                        getStateMessageForCurrentNode(newMod),
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

        uploadButton.addActionListener(crud.createCreateAction());
        propertiesButton.addActionListener(crud.createEditAction());
        Utilities.setDoubleClickAction(moduleTable, propertiesButton);
        deleteButton.addActionListener(crud.createDeleteAction());

        enableOrDisable();
    }

    /**
     * Determine whether Modules upload is enabled or not.
     * This is determined by checking the cluster wide property "serverModuleFile.upload.enable".
     */
    private boolean isModulesUploadEnabled() {
        if (Registry.getDefault().isAdminContextPresent()) {
            final ClusterStatusAdmin clusterAdmin = Registry.getDefault().getClusterStatusAdmin();
            try {
                final ClusterProperty prop = clusterAdmin.findPropertyByName(CLUSTER_PROP_UPLOAD_ENABLE);
                if (prop != null) {
                    return Boolean.valueOf(prop.getValue());
                }
                for (final ClusterPropertyDescriptor desc : clusterAdmin.getAllPropertyDescriptors()) {
                    if (desc.getName().equals(CLUSTER_PROP_UPLOAD_ENABLE)) {
                        return Boolean.valueOf(desc.getDefaultValue());
                    }
                }
                logger.log(Level.SEVERE, "Exception getting default value for cluster property :" + CLUSTER_PROP_UPLOAD_ENABLE);
            } catch (FindException e) {
                logger.log(Level.SEVERE, "Exception getting cluster property \"" + CLUSTER_PROP_UPLOAD_ENABLE + "\".", e);
            }
        }
        return false;
    }

    /**
     * Create a default delete confirmer.
     *
     * @return a new EntityDeleteConfirmer instance.  Never {@code null}.
     */
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

    private void reloadModuleTable() {
        final java.util.List<ServerModuleFile> mods = loadModuleFileDescriptors();
        moduleTableModel.setRows(mods);
    }

    /**
     * Utility method for extracting the current node {@link ServerModuleFileState state}, for the specified {@link ServerModuleFile module}.<br/>
     * If state error message is set, then the method will return the error message, otherwise the state itself will be return.
     *
     * @param moduleFile    The {@link ServerModuleFile module}.
     * @return if state error message is not blank, then the error message itself, otherwise the actual state.
     * {@link StringUtils#EMPTY Empty string} if {@code moduleFile} is {@code null} or the admin context is not present.
     */
    static String getStateMessageForCurrentNode(@Nullable final ServerModuleFile moduleFile) {
        if (moduleFile != null && Registry.getDefault().isAdminContextPresent()) {
            final ServerModuleFileState moduleState = Registry.getDefault().getClusterStatusAdmin().findServerModuleFileStateForCurrentNode(moduleFile);
            if (moduleState != null) {
                if (StringUtils.isNotBlank(moduleState.getErrorMessage())) {
                    return moduleState.getErrorMessage();
                } else {
                    return moduleState.getState().toString();
                }
            }
        }
        return StringUtils.EMPTY;
    }

    /**
     * Utility method for extracting the current node {@link ServerModuleFileState state}, for the specified {@link ServerModuleFile module}.
     *
     * @param moduleFile    The {@link ServerModuleFile module}.
     * @return if state error message is not blank, then the error message itself, otherwise the actual state.
     * {@link StringUtils#EMPTY Empty string} if {@code moduleFile} is {@code null} or the admin context is not present.
     */
    static String getStateForCurrentNode(@Nullable final ServerModuleFile moduleFile) {
        if (moduleFile != null && Registry.getDefault().isAdminContextPresent()) {
            final ServerModuleFileState moduleState = Registry.getDefault().getClusterStatusAdmin().findServerModuleFileStateForCurrentNode(moduleFile);
            if (moduleState != null) {
                return moduleState.getState().toString();
            }
        }
        return StringUtils.EMPTY;
    }

    /**
     * Loads all ServerModuleFiles from the Gateway.
     */
    private java.util.List<ServerModuleFile> loadModuleFileDescriptors() {
        java.util.List<ServerModuleFile> ret = Collections.emptyList();

        if ( Registry.getDefault().isAdminContextPresent() ) {
            try {
                ret = Registry.getDefault().getClusterStatusAdmin().findAllServerModuleFiles();
            } catch ( FindException e ) {
                showError( resources.getString("error.load.modules"), e );
            }
        }

        return ret;
    }

    /**
     * Convenient method without specifying the {@code logError} flag.
     *
     * @see #showError(String, Throwable, boolean)
     */
    private void showError( @NotNull String message, @Nullable final Throwable e ) {
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
    private static Functions.Unary<Object, ServerModuleFile> propFinder( @NotNull final String propertyName ) {
        return new Functions.Unary<Object, ServerModuleFile>() {
            @Override
            public Object call( ServerModuleFile mod ) {
                return mod.getProperty( propertyName );
            }
        };
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
                selected = moduleTableModel.getRowObject(modelIndex);
            }
        }
        return selected;
    }

    /**
     * Enable or Disable buttons based on the user's (currently logged-on) RBAC permissions for the specified
     * {@link ServerModuleFile Server Module File}.
     *
     * @param selected    the currently selected {@link ServerModuleFile Server Module File}.  Optional.
     */
    private void enableOrDisable(@Nullable final ServerModuleFile selected) {
        propertiesButton.setEnabled(selected != null);
        deleteButton.setEnabled(selected != null && canUpload && securityProvider.hasPermission(new AttemptedDeleteSpecific(EntityType.SERVER_MODULE_FILE, selected)));
        uploadButton.setEnabled(canCreate && canUpload);
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
}
