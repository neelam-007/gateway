package com.l7tech.policy.exporter;

import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.gateway.common.schema.SchemaEntry;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.gateway.common.transport.jms.JmsConnection;
import com.l7tech.gateway.common.transport.jms.JmsEndpoint;
import com.l7tech.identity.Group;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityHeaderSet;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.IdentityHeader;
import com.l7tech.policy.Policy;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.util.Pair;

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
    public TrustedCert findCertByPrimaryKey( final long certOid ) throws FindException {
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
    public Collection<SchemaEntry> findSchemaByName( final String schemaName ) throws FindException {
        return Collections.emptyList();
    }

    @Override
    public Collection<SchemaEntry> findSchemaByTNS( final String tns ) throws FindException {
        return Collections.emptyList();
    }

    @Override
    public JdbcConnection getJdbcConnection( final String name ) throws FindException {
        return null;
    }

    @Override
    public JmsEndpoint findEndpointByPrimaryKey( final long oid ) throws FindException {
        return null;
    }

    @Override
    public JmsConnection findConnectionByPrimaryKey( final long oid ) throws FindException {
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
}
