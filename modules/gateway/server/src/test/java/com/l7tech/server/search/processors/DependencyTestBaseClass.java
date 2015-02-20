package com.l7tech.server.search.processors;

import com.l7tech.common.io.CertGenParams;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.gateway.common.transport.SsgActiveConnector;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.xmlsec.LookupTrustedCertificateAssertion;
import com.l7tech.policy.assertion.xmlsec.WsSecurity;
import com.l7tech.search.Dependency;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.security.cert.TrustedCertManager;
import com.l7tech.security.prov.CertificateRequest;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.EntityCrud;
import com.l7tech.server.EntityHeaderUtils;
import com.l7tech.server.search.DependencyAnalyzer;
import com.l7tech.server.search.DependencyAnalyzerImpl;
import com.l7tech.server.search.DependencyProcessorRegistry;
import com.l7tech.server.security.keystore.SsgKeyFinder;
import com.l7tech.server.security.keystore.SsgKeyStore;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.server.security.password.SecurePasswordManager;
import com.l7tech.util.CollectionUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.SignatureException;
import java.util.List;

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
    protected DefaultKey defaultKey;
    @Mock
    protected SsgKeyStoreManager keyStoreManager;
    @Mock
    private TrustedCertManager trustedCertManager;

    @Spy
    DependencyProcessorRegistry dependencyProcessorRegistry;

    @Spy
    protected DependencyProcessorRegistry<SsgConnector> ssgConnectorDependencyProcessorRegistry;

    @Spy
    protected DependencyProcessorRegistry<SsgActiveConnector> ssgActiveConnectorDependencyProcessorRegistry;

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
    SsgActiveConnectorDependencyProcessor ssgActiveConnectorDependencyProcessor = new SsgActiveConnectorDependencyProcessor();
    @InjectMocks
    CassandraConnectionDependencyProcessor cassandraConnectionDependencyProcessor = new CassandraConnectionDependencyProcessor();
    @InjectMocks
    HttpConfigurationDependencyProcessor httpConfigurationDependencyProcessor = new HttpConfigurationDependencyProcessor();
    @InjectMocks
    SsgKeyEntryDependencyProcessor ssgKeyEntryDependencyProcessor = new SsgKeyEntryDependencyProcessor();

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
            .put(Dependency.DependencyType.SSG_ACTIVE_CONNECTOR, ssgActiveConnectorDependencyProcessor)
            .put(Dependency.DependencyType.JMS_ENDPOINT, jdbcDependencyProcessor)
            .put(Dependency.DependencyType.CASSANDRA_CONNECTION, cassandraConnectionDependencyProcessor)
            .put(Dependency.DependencyType.HTTP_CONFIGURATION, httpConfigurationDependencyProcessor)
            .put(Dependency.DependencyType.SSG_PRIVATE_KEY, ssgKeyEntryDependencyProcessor)
            .map());

    @InjectMocks
    protected DependencyAnalyzer dependencyAnalyzer = new DependencyAnalyzerImpl();

    protected DependencyTestBaseClass() {
    }

    @Before
    public void before() throws IOException, KeyStoreException, FindException {
        //add the custom connector dependency processor
        assertionDependencyProcessorRegistry.register(LookupTrustedCertificateAssertion.class.getName(), assertionLookupTrustedCertificate);
        assertionDependencyProcessorRegistry.register(WsSecurity.class.getName(), assertionWsSecurityProcessor);

        defaultSslKey = SsgKeyEntry.createDummyEntityForAuditing(defaultKeystoreId, "defaultSslKey");
        Mockito.when(defaultKey.getSslInfo()).thenReturn(defaultSslKey);
        Mockito.when(keyStoreManager.findByPrimaryKey(defaultSslKey.getKeystoreId())).thenReturn(ssgKeyFinder);
        EntityHeader ssgActiveConnectorHeader = EntityHeaderUtils.fromEntity(defaultSslKey);
        mockEntity(defaultSslKey, ssgActiveConnectorHeader);
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

    protected Goid defaultKeystoreId = new Goid(0, 1);

    protected SsgKeyEntry defaultSslKey;
    private SsgKeyFinder ssgKeyFinder = new SsgKeyFinder() {
        @Override
        public Goid getGoid() {
            return defaultKeystoreId;
        }

        @Override
        public SsgKeyStoreType getType() {
            return SsgKeyStoreType.PKCS12_SOFTWARE;
        }

        @Override
        public boolean isMutable() {
            return false;
        }

        @Override
        public boolean isKeyExportSupported() {
            return true;
        }

        @Override
        public SsgKeyStore getKeyStore() {
            return null;
        }

        @Override
        public List<String> getAliases() throws KeyStoreException {
            return null;
        }

        @Override
        public SsgKeyEntry getCertificateChain(String alias) throws ObjectNotFoundException, KeyStoreException {
            return null;
        }

        @Override
        public CertificateRequest makeCertificateSigningRequest(String alias, CertGenParams certGenParams) throws InvalidKeyException, SignatureException, KeyStoreException {
            return null;
        }

        @Override
        public String getName() {
            return null;
        }

        @Override
        public String getId() {
            return null;
        }
    };
}
