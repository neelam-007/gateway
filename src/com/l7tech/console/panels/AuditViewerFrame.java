/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.console.panels;

import javax.swing.*;
import java.awt.*;

/**
 * @author mike
 */
public class AuditViewerFrame extends JFrame {
    private JLabel messageNumberLabel;
    private JLabel serviceLabel;
    private JLabel userLabel;
    private JLabel dateLabel;
    private JLabel statusLabel;
    private JLabel requestLabel;
    private JLabel responseLabel;
    private JLabel clientIpLabel;
    private JButton viewResponseButton;
    private JButton viewRequestButton;
    private JSplitPane mainComponent;

    public AuditViewerFrame() throws HeadlessException {
        super("View Audit Records");
        initialize();
    }

    private void initialize() {
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(mainComponent, BorderLayout.CENTER);
        pack();
    }
}
