/*
 * Copyright (C) 2004-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.action;

import com.l7tech.console.panels.DownloadAuditEventsWindow;

import java.awt.*;

/**
 * Action to bring up the audit event download window.
 */
public class DownloadAuditEventsAction extends SecureAction {

    public DownloadAuditEventsAction() {
        super(null);
    }

    public String getName() {
        return "Download Audit Events";
    }

    protected void performAction() {
        final DownloadAuditEventsWindow window = DownloadAuditEventsWindow.getInstance();
        window.setState(Frame.NORMAL);  // Might have been iconized.
        window.toFront();               // Might have been hidden behind other windows.
    }
}
