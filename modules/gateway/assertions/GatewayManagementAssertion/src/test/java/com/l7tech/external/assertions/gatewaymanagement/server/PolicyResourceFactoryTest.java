package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.gateway.api.*;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.server.policy.PolicyManager;
import com.l7tech.server.policy.PolicyVersionManager;
import com.l7tech.server.security.rbac.RbacServices;
import com.l7tech.server.security.rbac.SecurityFilter;
import com.l7tech.server.security.rbac.SecurityZoneManager;
import com.l7tech.server.service.ServiceManager;
import com.l7tech.test.BugId;
import com.l7tech.util.Option;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

/**
 * @author alee, 2/20/2015
 */
@RunWith(MockitoJUnitRunner.class)
public class PolicyResourceFactoryTest {
    private PolicyResourceFactory factory;
    @Mock
    private RbacServices rbacServices;
    @Mock
    private SecurityFilter securityFilter;
    @Mock
    private PlatformTransactionManager transactionManager;
    @Mock
    private SecurityZoneManager securityZoneManager;
    @Mock
    private PolicyManager policyManager;
    @Mock
    private PolicyHelper policyHelper;
    @Mock
    private FolderResourceFactory folderResourceFactory;
    @Mock
    private PolicyVersionManager policyVersionManager;
    @Mock
    private ClusterPropertyManager clusterPropertyManager;
    @Mock
    private ServiceManager serviceManager;
    private Folder rootFolder = new Folder("Root", null);

    @Before
    public void setup() {
        factory = new PolicyResourceFactory(rbacServices, securityFilter, transactionManager, policyManager,
                policyHelper, folderResourceFactory, securityZoneManager, policyVersionManager,
                clusterPropertyManager, serviceManager);
        when(folderResourceFactory.getFolder(Option.some(Folder.ROOT_FOLDER_ID.toString()))).thenReturn(Option.some(rootFolder));
    }

    @BugId("SSG-10853")
    @Test
    public void asResourcePolicyBackedService() {
        final Policy policyBackedServicePolicy = new Policy(PolicyType.POLICY_BACKED_OPERATION, "test", "", false);
        final PolicyMO mo = factory.asResource(policyBackedServicePolicy);
        assertEquals(PolicyDetail.PolicyType.SERVICE_OPERATION, mo.getPolicyDetail().getPolicyType());
    }

    @BugId("SSG-10853")
    @Test
    public void fromResourcePolicyBackedService() throws Exception {
        final PolicyMO mo = createPolicyMO();
        final Policy policy = factory.fromResource(mo, false);
        assertEquals(PolicyType.POLICY_BACKED_OPERATION, policy.getType());
    }

    private PolicyMO createPolicyMO() {
        final PolicyMO mo = ManagedObjectFactory.createPolicy();
        final PolicyDetail detail = ManagedObjectFactory.createPolicyDetail();
        detail.setFolderId(Folder.ROOT_FOLDER_ID.toString());
        detail.setPolicyType(PolicyDetail.PolicyType.SERVICE_OPERATION);
        mo.setPolicyDetail(detail);
        final List<ResourceSet> resources = new ArrayList<>();
        final ResourceSet resourceSet = ManagedObjectFactory.createResourceSet();
        resourceSet.setTag("policy");
        final Resource resource = ManagedObjectFactory.createResource();
        resource.setType("policy");
        resourceSet.setResources(Collections.singletonList(resource));
        resources.add(resourceSet);
        mo.setResourceSets(resources);
        return mo;
    }
}
