/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.console.panels;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.transport.jms.JmsConnection;
import com.l7tech.common.transport.jms.JmsEndpoint;
import com.l7tech.console.util.Registry;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

/**
 * Simple modal dialog that allows management of the known JMS endpoints, and designation of which
 * endpoints the Gateway shall monitor for incoming SOAP messages.
 *
 * @author mike
 */
public class JmsEndpointsWindow extends JDialog {
    public static final int MESSAGE_SOURCE_COL = 2;

    private JButton closeButton;
    private JButton propertiesButton;
    private JTable endpointTable;
    private EndpointTableModel endpointTableModel;
    private JPanel sideButtonPanel;
    private JButton addButton;
    private JButton removeButton;

    public JmsEndpointsWindow(Frame owner) {
        super(owner, "JMS Queues", true);
        Container p = getContentPane();
        p.setLayout(new GridBagLayout());

        p.add(new JLabel("Known JMS queues:"),
              new GridBagConstraints(0, 0, 2, 1, 0.0, 0.0,
                      GridBagConstraints.CENTER,
                      GridBagConstraints.NONE,
                      new Insets(5, 5, 5, 5), 0, 0));

        JScrollPane sp = new JScrollPane(getEndpointTable(),
                                         JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                                         JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sp.setPreferredSize(new Dimension(400, 200));
        p.add(sp,
              new GridBagConstraints(0, 1, 1, 1, 10.0, 10.0,
                                     GridBagConstraints.CENTER,
                                     GridBagConstraints.BOTH,
                                     new Insets(5, 5, 5, 5), 0, 0));

        p.add(getSideButtonPanel(),
              new GridBagConstraints(1, 1, 1, GridBagConstraints.REMAINDER, 0.0, 1.0,
                                     GridBagConstraints.NORTH,
                                     GridBagConstraints.VERTICAL,
                                     new Insets(5, 5, 5, 5), 0, 0));

        pack();
        enableOrDisableButtons();

    }

    private static class EndpointListItem {
        JmsConnection connection;
        JmsEndpoint endpoint;
        EndpointListItem(JmsConnection connection, JmsEndpoint endpoint) {
            this.connection = connection;
            this.endpoint = endpoint;
        };
    }

    private class EndpointTableModel extends AbstractTableModel {
        private ArrayList endpointListItems = loadEndpointListItems();

        public int getColumnCount() {
            return 3;
        }

        public int getRowCount() {
            return getEndpointListItems().size();
        }

        public String getColumnName(int column) {
            switch (column) {
                case 0:
                    return "Connection";
                case 1:
                    return "Endpoint";
                case MESSAGE_SOURCE_COL:
                    return "Direction";
            }
            return "?";
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            EndpointListItem i = (EndpointListItem) getEndpointListItems().get(rowIndex);
            switch (columnIndex) {
                case 0:
                    return i.connection.getName();
                case 1:
                    return i.endpoint == null ? "(no endpoints)" : i.endpoint.getName();
                case MESSAGE_SOURCE_COL:
                    if (i.endpoint == null) {
                        return "";
                    } else {
                        return i.endpoint.isMessageSource() ? "Inbound (Monitored)" : "Outbound from Gateway";
                    }
            }
            return "?";
        }

        public ArrayList getEndpointListItems() {
            return endpointListItems;
        }

        /** Get the current list of JmsEndpoints from the server. */
        private ArrayList loadEndpointListItems() {
            try {
                ArrayList endpointListItems = new ArrayList();

                JmsConnection[] connections = Registry.getDefault().getJmsManager().findAllConnections();
                for (int i = 0; i < connections.length; i++) {
                    JmsConnection connection = connections[i];
                    JmsEndpoint[] endpoints = Registry.getDefault().getJmsManager().getEndpointsForConnection(connection.getOid());
                    if (endpoints.length > 0)
                        for (int k = 0; k < endpoints.length; k++) {
                            JmsEndpoint endpoint = endpoints[k];
                            endpointListItems.add(new EndpointListItem(connection, endpoint));
                        }
                    else
                        endpointListItems.add(new EndpointListItem(connection, null));
                }
                return endpointListItems;
            } catch (Exception e) {
                throw new RuntimeException("Unable to look up list of known JMS endpoints", e);
            }
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
                                                       new Insets(0, 0, 0, 0), 0, 0));
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

            Utilities.equalizeButtonSizes(new JButton[] { getAddButton(),
                                                          getRemoveButton(),
                                                          getPropertiesButton(),
                                                          getCloseButton() });
        }
        return sideButtonPanel;
    }

    private JButton getRemoveButton() {
        if (removeButton == null) {
            removeButton = new JButton("Remove");
            removeButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    int row = getEndpointTable().getSelectedRow();
                    if (row >= 0) {
                        EndpointListItem i = (EndpointListItem) getEndpointTableModel().getEndpointListItems().get(row);
                        if (i != null) {
                            if (i.endpoint == null)
                                try {
                                    // Won't bother with confirmation screen for removing connection with no endpoints
                                    Registry.getDefault().getJmsManager().deleteConnection(i.connection.getOid());
                                } catch (Exception e1) {
                                    throw new RuntimeException("Unable to delete connection " + i.connection, e1);
                                }
                            else
                                try {
                                    Object[] options = { "Remove", "Cancel" };
                                    int result = JOptionPane.showOptionDialog(null,
                                                                              "<HTML>Are you sure you want to remove the " +
                                                                              "registration for the JMS Endpoint " +
                                                                              i.endpoint + "?<br>" +
                                                                              "<center>This action cannot be undone." +
                                                                              "</center></html>",
                                                                              "Remove Endpoint?",
                                                                              0, JOptionPane.WARNING_MESSAGE,
                                                                              null, options, options[1]);
                                    if (result == 0)
                                        Registry.getDefault().getJmsManager().deleteEndpoint(i.endpoint.getOid());
                                } catch (Exception e1) {
                                    throw new RuntimeException("Unable to delete endpoint " + i.endpoint, e1);
                                }

                            updateEndpointList(null);
                        }
                    }
                }
            });
        }
        return removeButton;
    }

    private JButton getAddButton() {
        if (addButton == null) {
            addButton = new JButton("Add...");
            addButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    AddEndpointWindow amew = new AddEndpointWindow();
                    Utilities.centerOnScreen(amew);
                    amew.show();
                }
            });
        }
        return addButton;
    }

    private class AddEndpointWindow extends JDialog {
        AddEndpointWindow() {
            super(JmsEndpointsWindow.this, "Add Endpoint - Select a JMS Connection", true);

            final JButton okButton = new JButton("Ok");
            final JButton cancelButton = new JButton("Cancel");
            final JmsConnectionListPanel jpl = new JmsConnectionListPanel(JmsEndpointsWindow.this);

            jpl.addJmsConnectionListSelectionListener(new JmsConnectionListPanel.JmsConnectionListSelectionListener() {
                public void onSelected(JmsConnection selected) {
                    okButton.setEnabled(selected != null);
                }
            });

            okButton.setEnabled(false);
            okButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    JmsConnection connection = jpl.getSelectedJmsConnection();
                    if (connection == null)
                        return;
                    try {
                        AddEndpointWindow.this.hide();
                        AddEndpointWindow.this.dispose();
                        NewJmsEndpointDialog njed = new NewJmsEndpointDialog(JmsEndpointsWindow.this, connection);
                        Utilities.centerOnScreen(njed);
                        njed.show();
                        JmsEndpoint endpoint = njed.getNewJmsEndpoint();
                        if (endpoint != null) {
                            updateEndpointList(endpoint);
                        }
                    } catch (Exception e1) {
                        throw new RuntimeException("Unable to use this JMS connection", e1);
                    }
                }
            });

            cancelButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    AddEndpointWindow.this.hide();
                    AddEndpointWindow.this.dispose();
                }
            });

            Container p = getContentPane();
            p.setLayout(new GridBagLayout());
            p.add(new JLabel("Which JMS Connection will providing the new Endpoint?"),
                  new GridBagConstraints(0, 0, 3, 1, 0.0, 0.0,
                                         GridBagConstraints.WEST,
                                         GridBagConstraints.NONE,
                                         new Insets(15, 15, 0, 15), 0, 0));

            p.add(jpl,
                  new GridBagConstraints(0, 1, 3, 1, 1.0, 1.0,
                                         GridBagConstraints.CENTER,
                                         GridBagConstraints.BOTH,
                                         new Insets(0, 0, 0, 0), 0, 0));

            p.add(Box.createGlue(),
                  new GridBagConstraints(0, 2, 1, 1, 100.0, 1.0,
                                         GridBagConstraints.EAST,
                                         GridBagConstraints.HORIZONTAL,
                                         new Insets(0, 0, 0, 0), 0, 0));

            p.add(okButton,
                  new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0,
                                         GridBagConstraints.EAST,
                                         GridBagConstraints.NONE,
                                         new Insets(0, 0, 11, 5), 0, 0));

            p.add(cancelButton,
                  new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0,
                                         GridBagConstraints.EAST,
                                         GridBagConstraints.NONE,
                                         new Insets(0, 0, 11, 11), 0, 0));

            Utilities.equalizeButtonSizes(new JButton[] { okButton, cancelButton });

            pack();

        }
    }

    private JButton getCloseButton() {
        if (closeButton == null) {
            closeButton = new JButton("Close");
            closeButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    JmsEndpointsWindow.this.hide();
                    JmsEndpointsWindow.this.dispose();
                }
            });
        }
        return closeButton;
    }

    private JButton getPropertiesButton() {
        if (propertiesButton == null) {
            propertiesButton = new JButton("Properties...");
            propertiesButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    int row = getEndpointTable().getSelectedRow();
                    if (row >= 0) {
                        EndpointListItem i = (EndpointListItem) getEndpointTableModel().getEndpointListItems().get(row);
                        if (i != null && i.endpoint != null) {
                            new SimpleJmsEndpointPropertiesDialog(i.endpoint).show();
                            getEndpointTableModel().fireTableRowsUpdated(row, row);
                        }
                    }
                }
            });
        }
        return propertiesButton;
    }

    private JTable getEndpointTable() {
        if (endpointTable == null) {
            endpointTable = new JTable(getEndpointTableModel());
            endpointTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            endpointTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent e) {
                    enableOrDisableButtons();
                }
            });
        }
        return endpointTable;
    }

    private void enableOrDisableButtons() {
        boolean propsEnabled = false;
        boolean removeEnabled = false;
        int row = getEndpointTable().getSelectedRow();
        if (row >= 0) {
            EndpointListItem i = (EndpointListItem) getEndpointTableModel().getEndpointListItems().get(row);
            if (i != null) {
                removeEnabled = true;
                if (i.endpoint != null)
                    propsEnabled = true;
            }
        }
        getRemoveButton().setEnabled(removeEnabled);
        getPropertiesButton().setEnabled(propsEnabled);
    }

    private EndpointTableModel getEndpointTableModel() {
        if (endpointTableModel == null) {
            endpointTableModel = createEndpointTableModel();
        }
        return endpointTableModel;
    }

    private EndpointTableModel createEndpointTableModel() {
        endpointTableModel = new EndpointTableModel();
        return endpointTableModel;
    }

    /**
     * Rebuild the endpoints table model, reloading the list from the server.  If an endpoint argument is
     * given, the row containing the specified endpoint will be selected in the new table.
     *
     * @param endpoint endpoint to select after the update, or null
     */
    private void updateEndpointList(JmsEndpoint endpoint) {
        getEndpointTable().setModel(createEndpointTableModel());
        if (endpoint != null) {
            ArrayList rows = getEndpointTableModel().getEndpointListItems();
            for (int i = 0; i < rows.size(); ++i) {
                EndpointListItem item = (EndpointListItem) rows.get(i);
                if (item.endpoint != null && item.endpoint.getOid() == endpoint.getOid()) {
                    getEndpointTable().getSelectionModel().setSelectionInterval(i, i);
                    break;
                }
            }
        }
    }

    private class SimpleJmsEndpointPropertiesDialog extends JDialog {
        private JmsEndpoint endpoint;

        SimpleJmsEndpointPropertiesDialog(JmsEndpoint endpoint) {
            super(JmsEndpointsWindow.this, "Endpoint Properties", true);
            this.endpoint = endpoint;

            JButton okButton = new JButton("Ok");
            JButton cancelButton = new JButton("Cancel");
            JRadioButton outboundButton = new JRadioButton("Outbound");
            final JRadioButton inboundButton = new JRadioButton("Inbound (Gateway will take messages from this Endpoint)");
            ButtonGroup buttonGroup = new ButtonGroup();
            buttonGroup.add(inboundButton);
            buttonGroup.add(outboundButton);
            inboundButton.setSelected(endpoint.isMessageSource());
            outboundButton.setSelected(!inboundButton.isSelected());

            cancelButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    SimpleJmsEndpointPropertiesDialog.this.hide();
                    SimpleJmsEndpointPropertiesDialog.this.dispose();
                }
            });
            okButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    saveMessageSourceValue(inboundButton.isSelected());
                    SimpleJmsEndpointPropertiesDialog.this.hide();
                    SimpleJmsEndpointPropertiesDialog.this.dispose();
                }
            });

            Container p = getContentPane();
            p.setLayout(new GridBagLayout());

            p.add(new JLabel("Name:"),
                  new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                                         GridBagConstraints.WEST,
                                         GridBagConstraints.NONE,
                                         new Insets(12, 12, 0, 0), 0, 0));
            p.add(new JLabel(endpoint.getName()),
                  new GridBagConstraints(1, 0, GridBagConstraints.REMAINDER, 1, 1.0, 0.0,
                                         GridBagConstraints.WEST,
                                         GridBagConstraints.HORIZONTAL,
                                         new Insets(12, 5, 0, 5), 0, 0));

            p.add(new JLabel("Direction:"),
                  new GridBagConstraints(0, 1, GridBagConstraints.REMAINDER, 1, 0.0, 0.0,
                                         GridBagConstraints.WEST,
                                         GridBagConstraints.NONE,
                                         new Insets(12, 12, 0, 0), 0, 0));
            p.add(outboundButton,
                  new GridBagConstraints(0, 2, GridBagConstraints.REMAINDER, 1, 1.0, 1.0,
                                         GridBagConstraints.WEST,
                                         GridBagConstraints.NONE,
                                         new Insets(0, 30, 0, 11), 0, 0));
            p.add(inboundButton,
                  new GridBagConstraints(0, 3, GridBagConstraints.REMAINDER, 1, 1.0, 1.0,
                                         GridBagConstraints.WEST,
                                         GridBagConstraints.NONE,
                                         new Insets(0, 30, 11, 11), 0, 0));
            p.add(okButton,
                  new GridBagConstraints(3, 4, 1, 1, 0.0, 0.0,
                                         GridBagConstraints.EAST,
                                         GridBagConstraints.NONE,
                                         new Insets(0, 0, 5, 5), 0, 0));
            p.add(cancelButton,
                  new GridBagConstraints(4, 4, 1, 1, 0.0, 0.0,
                                         GridBagConstraints.EAST,
                                         GridBagConstraints.NONE,
                                         new Insets(0, 0, 5, 11), 0, 0));
            p.add(Box.createGlue(),
                  new GridBagConstraints(1, 4, 1, 1, 1.0, 1.0,
                                         GridBagConstraints.EAST,
                                         GridBagConstraints.HORIZONTAL,
                                         new Insets(0, 0, 0, 0), 0, 0));

            Utilities.equalizeButtonSizes(new JButton[] { okButton, cancelButton });

            pack();
            Utilities.centerOnScreen(this);
        }

        public void saveMessageSourceValue(boolean isMessageSource) {
            try {
                Registry.getDefault().getJmsManager().setEndpointMessageSource(endpoint.getOid(), isMessageSource);
                endpoint.setMessageSource(isMessageSource);
            } catch (Exception e) {
                throw new RuntimeException("Unable to save changes to endpoint " + endpoint, e);
            }
        }
    }
}
