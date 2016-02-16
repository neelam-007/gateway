package com.l7tech.external.assertions.apiportalintegration.server;

import com.l7tech.external.assertions.apiportalintegration.ProcessIncrementAssertion;
import com.l7tech.external.assertions.apiportalintegration.server.apikey.manager.ApiKey;
import com.l7tech.external.assertions.apiportalintegration.server.resource.ApplicationApi;
import com.l7tech.external.assertions.apiportalintegration.server.resource.ApplicationEntity;
import com.l7tech.external.assertions.apiportalintegration.server.resource.ApplicationJson;
import com.l7tech.gateway.common.task.ScheduledTask;
import com.l7tech.objectmodel.EntityManager;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.policy.GenericEntityHeader;
import com.l7tech.server.MockClusterPropertyManager;
import com.l7tech.server.PlatformTransactionManagerStub;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.server.entity.GenericEntityManager;
import com.l7tech.server.task.ScheduledTaskManager;
import com.l7tech.server.util.ApplicationEventProxy;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 * @author chean22, 1/21/2016
 */
@RunWith(MockitoJUnitRunner.class)
public class ServerProcessIncrementAssertionTest {
    private ServerProcessIncrementAssertion serverAssertion;
    private ProcessIncrementAssertion assertion;
    private PlatformTransactionManagerStub transactionManager = new PlatformTransactionManagerStub();
    private MockClusterPropertyManager clusterPropertyManager = new MockClusterPropertyManager();
    private List apiList;

    final long incrementStart = 1446503181273L;
    final long incrementEnd = 1446503181299L;

    @Mock
    private ApplicationContext applicationContext;
    @Mock
    private ApplicationEventProxy applicationEventProxy;
    @Mock
    private GenericEntityManager genericEntityManager;
    @Mock
    private EntityManager<ApiKey, GenericEntityHeader> entityManager;
    @Mock
    private PortalGenericEntityManager<ApiKey> portalGenericEntityManager;
    @Mock
    private ScheduledTaskManager scheduledTaskManager;

    @Before
    public void setup() throws Exception {
        when(applicationContext.getBean("applicationEventProxy", ApplicationEventProxy.class)).thenReturn(applicationEventProxy);
        when(applicationContext.getBean("genericEntityManager", GenericEntityManager.class)).thenReturn(genericEntityManager);
        when(applicationContext.getBean("transactionManager", PlatformTransactionManager.class)).thenReturn(transactionManager);
        when(applicationContext.getBean("clusterPropertyManager", ClusterPropertyManager.class)).thenReturn(clusterPropertyManager);
        when(applicationContext.getBean("scheduledTaskManager", ScheduledTaskManager.class)).thenReturn(scheduledTaskManager);
        assertion = new ProcessIncrementAssertion();
        serverAssertion = new ServerProcessIncrementAssertion(assertion, applicationContext);
        serverAssertion.setPortalGenericEntityManager(portalGenericEntityManager);

        ApiKey api = new ApiKey();
        api.setApplicationId("066f33d1-7e45-4434-be69-5aa7d20934e1");
        api.setName("test");
        ApiKey apiDelete = new ApiKey();
        apiDelete.setApplicationId("51432edf-db67-46c8-8a7b-4af57fbf8bd4");
        apiDelete.setName("deleteFail");
        apiList = Arrays.asList(api, apiDelete);

        ScheduledTask scheduledTask = new ScheduledTask();
        scheduledTask.setName("test");
        scheduledTask.setGoid(Goid.DEFAULT_GOID);
        scheduledTask.setCronExpression("*/20 * * * * ?");
        when(scheduledTaskManager.findByUniqueName(any(String.class))).thenReturn(scheduledTask);
    }

    @Test
    public void testApplicationAdd() throws Exception {
        Mockito.doReturn(apiList).when(portalGenericEntityManager).findAll();
        Mockito.doReturn(null).when(portalGenericEntityManager).add(any(ApiKey.class));
        ApplicationJson aj = new ApplicationJson();
        aj.setEntityType("APPLICATION");
        aj.setIncrementStart(incrementEnd);
        aj.setBulkSync(ServerIncrementalSyncCommon.BULK_SYNC_FALSE);
        aj.setNewOrUpdatedEntities(Arrays.asList(
                createApplicationEntity("9ecd3853-c9bb-417b-8a69-d75c53aeeb5f", "new1", "secret1"),
                createApplicationEntity("f40f9473-6867-4a16-9e3e-ddebb25bbf06", "new2", "secret2")));
        List<Map<String, String>> results = (List<Map<String, String>>) serverAssertion.applyChanges(aj);
        String json = serverAssertion.buildJsonPostBack(incrementStart, aj, results).replaceAll("\\s*", "");
        assertTrue(json.contains("\"incrementStatus\":\"ok\","));
        assertTrue(json.contains("\"incrementStart\":1446503181273,"));
        assertTrue(json.contains("\"incrementEnd\":1446503181299,"));
        assertTrue(json.contains("\"entityType\":\"APPLICATION\""));
        assertTrue(json.contains("\"bulkSync\":\"false\""));
        assertTrue(json.contains("{\\\"count\\\":\\\"2\\\",\\\"cron\\\":\\\"*/20****?\\\"}"));
        verify(portalGenericEntityManager, times(2)).add(any(ApiKey.class));
        verify(portalGenericEntityManager, times(0)).update(any(ApiKey.class));
    }

    @Test
    public void testApplicationAddFail() throws Exception {
        ApplicationJson aj = new ApplicationJson();
        aj.setEntityType("APPLICATION");
        aj.setIncrementStart(incrementEnd);
        aj.setBulkSync(ServerIncrementalSyncCommon.BULK_SYNC_FALSE);
        aj.setNewOrUpdatedEntities(Arrays.asList(createApplicationEntity("9ecd3853-c9bb-417b-8a69-d75c53aeeb5f", "new1", "secret1")));
        Mockito.doReturn(apiList).when(portalGenericEntityManager).findAll();
        Mockito.doThrow(ObjectModelException.class).when(portalGenericEntityManager).add(any(ApiKey.class));
        List<Map<String, String>> results = (List<Map<String, String>>) serverAssertion.applyChanges(aj);
        String json = serverAssertion.buildJsonPostBack(incrementStart, aj, results).replaceAll("\\s*", "");
        assertTrue(json.contains("\"incrementStatus\":\"error\","));
        assertTrue(json.contains("\"incrementStart\":1446503181273,"));
        assertTrue(json.contains("\"incrementEnd\":1446503181299,"));
        assertTrue(json.contains("\"entityType\":\"APPLICATION\""));
        assertTrue(json.contains("\"bulkSync\":\"false\""));
        assertTrue(json.contains("\"errorMessage\":"));
        verify(portalGenericEntityManager, times(1)).add(any(ApiKey.class));
        verify(portalGenericEntityManager, times(0)).update(any(ApiKey.class));
    }

    @Test
    public void testApplicationUpdate() throws Exception {
        Mockito.doReturn(apiList).when(portalGenericEntityManager).findAll();
        Mockito.doReturn(null).when(portalGenericEntityManager).update(any(ApiKey.class));
        ApplicationJson aj = new ApplicationJson();
        aj.setEntityType("APPLICATION");
        aj.setIncrementStart(incrementEnd);
        aj.setBulkSync(ServerIncrementalSyncCommon.BULK_SYNC_FALSE);
        aj.setNewOrUpdatedEntities(Arrays.asList(createApplicationEntity("066f33d1-7e45-4434-be69-5aa7d20934e1", "test", "secret1")));
        List<Map<String, String>> results = (List<Map<String, String>>) serverAssertion.applyChanges(aj);
        String json = serverAssertion.buildJsonPostBack(incrementStart, aj, results).replaceAll("\\s*", "");
        assertTrue(json.contains("\"incrementStatus\":\"ok\","));
        assertTrue(json.contains("\"incrementStart\":1446503181273,"));
        assertTrue(json.contains("\"incrementEnd\":1446503181299,"));
        assertTrue(json.contains("\"entityType\":\"APPLICATION\""));
        assertTrue(json.contains("\"bulkSync\":\"false\""));
        verify(portalGenericEntityManager, times(0)).add(any(ApiKey.class));
        verify(portalGenericEntityManager, times(1)).update(any(ApiKey.class));
    }

    @Test
    public void testApplicationUpdateFail() throws Exception {
        ApplicationJson aj = new ApplicationJson();
        aj.setEntityType("APPLICATION");
        aj.setIncrementStart(incrementEnd);
        aj.setBulkSync(ServerIncrementalSyncCommon.BULK_SYNC_FALSE);
        aj.setNewOrUpdatedEntities(Arrays.asList(createApplicationEntity("066f33d1-7e45-4434-be69-5aa7d20934e1", "test", "secret1")));
        Mockito.doReturn(apiList).when(portalGenericEntityManager).findAll();
        Mockito.doThrow(ObjectModelException.class).when(portalGenericEntityManager).update(any(ApiKey.class));
        List<Map<String, String>> results = (List<Map<String, String>>) serverAssertion.applyChanges(aj);
        String json = serverAssertion.buildJsonPostBack(incrementStart, aj, results).replaceAll("\\s*", "");
        assertTrue(json.contains("\"incrementStatus\":\"error\","));
        assertTrue(json.contains("\"incrementStart\":1446503181273,"));
        assertTrue(json.contains("\"incrementEnd\":1446503181299,"));
        assertTrue(json.contains("\"entityType\":\"APPLICATION\""));
        assertTrue(json.contains("\"bulkSync\":\"false\""));
        assertTrue(json.contains("\"errorMessage\":"));
        verify(portalGenericEntityManager, times(0)).add(any(ApiKey.class));
        verify(portalGenericEntityManager, times(1)).update(any(ApiKey.class));
    }

    @Test
    public void testApplicationDelete() throws Exception {
        ApplicationJson aj = new ApplicationJson();
        aj.setEntityType("APPLICATION");
        aj.setIncrementStart(incrementEnd);
        aj.setBulkSync(ServerIncrementalSyncCommon.BULK_SYNC_FALSE);
        aj.setDeletedIds(Arrays.asList("066f33d1-7e45-4434-be69-5aa7d20934e1"));
        Mockito.doReturn(apiList).when(portalGenericEntityManager).findAll();
        Mockito.doNothing().when(portalGenericEntityManager).delete(any(String.class));
        List<Map<String, String>> results = (List<Map<String, String>>) serverAssertion.applyChanges(aj);
        String json = serverAssertion.buildJsonPostBack(incrementStart, aj, results).replaceAll("\\s*", "");
        assertTrue(json.contains("\"incrementStatus\":\"ok\","));
        assertTrue(json.contains("\"incrementStart\":1446503181273,"));
        assertTrue(json.contains("\"incrementEnd\":1446503181299,"));
        assertTrue(json.contains("\"entityType\":\"APPLICATION\""));
        assertTrue(json.contains("\"bulkSync\":\"false\""));
        verify(portalGenericEntityManager, times(1)).delete(any(String.class));
        verify(portalGenericEntityManager, times(0)).add(any(ApiKey.class));
        verify(portalGenericEntityManager, times(0)).update(any(ApiKey.class));
    }

    @Test
    public void testApplicationDeleteFail() throws Exception {
        ApplicationJson aj = new ApplicationJson();
        aj.setEntityType("APPLICATION");
        aj.setIncrementStart(incrementEnd);
        aj.setBulkSync(ServerIncrementalSyncCommon.BULK_SYNC_FALSE);
        aj.setDeletedIds(Arrays.asList("51432edf-db67-46c8-8a7b-4af57fbf8bd4"));
        Mockito.doReturn(apiList).when(portalGenericEntityManager).findAll();
        Mockito.doThrow(ObjectModelException.class).when(portalGenericEntityManager).delete(any(String.class));
        List<Map<String, String>> results = (List<Map<String, String>>) serverAssertion.applyChanges(aj);
        String json = serverAssertion.buildJsonPostBack(incrementStart, aj, results).replaceAll("\\s*", "");
        assertTrue(json.contains("\"incrementStatus\":\"error\","));
        assertTrue(json.contains("\"incrementStart\":1446503181273,"));
        assertTrue(json.contains("\"incrementEnd\":1446503181299,"));
        assertTrue(json.contains("\"entityType\":\"APPLICATION\""));
        assertTrue(json.contains("\"bulkSync\":\"false\""));
        assertTrue(json.contains("\"errorMessage\":"));
        verify(portalGenericEntityManager, times(1)).delete(any(String.class));
        verify(portalGenericEntityManager, times(0)).add(any(ApiKey.class));
        verify(portalGenericEntityManager, times(0)).update(any(ApiKey.class));
    }

    @Test
    public void testApplicationBulk() throws Exception {
        ApplicationJson aj = new ApplicationJson();
        aj.setEntityType("APPLICATION");
        aj.setIncrementStart(incrementEnd);
        aj.setBulkSync(ServerIncrementalSyncCommon.BULK_SYNC_TRUE);
        aj.setNewOrUpdatedEntities(Arrays.asList(
                createApplicationEntity("9ecd3853-c9bb-417b-8a69-d75c53aeeb5f", "new1", "secret1"),
                createApplicationEntity("f40f9473-6867-4a16-9e3e-ddebb25bbf06", "new2", "secret2")));
        Mockito.doReturn(apiList).when(portalGenericEntityManager).findAll();
        Mockito.doReturn(null).when(portalGenericEntityManager).add(any(ApiKey.class));
        Mockito.doNothing().when(portalGenericEntityManager).delete(any(String.class));
        List<Map<String, String>> results = (List<Map<String, String>>) serverAssertion.applyChanges(aj);
        String json = serverAssertion.buildJsonPostBack(incrementStart, aj, results).replaceAll("\\s*", "");
        assertTrue(json.contains("\"incrementStatus\":\"ok\","));
        assertTrue(json.contains("\"incrementStart\":1446503181273,"));
        assertTrue(json.contains("\"incrementEnd\":1446503181299,"));
        assertTrue(json.contains("\"entityType\":\"APPLICATION\""));
        assertTrue(json.contains("\"bulkSync\":\"true\""));
        verify(portalGenericEntityManager, times(2)).delete(any(String.class));
        verify(portalGenericEntityManager, times(2)).add(any(ApiKey.class));
        verify(portalGenericEntityManager, times(0)).update(any(ApiKey.class));
    }

    @Test
    public void testApplicationBulkFail() throws Exception {
        ApplicationJson aj = new ApplicationJson();
        aj.setEntityType("APPLICATION");
        aj.setIncrementStart(incrementEnd);
        aj.setBulkSync(ServerIncrementalSyncCommon.BULK_SYNC_TRUE);
        aj.setNewOrUpdatedEntities(Arrays.asList(
                createApplicationEntity("9ecd3853-c9bb-417b-8a69-d75c53aeeb5f", "new1", "secret1"),
                createApplicationEntity("f40f9473-6867-4a16-9e3e-ddebb25bbf06", "new2", "secret2")));
        Mockito.doReturn(apiList).when(portalGenericEntityManager).findAll();
        Mockito.doThrow(ObjectModelException.class).when(portalGenericEntityManager).add(any(ApiKey.class));
        Mockito.doNothing().when(portalGenericEntityManager).delete(any(String.class));
        List<Map<String, String>> results = (List<Map<String, String>>) serverAssertion.applyChanges(aj);
        String json = serverAssertion.buildJsonPostBack(incrementStart, aj, results).replaceAll("\\s*", "");
        assertTrue(json.contains("\"incrementStatus\":\"error\","));
        assertTrue(json.contains("\"incrementStart\":1446503181273,"));
        assertTrue(json.contains("\"incrementEnd\":1446503181299,"));
        assertTrue(json.contains("\"entityType\":\"APPLICATION\""));
        assertTrue(json.contains("\"bulkSync\":\"true\""));
        verify(portalGenericEntityManager, times(0)).delete(any(String.class));
        verify(portalGenericEntityManager, times(1)).add(any(ApiKey.class));
        verify(portalGenericEntityManager, times(0)).update(any(ApiKey.class));
    }

    private ApplicationEntity createApplicationEntity(String id, String key, String secret) {
        ApplicationEntity appEntity = new ApplicationEntity();
        appEntity.setId(id);
        appEntity.setKey(key);
        appEntity.setSecret(secret);
        appEntity.setStatus("ENABLED");
        appEntity.setOrganizationId("de0d455c-89f9-4602-9494-20f6ae62c5d3");
        appEntity.setOrganizationName("org2");
        appEntity.setLabel("app1");
        appEntity.setOauthCallbackUrl(null);
        appEntity.setOauthScope("\\\\\\\\\\\\\\\\\\\\\\\\%^&*()");
        appEntity.setOauthType(null);
        ApplicationApi api = new ApplicationApi();
        api.setId("efb6f420-69da-49f6-bcd2-e283409e87fc");
        appEntity.getApis().add(api);
        return appEntity;
    }
}
