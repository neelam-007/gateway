package com.l7tech.console.panels;

import com.l7tech.console.util.TopComponents;
import com.l7tech.gui.widgets.CertificatePanel;
import com.l7tech.gui.util.GuiPasswordCallbackHandler;
import com.l7tech.gui.util.GuiCertUtil;
import com.l7tech.gui.util.DialogDisplayer;

import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.security.cert.X509Certificate;
import java.security.PrivateKey;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Hashtable;
import java.util.Set;
import java.util.Arrays;

public class UserIdentificationRequestDialog extends JDialog {
    private static final Logger log = Logger.getLogger(LogonDialog.class.getName());

    private JPanel contentPane;
    private JButton buttonOK;
    private JButton importBtn;
    private JPanel certDetails;
    private JButton deleteBtn;
    private JComboBox certListDropDownBox;

    private X509Certificate[] cert;
    private PrivateKey privateKey;
    private String alias;

    private Hashtable<String, X509Certificate> certsHash = new Hashtable<String, X509Certificate>();


    public UserIdentificationRequestDialog(Hashtable<String, X509Certificate> certsHash) {
        setContentPane(contentPane);
        setResizable(false);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);
        setTitle("Certificate Manager");
        setMinimumSize(new Dimension(525, 300));

        try {
            //populate list of certs
            this.certsHash = certsHash;
            if (certsHash != null) {
                certListDropDownBox.removeAllItems();
                final Set<String> keys = certsHash.keySet();
                certListDropDownBox.addItem("Import certificate ...");
                certListDropDownBox.setSelectedIndex(0);
                String[] items = new String[keys.size()];
                int i=0;
                for (String key : keys) {
                    X509Certificate certificateTemp = (X509Certificate) certsHash.get(key);
                    items[i++] = (String) certificateTemp.getSubjectDN().getName();
                    //certListDropDownBox.addItem((String) certificateTemp.getSubjectDN().getName());
                }

                Arrays.sort(items);
                for (int j=0; j < items.length; j++) {
                    certListDropDownBox.addItem(items[j]);
                }
            }
            pack();
        } catch (Exception e) {
            log.finest("Failed to display certificate details.");
        }

        deleteBtn.setEnabled(cert != null && cert.length > 0);

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        importBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onImport();
            }
        });

        deleteBtn.setEnabled(false);
        deleteBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onRevoke();
            }
        });

        certListDropDownBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCertificateSelection();
            }
        });


// call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

// call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

    }

    private void onOK() {
        dispose();
    }

    private void onCancel() {
// add your code here if necessary
        dispose();
    }

    private void onImport() {
        //import certificate
        try {
            GuiCertUtil.ImportedData certData = GuiCertUtil.importCertificate(this, true, new GuiPasswordCallbackHandler());
            if (certData != null && certData.getCertificate() != null) {
                cert = certData.getCertificateChain();
                privateKey = certData.getPrivateKey();
                TopComponents.getInstance().getPreferences().importPrivateKey(cert, privateKey);

                CertificatePanel certPanel = new CertificatePanel(certData.getCertificate());
                certPanel.setCertBorderEnabled(false);
                certDetails.removeAll();
                certDetails.add(certPanel);
                deleteBtn.setEnabled(true);
                certsHash.put(certData.getCertificate().getSubjectDN().getName(), certData.getCertificate());
                //loop through drop down list to check for any duplicate
                boolean duplicateEntry = false;
                for (int i=0; i < certListDropDownBox.getItemCount(); i++) {
                    String entry = (String) certListDropDownBox.getItemAt(i);
                    if (entry.equals(certData.getCertificate().getSubjectDN().getName())) {
                        duplicateEntry = true;
                    }
                }
                if (!duplicateEntry) {
                    String[] items = new String[certListDropDownBox.getItemCount()];
                    for (int i=1; i < items.length; i++) {
                        items[i-1] = (String) certListDropDownBox.getItemAt(i);
                    }
                    items[items.length-1] = certData.getCertificate().getSubjectDN().getName();

                    Arrays.sort(items);
                    certListDropDownBox.removeAllItems();
                    certListDropDownBox.addItem("Import certificate ...");
                    for (int i=0; i < items.length; i++) {
                        certListDropDownBox.addItem(items[i]);
                    }

                    //certListDropDownBox.addItem(certData.getCertificate().getSubjectDN().getName());
                }
                certListDropDownBox.setSelectedItem(certData.getCertificate().getSubjectDN().getName());
                pack();
            }
        } catch (Exception e) {
            log.log(Level.FINEST, "Problems with certificate.", e);
        }
    }

    private void onRevoke() {
        try {
            //needs confirmation before proceeding
            DialogDisplayer.showConfirmDialog(getParent(), "Are you sure you want to delete " +
                    cert[0].getSubjectDN().getName() + "?", "Delete certificate", JOptionPane.YES_NO_OPTION, new DialogDisplayer.OptionListener() {
                public void reportResult(int option) {
                    if (JOptionPane.YES_OPTION == option) {
                        try {
                            TopComponents.getInstance().getPreferences().deleteCertificate(cert[0]);
                            certDetails.removeAll();
                            certsHash.remove(cert[0].getSubjectDN().getName());
                            certListDropDownBox.removeItem(cert[0].getSubjectDN().getName());
                            cert = null;
                            certDetails.removeAll();
                            certListDropDownBox.setSelectedIndex(0);
                            deleteBtn.setEnabled(false);
                        } catch (Exception e) {
                            log.log(Level.FINEST, "Problems deleting certificate.", e);
                        }
                    }
                }
            });
            pack();
        } catch (Exception e) {
            log.log(Level.FINEST, "Problems deleting certificate.", e);
        }
        
    }

    private void onCertificateSelection() {
        try {
            if (certListDropDownBox.getSelectedItem() != null && certListDropDownBox.getSelectedIndex() != 0) {
                X509Certificate selectedCert = certsHash.get(certListDropDownBox.getSelectedItem());
                cert = new X509Certificate[]{selectedCert};
                CertificatePanel certPanel = new CertificatePanel(selectedCert);
                certPanel.setCertBorderEnabled(false);
                certDetails.removeAll();
                certDetails.add(certPanel);
                deleteBtn.setEnabled(true);
                importBtn.setEnabled(false);
            } else {
                cert = null;
                deleteBtn.setEnabled(false);
                importBtn.setEnabled(true);
                certDetails.removeAll();
            }
            pack();
        } catch (Exception e) {
            log.log(Level.FINEST, "Problems deleting certificate.", e);
        }
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public X509Certificate[] getCert() {
        return cert;
    }
}
