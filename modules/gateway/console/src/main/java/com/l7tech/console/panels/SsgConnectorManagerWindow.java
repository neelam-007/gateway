package com.l7tech.console.panels;

import com.l7tech.common.io.PortRange;
import com.l7tech.common.io.PortRanges;
import com.l7tech.console.action.ManageResolutionConfigurationAction;
import com.l7tech.console.util.CipherSuiteGuiUtil;
import com.l7tech.console.util.EntityUtils;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.gateway.common.transport.TransportAdmin;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.*;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Pair;

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
    private JButton interfacesButton;
    private JButton serviceResolutionButton;
    private JButton cloneButton;
    private JButton restoreFirewallDefaultButton;
    private ConnectorTable connectorTable;

    private PermissionFlags flags;
    private PortRanges reservedPorts;


    public SsgConnectorManagerWindow(Window owner) {
        super(owner, "Manage Ports");
        initialize();
    }

    private void initialize() {
        flags = PermissionFlags.get(EntityType.SSG_CONNECTOR);

        setContentPane(contentPane);
        setModalityType(ModalityType.DOCUMENT_MODAL);
        getRootPane().setDefaultButton(closeButton);

        closeButton.addActionListener(new ActionListener() {
            @Override
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

        interfacesButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                InterfaceTagsDialog.show(SsgConnectorManagerWindow.this, null);
            }
        });

        serviceResolutionButton.setAction( new ManageResolutionConfigurationAction( this ) );

        conflictLabel.setText(" ");

        Utilities.setDoubleClickAction(connectorTable, propertiesButton);

        reservedPorts = Registry.getDefault().getTransportAdmin().getReservedPorts();

        interfacesButton.setEnabled(InterfaceTagsDialog.canViewInterfaceTags());
        restoreFirewallDefaultButton.setEnabled(flags.canDeleteSome() || flags.canDeleteAll());
        restoreFirewallDefaultButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                DialogDisplayer.showSafeConfirmDialog(
                    contentPane,
                    "<html><center><p>Warning: You are about to remove all existing firewall rules.</p>" +
                            "<p>Do you wish to continue?</p></center></html>",
                    "Confirm Firewall Deletion",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    new DialogDisplayer.OptionListener() {
                        @Override
                        public void reportResult(int option) {
                            if (option == JOptionPane.CANCEL_OPTION) {
                                return;
                            }
                            final TransportAdmin ta = getTransportAdmin();
                            if(ta != null){
                                try {
                                    final Collection<SsgConnector> connectors = ta.findAllSsgConnectors();
                                    for(final SsgConnector c : connectors){
                                        if(SsgConnector.SCHEME_NA.equals(c.getScheme())){
                                            ta.deleteSsgConnector(c.getOid());
                                        }
                                    }
                                    loadConnectors();
                                } catch (FindException e1) {
                                    showErrorMessage("Deletion Failed", "Unable to find firewall port: " + ExceptionUtils.getMessage(e1), e1);
                                } catch (Exception e1) {
                                    showErrorMessage("Deletion Failed", "Unable to delete firewall port: " + ExceptionUtils.getMessage(e1), e1);
                                }
                            }
                        }
                    }
                );
            }
        });
        loadConnectors();
        pack();
        enableOrDisableButtons();
    }

    private void doRemove() {
        SsgConnector connector = connectorTable.getSelectedConnector();
        if (connector == null)
            return;

        String featureWarning = getFeatureWarningText( connector, null );
        if ( featureWarning != null ) {
            featureWarning = "\n\n" + featureWarning;
        } else {
            featureWarning = "";
        }
        String type = connector.getScheme().equals(SsgConnector.SCHEME_NA) ? "firewall rule" : "listen port";
        int result = JOptionPane.showConfirmDialog(this,
                                                   "Are you sure you want to remove the " + type + "\"" + connector.getName() + "\"?" + featureWarning,
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
        } catch (TransportAdmin.CurrentAdminConnectionException e) {
            showErrorMessage("Remove Failed", "Unable to remove the listen port for the current admin connection.", null);
        }
    }

    private void doCreate() {
        final SsgConnector connector = new SsgConnector();
        String[] known = Registry.getDefault().getTransportAdmin().getDefaultCipherSuiteNames();

        StringBuilder cipherlist = new StringBuilder();
        boolean first = true;
        for (String cipher : known) {
            if (CipherSuiteGuiUtil.cipherSuiteShouldBeCheckedByDefault(cipher)) {
                if (!first) cipherlist.append(',');
                cipherlist.append(cipher);
                first = false;
            }
        }
        connector.putProperty(SsgConnector.PROP_TLS_CIPHERLIST, cipherlist.toString());

        editAndSave(connector, connector.getReadOnlyCopy(), true);
    }


    private void doClone() {
        final SsgConnector connector = connectorTable.getSelectedConnector();
        if (connector == null)
            return;

        SsgConnector newConnector = connector.getCopy();
        EntityUtils.updateCopy( newConnector );
        editAndSave( newConnector, newConnector.getReadOnlyCopy(), true );

    }

    private void doProperties() {
        SsgConnector connector = connectorTable.getSelectedConnector();
        if (connector != null) {
            editAndSave(connector, connector.getReadOnlyCopy(),false);
        }
    }

    private void editAndSave(final SsgConnector connector, final SsgConnector originalConnector, final boolean selectName) {
        final SsgConnectorPropertiesDialog dlg = new SsgConnectorPropertiesDialog(this, connector, Registry.getDefault().getClusterStatusAdmin().isCluster());
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        if(selectName)
            dlg.selectName();
        DialogDisplayer.display(dlg, new Runnable() {
            @Override
            public void run() {
                if (dlg.isConfirmed()) {
                    Runnable reedit = new Runnable() {
                        @Override
                        public void run() {
                            loadConnectors();
                            editAndSave(connector, originalConnector, selectName);
                        }
                    };

                    if (warnAboutConflictsAndReedit(connector, reedit)) {
                        return;
                    }

                    if (warnAboutFeatures(originalConnector, connector)) {
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
                        showErrorMessage("Save Failed", "Failed to save listen port: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e), reedit);
                    } catch (UpdateException e) {
                        showErrorMessage("Save Failed", "Failed to save listen port: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e), reedit);
                    } catch (TransportAdmin.CurrentAdminConnectionException e) {
                        showErrorMessage("Save Failed", "Unable to modify the listen port for the current admin connection.", null,
                            new Runnable() {
                                @Override
                                public void run() {
                                    loadConnectors();
                                }
                            });
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
     * @param reedit a rnnable object will be run to re-edit the connector, if a conflict exists
     * @return true if conflicts were detected, false otherwise.
     */
    private boolean warnAboutConflictsAndReedit(SsgConnector connector, Runnable reedit) {
        if (connector == null) return false;

        if (reservedPorts != null) {
            Pair<PortRange, PortRange> conflict = connector.getFirstOverlappingPortRange(reservedPorts);
            if (null != conflict) {
                showConflictWarningDialog(conflict.left, conflict.right, "system reserved ports", reedit);
                return true;
            }
        }

        ConnectorTableModel model = (ConnectorTableModel)connectorTable.getModel();

        Pair<PortRange, Pair<SsgConnector, PortRange>> conflict = model.conflictChecking(connector, true);
        if ( null != conflict ) {
            SsgConnector theirConnector = conflict.right.left;
            String theirName = "connector: " + theirConnector.getName();
            showConflictWarningDialog(conflict.left, conflict.right.right, theirName, reedit);
            return true;
        } else {
            return false;
        }
    }

    private static void showConflictWarningDialog(PortRange ourRange, PortRange theirRange, String theirName, Runnable reedit) {

        String conflictMsgLeft = ourRange.isSinglePort() ?
            "Port " + ourRange.getPortStart() :
            "Port range [" + ourRange.getPortStart() + ":" + ourRange.getPortEnd() + "]";

        String conflictMsgRight = ourRange.isSinglePort() && theirRange.isSinglePort() ? theirName :
            ( theirRange.isSinglePort() ?
            "port " + theirRange.getPortStart() :
            "port range [" + theirRange.getPortStart() + ":" + theirRange.getPortEnd() + "]") +
            " of " + theirName;

        DialogDisplayer.showMessageDialog(TopComponents.getInstance().getTopParent(),
            "Port Conflict",
            conflictMsgLeft + " conflicts with " + conflictMsgRight,
            null,
            reedit);
    }

    private boolean warnAboutFeatures( final SsgConnector originalConnector, final SsgConnector editedConnector ) {
        boolean reedit = false;

        if (originalConnector != null && editedConnector != null) {
            String warningMessage = getFeatureWarningText( originalConnector, editedConnector );
            if ( warningMessage != null ) {
                String title = "Confirm Disabled Features";

                warningMessage += "\n\nSelect OK to Save or Cancel to edit.";

                if ( JOptionPane.CANCEL_OPTION == JOptionPane.showConfirmDialog(this, warningMessage, title, JOptionPane.OK_CANCEL_OPTION) ){
                    reedit = true;
                }
            }
        }

        return reedit;
    }

    private String getFeatureWarningText( final SsgConnector originalConnector, final SsgConnector editedConnector ) {
        String warningMessage = null;

        boolean disabledNodeToNode =
                (originalConnector.isEnabled() && originalConnector.offersEndpoint(SsgConnector.Endpoint.NODE_COMMUNICATION)) &&
                (editedConnector==null || !editedConnector.isEnabled() || !editedConnector.offersEndpoint(SsgConnector.Endpoint.NODE_COMMUNICATION));
        boolean disabledProcessController =
                (originalConnector.isEnabled() && originalConnector.offersEndpoint(SsgConnector.Endpoint.PC_NODE_API)) &&
                (editedConnector==null || !editedConnector.isEnabled() || !editedConnector.offersEndpoint(SsgConnector.Endpoint.PC_NODE_API));

        if ( disabledNodeToNode || disabledProcessController ) {
            warningMessage = "This will disable the following Gateway features:\n";

            if ( disabledNodeToNode ) {
                warningMessage += "\nInter-Node Communication - required for some administrative functionality (such as log viewing).";
            }
            if ( disabledProcessController ) {
                warningMessage += "\nNode Control - required for full management of the Gateway.";
            }
        }

        return warningMessage;
    }

    private void enableOrDisableButtons() {
        SsgConnector connector = connectorTable.getSelectedConnector();
        boolean haveSel = connector != null;

        createButton.setEnabled(flags.canCreateSome());
        propertiesButton.setEnabled(haveSel);
        removeButton.setEnabled(haveSel && flags.canDeleteSome());
        cloneButton.setEnabled(haveSel && flags.canCreateSome());
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

        } catch (FindException e) {
            showErrorMessage("Deletion Failed", "Unable to delete port: " + ExceptionUtils.getMessage(e), e);
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

        private ConnectorTableRow(SsgConnector connector) {
            this.connector = connector;
        }

        public SsgConnector getConnector() {
            return connector;
        }

        public Object getEnabled() {
            return connector.isEnabled() ? "Yes" : "No";
        }

        public Object getName() {
            return connector.getName();
        }

        public Object getScheme() {
            return connector.getScheme();
        }

        public Object getInterface() {
            String bindAddress = connector.getProperty(SsgConnector.PROP_BIND_ADDRESS);
            return bindAddress == null ? "(ALL)" : bindAddress;
        }

        public Object getPort() {
            return connector.getPort();
        }

        public Object getPortType(){
            final String row = connector.getScheme();
            if(SsgConnector.SCHEME_NA.equals(row)){
                final Object destPort = connector.getProperty("to-ports");
                if(destPort != null && !"".equals(destPort)) return "Firewall: Redirect to port " + destPort;
                return "Firewall";
            }
            return "Listen";
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
                TableCellRenderer cr = model.getCellRenderer(i, getDefaultRenderer(model.getColumnClass(i)));
                if (cr != null) col.setCellRenderer(cr);
            }
            Utilities.setRowSorter( this, model, new int[]{1,2,4}, new boolean[]{true, true, true}, null );
        }

        /**
         * Get a connector by view row
         */
        public ConnectorTableRow getRowAt(int row) {
            row = convertRowIndexToModel( row );
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
            rowNum = rowNum>=0 ? convertRowIndexToView( rowNum ) : rowNum;
            if (rowNum >= 0)
                getSelectionModel().setSelectionInterval(rowNum, rowNum);
            else
                getSelectionModel().clearSelection();
        }
    }

    private static class ConnectorTableModel extends AbstractTableModel {
        private Map<Long, Integer> rowMap;

        private abstract class Col {
            private final String name;
            private final int minWidth;
            private final int prefWidth;
            private final int maxWidth;
            private final Class clazz;

            protected Col( final String name,
                           final int minWidth,
                           final int prefWidth,
                           final int maxWidth,
                           final Class clazz) {
                this.name = name;
                this.minWidth = minWidth;
                this.prefWidth = prefWidth;
                this.maxWidth = maxWidth;
                this.clazz = clazz;
            }

            abstract Object getValueForRow(ConnectorTableRow row);

            public TableCellRenderer getCellRenderer(final TableCellRenderer current) {
                return new TableCellRenderer() {
                    private Color defFg;
                    private Color defSelFg;

                    @Override
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
                        ret.setForeground(isSelected ? defSelFg : defFg);

                        return ret;
                    }
                };
            }
        }

        public final Col[] columns = new Col[] {
                new Col("Enabled", 60, 90, 90, String.class) {
                    @Override
                    Object getValueForRow(ConnectorTableRow row) {
                        return row.getEnabled();
                    }
                },

                new Col("Name", 60, 90, 999999, String.class) {
                    @Override
                    Object getValueForRow(ConnectorTableRow row) {
                        return row.getName();
                    }
                },

                new Col("Scheme", 3, 100, 999999, String.class) {
                    @Override
                    Object getValueForRow(ConnectorTableRow row) {
                        return row.getScheme();
                    }
                },

                new Col("Interface", 3, 88, 999999, String.class) {
                    @Override
                    Object getValueForRow(ConnectorTableRow row) {
                        return row.getInterface();
                    }
                },

                new Col("Port", 3, 85, 85, Integer.class) {
                    @Override
                    Object getValueForRow(ConnectorTableRow row) {
                        return row.getPort();
                    }
                },
                new Col("Port Type", 3, 85, 999999, String.class) {
                    @Override
                    Object getValueForRow(ConnectorTableRow row) {
                        return row.getPortType();
                    }
                }
        };

        private final ArrayList<ConnectorTableRow> rows = new ArrayList<ConnectorTableRow>();

        public int getColumnMinWidth(int column) {
            return columns[column].minWidth;
        }

        public int getColumnPrefWidth(int column) {
            return columns[column].prefWidth;
        }

        public int getColumnMaxWidth(int column) {
            return columns[column].maxWidth;
        }

        @Override
        public String getColumnName(int column) {
            return columns[column].name;
        }

        @Override
        public Class<?> getColumnClass( final int column ) {
            return columns[column].clazz;
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

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return columns.length;
        }

        @Override
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

        /**
         * Check if there exists any port same as the port for the given connector and with the same interface.
         *
         * @param connector: the given connector to check
         * @param onlyEnabled: true to only check enabled connectors
         * @return the range (or port) in the provided connector and the conflicting connector and port range, or null if there are no conflicts
         */
        private Pair<PortRange, Pair<SsgConnector,PortRange>> conflictChecking(SsgConnector connector, boolean onlyEnabled) {
            if ( (!onlyEnabled || connector.isEnabled()) && !SsgConnector.SCHEME_NA.equals(connector.getScheme()) ) {
                for (ConnectorTableRow row: rows) {
                    if ( onlyEnabled && !row.getConnector().isEnabled() )
                        continue;

                    if (connector.getOid() == row.getConnector().getOid())
                        continue;

                    SsgConnector maybeConflictingConnector = row.getConnector();
                    Pair<PortRange, PortRange> conflict = connector.getFirstOverlappingPortRange(maybeConflictingConnector);
                    if (conflict != null) {
                        return new Pair<PortRange, Pair<SsgConnector, PortRange>>(conflict.left, new Pair<SsgConnector, PortRange>(maybeConflictingConnector, conflict.right));
                    }
                }
            }
            return null;
        }
    }
}