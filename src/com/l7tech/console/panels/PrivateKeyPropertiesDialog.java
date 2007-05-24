package com.l7tech.console.panels;

import com.l7tech.common.gui.util.Utilities;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
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
        // todo, delete
        dispose();
    }

}
