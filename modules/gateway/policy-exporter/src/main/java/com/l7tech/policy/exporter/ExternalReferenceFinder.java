package com.l7tech.policy.exporter;

import com.l7tech.gateway.common.export.ExternalReferenceFactory;
import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.gateway.common.resources.ResourceEntryHeader;
import com.l7tech.gateway.common.resources.ResourceType;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
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
import com.l7tech.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.security.KeyStoreException;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Interface for finding external reference targets.
 *
 * <p>Methods returning entities should return null if an entity is not
 * found. Methods returning collections should never return null.</p>
 */
public interface ExternalReferenceFinder {
    
    TrustedCert findCertByPrimaryKey( long certOid ) throws FindException;

    Collection<TrustedCert> findAllCerts() throws FindException;

    SsgKeyEntry findKeyEntry( final String alias, final long keystoreOid ) throws FindException, KeyStoreException;

    Collection getAssertions();

    Policy findPolicyByGuid( String guid ) throws FindException;

    Policy findPolicyByUniqueName( String name ) throws FindException;

    ResourceEntryHeader findResourceEntryByUriAndType( String uri, ResourceType type ) throws FindException;

    Collection<ResourceEntryHeader> findResourceEntryByKeyAndType( String key, ResourceType type ) throws FindException;

    JdbcConnection getJdbcConnection( String name ) throws FindException;

    JmsEndpoint findEndpointByPrimaryKey( long oid ) throws FindException;

    JmsConnection findConnectionByPrimaryKey( long oid ) throws FindException;

    SsgActiveConnector findConnectorByPrimaryKey (long oid) throws FindException;

    Collection<SsgActiveConnector> findSsgActiveConnectorsByType(String type) throws FindException;

    Set<ExternalReferenceFactory> findAllExternalReferenceFactories() throws FindException;

    List<Pair<JmsEndpoint,JmsConnection>> loadJmsQueues() throws FindException;

    EntityHeader[] findAllIdentityProviderConfig() throws FindException;

    IdentityProviderConfig findIdentityProviderConfigByID( long providerOid ) throws FindException;

    Collection<IdentityHeader> findAllGroups( long providerOid ) throws FindException;

    Group findGroupByID( long providerOid, String groupId ) throws FindException;

    Group findGroupByName( long providerOid, String name ) throws FindException;

    Collection<IdentityHeader> getUserHeaders( long providerOid, String groupId ) throws FindException;

    Collection<IdentityHeader> findAllUsers( long providerOid ) throws FindException;

    User findUserByID( long providerOid, String userId ) throws FindException;

    User findUserByLogin( long providerOid, String login ) throws FindException;

    <ET extends GenericEntity>
    GoidEntityManager<ET, GenericEntityHeader> getGenericEntityManager(@NotNull Class<ET> entityClass) throws FindException;

}
