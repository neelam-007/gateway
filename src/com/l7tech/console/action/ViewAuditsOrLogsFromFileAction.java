package com.l7tech.console.action;

import java.io.IOException;
import java.io.File;
import java.util.logging.Level;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;

import com.l7tech.console.GatewayAuditWindow;
import com.l7tech.console.GatewayLogWindow;
import com.l7tech.console.SsmApplication;
import com.l7tech.console.util.TopComponents;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.gui.util.DialogDisplayer;
import com.l7tech.common.gui.util.FileChooserUtil;
import com.l7tech.common.gui.ExceptionDialog;

/**
 * Action to load saved audit or log records.
 *
 * @author $Author$
 * @version $Revision$
 */
public class ViewAuditsOrLogsFromFileAction extends BaseAction {

    public ViewAuditsOrLogsFromFileAction() {
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "Saved Events ...";
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return "View saved audit or log events";
    }

    /**
     * Open the given file containing log or audit data.
     *
     * @param file the file.
     * @return true if an attempt to open the file was made
     */
    public boolean openFile(File file) {
        boolean accepted = false;
        if(file!=null && file.isFile() && file.canRead()) {
            accepted = true;
            try {
                if(file.getName().endsWith(".ssga")) {
                    GatewayAuditWindow gaw = new GatewayAuditWindow(false);
                    gaw.pack();
                    Utilities.centerOnScreen(gaw);
                    gaw.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                    if(gaw.displayAudits(file)) {
                        gaw.setVisible(true);
                    }
                    else {
                        gaw.dispose();
                    }
                }
                else if(file.getName().endsWith(".ssgl")) {
                    GatewayLogWindow gal = new GatewayLogWindow();
                    gal.pack();
                    gal.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                    Utilities.centerOnScreen(gal);
                    if(gal.displayLogs(file)){
                        gal.setVisible(true);
                    }
                    else {
                        gal.dispose();
                    }
                }
            }
            catch(IOException ioe) {
                log.log(Level.WARNING, "Error reading file.", ioe);
                ExceptionDialog exceptionDialog = ExceptionDialog.createExceptionDialog(
                        TopComponents.getInstance().getTopParent(),
                        "SecureSpan Manager - Error",
                        "Error reading file: \n  " + file.getName(), 
                        null,
                        Level.WARNING);
                exceptionDialog.pack();
                Utilities.centerOnScreen(exceptionDialog);
                DialogDisplayer.display(exceptionDialog);
            }
        }
        return accepted;
    }

    /**
     * subclasses override this method specifying the resource name
     */
    protected String iconResource() {
        return "com/l7tech/console/resources/AnalyzeGatewayLog16x16.gif";
    }

    /** Actually perform the action.
     * This is the method which should be called programmatically.

     * note on threading usage: do not access GUI components
     * without explicitly asking for the AWT event thread!
     */
    protected void performAction() {
        SsmApplication.doWithJFileChooser(new FileChooserUtil.FileChooserUser() {
            public void useFileChooser(JFileChooser fc) {
                doView(fc);
            }
        });
    }

    private void doView(final JFileChooser fc) {
        fc.setDialogTitle("Analyze saved events ...");
        fc.setDialogType(JFileChooser.OPEN_DIALOG);
        FileFilter fileFilter = new FileFilter() {
            public boolean accept(File f) {
                return  f.isDirectory() ||
                        f.getName().toLowerCase().endsWith(".ssga") ||
                        f.getName().toLowerCase().endsWith(".ssgl");
            }
            public String getDescription() {
                return "(*.ssga/*.ssgl) SecureSpan Gateway event data files.";
            }
        };
        fc.addChoosableFileFilter(fileFilter);
        fc.setMultiSelectionEnabled(false);
        int r = fc.showDialog(TopComponents.getInstance().getTopParent(), "Open");
        if(r == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            if(file!=null) {
                if(file.canRead()) {
                    openFile(file);
                }
                else {
                    ExceptionDialog d = ExceptionDialog.createExceptionDialog(
                            TopComponents.getInstance().getTopParent(),
                            "SecureSpan Manager - Error",
                            "File not accessible:\n'"+file.getAbsolutePath()+"'.",
                            null,
                            Level.INFO);
                    d.pack();
                    Utilities.centerOnScreen(d);
                    d.setVisible(true);
                }
            }
        }

    }
}
