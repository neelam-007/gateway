package com.l7tech.external.assertions.apiportalintegration.server;

import com.l7tech.external.assertions.apiportalintegration.ProcessIncrementAssertion;
import com.l7tech.external.assertions.apiportalintegration.server.apikey.manager.ApiKey;
import com.l7tech.external.assertions.apiportalintegration.server.resource.ApplicationApi;
import com.l7tech.external.assertions.apiportalintegration.server.resource.ApplicationEntity;
import com.l7tech.external.assertions.apiportalintegration.server.resource.ApplicationJson;
import com.l7tech.objectmodel.EntityManager;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.GenericEntityHeader;
import com.l7tech.server.MockClusterPropertyManager;
import com.l7tech.server.PlatformTransactionManagerStub;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.server.entity.GenericEntityManager;
import com.l7tech.server.util.ApplicationEventProxy;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

/**
 * @author chean22, 1/21/2016
 */
@RunWith(MockitoJUnitRunner.class)
public class ServerProcessIncrementAssertionTest {
    private ServerProcessIncrementAssertion serverAssertion;
    private ProcessIncrementAssertion assertion;
    private PlatformTransactionManagerStub transactionManager = new PlatformTransactionManagerStub();
    private MockClusterPropertyManager clusterPropertyManager = new MockClusterPropertyManager();
    @Mock
    private ApplicationContext applicationContext;
    @Mock
    private ApplicationEventProxy applicationEventProxy;
    @Mock
    private GenericEntityManager genericEntityManager;
    @Mock
    private EntityManager<ApiKey, GenericEntityHeader> entityManager;

    //
    // Due to how the EntityManager works, these tests are intentionally put together in one method in order to work properly.
    //
    @Test
    public void testAll() throws Exception {
        when(applicationContext.getBean("applicationEventProxy", ApplicationEventProxy.class)).thenReturn(applicationEventProxy);
        when(applicationContext.getBean("genericEntityManager", GenericEntityManager.class)).thenReturn(genericEntityManager);
        when(applicationContext.getBean("transactionManager", PlatformTransactionManager.class)).thenReturn(transactionManager);
        when(applicationContext.getBean("clusterPropertyManager", ClusterPropertyManager.class)).thenReturn(clusterPropertyManager);
        when(genericEntityManager.getEntityManager(ApiKey.class)).thenReturn(entityManager);
        assertion = new ProcessIncrementAssertion();
        serverAssertion = new ServerProcessIncrementAssertion(assertion, applicationContext);
        when(entityManager.save(any(ApiKey.class))).thenReturn(new Goid(0, 1234L), null);

        ApiKey api = new ApiKey();
        api.setApplicationId("066f33d1-7e45-4434-be69-5aa7d20934e1");
        api.setName("test");
        ApiKey apiDelete = new ApiKey();
        apiDelete.setApplicationId("066f33d1-7e45-4434-be69-5aa7d20934e1");
        apiDelete.setName("deleteFail");

        when(entityManager.findAll()).thenReturn(
                Arrays.asList(api),
                Arrays.asList(api),
                Arrays.asList(api),
                Arrays.asList(apiDelete));

        // test add
        final long incrementStart = 1446503181273L;
        final long incrementEnd = 1446503181299L;
        ApplicationJson aj = new ApplicationJson();
        aj.setEntityType("APPLICATION");
        aj.setIncrementStart(incrementEnd);
        aj.setNewOrUpdatedEntities(createApplicationEntities());
        List<Map<String, String>> results = (List<Map<String, String>>) serverAssertion.applyChanges(aj);
        String json = serverAssertion.buildJsonPostBack(incrementStart, aj, results).replaceAll("\\s*", "");
        assertTrue(json.contains("\"incrementStatus\":\"ok\","));
        assertTrue(json.contains("\"incrementStart\":1446503181273,"));
        assertTrue(json.contains("\"incrementEnd\":1446503181299,"));
        assertTrue(json.contains("\"entityType\":\"APPLICATION\""));

        // test add fail
        aj = new ApplicationJson();
        aj.setEntityType("APPLICATION");
        aj.setIncrementStart(incrementEnd);
        aj.setNewOrUpdatedEntities(createApplicationEntities());
        results = (List<Map<String, String>>) serverAssertion.applyChanges(aj);
        json = serverAssertion.buildJsonPostBack(incrementStart, aj, results).replaceAll("\\s*", "");
        assertTrue(json.contains("\"incrementStatus\":\"error\","));
        assertTrue(json.contains("\"incrementStart\":1446503181273,"));
        assertTrue(json.contains("\"incrementEnd\":1446503181299,"));
        assertTrue(json.contains("\"entityType\":\"APPLICATION\""));
        assertTrue(json.contains("\"errorMessage\":"));

        // test delete
        when(entityManager.findByUniqueName("test")).thenReturn(new ApiKey());
        doNothing().when(entityManager).delete(any(ApiKey.class));
        aj = new ApplicationJson();
        aj.setEntityType("APPLICATION");
        aj.setIncrementStart(incrementEnd);
        aj.setDeletedIds(Arrays.asList("066f33d1-7e45-4434-be69-5aa7d20934e1"));
        results = (List<Map<String, String>>) serverAssertion.applyChanges(aj);
        json = serverAssertion.buildJsonPostBack(incrementStart, aj, results).replaceAll("\\s*", "");
        assertTrue(json.contains("\"incrementStatus\":\"ok\","));
        assertTrue(json.contains("\"incrementStart\":1446503181273,"));
        assertTrue(json.contains("\"incrementEnd\":1446503181299,"));
        assertTrue(json.contains("\"entityType\":\"APPLICATION\""));

        // test delete fail
        when(entityManager.findByUniqueName("deleteFail")).thenReturn(null);
        aj = new ApplicationJson();
        aj.setEntityType("APPLICATION");
        aj.setIncrementStart(incrementEnd);
        aj.setDeletedIds(Arrays.asList("066f33d1-7e45-4434-be69-5aa7d20934e1"));
        results = (List<Map<String, String>>) serverAssertion.applyChanges(aj);
        json = serverAssertion.buildJsonPostBack(incrementStart, aj, results).replaceAll("\\s*", "");
        assertTrue(json.contains("\"incrementStatus\":\"error\","));
        assertTrue(json.contains("\"incrementStart\":1446503181273,"));
        assertTrue(json.contains("\"incrementEnd\":1446503181299,"));
        assertTrue(json.contains("\"entityType\":\"APPLICATION\""));
        assertTrue(json.contains("\"errorMessage\":"));
        //assertTrue(json.contains("\"entityErrors\""));
        //assertTrue(json.contains("\"msg\":\"Databasetransactionfailed\""));
    }

    private List<ApplicationEntity> createApplicationEntities() {
        List list = new ArrayList<>();
        ApplicationEntity appEntity = new ApplicationEntity();
        appEntity.setId("9ecd3853-c9bb-417b-8a69-d75c53aeeb5f");
        appEntity.setKey("l7xxab6a89e9598c42809d99debecba8c576");
        appEntity.setSecret("4e5ac053df584ef08e7f5232aed1c26f");
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
        list.add(appEntity);
        return list;
    }
}
