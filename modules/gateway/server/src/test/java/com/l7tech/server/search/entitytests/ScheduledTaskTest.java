package com.l7tech.server.search.entitytests;

import com.l7tech.gateway.common.task.JobStatus;
import com.l7tech.gateway.common.task.JobType;
import com.l7tech.gateway.common.task.ScheduledTask;
import com.l7tech.identity.IdentityProvider;
import com.l7tech.identity.User;
import com.l7tech.identity.UserManager;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.server.search.exceptions.CannotRetrieveDependenciesException;
import com.l7tech.server.search.objects.DependencySearchResults;
import com.l7tech.server.search.objects.DependentEntity;
import com.l7tech.server.search.processors.DependencyTestBaseClass;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mock;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * This was created: 6/12/13 as 4:55 PM
 */
public class ScheduledTaskTest extends DependencyTestBaseClass {

    AtomicLong idCount = new AtomicLong(1);

    @Mock
    private IdentityProviderFactory identityProviderFactory;

    @Test
    public void test() throws FindException, CannotRetrieveDependenciesException {

        SecurityZone securityZone = new SecurityZone();
        Goid securityZoneGoid = new Goid(0, idCount.getAndIncrement());
        securityZone.setGoid(securityZoneGoid);
        mockEntity(securityZone, new EntityHeader(securityZoneGoid, EntityType.SECURITY_ZONE, null, null));

        final String POLICY_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\"/>\n" +
                "</wsp:Policy>";
        Policy policy = new Policy(PolicyType.POLICY_BACKED_OPERATION, "Policy 1",
                POLICY_XML,
                false
        );
        Goid policyGoid = new Goid(0, idCount.getAndIncrement());
        policy.setGoid(policyGoid);
        policy.setFolder(new Folder("Root Folder", null));
        policy.setInternalTag("com.l7tech.objectmodel.polback.BackgroundTask");
        policy.setInternalSubTag("run");
        policy.setGuid(UUID.randomUUID().toString());
        policy.setSoap(true);
        mockEntity(policy, new EntityHeader(policyGoid, EntityType.POLICY, null, null));

        final Goid identityProviderOid = new Goid(0, idCount.getAndIncrement());
        IdentityProvider identityProvider = mock(IdentityProvider.class);
        UserManager userManager = mock(UserManager.class);
        User user = mock(User.class);
        when(identityProviderFactory.getProvider(identityProviderOid)).thenReturn(identityProvider);
        when(identityProvider.getUserManager()).thenReturn(userManager);
        when(userManager.findByPrimaryKey("userId")).thenReturn(user);
        when(user.getProviderId()).thenReturn(identityProviderOid);
        when(user.getId()).thenReturn("userId");

        ScheduledTask scheduledTask = new ScheduledTask();
        final Goid scheduledTaskOid = new Goid(0, idCount.getAndIncrement());
        scheduledTask.setGoid(scheduledTaskOid);
        scheduledTask.setSecurityZone(securityZone);
        scheduledTask.setName("Scheduled Task 1");
        scheduledTask.setPolicyGoid(policyGoid);
        scheduledTask.setJobType(JobType.ONE_TIME);
        scheduledTask.setJobStatus(JobStatus.COMPLETED);
        scheduledTask.setExecutionDate(System.currentTimeMillis());
        scheduledTask.setUseOneNode(true);
        scheduledTask.setSecurityZone(securityZone);
        scheduledTask.setIdProviderGoid(identityProviderOid);
        scheduledTask.setUserId("userId");

        final EntityHeader scheduledTaskHeader = new EntityHeader(scheduledTaskOid, EntityType.SCHEDULED_TASK, null, null);

        mockEntity(scheduledTask, scheduledTaskHeader);

        DependencySearchResults result = dependencyAnalyzer.getDependencies(scheduledTaskHeader);

        Assert.assertNotNull(result);
        Assert.assertEquals(scheduledTaskOid, new Goid(((DependentEntity) result.getDependent()).getEntityHeader().getStrId()));
        Assert.assertEquals(EntityType.SCHEDULED_TASK, ((DependentEntity) result.getDependent()).getDependencyType().getEntityType());
        Assert.assertNotNull(result.getDependencies());
        Assert.assertEquals(3, result.getDependencies().size());
        Assert.assertEquals(policyGoid.toString(), ((DependentEntity) result.getDependencies().get(0).getDependent()).getEntityHeader().getStrId());
        Assert.assertEquals(EntityType.POLICY, ((DependentEntity) result.getDependencies().get(0).getDependent()).getDependencyType().getEntityType());
        Assert.assertEquals(securityZoneGoid.toString(), ((DependentEntity) result.getDependencies().get(1).getDependent()).getEntityHeader().getStrId());
        Assert.assertEquals(EntityType.SECURITY_ZONE, ((DependentEntity) result.getDependencies().get(1).getDependent()).getDependencyType().getEntityType());
        Assert.assertEquals("userId", ((DependentEntity) result.getDependencies().get(2).getDependent()).getEntityHeader().getStrId());
        Assert.assertEquals(identityProviderOid, ((IdentityHeader) ((DependentEntity) result.getDependencies().get(2).getDependent()).getEntityHeader()).getProviderGoid());
        Assert.assertEquals(EntityType.USER, ((DependentEntity) result.getDependencies().get(2).getDependent()).getDependencyType().getEntityType());
    }

}
