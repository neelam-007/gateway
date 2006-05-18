package com.l7tech.common.gui.util;

import java.security.cert.X509Certificate;
import java.security.cert.CertificateEncodingException;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.util.logging.Logger;
import java.util.logging.Level;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;

import com.l7tech.common.util.CertUtils;
import com.l7tech.common.util.ResourceUtils;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.gui.ExceptionDialog;

/**
 * GUI certificate related utils.
 *
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
public class CertUtil {

    //- PUBLIC

    /**
     * Export a certificate to file.
     *
     * <p>This will present the user with a file save dialog and allows the
     * certificate to be saved as a (text) pem or (binary) cer.</p>
     *
     * @param parent The parent Frame or Dialog for the "save as" dialog.
     * @param certificate The X509 certificate to save
     */
    public static void exportCertificate(Window parent, X509Certificate certificate) {
        if (parent !=null) {
            if (!(parent instanceof Dialog) &&
                !(parent instanceof Frame)) {
                throw new IllegalArgumentException("parent must be a Dialog or Frame.");
            }
        }

        final JFileChooser fc = Utilities.createJFileChooser();
        fc.setDialogTitle("Save certificate as ...");
        fc.setDialogType(JFileChooser.SAVE_DIALOG);
        FileFilter pemFilter = buildFilter(".pem", "(*.pem) PEM/BASE64 X.509 certificates.");
        FileFilter cerFilter = buildFilter(".cer", "(*.cer) DER encoded X.509 certificates.");
        fc.addChoosableFileFilter(pemFilter);
        fc.addChoosableFileFilter(cerFilter);
        fc.setMultiSelectionEnabled(false);

        int r = fc.showDialog(parent, "Save");
        if(r == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            if(file!=null) {
                //
                // Can't check parent is writable due to JDK bug (see bug 2349 for info)
                //
                if ((!file.exists() && file.getParentFile()!=null /*&& file.getParentFile().canWrite()*/) ||
                    (file.isFile() && file.canWrite())) {

                    // add extension if not provided and pem or cer is selected.
                    boolean isPem = true;
                    if (file.getName().indexOf('.') < 0) {
                        if(fc.getFileFilter()==pemFilter) file = new File(file.getParent(), file.getName() + ".pem");
                        if(fc.getFileFilter()==cerFilter) {
                            isPem = false;
                            file = new File(file.getParent(), file.getName() + ".cer");
                        }
                    }
                    else if (file.getName().endsWith(".cer")) {
                        isPem = false;                        
                    }

                    byte[] data = null;

                    try {
                        data = isPem ?
                                CertUtils.encodeAsPEM(certificate) :
                                certificate.getEncoded();
                    }
                    catch(IOException e) {
                        logger.log(Level.WARNING, "Error getting certificate data", e);
                        displayError(parent, SAVE_DIALOG_ERROR_TITLE, "Certificate is invalid.");
                    }
                    catch(CertificateEncodingException cee) {
                        logger.log(Level.WARNING, "Error getting certificate data", cee);
                        displayError(parent, SAVE_DIALOG_ERROR_TITLE, "Certificate is invalid.");
                    }

                    if (data != null) {
                        OutputStream out = null;
                        try {
                            out = new FileOutputStream(file);
                            out.write(data);
                            out.flush();
                        }
                        catch(IOException ioe) {
                            logger.log(Level.WARNING, "Error writing certificate file", ioe);
                            displayError(parent, SAVE_DIALOG_ERROR_TITLE,
                                    "Error writing certificate file.\n" + ExceptionUtils.getMessage(ioe));
                        }
                        finally {
                            ResourceUtils.closeQuietly(out);
                        }
                    }
                }
                else {
                    logger.log(Level.WARNING, "Cannot write to selected certificate file");
                    displayError(parent, SAVE_DIALOG_ERROR_TITLE,
                            "Cannot write to selected file.\n "+file.getAbsolutePath());
                }
            }
        }
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(CertUtil.class.getName());

    private static final String SAVE_DIALOG_ERROR_TITLE = "Error saving certificate";

    /**
     *
     */
    private static FileFilter buildFilter(final String extension, final String description) {
        FileFilter fileFilter = new FileFilter() {
            public boolean accept(File f) {
                return  f.isDirectory() || f.getName().toLowerCase().endsWith(extension);
            }
            public String getDescription() {
                return description;
            }
        };
        return fileFilter;
    }

    /**
     *
     */
    private static final void displayError(Window parent, String title, String message) {
        ExceptionDialog ed;
        if (parent instanceof Dialog) {
            ed =ExceptionDialog.createExceptionDialog((Dialog) parent, title, null, message, null, Level.WARNING);
        }
        else {
            ed = ExceptionDialog.createExceptionDialog((Frame) parent, title, null, message, null, Level.WARNING);
        }
        ed.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        ed.pack();
        Utilities.centerOnScreen(ed);
        ed.setVisible(true);
    }
}
