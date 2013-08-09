package com.l7tech.policy.exporter;

import com.l7tech.gateway.common.export.ExternalReferenceFactory;
import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.gateway.common.resources.ResourceEntryHeader;
import com.l7tech.gateway.common.resources.ResourceType;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.gateway.common.siteminder.SiteMinderConfiguration;
import com.l7tech.gateway.common.transport.SsgActiveConnector;
import com.l7tech.gateway.common.transport.jms.JmsConnection;
import com.l7tech.gateway.common.transport.jms.JmsEndpoint;
import com.l7tech.identity.Group;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.GenericEntity;
import com.l7tech.policy.GenericEntityHeader;
import com.l7tech.policy.Policy;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.util.Either;
import com.l7tech.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.security.KeyStoreException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 *
 */
public class ExternalReferenceFinderStub implements ExternalReferenceFinder {

    @Override
    public TrustedCert findCertByPrimaryKey(Goid certGoid) throws FindException {
        return null;
    }

    @Override
    public Collection<TrustedCert> findAllCerts() throws FindException {
        return Collections.emptyList();
    }

    @Override
    public SsgKeyEntry findKeyEntry( final String alias, final long keystoreOid ) throws FindException, KeyStoreException {
        return null;
    }

    @Override
    public Collection getAssertions() {
        return Collections.emptyList();
    }

    @Override
    public Policy findPolicyByGuid( final String guid ) throws FindException {
        return null;
    }

    @Override
    public Policy findPolicyByUniqueName( final String name ) throws FindException {
        return null;
    }

    @Override
    public ResourceEntryHeader findResourceEntryByUriAndType( final String uri, final ResourceType type ) throws FindException {
        return null;
    }

    @Override
    public Collection<ResourceEntryHeader> findResourceEntryByKeyAndType( final String key, final ResourceType type ) throws FindException {
        return Collections.emptyList();
    }

    @Override
    public JdbcConnection getJdbcConnection( final String name ) throws FindException {
        return null;
    }

    @Override
    public JmsEndpoint findEndpointByOidOrGoid(Either<Long, Goid> endpointId) throws FindException {
        return null;
    }

    @Override
    public JmsEndpoint findEndpointByPrimaryKey( final Goid oid ) throws FindException {
        return null;
    }

    @Override
    public JmsConnection findConnectionByPrimaryKey( final Goid oid ) throws FindException {
        return null;
    }

    @Override
    public SsgActiveConnector findConnectorByPrimaryKey(Goid oid) throws FindException {
        return null;
    }

    @Override
    public SsgActiveConnector findConnectorByOidOrGoid(Either<Long, Goid> connectorId) throws FindException {
        return null;
    }

    @Override
    public Collection<SsgActiveConnector> findSsgActiveConnectorsByType(String type) throws FindException {
        return null;
    }

    @Override
    public Set<ExternalReferenceFactory> findAllExternalReferenceFactories() throws FindException {
        return null;
    }

    @Override
    public List<Pair<JmsEndpoint, JmsConnection>> loadJmsQueues() throws FindException {
        return Collections.emptyList();
    }

    @Override
    public EntityHeader[] findAllIdentityProviderConfig() throws FindException {
        return new EntityHeader[0];
    }

    @Override
    public IdentityProviderConfig findIdentityProviderConfigByID( final long providerOid ) throws FindException {
        return null;
    }

    @Override
    public EntityHeaderSet<IdentityHeader> findAllGroups( final long providerOid ) throws FindException {
        return new EntityHeaderSet<IdentityHeader>();
    }

    @Override
    public Group findGroupByID( final long providerOid, final String groupId ) throws FindException {
        return null;
    }

    @Override
    public Group findGroupByName( final long providerOid, final String name ) throws FindException {
        return null;
    }

    @Override
    public Set<IdentityHeader> getUserHeaders( final long providerOid, final String groupId ) throws FindException {
        return Collections.emptySet();
    }

    @Override
    public EntityHeaderSet<IdentityHeader> findAllUsers( final long providerOid ) throws FindException {
        return new EntityHeaderSet<IdentityHeader>();
    }

    @Override
    public User findUserByID( final long providerOid, final String userId ) throws FindException {
        return null;
    }

    @Override
    public User findUserByLogin( final long providerOid, final String login ) throws FindException {
        return null;
    }

    @Override
    public SiteMinderConfiguration findSiteMinderConfigurationByName(String name) throws FindException {
        return null;
    }

    @Override
    public SiteMinderConfiguration findSiteMinderConfigurationByID(Goid id) throws FindException {
        return null;
    }

    @Override
    public <ET extends GenericEntity> GoidEntityManager<ET, GenericEntityHeader> getGenericEntityManager(@NotNull Class<ET> entityClass) throws FindException {
        return null;
    }

}
