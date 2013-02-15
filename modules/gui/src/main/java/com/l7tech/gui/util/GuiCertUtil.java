package com.l7tech.gui.util;

import com.l7tech.common.io.ByteLimitInputStream;
import com.l7tech.common.io.CertUtils;
import com.l7tech.util.*;
import org.jetbrains.annotations.Nullable;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.io.*;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * GUI certificate related utils.
 *
 * @author Steve Jones
 * @noinspection UnnecessaryContinue
 */
public class GuiCertUtil {

    //- PUBLIC

    public static final FileFilter pemFilter = buildFilter(new String[] {".pem"}, "(*.pem) PEM/BASE64 X.509 certificates.");
    public static final FileFilter cerFilter = buildFilter(new String[] {".cer"}, "(*.cer) DER encoded X.509 certificates.");
    public static final FileFilter p12Filter = buildFilter(new String[] {".p12", ".pfx"}, "(*.p12, *.pfx) PKCS 12 key store.");
    public static final FileFilter jksFilter = buildFilter(new String[] {".jks", ".ks"}, "(*.jks, *.ks) Java JKS key store.");
    public static final FileFilter p7bFilter = buildFilter(new String[] {".p7b", ".p7c"}, "(*.p7b, *.p7c) PKCS#7 empty envelope");

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
     * @throws AccessControlException if this process is running with a restrictive security manager that
     *                                appears to prevent use of JFileChooser
     */
    public static ImportedData importCertificate( final Window parent,
                                                  final boolean privateKeyRequired,
                                                  final CallbackHandler callbackHandler ) throws AccessControlException {
        final ImportedData[] importedDataHolder = new ImportedData[1];
        doImport( parent, privateKeyRequired, callbackHandler, 1, new Functions.Unary<Boolean,ImportedData>(){
            @Override
            public Boolean call( final ImportedData importedData ) {
                importedDataHolder[0] = importedData;
                return true;
            }
        } );
        return importedDataHolder[0];
    }

    /**
     * Import one or more certificates from a file (PEM, CER/ASN.1, PKS12).
     *
     * <p>Read an X509 Certificate from a file, optionally requiring a private
     * key.</p>
     *
     * @param parent The parent Frame or Dialog for the "load" dialog.
     * @param privateKeyRequired True if a private key is required.
     * @param callbackHandler handler for any TextOutputCallback and PasswordCallbacks (may be null)
     * @param importCallback callback to perform import of each ImportedData item.
     * @throws AccessControlException if this process is running with a restrictive security manager that
     *                                appears to prevent use of JFileChooser
     */
    public static void importCertificates( final Window parent,
                                           final boolean privateKeyRequired,
                                           final CallbackHandler callbackHandler,
                                           final Functions.Unary<Boolean,ImportedData> importCallback ) throws AccessControlException {
        doImport( parent, privateKeyRequired, callbackHandler, 100000, importCallback );
    }

    /**
     * Create a JFileChooser for browsing for the appropriate file types for importing a certificate,
     * possibly including a private key.
     * @param privateKeyRequired  if true, will accept only PKCS#12 or JKS files.  If false, will additionally accept
     *                            .PEM, .CER and .P7B files.
     * @return a JFileChooser configured to browser for the appropriate file types.
     */
    public static JFileChooser createFileChooser(boolean privateKeyRequired) {
        final JFileChooser fc = FileChooserUtil.createJFileChooser();
        fc.setFileHidingEnabled(false);
        if (privateKeyRequired) {
            fc.setDialogTitle("Load Private Key");
            fc.addChoosableFileFilter(p12Filter);
            fc.addChoosableFileFilter(jksFilter);
        } else {
            fc.setDialogTitle("Load Certificate");
            fc.addChoosableFileFilter(pemFilter);
            fc.addChoosableFileFilter(cerFilter);
            fc.addChoosableFileFilter(p12Filter);
            fc.addChoosableFileFilter(jksFilter);
            fc.addChoosableFileFilter(p7bFilter);
        }

        fc.setDialogType(JFileChooser.OPEN_DIALOG);
        fc.setMultiSelectionEnabled(false);
        return fc;
    }

    /**
     * Export a certificate to file.
     *
     * <p>This will present the user with a file save dialog and allows the
     * certificate to be saved as a (text) pem or (binary) cer.</p>
     *
     * @param parent The parent Frame or Dialog for the "save as" dialog.
     * @param certificate The X509 certificate to save
     * @throws AccessControlException if this process is running with a restrictive security manager that
     *                                appears to prevent use of JFileChooser
     */
    public static void exportCertificate(Window parent, X509Certificate certificate) {
        final JFileChooser fc = FileChooserUtil.createJFileChooser();
        fc.setDialogTitle("Save certificate as ...");
        fc.setDialogType(JFileChooser.SAVE_DIALOG);
        FileFilter pemFilter = buildFilter(new String[] {".pem"}, "(*.pem) PEM/BASE64 X.509 certificates.");
        FileFilter cerFilter = buildFilter(new String[] {".cer"}, "(*.cer) DER encoded X.509 certificates.");
        fc.addChoosableFileFilter(pemFilter);
        fc.addChoosableFileFilter(cerFilter);
        fc.setMultiSelectionEnabled(false);

        int r = fc.showDialog(parent, "Save");
        if(r == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            if(file!=null) {

                // add extension if not provided and pem or cer is selected.
                boolean isPem = true;
                if (file.getName().indexOf('.') < 0) {
                    if (fc.getFileFilter() == pemFilter) file = new File(file.getParent(), file.getName() + ".pem");
                    if (fc.getFileFilter() == cerFilter) {
                        isPem = false;
                        file = new File(file.getParent(), file.getName() + ".cer");
                    }
                } else if (file.getName().endsWith(".cer")) {
                    isPem = false;
                }

                //if file already exists, we need to ask for confirmation to overwrite.
                if (file.exists())
                {
                    int result = JOptionPane.showOptionDialog(fc, "The file '" + file.getName() + "' already exists.  Overwrite?",
                                                      "Warning",JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, null, null);
                    if (result != JOptionPane.YES_OPTION) {
                        return;
                    }
                }

                //
                // Can't check parent is writable due to JDK bug (see bug 2349 for info)
                //
                if ((!file.exists() && file.getParentFile()!=null /*&& file.getParentFile().canWrite()*/) ||
                    (file.isFile() && file.canWrite())) {

                    byte[] data = null;

                    try {
                        data = isPem ?
                                CertUtils.encodeAsPEM(certificate).getBytes(Charsets.UTF8) :
                                certificate.getEncoded();
                    }
                    catch(IOException e) {
                        logger.log(Level.WARNING, "Error getting certificate data", e);
                        JOptionPane.showMessageDialog(parent, SAVE_DIALOG_ERROR_TITLE, "Certificate is invalid.", JOptionPane.ERROR_MESSAGE);
                    }
                    catch(CertificateEncodingException cee) {
                        logger.log(Level.WARNING, "Error getting certificate data", cee);
                        JOptionPane.showMessageDialog(parent, SAVE_DIALOG_ERROR_TITLE, "Certificate is invalid.", JOptionPane.ERROR_MESSAGE);
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
                            JOptionPane.showMessageDialog(parent, SAVE_DIALOG_ERROR_TITLE, "Error writing certificate file.", JOptionPane.ERROR_MESSAGE);
                        }
                        finally {
                            ResourceUtils.closeQuietly(out);
                        }
                    }
                }
                else {
                    logger.log(Level.WARNING, "Cannot write to selected certificate file");
                    JOptionPane.showMessageDialog(parent, SAVE_DIALOG_ERROR_TITLE,
                            "Cannot write to selected file.\n " + file.getAbsolutePath(), JOptionPane.ERROR_MESSAGE);
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
    private static final String PASSWORD_NOT_PROVIDED_MSG = "Password not provided.";

    /**
     * NOTE: For backwards compatibility a maximumImportedItems size of 1 means there
     * must only be one alias in the keystore.
     */
    private static void doImport( final Window parent,
                                  final boolean privateKeyRequired,
                                  final CallbackHandler callbackHandler,
                                  final int maximumImportedItems,
                                  final Functions.Unary<Boolean, ImportedData> importCallback ) {
        final JFileChooser fc = createFileChooser(privateKeyRequired);

        boolean done = false;
        while (!done) {
            int r = fc.showDialog(parent, "Load");

            if (r == JFileChooser.APPROVE_OPTION) {
                File file = fc.getSelectedFile();
                if (file!=null) {
                    //
                    // Can't check parent is writable due to JDK bug (see bug 2349 for info)
                    //
                    if ((!file.exists() && file.getParentFile()!=null /*&& file.getParentFile().canWrite()*/) ||
                        (file.isFile() && file.canRead())) {
                        String selectedFileName = file.getName();

                        FileFilter selectedFilter = fc.getFileFilter();
                        if (selectedFilter != pemFilter &&
                            selectedFilter != cerFilter &&
                            selectedFilter != p12Filter &&
                            selectedFilter != jksFilter &&
                            selectedFilter != p7bFilter) { // detect from extension
                            if (selectedFileName.endsWith(".pem") ||
                                selectedFileName.endsWith(".txt")) {
                                selectedFilter = pemFilter;
                            } else if (selectedFileName.endsWith(".cer") ||
                                     selectedFileName.endsWith(".der")) {
                                selectedFilter = cerFilter;
                            } else if (selectedFileName.endsWith(".p12") || selectedFileName.endsWith(".pfx")) {
                                selectedFilter = p12Filter;
                            } else if (selectedFileName.endsWith(".p7b") || selectedFileName.endsWith(".p7c")) {
                                selectedFilter = p7bFilter;
                            } else if (selectedFileName.endsWith(".jks") || selectedFileName.endsWith(".ks")) {
                                selectedFilter = jksFilter;
                            }
                        }


                        byte[] fileBytes = null;
                        InputStream in = null;
                        try {
                            in = new FileInputStream(file);
                            fileBytes = IOUtils.slurpStream(new ByteLimitInputStream(in, 1024, 1024*1024));
                        } catch (IOException ioe) {
                            JOptionPane.showMessageDialog(parent, "Error reading file", "Could not read file for import.", JOptionPane.ERROR_MESSAGE);
                            logger.log(Level.WARNING, "Error reading certificate data", ioe);
                            continue;
                        } finally {
                            ResourceUtils.closeQuietly(in);
                        }

                        try {
                            if (selectedFilter == pemFilter) {
                                String fileData = new String(fileBytes, Charsets.UTF8);
                                X509Certificate[] certificateChain = new X509Certificate[] { CertUtils.decodeFromPEM(fileData, true) };
                                PrivateKey privateKey = null;
                                if (privateKeyRequired || fileData.indexOf(" PRIVATE KEY-----")>0) {
                                    try {
                                        privateKey = CertUtils.decodeKeyFromPEM(fileData);
                                    }
                                    catch(IOException ioe) {
                                        if (privateKeyRequired) throw ioe;
                                    }
                                }
                                importData( certificateChain, privateKey, importCallback );
                            }
                            else if (selectedFilter == cerFilter) {
                                X509Certificate[] certificateChain = new X509Certificate[] { CertUtils.decodeCert(fileBytes) };
                                if (privateKeyRequired)
                                    throw new CausedIOException("Certificate files cannot contain private keys");
                                importData( certificateChain, null, importCallback );
                            }
                            else if (selectedFilter == p7bFilter) {
                                Collection<? extends Certificate> certs = CertUtils.getFactory().generateCertificates(new ByteArrayInputStream(fileBytes));
                                if (privateKeyRequired)
                                    throw new CausedIOException("Certificate files cannot contain private keys");
                                X509Certificate[] x509Certs = CertUtils.asX509CertificateArray(certs.toArray(new Certificate[certs.size()]));
                                importData(x509Certs, null, importCallback);
                            }
                            else if (selectedFilter == p12Filter || selectedFilter == jksFilter) {
                                boolean jks = selectedFilter == jksFilter;
                                try {
                                    String kstype = jks ? "JKS" : "PKCS12";
                                    KeyStore keyStore = KeyStore.getInstance(kstype);

                                    //javax.crypto.BadPaddingException
                                    boolean loaded = false;
                                    char[] password = new char[]{' '}; // don't pass null initially or integrity check is skipped;
                                    while (!loaded) {
                                        try {
                                            keyStore.load(new ByteArrayInputStream(fileBytes), password);
                                            loaded = true;
                                        }
                                        catch(Exception e) {
                                            if (callbackHandler != null &&
                                                    (ExceptionUtils.causedBy(e, javax.crypto.BadPaddingException.class) ||
                                                     ExceptionUtils.causedBy(e, UnrecoverableKeyException.class) ||
                                                     ExceptionUtils.causedBy(e, UnrecoverableEntryException.class))) {
                                                PasswordCallback passwordCallback = new PasswordCallback("Keystore password", false);
                                                Callback[] callbacks = new Callback[]{passwordCallback};
                                                callbackHandler.handle(callbacks);
                                                password = passwordCallback.getPassword();
                                                if (password == null) {
                                                    throw new CausedIOException(PASSWORD_NOT_PROVIDED_MSG);
                                                }
                                            }
                                            else {
                                                if (e instanceof IOException) {
                                                    throw (IOException)e;
                                                }
                                                throw new IOException(e);
                                            }
                                        }
                                    }

                                    List<String> aliases = Collections.list(keyStore.aliases());
                                    if ( aliases.isEmpty() )
                                        throw new CausedIOException(kstype + " keystore is empty");
                                    else if ( aliases.size() > 1 && maximumImportedItems==1 )
                                        throw new CausedIOException(kstype + " has unsupported number of entries (must be 1)"); // for backwards compatible behaviour

                                    int importedCount = 0;
                                    for ( String alias : aliases ) {
                                        if ( maximumImportedItems > 0 && importedCount >= maximumImportedItems ) break;

                                        if ( privateKeyRequired && !keyStore.isKeyEntry(alias) )
                                            throw new CausedIOException(kstype + " entry '"+alias+"' is not a key.");

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
                                        if ( privateKeyRequired && null == key )
                                            throw new IOException(kstype + " entry '"+alias+"' does not contain a key.");
                                        if ( privateKeyRequired && !(key instanceof PrivateKey))
                                            throw new IOException(kstype + " entry '"+alias+"' key is not a private key.");

                                        Certificate[] chain = keyStore.getCertificateChain(alias);
                                        if ( chain == null && !privateKeyRequired ) {
                                            final Certificate certificate = keyStore.getCertificate(alias);
                                            if (certificate != null)
                                                chain = new Certificate[] {certificate};
                                        }
                                        if ( chain==null || chain.length==0 ) {
                                            if ( maximumImportedItems != 1 ) continue;
                                            throw new CausedIOException(kstype + " entry '"+alias+"' missing does not contain a certificate chain.");
                                        }
                                        List<X509Certificate> got = new ArrayList<X509Certificate>();
                                        for (Certificate cert : chain) {
                                            if (cert == null)
                                                throw new IOException(kstype + " entry '" + alias + "' contains a null certificate in its certificate chain.");
                                            if (!(cert instanceof X509Certificate))
                                                throw new IOException(kstype + " entry '" + alias + "' certificate chain contains a non-X.509 certificate.");
                                            got.add((X509Certificate)cert);
                                        }

                                        X509Certificate[] certificateChain = got.toArray(new X509Certificate[got.size()]);
                                        PrivateKey privateKey = (PrivateKey) key;

                                        if ( !importData( certificateChain, privateKey, importCallback ) )
                                            break;
                                        importedCount++;
                                    }
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
                            final String msg = ioe.getMessage();
                            if (callbackHandler!=null &&
                                    ioe instanceof CausedIOException &&
                                    ("Invalid password for key entry.".equals(msg) || PASSWORD_NOT_PROVIDED_MSG.equals(msg))) {
                                logger.log(Level.INFO, "No password supplied, open cancelled.");
                                done = true;
                                continue;
                            }
                            else {
                                JOptionPane.showMessageDialog(parent, "Error reading file", "Could not read certificate.", JOptionPane.ERROR_MESSAGE);
                                logger.log(Level.WARNING, "Error reading certificate data. " + ExceptionUtils.getMessage(ioe));
                                continue;
                            }
                        }
                        catch(CertificateException ce) {
                            JOptionPane.showMessageDialog(parent, "Error decoding file", "Could not decode certificate.", JOptionPane.ERROR_MESSAGE);
                            logger.log(Level.WARNING, "Error reading certificate data", ce);
                            continue;
                        }
                    }
                    else {
                        logger.log(Level.WARNING, "Cannot read selected certificate file");
                        JOptionPane.showMessageDialog(parent, SAVE_DIALOG_ERROR_TITLE, "Cannot read selected file.\n " + file.getAbsolutePath(), JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
            else {
                done = true;
            }
        }
    }

    private static boolean importData( final X509Certificate[] certificateChain,
                                       final @Nullable PrivateKey privateKey,
                                       final Functions.Unary<Boolean, ImportedData> importCallback) {
        boolean continueImporting = true;

        if ( certificateChain != null && certificateChain.length > 0) {
            final ImportedData id = new ImportedData() {
                @Override
                public X509Certificate getCertificate() {
                    return getCertificateChain()[0];
                }

                @Override
                public X509Certificate[] getCertificateChain() {
                    return certificateChain;
                }

                @Override
                public PrivateKey getPrivateKey() {
                    return privateKey;
                }
            };

            continueImporting = importCallback.call(id);
        }

        return continueImporting;
    }

    private static FileFilter buildFilter(final String[] extensions, final String description) {
        return new FileFilter() {
            @Override
            public boolean accept(File f) {
                if(f.isDirectory()) {
                    return true;
                } else {
                    for(String extension : extensions) {
                        if(f.getName().toLowerCase().endsWith(extension)) {
                            return true;
                        }
                    }

                    return false;
                }
            }
            @Override
            public String getDescription() {
                return description;
            }
        };
    }
}
