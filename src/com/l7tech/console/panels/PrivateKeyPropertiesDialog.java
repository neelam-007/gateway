package com.l7tech.console.panels;

import com.l7tech.common.gui.util.DialogDisplayer;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.security.TrustedCert;
import com.l7tech.common.security.TrustedCertAdmin;
import com.l7tech.common.security.rbac.AttemptedDeleteSpecific;
import com.l7tech.common.security.rbac.AttemptedOperation;
import com.l7tech.common.security.rbac.AttemptedUpdate;
import com.l7tech.common.security.rbac.EntityType;
import com.l7tech.common.util.CertUtils;
import com.l7tech.console.action.SecureAction;
import com.l7tech.console.event.WizardAdapter;
import com.l7tech.console.event.WizardEvent;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.objectmodel.FindException;

import javax.security.auth.x500.X500Principal;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
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
    private PrivateKeyManagerWindow.KeyTableRow subject;

    private Logger logger = Logger.getLogger(PrivateKeyPropertiesDialog.class.getName());
    private boolean deleted = false;
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
        String location = subject.getKeystore().name;
        if (subject.getKeystore().readonly)
            location = location + "  (Read-Only)";
        locationField.setText(location);
        typeField.setText(subject.getKeyType().toString());
        populateList();

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

        TrustedCertAdmin.KeystoreInfo keystore = subject.getKeystore();
        if (keystore.readonly) {
            destroyPrivateKeyButton.setEnabled(false);
            replaceCertificateChainButton.setEnabled(false);
            generateCSRButton.setEnabled(false);
        }
        if (!flags.canDeleteSome())
            destroyPrivateKeyButton.setEnabled(false);
        if (!flags.canUpdateSome())
            replaceCertificateChainButton.setEnabled(false);
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
        try {
            X509Certificate cert = seled.getCert();
            TrustedCert tc = new TrustedCert();
            tc.setCertificate(cert);
            tc.setName(cert.getSubjectDN().toString());
            tc.setSubjectDn(cert.getSubjectDN().toString());
            CertPropertiesWindow dlg = new CertPropertiesWindow(this, tc, false, false);
            dlg.setModal(true);
            dlg.setTitle("Certificate Properties");
            Utilities.centerOnParentWindow(dlg);
            dlg.setVisible(true);
        } catch (CertificateEncodingException e) {
            logger.log(Level.WARNING, "problem reading cert", e);
        }
    }

    private TrustedCertAdmin getTrustedCertAdmin() throws RuntimeException {
        return Registry.getDefault().getTrustedCertManager();
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
                byte[] csr;
                try {
                    csr = admin.generateCSR(subject.getKeystore().id, subject.getAlias(), dn.getName());
                } catch (FindException e) {
                    logger.log(Level.WARNING, "cannot get csr from ssg", e);
                    DialogDisplayer.showMessageDialog(generateCSRButton, "Error getting CSR " + e.getMessage(),
                                                      "CSR Error", JOptionPane.ERROR_MESSAGE, null);
                    return;
                }
                // save CSR to file
                JFileChooser chooser = new JFileChooser();
                chooser.setDialogTitle("Save CSR to File");
                chooser.setMultiSelectionEnabled(false);
                chooser.setFileFilter(new FileFilter() {
                    public boolean accept(File f) {
                        return f.getAbsolutePath().endsWith(".p10") || f.getAbsolutePath().endsWith(".P10") || f.isDirectory();
                    }
                    public String getDescription() {
                        return "PKCS #10 Files";
                    }
                });

                int ret = chooser.showSaveDialog(TopComponents.getInstance().getTopParent());
                if (JFileChooser.APPROVE_OPTION == ret) {
                    String name = chooser.getSelectedFile().getPath();
                    // add extension if not present
                    if (!name.endsWith(".p10") && !name.endsWith(".P10")) {
                        name = name + ".p10";
                    }
                    // save the file
                    try {
                        FileOutputStream fos = new FileOutputStream(name);
                        fos.write(csr);
                        fos.close();
                    } catch (IOException e) {
                        logger.log(Level.WARNING, "error saving CSR", e);
                        DialogDisplayer.showMessageDialog(generateCSRButton, "Error Saving CSR " + e.getMessage(),
                                                      "Error", JOptionPane.ERROR_MESSAGE, null);
                    }
                }
            }
        };
        DialogDisplayer.showInputDialog(generateCSRButton, "CSR Subject (DN):",
                                        "Please provide DN subject for CSR", JOptionPane.QUESTION_MESSAGE,
                                        null, null, subject.getSubjectDN(), listener);
    }

    private void assignCert() {
        CertImportMethodsPanel sp = new CertImportMethodsPanel(
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

                final TrustedCert tc = (TrustedCert)o;
                final TrustedCertAdmin admin = getTrustedCertAdmin();
                try {
                    X509Certificate cert = tc.getCertificate();
                    admin.assignNewCert(subject.getKeystore().id, subject.getAlias(), new String[] { CertUtils.encodeAsPEM(cert) });
                } catch (Exception e) {
                    logger.log(Level.WARNING, "error assigning cert", e);
                    DialogDisplayer.showMessageDialog(generateCSRButton, "Error Assigning new Cert. Make sure the " +
                                                                         "cert you choose is related to the public " +
                                                                         "key it is being assigned for.",
                                                      "Error", JOptionPane.ERROR_MESSAGE, null);
                }
            }
        });

        w.pack();
        Utilities.centerOnScreen(w);
        DialogDisplayer.display(w);
    }

    private void close() {
        dispose();
    }

    private void delete() {

        final TrustedCertAdmin.KeystoreInfo keystore = subject.getKeystore();
        if (keystore.readonly) {
            JOptionPane.showMessageDialog(this, "This keystore is read-only.", "Unable to Remove Key", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        final String cancel = "Cancel";
        int option = JOptionPane.showOptionDialog(
                this,
                "Really delete private key " + subject.getAlias() + " (" + subject.getKeyEntry().getSubjectDN() + ")?\n\n" +
                "This will irrevocably destory this key, and cannot be undone.",
                "Confirm deletion",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                new Object[] { "Destroy Private Key", cancel },
                cancel);
        if (option != 0)
            return;
        deleted = true;
        dispose();
    }


    public boolean isDeleted() {
        return deleted;
    }
}
