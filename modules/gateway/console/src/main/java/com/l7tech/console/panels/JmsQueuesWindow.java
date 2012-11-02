package com.l7tech.console.panels;

import com.l7tech.console.util.EntityUtils;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import static com.l7tech.objectmodel.EntityType.JMS_ENDPOINT;
import com.l7tech.objectmodel.FindException;
import com.l7tech.gateway.common.transport.jms.JmsAdmin;
import com.l7tech.gateway.common.transport.jms.JmsConnection;
import com.l7tech.gateway.common.transport.jms.JmsEndpoint;
import com.l7tech.console.util.JmsUtilities;
import com.l7tech.console.util.Registry;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Comparator;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Simple modal dialog that allows management of the known JMS queues, and designation of which
 * queues the Gateway shall monitor for incoming SOAP messages.
 *
 * @author mike
 */
public class JmsQueuesWindow extends JDialog {
    public static final int MESSAGE_SOURCE_COL = 3;
    private static final Logger logger = Logger.getLogger(JmsQueuesWindow.class.getName());

    private JButton closeButton;
    private JButton propertiesButton;
    private JTable jmsQueueTable;
    private JmsQueueTableModel jmsQueueTableModel;
    private JPanel sideButtonPanel;
    private JButton addButton;
    private JButton removeButton;
    private JButton cloneButton;

    private PermissionFlags flags;
    private FilterTarget filterTarget;
    private String filterString;
    private FilterDirection filterDirection;

    private enum FilterTarget {
        NAME("Name Contains", 0),
        URL("JNDI URL Contains", 1),
        QUEUE_NAME("Destination Name Contains", 2);

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

    public JmsQueuesWindow(Frame owner) {
        super(owner, "Manage JMS Destinations", true);
        initialize();
    }

    public JmsQueuesWindow(Dialog owner) {
        super(owner, "Manage JMS Destinations", true);
        initialize();
    }

    private void initialize() {
        flags = PermissionFlags.get(JMS_ENDPOINT);

        Container p = getContentPane();
        p.setLayout(new GridBagLayout());

        p.add(new JLabel("Known JMS Destinations:"),
          new GridBagConstraints(0, 0, 2, 1, 0.0, 0.0,
            GridBagConstraints.WEST,
            GridBagConstraints.NONE,
            new Insets(5, 5, 5, 5), 0, 0));

        p.add(getFilterPanel(),
          new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0,
            GridBagConstraints.WEST,
            GridBagConstraints.HORIZONTAL,
            new Insets(5, 5, 5, 5), 0, 0));

        JScrollPane sp = new JScrollPane(getJmsQueueTable(),
          JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
          JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sp.setPreferredSize(new Dimension(400, 200));
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

        //setSorterAndFilter();
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

    private class JmsQueueTableModel extends AbstractTableModel {
        private List<JmsAdmin.JmsTuple> jmsQueues = JmsUtilities.loadJmsQueues(false);

        @Override
        public int getColumnCount() {
            return 4;
        }

        @Override
        public int getRowCount() {
            return getJmsQueues().size();
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
                case 0:
                    return "Name";
                case 1:
                    return "JNDI URL";
                case 2:
                    return "Destination Name";
                case MESSAGE_SOURCE_COL:
                    return "Direction";
            }
            return "?";
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            JmsAdmin.JmsTuple i = getJmsQueues().get(rowIndex);
            JmsConnection conn = i.getConnection();
            JmsEndpoint end = i.getEndpoint();
            switch (columnIndex) {
                case 0:
                    return end.getName();
                case 1:
                    return conn.getJndiUrl();
                case 2:
                    return end.getDestinationName();
                case MESSAGE_SOURCE_COL:
                    String direction_msg;
                    if (end.isMessageSource()) {
                        if (end.isDisabled()) direction_msg = "Inbound (Un-monitored)";
                        else direction_msg = "Inbound (Monitored)";
                    } else {
                        if (end.isTemplate() || conn.isTemplate()) direction_msg = "Outbound (Template)";
                        else direction_msg = "Outbound from Gateway";
                    }
                    return direction_msg;
            }
            return "?";
        }

        public List<JmsAdmin.JmsTuple> getJmsQueues() {
            return jmsQueues;
        }

        public void refreshJmsQueueList() {
            jmsQueues = JmsUtilities.loadJmsQueues(false);
        }
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
                    int viewRow = getJmsQueueTable().getSelectedRow();
                    if (viewRow >= 0) {
                        int modelRow = getJmsQueueTable().convertRowIndexToModel(viewRow);
                        JmsAdmin.JmsTuple i = getJmsQueueTableModel().getJmsQueues().get(modelRow);
                        if (i != null) {
                            JmsEndpoint end = i.getEndpoint();
                            JmsConnection conn = i.getConnection();
                            String name = end.getName();

                            Object[] options = {"Remove", "Cancel"};

                            int result = JOptionPane.showOptionDialog(
                              JmsQueuesWindow.this,
                              "<HTML>Are you sure you want to remove the " +
                              "registration for the JMS Destination " +
                              name + "?<br>" +
                              "<center>This action cannot be undone." +
                              "</center></html>",
                              "Remove JMS Destination?",
                              0, JOptionPane.WARNING_MESSAGE,
                              null, options, options[1]);
                            if (result == 0) {
                                try {
                                    Registry.getDefault().getJmsManager().deleteEndpoint(end.getOid());

                                    // If the new connection would be empty, delete it too (normal operation)
                                    JmsEndpoint[] endpoints = Registry.getDefault().getJmsManager().getEndpointsForConnection(i.getConnection().getOid());
                                    if (endpoints.length < 1)
                                        Registry.getDefault().getJmsManager().deleteConnection(conn.getOid());
                                } catch (Exception e1) {
                                    throw new RuntimeException("Unable to delete queue " + name, e1);
                                }

                                updateEndpointList(null);
                            }
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
                    final JmsQueuePropertiesDialog jmsQueuePropertiesDialog = JmsQueuePropertiesDialog.createInstance(JmsQueuesWindow.this, null, null, false);
                    jmsQueuePropertiesDialog.pack();
                    Utilities.centerOnScreen(jmsQueuePropertiesDialog);
                    DialogDisplayer.display(jmsQueuePropertiesDialog, new Runnable() {
                        @Override
                        public void run() {
                            if (!jmsQueuePropertiesDialog.isCanceled()) {
                                updateEndpointList(jmsQueuePropertiesDialog.getEndpoint());
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
                    int viewRow = getJmsQueueTable().getSelectedRow();
                    if (viewRow >= 0) {
                        int modelRow = getJmsQueueTable().convertRowIndexToModel(viewRow);
                        JmsAdmin.JmsTuple currJmsTuple = getJmsQueueTableModel().getJmsQueues().get(modelRow);
                        if (currJmsTuple != null) {
                            JmsConnection newConnection = new JmsConnection();
                            newConnection.copyFrom(currJmsTuple.getConnection());
                            EntityUtils.updateCopy( newConnection );

                            JmsEndpoint newEndpoint = new JmsEndpoint();
                            newEndpoint.copyFrom(currJmsTuple.getEndpoint());
                            EntityUtils.updateCopy( newEndpoint );

                            final JmsQueuePropertiesDialog pd = JmsQueuePropertiesDialog.createInstance(
                                    JmsQueuesWindow.this, newConnection, newEndpoint, false);
                            pd.pack();
                            Utilities.centerOnScreen(pd);
                            pd.selectNameField();
                            DialogDisplayer.display(pd, new Runnable() {
                                @Override
                                public void run() {
                                    if (! pd.isCanceled()) {
                                        updateEndpointList(pd.getEndpoint());
                                    }
                                }
                            });
                        }
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
                    JmsQueuesWindow.this.dispose();
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
        int viewRow = getJmsQueueTable().getSelectedRow();
        if (viewRow >= 0) {
            int modelRow = getJmsQueueTable().convertRowIndexToModel(viewRow);
            JmsAdmin.JmsTuple i = getJmsQueueTableModel().getJmsQueues().get(modelRow);
            if (i != null) {
                //grab the latest version from the list
                long connectionOid = i.getConnection().getOid();
                long endpointOid = i.getEndpoint().getOid();
                try {
                    JmsConnection connection = Registry.getDefault().getJmsManager().findConnectionByPrimaryKey(connectionOid);
                    JmsEndpoint endpoint = Registry.getDefault().getJmsManager().findEndpointByPrimaryKey(endpointOid);
                    if (connection == null || endpoint == null) {
                        //the connection or endpoint has been removed some how
                        DialogDisplayer.showMessageDialog(this, "JMS connection not found.", refreshEndpointList(null));
                    } else {
                        final JmsQueuePropertiesDialog pd =
                                JmsQueuePropertiesDialog.createInstance(JmsQueuesWindow.this, connection, endpoint, false);
                        pd.pack();
                        Utilities.centerOnScreen(pd);
                        DialogDisplayer.display(pd, refreshEndpointList(endpoint)); //refresh after any changes
                    }
                } catch (FindException fe) {
                    DialogDisplayer.showMessageDialog(this, "JMS connection not found.", refreshEndpointList(null));
                }
            }
        }
    }

    /**
     * Generate a runnable object to refresh endpoints in the table.
     *
     * @param selectedEndpoint: the endpoint saved or updated will be highlighted in the table.
     * @return  A runnable that will refresh the endpoint list
     */
    private Runnable refreshEndpointList(final JmsEndpoint selectedEndpoint) {
        return new Runnable() {
            @Override
            public void run() {
                updateEndpointList(selectedEndpoint);
            }
        };
    }

    private JTable getJmsQueueTable() {
        if (jmsQueueTable == null) {
            jmsQueueTable = new JTable(getJmsQueueTableModel());
            jmsQueueTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            jmsQueueTable.getTableHeader().setReorderingAllowed( false );
            jmsQueueTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
                @Override
                public void valueChanged(ListSelectionEvent e) {
                    enableOrDisableButtons();
                }
            });
            Utilities.setDoubleClickAction(jmsQueueTable, getPropertiesButton());
            // Add a sorter with a filter
            setSorterAndFilter();
        }
        return jmsQueueTable;
    }

    /**
     * Set a sorter and a filter in the table.
     */
    private void setSorterAndFilter() {
        // Clean the GUI of the table.
        getContentPane().repaint();

        // Reset the sorter
        Utilities.setRowSorter(getJmsQueueTable(), getJmsQueueTableModel(),
            new int[] {0, 1, 2, 3}, new boolean[] {true, true, true, true}, null);
        TableRowSorter<JmsQueueTableModel> sorter = (TableRowSorter<JmsQueueTableModel>) getJmsQueueTable().getRowSorter();

        // Reset the filter
        try {
            sorter.setRowFilter(getFilter());
        } catch (PatternSyntaxException e) {
            logger.info("Invalid Regular Expression, \"" + filterString + "\" :" + e.getMessage());

            DialogDisplayer.showMessageDialog(
                JmsQueuesWindow.this,
                "Invalid syntax for the regular expression, \"" + filterString + "\"",
                "JMS Destinations Filtering",
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
    private RowFilter<JmsQueueTableModel, Integer> getFilter() {
        final Pattern pattern = filterString == null? null : Pattern.compile(filterString, Pattern.CASE_INSENSITIVE);

        return new RowFilter<JmsQueueTableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends JmsQueueTableModel, ? extends Integer> entry) {
                 boolean canBeShown = true;

                // Check the setting of FilterDirection (BOTH, IN, or OUT)
                // If it is set as BOTH, then ignore the following checking and go to the next checking
                if (filterDirection != null && !FilterDirection.BOTH.name.equals(filterDirection.name)) {
                    JmsQueueTableModel model = entry.getModel();
                    int modelRow = entry.getIdentifier(); // Not view-based row.
                    JmsAdmin.JmsTuple tuple = model.getJmsQueues().get(modelRow);
                    JmsEndpoint end = tuple.getEndpoint();

                    canBeShown = (FilterDirection.IN.name.equals(filterDirection.name) && end.isMessageSource()) ||
                                 (FilterDirection.OUT.name.equals(filterDirection.name) && !end.isMessageSource());
                }

                // Check the setting of FilterTarget (NAME or URL) by using regular expression pattern matching.
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
        int viewRow = getJmsQueueTable().getSelectedRow();
        if (viewRow >= 0) {
            int modelRow = getJmsQueueTable().convertRowIndexToModel(viewRow);
            JmsAdmin.JmsTuple i = getJmsQueueTableModel().getJmsQueues().get(modelRow);
            if (i != null) {
                removeEnabled = true;
                propsEnabled = true;
                clonable = true;
            }
        }
        //enable/disable taking into account the permissions that this user has.
        getAddButton().setEnabled(flags.canCreateSome());
        getRemoveButton().setEnabled(flags.canDeleteSome() && removeEnabled);
        getCloneButton().setEnabled(flags.canCreateSome() && clonable);
        getPropertiesButton().setEnabled(propsEnabled);
    }

    private JmsQueueTableModel getJmsQueueTableModel() {
        if (jmsQueueTableModel == null) {
            jmsQueueTableModel = new JmsQueueTableModel();
        }
        return jmsQueueTableModel;
    }

    /**
     * Rebuild the endpoints table model, reloading the list from the server.  If an endpoint argument is
     * given, the row containing the specified endpoint will be selected in the new table.
     *
     * @param selectedEndpoint endpoint used to set selection highlight after the update.
     *        If it is null, then no rows will be selected.
     */
    private void updateEndpointList(JmsEndpoint selectedEndpoint) {
        // Update the JMS queue list in the table model.
        getJmsQueueTableModel().refreshJmsQueueList();

        // Update the GUI of the table.
        getJmsQueueTableModel().fireTableDataChanged();

        // Set the selection highlight for the saved/updated endpoint.
        if (selectedEndpoint != null) {
            List<JmsAdmin.JmsTuple> rows = getJmsQueueTableModel().getJmsQueues();
            for (int i = 0; i < rows.size(); ++i) {
                JmsAdmin.JmsTuple item = rows.get(i);
                JmsEndpoint end = item.getEndpoint();
                if (end != null && end.getOid() == selectedEndpoint.getOid()) {
                    int viewRow = getJmsQueueTable().convertRowIndexToView(i);
                    if (viewRow >= 0) {
                        getJmsQueueTable().getSelectionModel().setSelectionInterval(viewRow, viewRow);
                    }
                    break;
                }
            }
        }
    }
}
