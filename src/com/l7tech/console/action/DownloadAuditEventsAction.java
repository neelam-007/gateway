/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.console.action;

import com.l7tech.common.audit.AuditAdmin;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.logging.Level;

/**
 * Action that deletes the audit events older than 48 hours, after getting confirmation.
 */
public class DownloadAuditEventsAction extends SecureAction {
    public String getName() {
        return "Download all audit events";
    }

    public String getDescription() {
        return null;
    }

    protected String iconResource() {
        return null;
    }

    protected void performAction() {

        // File requestor
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Select file to save");
        fc.setDialogType(JFileChooser.SAVE_DIALOG);
        FileFilter fileFilter = new FileFilter() {
            public boolean accept(File f) {
                return (f.isDirectory() || f.getName().endsWith(".zip") || f.getName().endsWith(".ZIP") || f.getName().endsWith("Zip"));
            }

            public String getDescription() {
                return "(*.zip) ZIP archive";
            }
        };
        fc.setFileFilter(fileFilter);
        fc.setMultiSelectionEnabled(false);
        int r = fc.showDialog(TopComponents.getInstance().getMainWindow(), "Save Audit Events");
        if (r != JFileChooser.APPROVE_OPTION)
            return;
        File outFile = fc.getSelectedFile();
        if (outFile == null)
            return;

        if (outFile.exists()) {
            Object[] options = { "Overwrite", "Cancel" };
            int result = JOptionPane.showOptionDialog(TopComponents.getInstance().getMainWindow(),
                                                      "File: \n" +
                                                      "  " + outFile.toString() + "\n" +
                                                      "already exists.  Overwrite it?",
                                                      "Overwrite File?",
                                                      0, JOptionPane.WARNING_MESSAGE,
                                                      null, options, options[1]);
            if (result != 0)
                return;
        }

        // Download the audit events
        FileOutputStream out = null;
        try {
            // TODO create the file and save it
            out = new FileOutputStream(outFile, false);

            AuditAdmin.RemoteBulkStream bs = Registry.getDefault().getAuditAdmin().downloadAllAudits();
            byte[] chunk;
            while ((chunk = bs.nextChunk()) != null) {
                out.write(chunk);
            }

            out.close();
            out = null;

            JOptionPane.showMessageDialog(TopComponents.getInstance().getMainWindow(), "Audit records saved successfully.", "Audit Export", JOptionPane.INFORMATION_MESSAGE);
        } catch (RemoteException e1) {
            final String msg = "Unable to read exported audit events from Gateway.";
            log.log(Level.SEVERE, msg, e1);
            throw new RuntimeException(msg, e1);
        } catch (IOException e) {
            final String msg = "Unable to save audit events to this file.";
            log.log(Level.SEVERE, msg, e);
            throw new RuntimeException(msg, e);
        } finally {
            if (out != null) try { out.close(); } catch (IOException e) { log.log(Level.WARNING, "Unable to close output file", e); }
        }
    }
}
