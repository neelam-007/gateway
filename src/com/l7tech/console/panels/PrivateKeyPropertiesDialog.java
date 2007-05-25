package com.l7tech.console.panels;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.security.TrustedCert;
import com.l7tech.common.security.TrustedCertAdmin;
import com.l7tech.common.security.CertificateRequest;
import com.l7tech.common.security.rbac.AttemptedDeleteSpecific;
import com.l7tech.common.security.rbac.AttemptedOperation;
import com.l7tech.common.security.rbac.EntityType;
import com.l7tech.common.security.rbac.AttemptedUpdate;
import com.l7tech.console.action.SecureAction;
import com.l7tech.console.util.Registry;
import com.l7tech.objectmodel.FindException;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.naming.ldap.LdapName;
import javax.naming.InvalidNameException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
        TrustedCertAdmin admin = getTrustedCertAdmin();
        // todo, ask user for a dn instead
        LdapName dn;
        try {
            dn = new LdapName("cn=franco,ou=macusers,o=L7");
        } catch (InvalidNameException e) {
            logger.log(Level.INFO, "not a valid ldap name", e);
            // todo, error message
            return;
        }
        CertificateRequest csr;
        try {
            csr = admin.generateCSR(subject.getKeystore().id, subject.getAlias(), dn);
        } catch (FindException e) {
            logger.log(Level.WARNING, "cannot get csr from ssg", e);
            // todo, error message
            return;
        }

        // todo, show the resulting csr, provide option to save to file
        System.out.println(csr.getEncoded());

        JOptionPane.showMessageDialog(this, "Generate Certificate Signing Request goes here");
    }

    private void assignCert() {
        // todo
        JOptionPane.showMessageDialog(this, "Replace certificate chain goes here");
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
