/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.console.panels;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.transport.jms.JmsConnection;
import com.l7tech.console.util.Registry;
import com.l7tech.objectmodel.EntityHeader;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Simple modal dialog that allows management of the list of JMS endpoints which the Gateway will monitor
 * for incoming messages.
 *
 * @author mike
 */
public class MonitoredEndpointsWindow extends JDialog {
    private JList monitoredEndpointList;
    private JPanel bottomButtonPanel;
    private JButton okButton;
    private JButton cancelButton;
    private ListModel monitoredEndpointListModel;

    private ArrayList monitoredEntityHeaders;
    private JPanel sideButtonPanel;
    private JButton addButton;
    private JButton removeButton;

    public MonitoredEndpointsWindow(Frame owner) {
        super(owner, "Monitored JMS Endpoints", true);
        Container p = getContentPane();
        p.setLayout(new GridBagLayout());

        p.add(new JLabel("The following JMS endpoints are being monitored for incoming SOAP messages:"),
              new GridBagConstraints(0, 0, 2, 1, 0.0, 0.0,
                      GridBagConstraints.CENTER,
                      GridBagConstraints.NONE,
                      new Insets(5, 5, 5, 5), 0, 0));

        JScrollPane sp = new JScrollPane(getMonitoredEndpointList(),
                                         JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                                         JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        p.add(sp,
              new GridBagConstraints(0, 1, 1, 1, 10.0, 10.0,
                                     GridBagConstraints.CENTER,
                                     GridBagConstraints.BOTH,
                                     new Insets(5, 5, 5, 5), 0, 0));

        p.add(getSideButtonPanel(),
              new GridBagConstraints(1, 1, 1, 1, 10.0, 10.0,
                                     GridBagConstraints.CENTER,
                                     GridBagConstraints.BOTH,
                                     new Insets(5, 5, 5, 5), 0, 0));

        p.add(getBottomButtonPanel(),
              new GridBagConstraints(0, 2, 2, 1, 0.0, 0.0,
                                     GridBagConstraints.EAST,
                                     GridBagConstraints.NONE,
                                     new Insets(5, 5, 5, 5), 0, 0));

        pack();

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
            sideButtonPanel.add(Box.createGlue(),
                                new GridBagConstraints(0, 2, 1, 1, 1.0, 1.0,
                                                       GridBagConstraints.CENTER,
                                                       GridBagConstraints.VERTICAL,
                                                       new Insets(0, 0, 0, 0), 0, 0));

            Utilities.equalizeButtonSizes(new JButton[] { getAddButton(), getRemoveButton() });
        }
        return sideButtonPanel;
    }

    private JButton getRemoveButton() {
        if (removeButton == null) {
            removeButton = new JButton("Remove");
            removeButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    EntityHeader h = (EntityHeader) getMonitoredEndpointList().getSelectedValue();
                    getMonitoredEntityHeaders().remove(h);
                    getMonitoredEndpointList().setModel(createMonitoredEndpointListModel());
                    getOkButton().setEnabled(true);
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
                    AddMonitoredEndpointConnectionWindow amew = new AddMonitoredEndpointConnectionWindow();
                    amew.show();
                }
            });
        }
        return addButton;
    }

    private JPanel getBottomButtonPanel() {
        if (bottomButtonPanel == null) {
            bottomButtonPanel = new JPanel();
            bottomButtonPanel.setLayout(new GridBagLayout());
            bottomButtonPanel.add(Box.createGlue(),
                                  new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0,
                                                         GridBagConstraints.CENTER,
                                                         GridBagConstraints.HORIZONTAL,
                                                         new Insets(0, 0, 0, 0), 0, 0));
            bottomButtonPanel.add(getOkButton(),
                                  new GridBagConstraints(1, 0, 1, 1, 1.0, 1.0,
                                                         GridBagConstraints.EAST,
                                                         GridBagConstraints.NONE,
                                                         new Insets(0, 0, 0, 5), 0, 0));
            bottomButtonPanel.add(getCancelButton(),
                                  new GridBagConstraints(2, 0, 1, 1, 1.0, 1.0,
                                                         GridBagConstraints.EAST,
                                                         GridBagConstraints.NONE,
                                                         new Insets(0, 0, 0, 0), 0, 0));
            Utilities.equalizeButtonSizes(new JButton[] { getOkButton(), getCancelButton() });
        }
        return bottomButtonPanel;
    }

    private JButton getCancelButton() {
        if (cancelButton == null) {
            cancelButton = new JButton("Cancel");
            cancelButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    MonitoredEndpointsWindow.this.hide();
                    MonitoredEndpointsWindow.this.dispose();
                }
            });
        }
        return cancelButton;
    }

    private class AddMonitoredEndpointWindow extends JDialog {
        AddMonitoredEndpointWindow(Dialog parent, JmsConnection connection) {
            super(parent, "Select JMS Endpoint", true);

            final JmsEndpointListPanel jpl = new JmsEndpointListPanel(parent, connection);
            final JButton okButton = new JButton("Ok");
            final JButton cancelButton = new JButton("Cancel");

            okButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    EntityHeader endpoint = jpl.getSelectedJmsEndpoint();
                    addMonitoredEndpoint(endpoint);
                    AddMonitoredEndpointWindow.this.hide();
                    AddMonitoredEndpointWindow.this.dispose();
                }
            });

            cancelButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    AddMonitoredEndpointWindow.this.hide();
                    AddMonitoredEndpointWindow.this.dispose();
                }
            });


            Container p = getContentPane();
            p.setLayout(new GridBagLayout());
            p.add(new JLabel("Please select the endpoint to monitor:"),
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

    private class AddMonitoredEndpointConnectionWindow extends JDialog {
        AddMonitoredEndpointConnectionWindow() {
            super(MonitoredEndpointsWindow.this, "Select JMS Connection", true);

            final JButton okButton = new JButton("Ok");
            final JButton cancelButton = new JButton("Cancel");
            final JmsConnectionListPanel jpl = new JmsConnectionListPanel(MonitoredEndpointsWindow.this);

            jpl.addJmsConnectionListSelectionListener(new JmsConnectionListPanel.JmsConnectionListSelectionListener() {
                public void onSelected(EntityHeader selected) {
                    okButton.setEnabled(selected != null);
                }
            });

            okButton.setEnabled(false);
            okButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    EntityHeader connectionHeader = jpl.getSelectedJmsConnection();
                    if (connectionHeader == null)
                        return;
                    try {
                        JmsConnection connection = Registry.getDefault().getJmsManager().findConnectionByPrimaryKey(connectionHeader.getOid());
                        AddMonitoredEndpointConnectionWindow.this.hide();
                        AddMonitoredEndpointConnectionWindow.this.dispose();
                        AddMonitoredEndpointWindow amew = new AddMonitoredEndpointWindow(MonitoredEndpointsWindow.this, connection);
                        amew.show();
                    } catch (Exception e1) {
                        throw new RuntimeException("Unable to use this JMS connection", e1);
                    }
                }
            });

            cancelButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    AddMonitoredEndpointConnectionWindow.this.hide();
                    AddMonitoredEndpointConnectionWindow.this.dispose();
                }
            });

            Container p = getContentPane();
            p.setLayout(new GridBagLayout());
            p.add(new JLabel("Please select the JMS connection:"),
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

    private JButton getOkButton() {
        if (okButton == null) {
            okButton = new JButton("Ok");
            okButton.setEnabled(false);
            okButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    long monitoredOids[] = new long[getMonitoredEntityHeaders().size()];
                    for (int i = 0; i < monitoredOids.length; i++) {
                        monitoredOids[i] = ((EntityHeader)getMonitoredEntityHeaders().get(i)).getOid();
                    }
                    try {
                        Registry.getDefault().getJmsManager().saveAllMonitoredEndpoints(monitoredOids);
                    } catch (Exception e) {
                        throw new RuntimeException("Unable to update list of monitored JMS endpoints", e);
                    }
                    MonitoredEndpointsWindow.this.hide();
                    MonitoredEndpointsWindow.this.dispose();
                }
            });
        }
        return okButton;
    }

    private JList getMonitoredEndpointList() {
        if (monitoredEndpointList == null) {
            monitoredEndpointList = new JList(getMonitoredEndpointListModel());
            monitoredEndpointList.setCellRenderer(new DefaultListCellRenderer() {
                public Component getListCellRendererComponent(JList list,
                                                              Object value,
                                                              int index,
                                                              boolean isSelected,
                                                              boolean cellHasFocus)
                {
                    String string = ((EntityHeader) value).getName();
                    return super.getListCellRendererComponent(list, string, index, isSelected, cellHasFocus);
                }
            });

        }
        return monitoredEndpointList;
    }

    private ListModel getMonitoredEndpointListModel() {
        if (monitoredEndpointListModel == null) {
            monitoredEndpointListModel = createMonitoredEndpointListModel();
        }
        return monitoredEndpointListModel;
    }

    private void addMonitoredEndpoint(EntityHeader h) {
        getMonitoredEntityHeaders().add(h);
        getMonitoredEndpointList().setModel(createMonitoredEndpointListModel());
        getMonitoredEndpointList().setSelectedValue(h, true);
        getOkButton().setEnabled(true);
    }

    /** Get the current list of monitored JmsEndpoint EntityHeaders from the server. */
    private ArrayList loadMonitoredEntityHeaders() {
        try {
            EntityHeader[] endpoints = Registry.getDefault().getJmsManager().findAllMonitoredEndpoints();
            monitoredEntityHeaders = new ArrayList(Arrays.asList(endpoints));
        } catch (Exception e) {
            throw new RuntimeException("Unable to look up list of monitored JMS endpoints", e);
        }
        return monitoredEntityHeaders;
    }

    private ArrayList getMonitoredEntityHeaders() {
        if (monitoredEntityHeaders == null) {
            monitoredEntityHeaders = loadMonitoredEntityHeaders();
        }
        return monitoredEntityHeaders;
    }

    private ListModel createMonitoredEndpointListModel() {
        monitoredEndpointListModel = new AbstractListModel() {
            public int getSize() {
                return getMonitoredEntityHeaders().size();
            }

            public Object getElementAt(int i) {
                return getMonitoredEntityHeaders().get(i);
            }
        };

        return monitoredEndpointListModel;
    }
}
