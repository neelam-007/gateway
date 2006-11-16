package com.l7tech.console.action;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.util.CertUtils;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.SsmApplication;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.awt.*;


/**
 * The <code>ConsoleAction</code> shows the application console
 * with log.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class ImportCertificateAction extends SecureAction {
    static Logger log = Logger.getLogger(ImportCertificateAction.class.getName());

    protected ImportCertificateAction() {
        super(null, LIC_AUTH_ASSERTIONS);
    }

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
        return "Import the Gateway Certificate";
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
    protected void performAction() {
        SsmApplication.doWithJFileChooser(new SsmApplication.FileChooserUser() {
            public void useFileChooser(JFileChooser fc) {
                doImport(fc);
            }
        });
    }

    private void doImport(final JFileChooser fc) {
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

    private Frame getFrame() {
        return TopComponents.getInstance().getTopParent();
    }

    /**
     * Import a certificate into our trust store using the cert subject DN as
     * the cert alias.
     */
    public static void importSsgCertificate(X509Certificate cert)
      throws NoSuchAlgorithmException, IOException, CertificateException, KeyStoreException {
        importSsgCertificate(cert, cert.getSubjectDN().getName());
    }

    /**
     * Import a certificate into our trust store.
     */
    public static void importSsgCertificate(X509Certificate cert, String hostName)
            throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException
    {
        TopComponents.getInstance().getPreferences().importSsgCert(cert, hostName);
    }

    /**
     * Import an SSG certificate into our trust store.
     * @param selectedFile the SSG certificate file.  Must be in *.cer binary format, whatever that is.
     *                     Probably PKCS#7.
     */
    private void importSsgCertificate(File selectedFile)
      throws IOException, CertificateException, KeyStoreException, NoSuchAlgorithmException {

        FileInputStream certfis = new FileInputStream(selectedFile);
        CertificateFactory cf = CertUtils.getFactory();
        Collection c = cf.generateCertificates(certfis);
        Iterator i = c.iterator();
        if (i.hasNext()) {
            Certificate cert = (Certificate)i.next();
            if (cert instanceof X509Certificate) {
                X509Certificate x509Certificate = (X509Certificate)cert;
                importSsgCertificate(x509Certificate, "ssg");
            } else {
                throw new CertificateException("The certificate is not of expected type. X509 expected, received "+cert.getClass().getName());
            }
        }

        JOptionPane.showMessageDialog(getFrame(),
          "Certificate import was successful. \n" +
          "You'll need to restart the Policy Editor for it to take effect.",
          "Certificate import successful", JOptionPane.INFORMATION_MESSAGE);
    }

}
