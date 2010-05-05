package com.l7tech.console.policy.exporter;

import com.l7tech.console.panels.ResolveExternalPolicyReferencesWizard;
import com.l7tech.console.util.JmsUtilities;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.LicenseException;
import com.l7tech.gateway.common.admin.IdentityAdmin;
import com.l7tech.gateway.common.admin.PolicyAdmin;
import com.l7tech.gateway.common.jdbc.JdbcAdmin;
import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.gateway.common.schema.SchemaAdmin;
import com.l7tech.gateway.common.schema.SchemaEntry;
import com.l7tech.gateway.common.security.TrustedCertAdmin;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.gateway.common.transport.jms.JmsAdmin;
import com.l7tech.gateway.common.transport.jms.JmsConnection;
import com.l7tech.gateway.common.transport.jms.JmsEndpoint;
import com.l7tech.gui.util.Utilities;
import com.l7tech.identity.Group;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityHeaderSet;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.IdentityHeader;
import com.l7tech.policy.Policy;
import com.l7tech.policy.exporter.ExternalReference;
import com.l7tech.policy.exporter.ExternalReferenceErrorListener;
import com.l7tech.policy.exporter.ExternalReferenceFinder;
import com.l7tech.policy.exporter.PolicyImportCancelledException;
import com.l7tech.policy.exporter.PolicyImporter;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.Pair;

import javax.swing.*;
import java.awt.*;
import java.security.KeyStoreException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ExternalReferenceFinder implementation for console use.
 *
 * <p>This class also acts as an import advisor with the advice being provided
 * by the user via the external references wizard.</p>
 */
class ConsoleExternalReferenceFinder implements ExternalReferenceFinder, ExternalReferenceErrorListener, PolicyImporter.PolicyImporterAdvisor {

    //- PUBLIC

    @Override
    public TrustedCert findCertByPrimaryKey( long certOid ) throws FindException {
        final TrustedCertAdmin admin = admin(Registry.getDefault().getTrustedCertManager());
        return admin.findCertByPrimaryKey(certOid);
    }

    @Override
    public Collection<TrustedCert> findAllCerts() throws FindException {
        final TrustedCertAdmin admin = admin(Registry.getDefault().getTrustedCertManager());
        return admin.findAllCerts(); 
    }

    @Override
    public SsgKeyEntry findKeyEntry( final String alias, final long keystoreOid ) throws FindException, KeyStoreException {
        final TrustedCertAdmin admin = admin(Registry.getDefault().getTrustedCertManager());
        return admin.findKeyEntry( alias, keystoreOid );
    }

    @Override
    public Collection getAssertions() {
        try {
            return admin(Registry.getDefault().getCustomAssertionsRegistrar()).getAssertions();
        } catch (RuntimeException e) {
            if ( ExceptionUtils.causedBy(e, LicenseException.class)) {
                logger.log( Level.INFO, "Custom assertions unavailable or unlicensed");
            } else
                logger.log(Level.WARNING, "Cannot get remote assertions", e);
        } catch ( FindException e ) {
            logger.log(Level.WARNING, "Cannot get remote assertions", e);
        }

        return Collections.emptyList();
    }

    @Override
    public Policy findPolicyByGuid( final String guid ) throws FindException {
        PolicyAdmin policyAdmin = admin(Registry.getDefault().getPolicyAdmin());
        return policyAdmin.findPolicyByGuid( guid );
    }

    @Override
    public Policy findPolicyByUniqueName( final String name ) throws FindException {
        PolicyAdmin policyAdmin = admin(Registry.getDefault().getPolicyAdmin());
        return policyAdmin.findPolicyByUniqueName( name );        
    }

    @Override
    public Collection<SchemaEntry> findSchemaByName( final String schemaName ) throws FindException {
        SchemaAdmin schemaAdmin = admin(Registry.getDefault().getSchemaAdmin());
        return schemaAdmin.findByName( schemaName );
    }

    @Override
    public Collection<SchemaEntry> findSchemaByTNS( final String tns ) throws FindException {
        SchemaAdmin schemaAdmin = admin(Registry.getDefault().getSchemaAdmin());
        return schemaAdmin.findByTNS( tns );
    }

    @Override
    public JdbcConnection getJdbcConnection( final String name ) throws FindException {
        JdbcAdmin jdbcAdmin = admin(Registry.getDefault().getJdbcConnectionAdmin());
        return jdbcAdmin.getJdbcConnection( name );
    }

    @Override
    public JmsEndpoint findEndpointByPrimaryKey( final long oid ) throws FindException {
        JmsAdmin jmsAdmin = admin(Registry.getDefault().getJmsManager());
        return jmsAdmin.findEndpointByPrimaryKey( oid );
    }

    @Override
    public JmsConnection findConnectionByPrimaryKey( final long oid ) throws FindException {
        JmsAdmin jmsAdmin = admin(Registry.getDefault().getJmsManager());
        return jmsAdmin.findConnectionByPrimaryKey( oid );
    }

    @Override
    public List<Pair<JmsEndpoint,JmsConnection>> loadJmsQueues() throws FindException  {
        return Functions.map( JmsUtilities.loadJmsQueues(false), new Functions.Unary<Pair<JmsEndpoint,JmsConnection>,JmsAdmin.JmsTuple>(){
            @Override
            public Pair<JmsEndpoint, JmsConnection> call( final JmsAdmin.JmsTuple jmsTuple ) {
                return new Pair<JmsEndpoint, JmsConnection>( jmsTuple.getEndpoint(), jmsTuple.getConnection() );
            }
        });
    }

    @Override
    public EntityHeader[] findAllIdentityProviderConfig() throws FindException {
        IdentityAdmin idAdmin = admin(Registry.getDefault().getIdentityAdmin());
        return idAdmin.findAllIdentityProviderConfig();        
    }

    @Override
    public IdentityProviderConfig findIdentityProviderConfigByID( long providerOid ) throws FindException {
        IdentityAdmin idAdmin = admin(Registry.getDefault().getIdentityAdmin());
        return idAdmin.findIdentityProviderConfigByID( providerOid );
    }

    @Override
    public EntityHeaderSet<IdentityHeader> findAllGroups( long providerOid ) throws FindException {
        IdentityAdmin idAdmin = admin(Registry.getDefault().getIdentityAdmin());
        return idAdmin.findAllGroups( providerOid );
    }

    @Override
    public Group findGroupByID( long providerOid, String groupId ) throws FindException {
        IdentityAdmin idAdmin = admin(Registry.getDefault().getIdentityAdmin());
        return idAdmin.findGroupByID( providerOid, groupId );        
    }

    @Override
    public Group findGroupByName( long providerOid, String name ) throws FindException {
        IdentityAdmin idAdmin = admin(Registry.getDefault().getIdentityAdmin());
        return idAdmin.findGroupByName( providerOid, name );        
    }

    @Override
    public Set<IdentityHeader> getUserHeaders( long providerOid, String groupId ) throws FindException {
        IdentityAdmin idAdmin = admin(Registry.getDefault().getIdentityAdmin());
        return idAdmin.getUserHeaders( providerOid, groupId );        
    }

    @Override
    public  EntityHeaderSet<IdentityHeader> findAllUsers( long providerOid ) throws FindException {
        IdentityAdmin idAdmin = admin(Registry.getDefault().getIdentityAdmin());
        return idAdmin.findAllUsers( providerOid );
    }

    @Override
    public User findUserByID( long providerOid, String userId ) throws FindException {
        IdentityAdmin idAdmin = admin(Registry.getDefault().getIdentityAdmin());
        return idAdmin.findUserByID( providerOid, userId );        
    }

    @Override
    public User findUserByLogin( long providerOid, String login ) throws FindException {
        IdentityAdmin idAdmin = admin(Registry.getDefault().getIdentityAdmin());
        return idAdmin.findUserByLogin( providerOid, login );        
    }


    @Override
    public void warning( final String title, final String msg ) {
        JOptionPane.showMessageDialog(null, msg, title, JOptionPane.WARNING_MESSAGE);
    }

    @Override
    public boolean mapReference( final String referenceType, final String referenceId, final String targetId ) {
        return true;
    }

    @Override
    public boolean resolveReferences( final ExternalReference[] unresolvedRefsArray ) throws PolicyImportCancelledException {
        final Frame mw = TopComponents.getInstance().getTopParent();
        boolean wasCancelled = false;
        try {
            ResolveExternalPolicyReferencesWizard wiz =
                    ResolveExternalPolicyReferencesWizard.fromReferences(mw, unresolvedRefsArray);
            wiz.pack();
            Utilities.centerOnScreen(wiz);
            wiz.setModal(true);
            wiz.setVisible(true);
            // if the wizard returns false, we must return
            if (wiz.wasCanceled()) wasCancelled = true;
        } catch(Exception e) {
            return false;
        }

        if(wasCancelled) {
            throw new PolicyImportCancelledException();
        }

        return true;
    }

    @Override
    public boolean acceptPolicyConflict( final String policyName, final String existingPolicyName, final String guid ) {
        // Prompt an optional dialog to resolve policy fragment conflicts..
        int result = JOptionPane.showOptionDialog(TopComponents.getInstance().getTopParent(),
            "<html><center>The imported policy contains an embedded fragment with name '" + policyName + "' that already exists in the system.</center>" +
                "<center>This policy importer will use the already existing policy fragment in place of the embedded fragment.</center></html>",
            "Resolving Policy Fragment Conflict", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, null, null, null);
        return result == JOptionPane.OK_OPTION;
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( ConsoleExternalReferenceFinder.class.getName() );

    private <AI> AI admin( final AI adminInterface ) throws FindException {
        if ( adminInterface == null ) {
            throw new FindException( "Error accessing gateway." ); 
        }
        return adminInterface;
    }
}
