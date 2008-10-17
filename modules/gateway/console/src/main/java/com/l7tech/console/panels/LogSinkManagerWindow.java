package com.l7tech.console.panels;

import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gateway.common.log.LogSinkAdmin;
import com.l7tech.gateway.common.log.SinkConfiguration;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.console.util.Registry;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is the main window for managing log sinks.
 */
public class LogSinkManagerWindow extends JDialog {
    protected static final Logger logger = Logger.getLogger(LogSinkManagerWindow.class.getName());

    private static final String WINDOW_TITLE = "Manage Log Sinks";

    /** Resource bundle with default locale */
    private ResourceBundle resources = null;

    private JPanel contentPane;
    private JScrollPane mainScrollPane;
    private JButton createButton;
    private JButton removeButton;
    private JButton propertiesButton;
    private JButton closeButton;
    private LogSinkTable logSinkTable;

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
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        Utilities.setEscKeyStrokeDisposes(this);

        logSinkTable = new LogSinkTable(resources);
        mainScrollPane.setViewport(null);
        mainScrollPane.setViewportView(logSinkTable);
        mainScrollPane.getViewport().setBackground(Color.white);

        logSinkTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                enableOrDisableButtons();
            }
        });

        closeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                dispose();
            }
        });

        propertiesButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                doProperties();
            }
        });

        createButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doCreate();
            }
        });

        removeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doRemove();
            }
        });

        Utilities.setDoubleClickAction(logSinkTable, propertiesButton);

        loadSinkConfigurations();
        pack();
        enableOrDisableButtons();
    }

    private void doRemove() {
        SinkConfiguration sinkConfiguration = logSinkTable.getSelectedConnector();
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
            logSinkAdmin.deleteSinkConfiguration(sinkConfiguration.getOid());
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
        editAndSave(new SinkConfiguration());
    }

    private void doProperties() {
        SinkConfiguration sinkConfiguration = logSinkTable.getSelectedConnector();
        if (sinkConfiguration != null) {
            editAndSave(sinkConfiguration);
        }
    }

    private void editAndSave(final SinkConfiguration sinkConfiguration) {
        final SinkConfigurationPropertiesDialog dlg = new SinkConfigurationPropertiesDialog(this, sinkConfiguration);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            public void run() {
                if (dlg.isConfirmed()) {
                    Runnable reedit = new Runnable() {
                        public void run() {
                            loadSinkConfigurations();
                            editAndSave(sinkConfiguration);
                        }
                    };

                    try {
                        long oid = getLogSinkAdmin().saveSinkConfiguration(sinkConfiguration);
                        if (oid != sinkConfiguration.getOid()) sinkConfiguration.setOid(oid);
                        reedit = null;
                        loadSinkConfigurations();
                        logSinkTable.setSelectedConnector(sinkConfiguration);
                    } catch (SaveException e) {
                        showErrorMessage(resources.getString("errors.saveFailed.title"),
                                resources.getString("errors.saveFailed.message") + " " + ExceptionUtils.getMessage(e),
                                e,
                                reedit);
                    } catch (UpdateException e) {
                        showErrorMessage(resources.getString("errors.saveFailed.title"),
                                resources.getString("errors.saveFailed.message") + " " + ExceptionUtils.getMessage(e),
                                e,
                                reedit);
                    }
                }
            }
        });
    }

    private void enableOrDisableButtons() {
        SinkConfiguration sinkConfiguration = logSinkTable.getSelectedConnector();
        boolean haveSel = sinkConfiguration != null;

        createButton.setEnabled(flags.canCreateSome());
        propertiesButton.setEnabled(haveSel);
        removeButton.setEnabled(haveSel && flags.canDeleteSome());
    }

    private void showErrorMessage(String title, String msg, Throwable e) {
        showErrorMessage(title, msg, e, null);
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
                logSinkTable.setData(Collections.<LogSinkTableRow>emptyList());
                return;
            }
            Collection<SinkConfiguration> sinkConfigurations = logSinkAdmin.findAllSinkConfigurations();
            java.util.List<LogSinkTableRow> rows = new ArrayList<LogSinkTableRow>();
            for (SinkConfiguration sinkConfiguration : sinkConfigurations)
                rows.add(new LogSinkTableRow(sinkConfiguration, resources));
            logSinkTable.setData(rows);

        } catch (FindException e) {
            showErrorMessage(resources.getString("errors.loadFailed.title"),
                    resources.getString("errors.loadFailed.message") + " " + ExceptionUtils.getMessage(e),
                    e);
        }
    }

    private static class LogSinkTableRow {
        private final SinkConfiguration sinkConfiguration;
        private ResourceBundle resources = null;

        public LogSinkTableRow(SinkConfiguration sinkConfiguration, ResourceBundle resources) {
            this.sinkConfiguration = sinkConfiguration;
            this.resources = resources;
        }

        public SinkConfiguration getSinkConfiguration() {
            return sinkConfiguration;
        }

        public Object getEnabled() {
            return sinkConfiguration.isEnabled() ? resources.getString("enabledColumn.values.yes.text") : resources.getString("enabledColumn.values.no.text");
        }

        public Object getName() {
            return sinkConfiguration.getName();
        }

        public Object getType() {
            return resources.getString("typeColumn.values." + sinkConfiguration.getType().name() + ".text");
        }

        public Object getDescription() {
            return sinkConfiguration.getDescription();
        }
    }

    private static class LogSinkTable extends JTable {
        private final LogSinkTableModel model;

        LogSinkTable(ResourceBundle resources) {
            model = new LogSinkTableModel(resources);
            
            setModel(model);
            getTableHeader().setReorderingAllowed(false);
            getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            TableColumnModel cols = getColumnModel();
            int numCols = model.getColumnCount();
            for (int i = 0; i < numCols; ++i) {
                final TableColumn col = cols.getColumn(i);
                col.setMinWidth(model.getColumnMinWidth(i));
                col.setPreferredWidth(model.getColumnPrefWidth(i));
                col.setMaxWidth(model.getColumnMaxWidth(i));
                TableCellRenderer cr = model.getCellRenderer(i, getDefaultRenderer(String.class));
                if (cr != null) col.setCellRenderer(cr);
            }
        }

        public LogSinkTableRow getRowAt(int row) {
            return model.getRowAt(row);
        }

        public void setData(java.util.List<LogSinkTableRow> rows) {
            model.setData(rows);
        }

        /** @return the current selected SsgConnector, or null */
        public SinkConfiguration getSelectedConnector() {
            int rowNum = getSelectedRow();
            if (rowNum < 0)
                return null;
            LogSinkTableRow row = getRowAt(rowNum);
            if (row == null)
                return null;
            return row.getSinkConfiguration();
        }

        public void setSelectedConnector(SinkConfiguration connector) {
            int rowNum = model.findRowBySinkConfigurationOid(connector.getOid());
            if (rowNum >= 0)
                getSelectionModel().setSelectionInterval(rowNum, rowNum);
            else
                getSelectionModel().clearSelection();
        }
    }

    private static class LogSinkTableModel extends AbstractTableModel {
        private Map<Long, Integer> rowMap;

        private abstract class Col {
            final String name;
            final int minWidth;
            final int prefWidth;
            final int maxWidth;

            protected Col(String name, int minWidth, int prefWidth, int maxWidth) {
                this.name = name;
                this.minWidth = minWidth;
                this.prefWidth = prefWidth;
                this.maxWidth = maxWidth;
            }

            abstract Object getValueForRow(LogSinkTableRow row);

            public TableCellRenderer getCellRenderer(final TableCellRenderer current) {
                return new TableCellRenderer() {
                    private Color defFg;
                    private Color defSelFg;

                    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                        if (defFg == null) {
                            TableCellRenderer def1 = new DefaultTableCellRenderer();
                            Component c = def1.getTableCellRendererComponent(table, value, false, false, row, column);
                            defFg = c.getForeground();
                            TableCellRenderer def2 = new DefaultTableCellRenderer();
                            Component csel = def2.getTableCellRendererComponent(table, value, true, false, row, column);
                            defSelFg = csel.getForeground();
                        }

                        Component ret = current.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                        if (isSelected) {
                            ret.setForeground(defSelFg);
                        } else {
                            ret.setForeground(defFg);
                        }

                        return ret;
                    }
                };
            }
        }

        public final Col[] columns;

        private final ArrayList<LogSinkTableRow> rows = new ArrayList<LogSinkTableRow>();

        public LogSinkTableModel(final ResourceBundle resources) {
            columns = new Col[] {
                    new Col(resources.getString("columns.name.title"), 60, 90, 999999) {
                        Object getValueForRow(LogSinkTableRow row) {
                            return row.getName();
                        }
                    },

                    new Col(resources.getString("columns.type.title"), 3, 100, 150) {
                        Object getValueForRow(LogSinkTableRow row) {
                            return row.getType();
                        }
                    },

                    new Col(resources.getString("columns.description.title"), 60, 90, 999999) {
                        Object getValueForRow(LogSinkTableRow row) {
                            return row.getDescription();
                        }
                    },

                    new Col(resources.getString("columns.enabled.title"), 60, 90, 90) {
                        Object getValueForRow(LogSinkTableRow row) {
                            return row.getEnabled();
                        }
                    },
            };
        }

        public int getColumnMinWidth(int column) {
            return columns[column].minWidth;
        }

        public int getColumnPrefWidth(int column) {
            return columns[column].prefWidth;
        }

        public int getColumnMaxWidth(int column) {
            return columns[column].maxWidth;
        }

        public String getColumnName(int column) {
            return columns[column].name;
        }

        public TableCellRenderer getCellRenderer(int column, final TableCellRenderer current) {
            return columns[column].getCellRenderer(current);
        }

        public void setData(java.util.List<LogSinkTableRow> rows) {
            rowMap = null;
            this.rows.clear();
            this.rows.addAll(rows);
            fireTableDataChanged();
        }

        public int getRowCount() {
            return rows.size();
        }

        public int getColumnCount() {
            return columns.length;
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            return columns[columnIndex].getValueForRow(rows.get(rowIndex));
        }

        public LogSinkTableRow getRowAt(int rowIndex) {
            return rows.get(rowIndex);
        }

        /** @return a Map of Connector OID -> row number */
        private Map<Long, Integer> getRowMap() {
            if (rowMap != null)
                return rowMap;
            Map<Long, Integer> ret = new LinkedHashMap<Long, Integer>();
            for (int i = 0; i < rows.size(); i++) {
                LogSinkTableRow row = rows.get(i);
                final long oid = row.getSinkConfiguration().getOid();
                ret.put(oid, i);
            }
            return rowMap = ret;
        }

        /**
         * @param oid OID of connector whose row to find
         * @return the row number of the connector with a matching oid, or -1 if no match found
         */
        public int findRowBySinkConfigurationOid(long oid) {
            return getRowMap().containsKey(oid) ? getRowMap().get(oid) : -1;
        }
    }
}
