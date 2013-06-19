package com.l7tech.console.panels;

import com.l7tech.console.event.CertEvent;
import com.l7tech.console.event.CertListener;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.SecurityZoneWidget;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gui.util.*;
import com.l7tech.gui.widgets.TextListCellRenderer;
import com.l7tech.gui.widgets.CertificatePanel;
import com.l7tech.objectmodel.*;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.util.ResolvingComparator;
import com.l7tech.util.Resolver;
import com.l7tech.util.Functions;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.common.io.CertUtils;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Dialog for import of X509Certificates.
 */
public class CertificateImportDialog extends JDialog {

    //- PUBLIC

    /**
     * Create an import dialog for the given certificate chains.
     *
     * @param parent The parent window (may be null)
     */
    public CertificateImportDialog( final Window parent, final CertListener listener ) {
        super( parent, bundle.getString("dialog.title"), CertificateImportDialog.DEFAULT_MODALITY_TYPE );
        this.listener = listener;

        setContentPane(contentPane);
        getRootPane().setDefaultButton(okButton);
        setDefaultCloseOperation(CertificateImportDialog.DISPOSE_ON_CLOSE);

        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        viewButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onView();
            }
        });

        removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onRemove();
            }
        });

        final Resolver<X509Certificate[],String> certificateNameResolver = new Resolver<X509Certificate[],String>(){
            @Override
            public String resolve( final X509Certificate[] key ) {
                if ( key.length > 0 )
                    return key[0].getSubjectX500Principal().getName();
                else
                    return "";
            }
        };

        DefaultListModel model = new DefaultListModel();
        certificateList.setModel( model );
        certificateList.setCellRenderer(new TextListCellRenderer<Object>(new Functions.Unary<String, Object>() {
            @Override
            public String call(final Object o) {
                return certificateNameResolver.resolve((X509Certificate[]) o);
            }
        }, new Functions.Unary<String, Object>() {
            @Override
            public String call(final Object o) {
                return getTooltip(((X509Certificate[]) o)[0]);
            }
        }, false
        ));
        certificateList.addListSelectionListener(new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                updateControlState();
            }
        }));
        zoneControl.configure(EntityType.TRUSTED_CERT, OperationType.CREATE, null);
        updateControlState();
        pack();
        Utilities.setDoubleClickAction( certificateList, viewButton );
        Utilities.centerOnParentWindow(this);
        Utilities.setEscKeyStrokeDisposes(this);

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                // get certificates to import
                final java.util.List<X509Certificate[]> certificateChains = new ArrayList<X509Certificate[]>();
                GuiCertUtil.importCertificates(getOwner(), false, new GuiPasswordCallbackHandler(getOwner()), new Functions.Unary<Boolean, GuiCertUtil.ImportedData>() {
                    @Override
                    public Boolean call(final GuiCertUtil.ImportedData importedData) {
                        certificateChains.add(importedData.getCertificateChain());
                        return true;
                    }
                });

                // If there are no certificates then a warning should already have been displayed
                if ( certificateChains.size() == 0 ) {
                    dispose();
                    return ;
                }
                List<X509Certificate[]> certificateChainList = new ArrayList<X509Certificate[]>(certificateChains);
                Collections.sort( certificateChainList, new ResolvingComparator<X509Certificate[],String>( certificateNameResolver, false ) );
                DefaultListModel thisModel =  (DefaultListModel)certificateList.getModel();
                for ( X509Certificate[] certChain : certificateChainList ) {
                    thisModel.addElement( certChain );
                }
                certificateList.setSelectedIndex(0);
            }
        });

    }

    private String getTooltip( final X509Certificate x509Certificate ) {
        StringBuilder builder = new StringBuilder();

        builder.append("<html>Issuer: ");
        builder.append(x509Certificate.getIssuerDN().getName());
        builder.append("<br/>Serial Number: ");
        builder.append(x509Certificate.getSerialNumber().toString());
        builder.append("<br/>SHA-1 Thumbprint: ");

        try {
            builder.append(CertUtils.getCertificateFingerprint(x509Certificate, "SHA1"));
        } catch (CertificateEncodingException e) {
            builder.append("&lt;Unknown>");
        } catch (NoSuchAlgorithmException e) {
            builder.append("&lt;Unknown>");
        }
        builder.append("</html>");

        return builder.toString();
    }

    /**
     * Should the certificate chain be imported or just the end entity certificate.
     *
     * @return true if the chain should be imported.
     */
    public boolean isImportChain() {
        return importChainCheckBox.isSelected();
    }

    /**
     * Should the certificate (or end cert in the chain) be imported as a trust anchor.
     *
     * @return True to import as a Trust Anchor.
     */
    public boolean isImportAsTrustAnchor() {
        return importAsTrustAnchorCheckBox.isSelected();
    }

    /**
     * Get the certificate(s) (chain)s to be imported.
     *
     * @return The certificate chains
     */
    public Collection<X509Certificate[]> getCertificateChains() {
        final ListModel model = certificateList.getModel();
        final int size = model.getSize();

        Collection<X509Certificate[]> certificateChains = new ArrayList<X509Certificate[]>(size);
        for ( int i=0; i<size; i++ ) {
            certificateChains.add( (X509Certificate[])model.getElementAt(i) );
        }

        return certificateChains;
    }

    //- PRIVATE
    private static int maxNameLength = EntityUtil.getMaxFieldLength(TrustedCert.class, "name", 128);
    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.panels.CertificateImportDialog", Locale.getDefault());
    private static final ResourceBundle bundle = ResourceBundle.getBundle( CertificateImportDialog.class.getName() );
    private static final Logger logger = Logger.getLogger( CertificateImportDialog.class.getName() );

    private final CertListener listener;

    private JPanel contentPane;
    private JButton okButton;
    private JButton cancelButton;
    private JButton viewButton;
    private JButton removeButton;
    private JList certificateList;
    private JCheckBox importAsTrustAnchorCheckBox;
    private JCheckBox importChainCheckBox;
    private SecurityZoneWidget zoneControl;


    private void onOK() {
        final JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        final CancelableOperationDialog cancelDialog =
                new CancelableOperationDialog( getOwner(),
                                               resources.getString("importing.title"),
                                               resources.getString("importing.label"),
                                               progressBar);
        cancelDialog.pack();
        cancelDialog.setModal(true);
        Utilities.centerOnParentWindow(cancelDialog);

        final Collection<String> duplicateCerts = new LinkedHashSet<String>();
        final Collection<String> errorCerts = new LinkedHashSet<String>();
        Callable<Boolean> callable = new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                final boolean importAsTrustAnchor = isImportAsTrustAnchor();
                final boolean importChain = isImportChain();
                final Collection<X509Certificate[]> certificates = getCertificateChains();
                importCertificates(importAsTrustAnchor, importChain, certificates, duplicateCerts, errorCerts );
                return Boolean.TRUE;
            }
        };

        try {
            Utilities.doWithDelayedCancelDialog( callable, cancelDialog, 500L );
        } catch (InterruptedException e) {
            logger.finer("Import operation interrupted (cancelled)");
        } catch (InvocationTargetException e) {
            // we have handled any expected exceptions elsewhere
            if ( cancelDialog.wasCancelled() ) {
                logger.log( Level.WARNING, "Error during (cancelled) certificate import.", e );
            } else {
                throw ExceptionUtils.wrap( e.getTargetException() );
            }
        }

        if ( !cancelDialog.wasCancelled() ) {
            handleImportResult( duplicateCerts, errorCerts, getOwner() );
        }


        dispose();
    }
    private void importCertificates( final boolean importAsTrustAnchor,
                                            final boolean importChain,
                                            final Collection<X509Certificate[]> certificates,
                                            final Collection<String> duplicateCerts,
                                            final Collection<String> errorCerts ) {
        final Collection<String> importedCertificateThumprints = new ArrayList<String>();
        TrustedCert cert = null;
        for ( X509Certificate[] certificateChain : certificates ) {
            if ( Thread.interrupted() ) break;

            if ( importChain ) {
                List<X509Certificate> reverseCertificateChain = Arrays.asList(certificateChain);
                Collections.reverse( reverseCertificateChain );
                boolean first = true;
                for ( X509Certificate certificate : reverseCertificateChain ) {
                    boolean isTrustAnchor = false;
                    if ( first ) {
                        isTrustAnchor =  importAsTrustAnchor;
                        first = false;
                    }
                    cert = saveCert( certificate, isTrustAnchor, duplicateCerts, errorCerts, importedCertificateThumprints );
                }
            } else {
                cert = saveCert( certificateChain[0], importAsTrustAnchor, duplicateCerts, errorCerts, importedCertificateThumprints );
            }
        }
        listener.certSelected(new CertEvent(this,cert));

    }

        private void handleImportResult( final Collection<String> duplicateCerts,
                                         final Collection<String> errorCerts,
                                         Window owner) {
            if ( !duplicateCerts.isEmpty() || !errorCerts.isEmpty() ) {
                StringBuilder errorMessage = new StringBuilder();

                errorMessage.append( resources.getString("error.header") );
                errorMessage.append( "\n\n  " );
                if ( !duplicateCerts.isEmpty() ) {
                    errorMessage.append( resources.getString("error.duplicates") );
                    for ( String certDn : duplicateCerts ) {
                        errorMessage.append( "\n" );
                        errorMessage.append( "    " );
                        errorMessage.append( certDn );
                    }
                    errorMessage.append( "\n\n  " );
                }
                if ( !errorCerts.isEmpty() ) {
                    errorMessage.append( resources.getString("error.failures") );
                    for ( String certDn : errorCerts ) {
                        errorMessage.append( "\n" );
                        errorMessage.append( "    " );
                        errorMessage.append( certDn );
                    }
                    errorMessage.append( "\n" );
                }

                final JTextArea textArea = new JTextArea();
                textArea.setEditable( false );
                textArea.setText( errorMessage.toString() );
                final JScrollPane scrollPane = new JScrollPane(textArea);
                scrollPane.setPreferredSize(new Dimension(400, 200));
                DialogDisplayer.showMessageDialog(owner, scrollPane, resources.getString("error.failures.title"), JOptionPane.WARNING_MESSAGE, new Runnable(){
                    @Override
                    public void run() {
                        // reload all certs from server
                        listener.certSelected(new CertEvent(this,null));
                    }
                });
            } else {
                // reload all certs from server
//            loadTrustedCerts();
            }
        }

        private TrustedCert saveCert( final X509Certificate certificate,
                               final boolean isTrustAnchor,
                               final Collection<String> duplicateCerts,
                               final Collection<String> errorCerts,
                               final Collection<String> importedCertificateThumprints ) {

            String thumbprint = null;
            try {
                thumbprint = CertUtils.getThumbprintSHA1(certificate);
            } catch (CertificateEncodingException e) {
                errorCerts.add( describeCertificate( certificate ) );
                logger.log( Level.WARNING, "Certificate import failed", e);
            }

            if ( thumbprint != null && !importedCertificateThumprints.contains(thumbprint) ) {
                TrustedCert cert = new TrustedCert();
                String name = CertUtils.extractFirstCommonNameFromCertificate( certificate );
                if ( name == null ) {
                    name = certificate.getSubjectX500Principal().getName().trim();
                } else {
                    name = name.trim();
                }
                cert.setName( truncName( name ) );
                cert.setCertificate( certificate );
                cert.setTrustAnchor( isTrustAnchor );
                cert.setSecurityZone(zoneControl.getSelectedZone());

                try {
                    Registry.getDefault().getTrustedCertManager().saveCert(cert);
                    importedCertificateThumprints.add( thumbprint );
                } catch ( DuplicateObjectException doe ) {
                    duplicateCerts.add( describeCertificate( certificate ) );
                } catch ( ObjectModelException e) {
                    errorCerts.add( describeCertificate( certificate ) );
                    logger.log( Level.WARNING, "Certificate import failed", e);
                } catch ( VersionException e) {
                    // doesn't happen for new certs
                    errorCerts.add( describeCertificate( certificate ) );
                    logger.log( Level.WARNING, "Certificate import failed", e);
                }
                return cert;
            }
            return null;

        }

        private String describeCertificate( X509Certificate certificate ) {
           String thumbprint;
           try {
               thumbprint = CertUtils.getCertificateFingerprint( certificate, "SHA1" );
           } catch ( CertificateEncodingException e ) {
               thumbprint = "<Unknown>";
           } catch ( NoSuchAlgorithmException e ) {
               thumbprint = "<Unknown>";
           }

           return certificate.getSubjectX500Principal().getName() + " [SHA-1 Thumbprint: "+thumbprint+"]";
       }

       private String truncName(String s) {
           return s == null || s.length() < maxNameLength ? s : s.substring(0, maxNameLength);
       }


    private void onView() {
        final Object[] values = certificateList.getSelectedValues();
        if ( values != null && values.length==1 ) {
            try {
                DialogDisplayer.showMessageDialog(this, new CertificatePanel(((X509Certificate[])values[0])[0], false), bundle.getString("certificateDetails.title"), JOptionPane.INFORMATION_MESSAGE, null);
            } catch ( CertificateEncodingException e ) {
                logger.log( Level.WARNING, "Error displaying certificate when importing '"+ ExceptionUtils.getMessage(e)+"'.", e );
                DialogDisplayer.showMessageDialog( this, MessageFormat.format(resources.getString("error.display"),ExceptionUtils.getMessage(e)), resources.getString("cert.error"), JOptionPane.ERROR, null );
            } catch (NoSuchAlgorithmException e) {
                logger.log( Level.WARNING, "Error displaying certificate when importing '"+ ExceptionUtils.getMessage(e)+"'.", e );
                DialogDisplayer.showMessageDialog( this, MessageFormat.format(resources.getString("error.display"),ExceptionUtils.getMessage(e)), resources.getString("cert.error"), JOptionPane.ERROR, null );
            }
        }
    }

    private void onRemove() {
        final Object[] values = certificateList.getSelectedValues();
        final DefaultListModel model = (DefaultListModel) certificateList.getModel();

        if ( values != null ) {
            for ( Object value : values ) {
                model.removeElement( value );            
            }
        }

        updateControlState();
    }

    private void updateControlState() {
        final Object[] values = certificateList.getSelectedValues();

        boolean enableView = false;
        boolean enableRemove = false;

        if ( values != null ) {
            if ( values.length == 1 ) {
                enableView = enableRemove = true;
            } else if ( values.length > 1 ) {
                enableRemove = true;
            }
        }

        viewButton.setEnabled( enableView );
        removeButton.setEnabled( enableRemove );
        okButton.setEnabled(certificateList.getModel().getSize() > 0);
    }

}
