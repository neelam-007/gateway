/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.console.panels;

import com.l7tech.console.event.ConnectionListener;
import com.l7tech.console.event.ConnectionEvent;

import javax.swing.*;
import java.awt.*;

/**
 * Simple modeless dialog that allows management of the list of JMS endpoints which the Gateway will monitor
 * for incoming messages.
 *
 * @author mike
 */
public class MonitoredEndpointsWindow extends JDialog implements ConnectionListener {
    public MonitoredEndpointsWindow(Frame owner) {
        super(owner, "Monitored JMS Endpoints", false);
    }

    public void onConnect(ConnectionEvent e) {

    }

    public void onDisconnect(ConnectionEvent e) {

    }
}
