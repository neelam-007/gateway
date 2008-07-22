package com.l7tech.console.panels;

import com.l7tech.console.event.CertListener;
import com.l7tech.console.event.CertEvent;
import com.l7tech.console.util.Registry;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.gateway.common.security.RevocationCheckPolicy;
import com.l7tech.gateway.common.security.TrustedCertAdmin;
import com.l7tech.common.io.CertUtils;
import com.l7tech.util.ResolvingComparator;
import com.l7tech.util.Resolver;
import com.l7tech.util.ArrayUtils;
import com.l7tech.policy.CertificateInfo;
import com.l7tech.objectmodel.FindException;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import javax.swing.JDialog;
import javax.swing.ListSelectionModel;
import javax.swing.JOptionPane;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.AbstractTableModel;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.BorderLayout;
import java.awt.Component;
import java.util.Collection;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Locale;
import java.util.Comparator;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.text.SimpleDateFormat;
import java.security.cert.CertificateException;

/**
 * Component for selection of Trusted Certificates.
 *
 * <p>Can be reused in JDialogs for managing a list of selected trusted
 * certificates.</p>
 *
 * <p>This can be used with a listener to veto add/remove events and for
 * notification of any errors..</p>
 *
 * @author steve
 */
public class TrustedCertsPanel extends JPanel {

    //- PUBLIC

    /**
     * Create a TrustedCertsPanel with the given options.
     *
     * @param readOnly True for a read-only dialog
     * @param maximumItems The maximum number of certificates allowed (0 for no limit)
     * @param listener The listener for TrustedCert events
     */
    public TrustedCertsPanel( final boolean readOnly,
                              final int maximumItems,
                              final TrustedCertListener listener ) {
        this( readOnly, maximumItems, listener, null );
    }

    /**
     * Create a TrustedCertsPanel with the given options.
     *
     * @param readOnly True for a read-only dialog
     * @param maximumItems The maximum number of certificates allowed (0 for no limit)
     * @param listener The listener for TrustedCert events
     * @param policies The collection of all RevocationCheckPolicys (may be null)
     */
    public TrustedCertsPanel( final boolean readOnly,
                              final int maximumItems,
                              final TrustedCertListener listener,
                              final Collection<RevocationCheckPolicy> policies ) {
        this.readOnly = readOnly;
        this.maximumItems = maximumItems <= 0 ? Integer.MAX_VALUE : maximumItems;
        this.listener = listener == null ? new TrustedCertListenerSupport(this) : listener;
        this.policies = policies;
        init();
    }

    /**
     * Set the selected certificates.
     *
     * <p>Any null CertificateInfos are ignored. Any CertificateInfos that do
     * not have corresponding trusted certificate entries are ignored.</p>
     *
     * @param certificateInfos The certificates to use.
     */
    public void setCertificateInfos( final CertificateInfo[] certificateInfos ) {
        certificates.clear();
        populateTrustedCerts( certificates, certificateInfos );  
        sortTrustedCerts();
    }

    /**
     * Get the selected certificates.
     *
     * @return The selected certificates.
     */
    public CertificateInfo[] getCertificateInfos() {
        return getTrustedSigners( certificates );
    }

    /**
     * Set the selected certificates by OIDs.
     *
     * <p>Any OIDs that do not have corresponding trusted certificate entries
     * are ignored.</p>
     *
     * @param oids The OIDs to use.
     */
    public void setCertificateOids( final long[] oids ) {
        certificates.clear();
        populateTrustedCerts( certificates, oids );
        sortTrustedCerts();
    }

    /**
     * Get the selected certificate OIDs.
     *
     * @return The selected certificates.
     */
    public long[] getCertificateOids() {
        return getTrustedSignerOids( certificates );
    }

    /**
     * Listener interface to allow veto of add/remove.
     */
    interface TrustedCertListener {
        /**
         * Notification that a certificate is being added.
         *
         * @param trustedCert The trusted certificate
         * @return True to permit the add, False to veto
         */
        boolean addTrustedCert(TrustedCert trustedCert);

        /**
         * Notification that a certificate is being removed.
         *
         * @param trustedCert The trusted certificate
         * @return True to permit the removal, False to veto
         */
        boolean removeTrustedCert(TrustedCert trustedCert);

        /**
         * Notification that an error occurred during certificate load.
         */
        void notifyError();

        /**
         * Notification that the maximim number of certificates has been reached.
         */
        void notifyMaximum();
    }

    /**
     * Support class for TrustedCertListener implementations.
     */
    public static class TrustedCertListenerSupport implements TrustedCertListener {
        private final Component component;

        /**
         * Create a new TrustedCertListenerSupport with given parent.
         *
         * @param component The component used to obtain a parent
         */
        public TrustedCertListenerSupport(final Component component) {
            this.component = component;
        }

        /**
         * This implementation permits the certificate to be added.
         *
         * @param trustedCert The trusted certificate
         * @return True
         */
        public boolean addTrustedCert(final TrustedCert trustedCert) {
            return true;
        }

        /**
         * This implementation permits the certificate to be removed.
         *
         * @param trustedCert The trusted certificate
         * @return True
         */
        public boolean removeTrustedCert(final TrustedCert trustedCert) {
            return true;
        }

        /**
         * This implementation displays an error message.
         */
        public void notifyError() {
            JOptionPane.showMessageDialog( component,
                        resources.getString(RES_ERROR_LOAD_TEXT),
                        resources.getString(RES_ERROR_LOAD_TITLE),
                        JOptionPane.ERROR_MESSAGE);
        }

        /**
         * This implementation displays a warning message.
         */
        public void notifyMaximum() {
            JOptionPane.showMessageDialog( component,
                        resources.getString(RES_ERROR_MAX_TEXT),
                        resources.getString(RES_ERROR_MAX_TITLE),
                        JOptionPane.WARNING_MESSAGE);
        }
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(TrustedCertsPanel.class.getName());

    /**
     * Resource bundle for this dialog
     */
    private static final ResourceBundle resources =
            ResourceBundle.getBundle("com.l7tech.console.resources.TrustedCertsPanel", Locale.getDefault());

    /**
     * Resource bundle keys
     */
    private static final String RES_TABLE_COLUMN_NAME = "table.column.name";
    private static final String RES_TABLE_COLUMN_ISSUEDBY = "table.column.issuedby";
    private static final String RES_TABLE_COLUMN_EXPIRES = "table.column.expires";
    private static final String RES_ERROR_LOAD_TEXT = "error.load.text";
    private static final String RES_ERROR_LOAD_TITLE = "error.load.title";
    private static final String RES_ERROR_LOADPOL_TEXT = "error.loadpol.text";
    private static final String RES_ERROR_LOADPOL_TITLE = "error.loadpol.title";
    private static final String RES_ERROR_MAX_TEXT = "error.maximum.text";
    private static final String RES_ERROR_MAX_TITLE = "error.maximum.title";

    private JTable trustedCertsTable;
    private JButton addCertButton;
    private JButton removeCertButton;
    private JButton propertiesCertButton;
    private JButton createCertButton;
    private JPanel mainPanel;

    private final boolean readOnly;
    private final int maximumItems;
    private final TrustedCertListener listener;
    private final java.util.List<TrustedCert> certificates = new ArrayList<TrustedCert>();
    private Collection<RevocationCheckPolicy> policies;
    private boolean notifiedMaximum = false;

    /**
     * Initialize the panel
     */
    private void init() {
        this.setLayout(new BorderLayout());
        this.add( mainPanel, BorderLayout.CENTER );

        addCertButton.addActionListener(new ActionListener() {
            public void actionPerformed( ActionEvent event) {
                onAdd();
            }
        });

        removeCertButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                onRemove();
            }
        });

        propertiesCertButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                onProperties();
            }
        });

        createCertButton.setAction(new NewTrustedCertificateAction(new CertListener(){
            public void certSelected( CertEvent ce) {
                addTrustedCert(ce.getCert());
                enableOrDisableControls();
            }
        }, createCertButton.getText()));

        trustedCertsTable.setModel(new TrustedCertTableModel(certificates));
        trustedCertsTable.getTableHeader().setReorderingAllowed(false);
        trustedCertsTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        trustedCertsTable.getSelectionModel().addListSelectionListener(new ListSelectionListener(){
            public void valueChanged( ListSelectionEvent e) {
                enableOrDisableControls();
            }
        });
        
        Utilities.setDoubleClickAction(trustedCertsTable, propertiesCertButton);

        enableOrDisableControls();
    }

    /**
     * Get the parent dialog for this component.
     */
    private JDialog getJDialogParent() {
        return (JDialog) SwingUtilities.getWindowAncestor( this );
    }

    /**
     * Add a trusted cert to the list
     */
    private void onAdd() {
        CertSearchPanel sp = new CertSearchPanel( getJDialogParent() );
        sp.addCertListener(new CertListener(){
            public void certSelected(CertEvent ce) {
                addTrustedCert(ce.getCert());
                enableOrDisableControls();
            }
        });
        sp.pack();
        Utilities.centerOnScreen(sp);
        DialogDisplayer.display(sp);
    }

    /**
     * Remove the selected trusted cert from the list
     */
    private void onRemove() {
        int row = trustedCertsTable.getSelectedRow();
        if (row >= 0) {
            if ( listener.removeTrustedCert( certificates.get(row) )) {
                notifiedMaximum = false;
                certificates.remove(row);
                ((AbstractTableModel) trustedCertsTable.getModel()).fireTableDataChanged();
                enableOrDisableControls();
            }
        }
    }

    /**
     * View properties of the selected trusted cert
     */
    private void onProperties() {
        int row = trustedCertsTable.getSelectedRow();
        if (row >= 0) {
            CertPropertiesWindow cpw = new CertPropertiesWindow(getJDialogParent(), certificates.get(row), false, getRevocationCheckPolicies());
            DialogDisplayer.display(cpw);
        }
    }

    /**
     * Add a trusted cert to the table.
     */
    private void addTrustedCert( final TrustedCert trustedCert ) {
        if ( !isTrusted( getTrustedSigners(certificates), trustedCert) ) {
            if ( certificates.size() < maximumItems ) {
                if ( listener.addTrustedCert( trustedCert ) ) {
                    certificates.add(trustedCert);
                    sortTrustedCerts();
                    ((AbstractTableModel) trustedCertsTable.getModel()).fireTableDataChanged();
                }
            } else {
                if ( !notifiedMaximum ) {
                    notifiedMaximum = true;
                    listener.notifyMaximum();
                }
            }
        }
    }

    /**
     * Is the given trustedCert one of the given certificateInfos
     */
    private boolean isTrusted( final CertificateInfo[] certificateInfos, final TrustedCert trustedCert ) {
        boolean trusted = false;

        for ( CertificateInfo info : certificateInfos ) {
            if ( info != null && info.getSubjectDn().equals( trustedCert.getSubjectDn() )) {
                trusted = true;
                break;
            }
        }

        return trusted;
    }   

    /**
     * Get CertificateInfos from a list of trusted certs
     */
    private CertificateInfo[] getTrustedSigners( java.util.List<TrustedCert> trustedCerts) {
        java.util.List<CertificateInfo> signers = new ArrayList<CertificateInfo>();

        for ( TrustedCert cert : trustedCerts ) {
            try {
                signers.add( new CertificateInfo(cert.getCertificate()) );
            } catch ( CertificateException ce ) {
                logger.log( Level.WARNING, "Error processing trusted certificate '"+cert.getName()+"'.", ce );
            }
        }

        return signers.toArray( new CertificateInfo[signers.size()] );
    }

    /**
     * Get cert OIDs from a list of trusted certs
     */
    private long[] getTrustedSignerOids( java.util.List<TrustedCert> trustedCerts) {
        long[] signers = new long[trustedCerts.size()];

        int index = 0;
        for ( TrustedCert cert : trustedCerts ) {
            signers[index++] = cert.getOid();
        }

        return signers;
    }

    /**
     * Sort trusted certs by name
     */
    @SuppressWarnings( { "unchecked" } )
    private void sortTrustedCerts() {
        Comparator comparator = new ResolvingComparator(new Resolver<TrustedCert, String>(){
            public String resolve(TrustedCert key) {
                String name = key.getName();
                if (name == null)
                    name = "";
                return name.toLowerCase();
            }
        }, false);

        Collections.sort(certificates, comparator);
    }

    /**
     * Get the collection of policies, loading if required. 
     */
    private Collection<RevocationCheckPolicy> getRevocationCheckPolicies() {
        if ( policies == null ) {
            try {
                policies = getTrustedCertAdmin().findAllRevocationCheckPolicies();
            } catch ( FindException fe) {
                logger.log( Level.WARNING, "Error loading revocation check policies", fe);
                JOptionPane.showMessageDialog( this,
                        resources.getString(RES_ERROR_LOADPOL_TEXT),
                        resources.getString(RES_ERROR_LOADPOL_TITLE),
                        JOptionPane.ERROR_MESSAGE);
            }
        }
        return policies;
    }

    /**
     * Populate the given list of trusted certs using the given list of CertificateInfos
     */
    private void populateTrustedCerts( java.util.List<TrustedCert> certs, CertificateInfo[] certificateInfos ) {
        if ( certificateInfos != null ) {
            try {
                TrustedCertAdmin tca = getTrustedCertAdmin();

                if ( certificateInfos.length < 10 ) {
                    for ( CertificateInfo info : certificateInfos ) {
                        if ( info != null ) {
                            TrustedCert cert = tca.findCertBySubjectDn( info.getSubjectDn() );
                            if ( cert != null ) {
                                certs.add(cert);
                            }
                        }
                    }
                } else {
                    java.util.List<TrustedCert> trustedCertificates = tca.findAllCerts();
                    for ( TrustedCert cert : trustedCertificates ) {
                        if ( isTrusted(certificateInfos, cert) ) {
                            certs.add(cert);
                        }
                    }
                }
            } catch ( FindException fe ) {
                logger.log( Level.WARNING, "Error loading certificates", fe);
                JOptionPane.showMessageDialog( this,
                        resources.getString(RES_ERROR_LOAD_TEXT),
                        resources.getString(RES_ERROR_LOAD_TITLE),
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Populate the given list of trusted certs using the given list of oids
     */
    private void populateTrustedCerts( java.util.List<TrustedCert> certs, long[] oids ) {
        if ( oids != null ) {
            try {
                TrustedCertAdmin tca = getTrustedCertAdmin();
                
                if ( oids.length < 10 ) {
                    for ( Long oid : oids ) {
                        if ( oid != null ) {
                            TrustedCert cert = tca.findCertByPrimaryKey( oid );
                            if ( cert != null ) {
                                certs.add(cert);
                            }
                        }
                    }
                } else {
                    java.util.List<TrustedCert> trustedCertificates = tca.findAllCerts();
                    for ( TrustedCert cert : trustedCertificates ) {
                        if ( ArrayUtils.contains( oids, cert.getOid() )) {
                            certs.add(cert);
                        }
                    }
                }
            } catch ( FindException fe ) {
                logger.log( Level.WARNING, "Error loading certificates", fe);
                listener.notifyError();
            }
        }
    }

    /**
     * Enable/disable controls based on permissions and selections
     */
    private void enableOrDisableControls() {
        boolean enableControls = !readOnly;

        // disable if readonly
        createCertButton.setEnabled(enableControls);
        addCertButton.setEnabled(enableControls);
        removeCertButton.setEnabled(enableControls);

        boolean contextualEnabled = false;
        int row = trustedCertsTable.getSelectedRow();
        if ( row >= 0 ) {
            contextualEnabled = true;
        }

        createCertButton.setEnabled(enableControls && trustedCertsTable.getRowCount() < maximumItems);
        addCertButton.setEnabled(enableControls && trustedCertsTable.getRowCount() < maximumItems);
        removeCertButton.setEnabled(enableControls && contextualEnabled);
        propertiesCertButton.setEnabled(contextualEnabled);
    }

    /**
     * Retrieve the object reference of the Trusted Cert Admin service
     *
     * @return TrustedCertAdmin  - The object reference.
     * @throws RuntimeException if the object reference of the Trusted Cert Admin service is not found.
     */
    private TrustedCertAdmin getTrustedCertAdmin() throws RuntimeException {
        return Registry.getDefault().getTrustedCertManager();
    }

    /**
     * TableModel for trusted certs backed by a list.
     */
    private static final class TrustedCertTableModel extends AbstractTableModel {
        private static final String[] columnNames = new String[]{
                resources.getString(RES_TABLE_COLUMN_NAME),
                resources.getString(RES_TABLE_COLUMN_ISSUEDBY),
                resources.getString(RES_TABLE_COLUMN_EXPIRES)
        };
        private final List<TrustedCert> trustedCertificates;

        private TrustedCertTableModel(List<TrustedCert> trustedCertificates) {
            this.trustedCertificates = trustedCertificates;
        }

        public int getColumnCount() {
            return 3;
        }

        public int getRowCount() {
            return trustedCertificates.size();
        }

        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            TrustedCert trustedCert = trustedCertificates.get(rowIndex);
            Object value = null;

            try {
                switch ( columnIndex ) {
                    case 0:
                        value = trustedCert.getName();
                        break;
                    case 1:
                        value = CertUtils.extractIssuerNameFromClientCertificate(trustedCert.getCertificate());
                        break;
                    case 2:
                        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
                        value = sdf.format(trustedCert.getCertificate().getNotAfter());
                        break;
                }
            } catch ( CertificateException ce) {
                value = "";
            }

            return value;
        }
    }
}
