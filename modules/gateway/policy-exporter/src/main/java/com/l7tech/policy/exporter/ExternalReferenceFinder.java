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
import java.util.List;
import java.util.Set;

/**
 * Interface for finding external reference targets.
 *
 * <p>Methods returning entities should return null if an entity is not
 * found. Methods returning collections should never return null.</p>
 */
public interface ExternalReferenceFinder {

    TrustedCert findCertByPrimaryKey( Goid certGoid ) throws FindException;

    Collection<TrustedCert> findAllCerts() throws FindException;

    SsgKeyEntry findKeyEntry( final String alias, final long keystoreOid ) throws FindException, KeyStoreException;

    Collection getAssertions();

    Policy findPolicyByGuid( String guid ) throws FindException;

    Policy findPolicyByUniqueName( String name ) throws FindException;

    ResourceEntryHeader findResourceEntryByUriAndType( String uri, ResourceType type ) throws FindException;

    Collection<ResourceEntryHeader> findResourceEntryByKeyAndType( String key, ResourceType type ) throws FindException;

    JdbcConnection getJdbcConnection( String name ) throws FindException;

    JmsEndpoint findEndpointByOidOrGoid(Either<Long, Goid> endpointId) throws FindException;

    JmsEndpoint findEndpointByPrimaryKey( Goid goid ) throws FindException;

    JmsConnection findConnectionByPrimaryKey( Goid goid ) throws FindException;

    SsgActiveConnector findConnectorByOidOrGoid(Either<Long, Goid> connectorId) throws FindException;

    SsgActiveConnector findConnectorByPrimaryKey (Goid oid) throws FindException;

    Collection<SsgActiveConnector> findSsgActiveConnectorsByType(String type) throws FindException;

    Set<ExternalReferenceFactory> findAllExternalReferenceFactories() throws FindException;

    List<Pair<JmsEndpoint,JmsConnection>> loadJmsQueues() throws FindException;

    EntityHeader[] findAllIdentityProviderConfig() throws FindException;

    IdentityProviderConfig findIdentityProviderConfigByID( Goid providerOid ) throws FindException;

    Collection<IdentityHeader> findAllGroups( Goid providerOid ) throws FindException;

    Group findGroupByID( Goid providerOid, String groupId ) throws FindException;

    Group findGroupByName( Goid providerOid, String name ) throws FindException;

    Collection<IdentityHeader> getUserHeaders( Goid providerOid, String groupId ) throws FindException;

    Collection<IdentityHeader> findAllUsers( Goid providerOid ) throws FindException;

    User findUserByID( Goid providerOid, String userId ) throws FindException;

    User findUserByLogin( Goid providerOid, String login ) throws FindException;

    SiteMinderConfiguration findSiteMinderConfigurationByName(final String name) throws FindException;

    SiteMinderConfiguration findSiteMinderConfigurationByID(final Goid id) throws FindException;

    <ET extends GenericEntity>
    GoidEntityManager<ET, GenericEntityHeader> getGenericEntityManager(@NotNull Class<ET> entityClass) throws FindException;

}
