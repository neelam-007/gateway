/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.console.panels;

import javax.swing.*;
import java.awt.*;

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

    public MonitoredEndpointsWindow(Frame owner) {
        super(owner, "Monitored JMS Endpoints", true);
        Container p = getContentPane();
        p.setLayout(new GridBagLayout());

        p.add(new JLabel("The following JMS endpoints are being monitored for incoming SOAP messages:"),
              new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
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

        p.add(getBottomButtonPanel(),
              new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
                      GridBagConstraints.EAST,
                      GridBagConstraints.NONE,
                      new Insets(5, 5, 5, 5), 0, 0));

        pack();

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
        }
        return bottomButtonPanel;
    }

    private JButton getCancelButton() {
        if (cancelButton == null) {
            cancelButton = new JButton("Cancel");
        }
        return cancelButton;
    }

    private JButton getOkButton() {
        if (okButton == null) {
            okButton = new JButton("Ok");
        }
        return okButton;
    }

    private JList getMonitoredEndpointList() {
        if (monitoredEndpointList == null) {
            monitoredEndpointList = new JList();
        }
        return monitoredEndpointList;
    }
}
