package com.l7tech.policy.exporter;

import com.l7tech.gateway.common.cassandra.CassandraConnection;
import com.l7tech.gateway.common.export.ExternalReferenceFactory;
import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.gateway.common.resources.ResourceEntryHeader;
import com.l7tech.gateway.common.resources.ResourceType;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.gateway.common.security.password.SecurePassword;
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
import com.l7tech.policy.assertion.ext.entity.CustomEntitySerializer;
import com.l7tech.policy.assertion.ext.store.KeyValueStore;
import com.l7tech.security.cert.TrustedCert;
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
    public SsgKeyEntry findKeyEntry( final String alias, final Goid keystoreGoid ) throws FindException, KeyStoreException {
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
    public CassandraConnection getCassandraConnection(final String name) throws FindException {
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
    public IdentityProviderConfig findIdentityProviderConfigByID( final Goid providerOid ) throws FindException {
        return null;
    }

    @Override
    public EntityHeaderSet<IdentityHeader> findAllGroups( final Goid providerOid ) throws FindException {
        return new EntityHeaderSet<IdentityHeader>();
    }

    @Override
    public Group findGroupByID( final Goid providerOid, final String groupId ) throws FindException {
        return null;
    }

    @Override
    public Group findGroupByName( final Goid providerOid, final String name ) throws FindException {
        return null;
    }

    @Override
    public Set<IdentityHeader> getUserHeaders( final Goid providerOid, final String groupId ) throws FindException {
        return Collections.emptySet();
    }

    @Override
    public EntityHeaderSet<IdentityHeader> findAllUsers( final Goid providerOid ) throws FindException {
        return new EntityHeaderSet<IdentityHeader>();
    }

    @Override
    public User findUserByID( final Goid providerOid, final String userId ) throws FindException {
        return null;
    }

    @Override
    public User findUserByLogin( final Goid providerOid, final String login ) throws FindException {
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
    public <ET extends GenericEntity> EntityManager<ET, GenericEntityHeader> getGenericEntityManager(@NotNull Class<ET> entityClass) throws FindException {
        return null;
    }

    @Override
    public SecurePassword findSecurePasswordById(Goid id) throws FindException {
        return null;
    }

    @Override
    public SecurePassword findSecurePasswordByName(String name) throws FindException {
        return null;
    }

    @Override
    public KeyValueStore getCustomKeyValueStore() {
        return null;
    }

    @Override
    public CustomEntitySerializer getCustomKeyValueEntitySerializer(String entitySerializerClassName) {
        return null;
    }

}
