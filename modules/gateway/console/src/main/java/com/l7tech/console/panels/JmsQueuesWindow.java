/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 */

package com.l7tech.console.panels;

import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import static com.l7tech.gateway.common.security.rbac.EntityType.JMS_ENDPOINT;
import com.l7tech.gateway.common.transport.jms.JmsAdmin;
import com.l7tech.gateway.common.transport.jms.JmsConnection;
import com.l7tech.gateway.common.transport.jms.JmsEndpoint;
import com.l7tech.console.util.JmsUtilities;
import com.l7tech.console.util.Registry;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

/**
 * Simple modal dialog that allows management of the known JMS queues, and designation of which
 * queues the Gateway shall monitor for incoming SOAP messages.
 *
 * @author mike
 */
public class JmsQueuesWindow extends JDialog {
    public static final int MESSAGE_SOURCE_COL = 2;

    private JButton closeButton;
    private JButton propertiesButton;
    private JTable jmsQueueTable;
    private JmsQueueTableModel jmsQueueTableModel;
    private JPanel sideButtonPanel;
    private JButton addButton;
    private JButton removeButton;
    private final PermissionFlags flags;

    private JmsQueuesWindow(Frame owner) {
        super(owner, "Manage JMS Queues", true);
        flags = PermissionFlags.get(JMS_ENDPOINT);
    }

    private JmsQueuesWindow(Dialog owner) {
        super(owner, "Manage JMS Queues", true);
        flags = PermissionFlags.get(JMS_ENDPOINT);
    }

    public static JmsQueuesWindow createInstance(Window owner) {
        JmsQueuesWindow that;
        if (owner instanceof Dialog)
            that = new JmsQueuesWindow((Dialog)owner);
        else if (owner instanceof Frame)
            that = new JmsQueuesWindow((Frame)owner);
        else
            throw new IllegalArgumentException("Owner must be derived from either Frame or Window");

        Container p = that.getContentPane();
        p.setLayout(new GridBagLayout());

        p.add(new JLabel("Known JMS Queues:"),
          new GridBagConstraints(0, 0, 2, 1, 0.0, 0.0,
            GridBagConstraints.WEST,
            GridBagConstraints.NONE,
            new Insets(5, 5, 5, 5), 0, 0));

        JScrollPane sp = new JScrollPane(that.getJmsQueueTable(),
          JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
          JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sp.setPreferredSize(new Dimension(400, 200));
        p.add(sp,
          new GridBagConstraints(0, 1, 1, 1, 10.0, 10.0,
            GridBagConstraints.CENTER,
            GridBagConstraints.BOTH,
            new Insets(5, 5, 5, 5), 0, 0));

        p.add(that.getSideButtonPanel(),
          new GridBagConstraints(1, 1, 1, GridBagConstraints.REMAINDER, 0.0, 1.0,
            GridBagConstraints.NORTH,
            GridBagConstraints.VERTICAL,
            new Insets(5, 5, 5, 5), 0, 0));


        that.pack();
        that.enableOrDisableButtons();
        Utilities.setEscKeyStrokeDisposes(that);
        return that;
    }

    private class JmsQueueTableModel extends AbstractTableModel {
        private java.util.List jmsQueues = JmsUtilities.loadJmsQueues(false);

        public int getColumnCount() {
            return 3;
        }

        public int getRowCount() {
            return getJmsQueues().size();
        }

        public String getColumnName(int column) {
            switch (column) {
                case 0:
                    return "URL";
                case 1:
                    return "Queue Name";
                case MESSAGE_SOURCE_COL:
                    return "Direction";
            }
            return "?";
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            JmsAdmin.JmsTuple i = (JmsAdmin.JmsTuple)getJmsQueues().get(rowIndex);
            JmsConnection conn = i.getConnection();
            JmsEndpoint end = i.getEndpoint();
            switch (columnIndex) {
                case 0:
                    return conn.getJndiUrl();
                case 1:
                    return end.getName();
                case MESSAGE_SOURCE_COL:
                    String direction_msg;
                    if (end.isMessageSource()) {
                        if (end.isDisabled()) direction_msg = "Inbound (Un-monitored)";
                        else direction_msg = "Inbound (Monitored)";
                    } else {
                        direction_msg = "Outbound from Gateway";
                    }
                    return direction_msg;
            }
            return "?";
        }

        public List getJmsQueues() {
            return jmsQueues;
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
            sideButtonPanel.add(getRemoveButton(),
              new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL,
                new Insets(6, 0, 0, 0), 0, 0));
            sideButtonPanel.add(getPropertiesButton(),
              new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL,
                new Insets(6, 0, 0, 0), 0, 0));
            sideButtonPanel.add(Box.createGlue(),
              new GridBagConstraints(0, 3, 1, 1, 1.0, 1.0,
                GridBagConstraints.CENTER,
                GridBagConstraints.VERTICAL,
                new Insets(0, 0, 0, 0), 0, 0));
            sideButtonPanel.add(getCloseButton(),
              new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0,
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
                public void actionPerformed(ActionEvent e) {
                    int row = getJmsQueueTable().getSelectedRow();
                    if (row >= 0) {
                        JmsAdmin.JmsTuple i = (JmsAdmin.JmsTuple)getJmsQueueTableModel().getJmsQueues().get(row);
                        if (i != null) {
                            JmsEndpoint end = i.getEndpoint();
                            JmsConnection conn = i.getConnection();
                            String name = end.getName();

                            Object[] options = {"Remove", "Cancel"};

                            int result = JOptionPane.showOptionDialog(null,
                              "<HTML>Are you sure you want to remove the " +
                              "registration for the JMS Queue " +
                              name + "?<br>" +
                              "<center>This action cannot be undone." +
                              "</center></html>",
                              "Remove JMS Queue?",
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
                public void actionPerformed(ActionEvent event) {
                    final JmsQueuePropertiesDialog amew = JmsQueuePropertiesDialog.createInstance(JmsQueuesWindow.this, null, null, false);
                    amew.pack();
                    Utilities.centerOnScreen(amew);
                    DialogDisplayer.display(amew, new Runnable() {
                        public void run() {
                            if (!amew.isCanceled()) {
                                updateEndpointList(amew.getEndpoint());
                            }
                        }
                    });

                }
            });
        }
        return addButton;
    }

    private JButton getCloseButton() {
        if (closeButton == null) {
            closeButton = new JButton("Close");
            closeButton.addActionListener(new ActionListener() {
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
                public void actionPerformed(ActionEvent e) {
                    showPropertiesDialog();
                }
            });
        }
        return propertiesButton;
    }

    private void showPropertiesDialog() {
        int row = getJmsQueueTable().getSelectedRow();
        if (row >= 0) {
            JmsAdmin.JmsTuple i = (JmsAdmin.JmsTuple)getJmsQueueTableModel().getJmsQueues().get(row);
            if (i != null) {
                final JmsQueuePropertiesDialog pd =
                  JmsQueuePropertiesDialog.createInstance(JmsQueuesWindow.this, i.getConnection(), i.getEndpoint(), false);
                pd.pack();
                Utilities.centerOnScreen(pd);
                DialogDisplayer.display(pd, new Runnable() {
                    public void run() {
                        if (!pd.isCanceled()) {
                            updateEndpointList(pd.getEndpoint());
                        }
                    }
                });
            }
        }
    }

    private JTable getJmsQueueTable() {
        if (jmsQueueTable == null) {
            jmsQueueTable = new JTable(getJmsQueueTableModel());
            jmsQueueTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            jmsQueueTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent e) {
                    enableOrDisableButtons();
                }
            });
            Utilities.setDoubleClickAction(jmsQueueTable, getPropertiesButton());
        }
        return jmsQueueTable;
    }

    private void enableOrDisableButtons() {
        boolean propsEnabled = false;
        boolean removeEnabled = false;
        int row = getJmsQueueTable().getSelectedRow();
        if (row >= 0) {
            JmsAdmin.JmsTuple i = (JmsAdmin.JmsTuple)getJmsQueueTableModel().getJmsQueues().get(row);
            if (i != null) {
                removeEnabled = true;
                propsEnabled = true;
            }
        }
        //enable/disable taking into account the permissions that this user has.
        getAddButton().setEnabled(flags.canCreateSome());
        getRemoveButton().setEnabled(flags.canDeleteSome() && removeEnabled);
        getPropertiesButton().setEnabled(propsEnabled);
    }

    private JmsQueueTableModel getJmsQueueTableModel() {
        if (jmsQueueTableModel == null) {
            jmsQueueTableModel = createJmsQueueTableModel();
        }
        return jmsQueueTableModel;
    }

    private JmsQueueTableModel createJmsQueueTableModel() {
        jmsQueueTableModel = new JmsQueueTableModel();
        return jmsQueueTableModel;
    }

    /**
     * Rebuild the endpoints table model, reloading the list from the server.  If an endpoint argument is
     * given, the row containing the specified endpoint will be selected in the new table.
     *
     * @param selectedEndpoint endpoint to select after the update, or null
     */
    private void updateEndpointList(JmsEndpoint selectedEndpoint) {
        getJmsQueueTable().setModel(createJmsQueueTableModel());
        if (selectedEndpoint != null) {
            List rows = getJmsQueueTableModel().getJmsQueues();
            for (int i = 0; i < rows.size(); ++i) {
                JmsAdmin.JmsTuple item = (JmsAdmin.JmsTuple)rows.get(i);
                JmsEndpoint end = item.getEndpoint();
                if (end != null && end.getOid() == selectedEndpoint.getOid()) {
                    getJmsQueueTable().getSelectionModel().setSelectionInterval(i, i);
                    break;
                }
            }
        }
    }
}
