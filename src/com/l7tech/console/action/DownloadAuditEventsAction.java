/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.console.action;

import com.l7tech.common.audit.AuditAdmin;
import com.l7tech.common.gui.util.SwingWorker;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.util.OpaqueId;
import com.l7tech.console.MainWindow;
import com.l7tech.console.SsmApplication;
import com.l7tech.console.panels.CancelableOperationDialog;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.logging.Level;
import java.awt.*;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * Action that deletes the audit events older than 48 hours, after getting confirmation.
 */
public class DownloadAuditEventsAction extends SecureAction {
    public DownloadAuditEventsAction() {
        super(null);
    }

    public String getName() {
        return "Download All Audit Events";
    }

    public String getDescription() {
        return null;
    }

    protected String iconResource() {
        return null;
    }

    protected void performAction() {
        SsmApplication.doWithJFileChooser(new SsmApplication.FileChooserUser() {
            public void useFileChooser(JFileChooser fc) {
                doDownload(fc);
            }
        });
    }

    private void doDownload(final JFileChooser fc) {
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
        final Frame mainWindow = TopComponents.getInstance().getTopParent();
        int r = fc.showDialog(mainWindow, "Save Audit Events");
        if (r != JFileChooser.APPROVE_OPTION)
            return;
        File outFile = fc.getSelectedFile();
        if (outFile == null)
            return;

        // add "zip" extension if ZIP filter is selected, the selected file 
        // does not exist and the name does not end with ZIP (case insensitive)
        if(fc.getFileFilter()==fileFilter &&
           !outFile.exists() &&
           !outFile.getName().toLowerCase().endsWith(".zip")) {
            outFile = new File(outFile.getParentFile(), outFile.getName() + ".zip");
        }

        if (outFile.exists()) {
            Object[] options = { "Overwrite", "Cancel" };
            int result = JOptionPane.showOptionDialog(mainWindow,
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
            out = new FileOutputStream(outFile, false);
            final FileOutputStream fout = out;
            final JProgressBar progressBar = new JProgressBar(JProgressBar.HORIZONTAL);
            progressBar.setIndeterminate(true);
            final CancelableOperationDialog dlg = new CancelableOperationDialog(mainWindow,
                                                                                "Downloading Audits",
                                                                                "Please wait, downloading audits...",
                                                                                progressBar);
            SwingWorker worker = new SwingWorker() {
                public Object construct() {
                    try {
                        final AuditAdmin aa = Registry.getDefault().getAuditAdmin();
                        OpaqueId contextId = aa.downloadAllAudits(0); // default chunk size
                        AuditAdmin.DownloadChunk chunk;
                        while ((chunk = aa.downloadNextChunk(contextId)) != null) {
                            if (chunk.chunk != null && chunk.chunk.length > 0) {
                                fout.write(chunk.chunk);

                                logger.log(Level.FINEST, "Downloading chunk of audit records: " + chunk.auditsDownloaded + "/" + chunk.approxTotalAudits);
                                final int max = (int)chunk.approxTotalAudits;
                                final int min = (int)chunk.auditsDownloaded;
                                SwingUtilities.invokeLater(new Runnable() {
                                    public void run() {
                                        progressBar.setIndeterminate(false);
                                        progressBar.setMaximum(max);
                                        progressBar.setMinimum(0);
                                        progressBar.setValue(min);
                                    }
                                });
                            }
                        }

                        fout.close();
                        return "Success";
                    } catch (RemoteException e) {
                        return e;
                    } catch (IOException e) {
                        return e;
                    }
                }

                public void finished() {
                    dlg.setVisible(false);
                }
            };

            worker.start();
            dlg.setVisible(true);
            worker.interrupt();
            Object result = worker.get();
            if (result == null) {
                // canceled at user request -- clean up silently
                out.close();
                out = null;
                outFile.delete();
                return;
            }
            if (!(result instanceof String)) {
                // error -- report the problem then clean up
                String msg;
                if (result instanceof Throwable) {
                    msg = "Unable to read exported audit events from Gateway: " + ((Throwable)result).getMessage();
                    logger.log(Level.WARNING, msg, (Throwable)result);
                } else {
                    msg = "Unable to read exported audit events from Gateway: " + result;
                    logger.log(Level.WARNING, msg);
                }
                JOptionPane.showMessageDialog(null,
                                              msg,
                                              "Error",
                                              JOptionPane.ERROR_MESSAGE);
                out.close();
                out = null;
                outFile.delete();
                return;
            }


            out = null;
            JOptionPane.showMessageDialog(mainWindow, "Audit records saved successfully.", "Audit Export", JOptionPane.INFORMATION_MESSAGE);
        } catch (RemoteException e1) {
            final String msg = "Unable to read exported audit events from Gateway.";
            log.log(Level.SEVERE, msg, e1);
            throw new RuntimeException(msg, e1);
        } catch (IOException e) {
            final String msg = "Unable to save audit events to this file.";
            log.log(Level.SEVERE, msg, e);
            throw new RuntimeException(msg, e);
        } finally {
            if (out != null) try { out.close(); } catch (IOException e) { /* ignore; exception already logged */ }
        }
    }
}
