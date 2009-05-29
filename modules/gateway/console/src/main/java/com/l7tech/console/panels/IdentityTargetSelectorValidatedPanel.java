package com.l7tech.console.panels;

import com.l7tech.gui.widgets.ValidatedPanel;
import com.l7tech.gui.widgets.TextListCellRenderer;
import com.l7tech.policy.assertion.IdentityTargetable;
import com.l7tech.policy.assertion.IdentityTarget;
import com.l7tech.util.Functions;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.admin.IdentityAdmin;
import com.l7tech.identity.IdentityProviderConfig;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Panel for identity selection.
 */
public class IdentityTargetSelectorValidatedPanel extends ValidatedPanel<IdentityTarget> {

    //- PUBLIC

    public IdentityTargetSelectorValidatedPanel( final IdentityTargetable identityTargetable,
                                                 final IdentityTarget[] identityTargetOptions ) {
        this.identityTargetable = identityTargetable;
        this.identityTargetOptions = identityTargetOptions;
        init();
    }

    //- PROTECTED

    @Override
    protected void doUpdateModel() {
        IdentityTarget identityTarget = (IdentityTarget) identityComboBox.getSelectedItem();
        if ( new IdentityTarget().equals(identityTarget) ) {
            identityTargetable.setIdentityTarget( null );
        } else {
            identityTargetable.setIdentityTarget( identityTarget );
        }
    }

    @Override
    public void focusFirstComponent() {
        identityComboBox.requestFocus();
    }

    @Override
    protected IdentityTarget getModel() {
        return (IdentityTarget) identityComboBox.getSelectedItem();    
    }

    @Override
    protected void initComponents() {
        identityComboBox.setModel( new DefaultComboBoxModel( getIdentityOptions() ) );
        identityComboBox.setSelectedItem( new IdentityTarget(identityTargetable.getIdentityTarget()) );
        identityComboBox.setRenderer( new TextListCellRenderer<IdentityTarget>( new Functions.Unary<String,IdentityTarget>(){
            @Override
            public String call(final IdentityTarget identityTarget) {
                return describeIdentity(identityTarget);
            }
        } ) );
        identityComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                checkSyntax();
            }
        });
        if ( identityComboBox.getModel().getSize()==0 ) {
            identityComboBox.setEnabled(false);
            warningLabel.setVisible(true);
            warningLabel.setText("No identities are available for selection.");
        }        
        
        add( mainPanel, BorderLayout.CENTER );
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( IdentityTargetSelectorValidatedPanel.class.getName() );

    private JPanel mainPanel;
    private JComboBox identityComboBox;
    private JLabel warningLabel;

    private final Map<IdentityTarget,String> descriptionCache = new HashMap<IdentityTarget,String>();
    private final IdentityTargetable identityTargetable;
    private final IdentityTarget[] identityTargetOptions;

    private IdentityTarget[] getIdentityOptions() {
        java.util.List<IdentityTarget> identityTarget = new ArrayList<IdentityTarget>();
        identityTarget.add( new IdentityTarget() );
        identityTarget.addAll( Arrays.asList(identityTargetOptions) );
        return identityTarget.toArray(new IdentityTarget[identityTarget.size()]);
    }

    private String describeIdentity( final IdentityTarget identityTarget ) {
        String description = identityTarget.describeIdentity();

        if ( descriptionCache.containsKey( identityTarget ) ) {
            description = descriptionCache.get( identityTarget );
        } else if ( identityTarget.getIdentityProviderOid() != 0 && identityTarget.getTargetIdentityType() != null ) {
            try {
                final StringBuilder descriptionBuilder = new StringBuilder();

                final IdentityAdmin admin = Registry.getDefault().getIdentityAdmin();
                final IdentityProviderConfig config = admin.findIdentityProviderConfigByID(identityTarget.getIdentityProviderOid());
                final String providerName = config==null ? "Unknown #" + identityTarget.getIdentityProviderOid() : config.getName();

                switch (identityTarget.getTargetIdentityType()) {
                    case USER:
                        descriptionBuilder.append("User: ");
                        descriptionBuilder.append(identityTarget.getIdentityInfo()==null ? identityTarget.getIdentityId() : identityTarget.getIdentityInfo());
                        descriptionBuilder.append(" [");
                        descriptionBuilder.append(providerName);
                        descriptionBuilder.append("]");
                        break;
                    case GROUP:
                        descriptionBuilder.append("Group Membership: ");
                        descriptionBuilder.append(identityTarget.getIdentityInfo()==null ? identityTarget.getIdentityId() : identityTarget.getIdentityInfo());
                        descriptionBuilder.append(" [");
                        descriptionBuilder.append(providerName);
                        descriptionBuilder.append("]");
                        break;
                    case PROVIDER:
                        descriptionBuilder.append("Identity Provider: ");
                        descriptionBuilder.append(providerName);
                        break;
                }

                description = descriptionBuilder.toString(); 
                descriptionCache.put( new IdentityTarget( identityTarget ), description );
            } catch ( Exception e ) {
                logger.log( Level.WARNING, "Error getting description for identity '"+ExceptionUtils.getMessage(e)+"'.", ExceptionUtils.getDebugException(e) );
            }
        }

        return description;
    }

}
