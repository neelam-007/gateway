package com.l7tech.console.panels;

import com.l7tech.common.io.AliasNotFoundException;
import com.l7tech.common.io.CertUtils;
import com.l7tech.console.MainWindow;
import com.l7tech.console.security.SecurityProvider;
import com.l7tech.console.util.ActiveKeypairJob;
import com.l7tech.console.util.DefaultAliasTracker;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.AsyncAdminMethods;
import com.l7tech.gateway.common.security.MultipleAliasesException;
import com.l7tech.gateway.common.security.SpecialKeyType;
import com.l7tech.gateway.common.security.TrustedCertAdmin;
import com.l7tech.gateway.common.security.keystore.KeystoreFileEntityHeader;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.gateway.common.security.keystore.SsgKeyMetadata;
import com.l7tech.gui.util.*;
import com.l7tech.gui.widgets.PasswordEntryDialog;
import com.l7tech.objectmodel.*;
import com.l7tech.util.*;

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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Window for managing private key entries (certificate chains with private keys) in the Gateway.
 */
public class PrivateKeyManagerWindow extends JDialog {
    protected static final Logger logger = Logger.getLogger(PrivateKeyManagerWindow.class.getName());

    private static final int TICKS_PER_ICON_REPAINT = 3;
    public static final int TASK_DELAY = 311;
    public static final int EXECUTION_PERIOD = 311;

    private JPanel mainPanel;
    private JScrollPane keyTableScrollPane;
    private JButton propertiesButton;
    private JButton closeButton;
    private JButton createButton;
    private JButton importButton;
    private JButton signCsrButton;
    private JButton manageKeystoreButton;
    private DefaultAliasTracker defaultAliasTracker;

    private static final AtomicInteger iconFlipCount = new AtomicInteger(0);

    private static ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.resources.CertificateDialog", Locale.getDefault());

    private final Timer jobStatusTimer = new Timer("PrivateKeyManagerWindow job status timer");
    private KeystoreFileEntityHeader mutableKeystore = null;
    private Component keypairJobViewportView;
    private PermissionFlags flags;
    private KeyTable keyTable = null;
    private Component showingInScrollPane = null;
    private boolean hasAtLeastOneMultiRoleKey = false;
    private final ActiveKeypairJob activeKeypairJob;

    public PrivateKeyManagerWindow( Window owner ) {
        super(owner, resources.getString("keydialog.title"), DEFAULT_MODALITY_TYPE);
        activeKeypairJob = TopComponents.getInstance().getActiveKeypairJob();// set active keypair job that is used in asynchronous generation of the private keys
        initialize();
    }

    private void initialize() {
        flags = PermissionFlags.get(EntityType.SSG_KEY_ENTRY);

        final SecurityProvider provider = Registry.getDefault().getSecurityProvider();
        if (provider == null) {
            throw new IllegalStateException("Could not instantiate security provider");
        }

        setContentPane(mainPanel);

        defaultAliasTracker = new DefaultAliasTracker();

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

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                dispose();
            }
        });

        pack();
        enableOrDisableButtons();

        if (!flags.canReadSome()) {
            keyTable.setData(Collections.<KeyTableRow>emptyList());
        }

        if (!(flags.canUpdateSome() || flags.canCreateSome())) {
            mutableKeystore = null;
        }

        Utilities.setEscKeyStrokeDisposes(this);
        Utilities.setDoubleClickAction(keyTable, propertiesButton);

        loadPrivateKeys();
        checkActiveJob();
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
            getTrustedCertAdmin().deleteKey(keystore.getGoid(), alias);
        } catch (IOException e) {
            showErrorMessage("Deletion Failed", "Unable to delete key: " + ExceptionUtils.getMessage(e), e);
        } catch (CertificateException e) {
            showErrorMessage("Deletion Failed", "Unable to delete key: " + ExceptionUtils.getMessage(e), e);
        } catch (DeleteException e) {
            showErrorMessage("Deletion Failed", "Unable to delete key: " + ExceptionUtils.getMessage(e), e);
        }
        loadPrivateKeys();
    }

    private boolean showImportErrorMessage( final Throwable e,
                                            final boolean allowRetry ) {
        boolean retry = false;
        String msg = ExceptionUtils.getMessage(e);
        int dupePos = msg.indexOf("Keystore already contains an entry with the alias");
        if (dupePos > 0) msg = msg.substring(dupePos);
        if ( !allowRetry ) {
            showErrorMessage("Import Failed", "Import failed: " + msg, e);
        } else {
            final int choice = JOptionPane.showOptionDialog(
                    this,
                    "Import failed: " + msg + "\n\nDo you want to try again?",
                    "Import Failed",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    null,
                    new Object[]{ "Import", "Cancel" },
                    "Import" );
            retry = choice == JOptionPane.YES_OPTION;
        }
        return retry;
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

        final Goid keystoreId = subject.getKeyEntry().getKeystoreId();
        final String keyAlias = subject.getKeyEntry().getAlias();
        Functions.Nullary<Boolean> precheckingShortKeyFunc = new Functions.Nullary<Boolean>() {
            @Override
            public Boolean call() {
                try {
                    return getTrustedCertAdmin().isShortSigningKey(keystoreId, keyAlias);
                } catch (Exception e) {
                    return false;
                }
            }
        };

        // Display the contents of the CSR and allow user to set/modify some settings before signing certificate.
        final SigningCertificatePropertiesDialog signingCertPropertiesDialog;

        try {
            // For security permission issue in applet, we send the server a request to finish the task.
            signingCertPropertiesDialog = new SigningCertificatePropertiesDialog(
                TopComponents.getInstance().getTopParent(),
                getTrustedCertAdmin().getCsrProperties(csrBytes[0]),  // Call Remote Admin API to process the task
                precheckingShortKeyFunc
            );
        } catch (Exception e) {
            showErrorMessage("Unable to Sign Certificate", "Unable to process certificate signing request: " + ExceptionUtils.getMessage(e), e);
            return;
        }

        signingCertPropertiesDialog.setPostTaskFunc(new Functions.Nullary<Void>() {
            @Override
            public Void call() {
                // After the dialog is confirmed against the contents of the CSR, then generate a new certificate chain and save the new certificate.
                final String[] pemCertChain;
                try {
                    pemCertChain = getTrustedCertAdmin().signCSR(
                        keystoreId,
                        keyAlias,
                        csrBytes[0],
                        new X500Principal(signingCertPropertiesDialog.getSubjectDn()),
                        signingCertPropertiesDialog.getExpiryAge(),
                        null,
                        signingCertPropertiesDialog.getHashAlg()
                    );
                } catch (CertificateException e) {
                    showErrorMessage("Unable to Sign Certificate", "Unable to process certificate signing request: " + ExceptionUtils.getMessage(e), e);
                    return null;
                } catch (FindException e) {
                    showErrorMessage("Unable to Sign Certificate", "Unable to process certificate signing request: " + ExceptionUtils.getMessage(e), e);
                    return null;
                } catch (GeneralSecurityException e) {
                    showErrorMessage("Unable to Sign Certificate", "Unable to process certificate signing request: " + ExceptionUtils.getMessage(e), e);
                    return null;
                }

                FileChooserUtil.doWithJFileChooser(new FileChooserUtil.FileChooserUser() {
                    @Override
                    public void useFileChooser(JFileChooser fc) {
                        savePemCertChain(fc, pemCertChain);
                    }
                });

                return null;
            }
        });

        Utilities.centerOnScreen(signingCertPropertiesDialog);
        DialogDisplayer.display(signingCertPropertiesDialog);
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
                        fos.write(msg.getBytes(Charsets.ASCII)); // it's PEM
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
        final Goid mutableKeystoreId = mutableKeystore.getGoid();

        final ImportPrivateKeyDialog importDialog = new ImportPrivateKeyDialog(TopComponents.getInstance().getTopParent());
        importDialog.pack();
        Utilities.centerOnScreen(importDialog);
        DialogDisplayer.display(importDialog, new Runnable() {
            @Override
            public void run() {
                if (importDialog.isConfirmed()) {
                    final String alias = importDialog.getAlias();
                    SsgKeyEntry imported = performImport(mutableKeystoreId, alias, importDialog.getSecurityZone());
                    if (imported == null)
                        return;

                    X509Certificate[] chain = imported.getCertificateChain();
                    if (chain != null && chain.length > 0)
                        displayCertificateChainWarnings(chain);

                    loadPrivateKeys();
                    setSelectedKeyEntry(mutableKeystoreId, alias);
                }
            }
        });
    }


    private SsgKeyEntry performImport(Goid keystoreId, String alias, SecurityZone zone) {
        byte[] ksbytes = null;
        String kstype = null;
        String ksalias = null;
        boolean jks = false;

        while ( true ) {
            boolean passwordError = false;
            Throwable err;
            try {
                if ( ksbytes == null ) {
                    JFileChooser fc = GuiCertUtil.createFileChooser(true);
                    int r = fc.showDialog(this, "Load");
                    if (r != JFileChooser.APPROVE_OPTION)
                        return null;
                    File file = fc.getSelectedFile();
                    if (file == null)
                        return null;

                    ksbytes = IOUtils.slurpFile(file);
                    jks = file.getName().toLowerCase().endsWith(".jks") || file.getName().toLowerCase().endsWith(".ks");
                    kstype = jks ? "JKS" : "PKCS12";
                }

                final char[] kspass = PasswordEntryDialog.promptForPassword(this, "Enter pass phrase for key store file");
                if (kspass == null)
                    return null;

                final char[] entrypass = jks ? PasswordEntryDialog.promptForPassword(this, "Enter pass phrase for key entry") : kspass;
                if (entrypass == null)
                    return null;

                final SsgKeyMetadata metadata = zone == null ? null : new SsgKeyMetadata(keystoreId, alias, zone);

                try {
                    return Registry.getDefault().getTrustedCertManager().importKeyFromKeyStoreFile(keystoreId, alias, metadata, ksbytes, kstype, kspass, entrypass, ksalias);
                } catch (MultipleAliasesException e) {
                    Object defaultOptionPaneUI = UIManager.get("OptionPaneUI");
                    UIManager.put("OptionPaneUI", ComboBoxOptionPaneUI.class.getName());  // Let JOptionPane use JCombobox rather than JList to display aliases
                    ksalias = (String) JOptionPane.showInputDialog(this, "Select alias to import", "Select Alias", JOptionPane.QUESTION_MESSAGE, null, e.getAliases(), null);
                    UIManager.put("OptionPaneUI", defaultOptionPaneUI);

                    if (ksalias == null)
                        return null;
                    return Registry.getDefault().getTrustedCertManager().importKeyFromKeyStoreFile(keystoreId, alias, metadata, ksbytes, kstype, kspass, entrypass, ksalias);
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
                if ( ExceptionUtils.getMessage( e ).toLowerCase().contains( "password" )) {
                    passwordError = true;
                }
            } catch (MultipleAliasesException e) {
                // Can't happen; we pass an alias the second time
                err = e;
            } catch (SaveException e) {
                err = e;
            }

            if ( !showImportErrorMessage(err,passwordError) ) break;
        }

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
            StringBuilder mess = new StringBuilder("<html><center>The specified private key was imported successfully.</center><center>However, at least one certificate in its chain");

            List<String> possibleProblems = new ArrayList<String>();
            if (sawExpired) possibleProblems.add("has expired");
            if (sawNotYetValid) possibleProblems.add("is not yet valid");
            if (sawDnMismatch) possibleProblems.add("has an issuer DN that does not match its issuer's DN");
            if (sawBadSignature) possibleProblems.add("has an invalid issuer signature");

            if (possibleProblems.size() == 1) {
                mess.append(" ").append(possibleProblems.get(0)).append(".</center></html>");
            } else {
                mess.append(":</center><ul>");
                for (String problem: possibleProblems) {
                    mess.append("<li>").append(problem).append("</li>");
                }
                mess.append("</ul></html>");
            }

            JOptionPane.showMessageDialog(this, mess.toString(), "Certificate Chain Warning", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void doProperties() {
        final KeyTableRow data = getSelectedObject();
        final PrivateKeyPropertiesDialog dlg = new PrivateKeyPropertiesDialog(this, data, flags, defaultAliasTracker);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            @Override
            public void run() {
                if (dlg.isDefaultKeyChanged()) {
                    defaultAliasTracker.invalidate();
                    loadPrivateKeys();
                } else if (dlg.isCertificateChainChanged() || dlg.isSecurityZoneChanged()) {
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

    private void setActiveKeypairJob(AsyncAdminMethods.JobId<X509Certificate> keypairJobId, String alias, int secondsToWaitForJobToFinish) {
        activeKeypairJob.setKeypairJobId(keypairJobId);
        activeKeypairJob.setActiveKeypairJobAlias(alias);

        int minPoll = (secondsToWaitForJobToFinish * 1000) / 30;
        if (minPoll < ActiveKeypairJob.DEFAULT_POLL_INTERVAL) minPoll = ActiveKeypairJob.DEFAULT_POLL_INTERVAL;
        activeKeypairJob.setMinJobPollInterval(minPoll);
    }

    private void setSelectedKeyEntry(Goid keystoreId, String newAlias) {
        keyTable.setSelectedKeyEntry(keystoreId, newAlias);
    }

    private void checkActiveJob () {
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        iconFlipCount.incrementAndGet();
                        if (showingInScrollPane != keyTable) {
                            loadPrivateKeys();
                        }
                        if (hasAtLeastOneMultiRoleKey && iconFlipCount.get() % TICKS_PER_ICON_REPAINT == 0) {
                            keyTable.repaint();  // TODO repaint just the affected cells, not the whole table
                        }

                    }
                });
            }
        };

        jobStatusTimer.schedule(task, TASK_DELAY, EXECUTION_PERIOD);
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
            showingInScrollPane = keypairJobViewportView;
            keyTableScrollPane.setViewportView(showingInScrollPane);
            keyTableScrollPane.getViewport().setBackground(keypairJobViewportView.getBackground());
            disableManagementButtons();
            return Collections.emptyList();
        }

        if (showingInScrollPane != keyTable) {
            showingInScrollPane = keyTable;
            keyTableScrollPane.setViewport(null);
            keyTableScrollPane.setViewportView(keyTable);
            keyTableScrollPane.getViewport().setBackground(Color.white);
        }

        try {
            hasAtLeastOneMultiRoleKey = false;
            java.util.List<KeyTableRow> keyList = new ArrayList<KeyTableRow>();
            for (KeystoreFileEntityHeader keystore : getTrustedCertAdmin().findAllKeystores(true)) {
                if (mutableKeystore == null && !keystore.isReadonly())
                    mutableKeystore = keystore;
                for (SsgKeyEntry entry : getTrustedCertAdmin().findAllKeys(keystore.getGoid(), true)) {
                    final KeyTableRow row = new KeyTableRow(keystore, entry, defaultAliasTracker.getSpecialKeyTypes(entry));
                    if (row.isMultiRoleKey())
                        hasAtLeastOneMultiRoleKey = true;
                    keyList.add(row);
                }
            }

            keyTable.setData(keyList);
            enableManagementButtons();
            enableOrDisableButtons();

            if (activeKeypairJob.getActiveKeypairJobAlias() != null && mutableKeystore != null) {
                keyTable.setSelectedKeyEntry(mutableKeystore.getGoid(), activeKeypairJob.getActiveKeypairJobAlias());
                activeKeypairJob.setActiveKeypairJobAlias(null);
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
        boolean certSelected = row != null && activeKeypairJob.getKeypairJobId() == null;
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
        boolean isActive = false;
        if (activeKeypairJob.getKeypairJobId() != null) {

            final long now = System.currentTimeMillis();
            if (now - activeKeypairJob.getLastJobPollTime() < activeKeypairJob.getMinJobPollInterval())
                return true;

            try {
                activeKeypairJob.setLastJobPollTime(now);
                String status = getTrustedCertAdmin().getJobStatus(activeKeypairJob.getKeypairJobId());
                if (status == null) {
                    activeKeypairJob.setKeypairJobId(null);
                    return false;
                } else if (status.startsWith("inactive:")) {
                    AsyncAdminMethods.JobResult<X509Certificate> result = getTrustedCertAdmin().getJobResult(activeKeypairJob.getKeypairJobId());
                    activeKeypairJob.setKeypairJobId(null);
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
                        } else if (gotmess != null && (pos = gotmess.indexOf("Strong RSA key pair generation")) >= 0) {
                            // More friendly error message for another common, foreseeable problem (Bug #9198)
                            mess = "Unable to generate key pair: " + gotmess.substring(pos);
                        } else {
                            mess = "Key generation failed: " + result.throwableClassname + ": " + gotmess;
                        }

                        logger.log(Level.WARNING, mess);
                        JOptionPane.showMessageDialog(this,
                                                      mess,
                                                      "Key Generation Failed",
                                                      JOptionPane.ERROR_MESSAGE);
                    } else if (result.result != null && mutableKeystore != null && activeKeypairJob.getActiveKeypairJobAlias() != null) {
                        keyTable.setSelectedKeyEntry(mutableKeystore.getGoid(), activeKeypairJob.getActiveKeypairJobAlias());
                        activeKeypairJob.setActiveKeypairJobAlias(null);
                    }
                    isActive = false;
                } else {
                    isActive = true;
                }
            } catch (AsyncAdminMethods.UnknownJobException e) {
                logger.log(Level.WARNING, "Unable to check remote job status: " + ExceptionUtils.getMessage(e), e);
                activeKeypairJob.setKeypairJobId(null);
                isActive = false;
            } catch ( AsyncAdminMethods.JobStillActiveException e) {
                logger.log(Level.WARNING, "Unable to check remote job status: " + ExceptionUtils.getMessage(e), e);
                isActive = true;
            }
        }

        return isActive;
    }

    /** Represents a row in the Manage Private Keys table. */
    public static class KeyTableRow {
        private final KeystoreFileEntityHeader keystoreInfo;
        private final boolean certCaCapable;
        private final EnumSet<SpecialKeyType> specialKeyTypeDesignations;
        private int specialKeyTypeIndex = 0;
        private final boolean hasRestrictedAccessDesignation;
        private SsgKeyEntry keyEntry;
        private String keyType = null;

        public KeyTableRow(KeystoreFileEntityHeader keystoreInfo, SsgKeyEntry keyEntry, EnumSet<SpecialKeyType> specialKeyTypeDesignations) {
            this.keystoreInfo = keystoreInfo;
            this.keyEntry = keyEntry;
            this.certCaCapable = keyEntry != null && CertUtils.isCertCaCapable(keyEntry.getCertificate());
            this.specialKeyTypeDesignations = specialKeyTypeDesignations;

            boolean restricted = false;
            for (SpecialKeyType type : specialKeyTypeDesignations) {
                if (type.isRestrictedAccess())
                    restricted = true;
            }
            this.hasRestrictedAccessDesignation = restricted;
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
                final X509Certificate cert = getCertificate();
                if (cert == null) {
                    return "<Bad Cert>";
                }
                PublicKey publicKey = cert.getPublicKey();
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
            final X509Certificate cert = getCertificate();
            return cert == null ? new Date(0) : cert.getNotAfter();
        }

        public EnumSet<SpecialKeyType> getSpecialKeyTypeDesignations() {
            return specialKeyTypeDesignations;
        }
        
        public Integer getCurrentSpecialKeyTypeIndex() {
            EnumSet<SpecialKeyType> specialKeyTypes = getSpecialKeyTypeDesignations();
            return specialKeyTypeIndex++ % specialKeyTypes.size();
        }

        public boolean isRestrictedAccessDesignation() {
            return hasRestrictedAccessDesignation;
        }

        public boolean isCertCaCapable() {
            return certCaCapable;
        }

        /**
         * @return true if this key has more than one special purpose role designated
         */
        public boolean isMultiRoleKey() {
            return specialKeyTypeDesignations.size() > 1;
        }

        public boolean isDesignatedAs(SpecialKeyType type) {
            return specialKeyTypeDesignations.contains(type);
        }
    }

    private static final String RESDIR = MainWindow.RESOURCE_PATH;
    private static final String PATH_SSL = RESDIR + "/cert_flag_ssl_16.png";
    private static final String PATH_CA = RESDIR + "/cert_flag_ca_16.png";
    private static final String PATH_AUDITVIEWER = RESDIR + "/cert_flag_auditviewer_16.png";
    private static final String PATH_AUDITSIGNING = RESDIR + "/cert_flag_auditsigning_16.png";
    private static final String PATH_CERT_SSL = RESDIR + "/cert_ssl_16.gif";
    private static final String PATH_CERT_CA = RESDIR + "/cert_ca_16.gif";

    private static Map<SpecialKeyType, Icon> iconsByKeyType = new HashMap<SpecialKeyType, Icon>();
    static {
        iconsByKeyType.put(SpecialKeyType.SSL, ImageCache.getInstance().getIconAsIcon(PATH_SSL));
        iconsByKeyType.put(SpecialKeyType.CA, ImageCache.getInstance().getIconAsIcon(PATH_CA));
        iconsByKeyType.put(SpecialKeyType.AUDIT_VIEWER, ImageCache.getInstance().getIconAsIcon(PATH_AUDITVIEWER));
        iconsByKeyType.put(SpecialKeyType.AUDIT_SIGNING, ImageCache.getInstance().getIconAsIcon(PATH_AUDITSIGNING));
    }

    public static Icon getIconForSpecialKeyType(SpecialKeyType type) {
        return iconsByKeyType.get(type);
    }

    private static Map<SpecialKeyType, String> labelByKeyType = new HashMap<SpecialKeyType, String>();
    static {
        for (SpecialKeyType type : SpecialKeyType.values()) {
            String label = resources.getString("specialKeyType." + type.name() + ".label");
            labelByKeyType.put(type, label);
        }
    }

    public static String getLabelForSpecialKeyType(SpecialKeyType type) {
        return labelByKeyType.get(type);
    }

    /**
     * Overrides window dispose method
     * cancel job status timer before disposing window, this will insure that the timer tasks of checking
     * the job status are not running when the window is closed
     */
    @Override
    public void dispose() {
        jobStatusTimer.cancel();
        super.dispose();
    }

    private static class KeyTable extends JTable {
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
                if (model.isConColumn(i)) col.setCellRenderer(new JTable().getDefaultRenderer(Icon.class));
            }
            setRowHeight(19);
            Utilities.setRowSorter(this, model, new int[]{1,2}, new boolean[]{true, true}, null);
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

        public void setSelectedKeyEntry(Goid keystoreId, String newAlias) {
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
                boolean isIcon;

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

            private final Col[] columns = new Col[] {
                    new Col(" ", 19, 19, 19, Object.class) {
                        @Override
                        Object getValueForRow(KeyTableRow row) {
                            return row.isCertCaCapable()
                                    ? ImageCache.getInstance().getIconAsIcon(PATH_CERT_CA)
                                    : ImageCache.getInstance().getIconAsIcon(PATH_CERT_SSL);
                        }
                        { isIcon = true; }
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

                    new Col("Zone", 60, 90, 90) {
                        @Override
                        Object getValueForRow(KeyTableRow row) {
                            final SecurityZone zone = row.getKeyEntry().getSecurityZone();
                            return zone == null ? " " : zone.getName();
                        }
                    },

                    new Col(" ", 19, 19, 19, Object.class) {
                        @Override
                        Object getValueForRow(final KeyTableRow row) {
                            final List<Icon> icons = new ArrayList<Icon>();
                            for (SpecialKeyType type : row.getSpecialKeyTypeDesignations()) {
                                Icon icon = getIconForSpecialKeyType(type);
                                if (icon != null)
                                    icons.add(icon);
                            }
                            final int numIcons = icons.size();

                            return numIcons < 1 ? "" : new Icon() {
                                final Icon firstIcon = icons.iterator().next();

                                @Override
                                public void paintIcon(Component c, Graphics g, int x, int y) {
                                    int toDisp = row.getCurrentSpecialKeyTypeIndex();
                                    Icon icon = icons.get(toDisp);
                                    if (icon != null)
                                        icon.paintIcon(c, g, x,y);
                                }

                                @Override
                                public int getIconWidth() {
                                    return firstIcon.getIconWidth();
                                }

                                @Override
                                public int getIconHeight() {
                                    return firstIcon.getIconHeight();
                                }
                            };
                        }
                        { isIcon = true; }
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

            public boolean isConColumn(int column) {
                return columns[column].isIcon;
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
            public int findRowIndex(Goid keystoreId, String newAlias) {
                KeyTableRow[] rowsArray = rows.toArray(new KeyTableRow[rows.size()]);
                for (int i = 0; i < rowsArray.length; i++) {
                    KeyTableRow row = rowsArray[i];
                    if (Goid.equals(row.getKeystore().getGoid(), keystoreId) && row.getAlias().equalsIgnoreCase(newAlias))
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