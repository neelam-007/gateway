package com.l7tech.server.search.processors;

import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.xmlsec.LookupTrustedCertificateAssertion;
import com.l7tech.policy.assertion.xmlsec.WsSecurity;
import com.l7tech.search.Dependency;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.security.cert.TrustedCertManager;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.EntityCrud;
import com.l7tech.server.search.DependencyAnalyzer;
import com.l7tech.server.search.DependencyAnalyzerImpl;
import com.l7tech.server.search.DependencyProcessorRegistry;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.server.security.password.SecurePasswordManager;
import com.l7tech.util.CollectionUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * This was created: 6/12/13 as 4:44 PM
 *
 * @author Victor Kazakov
 */
@RunWith(MockitoJUnitRunner.class)
public abstract class DependencyTestBaseClass {
    @Mock
    private EntityCrud entityCrud;

    @Mock
    private IdentityProviderConfigManager identityProviderConfigManager;
    @Mock
    private SecurePasswordManager securePasswordManager;
    @Mock
    private DefaultKey defaultKey;
    @Mock
    private SsgKeyStoreManager keyStoreManager;
    @Mock
    private TrustedCertManager trustedCertManager;

    @Spy
    DependencyProcessorRegistry dependencyProcessorRegistry;

    @Spy
    protected DependencyProcessorRegistry ssgConnectorDependencyProcessorRegistry;

    @Spy
    DependencyProcessorRegistry assertionDependencyProcessorRegistry;

    @InjectMocks
    DefaultDependencyProcessor defaultDependencyProcessor = new DefaultDependencyProcessor();
    @InjectMocks
    PolicyDependencyProcessor policyDependencyProcessor = new PolicyDependencyProcessor();
    @InjectMocks
    FolderDependencyProcessor folderDependencyProcessor = new FolderDependencyProcessor();
    @InjectMocks
    JdbcConnectionDependencyProcessor jdbcConnectionDependencyProcessor = new JdbcConnectionDependencyProcessor();
    @InjectMocks
    SecurePasswordDependencyProcessor securePasswordDependencyProcessor = new SecurePasswordDependencyProcessor();
    @InjectMocks
    DefaultAssertionDependencyProcessor<Assertion> defaultAssertionDependencyProcessor = new DefaultAssertionDependencyProcessor<>();
    @InjectMocks
    AssertionDependencyProcessor assertionDependencyProcessor = new AssertionDependencyProcessor(defaultAssertionDependencyProcessor);
    @InjectMocks
    AssertionLookupTrustedCertificateProcessor assertionLookupTrustedCertificate = new AssertionLookupTrustedCertificateProcessor();
    @InjectMocks
    AssertionWsSecurityProcessor assertionWsSecurityProcessor = new AssertionWsSecurityProcessor();
    @InjectMocks
    JmsEndpointDependencyProcessor jdbcDependencyProcessor = new JmsEndpointDependencyProcessor();
    @InjectMocks
    ClusterPropertyDependencyProcessor clusterPropertyDependencyProcessor = new ClusterPropertyDependencyProcessor();
    @InjectMocks
    IdentityProviderProcessor identityProviderProcessor = new IdentityProviderProcessor();
    @InjectMocks
    SsgConnectorDependencyProcessor ssgConnectorDependencyProcessor = new SsgConnectorDependencyProcessor();
    @InjectMocks
    CassandraConnectionDependencyProcessor cassandraConnectionDependencyProcessor = new CassandraConnectionDependencyProcessor();

    @Spy
    DependencyProcessorStore processorStore = new DependencyProcessorStore(CollectionUtils.MapBuilder.<Dependency.DependencyType, InternalDependencyProcessor>builder()
            .put(Dependency.DependencyType.ANY, defaultDependencyProcessor)
            .put(Dependency.DependencyType.POLICY, policyDependencyProcessor)
            .put(Dependency.DependencyType.FOLDER, folderDependencyProcessor)
            .put(Dependency.DependencyType.JDBC_CONNECTION, jdbcConnectionDependencyProcessor)
            .put(Dependency.DependencyType.SECURE_PASSWORD, securePasswordDependencyProcessor)
            .put(Dependency.DependencyType.ASSERTION, assertionDependencyProcessor)
            .put(Dependency.DependencyType.CLUSTER_PROPERTY, clusterPropertyDependencyProcessor)
            .put(Dependency.DependencyType.ID_PROVIDER_CONFIG, identityProviderProcessor)
            .put(Dependency.DependencyType.SSG_CONNECTOR, ssgConnectorDependencyProcessor)
            .put(Dependency.DependencyType.JMS_ENDPOINT, jdbcDependencyProcessor)
            .put(Dependency.DependencyType.CASSANDRA_CONNECTION, cassandraConnectionDependencyProcessor)
            .map());

    @InjectMocks
    protected DependencyAnalyzer dependencyAnalyzer = new DependencyAnalyzerImpl();

    protected DependencyTestBaseClass() {
    }

    @Before
    public void before() {
        //add the custom connector dependency processor
        assertionDependencyProcessorRegistry.register(LookupTrustedCertificateAssertion.class.getName(), assertionLookupTrustedCertificate);
        assertionDependencyProcessorRegistry.register(WsSecurity.class.getName(), assertionWsSecurityProcessor);
    }

    @After
    public void after() {
        //remove all custom connector dependency processors.
        dependencyProcessorRegistry.remove(LookupTrustedCertificateAssertion.class.getName());
        dependencyProcessorRegistry.remove(WsSecurity.class.getName());
    }

    protected void mockEntity(Entity entity, EntityHeader entityHeader) throws FindException {
        Mockito.when(entityCrud.find(Matchers.eq(entityHeader))).thenReturn(entity);
        if(entity instanceof IdentityProviderConfig){
            Mockito.when(identityProviderConfigManager.findByHeader(entityHeader)).thenReturn((IdentityProviderConfig) entity);
        } else if(entity instanceof SecurePassword){
            Mockito.when(securePasswordManager.findByUniqueName(entityHeader.getName())).thenReturn((SecurePassword) entity);
            Mockito.when(securePasswordManager.findByPrimaryKey(entityHeader.getGoid())).thenReturn((SecurePassword) entity);
        } else if(entity instanceof TrustedCert){
            Mockito.when(trustedCertManager.findByUniqueName(entityHeader.getName())).thenReturn((TrustedCert) entity);
            Mockito.when(trustedCertManager.findByPrimaryKey(entityHeader.getGoid())).thenReturn((TrustedCert) entity);
        }
    }
}
