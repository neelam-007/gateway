/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.console.panels;

import com.l7tech.console.util.Registry;
import com.l7tech.objectmodel.EntityHeader;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * A panel that lists the known JMS connections, and allows the user to select one or create a new one.
 *
 */
public class JmsConnectionListPanel extends JPanel {
    private JComponent connectionList;
    private JButton addButton;
    private ListModel connectionListModel;

    public JmsConnectionListPanel() {
        setLayout(new GridBagLayout());
        add(getConnectionList(),
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

    private JComponent getConnectionList() {
        if (connectionList != null)
            return connectionList;

        JList list = new JList(getConnectionListModel());
        connectionList = new JScrollPane(list, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        connectionList.setMinimumSize(new Dimension(220, 40));
        return connectionList;
    }

    private ListModel getConnectionListModel() {
        if (connectionListModel != null)
            return connectionListModel;

        connectionListModel = new AbstractListModel() {
            private EntityHeader[] connections = null;

            private EntityHeader[] getConnections() {
                if (connections == null)
                    try {
                        return connections = Registry.getDefault().getJmsManager().findAllConnections();
                    } catch (Exception e) {
                        throw new RuntimeException("Unable to obtain list of JMS Connections", e);
                    }
                return connections;
            }

            public int getSize() {
                return getConnections().length;
            }

            public Object getElementAt(final int index) throws IndexOutOfBoundsException {
                return new Object() {
                    public String toString() {
                        return getConnections()[index].getName();
                    }
                };
            }
        };
        return connectionListModel;
    }

    private JButton getAddButton() {
        if (addButton != null)
            return addButton;

        addButton = new JButton("New JMS Connection...");
        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JDialog d = new NewJmsConnectionDialog();
                d.show();
            }
        });
        return addButton;
    }
}
