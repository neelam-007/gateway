package com.l7tech.console.panels;

import com.l7tech.common.gui.util.DialogDisplayer;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.log.LogSinkAdmin;
import com.l7tech.common.log.SinkConfiguration;
import com.l7tech.common.security.rbac.EntityType;
import com.l7tech.common.util.ExceptionUtils;
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
        super(owner, "Manage Listen Ports");
        initialize();
    }

    /**
     * Creates a new instance of LogSinkManagerWindow.
     *
     * @param owner The owner of this dialog
     */
    public LogSinkManagerWindow(Dialog owner) {
        super(owner, "Manage Listen Ports");
        initialize();
    }

    private void initialize() {
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

        logSinkTable = new LogSinkTable();
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

        loadSinkConfigurations();
        pack();
        enableOrDisableButtons();
    }

    private void doRemove() {
        SinkConfiguration sinkConfiguration = logSinkTable.getSelectedConnector();
        if (sinkConfiguration == null)
            return;

        int result = JOptionPane.showConfirmDialog(this,
                                                   "Are you sure you want to remove the log sink \"" + sinkConfiguration.getName() + "\"?",
                                                   "Confirm Removal",
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
            showErrorMessage("Remove Failed", "Failed to remove listen port: " + ExceptionUtils.getMessage(e), e);
        } catch (FindException e) {
            showErrorMessage("Remove Failed", "Failed to remove listen port: " + ExceptionUtils.getMessage(e), e);
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
                        showErrorMessage("Save Failed", "Failed to save log sink: " + ExceptionUtils.getMessage(e), e, reedit);
                    } catch (UpdateException e) {
                        showErrorMessage("Save Failed", "Failed to save log sink: " + ExceptionUtils.getMessage(e), e, reedit);
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

    /** @return the TransportAdmin interface, or null if not connected or it's unavailable for some other reason */
    private LogSinkAdmin getLogSinkAdmin() {
        Registry reg = Registry.getDefault();
        if (!reg.isAdminContextPresent())
            return null;
        return reg.getLogSinkAdmin();
    }

    private void loadSinkConfigurations() {
        try {
            LogSinkAdmin logSinkAdmin = getLogSinkAdmin();
            if (/*!flags.canReadSome() || */logSinkAdmin == null) {
                // Not connected to Gateway, or no permission to read connector list
                logSinkTable.setData(Collections.<LogSinkTableRow>emptyList());
                return;
            }
            Collection<SinkConfiguration> sinkConfigurations = logSinkAdmin.findAllSinkConfigurations();
            java.util.List<LogSinkTableRow> rows = new ArrayList<LogSinkTableRow>();
            for (SinkConfiguration sinkConfiguration : sinkConfigurations)
                rows.add(new LogSinkTableRow(sinkConfiguration));
            logSinkTable.setData(rows);

        } catch (FindException e) {
            showErrorMessage("Deletion Failed", "Unable to delete listen port: " + ExceptionUtils.getMessage(e), e);
        }
    }

    private static class LogSinkTableRow {
        private final SinkConfiguration sinkConfiguration;

        public LogSinkTableRow(SinkConfiguration sinkConfiguration) {
            this.sinkConfiguration = sinkConfiguration;
        }

        public SinkConfiguration getSinkConfiguration() {
            return sinkConfiguration;
        }

        public Object getEnabled() {
            return sinkConfiguration.isEnabled() ? "Yes" : "No";
        }

        public Object getName() {
            return sinkConfiguration.getName();
        }

        public Object getType() {
            return sinkConfiguration.getType();
        }

        public Object getDescription() {
            return sinkConfiguration.getDescription();
        }
    }

    private static class LogSinkTable extends JTable {
        private final LogSinkTableModel model = new LogSinkTableModel();

        LogSinkTable() {
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

        public final Col[] columns = new Col[] {
                new Col("Name", 60, 90, 999999) {
                    Object getValueForRow(LogSinkTableRow row) {
                        return row.getName();
                    }
                },

                new Col("Type", 3, 100, 150) {
                    Object getValueForRow(LogSinkTableRow row) {
                        return row.getType();
                    }
                },

                new Col("Description", 60, 90, 999999) {
                    Object getValueForRow(LogSinkTableRow row) {
                        return row.getDescription();
                    }
                },

                new Col("Enabled", 60, 90, 90) {
                    Object getValueForRow(LogSinkTableRow row) {
                        return row.getEnabled();
                    }
                },
        };

        private final ArrayList<LogSinkTableRow> rows = new ArrayList<LogSinkTableRow>();

        public LogSinkTableModel() {
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
