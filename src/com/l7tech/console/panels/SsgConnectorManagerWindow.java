package com.l7tech.console.panels;

import com.l7tech.common.gui.util.DialogDisplayer;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.security.rbac.EntityType;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.ISO8601Date;
import com.l7tech.common.transport.SsgConnector;
import com.l7tech.common.transport.TransportAdmin;
import com.l7tech.console.util.Registry;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.RemoteException;
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
    private ConnectorTable connectorTable;

    private PermissionFlags flags;


    public SsgConnectorManagerWindow(Frame owner) throws RemoteException {
        super(owner, "Manage Listen Ports");
        initialize();
    }

    public SsgConnectorManagerWindow(Dialog owner) throws RemoteException {
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

        loadConnectors();
        pack();
        enableOrDisableButtons();
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
        } catch (RemoteException e) {
            showErrorMessage("Remove Failed", "Failed to remove listen port: " + ExceptionUtils.getMessage(e), e);
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
            logger.info("Connector was last edited: " + connector.getProperty("lastEdited"));
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
                    connector.putProperty("lastEdited", ISO8601Date.format(new Date()));
                    try {
                        long oid = getTransportAdmin().saveSsgConnector(connector);
                        if (oid != connector.getOid()) connector.setOid(oid);
                        loadConnectors();
                        connectorTable.setSelectedConnector(connector);
                    } catch (RemoteException e) {
                        showErrorMessage("Save Failed", "Failed to save listen port: " + ExceptionUtils.getMessage(e), e);
                    } catch (SaveException e) {
                        showErrorMessage("Save Failed", "Failed to save listen port: " + ExceptionUtils.getMessage(e), e);
                    } catch (UpdateException e) {
                        showErrorMessage("Save Failed", "Failed to save listen port: " + ExceptionUtils.getMessage(e), e);
                    }
                }
            }
        });
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
        } catch (RemoteException e) {
            showErrorMessage("Deletion Failed", "Unable to delete listen port: " + ExceptionUtils.getMessage(e), e);
        } catch (FindException e) {
            showErrorMessage("Deletion Failed", "Unable to delete listen port: " + ExceptionUtils.getMessage(e), e);
        }
    }

    private void showErrorMessage(String title, String msg, Throwable e) {
        logger.log(Level.WARNING, msg, e);
        DialogDisplayer.showMessageDialog(this, msg, title, JOptionPane.ERROR_MESSAGE, null);
    }

    private static class ConnectorTableRow {
        private final SsgConnector connector;

        public ConnectorTableRow(SsgConnector connector) {
            this.connector = connector;
        }

        public SsgConnector getConnector() {
            return connector;
        }

        public Object getEnabled() {
            // TODO
            return null;
        }

        public Object getName() {
            return connector.getName();
        }

        public Object getProtocol() {
            return connector.getScheme();
        }

        public Object getInterface() {
            // TODO
            return null;
        }

        public Object getPort() {
            return connector.getPort();
        }

        public Object getAdmin() {
            // TODO
            return null;
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
    }

    private static class ConnectorTableModel extends AbstractTableModel {
        private static abstract class Col {
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
        }

        public static final Col[] columns = new Col[] {
                new Col("Emabled", 60, 90, 90) {
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

                new Col("Admin", 3, 85, 85) {
                    Object getValueForRow(ConnectorTableRow row) {
                        return row.getAdmin();
                    }
                }
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

        public void setData(List<ConnectorTableRow> rows) {
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

        /**
         * @param oid OID of connector whose row to find
         * @return the row number of the connector with a matching oid, or -1 if no match found
         */
        public int findRowByConnectorOid(long oid) {
            for (int i = 0; i < rows.size(); i++) {
                ConnectorTableRow row = rows.get(i);
                if (oid == row.getConnector().getOid())
                    return i;
            }
            return -1;
        }
    }
}
