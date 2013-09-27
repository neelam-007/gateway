package com.l7tech.console.panels;

import com.l7tech.console.util.EntityUtils;
import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.log.LogSinkAdmin;
import com.l7tech.gateway.common.log.SinkConfiguration;
import com.l7tech.gateway.common.security.rbac.AttemptedUpdate;
import com.l7tech.gui.SimpleTableModel;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.TableUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.*;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.Functions.Unary;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.util.Functions.grepFirst;
import static com.l7tech.util.Option.optional;

/**
 * This is the main window for managing log sinks.
 */
public class LogSinkManagerWindow extends JDialog {
    protected static final Logger logger = Logger.getLogger(LogSinkManagerWindow.class.getName());

    private static final String WINDOW_TITLE = "Manage Log and Audit Sinks";

    /** Resource bundle with default locale */
    private ResourceBundle resources = null;

    private JPanel contentPane;
    private JButton createButton;
    private JButton removeButton;
    private JButton propertiesButton;
    private JButton closeButton;
    private JButton auditSinkButton;
    private JButton cloneButton;
    private JTable logSinkTable;

    private SimpleTableModel<LogSinkTableRow> tableModel;
    private PermissionFlags flags;

    /**
     * Creates a new instance of LogSinkManagerWindow.
     *
     * @param owner The owner of this dialog
     */
    public LogSinkManagerWindow(Frame owner) {
        super(owner, WINDOW_TITLE);
        initialize();
    }

    /**
     * Creates a new instance of LogSinkManagerWindow.
     *
     * @param owner The owner of this dialog
     */
    public LogSinkManagerWindow(Dialog owner) {
        super(owner, WINDOW_TITLE);
        initialize();
    }

    /**
     * Loads locale-specific resources: strings, images, etc
     */
    private void initResources() {
        Locale locale = Locale.getDefault();
        resources = ResourceBundle.getBundle("com.l7tech.console.resources.LogSinkManagerWindow", locale);
    }

    private void initialize() {
        initResources();

        setTitle(resources.getString("window.title"));
        
        flags = PermissionFlags.get(EntityType.LOG_SINK);

        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(closeButton);

        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        //noinspection unchecked
        tableModel = TableUtil.configureTable(
                logSinkTable,
                TableUtil.column( resources.getString("columns.name.title"), 60, 90, 999999, property( "name" ) ),
                TableUtil.column( resources.getString("columns.type.title"), 3, 100, 150, property( "type" ) ),
                TableUtil.column( resources.getString("columns.description.title"), 60, 90, 999999, property( "description" ) ),
                TableUtil.column( resources.getString("columns.enabled.title"), 60, 90, 90, property( "enabled" ) )
        );
        Utilities.setRowSorter( logSinkTable, tableModel, new int[]{0,1,2,3}, new boolean[]{true,true,true,true}, null );
        logSinkTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                enableOrDisableButtons();
            }
        });

        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                dispose();
            }
        });

        propertiesButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                doProperties();
            }
        });

        createButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doCreate();
            }
        });

        cloneButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doClone();
            }
        });

        removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doRemove();
            }
        });

        auditSinkButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doAuditSink();
            }
        });

        Utilities.setDoubleClickAction(logSinkTable, propertiesButton);
        Utilities.setEscKeyStrokeDisposes( this );

        loadSinkConfigurations();
        pack();
        enableOrDisableButtons();
        Utilities.setMinimumSize( this );
    }

    private void doAuditSink() {
        final AuditSinkGlobalPropertiesDialog dlg = new AuditSinkGlobalPropertiesDialog(this);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            @Override
            public void run() {
                if (dlg.isPolicyEditRequested())
                    dispose();
            }
        });
    }

    private void doRemove() {
        SinkConfiguration sinkConfiguration = getSelectedSinkConfiguration();
        if (sinkConfiguration == null)
            return;

        String message = resources.getString("confirmDelete.message.long");
        message = message.replace("$1", sinkConfiguration.getName());
        int result = JOptionPane.showConfirmDialog(this,
                                                   message,
                                                   resources.getString("confirmDelete.message.short"),
                                                   JOptionPane.YES_NO_CANCEL_OPTION,
                                                   JOptionPane.QUESTION_MESSAGE);
        if (result != JOptionPane.YES_OPTION)
            return;

        LogSinkAdmin logSinkAdmin = getLogSinkAdmin();
        if (logSinkAdmin == null)
            return;
        try {
            logSinkAdmin.deleteSinkConfiguration(sinkConfiguration.getGoid());
            loadSinkConfigurations();
        } catch (DeleteException e) {
            showErrorMessage(resources.getString("errors.removalFailed.title"),
                    resources.getString("errors.removalFailed.message") + " " + ExceptionUtils.getMessage(e),
                    e);
        } catch (FindException e) {
            showErrorMessage(resources.getString("errors.removalFailed.title"),
                    resources.getString("errors.removalFailed.message") + " " + ExceptionUtils.getMessage(e),
                    e);
        }
    }

    private void doCreate() {
        editAndSave(new SinkConfiguration(), true);
    }

    private void doClone() {
        SinkConfiguration sinkConfiguration = getSelectedSinkConfiguration();
        SinkConfiguration newSinkConfiguration = new SinkConfiguration();
        newSinkConfiguration.copyFrom(sinkConfiguration);
        EntityUtils.updateCopy( newSinkConfiguration );
        editAndSave(newSinkConfiguration, true);
    }

    private void doProperties() {
        SinkConfiguration sinkConfiguration = getSelectedSinkConfiguration();
        if (sinkConfiguration != null) {
            editAndSave(sinkConfiguration, false);
        }
    }

    private void editAndSave(final SinkConfiguration sinkConfiguration, final boolean selectNameField) {
        boolean readOnly = false;
        if (!sinkConfiguration.isUnsaved()) {
            readOnly = !Registry.getDefault().getSecurityProvider().hasPermission(new AttemptedUpdate(EntityType.LOG_SINK, sinkConfiguration));
        }
        final SinkConfigurationPropertiesDialog dlg = new SinkConfigurationPropertiesDialog(this, sinkConfiguration, readOnly);
        dlg.pack();
        Utilities.centerOnScreen( dlg );
        if(selectNameField)
            dlg.selectNameField();
        DialogDisplayer.display( dlg, new Runnable() {
            @Override
            public void run() {
                if ( dlg.isConfirmed() ) {
                    Runnable reedit = new Runnable() {
                        @Override
                        public void run() {
                            loadSinkConfigurations();
                            editAndSave( sinkConfiguration, selectNameField );
                        }
                    };

                    try {
                        Goid oid = getLogSinkAdmin().saveSinkConfiguration( sinkConfiguration );
                        if ( !Goid.equals(oid, sinkConfiguration.getGoid()) ) sinkConfiguration.setGoid( oid );
                        reedit = null;
                        loadSinkConfigurations();
                        setSelectedSinkConfiguration( sinkConfiguration );
                    } catch ( SaveException | UpdateException e ) {
                        showErrorMessage( resources.getString( "errors.saveFailed.title" ),
                                resources.getString( "errors.saveFailed.message" ) + " " + ExceptionUtils.getMessage( e ),
                                ExceptionUtils.getDebugException( e ),
                                reedit );
                    }
                }
            }
        } );
    }

    private void enableOrDisableButtons() {
        SinkConfiguration sinkConfiguration = getSelectedSinkConfiguration();
        boolean haveSel = sinkConfiguration != null;

        createButton.setEnabled(flags.canCreateSome());
        propertiesButton.setEnabled(haveSel);
        removeButton.setEnabled(haveSel && flags.canDeleteSome());
        cloneButton.setEnabled( haveSel && flags.canCreateSome() );
    }

    private void showErrorMessage(String title, String msg, Throwable e) {
        showErrorMessage( title, msg, e, null );
    }

    private void showErrorMessage(String title, String msg, Throwable e, Runnable continuation) {
        logger.log(Level.WARNING, msg, e);
        DialogDisplayer.showMessageDialog(this, msg, title, JOptionPane.ERROR_MESSAGE, continuation);
    }

    /** @return the LogSinkAdmin interface, or null if not connected or it's unavailable for some other reason */
    private LogSinkAdmin getLogSinkAdmin() {
        Registry reg = Registry.getDefault();
        if (!reg.isAdminContextPresent())
            return null;
        return reg.getLogSinkAdmin();
    }

    private void loadSinkConfigurations() {
        try {
            LogSinkAdmin logSinkAdmin = getLogSinkAdmin();
            if (!flags.canReadSome() || logSinkAdmin == null) {
                // Not connected to Gateway, or no permission to read connector list
                tableModel.setRows(Collections.<LogSinkTableRow>emptyList());
                return;
            }
            Collection<SinkConfiguration> sinkConfigurations = logSinkAdmin.findAllSinkConfigurations();
            java.util.List<LogSinkTableRow> rows = new ArrayList<LogSinkTableRow>();
            for (SinkConfiguration sinkConfiguration : sinkConfigurations)
                rows.add(new LogSinkTableRow(sinkConfiguration, resources));
            tableModel.setRows(rows);

        } catch (FindException e) {
            showErrorMessage(resources.getString("errors.loadFailed.title"),
                    resources.getString("errors.loadFailed.message") + " " + ExceptionUtils.getMessage(e),
                    e);
        }
    }

    private SinkConfiguration getSelectedSinkConfiguration() {
        SinkConfiguration configuration = null;
        final int viewRow = logSinkTable.getSelectedRow();
        if ( viewRow > -1 ) {
            final int modelRow = logSinkTable.convertRowIndexToModel( viewRow );
            configuration = optional( tableModel.getRowObject( modelRow ) ).map( new Unary<SinkConfiguration,LogSinkTableRow>(){
                @Override
                public SinkConfiguration call( final LogSinkTableRow logSinkTableRow ) {
                    return logSinkTableRow.getSinkConfiguration();
                }
            } ).toNull();
        }
        return configuration;
    }

    private void setSelectedSinkConfiguration( final SinkConfiguration sinkConfiguration ) {
        if ( sinkConfiguration != null ) {
            final LogSinkTableRow row = grepFirst( tableModel.getRows(), new Unary<Boolean, LogSinkTableRow>() {
                @Override
                public Boolean call( final LogSinkTableRow logSinkTableRow ) {
                    return Goid.equals(logSinkTableRow.getSinkConfiguration().getGoid(), sinkConfiguration.getGoid());
                }
            } );
            if ( row != null ) {
                final int viewRow = logSinkTable.convertRowIndexToView( tableModel.getRowIndex( row ) );
                logSinkTable.getSelectionModel().setSelectionInterval( viewRow, viewRow );
            }
        } else {
            logSinkTable.clearSelection();
        }
    }

    private static Functions.Unary<String,LogSinkTableRow> property(String propName) {
        return Functions.propertyTransform(LogSinkTableRow.class, propName);
    }

    public static class LogSinkTableRow {
        private final SinkConfiguration sinkConfiguration;
        private final ResourceBundle resources;

        public LogSinkTableRow( final SinkConfiguration sinkConfiguration,
                                final ResourceBundle resources ) {
            this.sinkConfiguration = sinkConfiguration;
            this.resources = resources;
        }

        private SinkConfiguration getSinkConfiguration() {
            return sinkConfiguration;
        }

        public String getEnabled() {
            return sinkConfiguration.isEnabled() ? resources.getString("enabledColumn.values.yes.text") : resources.getString("enabledColumn.values.no.text");
        }

        public String getName() {
            return sinkConfiguration.getName();
        }

        public String getType() {
            return resources.getString("typeColumn.values." + sinkConfiguration.getType().name() + ".text");
        }

        public String getDescription() {
            return sinkConfiguration.getDescription();
        }
    }
}
