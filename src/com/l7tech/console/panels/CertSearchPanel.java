/*
 * Copyright (C) 2004-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.panels;

import com.l7tech.common.gui.util.DialogDisplayer;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.security.TrustedCert;
import com.l7tech.common.security.TrustedCertAdmin;
import com.l7tech.common.util.CertUtils;
import com.l7tech.console.event.CertEvent;
import com.l7tech.console.event.CertListener;
import com.l7tech.console.table.TrustedCertTableSorter;
import com.l7tech.console.table.TrustedCertsTable;
import com.l7tech.console.util.Registry;
import com.l7tech.objectmodel.FindException;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.RemoteException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import java.io.IOException;

/**
 * @author fpang
 */
public class CertSearchPanel extends JDialog {

    private JButton searchButton;
    private JButton stopButton;
    private JButton selectButton;
    private JButton viewButton;
    private JButton cancelButton;
    private JLabel resultCounter;
    private JComboBox subjectSearchComboBox;
    private JComboBox issuerSearchComboBox;
    private JTextField subjectNameTextField;
    private JTextField issuerNameTextField;
    private final boolean keyRequired;
    private boolean cancelled;

    private JPanel mainPanel;
    private JScrollPane certScrollPane;
    private JLabel descriptionLabel;
    private TrustedCertsTable trustedCertTable = null;
    private EventListenerList listenerList = new EventListenerList();

    private final static String STARTS_WITH = "Starts with";
    private final static String EQUALS = "Equals";
    private final static int SEARCH_SELECTION_STARTS_WITH = 0;
    private final static int SEARCH_SELECTION_EQUALS = 1;

    private static ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.resources.CertificateDialog", Locale.getDefault());
    private static Logger logger = Logger.getLogger(CertSearchPanel.class.getName());

    /**
     * Constructor
     *
     * @param owner The parent component.
     */
    public CertSearchPanel(JDialog owner) {
        this(owner, false);
    }

    /**
     * Constructor
     *
     * @param owner The parent component.
     */
    public CertSearchPanel(JDialog owner, boolean keyRequired) {
        super(owner, resources.getString(getPrefix(keyRequired) + "cert.search.dialog.title"), true);
        this.keyRequired = keyRequired; 
        initialize();
        pack();
        Utilities.centerOnScreen(this);
    }

    private void initialize() {

        Container p = getContentPane();
        p.setLayout(new BorderLayout());
        p.add(mainPanel, BorderLayout.CENTER);
        stopButton.setEnabled(false);
        viewButton.setEnabled(false);
        selectButton.setEnabled(false);

        descriptionLabel.setText(resources.getString(getPrefix(keyRequired) + "cert.search.dialog.description") + ":");

        subjectSearchComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { STARTS_WITH, EQUALS }));
        issuerSearchComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { STARTS_WITH, EQUALS }));

        if (trustedCertTable == null) {
            trustedCertTable = new TrustedCertsTable();
        }
        certScrollPane.setViewportView(trustedCertTable);
        certScrollPane.getViewport().setBackground(Color.white);

        // Hide the cert usage data column
        trustedCertTable.hideColumn(TrustedCertTableSorter.CERT_TABLE_CERT_USAGE_COLUMN_INDEX);
        trustedCertTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        trustedCertTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            /**
             * Called whenever the value of the selection changes.
             *
             * @param e the event that characterizes the change.
             */
            public void valueChanged(ListSelectionEvent e) {

                enableOrDisableButtons();
            }
        });

        trustedCertTable.getTableSorter().addTableModelListener(new TableModelListener() {

              /**
               * This fine grain notification tells listeners the exact range
               * of cells, rows, or columns that changed.
               */
              public void tableChanged(TableModelEvent e) {
                  if (e.getType() == TableModelEvent.INSERT) {
                      resultCounter.setText("[ " + trustedCertTable.getTableSorter().getRealModel().getRowCount() + " objects found]");
                  }
              }
          });

        searchButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                cancelled = false;
                searchButton.setEnabled(false);
                stopButton.setEnabled(true);
                loadTrustedCerts();
            }
        });

        stopButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                 cancelled = true;
            }
        });

        selectButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                int row[] = trustedCertTable.getSelectedRows();
                if (row.length > 0) {
                    TrustedCert[] certs = new TrustedCert[row.length];
                    for (int i = 0; i < row.length; i++) {
                        certs[i]  = (TrustedCert) trustedCertTable.getTableSorter().getData(row[i]);
                    }
                    fireEventCertSelected(certs);
                }

                dispose();
            }

        });

        viewButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                CertPropertiesWindow cpw;
                int row = trustedCertTable.getSelectedRow();
                if (row >= 0) {
                    cpw = new CertPropertiesWindow(CertSearchPanel.this, (TrustedCert) trustedCertTable.getTableSorter().getData(row), false);
                    DialogDisplayer.display(cpw);
                }
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {

                dispose();
            }
        });

    }

    /**
     * Enable or disable the fields based on the current selections.
     */
    private void enableOrDisableButtons() {
        boolean viewEnabled = false;
        boolean selectEnabled = false;

        if (trustedCertTable.getSelectedRowCount() == 1) {
            viewEnabled = true;
        }

        if (trustedCertTable.getSelectedRowCount() > 0) {
            selectEnabled = true;
        }

        viewButton.setEnabled(viewEnabled);
        selectButton.setEnabled(selectEnabled);
    }

    /**
     * add the CertListener
     *
     * @param listener the CertListener
     */
    public void addCertListener(CertListener listener) {
        listenerList.add(CertListener.class, listener);
    }

    /**
     * remove the the CertListener
     *
     * @param listener the CertListener
     */
    public void removeCertListener(CertListener listener) {
        listenerList.remove(CertListener.class, listener);
    }

    /**
     * notfy the listeners
     *
     * @param certs the trusted certs
     */
    private void fireEventCertSelected(final TrustedCert[] certs) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                for (TrustedCert tc : certs) {
                    CertEvent event = new CertEvent(this, tc);
                    EventListener[] listeners = listenerList.getListeners(CertListener.class);
                    for (EventListener listener : listeners) {
                        ((CertListener) listener).certSelected(event);
                    }
                }
            }
        });
    }

    /**
     * Load the certs from the database
     */
    private void loadTrustedCerts() {

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {

                java.util.List<TrustedCert> certList;
                try {
                    certList = keyRequired ?
                            findPrivateKeyCerts() :
                            findTrustedCerts();

                    // clear the table
                    trustedCertTable.getTableSorter().setData(new ArrayList<TrustedCert>());
                    resultCounter.setText("[ " + trustedCertTable.getTableSorter().getRealModel().getRowCount() + " objects found]");
                    
                    //Vector certs = new Vector();
                    for (int i = 0; i < certList.size() && !cancelled ; i++) {
                        TrustedCert tc = certList.get(i);
                        if (isShown(tc)) {
                            trustedCertTable.getTableSorter().addRow(tc);
                        }
                    }

                } catch (RemoteException re) {
                    JOptionPane.showMessageDialog(CertSearchPanel.this, resources.getString("cert.remote.exception"),
                            resources.getString("load.error.title"),
                            JOptionPane.ERROR_MESSAGE);
                } catch (FindException e) {
                    JOptionPane.showMessageDialog(CertSearchPanel.this, resources.getString("cert.find.error"),
                            resources.getString("load.error.title"),
                            JOptionPane.ERROR_MESSAGE);
                }
                
                stopButton.setEnabled(false);
                searchButton.setEnabled(true);
            }
        });
    }

    private java.util.List<TrustedCert> findTrustedCerts() throws RemoteException, FindException {
        return getTrustedCertAdmin().findAllCerts();
    }

    /**
     * Load the certs from the SSG
     */
    private java.util.List<TrustedCert> findPrivateKeyCerts() throws RemoteException, FindException {
        java.util.List<TrustedCert> certList = new ArrayList();
        try {
            TrustedCert sslCertHolder = new TrustedCert();
            sslCertHolder.setCertificate(getTrustedCertAdmin().getSSGSslCert());
            sslCertHolder.setName(CertUtils.getCn(sslCertHolder.getCertificate()));
            sslCertHolder.setSubjectDn(sslCertHolder.getCertificate().getSubjectDN().toString());
            certList.add(sslCertHolder);
        }
        catch(IOException ioe) {
            throw new FindException("Error accessing Private Key information.", ioe);
        }
        catch(CertificateException ce) {
            throw new FindException("Error accessing Private Key information.", ce);
        }
        return certList;
    }

    /**
     * Check if the cert should be shown or not
     * @param tc  The trusted cert
     * @return boolean TRUE if the cert should be shown, FALSE otherwise.
     */
    private boolean isShown(TrustedCert tc) {

        X509Certificate cert;
        try {
            cert = tc.getCertificate();
        } catch (CertificateException e) {
            logger.warning(resources.getString("cert.decode.error"));
            return false;
        }

        String subjectName = CertUtils.extractCommonNameFromClientCertificate(cert);
        String issuerName = CertUtils.extractIssuerNameFromClientCertificate(cert);

        boolean show1 = true;
        boolean show2 = true;
        if (subjectNameTextField.getText().trim().length() > 0) {
            if (subjectSearchComboBox.getSelectedIndex() == SEARCH_SELECTION_STARTS_WITH) {
                if(!subjectName.startsWith(subjectNameTextField.getText().trim())) {
                     show1 = false;
                }
            } else if (subjectSearchComboBox.getSelectedIndex() == SEARCH_SELECTION_EQUALS) {
                if(!subjectName.equals(subjectNameTextField.getText().trim())) {
                     show1 = false;
                }
            }
        }

        if (issuerNameTextField.getText().trim().length() > 0) {
            if (issuerSearchComboBox.getSelectedIndex() == SEARCH_SELECTION_STARTS_WITH) {
                if(!issuerName.startsWith(issuerNameTextField.getText().trim())) {
                     show2 = false;
                }
            } else if (issuerSearchComboBox.getSelectedIndex() == SEARCH_SELECTION_EQUALS) {
                 if(!issuerName.equals(issuerNameTextField.getText().trim())) {
                     show2 = false;
                }
            }
        }

        return (show1 && show2);
    }

    /**
     * Retrieve the object reference of the Trusted Cert Admin service
     *
     * @return TrustedCertAdmin  - The object reference.
     * @throws RuntimeException  if the object reference of the Trusted Cert Admin service is not found.
     */
    private TrustedCertAdmin getTrustedCertAdmin() throws RuntimeException {
        return Registry.getDefault().getTrustedCertManager();
    }

    /**
     *
     */
    private static String getPrefix(boolean key) {
        return key ?  "keyed" : "";
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
    }
}
