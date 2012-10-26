/*
 * Copyright (C) 2004-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.panels;

import com.l7tech.common.io.CertUtils;
import com.l7tech.console.event.CertEvent;
import com.l7tech.console.event.CertListener;
import com.l7tech.console.table.TrustedCertTableSorter;
import com.l7tech.console.table.TrustedCertsTable;
import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.security.RevocationCheckPolicy;
import com.l7tech.gateway.common.security.TrustedCertAdmin;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.FindException;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.util.TextUtils;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    private Collection<RevocationCheckPolicy> revocationCheckPolicies;
    private EventListenerList listenerList = new EventListenerList();

    private final static String STARTS_WITH = "Starts with";
    private final static String EQUALS = "Equals";
    private final static String CONTAINS = "Contains";
    private final static int SEARCH_SELECTION_EQUALS_WITH = 1;
    private final static int SEARCH_SELECTION_CONTAINS_WITH = 2;

    private static ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.resources.CertificateDialog", Locale.getDefault());
    private static Logger logger = Logger.getLogger(CertSearchPanel.class.getName());

    /**
     * Constructor
     *
     * @param owner The parent window.
     */
    public CertSearchPanel(Window owner) {
        this(owner, false, false);
    }

    /**
     * Constructor
     *
     * @param owner The parent window.
     * @param keyRequired True if searching for certificates related to private keys
     */
    public CertSearchPanel(Window owner, boolean keyRequired) {
        this(owner, keyRequired, false);
    }

    /**
     * Constructor
     *
     * @param owner The parent window.
     * @param keyRequired True if searching for certificates related to private keys
     * @param singleSelection True if a single certificate is required
     */
    public CertSearchPanel(Window owner, boolean keyRequired, boolean singleSelection ) {
        super(owner, resources.getString(getPrefix(keyRequired) + "cert.search.dialog.title"), CertSearchPanel.DEFAULT_MODALITY_TYPE);
        this.keyRequired = keyRequired; 
        initialize(singleSelection);
        pack();
        Utilities.centerOnParentWindow(this);
    }

    private void initialize( boolean singleSelection ) {
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        Utilities.setEscKeyStrokeDisposes(this);

        Container p = getContentPane();
        p.setLayout(new BorderLayout());
        p.add(mainPanel, BorderLayout.CENTER);
        stopButton.setEnabled(false);
        viewButton.setEnabled(false);
        selectButton.setEnabled(false);

        descriptionLabel.setText(resources.getString(getPrefix(keyRequired) + "cert.search.dialog.description") + ":");

        subjectSearchComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { STARTS_WITH, EQUALS, CONTAINS }));
        issuerSearchComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { STARTS_WITH, EQUALS, CONTAINS }));

        if (trustedCertTable == null) {
            trustedCertTable = new TrustedCertsTable();
        }
        certScrollPane.setViewportView(trustedCertTable);
        certScrollPane.getViewport().setBackground(Color.white);

        // Hide the cert usage data column
        trustedCertTable.hideColumn(TrustedCertTableSorter.CERT_TABLE_CERT_USAGE_COLUMN_INDEX);
        if ( singleSelection ) {
            trustedCertTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);            
        } else {
            trustedCertTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        }

        trustedCertTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            /**
             * Called whenever the value of the selection changes.
             *
             * @param e the event that characterizes the change.
             */
            @Override
            public void valueChanged(ListSelectionEvent e) {

                enableOrDisableButtons();
            }
        });

        trustedCertTable.getTableSorter().addTableModelListener(new TableModelListener() {

              /**
               * This fine grain notification tells listeners the exact range
               * of cells, rows, or columns that changed.
               */
              @Override
              public void tableChanged(TableModelEvent e) {
                  if (e.getType() == TableModelEvent.INSERT) {
                      resultCounter.setText("[ " + trustedCertTable.getTableSorter().getRealModel().getRowCount() + " objects found]");
                  }
              }
          });

        searchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                cancelled = false;
                searchButton.setEnabled(false);
                stopButton.setEnabled(true);
                loadTrustedCerts();
            }
        });

        stopButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                 cancelled = true;
            }
        });

        selectButton.addActionListener(new ActionListener() {
            @Override
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
            @Override
            public void actionPerformed(ActionEvent event) {
                CertPropertiesWindow cpw;
                int row = trustedCertTable.getSelectedRow();
                if (row >= 0) {
                    try {
                        cpw = new CertPropertiesWindow(CertSearchPanel.this, (TrustedCert) trustedCertTable.getTableSorter().getData(row), false, getRevocationCheckPolicies());
                        DialogDisplayer.display(cpw);
                    } catch (FindException fe) {
                        logger.log(Level.WARNING, "Unable to load certificate data from server", fe);
                        JOptionPane.showMessageDialog(CertSearchPanel.this,
                                resources.getString("cert.load.error"),
                                resources.getString("load.error.title"), 
                                JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                dispose();
            }
        });

        Utilities.setDoubleClickAction( trustedCertTable, selectButton );
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
            @Override
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
            @Override
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

    private Collection<RevocationCheckPolicy> getRevocationCheckPolicies() throws FindException {
        Collection<RevocationCheckPolicy> policies = revocationCheckPolicies;

        if ( revocationCheckPolicies == null ) {
            policies = loadRevocationCheckPolicies();
            revocationCheckPolicies = policies;
        }

        return policies;
    }

    private Collection<RevocationCheckPolicy> loadRevocationCheckPolicies() throws FindException {
        return getTrustedCertAdmin().findAllRevocationCheckPolicies();
    }

    private java.util.List<TrustedCert> findTrustedCerts() throws FindException {
        return getTrustedCertAdmin().findAllCerts();
    }

    /**
     * Load the certs from the SSG
     */
    private java.util.List<TrustedCert> findPrivateKeyCerts() throws FindException {
        java.util.List<TrustedCert> certList = new ArrayList<TrustedCert>();
        try {
            TrustedCert sslCertHolder = new TrustedCert();
            sslCertHolder.setCertificate(getTrustedCertAdmin().getSSGSslCert());
            sslCertHolder.setName(CertUtils.extractFirstCommonNameFromCertificate(sslCertHolder.getCertificate()));
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

    private boolean textMatches(JTextComponent tc, int matchStyle, Collection<String> values) {
        String text = tc.getText().trim();
        if (text.length() < 1)
            return true;

        for (String value : values) {
            if (matchStyle == SEARCH_SELECTION_CONTAINS_WITH) {
                if (value.contains(text)) return true;
                else continue;
            } else {
                boolean fullMatch = matchStyle == SEARCH_SELECTION_EQUALS_WITH;
                if (TextUtils.matches(text, value, false, fullMatch))
                    return true;
            }
        }

        return false;
    }

    /**
     * Check if the cert should be shown or not
     * @param tc  The trusted cert
     * @return boolean TRUE if the cert should be shown, FALSE otherwise.
     */
    private boolean isShown(TrustedCert tc) {

        X509Certificate cert = tc.getCertificate();

        Collection<String> subjectNames = CertUtils.extractCommonNamesFromCertificate(cert);
        Collection<String> issuerNames = CertUtils.extractIssuerNamesFromCertificate(cert);

        return textMatches(subjectNameTextField, subjectSearchComboBox.getSelectedIndex(), subjectNames) &&
               textMatches(issuerNameTextField, issuerSearchComboBox.getSelectedIndex(), issuerNames);
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
}