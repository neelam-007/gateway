package com.l7tech.console.panels;

import com.l7tech.console.logging.ErrorManager;
import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.transport.ResolutionConfiguration;
import com.l7tech.gateway.common.transport.TransportAdmin;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Dialog for editing service resolution settings.
 */
public class ServiceResolutionDialog extends JDialog {

    //- PUBLIC

    public ServiceResolutionDialog( final Window parent ) {
        super( parent, resources.getString( "dialog.title" ), JDialog.DEFAULT_MODALITY_TYPE );
        permissions = PermissionFlags.get( EntityType.RESOLUTION_CONFIGURATION );
        resolutionConfiguration = loadDefaultResourceConfiguration();
        initComponents();
        modelToView( resolutionConfiguration );
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(ServiceResolutionDialog.class.getName());

    private static final ResourceBundle resources = ResourceBundle.getBundle( ServiceResolutionDialog.class.getName() );

    private static final String DEFAULT_CONFIGURATION_NAME = "Default";

    private JPanel mainPanel;
    private JButton cancelButton;
    private JButton okButton;
    private JCheckBox pathRequiredCheckBox;
    private JCheckBox pathCaseSensitiveCheckBox;
    private JCheckBox useOriginalUrlCheckBox;
    private JCheckBox useServiceOidCheckBox;
    private JCheckBox useSoapActionCheckBox;
    private JCheckBox useSoapBodyChildCheckBox;

    private PermissionFlags permissions;
    private ResolutionConfiguration resolutionConfiguration;

    private ResolutionConfiguration loadDefaultResourceConfiguration() {
        ResolutionConfiguration resolutionConfiguration = null;
        try {
            resolutionConfiguration = getTransportAdmin().getResolutionConfigurationByName( DEFAULT_CONFIGURATION_NAME );
        } catch ( FindException e ) {
            logger.log( Level.WARNING, "Error loading default resolution configuration.", e );
        }
        if ( resolutionConfiguration == null ) {
            resolutionConfiguration = new ResolutionConfiguration();
            resolutionConfiguration.setName( DEFAULT_CONFIGURATION_NAME );
        }
        return resolutionConfiguration;
    }

    private void initComponents() {
        setDefaultCloseOperation( DISPOSE_ON_CLOSE );
        setContentPane( mainPanel );

        okButton.addActionListener( new ActionListener(){
            @Override
            public void actionPerformed( final ActionEvent e ) {
                ok();
            }
        } );

        cancelButton.addActionListener( new ActionListener(){
            @Override
            public void actionPerformed( final ActionEvent e ) {
                cancel();
            }
        } );


        final boolean editable = permissions.canUpdateAll() || permissions.canCreateAll();
        okButton.setEnabled( editable );
        pathRequiredCheckBox.setEnabled( editable );
        pathCaseSensitiveCheckBox.setEnabled( editable );
        useOriginalUrlCheckBox.setEnabled( editable );
        useServiceOidCheckBox.setEnabled( editable );
        useSoapActionCheckBox.setEnabled( editable );
        useSoapBodyChildCheckBox.setEnabled( editable );

        pack();
        Utilities.setMinimumSize( this );
        Utilities.setEscKeyStrokeDisposes( this );
        Utilities.centerOnParentWindow( this );
    }

    private void ok() {
        viewToModel( resolutionConfiguration );
        try {
            getTransportAdmin().saveResolutionConfiguration( resolutionConfiguration );
        } catch ( SaveException e ) {
            ErrorManager.getDefault().notify( Level.WARNING, e, "Error saving resolution settings" );
        }
        dispose();
    }

    private void cancel() {
        dispose();
    }

    private void modelToView( final ResolutionConfiguration resolutionConfiguration ) {
        pathRequiredCheckBox.setSelected( resolutionConfiguration.isPathRequired() );
        pathCaseSensitiveCheckBox.setSelected( resolutionConfiguration.isPathCaseSensitive() );
        useOriginalUrlCheckBox.setSelected( resolutionConfiguration.isUseL7OriginalUrl() );
        useServiceOidCheckBox.setSelected( resolutionConfiguration.isUseServiceOid() );
        useSoapActionCheckBox.setSelected( resolutionConfiguration.isUseSoapAction() );
        useSoapBodyChildCheckBox.setSelected( resolutionConfiguration.isUseSoapBodyChildNamespace() );
    }

    private void viewToModel( final ResolutionConfiguration resolutionConfiguration ) {
        resolutionConfiguration.setPathRequired( pathRequiredCheckBox.isSelected() );
        resolutionConfiguration.setPathCaseSensitive( pathCaseSensitiveCheckBox.isSelected() );
        resolutionConfiguration.setUseL7OriginalUrl( useOriginalUrlCheckBox.isSelected() );
        resolutionConfiguration.setUseServiceOid( useServiceOidCheckBox.isSelected() );
        resolutionConfiguration.setUseSoapAction( useSoapActionCheckBox.isSelected() );
        resolutionConfiguration.setUseSoapBodyChildNamespace( useSoapBodyChildCheckBox.isSelected() );
    }

    private TransportAdmin getTransportAdmin() {
        final Registry registry = Registry.getDefault();
        return registry.getTransportAdmin();
    }
}
