package com.l7tech.console.panels;

import com.l7tech.common.gui.util.DialogDisplayer;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.security.TrustedCertAdmin;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.security.cert.X509Certificate;

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

    private boolean deleted = false;

    public PrivateKeyPropertiesDialog(JDialog owner, PrivateKeyManagerWindow.KeyTableRow subject) {
        super(owner, true);
        this.subject = subject;
        initialize();
    }

    private void initialize() {
        setContentPane(mainPanel);
        setTitle("Private Key Properties");

        closeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                close();
            }
        });

        destroyPrivateKeyButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                delete();
            }
        });

        replaceCertificateChainButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
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
        locationField.setText(subject.getKeystore().name);
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
        viewCertificateButton.setEnabled(false);

        TrustedCertAdmin.KeystoreInfo keystore = subject.getKeystore();
        if (keystore.readonly) {
            destroyPrivateKeyButton.setEnabled(false);
            replaceCertificateChainButton.setEnabled(false);
        }
    }

    class ListEntry {
        public ListEntry(X509Certificate cert) {
            this.cert = cert;
        }
        public X509Certificate cert;
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
        // todo
    }

    private void getCSR() {
        // todo
    }

    private void assignCert() {
        // todo
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
        String cancel = "Cancel";
        DialogDisplayer.showOptionDialog(
                this,
                "Really delete private key " + subject.getAlias() + " (" + subject.getKeyEntry().getSubjectDN() + ")?\n\n" +
                "This will irrevocably destory this key, and cannot be undone.",
                "Confirm deletion",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                new Object[] { "Destroy Private Key", cancel},
                cancel,
                new DialogDisplayer.OptionListener() {
                    public void reportResult(int option) {
                        if (option != 0)
                            return;
                        deleted = true;
                        dispose();
                    }
                }
        );
    }


    public boolean isDeleted() {
        return deleted;
    }
}
