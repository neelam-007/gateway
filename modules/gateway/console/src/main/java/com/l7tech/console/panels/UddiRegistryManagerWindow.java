package com.l7tech.console.panels;

import com.l7tech.console.util.EntityUtils;
import com.l7tech.gateway.common.security.rbac.AttemptedUpdate;
import com.l7tech.objectmodel.*;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gateway.common.uddi.UDDIRegistry;
import com.l7tech.gateway.common.uddi.UDDIProxiedServiceInfo;
import com.l7tech.gateway.common.uddi.UDDIServiceControl;
import com.l7tech.uddi.UDDIException;
import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.admin.UDDIRegistryAdmin;
import com.l7tech.util.ExceptionUtils;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.*;
import java.awt.event.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UddiRegistryManagerWindow extends JDialog {
    private static final Logger logger = Logger.getLogger(UddiRegistryManagerWindow.class.getName());
    
    private JPanel contentPane;
    private JButton createButton;
    private JButton removeButton;
    private JButton propertiesButton;
    private JButton closeButton;
    private JScrollPane mainScrollPane;
    private JButton cloneButton;
    private UddiRegistryTable uddiRegistryTable;

    private PermissionFlags flags;

    public UddiRegistryManagerWindow(Frame owner) {
        super(owner, "Manage UDDI Registries");
        initialize();
    }

    public UddiRegistryManagerWindow() {
        initialize();
    }

    private void initialize() {
        flags = PermissionFlags.get(EntityType.UDDI_REGISTRY);

        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(closeButton);

        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        Utilities.setEscKeyStrokeDisposes(this);

        uddiRegistryTable = new UddiRegistryTable();
        mainScrollPane.setViewport(null);
        mainScrollPane.setViewportView(uddiRegistryTable);
        mainScrollPane.getViewport().setBackground(Color.white);

        Utilities.setRowSorter(uddiRegistryTable, uddiRegistryTable.getModel(), new int[]{1,2,3}, new boolean[]{true,true,true}, null);
        uddiRegistryTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
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

        Utilities.setDoubleClickAction(uddiRegistryTable, propertiesButton);

        loadUddiRegistries();
        pack();
        enableOrDisableButtons();
    }

    private void doProperties() {
        UDDIRegistry uddiRegistry = uddiRegistryTable.getSelectedUddiRegistry();
        if (uddiRegistry != null) {
            editAndSave(uddiRegistry, false);
        }
    }

    private void doCreate() {
        final UDDIRegistry uddiRegistry = new UDDIRegistry();
        editAndSave(uddiRegistry, true);
    }

    private void doClone() {
        UDDIRegistry uddiRegistry = uddiRegistryTable.getSelectedUddiRegistry();
        UDDIRegistry newUddiRegistry = new UDDIRegistry();
        newUddiRegistry.copyFrom( uddiRegistry );
        EntityUtils.updateCopy( newUddiRegistry );
        editAndSave(newUddiRegistry, true);
    }

    private void doRemove() {
        final UDDIRegistry uddiRegistry = uddiRegistryTable.getSelectedUddiRegistry();
        if (uddiRegistry == null)
            return;

        UDDIRegistryAdmin uddiRegistryAdmin = Registry.getDefault().getUDDIRegistryAdmin();
        final Collection<UDDIProxiedServiceInfo> proxiedServicesInfos;
        try {
            proxiedServicesInfos = uddiRegistryAdmin.getAllProxiedServicesForRegistry(uddiRegistry.getOid());
        } catch (FindException e) {
            showErrorMessage("Cannot Delete UDDI Registry", "Cannot determine if UDDI Registry contains published information", e);
            return;
        }

        final Collection<UDDIServiceControl> serviceControls;
        try{
            serviceControls = uddiRegistryAdmin.getAllServiceControlsForRegistry(uddiRegistry.getOid());
        } catch (FindException e) {
            showErrorMessage("Cannot Delete UDDI Registry", "Cannot determine if services were created from this registry.", e);
            return;
        }

        final String commonMsg = "Are you sure you want to remove the UDDI Registry \"" + uddiRegistry.getName() + "\"?";
        String warning = null;

        if(!proxiedServicesInfos.isEmpty() && !serviceControls.isEmpty()){
            warning = "WARNING: The Gateway has published to and created services from this UDDI registry. UDDI published information will not be removed.\n" +
                    "No published services will be removed, however any Gateway's records of this UDDI registry will be deleted.";
        }else if(!proxiedServicesInfos.isEmpty()){
            warning = "WARNING: The Gateway has published to this UDDI registry. UDDI published information will not be removed.";
        }else if(!serviceControls.isEmpty()){
            warning = "WARNING: The Gateway has created services from this UDDI registry.\n" +
                    "No published services will be removed, however any Gateway's records of this UDDI registry will be deleted.";
        }
        final String msgToUse = commonMsg + ((warning != null) ? "\n" + warning : "");

        final int msgType = (proxiedServicesInfos.isEmpty() && serviceControls.isEmpty())? JOptionPane.QUESTION_MESSAGE: JOptionPane.WARNING_MESSAGE;
        final String title = (proxiedServicesInfos.isEmpty()) ? "Confirm UDDI Registry Removal" : "WARNING: UDDI Registry in use. Confirm Removal";

        DialogDisplayer.showConfirmDialog(this,
                                                   msgToUse,
                                                   title,
                                                   JOptionPane.YES_NO_OPTION,
                                                   msgType, new DialogDisplayer.OptionListener() {
                    @Override
                    public void reportResult(int option) {
                        if (option != JOptionPane.YES_OPTION)
                            return;

                        try {
                            getUDDIRegistryAdmin().deleteUDDIRegistry(uddiRegistry.getOid());
                            loadUddiRegistries();
                        } catch (DeleteException e) {
                            showErrorMessage("Remove Failed", "Failed to remove UDDI Registry: " + ExceptionUtils.getMessage(e), e);
                        } catch (FindException e) {
                            showErrorMessage("Remove Failed", "Failed to remove UDDI Registry: " + ExceptionUtils.getMessage(e), e);
                        } catch (UDDIException e) {
                            showErrorMessage("Remove Failed", "Failed to remove UDDI Registry: " + ExceptionUtils.getMessage(e), e);
                        } catch (UDDIRegistryAdmin.UDDIRegistryNotEnabledException e) {
                            showErrorMessage("Remove Failed", "UDDI Registry is not enabled and contains published data. Cannot remove: " + ExceptionUtils.getMessage(e), e);
                        }
                    }
                });
    }

    private void editAndSave(final UDDIRegistry uddiRegistry, final boolean selectNameField) {
        //Safely check for UDDIRegistry existence when it has a non default OID.
        if(uddiRegistry.getOid() != UDDIRegistry.DEFAULT_OID){
            final Runnable errorFunction = new Runnable() {
                @Override
                public void run() {
                    showErrorMessage("UDDIRegistry not found", "UDDI Registry no longer exists. Please reopen dialog.", null);
                }
            };
            
            try {
                final UDDIRegistry reg = getUDDIRegistryAdmin().findByPrimaryKey(uddiRegistry.getOid());
                if (reg == null) {
                    //if the registry is not found and has a non default id, then it was deleted by another manager
                    errorFunction.run();
                    return;
                }

            } catch (FindException e) {
                //FindException is only thrown for a db exception, not for the entity not being found.
                errorFunction.run();
                return;
            }
        }
        final boolean canUpdate = Registry.getDefault().getSecurityProvider().hasPermission(new AttemptedUpdate(EntityType.UDDI_REGISTRY, uddiRegistry));
        final UddiRegistryPropertiesDialog dlg = new UddiRegistryPropertiesDialog(this, uddiRegistry, canUpdate);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        if(selectNameField)
            dlg.selectNameField();
        DialogDisplayer.display(dlg, new Runnable() {
            @Override
            public void run() {
                if (dlg.isConfirmed()) {
                    Runnable reedit = new Runnable() {
                        @Override
                        public void run() {
                            loadUddiRegistries();
                            editAndSave(uddiRegistry,selectNameField);
                        }
                    };

                    try {
                        long oid = getUDDIRegistryAdmin().saveUDDIRegistry(uddiRegistry);
                        if (oid != uddiRegistry.getOid()) uddiRegistry.setOid(oid);
                        reedit = null;
                        loadUddiRegistries();
                        uddiRegistryTable.setSelectedUddiRegistry(uddiRegistry);
                    } catch (UpdateException e) {
                        showErrorMessage("Update Failed", "Failed to update UDDI Registry: " + ExceptionUtils.getMessage(e),ExceptionUtils.getDebugException(e), reedit);
                    } catch (SaveException e) {
                        showErrorMessage("Save Failed", "Failed to save UDDI Registry: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e), reedit);
                    } catch (FindException e) {
                        showErrorMessage("Save / Update Failed", "Failed to save UDDI Registry: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e), reedit);
                    }
                }
            }
        });
    }

    /** @return the UDDIRegistryAdmin interface, or null if not connected or it's unavailable for some other reason */
    private UDDIRegistryAdmin getUDDIRegistryAdmin() {
        Registry reg = Registry.getDefault();
        if (!reg.isAdminContextPresent())
            return null;
        return reg.getUDDIRegistryAdmin();
    }

    private void loadUddiRegistries() {
        try {
            UDDIRegistryAdmin uddiRegistryAdmin = getUDDIRegistryAdmin();
            if (!flags.canReadSome() || uddiRegistryAdmin == null) {
                // Not connected to Gateway, or no permission to read connector list
                uddiRegistryTable.setData(Collections.<UddiRegistryTableRow>emptyList());
                return;
            }
            Collection<UDDIRegistry> registries = uddiRegistryAdmin.findAllUDDIRegistries();
            List<UddiRegistryTableRow> rows = new ArrayList<UddiRegistryTableRow>();
            for (UDDIRegistry uddiRegistry : registries)
                rows.add(new UddiRegistryTableRow(uddiRegistry));
            uddiRegistryTable.setData(rows);
        } catch (FindException e) {
            showErrorMessage("Loading failed", "Unable to list all UDDI Registry: " + ExceptionUtils.getMessage(e), e);
        }
    }

    private void showErrorMessage(String title, String msg, Throwable e) {
        showErrorMessage(title, msg, e, null);
    }

    private void showErrorMessage(String title, String msg, Throwable e, Runnable continuation) {
        logger.log(Level.WARNING, msg, e);
        DialogDisplayer.showMessageDialog(this, msg, title, JOptionPane.ERROR_MESSAGE, continuation);
    }

    private void enableOrDisableButtons() {
        UDDIRegistry uddiRegistry = uddiRegistryTable.getSelectedUddiRegistry();
        boolean haveSel = uddiRegistry != null;

        createButton.setEnabled(flags.canCreateSome());
        propertiesButton.setEnabled(haveSel);
        removeButton.setEnabled(haveSel && flags.canDeleteSome());
        cloneButton.setEnabled(haveSel && flags.canCreateSome());
    }

    private static class UddiRegistryTable extends JTable {
        private final UddiRegistryTableModel model = new UddiRegistryTableModel();

        UddiRegistryTable() {
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

        public UddiRegistryTableRow getRowAt(int row) {
            return model.getRowAt(row);
        }

        public void setData(java.util.List<UddiRegistryTableRow> rows) {
            model.setData(rows);
        }

        /** @return the current selected SsgConnector, or null */
        public UDDIRegistry getSelectedUddiRegistry() {
            int selectedRow = getSelectedRow();
            if (selectedRow < 0)
                return null;

            int rowNum = this.getRowSorter().convertRowIndexToModel(selectedRow);
            UddiRegistryTableRow row = getRowAt(rowNum);
            if (row == null)
                return null;
            return row.getUddiRegistry();
        }

        public void setSelectedUddiRegistry(UDDIRegistry uddiRegistry) {
            int rowNum = model.findRowByRegistryOid(uddiRegistry.getOid());
            if (rowNum >= 0)
                getSelectionModel().setSelectionInterval(rowNum, rowNum);
            else
                getSelectionModel().clearSelection();
        }
    }

    private static class UddiRegistryTableModel extends AbstractTableModel {
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

            abstract Object getValueForRow(UddiRegistryTableRow row);

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
                new Col("Enabled", 60, 90, 90) {
                    @Override
                    Object getValueForRow(UddiRegistryTableRow row) {
                        return row.getEnabled();
                    }
                },

                new Col("UDDI Registry Name", 60, 90, 999999) {
                    @Override
                    Object getValueForRow(UddiRegistryTableRow row) {
                        return row.getName();
                    }
                },

                new Col("Base URL", 60, 90, 999999) {
                    @Override
                    Object getValueForRow(UddiRegistryTableRow row) {
                        return row.getBaseUrl();
                    }
                },

                new Col("UDDI Registry Type", 60, 90, 999999) {
                    @Override
                    Object getValueForRow(UddiRegistryTableRow row) {
                        return row.getUddiRegistryType();
                    }
                },

        };

        private final ArrayList<UddiRegistryTableRow> rows = new ArrayList<UddiRegistryTableRow>();

        private UddiRegistryTableModel() {
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

        @Override
        public String getColumnName(int column) {
            return columns[column].name;
        }

        public TableCellRenderer getCellRenderer(int column, final TableCellRenderer current) {
            return columns[column].getCellRenderer(current);
        }

        public void setData(List<UddiRegistryTableRow> rows) {
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

        public UddiRegistryTableRow getRowAt(int rowIndex) {
            return rows.get(rowIndex);
        }

        /** @return a Map of Connector OID -> row number */
        private Map<Long, Integer> getRowMap() {
            if (rowMap != null)
                return rowMap;
            Map<Long, Integer> ret = new LinkedHashMap<Long, Integer>();
            for (int i = 0; i < rows.size(); i++) {
                UddiRegistryTableRow row = rows.get(i);
                final long oid = row.getUddiRegistry().getOid();
                ret.put(oid, i);
            }
            return rowMap = ret;
        }

        /**
         * @param oid OID of the UDDIRegistry whose row to find
         * @return the row number of the UDDIRegistry with a matching oid, or -1 if no match found
         */
        public int findRowByRegistryOid(long oid) {
            return getRowMap().containsKey(oid) ? getRowMap().get(oid) : -1;
        }
    }

    private static class UddiRegistryTableRow {
        private final UDDIRegistry uddiRegistry;

        public UddiRegistryTableRow(UDDIRegistry uddiRegistry) {
            this.uddiRegistry = uddiRegistry;
        }

        public UDDIRegistry getUddiRegistry() {
            return uddiRegistry;
        }

        public Object getEnabled() {
            return uddiRegistry.isEnabled() ? "Yes" : "No";
        }

        public Object getName() {
            return uddiRegistry.getName();
        }

        public Object getBaseUrl(){
            return uddiRegistry.getBaseUrl();
        }

        public Object getUddiRegistryType(){
            return uddiRegistry.getUddiRegistryType();
        }
    }

    public static void main(String[] args) {
        UddiRegistryManagerWindow dialog = new UddiRegistryManagerWindow();
        dialog.pack();
        dialog.setVisible(true);
        System.exit(0);
    }
}
