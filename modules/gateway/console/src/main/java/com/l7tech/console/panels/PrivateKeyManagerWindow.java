package com.l7tech.console.panels;

import com.l7tech.common.io.AliasNotFoundException;
import com.l7tech.common.io.CertUtils;
import com.l7tech.console.MainWindow;
import com.l7tech.console.security.SecurityProvider;
import com.l7tech.console.util.DefaultAliasTracker;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.AsyncAdminMethods;
import com.l7tech.gateway.common.security.MultipleAliasesException;
import com.l7tech.gateway.common.security.TrustedCertAdmin;
import com.l7tech.gateway.common.security.keystore.KeystoreFileEntityHeader;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.gui.util.*;
import com.l7tech.gui.widgets.PasswordEntryDialog;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.FileUtils;
import com.l7tech.util.IOUtils;

import javax.security.auth.x500.X500Principal;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.AccessControlException;
import java.security.GeneralSecurityException;
import java.security.KeyStoreException;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.*;
import java.util.List;
import java.util.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Window for managing private key entries (certificate chains with private keys) in the Gateway.
 */
public class PrivateKeyManagerWindow extends JDialog {
    protected static final Logger logger = Logger.getLogger(PrivateKeyManagerWindow.class.getName());

    private JPanel mainPanel;
    private JScrollPane keyTableScrollPane;
    private JButton propertiesButton;
    private JButton closeButton;
    private JButton createButton;
    private JButton importButton;
    private JButton signCsrButton;
    private JButton manageKeystoreButton;
    private DefaultAliasTracker defaultAliasTracker;

    private static final Timer jobStatusTimer = new Timer("PrivateKeyManagerWindow job status timer");
    private static final Map<PrivateKeyManagerWindow, Object> timerClients = new WeakHashMap<PrivateKeyManagerWindow, Object>();
    static {
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        // Deliver tick events on the swing thread, as long as we are connected to the SSG
                        if (timerClients.isEmpty() || !Registry.getDefault().isAdminContextPresent())
                            return;

                        for (PrivateKeyManagerWindow client : timerClients.keySet())
                            if (client != null)
                                deliverTimerTick(client);
                    }

                    private void deliverTimerTick(PrivateKeyManagerWindow client) {
                        try {
                            client.onTimerTick();
                        } catch (Exception e) {
                            logger.log(Level.WARNING, "Unable to check status of background key management jobs: " + ExceptionUtils.getMessage(e), e);
                        }
                    }
                });
            }
        };
        jobStatusTimer.schedule(task, 311, 311);
    }

    private static ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.resources.CertificateDialog", Locale.getDefault());
    private static AsyncAdminMethods.JobId<X509Certificate> activeKeypairJob = null;
    private static String activeKeypairJobAlias = "";
    private static long lastJobPollTime = 0;
    private static long minJobPollInterval = 1011;

    private KeystoreFileEntityHeader mutableKeystore = null;
    private Component keypairJobViewportView;
    private PermissionFlags flags;
    private KeyTable keyTable = null;
    private Component showingInScrollPane = null;

    public PrivateKeyManagerWindow( Window owner ) {
        super(owner, resources.getString("keydialog.title"), DEFAULT_MODALITY_TYPE);
        initialize();
    }

    private void initialize() {
        timerClients.put(this, Boolean.TRUE);
        flags = PermissionFlags.get(EntityType.SSG_KEY_ENTRY);

        final SecurityProvider provider = Registry.getDefault().getSecurityProvider();
        if (provider == null) {
            throw new IllegalStateException("Could not instantiate security provider");
        }

        setContentPane(mainPanel);

        defaultAliasTracker = TopComponents.getInstance().getBean("defaultAliasTracker", DefaultAliasTracker.class);

        keyTable = new KeyTable();

        keyTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                enableOrDisableButtons();
            }
        });

        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                dispose();
            }
        });

        propertiesButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                doProperties();
            }
        });

        createButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doNewPrivateKey();
            }
        });

        importButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doImport();
            }
        });

        signCsrButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doSignCsr();
            }
        });

        manageKeystoreButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doManageKeystore();
            }
        });

        pack();
        enableOrDisableButtons();

        if (!flags.canReadAll()) {
            keyTable.setData(Collections.<KeyTableRow>emptyList());
            mutableKeystore = null;
        }

        Utilities.setEscKeyStrokeDisposes(this);
        Utilities.setDoubleClickAction(keyTable, propertiesButton);

        loadPrivateKeys();
    }

    private void disableManagementButtons() {
        createButton.setEnabled(false);
        importButton.setEnabled(false);
        propertiesButton.setEnabled(false);
    }

    private void enableManagementButtons() {
        propertiesButton.setEnabled(true);
        if (mutableKeystore == null) {
            createButton.setEnabled(false);
            importButton.setEnabled(false);
        } else {
            createButton.setEnabled(true);
            importButton.setEnabled(true);
        }

        if (!flags.canCreateSome()) {
            createButton.setEnabled(false);
            importButton.setEnabled(false);
        }
    }

    private void doRemove(KeyTableRow certHolder) {
        final KeystoreFileEntityHeader keystore = certHolder.getKeystore();
        String alias = certHolder.getAlias();
        try {
            getTrustedCertAdmin().deleteKey(keystore.getOid(), alias);
        } catch (IOException e) {
            showErrorMessage("Deletion Failed", "Unable to delete key: " + ExceptionUtils.getMessage(e), e);
        } catch (CertificateException e) {
            showErrorMessage("Deletion Failed", "Unable to delete key: " + ExceptionUtils.getMessage(e), e);
        } catch (DeleteException e) {
            showErrorMessage("Deletion Failed", "Unable to delete key: " + ExceptionUtils.getMessage(e), e);
        }
        loadPrivateKeys();
    }

    private void showImportErrorMessage(Throwable e) {
        String msg = ExceptionUtils.getMessage(e);
        int dupePos = msg.indexOf("Keystore already contains an entry with the alias");
        if (dupePos > 0) msg = msg.substring(dupePos);
        showErrorMessage("Import Failed", "Import failed: " + msg, e);
    }

    private void showErrorMessage(String title, String msg, Throwable e) {
        logger.log(Level.WARNING, msg, e);
        DialogDisplayer.showMessageDialog(this, msg, title, JOptionPane.ERROR_MESSAGE, null);
    }

    private void doManageKeystore() {
        boolean usingLuna = mutableKeystore != null && "LUNA_HARDWARE".equals(mutableKeystore.getKeyStoreType());
        ManageKeystoresDialog dlg = new ManageKeystoresDialog(this, usingLuna);
        dlg.pack();
        dlg.setModal(true);
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg);
    }

    private void doSignCsr() {
        final KeyTableRow subject = getSelectedObject();
        if (subject == null)
            return;

        if (subject.isCertCaCapable()) {
            signCsrNoConfirm(subject);
            return;
        }

        DialogDisplayer.showConfirmDialog(this,
                "This selected key's certificate does not specifically enable use as a CA cert.\n" +
                "Some software will reject certificates signed by this key." +
                "\n\nAre you sure you want to sign a certificate request using this key?",
                "Unsuitable CA Certificate",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE,
                new DialogDisplayer.OptionListener() {
                    @Override
                    public void reportResult(int option) {
                        if (option == JOptionPane.YES_OPTION)
                            signCsrNoConfirm(subject);
                    }
                });
    }

    private void signCsrNoConfirm(final KeyTableRow subject) {
        final byte[][] csrBytes = { null };
        FileChooserUtil.doWithJFileChooser(new FileChooserUtil.FileChooserUser() {
            @Override
            public void useFileChooser(JFileChooser fc) {
                csrBytes[0] = readCsrFile(fc);
            }
        });

        if (csrBytes[0] == null)
            return;

        final String[] pemCertChain;
        try {
            pemCertChain = getTrustedCertAdmin().signCSR(subject.getKeyEntry().getKeystoreId(), subject.getKeyEntry().getAlias(), csrBytes[0], null);
        } catch (CertificateException e) {
            showErrorMessage("Unable to Sign Certificate", "Unable to process certificate signing request: " + ExceptionUtils.getMessage(e), e);
            return;
        } catch (FindException e) {
            showErrorMessage("Unable to Sign Certificate", "Unable to process certificate signing request: " + ExceptionUtils.getMessage(e), e);
            return;
        } catch (GeneralSecurityException e) {
            showErrorMessage("Unable to Sign Certificate", "Unable to process certificate signing request: " + ExceptionUtils.getMessage(e), e);
            return;
        }

        FileChooserUtil.doWithJFileChooser(new FileChooserUtil.FileChooserUser() {
            @Override
            public void useFileChooser(JFileChooser fc) {
                savePemCertChain(fc, pemCertChain);
            }
        });
    }

    private void savePemCertChain(JFileChooser fc, final String[] pemCertChain) {
        fc.setDialogTitle("Save Signed Certificate Chain");
        fc.setDialogType(JFileChooser.SAVE_DIALOG);
        fc.setMultiSelectionEnabled(false);
        FileFilter pemFilter = FileChooserUtil.buildFilter(".pem", "(*.pem) BASE64 PEM Certificate Chain");
        fc.setFileFilter(pemFilter);

        int result = fc.showSaveDialog(this);
        if (JFileChooser.APPROVE_OPTION != result)
            return;

        File file = fc.getSelectedFile();
        if (file == null)
            return;

        //if user did not append .pem extension, we'll append to it
        if (!file.getName().endsWith(".pem")){
            file = new File(file.toString() + ".pem");
        } else {
            file = new File(file.getParent(), file.getName());
        }

        try {

            //if file already exists, we need to ask for confirmation to overwrite.
            if (file.exists()) {
                result = JOptionPane.showOptionDialog(fc, "The file '" + file.getName() + "' already exists.  Overwrite?",
                        "Warning",JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, null, null);
                if (result != JOptionPane.YES_OPTION) {
                    return;
                }
            }

            FileUtils.save(file, new FileUtils.Saver() {
                @Override
                public void doSave(FileOutputStream fos) throws IOException {
                    for (String msg : pemCertChain) {
                        fos.write(msg.getBytes("ASCII")); // it's PEM
                    }
                }
            });
        } catch (IOException e) {
            showErrorMessage("Unable to Save Certificate Chain", "Unable to save certificate chain: " + ExceptionUtils.getMessage(e), e);
        }
    }

    private byte[] readCsrFile(JFileChooser fc) {
        fc.setDialogTitle("Open Certificate Signing Request");
        fc.setDialogType(JFileChooser.OPEN_DIALOG);
        fc.setMultiSelectionEnabled(false);
        FileFilter pemFilter = FileChooserUtil.buildFilter(".pem", "(*.pem) BASE64 PEM PKCS#10 Certificate Signing Request");
        fc.setFileFilter(pemFilter);

        int result = fc.showOpenDialog(this);
        if (JFileChooser.APPROVE_OPTION != result)
            return null;

        File file = fc.getSelectedFile();
        if (file == null)
            return null;

        try {
            return FileUtils.load(file);
        } catch (IOException e) {
            showErrorMessage("Unable to Open CSR", "Unable to read CSR file: " + ExceptionUtils.getMessage(e), e);
            return null;
        }
    }

    private void doImport() {
        if (mutableKeystore == null)
            return;
        final long mutableKeystoreId = mutableKeystore.getOid();

        String alias = JOptionPane.showInputDialog(this, "Please enter an alias to use for the new private key entry.");
        if (alias == null || alias.trim().length() < 1)
            return;
        alias = alias.trim();

        SsgKeyEntry imported = performImport(mutableKeystoreId, alias);
        if (imported == null)
            return;

        X509Certificate[] chain = imported.getCertificateChain();
        if (chain != null && chain.length > 0)
            displayCertificateChainWarnings(chain);

        loadPrivateKeys();
        setSelectedKeyEntry(mutableKeystoreId, alias);
    }


    private SsgKeyEntry performImport(long keystoreId, String alias) {
        Throwable err;
        try {
            JFileChooser fc = GuiCertUtil.createFileChooser(true);
            int r = fc.showDialog(this, "Load");
            if (r != JFileChooser.APPROVE_OPTION)
                return null;
            File file = fc.getSelectedFile();
            if (file == null)
                return null;

            byte[] pkcs12bytes = IOUtils.slurpFile(file);

            char[] pkcs12pass = PasswordEntryDialog.promptForPassword(this, "Enter pass phrase for PKCS#12 file");

            try {
                return Registry.getDefault().getTrustedCertManager().importKeyFromPkcs12(keystoreId, alias, pkcs12bytes, pkcs12pass, null);
            } catch (MultipleAliasesException e) {
                Object defaultOptionPaneUI = UIManager.get("OptionPaneUI");
                UIManager.put("OptionPaneUI", ComboBoxOptionPaneUI.class.getName());  // Let JOptionPane use JCombobox rather than JList to display aliases
                String pkcs12alias = (String) JOptionPane.showInputDialog(this, "Select alias to import", "Select Alias", JOptionPane.QUESTION_MESSAGE, null, e.getAliases(), null);
                UIManager.put("OptionPaneUI", defaultOptionPaneUI);

                if (pkcs12alias == null)
                    return null;
                return Registry.getDefault().getTrustedCertManager().importKeyFromPkcs12(keystoreId, alias, pkcs12bytes, pkcs12pass, pkcs12alias);
            }

        } catch (AccessControlException ace) {
            TopComponents.getInstance().showNoPrivilegesErrorMessage();
            return null;
        } catch (AliasNotFoundException e) {
            err = ExceptionUtils.getDebugException(e);
        } catch (FindException e) {
            err = e;
        } catch (IOException e) {
            err = e;
        } catch (KeyStoreException e) {
            err = e;
        } catch (MultipleAliasesException e) {
            // Can't happen; we pass an alias the second time
            err = e;
        } catch (SaveException e) {
            err = e;
        }

        showImportErrorMessage(err);

        return null;
    }

    private void displayCertificateChainWarnings(X509Certificate[] chain) {
        boolean sawExpired = false;
        boolean sawNotYetValid = false;
        boolean sawDnMismatch = false;
        boolean sawBadSignature = false;
        for (int i = 0; i < chain.length; i++) {
            X509Certificate cert = chain[i];

            // Record warnings if any cert expired or not yet valid
            try {
                cert.checkValidity();
            } catch (CertificateExpiredException e) {
                sawExpired = true;
            } catch (CertificateNotYetValidException e) {
                sawNotYetValid = true;
            }

            // Record warnings about broken chain
            if (i + 1 < chain.length) {
                X509Certificate issuerCert = chain[i + 1];
                String certIssuerDn = cert.getIssuerX500Principal().getName(X500Principal.CANONICAL);
                String issuerSubjectDn = issuerCert.getSubjectX500Principal().getName(X500Principal.CANONICAL);
                if (!certIssuerDn.equalsIgnoreCase(issuerSubjectDn)) {
                    sawDnMismatch = true;
                }

                try {
                    cert.verify(issuerCert.getPublicKey()); // TODO check that issuer allows use as CA cert
                } catch (GeneralSecurityException e) {
                    sawBadSignature = true;
                }

                // TODO warn here about key usage problems that we detect
                // can't just use KeyUsageChecker since its Gateway-side configuration isn't known
            }
        }

        if (sawExpired || sawNotYetValid || sawDnMismatch || sawBadSignature) {
            StringBuffer mess = new StringBuffer("The specified private key was imported successfully.  However, at least one certificate in its chain: \n\n");
            if (sawExpired) mess.append("     * has expired\n");
            if (sawNotYetValid) mess.append("     * is not yet valid\n");
            if (sawDnMismatch) mess.append("     * has an issuer DN that does not match its issuer's DN\n");
            if (sawBadSignature) mess.append("     * has an invalid issuer signature\n");
            JOptionPane.showMessageDialog(this, mess.toString(), "Certificate Chain Warning", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void doProperties() {
        final KeyTableRow data = getSelectedObject();
        final PrivateKeyPropertiesDialog dlg = new PrivateKeyPropertiesDialog(this, data, flags);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            @Override
            public void run() {
                if (dlg.isDefaultKeyChanged()) {
                    defaultAliasTracker.invalidate();
                    loadPrivateKeys();
                } else if (dlg.isDeleted()) {
                    doRemove(data);
                }
            }
        });
    }

    /** @return the currently selected row or null */
    private KeyTableRow getSelectedObject() {
        int sr = keyTable.getSelectedRow();
        if (sr < 0)
            return null;
        sr = keyTable.convertRowIndexToModel(sr);
        return keyTable.getRowAt(sr);
    }

    private void doNewPrivateKey() {
        final NewPrivateKeyDialog dlg = new NewPrivateKeyDialog(this, mutableKeystore);
        dlg.setModal(true);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            @Override
            public void run() {
                if (dlg.isConfirmed()) {
                    setActiveKeypairJob(dlg.getKeypairJobId(), dlg.getNewAlias(), dlg.getSecondsToWaitForJobToFinish());
                    loadPrivateKeys();
                }
            }
        });
    }

    private static void setActiveKeypairJob(AsyncAdminMethods.JobId<X509Certificate> keypairJobId, String alias, int secondsToWaitForJobToFinish) {
        activeKeypairJob = keypairJobId;
        activeKeypairJobAlias = alias;

        int minPoll = (secondsToWaitForJobToFinish * 1000) / 30;
        if (minPoll < 1011) minPoll = 1011;
        minJobPollInterval = minPoll;
    }

    private void setSelectedKeyEntry(long keystoreId, String newAlias) {
        keyTable.setSelectedKeyEntry(keystoreId, newAlias);
    }

    private void onTimerTick() {
        if (activeKeypairJob != null || showingInScrollPane != keyTable)
            loadPrivateKeys();
    }

    /*
     * Configure the dialog to display either the current cert list, or a please wait panel
     * if the Gateway is currently performing a generate keypair operation for us.
     */
    private List<KeyTableRow> loadPrivateKeys() {
        if (isKeypairJobActive()) {
            String mess = "        Gateway is generating a new key pair (may take up to several minutes)...";
            if (keypairJobViewportView == null) {
                JPanel panel = new JPanel(new BorderLayout());
                JLabel keypairJobProgressLabel = new JLabel(mess);
                JProgressBar keypairJobProgressBar = new JProgressBar();
                JPanel p = new JPanel();
                p.setLayout(null);
                p.add(keypairJobProgressLabel);
                p.add(keypairJobProgressBar);
                p.setSize(480, 110);
                keypairJobProgressLabel.setLocation(10, 20);
                keypairJobProgressLabel.setSize(keypairJobProgressLabel.getPreferredSize());
                keypairJobProgressBar.setLocation(10, 80);
                keypairJobProgressBar.setSize(460, 20);
                keypairJobProgressBar.setPreferredSize(new Dimension(460, 20));
                panel.add(p, BorderLayout.CENTER);
                keypairJobViewportView = panel;
                keypairJobProgressBar.setIndeterminate(true);
                keypairJobProgressBar.setStringPainted(false);
            }

            keyTableScrollPane.setViewport(null);
            keyTableScrollPane.setViewportView(showingInScrollPane = keypairJobViewportView);
            keyTableScrollPane.getViewport().setBackground(keypairJobViewportView.getBackground());
            disableManagementButtons();
            return Collections.emptyList();
        }

        if (showingInScrollPane != keyTable) {
            keyTableScrollPane.setViewport(null);
            keyTableScrollPane.setViewportView(showingInScrollPane = keyTable);
            keyTableScrollPane.getViewport().setBackground(Color.white);
        }

        try {
            java.util.List<KeyTableRow> keyList = new ArrayList<KeyTableRow>();
            for (KeystoreFileEntityHeader keystore : getTrustedCertAdmin().findAllKeystores(true)) {
                if (mutableKeystore == null && !keystore.isReadonly()) mutableKeystore = keystore;
                for (SsgKeyEntry entry : getTrustedCertAdmin().findAllKeys(keystore.getOid())) {
                    keyList.add(new KeyTableRow(keystore, entry, isDefaultSslCert(entry), isDefaultCaCert(entry)));
                }
            }

            keyTable.setData(keyList);
            enableManagementButtons();
            enableOrDisableButtons();

            if (activeKeypairJobAlias != null && mutableKeystore != null) {
                keyTable.setSelectedKeyEntry(mutableKeystore.getOid(), activeKeypairJobAlias);
                activeKeypairJobAlias = null;
            }

            return keyList;

        } catch ( FindException fe) {
            displayError( fe );
        } catch ( IOException ioe ) {
            displayError( ioe );
        } catch ( KeyStoreException kse ) {
            displayError( kse );
        } catch ( CertificateException ce) {
            displayError( ce );
        }

        return Collections.emptyList();
    }

    private void displayError( final Exception e ) {
        String msg = "Unable to load private keys";
        String logMsg = msg + ": "  + ExceptionUtils.getMessage(e);
        logger.log(Level.WARNING, logMsg , e);
        DialogDisplayer.showMessageDialog(PrivateKeyManagerWindow.this, null, msg, e);
    }

    /**
     * Enable or disable the fields based on the current selections.
     */
    private void enableOrDisableButtons() {
        KeyTableRow row = getSelectedObject();
        boolean certSelected = row != null && activeKeypairJob == null;
        propertiesButton.setEnabled(certSelected);
        signCsrButton.setEnabled(certSelected && flags.canDeleteSome());
        manageKeystoreButton.setEnabled(flags.canDeleteAll());
    }

    /**
     * Retrieve the object reference of the Trusted Cert Admin service
     *
     * @return TrustedCertAdmin  - The object reference.
     * @throws RuntimeException if the object reference of the Trusted Cert Admin service is not found.
     */
    private static TrustedCertAdmin getTrustedCertAdmin() throws RuntimeException {
        return Registry.getDefault().getTrustedCertManager();
    }

    /**
     * Check if there is a keypair job currently active.  If one has recently finished, displays the result
     * of the job in a dialog.
     *
     * @return true if there is currently a private key job being performed on this Gateway node.
     */
    public boolean isKeypairJobActive() {
        if (activeKeypairJob == null)
            return false;

        final long now = System.currentTimeMillis();
        if (now - lastJobPollTime < minJobPollInterval)
            return true;

        try {
            lastJobPollTime = now;
            String status = getTrustedCertAdmin().getJobStatus(activeKeypairJob);
            if (status == null) {
                activeKeypairJob = null;
                return false;
            } else if (status.startsWith("inactive:")) {
                AsyncAdminMethods.JobResult<X509Certificate> result = getTrustedCertAdmin().getJobResult(activeKeypairJob);
                activeKeypairJob = null;
                if (result.throwableClassname != null) {
                    final String gotmess = result.throwableMessage;
                    final String mess;
                    int pos;
                    if (gotmess != null && gotmess.indexOf("com.l7tech.common.io.DuplicateAliasException") >= 0) {
                        // More friendly error message for one common, foreseeable problem (Bug #3923)
                        mess = "Unable to generate key pair: the specified alias is already in use.";
                    } else if (gotmess != null && (pos = gotmess.indexOf("java.security.InvalidKeyException: Curve is too small for a ")) >= 0) {
                        // More friendly error message for another common, foreseeable problem (Bug #7648)
                        mess = "Unable to generate key pair: " + gotmess.substring(pos + 35);
                    } else if (gotmess != null && (pos = gotmess.indexOf("NoSuchAlgorithmException:")) >= 0) {
                        // More friendly error message for another common, foreseeable problem (Bug #7648)
                        mess = "Unable to generate key pair: " + gotmess.substring(pos + 25);
                    } else {
                        mess = "Key generation failed: " + result.throwableClassname + ": " + gotmess;
                    }

                    logger.log(Level.WARNING, mess);
                    JOptionPane.showMessageDialog(this,
                                                  mess,
                                                  "Key Generation Failed",
                                                  JOptionPane.ERROR_MESSAGE);
                } else if (result.result != null && mutableKeystore != null && activeKeypairJobAlias != null) {
                    keyTable.setSelectedKeyEntry(mutableKeystore.getOid(), activeKeypairJobAlias);
                    activeKeypairJobAlias = null;
                }
                return false;
            } else {
                return true;
            }
        } catch (AsyncAdminMethods.UnknownJobException e) {
            logger.log(Level.WARNING, "Unable to check remote job status: " + ExceptionUtils.getMessage(e), e);
            activeKeypairJob = null;
            return false;
        } catch ( AsyncAdminMethods.JobStillActiveException e) {
            logger.log(Level.WARNING, "Unable to check remote job status: " + ExceptionUtils.getMessage(e), e);
            return true;
        }
    }


    public boolean isDefaultSslCert(SsgKeyEntry entry) {
        String alias = entry.getAlias();
        return alias != null && alias.equalsIgnoreCase(defaultAliasTracker.getDefaultSslAlias());
    }

    public boolean isDefaultCaCert(SsgKeyEntry entry) {
        String alias = entry.getAlias();
        return alias != null && alias.equalsIgnoreCase(defaultAliasTracker.getDefaultCaAlias());
    }

    /** Represents a row in the Manage Private Keys table. */
    public static class KeyTableRow {
        private final KeystoreFileEntityHeader keystoreInfo;
        private SsgKeyEntry keyEntry;
        private String keyType = null;
        private boolean defaultSsl;
        private boolean defaultCa;
        private boolean certCaCapable;

        public KeyTableRow(KeystoreFileEntityHeader keystoreInfo, SsgKeyEntry keyEntry, boolean defaultSsl, boolean defaultCa) {
            this.keystoreInfo = keystoreInfo;
            this.keyEntry = keyEntry;
            this.defaultSsl = defaultSsl;
            this.defaultCa = defaultCa;
            this.certCaCapable = keyEntry != null && CertUtils.isCertCaCapable(keyEntry.getCertificate());
        }

        public KeystoreFileEntityHeader getKeystore() {
            return keystoreInfo;
        }

        public SsgKeyEntry getKeyEntry() {
            return keyEntry;
        }

        public void setKeyEntry(SsgKeyEntry keyEntry) {
            this.keyEntry = keyEntry;
        }

        public String getAlias() {
            return keyEntry.getAlias();
        }

        public String getSubjectDN() {
            return keyEntry.getSubjectDN();
        }

        public X509Certificate getCertificate() {
            return keyEntry.getCertificate();
        }

        public String getKeyType() {
            if (keyType == null) {
                PublicKey publicKey = getCertificate().getPublicKey();
                String alg = publicKey.getAlgorithm();
                if (alg == null) alg = "N/A"; // can't happen
                if (publicKey instanceof RSAPublicKey) {
                    RSAPublicKey rsaKey = (RSAPublicKey) publicKey;
                    String modulus = rsaKey.getModulus().toString(16);
                    alg = MessageFormat.format("{0} {1} bits", alg, modulus.length() * 4);
                }

                if ("EC".equals(alg)) {
                    // See if we can be more specific
                    String guessedCurveName = CertUtils.guessEcCurveName(publicKey);
                    if (guessedCurveName != null)
                        alg = "EC " + guessedCurveName;
                }

                keyType = alg;
            }
            return keyType;
        }

        public Object getExpiry() {
            return getCertificate().getNotAfter();
        }

        public boolean isDefaultSsl() {
            return defaultSsl;
        }

        public boolean isDefaultCa() {
            return defaultCa;
        }

        public boolean isCertCaCapable() {
            return certCaCapable;
        }
    }

    private static class KeyTable extends JTable {
        private static final String RESDIR = MainWindow.RESOURCE_PATH;
        private static final String PATH_SSL = RESDIR + "/cert_flag_ssl_16.png";
        private static final String PATH_CA = RESDIR + "/cert_flag_ca_16.png";
        private static final String PATH_SSLCA = RESDIR + "/cert_flag_sslca_16.png";
        private static final String PATH_CERT_SSL = RESDIR + "/cert_ssl_16.gif";
        private static final String PATH_CERT_CA = RESDIR + "/cert_ca_16.gif";

        private final KeyTableModel model = new KeyTableModel();

        public KeyTable() {
            setModel(model);
            getTableHeader().setReorderingAllowed(false);
            getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            TableColumnModel cols = getColumnModel();
            int numCols = model.getColumnCount();
            for (int i = 0; i < numCols; ++i) {
                final TableColumn col = cols.getColumn(i);
                col.setMinWidth(model.getColumnMinWidth(i));
                col.setPreferredWidth(model.getColumnPrefWidth(i));
                col.setMaxWidth(model.getColumnMaxWidth(i));
                if (model.isColumnImage(i)) col.setCellRenderer(new JTable().getDefaultRenderer(ImageIcon.class));
            }
            setRowHeight(19);
            Utilities.setRowSorter(this, model, new int[]{1,2}, new boolean[]{true, true},
                    new Comparator[]{null, null});
            this.setDefaultRenderer( Date.class,  new DefaultTableCellRenderer(){
                @Override
                protected void setValue(Object value) {
                    super.setValue( DateFormat.getDateInstance().format((Date)value) );
                }
            });
        }

        public KeyTableRow getRowAt(int row) {
            return model.getRowAt(row);
        }

        public void setData(List<KeyTableRow> rows) {
            model.setData(rows);
        }

        public void setSelectedKeyEntry(long keystoreId, String newAlias) {
            int row = model.findRowIndex(keystoreId, newAlias);
            if (row < 0) {
                getSelectionModel().clearSelection();
            } else {
                int viewRow = this.convertRowIndexToView(row);
                getSelectionModel().setSelectionInterval(viewRow, viewRow);
                scrollRectToVisible(getCellRect(viewRow, 0, true));
            }
        }

        private static class KeyTableModel extends AbstractTableModel {
            private static abstract class Col {
                final String name;
                final int minWidth;
                final int prefWidth;
                final int maxWidth;
                final Class columnClass;
                boolean isImage;

                protected Col(String name, int minWidth, int prefWidth, int maxWidth) {
                    this( name, minWidth, prefWidth, maxWidth, String.class );
                }

                protected Col(String name, int minWidth, int prefWidth, int maxWidth, Class columnClass) {
                    this.name = name;
                    this.minWidth = minWidth;
                    this.prefWidth = prefWidth;
                    this.maxWidth = maxWidth;
                    this.columnClass = columnClass;
                }
                abstract Object getValueForRow(KeyTableRow row);
            }

            public static final Col[] columns = new Col[] {
                    new Col(" ", 19, 19, 19, Object.class) {
                        @Override
                        Object getValueForRow(KeyTableRow row) {
                            return row.isCertCaCapable()
                                    ? ImageCache.getInstance().getIconAsIcon(PATH_CERT_CA)
                                    : ImageCache.getInstance().getIconAsIcon(PATH_CERT_SSL);
                        }
                        { isImage = true; }
                    },

                    new Col("Alias", 60, 90, 300) {
                        @Override
                        Object getValueForRow(KeyTableRow row) {
                            return row.getAlias();
                        }
                    },

                    new Col("Subject", 3, 100, 999999) {
                        @Override
                        Object getValueForRow(KeyTableRow row) {
                            return row.getSubjectDN();
                        }
                    },

                    new Col("Key Type", 3, 88, 88) {
                        @Override
                        Object getValueForRow(KeyTableRow row) {
                            return row.getKeyType();
                        }
                    },

                    new Col("Expiry", 3, 85, 85, Date.class) {
                        @Override
                        Object getValueForRow(KeyTableRow row) {
                            return row.getExpiry();
                        }
                    },

                    new Col("Location", 60, 90, 90) {
                        @Override
                        Object getValueForRow(KeyTableRow row) {
                            return row.getKeystore().getName();
                        }
                    },

                    new Col(" ", 19, 19, 19, Object.class) {
                        @Override
                        Object getValueForRow(KeyTableRow row) {
                            int val = row.isDefaultSsl() ? 1 : 0;
                            if (row.isDefaultCa()) val += 2;
                            switch (val) {
                                case 0:
                                    return "";
                                case 1:
                                    return ImageCache.getInstance().getIconAsIcon(PATH_SSL);
                                case 2:
                                    return ImageCache.getInstance().getIconAsIcon(PATH_CA);
                                case 3:
                                    return ImageCache.getInstance().getIconAsIcon(PATH_SSLCA);
                            }
                            /* NOTREACHED */
                            return "";
                        }
                        { isImage = true; }
                    },
            };

            private final List<KeyTableRow> rows = new ArrayList<KeyTableRow>();

            public KeyTableModel() {
            }

            public int getColumnMinWidth(int column) {
                return columns[column].minWidth;
            }

            public int getColumnPrefWidth(int column) {
                return columns[column].prefWidth;
            }

            public int getColumnMaxWidth(int column) {
                return columns[column].maxWidth;
            }

            @Override
            public String getColumnName(int column) {
                return columns[column].name;
            }

            public boolean isColumnImage(int column) {
                return columns[column].isImage;
            }

            public void setData(List<KeyTableRow> rows) {
                this.rows.clear();
                this.rows.addAll(rows);
                fireTableDataChanged();
            }

            /**
             * @param keystoreId keystore id to search for
             * @param newAlias   alias to search for.  must not be null
             * @return index of row with the specified keystore ID and alias, or -1 if not found.
             */
            public int findRowIndex(long keystoreId, String newAlias) {
                KeyTableRow[] rowsArray = rows.toArray(new KeyTableRow[rows.size()]);
                for (int i = 0; i < rowsArray.length; i++) {
                    KeyTableRow row = rowsArray[i];
                    if (row.getKeystore().getOid() == keystoreId && row.getAlias().equalsIgnoreCase(newAlias))
                        return i;
                }
                return -1;
            }

            @Override
            public Class<?> getColumnClass(int column) {
                return columns[column].columnClass;
            }

            @Override
            public int getRowCount() {
                return rows.size();
            }

            @Override
            public int getColumnCount() {
                return columns.length;
            }

            @Override
            public Object getValueAt(int rowIndex, int columnIndex) {
                return columns[columnIndex].getValueForRow(rows.get(rowIndex));
            }

            public KeyTableRow getRowAt(int rowIndex) {
                return rows.get(rowIndex);
            }
        }
    }
}
