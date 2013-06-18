package com.l7tech.server.search.entitytests;

import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.search.Dependency;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.EntityCrud;
import com.l7tech.server.search.DependencyAnalyzer;
import com.l7tech.server.search.DependencyAnalyzerImpl;
import com.l7tech.server.search.DependencyProcessorStore;
import com.l7tech.server.search.processors.*;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.server.security.password.SecurePasswordManager;
import com.l7tech.util.CollectionUtils;
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

    @InjectMocks
    GenericDependencyProcessor genericDependencyProcessor = new GenericDependencyProcessor();
    @InjectMocks
    PolicyDependencyProcessor policyDependencyProcessor = new PolicyDependencyProcessor();
    @InjectMocks
    FolderDependencyProcessor folderDependencyProcessor = new FolderDependencyProcessor();
    @InjectMocks
    JdbcConnectionDependencyProcessor jdbcConnectionDependencyProcessor = new JdbcConnectionDependencyProcessor();
    @InjectMocks
    SecurePasswordDependencyProcessor securePasswordDependencyProcessor = new SecurePasswordDependencyProcessor();
    @InjectMocks
    AssertionDependencyProcessor assertionDependencyProcessor = new AssertionDependencyProcessor();
    @InjectMocks
    ClusterPropertyDependencyProcessor clusterPropertyDependencyProcessor = new ClusterPropertyDependencyProcessor();
    @InjectMocks
    IdentityProviderProcessor identityProviderProcessor = new IdentityProviderProcessor();

    @Spy
    DependencyProcessorStore processorStore = new DependencyProcessorStore(CollectionUtils.MapBuilder.<Dependency.DependencyType, DependencyProcessor>builder()
            .put(Dependency.DependencyType.GENERIC, genericDependencyProcessor)
            .put(Dependency.DependencyType.POLICY, policyDependencyProcessor)
            .put(Dependency.DependencyType.FOLDER, folderDependencyProcessor)
            .put(Dependency.DependencyType.JDBC_CONNECTION, jdbcConnectionDependencyProcessor)
            .put(Dependency.DependencyType.SECURE_PASSWORD, securePasswordDependencyProcessor)
            .put(Dependency.DependencyType.ASSERTION, assertionDependencyProcessor)
            .put(Dependency.DependencyType.CLUSTER_PROPERTY, clusterPropertyDependencyProcessor)
            .put(Dependency.DependencyType.ID_PROVIDER_CONFIG, identityProviderProcessor)
            .map());

    @InjectMocks
    protected DependencyAnalyzer dependencyAnalyzer = new DependencyAnalyzerImpl();

    protected DependencyTestBaseClass() {
    }

    @Before
    public void beforeTests(){
    }

    protected void mockEntity(Entity entity, EntityHeader entityHeader) throws FindException {
        Mockito.when(entityCrud.find(Matchers.eq(entityHeader))).thenReturn(entity);
        if(entity instanceof IdentityProviderConfig){
            Mockito.when(identityProviderConfigManager.findByHeader(entityHeader)).thenReturn((IdentityProviderConfig) entity);
        } else if(entity instanceof SecurePassword){
            Mockito.when(securePasswordManager.findByUniqueName(entityHeader.getName())).thenReturn((SecurePassword) entity);
            Mockito.when(securePasswordManager.findByPrimaryKey(entityHeader.getOid())).thenReturn((SecurePassword) entity);
        }
    }
}
