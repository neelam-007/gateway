package com.l7tech.console.panels;

import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.widgets.TextListCellRenderer;
import com.l7tech.gui.widgets.CertificatePanel;
import com.l7tech.util.ResolvingComparator;
import com.l7tech.util.Resolver;
import com.l7tech.util.Functions;
import com.l7tech.util.ExceptionUtils;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.*;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.List;
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
     * @param certificateChains The certificate chains for import, must not be null.
     */
    public CertificateImportDialog( final Window parent,
                                    final Collection<X509Certificate[]> certificateChains ) {
        super( parent, bundle.getString("dialog.title"), CertificateImportDialog.DEFAULT_MODALITY_TYPE );
        setContentPane(contentPane);
        getRootPane().setDefaultButton(cancelButton);
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

        List<X509Certificate[]> certificateChainList = new ArrayList<X509Certificate[]>( certificateChains );
        Collections.sort( certificateChainList, new ResolvingComparator<X509Certificate[],String>( certificateNameResolver, false ) );
        DefaultListModel model = new DefaultListModel();
        for ( X509Certificate[] certChain : certificateChainList ) {
            model.addElement( certChain );
        }
        certificateList.setModel( model );
        certificateList.setCellRenderer( new TextListCellRenderer<Object>( new Functions.Unary<String,Object>(){
            @Override
            public String call( final Object o) {
                return certificateNameResolver.resolve((X509Certificate[])o);  
            }
        } ) );
        certificateList.addListSelectionListener( new RunOnChangeListener( new Runnable(){
            @Override
            public void run() {
                updateControlState();
            }
        } ) );

        updateControlState();
        pack();
        Utilities.setDoubleClickAction( certificateList, viewButton );
        Utilities.centerOnParentWindow(this);
        Utilities.setEscKeyStrokeDisposes(this);
    }

    /**
     * Was the dialog closed with the "OK" button.
     *
     * @return true if closed with "OK"
     */
    public boolean wasOk() {
        return wasOk;
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

    private static final ResourceBundle bundle = ResourceBundle.getBundle( CertificateImportDialog.class.getName() );
    private static final Logger logger = Logger.getLogger( CertificateImportDialog.class.getName() );

    private JPanel contentPane;
    private JButton okButton;
    private JButton cancelButton;
    private JButton viewButton;
    private JButton removeButton;
    private JList certificateList;
    private JCheckBox importAsTrustAnchorCheckBox;
    private JCheckBox importChainCheckBox;

    private boolean wasOk = false;

    private void onOK() {
        wasOk = true;
        dispose();
    }

    private void onView() {
        final Object[] values = certificateList.getSelectedValues();
        if ( values != null && values.length==1 ) {
            try {
                DialogDisplayer.showMessageDialog(this, new CertificatePanel(((X509Certificate[])values[0])[0], false), bundle.getString("certificateDetails.title"), JOptionPane.INFORMATION_MESSAGE, null);
            } catch ( CertificateEncodingException e ) {
                logger.log( Level.WARNING, "Error displaying certificate when importing '"+ ExceptionUtils.getMessage(e)+"'.", e );
                DialogDisplayer.showMessageDialog( this, "Error processing certificate for display '"+ ExceptionUtils.getMessage(e)+"'.", "Certificate Error", JOptionPane.ERROR, null );
            } catch (NoSuchAlgorithmException e) {
                logger.log( Level.WARNING, "Error displaying certificate when importing '"+ ExceptionUtils.getMessage(e)+"'.", e );
                DialogDisplayer.showMessageDialog( this, "Error processing certificate for display '"+ ExceptionUtils.getMessage(e)+"'.", "Certificate Error", JOptionPane.ERROR, null );
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
