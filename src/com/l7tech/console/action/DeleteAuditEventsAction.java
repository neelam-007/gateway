/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.console.action;

import com.l7tech.console.util.Registry;

import javax.swing.*;
import java.rmi.RemoteException;

/**
 * Action that deletes the audit events older than 48 hours, after getting confirmation.
 */
public class DeleteAuditEventsAction extends SecureAction {
    public String getName() {
        return "Delete old audit events";
    }

    public String getDescription() {
        return null;
    }

    protected String iconResource() {
        return null;
    }

    protected void performAction() {
        Object[] options  = new Object[] {"Delete Events", "Cancel"};
        String title  ="Delete Audit Events";
        String message = "You are about to cause this Gateway to delete\n" +
                "all non-SEVERE audit events more than 48 hours old.\n" +
                "This action will result in a permanent audit entry,\n" +
                "and cannot be undone.\n\nDo you wish to proceed?\n";
        int res2 = JOptionPane.showOptionDialog(null, message, title,
                                                0, JOptionPane.WARNING_MESSAGE,
                                                null, options, options[1]);
        if (res2 != 0)
            return;

        // Delete the audit events
        try {
            Registry.getDefault().getAuditAdmin().deleteOldAuditRecords();
        } catch (RemoteException e1) {
            throw new RuntimeException("Unable to delete old audit events.", e1);
        }
    }
}
