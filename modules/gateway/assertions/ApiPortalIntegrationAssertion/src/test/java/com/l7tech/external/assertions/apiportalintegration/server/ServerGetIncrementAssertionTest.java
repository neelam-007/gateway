package com.l7tech.external.assertions.apiportalintegration.server;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import com.l7tech.external.assertions.apiportalintegration.GetIncrementAssertion;
import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.jdbc.JdbcConnectionManager;
import com.l7tech.server.jdbc.JdbcQueryingManager;
import com.l7tech.server.message.PolicyEnforcementContext;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;import com.l7tech.test.BugId;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationContext;


/**
 * @author chean22, 1/15/2016
 */
public class ServerGetIncrementAssertionTest {
    public static final String MASTER_TENANT_INFO = "Master Tenant Info";
    public static final String TENANT_ID = "tenant1";
    private ServerGetIncrementAssertion serverAssertion;
    private GetIncrementAssertion assertion;
    private ApplicationContext applicationContext;
    private JdbcQueryingManager queryingManager;
    private JdbcConnectionManager jdbcConnectionManager;
    private PolicyEnforcementContext pec;

    @Before
    public void setup() throws Exception {
        applicationContext = mock(ApplicationContext.class);
        queryingManager = mock(JdbcQueryingManager.class);
        pec = mock(PolicyEnforcementContext.class);
        jdbcConnectionManager = mock(JdbcConnectionManager.class);
        when(applicationContext.getBean("jdbcQueryingManager", JdbcQueryingManager.class)).thenReturn(queryingManager);
        when(applicationContext.getBean("jdbcConnectionManager", JdbcConnectionManager.class)).thenReturn(jdbcConnectionManager);

        assertion = new GetIncrementAssertion();
        serverAssertion = new ServerGetIncrementAssertion(assertion, applicationContext);
    }

    @Test
    public void testCheckRequestWithApiPlansAndVerifyJdbcQueries() throws FindException, IOException, PolicyAssertionException {
      Map<String, Object> vars = mock(Map.class);
      when(vars.get(eq("portal.sync.increment.jdbc"))).thenReturn(MASTER_TENANT_INFO);
      when(vars.get(eq("portal.sync.increment.nodeId"))).thenReturn("abc");
      when(vars.get(eq("portal.sync.increment.tenantId"))).thenReturn(TENANT_ID);
      when(vars.get(eq("portal.sync.increment.type"))).thenReturn(ServerIncrementalSyncCommon.ENTITY_TYPE_APPLICATION);
      when(vars.get(eq("portal.sync.increment.since"))).thenReturn("0");
      when(vars.get(eq("portal.sync.increment.apps.with.api.plans"))).thenReturn("true");
      when(pec.getVariableMap(any(String[].class), any(Auditor.class))).thenReturn(vars);
      when(jdbcConnectionManager.getJdbcConnection(anyString())).thenReturn(mock(JdbcConnection.class));
      ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
      ArgumentCaptor<List> queryArgumentsCaptor = ArgumentCaptor.forClass(List.class);
      when(queryingManager.performJdbcQuery(anyString(), queryCaptor.capture(), anyString(), anyInt(), anyInt(), queryArgumentsCaptor.capture()))
                .thenReturn(buildDeletedAppResults(), new HashMap<>(), buildResultsMap(), buildCustomFieldsResults());
      AssertionStatus result = serverAssertion.checkRequest(pec);
      assertEquals(AssertionStatus.NONE, result);

      verify(queryingManager, times(2)).performJdbcQuery(anyString(), anyString(), anyString(), anyInt(), anyInt(), anyListOf(Object.class));
      //verify api plans specific query used for fetching apps with apis
      String appsWithApisQuery = queryCaptor.getAllValues().get(1);
      assertTrue(appsWithApisQuery.contains("DISTINCT aaapx.API_UUID,aaapx.API_PLAN_UUID,a.UUID,concat(a.NAME," +
              " '-', o.NAME) as NAME,a.API_KEY,a.KEY_SECRET,coalesce(r.PREVIOUS_STATE, a.STATUS) as " +
              "STATUS,a.ORGANIZATION_UUID,o.NAME as ORGANIZATION_NAME,a.OAUTH_CALLBACK_URL,a" +
              ".OAUTH_SCOPE,a.OAUTH_TYPE,a.MAG_SCOPE,a.MAG_MASTER_KEY,a.CREATED_BY,a" +
              ".MODIFIED_BY"));
      assertEquals(7, queryArgumentsCaptor.getAllValues().get(1).size());
    }

    @Test
    public void testCheckRequestWithoutApiPlansAndVerifyJdbcQueries() throws FindException, IOException, PolicyAssertionException {
      Map<String, Object> vars = mock(Map.class);
      when(vars.get(eq("portal.sync.increment.jdbc"))).thenReturn(MASTER_TENANT_INFO);
      when(vars.get(eq("portal.sync.increment.nodeId"))).thenReturn("abc");
      when(vars.get(eq("portal.sync.increment.tenantId"))).thenReturn(TENANT_ID);
      when(vars.get(eq("portal.sync.increment.type"))).thenReturn(ServerIncrementalSyncCommon.ENTITY_TYPE_APPLICATION);
      when(vars.get(eq("portal.sync.increment.since"))).thenReturn("0");
      when(vars.get(eq("portal.sync.increment.apps.with.api.plans"))).thenReturn("false");
      when(pec.getVariableMap(any(String[].class), any(Auditor.class))).thenReturn(vars);
      when(jdbcConnectionManager.getJdbcConnection(anyString())).thenReturn(mock(JdbcConnection.class));
      ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
      ArgumentCaptor<List> queryArgumentsCaptor = ArgumentCaptor.forClass(List.class);
      when(queryingManager.performJdbcQuery(anyString(), queryCaptor.capture(), anyString(), anyInt(), anyInt(), queryArgumentsCaptor.capture()))
                .thenReturn(buildDeletedAppResults(), new HashMap<>(), buildResultsMap(), buildCustomFieldsResults());
      AssertionStatus result = serverAssertion.checkRequest(pec);
      assertEquals(AssertionStatus.NONE, result);

      verify(queryingManager, times(4)).performJdbcQuery(anyString(), anyString(), anyString(), anyInt(), anyInt(), anyListOf(Object.class));
      //verify api group specific query used for app with no apis
      assertEquals(ServerIncrementalSyncCommon.SELECT_APP_WITH_NO_API_SQL, queryCaptor.getAllValues().get(1));
      //verify api group specific query used for fetching apps with apis
      String appsWithApisQuery = queryCaptor.getAllValues().get(2);
      assertTrue(appsWithApisQuery.contains("DISTINCT aaagx.API_UUID,a.UUID,concat(a.NAME," +
              " '-', o.NAME) as NAME,a.API_KEY,a.KEY_SECRET,coalesce(r.PREVIOUS_STATE, a.STATUS) as " +
              "STATUS,a.ORGANIZATION_UUID,o.NAME as ORGANIZATION_NAME,a.OAUTH_CALLBACK_URL,a" +
              ".OAUTH_SCOPE,a.OAUTH_TYPE,a.MAG_SCOPE,a.MAG_MASTER_KEY,a.CREATED_BY,a" +
              ".MODIFIED_BY"));
      assertEquals(11, queryArgumentsCaptor.getAllValues().get(2).size());
    }

    @Test
    public void testGetJsonMessage() throws Exception {
        final String ref = "{\n" +
                "  \"incrementStart\" : ,\n" +
                "  \"entityType\" : \"APPLICATION\",\n" +
                "  \"bulkSync\" : \"false\",\n" +
                "  \"deletedIds\" : [ \"3c2acfb5-8803-4c0c-8de3-a9224cad2595\", \"066f33d1-7e45-4434-be69-5aa7d20934e1\" ],\n" +
                "  \"newOrUpdatedEntities\" : [ {\n" +
                "    \"id\" : \"085f4526-9c23-416d-be73-eaba4da83249\",\n" +
                "    \"key\" : \"l7xx2738fab70d824c059f28a922a1edab15\",\n" +
                "    \"secret\" : \"6106dceac26844f0b1d9688bb565acf6\",\n" +
                "    \"status\" : \"active\",\n" +
                "    \"organizationId\" : \"4c35f9cd-8eb2-11e3-ae6b-000c2911a4db\",\n" +
                "    \"organizationName\" : \"Sample Org\",\n" +
                "    \"label\" : \"app1\",\n" +
                "    \"oauthCallbackUrl\" : \"https://some-uri.com\",\n" +
                "    \"oauthScope\" : \"\\\\\\\\\\\\\\\\\\\\\\\\%^&*()\",\n" +
                "    \"apis\" : [ {\r\n" +
                "      \"id\" : \"efb6f420-69da-49f6-bcd2-e283409e87fc\"\n" +
                "    } ],\n" +
                "    \"mag\" : {\n" +
                "      \"scope\" : \"\\\\\\\\\\\\\\\\\\\\\\\\%^&*() msso openid\",\n" +
                "      \"redirectUri\" : \"https://some-uri.com\",\n" +
                "      \"masterKeys\" : [ {\n" +
                "        \"masterKey\" : \"f08985a0-e164-11e5-b86d-9a79f06e9478\",\n" +
                "        \"environment\" : \"all\"\n" +
                "      } ]\n" +
                "    },\n" +
                "    \"createdBy\" : \"admin\",\n" +
                "    \"modifiedBy\" : \"user1\",\n" +
                "    \"custom\" : \"{\\\"TEST2\\\":\\\"1234\\\",\\\"TEST1\\\":\\\"123\\\"}\"" +
                "  } ]\n" +
                "}";

        when(queryingManager.performJdbcQuery(anyString(), anyString(), anyString(), anyInt(), anyInt(), anyListOf(Object.class)))
                .thenReturn(buildDeletedAppResults(), new HashMap<>(), buildResultsMap(), buildCustomFieldsResults());

        String json = serverAssertion.getJsonMessage("conn", "1446501119477", "","", false);
        // remove timestamp for comparison
        assertEquals(json.replaceFirst("\\d{13}", "").replaceAll("\\s+", ""), ref.replaceAll("\\s+", ""));
    }

    @Test
    public void testGetJsonMessageWithApiPlans() throws Exception {
        final String ref = "{\n" +
                "  \"incrementStart\" : ,\n" +
                "  \"entityType\" : \"APPLICATION\",\n" +
                "  \"bulkSync\" : \"false\",\n" +
                "  \"deletedIds\" : [ \"3c2acfb5-8803-4c0c-8de3-a9224cad2595\", \"066f33d1-7e45-4434-be69-5aa7d20934e1\" ],\n" +
                "  \"newOrUpdatedEntities\" : [ {\n" +
                "    \"id\" : \"085f4526-9c23-416d-be73-eaba4da83249\",\n" +
                "    \"key\" : \"l7xx2738fab70d824c059f28a922a1edab15\",\n" +
                "    \"secret\" : \"6106dceac26844f0b1d9688bb565acf6\",\n" +
                "    \"status\" : \"active\",\n" +
                "    \"organizationId\" : \"4c35f9cd-8eb2-11e3-ae6b-000c2911a4db\",\n" +
                "    \"organizationName\" : \"Sample Org\",\n" +
                "    \"label\" : \"app1\",\n" +
                "    \"oauthCallbackUrl\" : \"https://some-uri.com\",\n" +
                "    \"oauthScope\" : \"\\\\\\\\\\\\\\\\\\\\\\\\%^&*()\",\n" +
                "    \"apis\" : [ {\r\n" +
                "      \"id\" : \"efb6f420-69da-49f6-bcd2-e283409e87fc\",\n" +
                "      \"planId\" : \"efb6f420-69da-49f6-bcd2-e283409e87fc\"\n" +
                "    } ],\n" +
                "    \"mag\" : {\n" +
                "      \"scope\" : \"\\\\\\\\\\\\\\\\\\\\\\\\%^&*() msso openid\",\n" +
                "      \"redirectUri\" : \"https://some-uri.com\",\n" +
                "      \"masterKeys\" : [ {\n" +
                "        \"masterKey\" : \"f08985a0-e164-11e5-b86d-9a79f06e9478\",\n" +
                "        \"environment\" : \"all\"\n" +
                "      } ]\n" +
                "    },\n" +
                "    \"createdBy\" : \"admin\",\n" +
                "    \"modifiedBy\" : \"user1\",\n" +
                "    \"custom\" : \"{\\\"TEST2\\\":\\\"1234\\\",\\\"TEST1\\\":\\\"123\\\"}\"" +
                "  } ]\n" +
                "}";

        when(queryingManager.performJdbcQuery(anyString(), anyString(), anyString(), anyInt(), anyInt(), anyListOf(Object.class)))
                .thenReturn(buildDeletedAppResults(), buildResultsMap(), buildCustomFieldsResults());

        String json = serverAssertion.getJsonMessage("conn", "1446501119477", "","", true);
        // remove timestamp for comparison
        assertEquals(json.replaceFirst("\\d{13}", "").replaceAll("\\s+", ""), ref.replaceAll("\\s+", ""));
    }

  @Test
  public void testGetJsonMessageWithAppWithNoApi() throws Exception {
    final String ref = "{\n" +
        "  \"incrementStart\" : ,\n" +
        "  \"entityType\" : \"APPLICATION\",\n" +
        "  \"bulkSync\" : \"false\",\n" +
        "  \"deletedIds\" : [ \"app with no api\", \"3c2acfb5-8803-4c0c-8de3-a9224cad2595\", \"066f33d1-7e45-4434-be69-5aa7d20934e1\" ],\n" +
        "  \"newOrUpdatedEntities\" : [ {\n" +
        "    \"id\" : \"085f4526-9c23-416d-be73-eaba4da83249\",\n" +
        "    \"key\" : \"l7xx2738fab70d824c059f28a922a1edab15\",\n" +
        "    \"secret\" : \"6106dceac26844f0b1d9688bb565acf6\",\n" +
        "    \"status\" : \"active\",\n" +
        "    \"organizationId\" : \"4c35f9cd-8eb2-11e3-ae6b-000c2911a4db\",\n" +
        "    \"organizationName\" : \"Sample Org\",\n" +
        "    \"label\" : \"app1\",\n" +
        "    \"oauthCallbackUrl\" : \"https://some-uri.com\",\n" +
        "    \"oauthScope\" : \"\\\\\\\\\\\\\\\\\\\\\\\\%^&*()\",\n" +
        "    \"apis\" : [ {\r\n" +
        "      \"id\" : \"efb6f420-69da-49f6-bcd2-e283409e87fc\"\n" +
        "    } ],\n" +
        "    \"mag\" : {\n" +
        "      \"scope\" : \"\\\\\\\\\\\\\\\\\\\\\\\\%^&*() msso openid\",\n" +
        "      \"redirectUri\" : \"https://some-uri.com\",\n" +
        "      \"masterKeys\" : [ {\n" +
        "        \"masterKey\" : \"f08985a0-e164-11e5-b86d-9a79f06e9478\",\n" +
        "        \"environment\" : \"all\"\n" +
        "      } ]\n" +
        "    },\n" +
        "    \"createdBy\" : \"admin\",\n" +
        "    \"modifiedBy\" : \"user1\",\n" +
        "    \"custom\" : \"{\\\"TEST2\\\":\\\"1234\\\",\\\"TEST1\\\":\\\"123\\\"}\"" +
        "  } ]\n" +
        "}";

    Map<String, List> appWithNoApiResults = new HashMap<>();
    appWithNoApiResults.put("uuid", Arrays.asList("app with no api"));

    when(queryingManager.performJdbcQuery(anyString(), anyString(), anyString(), anyInt(), anyInt(), anyListOf(Object.class)))
        .thenReturn(buildDeletedAppResults(), appWithNoApiResults, buildResultsMap(), buildCustomFieldsResults());

    String json = serverAssertion.getJsonMessage("conn", "1446501119477", "","", false);
    // remove timestamp for comparison
    assertEquals(json.replaceFirst("\\d{13}", "").replaceAll("\\s+", ""), ref.replaceAll("\\s+", ""));
  }

    private Map<String, List> buildResultsMap() {
        List urlList = new ArrayList();
        urlList.add("https://some-uri.com");
        List typeList = new ArrayList();
        typeList.add(null);

        Map<String, List> results = new HashMap<>();
        results.put("uuid", Arrays.asList("085f4526-9c23-416d-be73-eaba4da83249"));
        results.put("api_key", Arrays.asList("l7xx2738fab70d824c059f28a922a1edab15"));
        results.put("key_secret", Arrays.asList("6106dceac26844f0b1d9688bb565acf6"));
        results.put("status", Arrays.asList("ENABLED"));
        results.put("organization_uuid", Arrays.asList("4c35f9cd-8eb2-11e3-ae6b-000c2911a4db"));
        results.put("organization_name", Arrays.asList("Sample Org"));
        results.put("name", Arrays.asList("app1"));
        results.put("oauth_callback_url", urlList);
        results.put("oauth_scope", Arrays.asList("\\\\\\\\\\\\%^&*()"));
        results.put("oauth_type", typeList);
        results.put("api_uuid", Arrays.asList("efb6f420-69da-49f6-bcd2-e283409e87fc"));
        results.put("api_plan_uuid", Arrays.asList("efb6f420-69da-49f6-bcd2-e283409e87fc"));
        results.put("mag_scope", Arrays.asList("msso openid"));
        results.put("mag_master_key", Arrays.asList("f08985a0-e164-11e5-b86d-9a79f06e9478"));
        results.put("created_by", Arrays.asList("admin"));
        results.put("modified_by", Arrays.asList("user1"));
        return results;
    }

    private Map<String, List> buildDeletedAppResults() {
      Map<String, List> results = new HashMap<>();
      results.put("entity_uuid", Arrays.asList("3c2acfb5-8803-4c0c-8de3-a9224cad2595", "066f33d1-7e45-4434-be69-5aa7d20934e1"));
      return results;
    }

    private Map<String, List> buildCustomFieldsResults() {
      List urlList = new ArrayList();
      urlList.add(null);
      List typeList = new ArrayList();
      typeList.add(null);

      Map<String, List> results = new HashMap<>();
      results.put("entity_uuid", Arrays.asList("085f4526-9c23-416d-be73-eaba4da83249","085f4526-9c23-416d-be73-eaba4da83249"));
      results.put("system_property_name", Arrays.asList("TEST1","TEST2"));
      results.put("value", Arrays.asList("123","1234"));
      return results;
    }

    @Test
    public void testGetJsonMessageWithMultipleApis() throws Exception {
        final String ref = "{\n" +
                "  \"incrementStart\" : ,\n" +
                "  \"entityType\" : \"APPLICATION\",\n" +
                "  \"bulkSync\" : \"true\",\n" +
                "  \"newOrUpdatedEntities\" : [ {\n" +
                "    \"id\" : \"085f4526-9c23-416d-be73-eaba4da83249\",\n" +
                "    \"key\" : \"l7xx2738fab70d824c059f28a922a1edab15\",\n" +
                "    \"secret\" : \"6106dceac26844f0b1d9688bb565acf6\",\n" +
                "    \"status\" : \"active\",\n" +
                "    \"organizationId\" : \"4c35f9cd-8eb2-11e3-ae6b-000c2911a4db\",\n" +
                "    \"organizationName\" : \"Sample Org\",\n" +
                "    \"label\" : \"app1\",\n" +
                "    \"oauthCallbackUrl\" : \"https://some-uri1.com\",\n" +
                "    \"oauthScope\" : \"\\\\\\\\\\\\\\\\\\\\\\\\%^&*()\",\n" +
                "    \"apis\" : [ {\r\n" +
                "      \"id\" : \"efb6f420-69da-49f6-bcd2-e283409e87fc\"\n" +
                "    }, {\n" +
                "      \"id\" : \"efb6f420-69da-49f6-bcd2-e283409e87fc\"\n" +
                "    } ],\n" +
                "    \"mag\" : {\n" +
                "      \"scope\" : \"\\\\\\\\\\\\\\\\\\\\\\\\%^&*() msso openid\",\n" +
                "      \"redirectUri\" : \"https://some-uri1.com\",\n" +
                "      \"masterKeys\" : [ {\n" +
                "        \"masterKey\" : \"f08985a0-e164-11e5-b86d-9a79f06e9478\",\n" +
                "        \"environment\" : \"all\"\n" +
                "      } ]\n" +
                "    },\n" +
                "    \"createdBy\" : \"admin\",\n" +
                "    \"modifiedBy\" : \"user1\",\n" +
                "    \"custom\" : \"{}\"\n" +
                "  } ]\n" +
                "}";

        Map<String, List> results = buildResultsMapWithMultipleApis();
        when(queryingManager.performJdbcQuery(anyString(), anyString(), anyString(), anyInt(), anyInt(), anyListOf(Object.class)))
                .thenReturn(results, new HashMap<String, String>());
        String json = serverAssertion.getJsonMessage("conn", null, "","", false);
        // remove timestamp for comparison
        assertEquals(json.replaceFirst("\\d{13}", "").replaceAll("\\s+", ""), ref.replaceAll("\\s+", ""));
    }

    @Test
    public void testGetJsonMessageWithMultipleApisWithApiPlans() throws Exception {
        final String ref = "{\n" +
                "  \"incrementStart\" : ,\n" +
                "  \"entityType\" : \"APPLICATION\",\n" +
                "  \"bulkSync\" : \"true\",\n" +
                "  \"newOrUpdatedEntities\" : [ {\n" +
                "    \"id\" : \"085f4526-9c23-416d-be73-eaba4da83249\",\n" +
                "    \"key\" : \"l7xx2738fab70d824c059f28a922a1edab15\",\n" +
                "    \"secret\" : \"6106dceac26844f0b1d9688bb565acf6\",\n" +
                "    \"status\" : \"active\",\n" +
                "    \"organizationId\" : \"4c35f9cd-8eb2-11e3-ae6b-000c2911a4db\",\n" +
                "    \"organizationName\" : \"Sample Org\",\n" +
                "    \"label\" : \"app1\",\n" +
                "    \"oauthCallbackUrl\" : \"https://some-uri1.com\",\n" +
                "    \"oauthScope\" : \"\\\\\\\\\\\\\\\\\\\\\\\\%^&*()\",\n" +
                "    \"apis\" : [ {\r\n" +
                "      \"id\" : \"efb6f420-69da-49f6-bcd2-e283409e87fc\",\n" +
                "      \"planId\" : \"efb6f420-69da-49f6-bcd2-e283409e87fc\"\n" +
                "    }, {\n" +
                "      \"id\" : \"efb6f420-69da-49f6-bcd2-e283409e87fc\",\n" +
                "      \"planId\" : \"efb6f420-69da-49f6-bcd2-e283409e87fc\"\n" +
                "    } ],\n" +
                "    \"mag\" : {\n" +
                "      \"scope\" : \"\\\\\\\\\\\\\\\\\\\\\\\\%^&*() msso openid\",\n" +
                "      \"redirectUri\" : \"https://some-uri1.com\",\n" +
                "      \"masterKeys\" : [ {\n" +
                "        \"masterKey\" : \"f08985a0-e164-11e5-b86d-9a79f06e9478\",\n" +
                "        \"environment\" : \"all\"\n" +
                "      } ]\n" +
                "    },\n" +
                "    \"createdBy\" : \"admin\",\n" +
                "    \"modifiedBy\" : \"user1\",\n" +
                "    \"custom\" : \"{}\"\n" +
                "  } ]\n" +
                "}";

        Map<String, List> results = buildResultsMapWithMultipleApis();
        when(queryingManager.performJdbcQuery(anyString(), anyString(), anyString(), anyInt(), anyInt(), anyListOf(Object.class)))
                .thenReturn(results, new HashMap<String, String>());
        String json = serverAssertion.getJsonMessage("conn", null, "","", true);
        // remove timestamp for comparison
        assertEquals(json.replaceFirst("\\d{13}", "").replaceAll("\\s+", ""), ref.replaceAll("\\s+", ""));
    }

    private Map<String, List> buildResultsMapWithMultipleApis() {
        List urlList = new ArrayList();
        urlList.add("https://some-uri1.com");
        urlList.add("https://some-uri1.com");
        List typeList = new ArrayList();
        typeList.add(null);
        typeList.add(null);

        Map<String, List> results = new HashMap<>();
        results.put("uuid", Arrays.asList("085f4526-9c23-416d-be73-eaba4da83249", "085f4526-9c23-416d-be73-eaba4da83249"));
        results.put("api_key", Arrays.asList("l7xx2738fab70d824c059f28a922a1edab15", "l7xx2738fab70d824c059f28a922a1edab15"));
        results.put("key_secret", Arrays.asList("6106dceac26844f0b1d9688bb565acf6", "6106dceac26844f0b1d9688bb565acf6"));
        results.put("status", Arrays.asList("ENABLED", "ENABLED"));
        results.put("organization_uuid", Arrays.asList("4c35f9cd-8eb2-11e3-ae6b-000c2911a4db", "4c35f9cd-8eb2-11e3-ae6b-000c2911a4db"));
        results.put("organization_name", Arrays.asList("Sample Org", "Sample Org"));
        results.put("name", Arrays.asList("app1", "app1"));
        results.put("oauth_callback_url", urlList);
        results.put("oauth_scope", Arrays.asList("\\\\\\\\\\\\%^&*()", "\\\\\\\\\\\\%^&*()"));
        results.put("oauth_type", typeList);
        results.put("api_uuid", Arrays.asList("efb6f420-69da-49f6-bcd2-e283409e87fc", "efb6f420-69da-49f6-bcd2-e283409e87fc"));
        results.put("api_plan_uuid", Arrays.asList("efb6f420-69da-49f6-bcd2-e283409e87fc", "efb6f420-69da-49f6-bcd2-e283409e87fc"));
        results.put("mag_scope", Arrays.asList("msso openid", "msso openid"));
        results.put("mag_master_key", Arrays.asList("f08985a0-e164-11e5-b86d-9a79f06e9478", "35923776-e16c-11e5-b86d-9a79f06e9478"));
        results.put("created_by", Arrays.asList("admin", "admin"));
        results.put("modified_by", Arrays.asList("user1", "user1"));
        return results;
    }

    @BugId("AMP-6117")
    @Test
    public void buildMagScopeTest() throws Exception {
        String result = ServerGetIncrementAssertion.buildMagScope("other","msso_register msso_client_register email address profile phone");
        assertTrue(result.contains("msso"));
        assertTrue(result.contains("openid"));

        // test no repeating required scopes (openid)
        result = ServerGetIncrementAssertion.buildMagScope("other openid","msso_register msso_client_register email address profile phone");
        assertEquals(result.indexOf("openid"), result.lastIndexOf("openid"));
        assertTrue(result.contains("msso"));

        // test OOB values
        result = ServerGetIncrementAssertion.buildMagScope("oob openid","msso_register msso_client_register email address profile phone");
        assertEquals(result.indexOf("openid"), result.lastIndexOf("openid"));
        assertTrue(result.contains("msso"));
        assertFalse(result.contains("oob"));

        result = ServerGetIncrementAssertion.buildMagScope("oob other","msso_register msso_client_register email address profile phone");
        assertTrue(result.contains("openid"));
        assertTrue(result.contains("msso"));
        assertFalse(result.contains("oob"));


        // null scope values
        result = ServerGetIncrementAssertion.buildMagScope(null,null);
        assertFalse(result.contains("null"));
    }

    @BugId("AMP-6185")
    @Test
    public void buildOauthScopeTest() throws Exception {
      String result = ServerGetIncrementAssertion.buildScope("oob");
      assertTrue(result.equals("oob"));

      result = ServerGetIncrementAssertion.buildScope("OOB");
      assertTrue(result.equals("OOB"));

      result = ServerGetIncrementAssertion.buildScope("oob other");
      assertTrue(result.equals("other"));

      result = ServerGetIncrementAssertion.buildScope("OOB other");
      assertTrue(result.contains("OOB"));
      assertTrue(result.contains("other"));

      result = ServerGetIncrementAssertion.buildScope("SOMETHING other");
      assertTrue(result.contains("SOMETHING"));
      assertTrue(result.contains("other"));

      result = ServerGetIncrementAssertion.buildScope("oob OOB other");
      assertTrue(result.contains("OOB"));
      assertTrue(result.contains("other"));

      result = ServerGetIncrementAssertion.buildScope("abc Abc ABC");
      assertTrue(result.contains("abc"));
      assertTrue(result.contains("Abc"));
      assertTrue(result.contains("ABC"));

      result = ServerGetIncrementAssertion.buildScope("openid openid");
      assertTrue(result.equals("openid"));

      // null scope values
      result = ServerGetIncrementAssertion.buildScope(null);
      assertTrue(result == null);
    }
}
