package com.l7tech.console.panels;

import com.l7tech.common.gui.util.TableUtil;
import com.l7tech.console.util.Preferences;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.ComponentRegistry;
import com.l7tech.identity.User;
import com.l7tech.identity.UserManager;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.identity.UserManagerClient;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class is the Certificate Info dialog
 */
class CertificatePanel extends JPanel {
    static Logger log = Logger.getLogger(CertificatePanel.class.getName());
    private X509Certificate cert;
    private UserPanel userPanel;
    private JTable certificateTable;
    private JScrollPane tableScrollPane;
    private JLabel certStatusLabel;

    /**
     * Create a new CertificatePanel
     */
    public CertificatePanel(UserPanel userPanel) {
        this.userPanel = userPanel;
        this.addHierarchyListener(hierarchyListener);
        initComponents();
    }

    /**
     * This method is called from within the constructor to
     * initialize the dialog.
     */
    private void initComponents() {
        setLayout(new GridBagLayout());
        // setTitle("Certificate Information");

        certificateTable = new JTable();
        certificateTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        certificateTable.getTableHeader().setReorderingAllowed(false);

        tableScrollPane = new JScrollPane(certificateTable);
        certStatusLabel = new JLabel();
        Font f = certStatusLabel.getFont();
        certStatusLabel.setFont(new Font(f.getName(), Font.BOLD, f.getSize()));
        add(certStatusLabel,
          new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
            GridBagConstraints.WEST,
            GridBagConstraints.NONE,
            new Insets(15, 15, 0, 15), 0, 0));

        add(tableScrollPane,
          new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0,
            GridBagConstraints.CENTER,
            GridBagConstraints.BOTH,
            new Insets(15, 15, 0, 15), 0, 0));

        // Buttons
        add(getRevokeCertButton(),
          new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
            GridBagConstraints.EAST,
            GridBagConstraints.NONE,
            new Insets(17, 12, 11, 11), 0, 0));
    }

    /**
     * create the table model with certificate fields
     *
     * @return the <code>AbstractTableModel</code> for the
     * user's certificate
     * @throws NoSuchAlgorithmException
     * @throws CertificateEncodingException
     */
    public AbstractTableModel getCertificateTableModel()
      throws NoSuchAlgorithmException, CertificateEncodingException {

        final AbstractTableModel certificateTableModel;
        certificateTableModel = new AbstractTableModel() {
            String[] cols = {"Certificate Field", "Value"};
            ArrayList data = getCertProperties();

            public String getColumnName(int col) {
                return cols[col];
            }

            public int getColumnCount() {
                return cols.length;
            }

            public int getRowCount() {
                return data.size();
            }

            public Object getValueAt(int row, int col) {
                return ((String[])data.get(row))[col];
            }
        };

        SwingUtilities.invokeLater(
          new Runnable() {
              public void run() {

                  DefaultTableCellRenderer dft = new DefaultTableCellRenderer();
                  dft.setText("Value");
                  dft.setHorizontalAlignment(SwingConstants.LEFT);
                  dft.setBackground(tableScrollPane.getBackground());
                  certificateTable.getColumn("Value").setHeaderRenderer(dft);

                  dft = new DefaultTableCellRenderer();
                  dft.setText("Certificate Field");
                  dft.setHorizontalAlignment(SwingConstants.LEFT);
                  dft.setBackground(tableScrollPane.getBackground());
                  certificateTable.getColumn("Certificate Field").setHeaderRenderer(dft);

                  int rows = certificateTableModel.getRowCount();

                  String[] cols = {"Certificate Field", "Value"};
                  for (int i = 0; i < cols.length; i++) {
                      String longest = cols[i];
                      for (int j = 0; j < rows; j++) {
                          String val = (String)certificateTableModel.getValueAt(j, i);
                          if (val.length() > longest.length()) {
                              longest = val;
                          }
                      }
                      TableUtil.adjustColumnWidth(certificateTable, i, longest);
                  }
              }
          });

        return certificateTableModel;
    }

    /** Returns a properties instance filled out with info about the certificate. */
    private ArrayList getCertProperties()
      throws CertificateEncodingException, NoSuchAlgorithmException {
        ArrayList l = new ArrayList();
        if (cert == null) return l;

        l.add(new String[]{"User", user.getName()});


        // l.add(new String[]{"Revocation date", new Date().toString()});
        l.add(new String[]{"Creation date", nullNa(cert.getNotBefore())});
        l.add(new String[]{"Expiry date", nullNa(cert.getNotAfter())});
        l.add(new String[]{"Issued to", nullNa(cert.getSubjectDN())});
        l.add(new String[]{"Serial number", nullNa(cert.getSerialNumber())});
        l.add(new String[]{"Issuer", nullNa(cert.getIssuerDN())});

        l.add(new String[]{"SHA-1 fingerprint", getCertificateFingerprint(cert, "SHA1")});
        l.add(new String[]{"MD5 fingerprint", getCertificateFingerprint(cert, "MD5")});

        return l;
    }

    /** Creates if needed the Ok button. */
    private JButton getRevokeCertButton() {
        if (revokeCertButton == null) {
            revokeCertButton = new JButton();
            revokeCertButton.setText("Revoke");
            revokeCertButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    int answer = (JOptionPane.showConfirmDialog(
                      ComponentRegistry.getInstance().getMainWindow(),
                      "<html><center><b>Please confirm certificate revoke " +
                      "for user '" + user.getName() + "'<br>This will also revoke the user's password.</b></center></html>",
                      "Revoke User Certificate",
                      JOptionPane.YES_NO_OPTION));
                    if (answer == JOptionPane.YES_OPTION) {

                        // revoke the user cert
                        try {
                            final UserManagerClient userManagerClient =
                              ((UserManagerClient)Registry.getDefault().getInternalUserManager());
                            userManagerClient.revokeCert(Long.toString(user.getOid()));
                        } catch (UpdateException e) {
                            log.log(Level.WARNING, "ERROR Revoking certificate", e);
                        }
                        // reset values and redisplay
                        cert = null;
                        loadCertificateInfo();
                    }
                }
            });
        }
        return revokeCertButton;
    }

    /**
     * load certificate info and updates the data and status of the
     * form elements
     */
    private void loadCertificateInfo() {
        try {
            AbstractTableModel
              certificateTableModel = getCertificateTableModel();
            certificateTable.setModel(certificateTableModel);
            boolean enabled = certificateTableModel.getRowCount() > 0;
            getRevokeCertButton().setEnabled(enabled);
            if (enabled) {
                certStatusLabel.setText("Certificate status: issued");
            } else {
                certStatusLabel.setText("Certificate status: not issued");
            }
        } catch (Exception e) {
            log.log(Level.WARNING, "There was an error loading the certifuc", e);
        }
    }


    // hierarchy listener
    private final HierarchyListener hierarchyListener = new HierarchyListener() {
        /** Called when the hierarchy has been changed.*/
        public void hierarchyChanged(HierarchyEvent e) {
            long flags = e.getChangeFlags();
            if ((flags & HierarchyEvent.SHOWING_CHANGED) == HierarchyEvent.SHOWING_CHANGED) {
                if (CertificatePanel.this.isShowing()) {
                    user = userPanel.getUser();
                    //getTestCertificate();
                    getUserCert();
                    CertificatePanel.this.loadCertificateInfo();
                }
            }
        }
    };


    /**
     * The method creates the fingerprint and returns it in a
     * String to the caller.
     *
     * @param cert      the certificate
     * @param algorithm the alghorithm (MD5 or SHA1)
     * @return the certificate fingerprint as a String
     * @exception NoSuchAlgorithmException
     *                      if the algorithm is not available.
     * @exception CertificateEncodingException
     *                      thrown whenever an error occurs while attempting to
     *                      encode a certificate.
     */
    private String getCertificateFingerprint(X509Certificate cert, String algorithm)
      throws NoSuchAlgorithmException, CertificateEncodingException {
        if (cert == null) {
            throw new NullPointerException("cert");
        }
        StringBuffer buff = new StringBuffer();
        byte[] fingers = cert.getEncoded();

        MessageDigest md = MessageDigest.getInstance(algorithm);
        md.update(fingers);
        byte[] digest = md.digest();
        // the algorithm
        buff.append(algorithm + ":");

        for (int i = 0; i < digest.length; i++) {
            if (i != 0) buff.append(":");
            int b = digest[i] & 0xff;
            String hex = Integer.toHexString(b);
            if (hex.length() == 1) buff.append("0");
            buff.append(hex.toUpperCase());
        }
        return buff.toString();
    }


    /**
     * obtain certificate from the application default
     * truststore.
     * This method is for testing and will be removed once the
     * cert support is implemented
     */
    private void getTestCertificate() {
        try {
            KeyStore ks = KeyStore.getInstance("JKS");
            char[] trustStorPassword = Preferences.getPreferences().getTrustStorePassword().toCharArray();
            String trustStoreFile = Preferences.getPreferences().getTrustStoreFile();
            FileInputStream ksfis = new FileInputStream(trustStoreFile);
            try {
                ks.load(ksfis, trustStorPassword);
                for (Enumeration e = ks.aliases(); e.hasMoreElements();) {
                    String alias = (String)e.nextElement();
                    Certificate c = ks.getCertificate(alias);
                    if (c != null && c instanceof X509Certificate) {
                        cert = (X509Certificate)c;
                        break;
                    }
                }
            } catch (FileNotFoundException e) {
                log.log(Level.WARNING, "Could not find application trust store", e);
            } finally {
                if (ksfis != null) ksfis.close();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void getUserCert() {
        // fla note, in the future the cert will come from some sort of CertManager instead of the interal user manager
        try {
            String uidStr = Long.toString(user.getOid());
            Registry reg = Registry.getDefault();
            UserManager um = reg.getInternalUserManager();
            UserManagerClient umc = (UserManagerClient)um;
            Certificate clientCert = umc.retrieveUserCert(uidStr);

            cert = (X509Certificate)clientCert;
        } catch (FindException e) {
            log.log(Level.WARNING, "There was an error loading the certifuc", e);
        }
    }

    /** Convert a null String into "N/A" */
    private String nullNa(String s) {
        return s == null ? "N/A" : s;
    }

    /** Convert a null object into "N/A", otherwise toString */
    private String nullNa(Object o) {
        return o == null ? "N/A" : o.toString();
    }

    /** UI Elements */
    private JButton revokeCertButton;

    /** The agent whose certificate we are inspecting. */
    private User user;

}

