package com.l7tech.console.panels;

import com.l7tech.common.io.CertUtils;
import com.l7tech.console.event.CertEvent;
import com.l7tech.console.event.CertListener;
import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.security.RevocationCheckPolicy;
import com.l7tech.gateway.common.security.TrustedCertAdmin;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.CertificateInfo;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.util.ArrayUtils;
import com.l7tech.util.Resolver;
import com.l7tech.util.ResolvingComparator;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.objectmodel.EntityType.TRUSTED_CERT;
import static com.l7tech.util.CollectionUtils.toList;

/**
 * Component for selection of Trusted Certificates.
 *
 * <p>Can be reused for managing a list of selected trusted certificates.</p>
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
                              @Nullable final Collection<RevocationCheckPolicy> policies ) {
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
     * Set the selected certificates by GOIDs.
     *
     * <p>Any GOIDs that do not have corresponding trusted certificate entries
     * are ignored.</p>
     *
     * @param goids The GOIDs to use.
     */
    public void setCertificateGoids(final Goid[] goids) {
        certificates.clear();
        populateTrustedCerts( certificates, goids );
        sortTrustedCerts();
    }

    /**
     * Get the selected certificate GOIDs.
     *
     * @return The selected certificates.
     */
    public Goid[] getCertificateGoids() {
        return getTrustedSignerGoids(certificates);
    }

    /**
     * Get the selected trusted certificates.
     *
     * @return The selected certificates
     */
    public List<TrustedCert> getTrustedCertificates() {
        return toList( certificates );
    }

    /**
     * Get the removed certificates.
     * @return the removed certificates.
     */
    public List<TrustedCert> getRemovedCerts() {
        return removedCerts;
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
        @Override
        public boolean addTrustedCert(final TrustedCert trustedCert) {
            return true;
        }

        /**
         * This implementation permits the certificate to be removed.
         *
         * @param trustedCert The trusted certificate
         * @return True
         */
        @Override
        public boolean removeTrustedCert(final TrustedCert trustedCert) {
            return true;
        }

        /**
         * This implementation displays an error message.
         */
        @Override
        public void notifyError() {
            JOptionPane.showMessageDialog( component,
                        resources.getString(RES_ERROR_LOAD_TEXT),
                        resources.getString(RES_ERROR_LOAD_TITLE),
                        JOptionPane.ERROR_MESSAGE);
        }

        /**
         * This implementation displays a warning message.
         */
        @Override
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
    private static final String RES_TABLE_COLUMN_THUMBPRINT = "table.column.thumbprint";
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
    private final java.util.List<TrustedCert> removedCerts = new ArrayList<TrustedCert>();
    private Collection<RevocationCheckPolicy> policies;
    private boolean notifiedMaximum = false;
    private final PermissionFlags flags = PermissionFlags.get(TRUSTED_CERT);

    /**
     * Initialize the panel
     */
    private void init() {
        this.setLayout(new BorderLayout());
        this.add( mainPanel, BorderLayout.CENTER );

        addCertButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed( ActionEvent event) {
                onAdd();
            }
        });

        removeCertButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                onRemove();
            }
        });

        propertiesCertButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                onProperties();
            }
        });

        createCertButton.setAction(new NewTrustedCertificateAction(new CertListener(){
            @Override
            public void certSelected( CertEvent ce) {
                addTrustedCert(ce.getCert());
                enableOrDisableControls();
            }
        }, createCertButton.getText()));

        trustedCertsTable.setModel(new TrustedCertTableModel(certificates));
        trustedCertsTable.getTableHeader().setReorderingAllowed(false);
        trustedCertsTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        trustedCertsTable.getSelectionModel().addListSelectionListener(new ListSelectionListener(){
            @Override
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
    private Window getWindowParent() {
        return SwingUtilities.getWindowAncestor( this );
    }

    /**
     * Add a trusted cert to the list
     */
    private void onAdd() {
        final List<TrustedCert> certs = new ArrayList<TrustedCert>();
        CertSearchPanel sp = new CertSearchPanel( getWindowParent() );
        sp.addCertListener(new CertListener(){
            @Override
            public void certSelected(CertEvent ce) {
                certs.add(ce.getCert());
            }
        });
        sp.pack();
        Utilities.centerOnScreen(sp);
        DialogDisplayer.display(sp, new Runnable() {
            @Override
            public void run() {
                int totalSize = certificates.size() + certs.size();
                if (maximumItems > 0 && totalSize > maximumItems) {
                    listener.notifyMaximum();
                } else {
                    for (TrustedCert cert: certs) {
                        addTrustedCert(cert);
                        enableOrDisableControls();
                    }
                }
            }
        });
    }

    /**
     * Remove the selected trusted cert from the list
     */
    private void onRemove() {
        int row = trustedCertsTable.getSelectedRow();
        if (row >= 0) {
            if ( listener.removeTrustedCert( certificates.get(row) )) {
                notifiedMaximum = false;
                removedCerts.add(certificates.get(row));
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
            CertPropertiesWindow cpw = new CertPropertiesWindow(getWindowParent(), certificates.get(row), false, getRevocationCheckPolicies());
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
                    removedCerts.remove(trustedCert);
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
        CertificateInfo newInfo = new CertificateInfo( trustedCert.getCertificate() );
        return ArrayUtils.contains( certificateInfos, newInfo );
    }

    /**
     * Get CertificateInfos from a list of trusted certs
     */
    private CertificateInfo[] getTrustedSigners( java.util.List<TrustedCert> trustedCerts) {
        java.util.List<CertificateInfo> signers = new ArrayList<CertificateInfo>();

        for ( TrustedCert cert : trustedCerts ) {
            signers.add( new CertificateInfo(cert.getCertificate()) );
        }

        return signers.toArray( new CertificateInfo[signers.size()] );
    }

    /**
     * Get cert OIDs from a list of trusted certs
     */
    private Goid[] getTrustedSignerGoids(java.util.List<TrustedCert> trustedCerts) {
        Goid[] signers = new Goid[trustedCerts.size()];

        int index = 0;
        for ( TrustedCert cert : trustedCerts ) {
            signers[index++] = cert.getGoid();
        }

        return signers;
    }

    /**
     * Sort trusted certs by name
     */
    @SuppressWarnings( { "unchecked" } )
    private void sortTrustedCerts() {
        Comparator comparator = new ResolvingComparator(new Resolver<TrustedCert, String>(){
            @Override
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
                            Collection<TrustedCert> certsWithDn = tca.findCertsBySubjectDn( info.getSubjectDn() );
                            if (certsWithDn != null) {
                                for (TrustedCert cert : certsWithDn) {
                                    if ( ArrayUtils.contains( certificateInfos, new CertificateInfo(cert.getCertificate()) ) ) {
                                        certs.add(cert);
                                    }
                                }
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
    private void populateTrustedCerts( java.util.List<TrustedCert> certs, Goid[] goids ) {
        if ( goids != null ) {
            try {
                TrustedCertAdmin tca = getTrustedCertAdmin();
                
                if ( goids.length < 10 ) {
                    for ( Goid goid : goids ) {
                        if ( goid != null ) {
                            TrustedCert cert = tca.findCertByPrimaryKey( goid );
                            if ( cert != null ) {
                                certs.add(cert);
                            }
                        }
                    }
                } else {
                    java.util.List<TrustedCert> trustedCertificates = tca.findAllCerts();
                    for ( TrustedCert cert : trustedCertificates ) {
                        if ( ArrayUtils.contains( goids, cert.getGoid() )) {
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


        // check if user is allowed to import/create certificates
         boolean enableCreate = flags.canCreateSome();

        // disable if readonly
        createCertButton.setEnabled(enableControls && enableCreate);
        addCertButton.setEnabled(enableControls);
        removeCertButton.setEnabled(enableControls);

        boolean contextualEnabled = false;
        int row = trustedCertsTable.getSelectedRow();
        if ( row >= 0 ) {
            contextualEnabled = true;
        }

        createCertButton.setEnabled(enableControls && enableCreate && trustedCertsTable.getRowCount() < maximumItems);
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
                resources.getString(RES_TABLE_COLUMN_EXPIRES),
                resources.getString(RES_TABLE_COLUMN_THUMBPRINT),
        };
        private final List<TrustedCert> trustedCertificates;

        private TrustedCertTableModel(List<TrustedCert> trustedCertificates) {
            this.trustedCertificates = trustedCertificates;
        }

        @Override
        public int getColumnCount() {
            return 3;
        }

        @Override
        public int getRowCount() {
            return trustedCertificates.size();
        }

        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            TrustedCert trustedCert = trustedCertificates.get(rowIndex);
            Object value = null;

            switch ( columnIndex ) {
                case 0:
                    value = trustedCert.getName();
                    break;
                case 1:
                    value = CertUtils.extractFirstIssuerNameFromCertificate(trustedCert.getCertificate());
                    break;
                case 2:
                    SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
                    value = sdf.format(trustedCert.getCertificate().getNotAfter());
                    break;
            }

            return value;
        }
    }
}
