/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.console.panels;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.console.util.Registry;
import com.l7tech.common.transport.jms.JmsConnection;
import com.l7tech.common.transport.jms.JmsEndpoint;
import com.l7tech.common.gui.util.Utilities;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * A panel that lists the known JMS endpoints in a given connection, and allows selection of one of them
 * or creation of a new one.
 */
public class JmsEndpointListPanel extends JPanel {
    private JmsConnection jmsConnection;
    private Window owner;
    private JButton addButton;
    private JList endpointList;
    private AbstractListModel endpointListModel;

    public JmsEndpointListPanel(Frame owner, JmsConnection jmsConnection) {
        this.owner = owner;
        this.jmsConnection = jmsConnection;
        init();
    }

    public JmsEndpointListPanel(Window owner, JmsConnection jmsConnection) {
        this.owner = owner;
        this.jmsConnection = jmsConnection;
        init();
    }

    private void init() {
        setLayout(new GridBagLayout());

        JScrollPane sp = new JScrollPane(getEndpointList(),
                                         JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                                         JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sp.setMinimumSize(new Dimension(220, 40));
        add(sp,
            new GridBagConstraints(0, 0, 1, 8, 1.0, 1.0,
                                   GridBagConstraints.CENTER,
                                   GridBagConstraints.BOTH,
                                   new Insets(12, 12, 5, 11), 0, 0));
        add(getAddButton(),
            new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                                   GridBagConstraints.EAST,
                                   GridBagConstraints.NONE,
                                   new Insets(12, 0, 5, 11), 0, 0));
    }

    private JButton getAddButton() {
        if (addButton == null) {
            addButton = new JButton("Add...");
            addButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    NewJmsEndpointDialog d = owner instanceof Frame
                                                  ? new NewJmsEndpointDialog((Frame)owner, jmsConnection)
                                                  : new NewJmsEndpointDialog((Dialog)owner, jmsConnection);
                    Utilities.centerOnScreen(d);
                    d.setModal(true);
                    d.show();
                    JmsEndpoint e = d.getNewJmsEndpoint();
                    d.dispose();

                    if (e != null) {
                        ListModel clm = createNewEndpointListModel();
                        getEndpointList().setModel(clm);
                        for (int i = 0; i < clm.getSize(); ++i) {
                            JmsEndpoint endpoint = (JmsEndpoint) clm.getElementAt(i);
                            if (endpoint.getOid() == e.getOid()) {
                                getEndpointList().setSelectedIndex(i);
                            }
                        }
                    }
                }
            });
        }
        return addButton;
    }

    private JList getEndpointList() {
        if (endpointList == null) {
            endpointList = new JList(getEndpointListModel());
        }
        return endpointList;
    }

    private ListModel getEndpointListModel() {
        if (endpointListModel == null) {
            createNewEndpointListModel();
        }
        return endpointListModel;
    }

    private ListModel createNewEndpointListModel() {
        final JmsEndpoint[] endpoints = (JmsEndpoint[]) jmsConnection.getEndpoints().toArray(new JmsEndpoint[0]);
        endpointListModel = new AbstractListModel() {
            public int getSize() { return endpoints.length; }
            public Object getElementAt(final int index) throws IndexOutOfBoundsException { return endpoints[index]; }
        };
        return endpointListModel;
    }

    public JmsEndpoint getSelectedJmsEndpoint() {
        return (JmsEndpoint) getEndpointList().getSelectedValue();
    }
}
