/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.console.panels;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.transport.jms.JmsAdmin;
import com.l7tech.common.transport.jms.JmsConnection;
import com.l7tech.common.transport.jms.JmsEndpoint;
import com.l7tech.console.util.Registry;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

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
                            JmsEndpoint endpoint = (JmsEndpoint)clm.getElementAt(i);
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
            endpointList.setCellRenderer(new DefaultListCellRenderer() {
                public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    String string = ((JmsEndpoint) value).getName();
                    return super.getListCellRendererComponent(list, string, index, isSelected, cellHasFocus);
                }
            });
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
        JmsAdmin jmsAdmin = Registry.getDefault().getJmsManager();
        try {
            final JmsEndpoint[] endpointHeaders = jmsAdmin.getEndpointsForConnection( jmsConnection.getOid() );
            endpointListModel = new AbstractListModel() {
                public int getSize() { return endpointHeaders.length; }
                public Object getElementAt(final int index) throws IndexOutOfBoundsException { return endpointHeaders[index]; }
            };
            return endpointListModel;
        } catch ( Exception e ) {
            throw new RuntimeException( "Unable to locate endpoints for connection " + jmsConnection, e );
        }
    }

    public JmsEndpoint getSelectedJmsEndpoint() {
        return (JmsEndpoint)getEndpointList().getSelectedValue();
    }

    public interface JmsEndpointListSelectionListener {
        /**
         * Called when the selection changes.
         * @param selected the newly selected JmsEndpoint, or null if none is selected.
         */
        void onSelected(JmsEndpoint selected);
    }

    /**
     * Subscribe to be notified whenever the selected JMS endpoint changes.  The provided listener's
     * onSelected() method will be called each time.  Note that "no selection" is a possibility.
     * @param listener listener which will be called back when the selection changes
     */
    public void addJmsEndpointListSelectionListener(final JmsEndpointListSelectionListener listener) {
        getEndpointList().getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                listener.onSelected(getSelectedJmsEndpoint());
            }
        });
    }   
}
