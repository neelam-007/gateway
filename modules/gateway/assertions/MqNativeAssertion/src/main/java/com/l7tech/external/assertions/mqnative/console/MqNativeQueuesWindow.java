package com.l7tech.external.assertions.mqnative.console;

import com.l7tech.console.panels.PermissionFlags;
import com.l7tech.console.util.EntityUtils;
import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.transport.SsgActiveConnector;
import com.l7tech.gateway.common.transport.TransportAdmin;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static com.l7tech.gateway.common.transport.SsgActiveConnector.*;

/**
 * Simple modal dialog that allows management of the known MQ Native queues including designation of which
 * queues the Gateway shall monitor for incoming SOAP messages.
 */
public class MqNativeQueuesWindow extends JDialog {
    private static final Logger logger = Logger.getLogger(MqNativeQueuesWindow.class.getName());

    private JButton closeButton;
    private JButton propertiesButton;
    private JTable mqQueueTable;
    private MqQueueTableModel mqQueueTableModel;
    private JPanel sideButtonPanel;
    private JButton addButton;
    private JButton removeButton;
    private JButton cloneButton;

    private PermissionFlags flags;
    private FilterTarget filterTarget;
    private String filterString;
    private FilterDirection filterDirection;

    private enum FilterTarget {
        NAME("Name Contains", 1),
        QUEUE_NAME("Queue Name Contains", 3);

        private final String name;
        private final int tableModelColumn;

        private FilterTarget( final String name, final int tableModelColumn) {
            this.name = name;
            this.tableModelColumn = tableModelColumn;
        }

        private int getTableModelColumn() {
            return tableModelColumn;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static enum FilterDirection {
        BOTH("Both"),
        IN("In"),
        OUT("Out");

        private final String name;

        private FilterDirection(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private class MqQueueTableModel extends AbstractTableModel {
        private List<SsgActiveConnector> mqConnectors = loadMqConfigurations();

        @Override
        public int getColumnCount() {
            return 5;
        }

        @Override
        public int getRowCount() {
            if(mqConnectors == null){
                return 0;
            }else{
                return mqConnectors.size();
            }
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
                case 0:
                    return "Enabled";
                case 1:
                    return "Name";
                case 2:
                    return "Queue Manager Name";
                case 3:
                    return "Queue Name";
                case 4:
                    return "Direction";
                default:
                    return "?";
            }
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            final SsgActiveConnector connector = mqConnectors.get(rowIndex);
            switch (columnIndex) {
                case 0:
                    return connector.isEnabled()? "Yes" : "No";
                case 1:
                    return connector.getName();
                case 2:
                    return connector.getProperty(PROPERTIES_KEY_MQ_NATIVE_QUEUE_MANAGER_NAME);
                case 3:
                    return connector.getProperty(PROPERTIES_KEY_MQ_NATIVE_TARGET_QUEUE_NAME);
                case 4:
                    String direction_msg;
                    if (connector.getBooleanProperty( PROPERTIES_KEY_IS_INBOUND )) {
                        if (connector.isEnabled()) {
                            direction_msg = "Inbound (Monitored)";
                        } else {
                            direction_msg = "Inbound (Un-monitored)";
                        }
                    } else {
                        direction_msg = "Outbound from Gateway";
                    }
                    return direction_msg;
                default:
                    return "?";
            }
        }

        public List<SsgActiveConnector> getConnectors() {
            return mqConnectors;
        }

        public SsgActiveConnector getSelectedConnector(JTable listenersTable) {
            SsgActiveConnector selectedConnector = null;
            int viewRow = listenersTable.getSelectedRow();
            if(viewRow > -1) {
                int modelRow = listenersTable.convertRowIndexToModel(viewRow);
                selectedConnector = getMqQueueTableModel().getConnectors().get(modelRow);
            }
            return selectedConnector;
        }

        public void refreshLMqConfigurationsList() {
            mqConnectors = loadMqConfigurations();
        }
    }

    public MqNativeQueuesWindow(Frame owner) {
        super(owner, "Manage MQ Native Queues", true);
        initialize();
    }

    private void initialize() {
//        flags = PermissionFlags.get(JMS_ENDPOINT);

        Container p = getContentPane();
        p.setLayout(new GridBagLayout());

        p.add(new JLabel("Known MQ Native Queues:"),
          new GridBagConstraints(0, 0, 2, 1, 0.0, 0.0,
            GridBagConstraints.WEST,
            GridBagConstraints.NONE,
            new Insets(5, 5, 5, 5), 0, 0));

        p.add(getFilterPanel(),
          new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0,
            GridBagConstraints.WEST,
            GridBagConstraints.HORIZONTAL,
            new Insets(5, 5, 5, 5), 0, 0));

        JScrollPane sp = new JScrollPane(getMqQueueTable(),
          JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
          JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sp.setPreferredSize(new Dimension(520, 200));
        p.add(sp,
          new GridBagConstraints(0, 2, 1, 1, 10.0, 10.0,
            GridBagConstraints.CENTER,
            GridBagConstraints.BOTH,
            new Insets(5, 5, 5, 5), 0, 0));

        p.add(getSideButtonPanel(),
          new GridBagConstraints(1, 2, 1, GridBagConstraints.REMAINDER, 0.0, 1.0,
            GridBagConstraints.NORTH,
            GridBagConstraints.VERTICAL,
            new Insets(5, 5, 5, 5), 0, 0));

        pack();
        enableOrDisableButtons();
        Utilities.setEscKeyStrokeDisposes(this);
    }

    /**
     * Create a panel to set filter criteria.
     *
     * @return a JPanel object.
     */
    private JPanel getFilterPanel() {
        JPanel filterPanel = new JPanel();
        filterPanel.setLayout(new BoxLayout(filterPanel, BoxLayout.X_AXIS));

        final JComboBox filterTargetComboBox = new JComboBox(FilterTarget.values());
        filterPanel.add(filterTargetComboBox);

        final JTextField filterStringTextField = new JTextField();
        filterPanel.add(filterStringTextField);

        final JComboBox filterTypeComboBox = new JComboBox(FilterDirection.values());
        filterPanel.add(filterTypeComboBox);

        JButton filterButton = new JButton("Filter");
        filterButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                filterTarget = (FilterTarget)filterTargetComboBox.getSelectedItem();
                filterString = filterStringTextField.getText();
                filterDirection = (FilterDirection)filterTypeComboBox.getSelectedItem();

                // After getting new filter criteria, update the sorter and the filter
                setSorterAndFilter();
            }
        });
        filterPanel.add(filterButton);
        getRootPane().setDefaultButton(filterButton);

        return filterPanel;
    }

    private JPanel getSideButtonPanel() {
        if (sideButtonPanel == null) {
            sideButtonPanel = new JPanel();
            sideButtonPanel.setLayout(new GridBagLayout());
            sideButtonPanel.add(getAddButton(),
              new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL,
                new Insets(0, 0, 0, 0), 0, 0));
            sideButtonPanel.add(getCloneButton(),
              new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL,
                new Insets(6, 0, 0, 0), 0, 0));
            sideButtonPanel.add(getRemoveButton(),
              new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL,
                new Insets(6, 0, 0, 0), 0, 0));
            sideButtonPanel.add(getPropertiesButton(),
              new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL,
                new Insets(6, 0, 0, 0), 0, 0));
            sideButtonPanel.add(Box.createGlue(),
              new GridBagConstraints(0, 4, 1, 1, 1.0, 1.0,
                GridBagConstraints.CENTER,
                GridBagConstraints.VERTICAL,
                new Insets(0, 0, 0, 0), 0, 0));
            sideButtonPanel.add(getCloseButton(),
              new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0,
                GridBagConstraints.SOUTH,
                GridBagConstraints.HORIZONTAL,
                new Insets(0, 0, 0, 0), 0, 0));

            Utilities.equalizeButtonSizes(new JButton[]{getAddButton(),
                                                        getRemoveButton(),
                                                        getPropertiesButton(),
                                                        getCloseButton()});
        }
        return sideButtonPanel;
    }

    private JButton getRemoveButton() {
        if (removeButton == null) {
            removeButton = new JButton("Remove");
            removeButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    SsgActiveConnector connector = getMqQueueTableModel().getSelectedConnector(getMqQueueTable());
                    if (connector != null) {
                        String name = connector.getName();

                        Object[] options = {"Remove", "Cancel"};

                        int result = JOptionPane.showOptionDialog(null,
                                "<HTML>Are you sure you want to remove the " +
                                        "registration for the MQ Native Queue " +
                                        name + "?<br>" +
                                        "<center>This action cannot be undone." +
                                        "</center></html>",
                                "Remove MQ Native Queue?",
                                0, JOptionPane.WARNING_MESSAGE,
                                null, options, options[1]);
                        if (result == 0) {
                            try {
                                deleteConfiguration(connector);
                            } catch (Exception e1) {
                                throw new RuntimeException("Unable to delete queue " + name, e1);
                            }
                            updateMqList(null);
                        }
                    }
                }
            });
        }
        return removeButton;
    }

    private JButton getAddButton() {
        if (addButton == null) {
            addButton = new JButton("Add");
            addButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    final MqNativePropertiesDialog mqQueuePropertiesDialog =
                            MqNativePropertiesDialog.createInstance( MqNativeQueuesWindow.this, null, false, false );
                    mqQueuePropertiesDialog.pack();
                    Utilities.centerOnScreen(mqQueuePropertiesDialog);
                    DialogDisplayer.display(mqQueuePropertiesDialog, new Runnable() {
                        @Override
                        public void run() {
                            if (!mqQueuePropertiesDialog.isCanceled()) {
                                updateMqList(mqQueuePropertiesDialog.getTheMqResource());
                            }
                        }
                    });
                }
            });
        }
        return addButton;
    }

    private JButton getCloneButton() {
        if (cloneButton == null) {
            cloneButton = new JButton("Clone");
            cloneButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    SsgActiveConnector currMqQueue = getMqQueueTableModel().getSelectedConnector(getMqQueueTable());
                    if (currMqQueue != null) {
                        SsgActiveConnector clone = new SsgActiveConnector(currMqQueue);
                        EntityUtils.updateCopy(clone);
                        final MqNativePropertiesDialog pd = MqNativePropertiesDialog.createInstance(
                                MqNativeQueuesWindow.this, clone, false, true);
                        pd.selectNameField();
                        pd.pack();
                        Utilities.centerOnScreen(pd);
                        DialogDisplayer.display(pd, new Runnable() {
                            @Override
                            public void run() {
                                if (! pd.isCanceled()) {
                                    updateMqList(pd.getTheMqResource());
                                }
                            }
                        });
                    }
                }
            });
        }
        return cloneButton;
    }

    private JButton getCloseButton() {
        if (closeButton == null) {
            closeButton = new JButton("Close");
            closeButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    MqNativeQueuesWindow.this.dispose();
                }
            });
        }
        return closeButton;
    }

    private JButton getPropertiesButton() {
        if (propertiesButton == null) {
            propertiesButton = new JButton("Properties");
            propertiesButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    showPropertiesDialog();
                }
            });
        }
        return propertiesButton;
    }

    private void showPropertiesDialog() {
        final SsgActiveConnector connector = getMqQueueTableModel().getSelectedConnector(getMqQueueTable());
        if (connector == null) {
            // connection or endpoint has been removed some how
            DialogDisplayer.showMessageDialog(this, "MQ Native connection not found.", new Runnable() {
                @Override
                public void run() {
                    updateMqList(null);
                }
            });
        } else {
            final MqNativePropertiesDialog pd =
                    MqNativePropertiesDialog.createInstance(MqNativeQueuesWindow.this, connector, false, false);
            pd.pack();
            Utilities.centerOnScreen(pd);
            DialogDisplayer.display(pd, new Runnable() {
                @Override
                public void run() {
                    updateMqList(connector);
                }
            }); //refresh after any changes
        }
    }

    private JTable getMqQueueTable() {
        if (mqQueueTable == null) {
            mqQueueTable = new JTable(getMqQueueTableModel());
            mqQueueTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            mqQueueTable.getTableHeader().setReorderingAllowed( false );
            mqQueueTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
                @Override
                public void valueChanged(ListSelectionEvent e) {
                    enableOrDisableButtons();
                }
            });

            // Set the column widths
            mqQueueTable.getColumnModel().getColumn(0).setPreferredWidth(60);  // Enabled
            mqQueueTable.getColumnModel().getColumn(1).setPreferredWidth(120); // Name
            mqQueueTable.getColumnModel().getColumn(2).setPreferredWidth(150); // Queue Manager Name
            mqQueueTable.getColumnModel().getColumn(3).setPreferredWidth(120); // Queue Name
            mqQueueTable.getColumnModel().getColumn(4).setPreferredWidth(150); // Direction

            //Set up tool tips for the columns except "Enabled" (since its content is too short.)
            final DefaultTableCellRenderer renderer = new DefaultTableCellRenderer(){
                public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                    Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                    if((comp instanceof JComponent) && (value instanceof String)) {
                       ((JComponent)comp).setToolTipText((String) value);
                    }
                    return comp;
                }
            };
            mqQueueTable.getColumnModel().getColumn(1).setCellRenderer(renderer);
            mqQueueTable.getColumnModel().getColumn(2).setCellRenderer(renderer);
            mqQueueTable.getColumnModel().getColumn(3).setCellRenderer(renderer);
            mqQueueTable.getColumnModel().getColumn(4).setCellRenderer(renderer);

            Utilities.setDoubleClickAction(mqQueueTable, getPropertiesButton());
            // Add a sorter with a filter
            setSorterAndFilter();
        }
        return mqQueueTable;
    }

    /**
     * Set a sorter and a filter in the table.
     */
    private void setSorterAndFilter() {
        // Clean the GUI of the table.
        getContentPane().repaint();

        // Reset the sorter
        Utilities.setRowSorter(getMqQueueTable(), getMqQueueTableModel(),
            new int[] {1, 2, 3, 4}, new boolean[] {true, true, true, true}, new Comparator[] {null, null, null, null});
        TableRowSorter<MqQueueTableModel> sorter = (TableRowSorter<MqQueueTableModel>) getMqQueueTable().getRowSorter();

        // Reset the filter
        try {
            sorter.setRowFilter(getFilter());
        } catch (PatternSyntaxException e) {
            logger.info("Invalid Regular Expression, \"" + filterString + "\" :" + e.getMessage());

            DialogDisplayer.showMessageDialog(
                MqNativeQueuesWindow.this,
                "Invalid syntax for the regular expression, \"" + filterString + "\"",
                "MQ Native Queues Filtering",
                JOptionPane.WARNING_MESSAGE,
                null);
        }
    }

    /**
     * Generate a filter that will be used to show rows satisfying the setting of FilterDirection, FilterTarget, and Filter String.
     * Regular Expression Pattern Matching will be used to filter rows against FilterTarget and Filter String.
     *
     * @return a RowFilter object.
     */
    private RowFilter<MqQueueTableModel, Integer> getFilter() {
        final Pattern pattern = filterString == null? null : Pattern.compile(filterString, Pattern.CASE_INSENSITIVE);

        return new RowFilter<MqQueueTableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends MqQueueTableModel, ? extends Integer> entry) {
                 boolean canBeShown = true;

                // Check the setting of FilterDirection (BOTH, IN, or OUT)
                // If it is set as BOTH, then ignore the following checking and go to the next checking
                if (filterDirection != null && !FilterDirection.BOTH.name.equals(filterDirection.name)) {

                    MqQueueTableModel model = entry.getModel();
                    final int modelRow = entry.getIdentifier(); // Not view-based row.
                    final SsgActiveConnector mqConnector = model.getConnectors().get(modelRow);
                    final boolean isInbound = mqConnector.getBooleanProperty(PROPERTIES_KEY_IS_INBOUND);

                    canBeShown = (FilterDirection.IN.name.equals(filterDirection.name) && isInbound) ||
                                 (FilterDirection.OUT.name.equals(filterDirection.name) && !isInbound);
                }

                // Check the setting of FilterTarget (NAME or QUEUE_NAME) by using regular expression pattern matching.
                // If the filter string is not specified, then ignore the following checking.
                if (filterString != null && !filterString.trim().isEmpty() && filterTarget != null && pattern != null) {
                    int colIdx = filterTarget.getTableModelColumn(); // Since the index of FilterTarget.NAME is 0, but the index of Name column is 1.
                    Matcher matcher = pattern.matcher(entry.getStringValue(colIdx));
                    canBeShown = canBeShown && matcher.find();
                }

                return canBeShown;
            }
        };
    }

    private void enableOrDisableButtons() {
        boolean propsEnabled = false;
        boolean removeEnabled = false;
        boolean clonable = false;
            SsgActiveConnector i = getMqQueueTableModel().getSelectedConnector(getMqQueueTable());
            if (i != null) {
                removeEnabled = true;
                propsEnabled = true;
                clonable = true;
            }
        //enable/disable taking into account the permissions that this user has.
        //Must fix permissions at some later time.
//        getAddButton().setEnabled(flags.canCreateSome());
//        getRemoveButton().setEnabled(flags.canDeleteSome() && removeEnabled);
//        getCloneButton().setEnabled(flags.canCreateSome() && clonable);
        getAddButton().setEnabled(true);
        getRemoveButton().setEnabled(removeEnabled);
        getCloneButton().setEnabled(clonable);

        getPropertiesButton().setEnabled(propsEnabled);
    }

    private MqQueueTableModel getMqQueueTableModel() {
        if (mqQueueTableModel == null) {
            mqQueueTableModel = new MqQueueTableModel();
        }
        return mqQueueTableModel;
    }

    /**
     * Rebuild the endpoints table model, reloading the list from the server.  If an endpoint argument is
     * given, the row containing the specified endpoint will be selected in the new table.
     *
     * @param selectedConfiguration mq queue used to set selection highlight after the update.
     *        If it is null, then no rows will be selected.
     */
    private void updateMqList(@Nullable SsgActiveConnector selectedConfiguration) {
        // Update the queue list in the table model.
        getMqQueueTableModel().refreshLMqConfigurationsList();

        // Update the GUI of the table.
        getMqQueueTableModel().fireTableDataChanged();

        // Set the selection highlight for the saved/updated endpoint.
        if (selectedConfiguration != null) {
            List<SsgActiveConnector> modelRows = getMqQueueTableModel().getConnectors();
            for (int i = 0; i < modelRows.size(); ++i) {
                SsgActiveConnector modelItem = modelRows.get(i);
                if (modelItem != null && modelItem.getGoid().equals(selectedConfiguration.getGoid())) {
                    int viewRow = getMqQueueTable().convertRowIndexToView(i);
                    if (viewRow >= 0) {
                        getMqQueueTable().getSelectionModel().setSelectionInterval(viewRow, viewRow);
                    }
                    break;
                }
            }
        }
    }

    private TransportAdmin getTransportAdmin() {
        final Registry registry = Registry.getDefault();
        if (!registry.isAdminContextPresent()) {
            logger.warning("Admin context not present.");
            return null;
        }
        return registry.getTransportAdmin();
    }

    /*
     * Load list of listener configurations from the server.
     */
    private List<SsgActiveConnector> loadMqConfigurations(){
        List<SsgActiveConnector> listenerConfigurations = Collections.emptyList();
        try {
            final TransportAdmin transportAdmin = getTransportAdmin();
            if ( transportAdmin != null ) {
                listenerConfigurations = new ArrayList<SsgActiveConnector>(transportAdmin.findSsgActiveConnectorsByType(SsgActiveConnector.ACTIVE_CONNECTOR_TYPE_MQ_NATIVE));
            }
        } catch (FindException e) {
            throw new RuntimeException( e );
        }

        return listenerConfigurations;
          
    }

    private void deleteConfiguration( final SsgActiveConnector configuration ) throws FindException, DeleteException {
        final TransportAdmin transportAdmin = getTransportAdmin();
        if (transportAdmin != null) {
            transportAdmin.deleteSsgActiveConnector( configuration.getGoid() );
        }
    }
}