package com.l7tech.console.panels;

import com.l7tech.common.io.CertUtils;
import com.l7tech.console.SsmApplication;
import com.l7tech.console.action.SecureAction;
import com.l7tech.console.event.WizardAdapter;
import com.l7tech.console.event.WizardEvent;
import com.l7tech.console.util.DefaultAliasTracker;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.gateway.common.cluster.ClusterStatusAdmin;
import com.l7tech.gateway.common.security.TrustedCertAdmin;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.gateway.common.security.keystore.KeystoreFileEntityHeader;
import com.l7tech.gateway.common.security.rbac.AttemptedDeleteSpecific;
import com.l7tech.gateway.common.security.rbac.AttemptedOperation;
import com.l7tech.gateway.common.security.rbac.AttemptedUpdate;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.FileChooserUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.PasswordDoubleEntryDialog;
import com.l7tech.objectmodel.*;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.FileUtils;
import com.l7tech.util.HexUtils;

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
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class PrivateKeyPropertiesDialog extends JDialog {
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
    private JButton makeDefaultSSLButton;
    private JButton makeDefaultCAButton;
    private JLabel defaultSslLabel;
    private JLabel defaultCaLabel;
    private JLabel caCapableLabel;
    private JButton exportKeyButton;
    private PrivateKeyManagerWindow.KeyTableRow subject;

    private Logger logger = Logger.getLogger(PrivateKeyPropertiesDialog.class.getName());
    private boolean deleted = false;
    private boolean defaultKeyChanged = false;
    private final PermissionFlags flags;

    public PrivateKeyPropertiesDialog(JDialog owner, PrivateKeyManagerWindow.KeyTableRow subject, PermissionFlags flags) {
        super(owner, true);
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
            public void actionPerformed(ActionEvent actionEvent) {
                close();
            }
        });

        destroyPrivateKeyButton.addActionListener(new SecureAction(deleteOperation) {
            protected void performAction() {
                delete();
            }
        });

        replaceCertificateChainButton.addActionListener(new SecureAction(updateOperation) {
            public void performAction() {
                assignCert();
            }
        });

        generateCSRButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                getCSR();
            }
        });

        viewCertificateButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                viewCert();
            }
        });

        makeDefaultSSLButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                makeDefaultSsl();
            }
        });

        makeDefaultCAButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                makeDefaultCa();
            }
        });

        exportKeyButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                exportKey();
            }
        });

        Utilities.setEnterAction(this, new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                close();
            }
        });
        Utilities.setEscAction(this, new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                close();
            }
        });
        aliasField.setText(subject.getAlias());
        String location = subject.getKeystore().getName();
        if (subject.getKeystore().isReadonly())
            location = location + "  (Read-Only)";
        locationField.setText(location);
        typeField.setText(subject.getKeyType().toString());
        populateList();

        defaultSslLabel.setVisible(subject.isDefaultSsl());
        defaultCaLabel.setVisible(subject.isDefaultCa());
        caCapableLabel.setVisible(isCertChainCaCapable(subject));

        makeDefaultCAButton.setEnabled(!subject.isDefaultCa());
        makeDefaultSSLButton.setEnabled(!subject.isDefaultSsl());

        certList.addListSelectionListener(new ListSelectionListener() {
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
                makeDefaultCAButton,
                makeDefaultSSLButton,
                generateCSRButton,
                replaceCertificateChainButton,
                exportKeyButton,
        });
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
        public ListEntry(X509Certificate cert) {
            this.cert = cert;
        }
        public X509Certificate cert;

        public X509Certificate getCert() {
            return cert;
        }

        public String toString() {
            return cert.getSubjectDN().getName();
        }
    }

    private void populateList() {
        X509Certificate[] data = subject.getKeyEntry().getCertificateChain();
        ListEntry[] listData = new ListEntry[data.length];
        for (int i = 0; i < data.length; i++) {
            listData[i] = new ListEntry(data[i]);
        }
        certList.setListData(listData);
    }

    private void viewCert() {
        ListEntry seled = (ListEntry)certList.getSelectedValue();
        if (seled == null) {
            return;
        }
        X509Certificate cert = seled.getCert();
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
        confirmPutClusterProperty(makeDefaultSSLButton, "SSL", DefaultAliasTracker.CLUSTER_PROP_DEFAULT_SSL, subject);
    }

    private void makeDefaultCa() {
        if (!isCertChainCaCapable(subject)) {
            DialogDisplayer.showConfirmDialog(
                    makeDefaultCAButton,
                    "This certificate chain does not specifically enable use as a CA cert.\n" +
                    "Some software will reject client certificates signed by this key." +
                    "\n\nAre you sure you want the cluster to use this as the default CA private key?",
                    "Unsuitable CA Certificate",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    new DialogDisplayer.OptionListener() {
                        public void reportResult(int option) {
                            if (option == JOptionPane.YES_OPTION)
                                doMakeDefaultCa();
                        }
                    });
            return;
        }
        doMakeDefaultCa();
    }

    private void doMakeDefaultCa() {
        confirmPutClusterProperty(makeDefaultCAButton, "CA", DefaultAliasTracker.CLUSTER_PROP_DEFAULT_CA, subject);
    }

    private void confirmPutClusterProperty(final JButton triggerButton, final String what, final String clusterProp, final PrivateKeyManagerWindow.KeyTableRow subject) {
        DialogDisplayer.showConfirmDialog(
                makeDefaultCAButton,
                "Are you sure you wish to change the cluster default " + what + " private key?\n\n" +
                "All cluster nodes will need to be restarted before the change will fully take effect.",
                "Confirm New Cluster " + what + " Key",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE,
                new DialogDisplayer.OptionListener() {
                    public void reportResult(int option) {
                        if (option == JOptionPane.YES_OPTION)
                            doPutClusterProperty(triggerButton, what, clusterProp, subject);
                    }
                }
        );
    }

    private void doPutClusterProperty(final JButton triggerButton, String what, String clusterProp, PrivateKeyManagerWindow.KeyTableRow subject) {
        String value = subject.getKeyEntry().getKeystoreId() + ":" + subject.getAlias();
        String failmess = "Failed to change default " + what + " key: ";
        try {
            ClusterStatusAdmin csa = Registry.getDefault().getClusterStatusAdmin();
            ClusterProperty property = csa.findPropertyByName(clusterProp);
            if (property == null)
                property = new ClusterProperty(clusterProp, value);
            else
                property.setValue(value);
            csa.saveProperty(property);
            TopComponents.getInstance().getBean("defaultAliasTracker", DefaultAliasTracker.class).invalidate();                    

            triggerButton.setEnabled(false);
            DialogDisplayer.showMessageDialog(this,
                    "The " + what + " key has been changed.\n\nThe change will not fully take effect until all cluster nodes have been restarted.",
                    "Default " + what + " Key Updated",
                    JOptionPane.INFORMATION_MESSAGE, null);
            defaultKeyChanged = true;
            close();
        } catch (SaveException e) {
            showErrorMessage("Update Failed", failmess + ExceptionUtils.getMessage(e), e);
        } catch (UpdateException e) {
            showErrorMessage("Update Failed", failmess + ExceptionUtils.getMessage(e), e);
        } catch (DeleteException e) {
            showErrorMessage("Update Failed", failmess + ExceptionUtils.getMessage(e), e);
        } catch (FindException e) {
            showErrorMessage("Update Failed", failmess + ExceptionUtils.getMessage(e), e);
        }
    }

    private void getCSR() {
        final TrustedCertAdmin admin = getTrustedCertAdmin();
        DialogDisplayer.InputListener listener = new DialogDisplayer.InputListener() {
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
                    csr = admin.generateCSR(subject.getKeystore().getOid(), subject.getAlias(), dn.getName());
                } catch (FindException e) {
                    logger.log(Level.WARNING, "cannot get csr from ssg", e);
                    DialogDisplayer.showMessageDialog(generateCSRButton, "Error getting CSR " + e.getMessage(),
                                                      "CSR Error", JOptionPane.ERROR_MESSAGE, null);
                    return;
                }
                // save CSR to file
                SsmApplication.doWithJFileChooser(new FileChooserUtil.FileChooserUser() {
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
                                bytes = HexUtils.encodeBase64(csr).getBytes();
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
                                        "Please provide DN subject for CSR", JOptionPane.QUESTION_MESSAGE,
                                        null, null, subject.getSubjectDN(), listener);
    }

    private void assignCert() {
        final CertImportMethodsPanel sp = new CertImportMethodsPanel(
                            new CertDetailsPanel(null) {
                                public boolean canFinish() {
                                    return true;
                                }
                            }, false);

        final AddCertificateWizard w = new AddCertificateWizard(this, sp);
        w.setTitle("Assign Certificate to Private Key");
        w.addWizardListener(new WizardAdapter() {
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
                    List<SsgKeyEntry> tmp = admin.findAllKeys(subject.getKeystore().getOid());
                    for (SsgKeyEntry ske : tmp) {
                        if (ske.getAlias().equals(subject.getAlias())) {
                            subject.setKeyEntry(ske);
                            break;
                        }
                    }
                    populateList();
                } catch (GeneralSecurityException e) {
                    showErrorMessage("Error Assigning Certificate",
                            "Error Assigning new Cert. Make sure the " +
                            "cert you choose is related to the public " +
                            "key it is being assigned for.", e);
                } catch (ObjectModelException e) {
                    showErrorMessage("Error Assigning Certificate",
                            "Error Assigning new Cert. Make sure the " +
                            "cert you choose is related to the public " +
                            "key it is being assigned for.", e);
                } catch (IOException e) {
                    showErrorMessage("Error Assigning Certificate",
                            "Error Assigning new Cert. Make sure the " +
                            "cert you choose is related to the public " +
                            "key it is being assigned for.", e);
                }
            }
        });

        w.pack();
        Utilities.centerOnScreen(w);
        DialogDisplayer.display(w);
    }

    private void exportKey() {
        final SsgKeyEntry entry = subject.getKeyEntry();
        final boolean hardwareHint = "PKCS11_HARDWARE".equals(subject.getKeystore().getKeyStoreType());

        final PasswordDoubleEntryDialog passDlg = new PasswordDoubleEntryDialog(this, "Enter Export Passphrase");
        DialogDisplayer.display(passDlg, new Runnable() {
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
            public void useFileChooser(JFileChooser chooser) {
                chooser.setDialogTitle("Save As PKCS#12 File");
                chooser.setMultiSelectionEnabled(false);
                FileFilter p12Fil = FileChooserUtil.buildFilter(".p12", "(*.p12) PKCS #12 (PFX) Keystore Files");
                chooser.setFileFilter(p12Fil);

                int ret = chooser.showSaveDialog(TopComponents.getInstance().getTopParent());
                if (JFileChooser.APPROVE_OPTION == ret) {
                    try {
                        String name = chooser.getSelectedFile().getPath();
                        if (name.indexOf('.') < 0)
                            name = name + ".p12";

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
            "<html><center>This will irrevocably destroy this key and cannot be undone. " +
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
