package com.l7tech.console.action;

import com.l7tech.console.util.WindowManager;
import com.l7tech.proxy.ClientProxy;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.io.*;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * The <code>ConsoleAction</code> shows the application console
 * with log.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class ImportCertificateAction extends BaseAction {
    static Logger log = Logger.getLogger(ImportCertificateAction.class.getName());

    /**
     * @return the action name
     */
    public String getName() {
        return "Import certificate";
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return "Import the SSG Certificate";
    }

    /**
     * subclasses override this method specifying the resource name
     */
    protected String iconResource() {
        return "com/l7tech/console/resources/ssl.gif";
    }

    /** Actually perform the action.
     * This is the method which should be called programmatically.

     * note on threading usage: do not access GUI components
     * without explicitly asking for the AWT event thread!
     */
    public void performAction() {
        JFileChooser fc = new JFileChooser();
        FileFilter filter = new FileFilter() {
            public boolean accept(File f) {
                if (f.isDirectory())
                    return true;
                String name = f.getName();
                int dot = name.lastIndexOf('.');
                if (dot < 0)
                    return false;
                String ext = name.substring(dot);
                return ext.equalsIgnoreCase(".cer");
            }

            public String getDescription() {
                return "Certificate files (*.cer)";
            }
        };
        fc.setFileFilter(filter);

        if (JFileChooser.APPROVE_OPTION == fc.showOpenDialog(getFrame())) {
            try {
                importSsgCertificate(fc.getSelectedFile());
            } catch (IOException e) {
                log.log(Level.WARNING, "Error importing certificate", e);
                JOptionPane.showMessageDialog(getFrame(),
                  "The system was unable to read the specified file.",
                  "Unable to read file",
                  JOptionPane.ERROR_MESSAGE);
            } catch (GeneralSecurityException e) {
                log.log(Level.WARNING, "Error importing certificate", e);
                JOptionPane.showMessageDialog(getFrame(),
                  "The system was unable to import the specified certificate: \n" +
                  e.getMessage(),
                  "Unable to import certificate",
                  JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private JFrame getFrame() {
        JFrame mw = WindowManager.getInstance().getMainWindow();
        return mw;
    }

    /**
     * Import an SSG certificate into our trust store.
     * @param selectedFile the SSG certificate file.  Must be in *.cer binary format, whatever that is.
     *                     Probably PKCS#7.
     */
    private void importSsgCertificate(File selectedFile)
      throws IOException, CertificateException, KeyStoreException, NoSuchAlgorithmException {
        KeyStore ks = KeyStore.getInstance("JKS");
        try {
            FileInputStream ksfis = new FileInputStream(ClientProxy.TRUST_STORE_FILE);
            ks.load(ksfis, ClientProxy.TRUST_STORE_PASSWORD.toCharArray());
            ksfis.close();
        } catch (FileNotFoundException e) {
            // Create a new one.
            ks.load(null, ClientProxy.TRUST_STORE_PASSWORD.toCharArray());
        }

        FileInputStream certfis = new FileInputStream(selectedFile);
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        Collection c = cf.generateCertificates(certfis);
        Iterator i = c.iterator();
        while (i.hasNext()) {
            Certificate cert = (Certificate)i.next();
            log.info("Adding certificate: " + cert);
            ks.setCertificateEntry("tomcat", cert);
        }

        FileOutputStream ksfos = null;
        try {
            ksfos = new FileOutputStream(ClientProxy.TRUST_STORE_FILE);
            ks.store(ksfos, ClientProxy.TRUST_STORE_PASSWORD.toCharArray());
        } finally {
            if (ksfos != null)
                ksfos.close();
        }

        JOptionPane.showMessageDialog(getFrame(),
          "Certificate import was successful. \n" +
          "You'll need to restart the Policy Editor for it to take effect.",
          "Certificate import successful", JOptionPane.INFORMATION_MESSAGE);
    }

}
