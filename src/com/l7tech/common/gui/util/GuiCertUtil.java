package com.l7tech.common.gui.util;

import java.security.cert.X509Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.Certificate;
import java.security.PrivateKey;
import java.security.KeyStore;
import java.security.Key;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.ByteArrayInputStream;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Window;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

import com.l7tech.common.util.CertUtils;
import com.l7tech.common.util.ResourceUtils;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.CausedIOException;
import com.l7tech.common.gui.ExceptionDialog;

/**
 * GUI certificate related utils.
 *
 * @author Steve Jones
 * @noinspection UnnecessaryContinue
 */
public class GuiCertUtil {

    //- PUBLIC

    /**
     * Import a certificate from a file (PEM, CER/ASN.1, PKS12).
     *
     * <p>Read an X509 Certificate from a file, optionally requiring a private
     * key.</p>
     *
     * @param parent The parent Frame or Dialog for the "load" dialog.
     * @param privateKeyRequired True if a private key is required.
     * @param callbackHandler handler for any TextOutputCallback and PasswordCallbacks (may be null)
     * @return The ImportedData structure which may be null or empty.
     */
    public static ImportedData importCertificate(Window parent, boolean privateKeyRequired, CallbackHandler callbackHandler){
        X509Certificate[] certificateChain = null;
        PrivateKey privateKey = null;

        if (parent !=null) {
            if (!(parent instanceof Dialog) &&
                !(parent instanceof Frame)) {
                throw new IllegalArgumentException("parent must be a Dialog or Frame.");
            }
        }

        final JFileChooser fc = Utilities.createJFileChooser();
        fc.setDialogTitle("Load certificate from ...");
        fc.setDialogType(JFileChooser.OPEN_DIALOG);
        FileFilter pemFilter = buildFilter(".pem", "(*.pem) PEM/BASE64 X.509 certificates.");
        FileFilter cerFilter = buildFilter(".cer", "(*.cer) DER encoded X.509 certificates.");
        FileFilter p12Filter = buildFilter(".p12", "(*.p12) PKCS 12 key store.");
        fc.addChoosableFileFilter(pemFilter);
        fc.addChoosableFileFilter(cerFilter);
        fc.addChoosableFileFilter(p12Filter);
        fc.setMultiSelectionEnabled(false);

        boolean done = false;
        while (!done) {
            int r = fc.showDialog(parent, "Load");

            if(r == JFileChooser.APPROVE_OPTION) {
                File file = fc.getSelectedFile();
                if(file!=null) {
                    //
                    // Can't check parent is writable due to JDK bug (see bug 2349 for info)
                    //
                    if ((!file.exists() && file.getParentFile()!=null /*&& file.getParentFile().canWrite()*/) ||
                        (file.isFile() && file.canRead())) {
                        String selectedFileName = file.getName();

                        FileFilter selectedFilter = fc.getFileFilter();
                        if (selectedFilter != pemFilter &&
                            selectedFilter != cerFilter &&
                            selectedFilter != p12Filter) { // detect from extension
                            if (selectedFileName.endsWith(".pem") ||
                                selectedFileName.endsWith(".txt")) {
                                selectedFilter = pemFilter;
                            }
                            else if (selectedFileName.endsWith(".cer") ||
                                     selectedFileName.endsWith(".der")) {
                                selectedFilter = cerFilter;
                            }
                            else if (selectedFileName.endsWith(".p12")) {
                                selectedFilter = p12Filter;
                            }
                        }


                        byte[] fileBytes = null;
                        InputStream in = null;
                        try {
                            in = new FileInputStream(file);
                            fileBytes = HexUtils.slurpStream(in, 1024*64);
                        }
                        catch(IOException ioe) {
                            displayError(parent, "Error reading file", "Could not read certificate.");
                            logger.log(Level.WARNING, "Error reading certificate data", ioe);
                            continue;
                        }
                        finally {
                            ResourceUtils.closeQuietly(in);
                        }

                        try {
                            if (selectedFilter == pemFilter) {
                                String fileData = new String(fileBytes, "UTF-8");
                                certificateChain = new X509Certificate[] { CertUtils.decodeFromPEM(fileData) };
                                if (privateKeyRequired || fileData.indexOf(" PRIVATE KEY-----")>0) {
                                    try {
                                        privateKey = CertUtils.decodeKeyFromPEM(fileData);
                                    }
                                    catch(IOException ioe) {
                                        if (privateKeyRequired) throw ioe;
                                    }
                                }
                            }
                            else if (selectedFilter == cerFilter) {
                                certificateChain = new X509Certificate[] { CertUtils.decodeCert(fileBytes) };
                                if (privateKeyRequired)
                                    throw new CausedIOException("Certificate files cannot contain private keys");
                            }
                            else if (selectedFilter == p12Filter) {
                                try {
                                    KeyStore keyStore = KeyStore.getInstance("PKCS12");

                                    //javax.crypto.BadPaddingException
                                    boolean loaded = false;
                                    char[] password = new char[]{' '}; // don't pass null initially or integrity check is skipped;
                                    while (!loaded) {
                                        try {
                                            keyStore.load(new ByteArrayInputStream(fileBytes), password);
                                            loaded = true;
                                        }
                                        catch(IOException ioe) {
                                            if (callbackHandler != null &&
                                                ExceptionUtils.causedBy(ioe, javax.crypto.BadPaddingException.class)) {
                                                PasswordCallback passwordCallback = new PasswordCallback("Keystore password", false);
                                                Callback[] callbacks = new Callback[]{passwordCallback};
                                                callbackHandler.handle(callbacks);
                                                password = passwordCallback.getPassword(); // if null IOException thrown on next load
                                            }
                                            else {
                                                throw ioe;
                                            }
                                        }
                                    }

                                    List aliases = Collections.list(keyStore.aliases());
                                    if (aliases.size() > 1 || aliases.isEmpty())
                                        throw new CausedIOException("PKCS12 has unsupported number of entries (must be 1)");

                                    String alias = (String) aliases.get(0);
                                    if (!keyStore.isKeyEntry(alias))
                                        throw new CausedIOException("PKCS12 entry '"+alias+"' is not a key.");

                                    Key key = null;
                                    while (key == null) {
                                        try {
                                            key = keyStore.getKey(alias, password);
                                            break;
                                        }
                                        catch(UnrecoverableKeyException uke) {
                                            if (callbackHandler != null && password != null) {
                                                PasswordCallback passwordCallback = new PasswordCallback("Key password", false);
                                                Callback[] callbacks = new Callback[]{passwordCallback};
                                                callbackHandler.handle(callbacks);
                                                password = passwordCallback.getPassword();
                                                passwordCallback.clearPassword();
                                            }
                                            else {
                                                throw new CausedIOException("Invalid password for key entry.");
                                            }
                                        }
                                    }
                                    if (null == key)
                                        throw new IOException("PKCS12 entry '"+alias+"' does not contain a key.");
                                    if (!(key instanceof PrivateKey))
                                        throw new IOException("PKCS12 entry '"+alias+"' key is not a private key.");

                                    Certificate[] chain = keyStore.getCertificateChain(alias);
                                    if (chain==null || chain.length==0)
                                        throw new CausedIOException("PKCS12 entry '"+alias+"' missing does not contain a certificate chain.");

                                    List<X509Certificate> got = new ArrayList<X509Certificate>();
                                    for (Certificate cert : chain) {
                                        if (cert == null)
                                            throw new IOException("PKCS12 entry '" + alias + "' contains a null certificate in its certificate chain.");
                                        if (!(cert instanceof X509Certificate))
                                            throw new IOException("PKCS12 entry '" + alias + "' certificate chain contains a non-X.509 certificate.");
                                        got.add((X509Certificate)cert);
                                    }

                                    certificateChain = got.toArray(new X509Certificate[0]);
                                    privateKey = (PrivateKey) key;
                                }
                                catch (KeyStoreException ke) {
                                    throw new CausedIOException("Keystore error", ke);
                                }
                                catch (NoSuchAlgorithmException nsae) { // from keystore load
                                    throw new CausedIOException("Keystore error", nsae);
                                }
                                catch (UnsupportedCallbackException uce) { // from callback handler
                                    throw new CausedIOException("Password error", uce);
                                }
                            }

                            done = true;
                        }
                        catch(IOException ioe) {
                            if (callbackHandler!=null &&
                                    ioe instanceof CausedIOException &&
                                    "Invalid password for key entry.".equals(ioe.getMessage())) {
                                logger.log(Level.INFO, "No password supplied, open cancelled.");
                                done = true;
                                continue;
                            }
                            else {
                                displayError(parent, "Error reading file", "Could not read certificate.");
                                logger.log(Level.WARNING, "Error reading certificate data", ioe);
                                continue;
                            }
                        }
                        catch(CertificateException ce) {
                            displayError(parent, "Error decoding file", "Could not decode certificate.");
                            logger.log(Level.WARNING, "Error reading certificate data", ce);
                            continue;
                        }
                    }
                    else {
                        logger.log(Level.WARNING, "Cannot read selected certificate file");
                        displayError(parent, SAVE_DIALOG_ERROR_TITLE,
                                "Cannot read selected file.\n "+file.getAbsolutePath());
                    }
                }
            }
            else {
                done = true;
            }
        }

        // create result data
        ImportedData id = null;
        if (certificateChain != null && certificateChain.length > 0) {
            final X509Certificate[] idCertificateChain = certificateChain;
            final PrivateKey idPrivateKey = privateKey;
            id = new ImportedData() {
                public X509Certificate getCertificate() {
                    return getCertificateChain()[0];
                }

                public X509Certificate[] getCertificateChain() {
                    return idCertificateChain;
                }

                public PrivateKey getPrivateKey() {
                    return idPrivateKey;
                }
            };
        }

        return id;
    }


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
                                CertUtils.encodeAsPEM(certificate).getBytes("UTF-8") :
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

    /** Represents the result of importing data from a keystore file. */
    public static interface ImportedData {
        /** @return the first entry in the certificate chain.  Never null. */
        X509Certificate getCertificate();

        /** @return The certificate chain.  Never null and always contains at least one entry. */
        X509Certificate[] getCertificateChain();

        /** @return The private key for this certificate chain.  May be null if a private key was not required. */
        PrivateKey getPrivateKey();
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(GuiCertUtil.class.getName());

    private static final String SAVE_DIALOG_ERROR_TITLE = "Error saving certificate";

    private static FileFilter buildFilter(final String extension, final String description) {
        return new FileFilter() {
            public boolean accept(File f) {
                return  f.isDirectory() || f.getName().toLowerCase().endsWith(extension);
            }
            public String getDescription() {
                return description;
            }
        };
    }

    private static void displayError(Window parent, String title, String message) {
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
        DialogDisplayer.display(ed);
    }
}
