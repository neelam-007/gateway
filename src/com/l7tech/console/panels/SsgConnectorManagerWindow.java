package com.l7tech.console.panels;

import com.l7tech.common.gui.util.DialogDisplayer;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.security.rbac.EntityType;
import com.l7tech.common.transport.SsgConnector;
import com.l7tech.common.transport.TransportAdmin;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.Pair;
import com.l7tech.common.util.Triple;
import com.l7tech.common.io.PortRange;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SsgConnectorManagerWindow extends JDialog {
    protected static final Logger logger = Logger.getLogger(SsgConnectorManagerWindow.class.getName());

    private JPanel contentPane;
    private JButton closeButton;
    private JButton createButton;
    private JButton removeButton;
    private JButton propertiesButton;
    private JScrollPane mainScrollPane;
    private JLabel conflictLabel;
    private ConnectorTable connectorTable;

    private PermissionFlags flags;


    public SsgConnectorManagerWindow(Frame owner) {
        super(owner, "Manage Listen Ports");
        initialize();
    }

    public SsgConnectorManagerWindow(Dialog owner) {
        super(owner, "Manage Listen Ports");
        initialize();
    }

    private void initialize() {
        flags = PermissionFlags.get(EntityType.SSG_CONNECTOR);

        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(closeButton);

        closeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        Utilities.setEscKeyStrokeDisposes(this);

        connectorTable = new ConnectorTable();
        mainScrollPane.setViewport(null);
        mainScrollPane.setViewportView(connectorTable);
        mainScrollPane.getViewport().setBackground(Color.white);

        connectorTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                reportConflicts();
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

        conflictLabel.setText(" ");

        loadConnectors();
        pack();
        enableOrDisableButtons();
    }

    private void reportConflicts() {
        conflictLabel.setText(connectorTable.getConflictString());
    }

    private void doRemove() {
        SsgConnector connector = connectorTable.getSelectedConnector();
        if (connector == null)
            return;

        int result = JOptionPane.showConfirmDialog(this,
                                                   "Are you sure you want to remove the listen port \"" + connector.getName() + "\"?",
                                                   "Confirm Removal",
                                                   JOptionPane.YES_NO_CANCEL_OPTION,
                                                   JOptionPane.QUESTION_MESSAGE);
        if (result != JOptionPane.YES_OPTION)
            return;

        TransportAdmin ta = getTransportAdmin();
        if (ta == null)
            return;
        try {
            ta.deleteSsgConnector(connector.getOid());
            loadConnectors();
        } catch (DeleteException e) {
            showErrorMessage("Remove Failed", "Failed to remove listen port: " + ExceptionUtils.getMessage(e), e);
        } catch (FindException e) {
            showErrorMessage("Remove Failed", "Failed to remove listen port: " + ExceptionUtils.getMessage(e), e);
        }
    }

    private void doCreate() {
        editAndSave(new SsgConnector());
    }

    private void doProperties() {
        SsgConnector connector = connectorTable.getSelectedConnector();
        if (connector != null) {
            editAndSave(connector);
        }
    }

    private void editAndSave(final SsgConnector connector) {
        final SsgConnectorPropertiesDialog dlg = new SsgConnectorPropertiesDialog(this, connector);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            public void run() {
                if (dlg.isConfirmed()) {
                    Runnable reedit = new Runnable() {
                        public void run() {
                            loadConnectors();
                            editAndSave(connector);
                        }
                    };

                    if (warnAboutConflicts(connector)) {
                        reedit.run();
                        return;
                    }

                    try {
                        long oid = getTransportAdmin().saveSsgConnector(connector);
                        if (oid != connector.getOid()) connector.setOid(oid);
                        reedit = null;
                        loadConnectors();
                        connectorTable.setSelectedConnector(connector);
                    } catch (SaveException e) {
                        showErrorMessage("Save Failed", "Failed to save listen port: " + ExceptionUtils.getMessage(e), e, reedit);
                    } catch (UpdateException e) {
                        showErrorMessage("Save Failed", "Failed to save listen port: " + ExceptionUtils.getMessage(e), e, reedit);
                    }
                }
            }
        });
    }

    /**
     * Check if the specified possibly-unsaved connector conflicts with any other ports known to be
     * in use in the system and, if so, display a warning dialog.
     *
     * @param connector the connector to check
     * @return true if conflicts were detected and the user opted not to continue anyway;
     *         false if no conflicts were detected or the user chose to ignore them
     */
    private boolean warnAboutConflicts(SsgConnector connector) {
        try {
            Collection<Pair<PortRange,String>> conflicts = getTransportAdmin().findPortConflicts(connector);
            if (conflicts == null || conflicts.isEmpty())
                return false;

            Pair<PortRange, String> conflict = conflicts.iterator().next();
            PortRange range = conflict.left;
            if (range == null) {
                logger.warning("Port conflict: range is null"); // can't happen
                return false;
            }
            String message = explainConflict(conflict);

            String cancelOption = "Cancel";
            String saveOption = "Save Anyway";
            String[] options = new String[] { saveOption, cancelOption };

            int option = JOptionPane.showOptionDialog(TopComponents.getInstance().getTopParent(),
                                                      message,
                                                      "Port Conflict Detected",
                                                      JOptionPane.CANCEL_OPTION,
                                                      JOptionPane.WARNING_MESSAGE,
                                                      null,
                                                      options,
                                                      cancelOption);
            return option != 0;

        } catch (FindException e) {
            logger.log(Level.WARNING, "Unable to check for port conflicts: " + ExceptionUtils.getMessage(e), e);
            return false;
        }
    }

    private static String explainConflict(Pair<PortRange, String> conflict) {
        PortRange range = conflict.left;
        String displayRange = range.getPortStart() == range.getPortEnd()
                              ? "The port " + range.getPortStart()
                              : "The port range from " + range.getPortStart() + " to " + range.getPortEnd();

        String partition = conflict.right;
        String displayPart = partition == null ? "" : " by the partition \"" + partition + "\"";

        return displayRange + " conflicts with ports already in use" + displayPart + ".";
    }

    private void enableOrDisableButtons() {
        SsgConnector connector = connectorTable.getSelectedConnector();
        boolean haveSel = connector != null;

        createButton.setEnabled(flags.canCreateSome());
        propertiesButton.setEnabled(haveSel);
        removeButton.setEnabled(haveSel && flags.canDeleteSome());
    }

    /** @return the TransportAdmin interface, or null if not connected or it's unavailable for some other reason */
    private TransportAdmin getTransportAdmin() {
        Registry reg = Registry.getDefault();
        if (!reg.isAdminContextPresent())
            return null;
        return reg.getTransportAdmin();
    }

    private void loadConnectors() {
        try {
            TransportAdmin transportAdmin = getTransportAdmin();
            if (!flags.canReadSome() || transportAdmin == null) {
                // Not connected to Gateway, or no permission to read connector list
                connectorTable.setData(Collections.<ConnectorTableRow>emptyList());
                return;
            }
            Collection<SsgConnector> connectors = transportAdmin.findAllSsgConnectors();
            List<ConnectorTableRow> rows = new ArrayList<ConnectorTableRow>();
            for (SsgConnector connector : connectors)
                rows.add(new ConnectorTableRow(connector));
            connectorTable.setData(rows);

            Collection<Triple<Long,PortRange,String>> conflicts = transportAdmin.findAllPortConflicts();
            for (Triple<Long, PortRange, String> conflict : conflicts) {
                connectorTable.flagConflict(conflict);
            }

        } catch (FindException e) {
            showErrorMessage("Deletion Failed", "Unable to delete listen port: " + ExceptionUtils.getMessage(e), e);
        }
    }

    private void showErrorMessage(String title, String msg, Throwable e) {
        showErrorMessage(title, msg, e, null);
    }

    private void showErrorMessage(String title, String msg, Throwable e, Runnable continuation) {
        logger.log(Level.WARNING, msg, e);
        DialogDisplayer.showMessageDialog(this, msg, title, JOptionPane.ERROR_MESSAGE, continuation);
    }

    private static class ConnectorTableRow {
        private final SsgConnector connector;
        private Pair<PortRange, String> conflict;

        public ConnectorTableRow(SsgConnector connector) {
            this.connector = connector;
        }

        public SsgConnector getConnector() {
            return connector;
        }

        public Pair<PortRange, String> getConflict() {
            return conflict;
        }

        public void setConflict(Pair<PortRange, String> conflict) {
            this.conflict = conflict;
        }

        public Object getEnabled() {
            return connector.isEnabled() ? "Yes" : "No";
        }

        public Object getName() {
            return connector.getName();
        }

        public Object getProtocol() {
            return connector.getScheme();
        }

        public Object getInterface() {
            String bindAddress = connector.getProperty(SsgConnector.PROP_BIND_ADDRESS);
            return bindAddress == null ? "(ALL)" : bindAddress;
        }

        public Object getPort() {
            return connector.getPort();
        }

        public Object getMA() {
            return connector.offersEndpoint(SsgConnector.Endpoint.ADMIN_REMOTE) ? "Y" : "";
        }

        public Object getAW() {
            return connector.offersEndpoint(SsgConnector.Endpoint.ADMIN_APPLET) ? "Y" : "";
        }

        public Object getPS() {
            return connector.offersEndpoint(SsgConnector.Endpoint.MESSAGE_INPUT) ? "Y" : "";
        }

        public Object getBS() {
            return connector.offersEndpoint(SsgConnector.Endpoint.OTHER_SERVLETS) ? "Y" : "";
        }
    }

    private static class ConnectorTable extends JTable {
        private final ConnectorTableModel model = new ConnectorTableModel();

        ConnectorTable() {
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
                TableCellRenderer hr = model.getHeaderRenderer(i, getTableHeader().getDefaultRenderer());
                if (hr != null) col.setHeaderRenderer(hr);
                TableCellRenderer cr = model.getCellRenderer(i, getDefaultRenderer(String.class));
                if (cr != null) col.setCellRenderer(cr);
            }
        }

        public ConnectorTableRow getRowAt(int row) {
            return model.getRowAt(row);
        }

        public void setData(List<ConnectorTableRow> rows) {
            model.setData(rows);
        }

        /** @return the current selected SsgConnector, or null */
        public SsgConnector getSelectedConnector() {
            int rowNum = getSelectedRow();
            if (rowNum < 0)
                return null;
            ConnectorTableRow row = getRowAt(rowNum);
            if (row == null)
                return null;
            return row.getConnector();
        }

        public void setSelectedConnector(SsgConnector connector) {
            int rowNum = model.findRowByConnectorOid(connector.getOid());
            if (rowNum >= 0)
                getSelectionModel().setSelectionInterval(rowNum, rowNum);
            else
                getSelectionModel().clearSelection();
        }

        public void flagConflict(Triple<Long, PortRange, String> conflict) {
            model.flagConflict(conflict);
        }

        public String getConflictString() {
            int rowIndex = getSelectedRow();
            if (rowIndex < 0)
                return " ";
            ConnectorTableRow row = model.getRowAt(rowIndex);
            Pair<PortRange, String> conflict = row.getConflict();
            if (conflict == null)
                return " ";
            return explainConflict(conflict);
        }
    }

    private static class ConnectorTableModel extends AbstractTableModel {
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

            abstract Object getValueForRow(ConnectorTableRow row);

            public TableCellRenderer getHeaderRenderer(final TableCellRenderer current) {
                return null;
            }

            public TableCellRenderer getCellRenderer(final TableCellRenderer current) {
                return new TableCellRenderer() {
                    private Color defFg;
                    private Color defSelFg;
                    private Color conflictColor = new Color(128, 0, 0);

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

                        boolean conflict = rows.get(row).getConflict() != null;

                        if (isSelected) {
                            ret.setForeground(conflict ? conflictColor : defSelFg);
                        } else {
                            ret.setForeground(conflict ? conflictColor : defFg);
                        }

                        return ret;
                    }
                };
            }
        }

        private static final int NARROW_COL_WIDTH = 20;
        private abstract class NarrowCol extends Col {
            protected NarrowCol(String name) {
                super(name, NARROW_COL_WIDTH, NARROW_COL_WIDTH, NARROW_COL_WIDTH);
            }

            public TableCellRenderer getHeaderRenderer(final TableCellRenderer current) {
                return new TableCellRenderer() {
                    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                        JLabel c = (JLabel)current.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                        c.setText(NarrowCol.this.name);
                        c.setBorder(null);
                        c.setHorizontalAlignment(SwingConstants.LEFT);
                        return c;
                    }
                };
            }
        }

        public final Col[] columns = new Col[] {
                new Col("Enabled", 60, 90, 90) {
                    Object getValueForRow(ConnectorTableRow row) {
                        return row.getEnabled();
                    }
                },

                new Col("Name", 60, 90, 999999) {
                    Object getValueForRow(ConnectorTableRow row) {
                        return row.getName();
                    }
                },

                new Col("Protocol", 3, 100, 150) {
                    Object getValueForRow(ConnectorTableRow row) {
                        return row.getProtocol();
                    }
                },

                new Col("Interface", 3, 88, 88) {
                    Object getValueForRow(ConnectorTableRow row) {
                        return row.getInterface();
                    }
                },

                new Col("Port", 3, 85, 85) {
                    Object getValueForRow(ConnectorTableRow row) {
                        return row.getPort();
                    }
                },

                new NarrowCol("PS") {
                    Object getValueForRow(ConnectorTableRow row) {
                        return row.getPS();
                    }
                },

                new NarrowCol("BS") {
                    Object getValueForRow(ConnectorTableRow row) {
                        return row.getBS();
                    }
                },

                new NarrowCol("MA") {
                    Object getValueForRow(ConnectorTableRow row) {
                        return row.getMA();
                    }
                },

                new NarrowCol("PS") {
                    Object getValueForRow(ConnectorTableRow row) {
                        return row.getAW();
                    }
                },
        };

        private final ArrayList<ConnectorTableRow> rows = new ArrayList<ConnectorTableRow>();

        public ConnectorTableModel() {
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

        public TableCellRenderer getHeaderRenderer(int column, final TableCellRenderer current) {
            return columns[column].getHeaderRenderer(current);
        }

        public TableCellRenderer getCellRenderer(int column, final TableCellRenderer current) {
            return columns[column].getCellRenderer(current);
        }

        public void setData(List<ConnectorTableRow> rows) {
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

        public ConnectorTableRow getRowAt(int rowIndex) {
            return rows.get(rowIndex);
        }

        /** @return a Map of Connector OID -> row number */
        private Map<Long, Integer> getRowMap() {
            if (rowMap != null)
                return rowMap;
            Map<Long, Integer> ret = new LinkedHashMap<Long, Integer>();
            for (int i = 0; i < rows.size(); i++) {
                ConnectorTableRow row = rows.get(i);
                final long oid = row.getConnector().getOid();
                ret.put(oid, i);
            }
            return rowMap = ret;
        }

        /**
         * @param oid OID of connector whose row to find
         * @return the row number of the connector with a matching oid, or -1 if no match found
         */
        public int findRowByConnectorOid(long oid) {
            return getRowMap().containsKey(oid) ? getRowMap().get(oid) : -1;
        }

        public void flagConflict(Triple<Long, PortRange, String> conflict) {
            final Long oid = conflict.left;
            if (!getRowMap().containsKey(oid)) return;
            int rowIndex = getRowMap().get(oid);
            rows.get(rowIndex).setConflict(new Pair<PortRange, String>(conflict.middle, conflict.right));
            fireTableRowsUpdated(rowIndex, rowIndex);
        }
    }
}
