/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.console.panels;

import com.l7tech.console.util.Registry;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.transport.jms.JmsConnection;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * A panel that lists the known JMS connections, and allows the user to select one or create a new one.
 *
 */
public class JmsConnectionListPanel extends JPanel {
    private JList connectionList;
    private JButton addButton;
    private ListModel connectionListModel;
    private Window owner;

    private static class ConnectionListItem {
        private EntityHeader entityHeader;

        ConnectionListItem(EntityHeader entityHeader) {
            this.entityHeader = entityHeader;
        }

        EntityHeader getEntityHeader() {
            return entityHeader;
        }

        public String toString() {
            return entityHeader.getName();
        }
    }

    public JmsConnectionListPanel(Frame owner) {
        this.owner = owner;
        init();
    }

    public JmsConnectionListPanel(Window owner) {
        this.owner = owner;
        init();
    }

    /** @return the currently-selected JmsConnection, or null if there isn't one. */
    public EntityHeader getSelectedJmsConnection() {
        ConnectionListItem cli = (ConnectionListItem) getConnectionList().getSelectedValue();
        return cli == null ? null : cli.getEntityHeader();
    }

    private void init() {
        setLayout(new GridBagLayout());

        JScrollPane sp = new JScrollPane(getConnectionList(),
                                         JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                                         JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sp.setMinimumSize(new Dimension(220, 40));
        add(sp,
            new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0,
                                   GridBagConstraints.CENTER,
                                   GridBagConstraints.BOTH,
                                   new Insets(12, 12, 5, 11), 0, 0));
        add(getAddButton(),
            new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                                   GridBagConstraints.EAST,
                                   GridBagConstraints.NONE,
                                   new Insets(0, 12, 11, 11), 0, 0));
    }

    private JList getConnectionList() {
        if (connectionList != null)
            return connectionList;

        connectionList = new JList(getConnectionListModel());
        return connectionList;
    }

    /** Create a new connection list model.  Will fetch the up-to-date list from the Gateway each time it is called. */
    private ListModel createNewConnectionListModel() {
        connectionListModel = new AbstractListModel() {
            private EntityHeader[] connections = null;
            private ConnectionListItem[] items = null;

            private EntityHeader[] getConnections() {
                if (connections == null)
                    try {
                        connections = Registry.getDefault().getJmsManager().findAllConnections();
                        items = new ConnectionListItem[connections.length];
                    } catch (Exception e) {
                        throw new RuntimeException("Unable to obtain list of JMS Connections", e);
                    }
                return connections;
            }

            public int getSize() {
                return getConnections().length;
            }

            public Object getElementAt(final int index) throws IndexOutOfBoundsException {
                ConnectionListItem item = items[index];
                if (item == null)
                    item = items[index] = new ConnectionListItem(connections[index]);
                return item;
            }
        };

        return connectionListModel;
    }

    private ListModel getConnectionListModel() {
        if (connectionListModel == null) {
            createNewConnectionListModel();
        }
        return connectionListModel;
    }

    private JButton getAddButton() {
        if (addButton != null)
            return addButton;

        addButton = new JButton("New connection...");
        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                NewJmsConnectionDialog d = owner instanceof Frame
                                                ? new NewJmsConnectionDialog((Frame)owner)
                                                : new NewJmsConnectionDialog((Dialog)owner);
                Utilities.centerOnScreen(d);
                d.setModal(true);
                d.show();
                JmsConnection c = d.getNewJmsConnection();
                d.dispose();

                if (c != null) {
                    ListModel clm = createNewConnectionListModel();
                    getConnectionList().setModel(clm);
                    for (int i = 0; i < clm.getSize(); ++i) {
                        EntityHeader entityHeader = ((ConnectionListItem) clm.getElementAt(i)).getEntityHeader();
                        if (entityHeader.getOid() == c.getOid()) {
                            getConnectionList().setSelectedIndex(i);
                            break;
                        }
                    }
                }
            }
        });
        return addButton;
    }
}
