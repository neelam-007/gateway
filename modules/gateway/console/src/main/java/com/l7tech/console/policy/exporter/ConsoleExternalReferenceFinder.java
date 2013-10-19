package com.l7tech.console.policy.exporter;

import com.l7tech.console.panels.ResolveExternalPolicyReferencesWizard;
import com.l7tech.console.util.JmsUtilities;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.ResourceAdminEntityResolver;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.LicenseException;
import com.l7tech.gateway.common.admin.IdentityAdmin;
import com.l7tech.gateway.common.admin.PolicyAdmin;
import com.l7tech.gateway.common.custom.CustomAssertionsRegistrar;
import com.l7tech.gateway.common.export.ExternalReferenceFactory;
import com.l7tech.gateway.common.jdbc.JdbcAdmin;
import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.gateway.common.resources.ResourceAdmin;
import com.l7tech.gateway.common.resources.ResourceEntryHeader;
import com.l7tech.gateway.common.resources.ResourceType;
import com.l7tech.gateway.common.security.TrustedCertAdmin;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.gateway.common.siteminder.SiteMinderAdmin;
import com.l7tech.gateway.common.siteminder.SiteMinderConfiguration;
import com.l7tech.gateway.common.transport.SsgActiveConnector;
import com.l7tech.gateway.common.transport.TransportAdmin;
import com.l7tech.gateway.common.transport.jms.JmsAdmin;
import com.l7tech.gateway.common.transport.jms.JmsConnection;
import com.l7tech.gateway.common.transport.jms.JmsEndpoint;
import com.l7tech.gui.util.Utilities;
import com.l7tech.identity.Group;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.GenericEntity;
import com.l7tech.policy.GenericEntityHeader;
import com.l7tech.policy.Policy;
import com.l7tech.policy.exporter.*;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.util.*;
import org.jetbrains.annotations.NotNull;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
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
public class ConsoleExternalReferenceFinder implements ExternalReferenceFinder, ExternalReferenceErrorListener, PolicyImporter.PolicyImporterAdvisor, EntityResolver {

    //- PUBLIC

    @Override
    public TrustedCert findCertByPrimaryKey(Goid certGoid) throws FindException {
        final TrustedCertAdmin admin =  getAdminInterface( TrustedCertAdmin.class );
        return admin.findCertByPrimaryKey(certGoid);
    }

    @Override
    public Collection<TrustedCert> findAllCerts() throws FindException {
        final TrustedCertAdmin admin = getAdminInterface(TrustedCertAdmin.class);
        return admin.findAllCerts();
    }

    @Override
    public SsgKeyEntry findKeyEntry( final String alias, final Goid keystoreGoid ) throws FindException, KeyStoreException {
        final TrustedCertAdmin admin = getAdminInterface( TrustedCertAdmin.class );
        return admin.findKeyEntry( alias, keystoreGoid );
    }

    @Override
    public Collection getAssertions() {
        try {
            return getAdminInterface(CustomAssertionsRegistrar.class).getAssertions();
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
        PolicyAdmin policyAdmin = getAdminInterface(PolicyAdmin.class);
        return policyAdmin.findPolicyByGuid( guid );
    }

    @Override
    public Policy findPolicyByUniqueName( final String name ) throws FindException {
        PolicyAdmin policyAdmin = getAdminInterface( PolicyAdmin.class );
        return policyAdmin.findPolicyByUniqueName( name );
    }

    @Override
    public ResourceEntryHeader findResourceEntryByUriAndType( final String uri, final ResourceType type ) throws FindException {
        ResourceAdmin resourceAdmin = getAdminInterface( ResourceAdmin.class );
        return resourceAdmin.findResourceHeaderByUriAndType( uri, type );
    }

    @Override
    public Collection<ResourceEntryHeader> findResourceEntryByKeyAndType( final String key, final ResourceType type ) throws FindException {
        ResourceAdmin resourceAdmin = getAdminInterface( ResourceAdmin.class );
        return resourceAdmin.findResourceHeadersByKeyAndType( key, type );
    }

    @Override
    public JdbcConnection getJdbcConnection( final String name ) throws FindException {
        JdbcAdmin jdbcAdmin = getAdminInterface( JdbcAdmin.class );
        return jdbcAdmin.getJdbcConnection( name );
    }

    @Override
    public JmsEndpoint findEndpointByPrimaryKey( final Goid goid ) throws FindException {
        JmsAdmin jmsAdmin = getAdminInterface( JmsAdmin.class );
        return jmsAdmin.findEndpointByPrimaryKey( goid );
    }

    @Override
    public JmsConnection findConnectionByPrimaryKey( final Goid goid ) throws FindException {
        JmsAdmin jmsAdmin = getAdminInterface( JmsAdmin.class );
        return jmsAdmin.findConnectionByPrimaryKey( goid );
    }

    @Override
    public SsgActiveConnector findConnectorByPrimaryKey(Goid goid) throws FindException {
        TransportAdmin transportAdmin = getAdminInterface( TransportAdmin.class );
        return transportAdmin.findSsgActiveConnectorByPrimaryKey(goid);
    }

    @Override
    public Collection<SsgActiveConnector> findSsgActiveConnectorsByType(String type) throws FindException {
        TransportAdmin transportAdmin = getAdminInterface( TransportAdmin.class );
        return transportAdmin.findSsgActiveConnectorsByType(type);
    }

    @Override
    public Set<ExternalReferenceFactory> findAllExternalReferenceFactories() throws FindException {
        PolicyAdmin policyAdmin = getAdminInterface( PolicyAdmin.class );
        return policyAdmin.findAllExternalReferenceFactories();
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
        IdentityAdmin idAdmin = getAdminInterface( IdentityAdmin.class );
        return idAdmin.findAllIdentityProviderConfig();
    }

    @Override
    public IdentityProviderConfig findIdentityProviderConfigByID( Goid providerOid ) throws FindException {
        IdentityAdmin idAdmin = getAdminInterface(IdentityAdmin.class);
        return idAdmin.findIdentityProviderConfigByID( providerOid );
    }

    @Override
    public EntityHeaderSet<IdentityHeader> findAllGroups( Goid providerOid ) throws FindException {
        IdentityAdmin idAdmin = getAdminInterface( IdentityAdmin.class );
        return idAdmin.findAllGroups( providerOid );
    }

    @Override
    public Group findGroupByID( Goid providerOid, String groupId ) throws FindException {
        IdentityAdmin idAdmin = getAdminInterface( IdentityAdmin.class );
        return idAdmin.findGroupByID( providerOid, groupId );
    }

    @Override
    public Group findGroupByName( Goid providerOid, String name ) throws FindException {
        IdentityAdmin idAdmin = getAdminInterface( IdentityAdmin.class );
        return idAdmin.findGroupByName( providerOid, name );
    }

    @Override
    public Set<IdentityHeader> getUserHeaders( Goid providerOid, String groupId ) throws FindException {
        IdentityAdmin idAdmin = getAdminInterface( IdentityAdmin.class );
        return idAdmin.getUserHeaders( providerOid, groupId );
    }

    @Override
    public  EntityHeaderSet<IdentityHeader> findAllUsers( Goid providerOid ) throws FindException {
        IdentityAdmin idAdmin = getAdminInterface( IdentityAdmin.class );
        return idAdmin.findAllUsers( providerOid );
    }

    @Override
    public User findUserByID( Goid providerOid, String userId ) throws FindException {
        IdentityAdmin idAdmin = getAdminInterface( IdentityAdmin.class );
        return idAdmin.findUserByID( providerOid, userId );
    }

    @Override
    public User findUserByLogin( Goid providerOid, String login ) throws FindException {
        IdentityAdmin idAdmin = getAdminInterface( IdentityAdmin.class );
        return idAdmin.findUserByLogin( providerOid, login );
    }

    @Override
    public SiteMinderConfiguration findSiteMinderConfigurationByName(String name) throws FindException {
        SiteMinderAdmin siteMinderAdmin = getAdminInterface(SiteMinderAdmin.class);
        return siteMinderAdmin.getSiteMinderConfiguration(name);
    }

    @Override
    public SiteMinderConfiguration findSiteMinderConfigurationByID(Goid id) throws FindException {
        SiteMinderAdmin siteMinderAdmin = getAdminInterface(SiteMinderAdmin.class);
        return siteMinderAdmin.getSiteMinderConfiguration(id);
    }

    @Override
    public <ET extends GenericEntity> EntityManager<ET, GenericEntityHeader> getGenericEntityManager(@NotNull Class<ET> entityClass) throws FindException {
        return null; //Intentionally null, meant to support interface only. Not to be called in this context.
    }

    @Override
    public SecurePassword findSecurePasswordById(Goid id) throws FindException {
        final TrustedCertAdmin admin = getAdminInterface(TrustedCertAdmin.class);
        return admin.findSecurePasswordById(id);
    }

    @Override
    public SecurePassword findSecurePasswordByName(String name) throws FindException {
        final TrustedCertAdmin admin = getAdminInterface(TrustedCertAdmin.class);
        return admin.findSecurePasswordByName(name);
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

    @Override
    public InputSource resolveEntity( final String publicId, final String systemId ) throws IOException {
        ResourceAdmin resourceAdmin = Registry.getDefault().getResourceAdmin();
        if ( resourceAdmin==null ) throw new IOException("Error accessing gateway.");
        return new ResourceAdminEntityResolver( resourceAdmin ).resolveEntity( publicId, systemId );
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( ConsoleExternalReferenceFinder.class.getName() );

    private <AI> AI getAdminInterface( final Class<AI> adminInterfaceClass ) throws FindException {
        final Option<AI> adminInterface = Registry.getDefault().getAdminInterface(adminInterfaceClass);
        if ( adminInterface.isSome() ) {
            return adminInterface.some();
        } else {
            throw new FindException( "Error accessing gateway." );
        }
    }
}
