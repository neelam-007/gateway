package com.l7tech.console.panels;

import com.l7tech.common.io.CertUtils;
import com.l7tech.console.SsmApplication;
import com.l7tech.console.action.SecureAction;
import com.l7tech.console.event.WizardAdapter;
import com.l7tech.console.event.WizardEvent;
import com.l7tech.console.util.ClusterPropertyCrud;
import com.l7tech.console.util.DefaultAliasTracker;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.security.TrustedCertAdmin;
import com.l7tech.gateway.common.security.keystore.KeystoreFileEntityHeader;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.gateway.common.security.rbac.AttemptedDeleteSpecific;
import com.l7tech.gateway.common.security.rbac.AttemptedOperation;
import com.l7tech.gateway.common.security.rbac.AttemptedUpdate;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.FileChooserUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.PasswordDoubleEntryDialog;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.security.cert.*;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.FileUtils;
import com.l7tech.util.SyspropUtil;

import javax.security.auth.x500.X500Principal;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class PrivateKeyPropertiesDialog extends JDialog {
    private static final String PROP_ALLOW_EC_FOR_DEFAULT_SSL = "com.l7tech.allowEcKeyForDefaultSsl";

    private JList certList;
    private JButton destroyPrivateKeyButton;
    private JButton viewCertificateButton;
    private JButton generateCSRButton;
    private JButton closeButton;
    private JButton replaceCertificateChainButton;
    private JPanel mainPanel;
    private JTextField locationField;
    private JTextField aliasField;
    private JTextField typeField;
    private JButton markAsSpecialPurposeButton;
    private JLabel defaultSslLabel;
    private JLabel defaultCaLabel;
    private JLabel auditDecryptionLabel;
    private JLabel caCapableLabel;
    private JButton exportKeyButton;
    private PrivateKeyManagerWindow.KeyTableRow subject;

    private Action makeDefaultSslAction = new AbstractAction("Make Default SSL Key") {
        @Override
        public void actionPerformed(ActionEvent e) {
            makeDefaultSsl();
        }
    };

    private Action makeDefaultCaAction = new AbstractAction("Make Default CA Key") {
        @Override
        public void actionPerformed(ActionEvent e) {
            makeDefaultCa();
        }
    };

    private Action makeAuditViewerKeyAction = new AbstractAction("Make Audit Viewer Key") {
        @Override
        public void actionPerformed(ActionEvent e) {
            makeAuditViewerKey();
        }
    };

    private Logger logger = Logger.getLogger(PrivateKeyPropertiesDialog.class.getName());
    private boolean deleted = false;
    private boolean defaultKeyChanged = false;
    private final DefaultAliasTracker defaultAliasTracker;
    private final PermissionFlags flags;

    public PrivateKeyPropertiesDialog(JDialog owner, PrivateKeyManagerWindow.KeyTableRow subject, PermissionFlags flags, DefaultAliasTracker defaultAliasTracker)
    {
        super(owner, true);
        this.defaultAliasTracker = defaultAliasTracker;
        this.subject = subject;
        this.flags = flags;
        initialize();
    }

    private void initialize() {
        setContentPane(mainPanel);
        setTitle("Private Key Properties");

        AttemptedOperation deleteOperation = new AttemptedDeleteSpecific(EntityType.SSG_KEY_ENTRY, subject.getKeyEntry());
        AttemptedOperation updateOperation = new AttemptedUpdate(EntityType.SSG_KEY_ENTRY, subject.getKeyEntry());

        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                close();
            }
        });

        destroyPrivateKeyButton.addActionListener(new SecureAction(deleteOperation) {
            @Override
            protected void performAction() {
                delete();
            }
        });

        replaceCertificateChainButton.addActionListener(new SecureAction(updateOperation) {
            @Override
            public void performAction() {
                assignCert();
            }
        });

        generateCSRButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                getCSR();
            }
        });

        viewCertificateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                viewCert();
            }
        });

        markAsSpecialPurposeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                markAsSpecialPurpose();
            }
        });

        exportKeyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                exportKey();
            }
        });

        Utilities.setEnterAction(this, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                close();
            }
        });
        Utilities.setEscAction(this, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                close();
            }
        });
        aliasField.setText(subject.getAlias());
        String location = subject.getKeystore().getName();
        if (subject.getKeystore().isReadonly())
            location = location + "  (Read-Only)";
        locationField.setText(location);
        typeField.setText(subject.getKeyType());
        populateList();

        defaultSslLabel.setVisible(subject.isDefaultSsl());
        defaultCaLabel.setVisible(subject.isDefaultCa());
        auditDecryptionLabel.setVisible(subject.isAuditViewerKey());
        caCapableLabel.setVisible(isCertChainCaCapable(subject));

        makeDefaultCaAction.setEnabled(!subject.isDefaultCa() && defaultAliasTracker.isDefaultCaKeyMutable());
        makeDefaultSslAction.setEnabled(!subject.isDefaultSsl() && defaultAliasTracker.isDefaultSslKeyMutable());
        makeAuditViewerKeyAction.setEnabled(!subject.isAuditViewerKey() && defaultAliasTracker.isDefaultAuditViewerMutable());
        markAsSpecialPurposeButton.setEnabled(makeDefaultCaAction.isEnabled() || makeDefaultSslAction.isEnabled() || makeAuditViewerKeyAction.isEnabled());
        if (!markAsSpecialPurposeButton.isEnabled())
            markAsSpecialPurposeButton.setToolTipText("Special-purpose key roles cannot be changed.");

        certList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent listSelectionEvent) {
                Object seled = certList.getSelectedValue();
                if (seled != null) {
                    viewCertificateButton.setEnabled(true);
                } else {
                    viewCertificateButton.setEnabled(false);
                }
            }
        });
        certList.setSelectedIndex(0);
        viewCertificateButton.setEnabled(true);

        KeystoreFileEntityHeader keystore = subject.getKeystore();
        if (keystore.isReadonly()) {
            destroyPrivateKeyButton.setEnabled(false);
            replaceCertificateChainButton.setEnabled(false);
            generateCSRButton.setEnabled(false);
        }

        if (!flags.canDeleteSome()) {
            destroyPrivateKeyButton.setEnabled(false);
            exportKeyButton.setEnabled(false);
            generateCSRButton.setEnabled(false);
        }
        if (!flags.canUpdateSome())
            replaceCertificateChainButton.setEnabled(false);

        Utilities.equalizeButtonSizes(new JButton[] {
                markAsSpecialPurposeButton,
                generateCSRButton,
                replaceCertificateChainButton,
                exportKeyButton,
        });
    }

    private void markAsSpecialPurpose() {
        JPopupMenu pop = new JPopupMenu();
        pop.add(new JMenuItem(makeDefaultSslAction));
        pop.add(new JMenuItem(makeDefaultCaAction));
        pop.add(new JMenuItem(makeAuditViewerKeyAction));
        pop.show(markAsSpecialPurposeButton, 0, 0);
    }

    private boolean isCertChainCaCapable(PrivateKeyManagerWindow.KeyTableRow subject) {
        X509Certificate[] chain = subject.getKeyEntry().getCertificateChain();
        int pathLen = chain.length;
        for (X509Certificate cert : chain) {
            if (!CertUtils.isCertCaCapable(cert))
                return false;
            if (pathLen > cert.getBasicConstraints())
                return false;
            pathLen--;
        }
        return true;
    }

    public boolean isDefaultKeyChanged() {
        return defaultKeyChanged;
    }

    class ListEntry {
        public ListEntry(String subjectDn, X509Certificate cert) {
            this.subjectDn = subjectDn;
            this.cert = cert;
        }
        public X509Certificate cert;
        public String subjectDn;

        public X509Certificate getCert() {
            return cert;
        }

        public String getSubjectDn() {
            return subjectDn;
        }

        @Override
        public String toString() {
            return getSubjectDn();
        }
    }

    private void populateList() {
        X509Certificate[] data = subject.getKeyEntry().getCertificateChain();
        String[] dns = subject.getKeyEntry().getCertificateChainSubjectDns();
        ListEntry[] listData = new ListEntry[data.length];
        for (int i = 0; i < data.length; i++) {
            listData[i] = new ListEntry(dns[i], data[i]);
        }
        certList.setListData(listData);
    }

    private void viewCert() {
        ListEntry seled = (ListEntry)certList.getSelectedValue();
        if (seled == null) {
            return;
        }
        X509Certificate cert = seled.getCert();
        if (cert == null) {
            DialogDisplayer.showMessageDialog(this, "The Policy Manager is unable to read or display this certificate.", "Unable to Parse Certificate", JOptionPane.ERROR_MESSAGE, null);
            return;
        }

        TrustedCert tc = new TrustedCert();
        tc.setCertificate(cert);
        tc.setName(cert.getSubjectDN().toString());
        tc.setSubjectDn(cert.getSubjectDN().toString());
        CertPropertiesWindow dlg = new CertPropertiesWindow(this, tc, false, false);
        dlg.setModal(true);
        dlg.setTitle("Certificate Properties");
        Utilities.centerOnParentWindow(dlg);
        DialogDisplayer.display(dlg);
    }

    private TrustedCertAdmin getTrustedCertAdmin() throws RuntimeException {
        return Registry.getDefault().getTrustedCertManager();
    }

    private void makeDefaultSsl() {
        if (subject.isAuditViewerKey()) {
            showAlreadyDesignatedForAuditViewingMessage();
            return;
        }

        // Check for RSA cert that disallows keyEncipherment key usage, since this can lock you out of the SSM.  (Bug #6908)
        if (subject.getKeyType().toUpperCase().startsWith("RSA") && !isCertChainSslCapable(subject)) {
            DialogDisplayer.showMessageDialog(
                    markAsSpecialPurposeButton,
                    "This key's certificate chain has a key usage disallowing use as an SSL server cert.\n" +
                    "Many SSL clients -- including the Policy Manager, and web browsers -- will refuse\n" +
                    "to connect to an SSL server that uses this key for its SSL server cert.",
                    "Unsuitable SSL Certificate",
                    JOptionPane.WARNING_MESSAGE,
                    null);
            return;
        }
        // Check for EC cert, since this can lock you out of the SSM.  (Bug #7563)
        if (subject.getKeyType().toUpperCase().startsWith("EC") && !SyspropUtil.getBoolean(PROP_ALLOW_EC_FOR_DEFAULT_SSL, false)) {
            DialogDisplayer.showConfirmDialog(
                    markAsSpecialPurposeButton,
                    "This is an elliptic curve private key.\n\n" +
                    "Many SSL clients -- including the Gateway's browser-based admin applet when run\n" +
                    "with a standard Java install, and many web browsers -- will be unable to connect\n" +
                    "to an SSL server that uses this key as its SSL server certificate.\n\n" +
                    "Are you sure you wish the cluster to use this as the default SSL private key?",
                    "Unsuitable Default SSL Key",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    new DialogDisplayer.OptionListener() {
                        @Override
                        public void reportResult(int option) {
                            if (option == JOptionPane.OK_OPTION)
                                doMakeDefaultSsl();
                        }
                    });
            return;
        }
        doMakeDefaultSsl();
    }

    private void doMakeDefaultSsl() {
        confirmPutClusterProperty("Default SSL", DefaultAliasTracker.CLUSTER_PROP_DEFAULT_SSL, subject, null);
    }

    private KeyUsagePolicy makeRsaSslServerKeyUsagePolicy() {
        HashMap<KeyUsageActivity, List<KeyUsagePermitRule>> kuPermits = new HashMap<KeyUsageActivity, List<KeyUsagePermitRule>>();
        KeyUsagePermitRule permitRule = new KeyUsagePermitRule(KeyUsageActivity.sslServerRemote, CertUtils.KEY_USAGE_BITS_BY_NAME.get("keyEncipherment"));
        kuPermits.put(KeyUsageActivity.sslServerRemote, Arrays.<KeyUsagePermitRule>asList(permitRule));
        return KeyUsagePolicy.fromRules(null, kuPermits, null);
    }

    private boolean isCertChainSslCapable(PrivateKeyManagerWindow.KeyTableRow subject) {
        try {
            return new KeyUsageChecker(makeRsaSslServerKeyUsagePolicy(), KeyUsageChecker.ENFORCEMENT_MODE_ALWAYS).permitsActivity(KeyUsageActivity.sslServerRemote, subject.getCertificate());
        } catch (CertificateParsingException e) {
            // Can't happen by this point
            //noinspection ThrowableResultOfMethodCallIgnored
            logger.log(Level.WARNING, "Unable to parse certificate: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            return false;
        }
    }

    private void makeDefaultCa() {
        if (subject.isAuditViewerKey()) {
            showAlreadyDesignatedForAuditViewingMessage();
            return;
        }

        if (!isCertChainCaCapable(subject)) {
            DialogDisplayer.showConfirmDialog(
                    markAsSpecialPurposeButton,
                    "This certificate chain does not specifically enable use as a CA cert.\n" +
                    "Some software will reject client certificates signed by this key." +
                    "\n\nAre you sure you want the cluster to use this as the default CA private key?",
                    "Unsuitable CA Certificate",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    new DialogDisplayer.OptionListener() {
                        @Override
                        public void reportResult(int option) {
                            if (option == JOptionPane.YES_OPTION)
                                doMakeDefaultCa();
                        }
                    });
            return;
        }
        doMakeDefaultCa();
    }

    private void showAlreadyDesignatedForAuditViewingMessage() {
        DialogDisplayer.showMessageDialog(
                markAsSpecialPurposeButton,
                "This key is already designated as the audit viewer private key.\n\n" +
                        "The Gateway is unable to permit the audit viewer key to be used for any other purpose.\n",
                "Key Already Designated For Audit Viewing",
                JOptionPane.WARNING_MESSAGE,
                null);
    }

    private void doMakeDefaultCa() {
        confirmPutClusterProperty("Default CA", DefaultAliasTracker.CLUSTER_PROP_DEFAULT_CA, subject, null);
    }

    private void makeAuditViewerKey() {
        if (subject.isDefaultSsl() || subject.isDefaultCa()) {
            DialogDisplayer.showMessageDialog(
                    markAsSpecialPurposeButton,
                    "This key is already designated for another special purpose and cannot be used as the audit viewer key.\n\n" +
                    "The Gateway is unable to permit the audit viewer key to be used for any other purpose.\n",
                    "Key Already Designated For Conflicting Special Purpose",
                    JOptionPane.WARNING_MESSAGE,
                    null);
            return;
        }

        if (!"RSA".equalsIgnoreCase(subject.getCertificate().getPublicKey().getAlgorithm())) {
            DialogDisplayer.showConfirmDialog(
                    markAsSpecialPurposeButton,
                    "This private key is an elliptic curve key.\n" +
                    "The Gateway currently cannot use elliptic curve keys for message-level decryption.\n" +
                    "An audit viewer policy will not be able to use this key to decrypt encrypted audit messages.\n" +
                    "\nAre you sure you want the cluster to use this as the default audit viewer private key?",
                    "Unsuitable Audit Viewer Certificate",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    new DialogDisplayer.OptionListener() {
                        @Override
                        public void reportResult(int option) {
                            if (option == JOptionPane.YES_OPTION)
                                doMakeAuditViewerKey();
                        }
                    });
            return;
        }
        doMakeAuditViewerKey();
    }

    private void doMakeAuditViewerKey() {
        confirmPutClusterProperty("Audit Viewer", DefaultAliasTracker.CLUSTER_PROP_AUDIT_VIEWER, subject,
                "The current Audit Viewer Key will become available for use elsewhere in the Gateway.\n" +
                "Delete the existing key to ensure it cannot be used to decrypt any audits encrypted for it.\n" +
                "\nThe new Audit Viewer key will no longer be available for any other usage in the Gateway.\n" +
                "Ensure this will not break any existing policies or configuration before continuing.\n\n");
    }

    private void confirmPutClusterProperty(final String what, final String clusterProp, final PrivateKeyManagerWindow.KeyTableRow subject, String extraParagraph) {
        if (extraParagraph == null)
            extraParagraph = "";
        DialogDisplayer.showSafeConfirmDialog(
                markAsSpecialPurposeButton,
                "Are you sure you wish to change the cluster " + what + " private key?\n\n" +
                        extraParagraph +
                        "All cluster nodes will need to be restarted before the change will fully take effect.",
                "Confirm New Cluster " + what + " Key",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE,
                new DialogDisplayer.OptionListener() {
                    @Override
                    public void reportResult(int option) {
                        if (option == JOptionPane.YES_OPTION)
                            doPutClusterProperty(what, clusterProp, subject);
                    }
                }
        );
    }

    private void doPutClusterProperty(String what, String clusterProp, PrivateKeyManagerWindow.KeyTableRow subject) {
        String value = subject.getKeyEntry().getKeystoreId() + ":" + subject.getAlias();
        String failmess = "Failed to change default " + what + " key: ";
        try {
            ClusterPropertyCrud.putClusterProperty(clusterProp, value);            
            defaultAliasTracker.invalidate();

            DialogDisplayer.showMessageDialog(this,
                    "The " + what + " key has been changed.\n\nThe change will not fully take effect until all cluster nodes have been restarted.",
                    "Default " + what + " Key Updated",
                    JOptionPane.INFORMATION_MESSAGE, null);
            defaultKeyChanged = true;
            close();
        } catch (ObjectModelException e) {
            showErrorMessage("Update Failed", failmess + ExceptionUtils.getMessage(e), e);
        }
    }

    private void getCSR() {
        final TrustedCertAdmin admin = getTrustedCertAdmin();
        DialogDisplayer.InputListener listener = new DialogDisplayer.InputListener() {
            @Override
            public void reportResult(Object option) {
                if (option == null)
                    return;
                String dnres = option.toString();
                X500Principal dn;
                try {
                    dn = new X500Principal(dnres);
                } catch (IllegalArgumentException e) {
                    logger.log(Level.INFO, "not a valid ldap name", e);
                    DialogDisplayer.showMessageDialog(generateCSRButton, dnres + " is not a valid DN",
                                                      "Invalid Subject", JOptionPane.ERROR_MESSAGE, null);
                    return;
                }
                final byte[] csr;
                try {
                    csr = admin.generateCSR(subject.getKeystore().getOid(), subject.getAlias(), dn, null);
                } catch (FindException e) {
                    logger.log(Level.WARNING, "cannot get csr from ssg", e);
                    DialogDisplayer.showMessageDialog(generateCSRButton, "Error getting CSR " + e.getMessage(),
                                                      "CSR Error", JOptionPane.ERROR_MESSAGE, null);
                    return;
                }
                // save CSR to file
                SsmApplication.doWithJFileChooser(new FileChooserUtil.FileChooserUser() {
                    @Override
                    public void useFileChooser(JFileChooser chooser) {
                        chooser.setDialogTitle("Save CSR to File");
                        chooser.setMultiSelectionEnabled(false);
                        FileFilter p10Filter = FileChooserUtil.buildFilter(".p10", "(*.p10) PKCS #10 Files");
                        FileFilter pemFilter = FileChooserUtil.buildFilter(".pem", "(*.pem) BASE64 PEM Files");
                        chooser.setFileFilter(p10Filter);
                        chooser.setFileFilter(pemFilter);

                        int ret = chooser.showSaveDialog(TopComponents.getInstance().getTopParent());
                        if (JFileChooser.APPROVE_OPTION == ret) {
                            String name = chooser.getSelectedFile().getPath();
                            // add an extension if not presented.
                            if (name.indexOf('.') < 0 ||
                                (!name.endsWith(".p10") && !name.endsWith(".pem"))) {
                                if (chooser.getFileFilter() == pemFilter) {
                                    name = name + ".pem";
                                } else {
                                    name = name + ".p10";
                                }
                            }

                            byte[] bytes;
                            if (chooser.getFileFilter() == pemFilter) {
                                try {
                                    bytes = CertUtils.encodeCsrAsPEM(csr).getBytes();
                                } catch (IOException e) {
                                    logger.log(Level.WARNING, "error encoding as PEM", e);
                                    DialogDisplayer.showMessageDialog(generateCSRButton, "Error Encoding As PEM " + e.getMessage(),
                                                              "Error", JOptionPane.ERROR_MESSAGE, null);
                                    return;
                                }
                            } else {
                                bytes = csr;
                            }

                            // save the file
                            try {
                                File newFile = new File(name);
                                //if file already exists, we need to ask for confirmation to overwrite. (Bug 6026)
                                if (newFile.exists()) {
                                    int result = JOptionPane.showOptionDialog(chooser, "The file '" + newFile.getName() + "' already exists.  Overwrite?",
                                                        "Warning",JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, null, null);
                                    if (result != JOptionPane.YES_OPTION)
                                        return;
                                }
                                FileUtils.save(new ByteArrayInputStream(bytes), newFile);
                            } catch (IOException e) {
                                logger.log(Level.WARNING, "error saving CSR", e);
                                DialogDisplayer.showMessageDialog(generateCSRButton, "Error Saving CSR " + e.getMessage(),
                                                              "Error", JOptionPane.ERROR_MESSAGE, null);
                            }
                        }
                    }
                });
            }
        };
        DialogDisplayer.showInputDialog(generateCSRButton, "CSR Subject (DN):",
                                        "Please provide subject DN for CSR", JOptionPane.QUESTION_MESSAGE,
                                        null, null, subject.getSubjectDN(), listener);
    }

    private void assignCert() {
        final CertImportMethodsPanel sp = new CertImportMethodsPanel(
                            new CertDetailsPanel(null) {
                                @Override
                                public boolean canFinish() {
                                    return true;
                                }
                            }, false);

        final AddCertificateWizard w = new AddCertificateWizard(this, sp);
        w.setTitle("Assign Certificate to Private Key");
        w.addWizardListener(new WizardAdapter() {
            @Override
            public void wizardFinished(WizardEvent we) {
                Object o = w.getWizardInput();

                if (o == null) return;
                if (!(o instanceof TrustedCert)) {
                    // shouldn't happen
                    throw new IllegalStateException("Wizard returned a " + o.getClass().getName() + ", was expecting a " + TrustedCert.class.getName());
                }

                final TrustedCertAdmin admin = getTrustedCertAdmin();
                try {
                    X509Certificate[] certChain = sp.getCertChain();
                    String[] pemchain = new String[certChain.length];
                    for (int i = 0; i < certChain.length; i++) {
                        pemchain[i] = CertUtils.encodeAsPEM(certChain[i]);
                    }
                    admin.assignNewCert(subject.getKeystore().getOid(), subject.getAlias(), pemchain);
                    //re-get the entry from the ssg after assigning (weird but see bzilla #3852)
                    List<SsgKeyEntry> tmp = admin.findAllKeys(subject.getKeystore().getOid(), true);
                    for (SsgKeyEntry ske : tmp) {
                        if (ske.getAlias().equalsIgnoreCase(subject.getAlias())) {
                            subject.setKeyEntry(ske);
                            break;
                        }
                    }
                    populateList();
                } catch (GeneralSecurityException e) {
                    showErrorMessage("Error Assigning Certificate",
                            "Error Assigning new Cert.  Make sure the " +
                            "cert you choose is related to the public " +
                            "key it is being assigned for.",
                            ExceptionUtils.getDebugException(e));
                } catch (ObjectModelException e) {
                    showErrorMessage("Error Assigning Certificate",
                            "Error Assigning new Cert.  Make sure the " +
                            "cert you choose is related to the public " +
                            "key it is being assigned for.",
                            ExceptionUtils.getDebugException(e));
                } catch (IOException e) {
                    showErrorMessage("Error Assigning Certificate",
                            "Error Assigning new Cert.  Make sure the " +
                            "cert you choose is related to the public " +
                            "key it is being assigned for.",
                            ExceptionUtils.getDebugException(e));
                }
            }
        });

        w.pack();
        Utilities.centerOnScreen(w);
        DialogDisplayer.display(w);
    }

    private void exportKey() {
        final SsgKeyEntry entry = subject.getKeyEntry();
        final String kstype = subject.getKeystore().getKeyStoreType();
        final boolean hardwareHint = kstype != null && kstype.contains("HARDWARE");

        final PasswordDoubleEntryDialog passDlg = new PasswordDoubleEntryDialog(this, "Enter Export Passphrase");
        DialogDisplayer.display(passDlg, new Runnable() {
            @Override
            public void run() {
                if (!passDlg.isConfirmed())
                    return;

                char[] passphrase = passDlg.getPassword();

                try {
                    byte[] p12bytes = getTrustedCertAdmin().exportKey(entry.getKeystoreId(), entry.getAlias(), entry.getAlias(), passphrase);
                    saveKeystoreBytes(p12bytes);
                } catch (UnrecoverableKeyException e) {
                    String hardwaremsg = hardwareHint ? " because it is stored in a hardware keystore" : "";
                    showErrorMessage("Unable to Export Key", "This private key cannot be exported" + hardwaremsg + ".", e);
                } catch (GeneralSecurityException e) {
                    showErrorMessage("Unable to Export Key", "Unable to export key: " + ExceptionUtils.getMessage(e), e);
                } catch (ObjectModelException e) {
                    showErrorMessage("Unable to Export Key", "Unable to export key: " + ExceptionUtils.getMessage(e), e);
                }
            }
        });
    }

    private void saveKeystoreBytes(final byte[] p12bytes) {
        FileChooserUtil.doWithJFileChooser(new FileChooserUtil.FileChooserUser() {
            @Override
            public void useFileChooser(JFileChooser chooser) {
                chooser.setDialogTitle("Save As PKCS#12 File");
                chooser.setMultiSelectionEnabled(false);
                FileFilter p12Fil = FileChooserUtil.buildFilter(".p12", "(*.p12) PKCS #12 (PFX) Keystore Files");
                chooser.setFileFilter(p12Fil);

                int ret = chooser.showSaveDialog(TopComponents.getInstance().getTopParent());
                if (JFileChooser.APPROVE_OPTION == ret) {
                    try {
                        String name = chooser.getSelectedFile().getPath();
                        if (!name.endsWith(".p12")) {
                            name = name + ".p12";
                        }

                        File newFile = new File(name);

                        //if file already exists, we need to ask for confirmation to overwrite.
                        if (newFile.exists()) {
                            int result = JOptionPane.showOptionDialog(chooser, "The file '" + newFile.getName() + "' already exists.  Overwrite?",
                                    "Warning",JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, null, null);
                            if (result != JOptionPane.YES_OPTION) {
                                return;
                            }
                        }
                        FileUtils.save(new ByteArrayInputStream(p12bytes), newFile);
                    } catch (IOException e) {
                        showErrorMessage("Unable to Save", "Unable to save PKCS#12 file: " + ExceptionUtils.getMessage(e), e);
                    }
                }
            }
        });
    }

    private void close() {
        dispose();
    }

    private void delete() {

        final KeystoreFileEntityHeader keystore = subject.getKeystore();
        if (keystore.isReadonly()) {
            JOptionPane.showMessageDialog(this, "This keystore is read-only.", "Unable to Remove Key", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String confirmationDialogTitle = "Confirm Private Key Deletion";
        String alias = subject.getAlias();
        if (alias.length() > 50) {
            alias = alias.substring(0, 49) + "...";
        }

        String subjectDn = subject.getKeyEntry().getSubjectDN();
        if (subjectDn.length() > 50) {
            subjectDn = subjectDn.substring(0,42) + "...";
        }
        String confirmationDialogMessage =
            "<html><center>This will delete this key and cannot be undone. " +
                    (subject.isAuditViewerKey() ? "Encrypted audit records will no longer be viewable. " : "") +
                    "The change will not fully take effect until all cluster nodes have been restarted.</center><p>" +
                "<center>Really delete the private key " + alias + " (" + subjectDn + ")?</center></html>";

        DialogDisplayer.showSafeConfirmDialog(
            this,
            confirmationDialogMessage,
            confirmationDialogTitle,
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.WARNING_MESSAGE,
            465, 180,
            new DialogDisplayer.OptionListener() {
                @Override
                public void reportResult(int option) {
                    if (option == JOptionPane.CANCEL_OPTION) {
                        return;
                    }
                    deleted = true;
                    dispose();
                }
            }
        );
    }

    public boolean isDeleted() {
        return deleted;
    }

    private void showErrorMessage(String title, String msg, Throwable e) {
        logger.log(Level.WARNING, msg, e);
        DialogDisplayer.showMessageDialog(this, msg, title, JOptionPane.ERROR_MESSAGE, null);
    }
}
