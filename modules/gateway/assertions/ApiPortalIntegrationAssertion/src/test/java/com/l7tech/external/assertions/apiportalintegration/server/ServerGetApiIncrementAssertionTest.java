package com.l7tech.external.assertions.apiportalintegration.server;

import com.l7tech.external.assertions.apiportalintegration.GetApiIncrementAssertion;
import com.l7tech.server.jdbc.JdbcQueryingManager;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ServerGetApiIncrementAssertionTest {
  private ServerGetApiIncrementAssertion serverAssertion;
  private GetApiIncrementAssertion assertion;
  private ApplicationContext applicationContext;
  private JdbcQueryingManager queryingManager;

  @Before
  public void setup() throws Exception {
    applicationContext = mock(ApplicationContext.class);
    queryingManager = mock(JdbcQueryingManager.class);
    when(applicationContext.getBean("jdbcQueryingManager", JdbcQueryingManager.class)).thenReturn(queryingManager);

    assertion = new GetApiIncrementAssertion();
    serverAssertion = new ServerGetApiIncrementAssertion(assertion, applicationContext);
    serverAssertion.jdbcConnectionName = "blah";
  }

  @Test
  public void testGetJsonMessage() throws Exception {
    final String ref = "{\n" +
        "  \"apis\" : [ {\n" +
        "    \"Uuid\" : \"e7c076e3-790d-49d7-8cd8-33e93e25ee88\",\n" +
        "    \"Name\" : \"apitest\",\n" +
        "    \"ServiceEnabled\" : true,\n" +
        "    \"PortalPublished\" : true,\n" +
        "    \"SsgUrl\" : \"apitest\",\n" +
        "    \"PortalModifyTS\" : 1522200783810,\n" +
        "    \"ApiLocationUrl\" : \"http://localhost:8080/echo\",\n" +
        "    \"CustomFields\" : [ {\n" +
        "        \"Name\" : \"test\",\n" +
        "        \"Value\" : \"test\"\n" +
        "     } ],\n" +
        "    \"PolicyEntities\" : [ {\n" +
        "      \"PolicyEntityUuid\" : \"cc9a43a5-d67c-4e86-81c4-f0ca58b93a1b\",\n" +
        "      \"Arguments\" : [ {\n" +
        "        \"Value\" : \"123\",\n" +
        "        \"Name\" : \"password\"\n" +
        "      }, {\n" +
        "        \"Value\" : \"asdasd\",\n" +
        "        \"Name\" : \"portalHostName\"\n" +
        "      }, {\n" +
        "        \"Value\" : \"asdasd\",\n" +
        "        \"Name\" : \"userName\"\n" +
        "      } ]\n" +
        "    }, {\n" +
        "      \"PolicyEntityUuid\" : \"172594b6-18ba-4b0c-8d61-807db457e81d\",\n" +
        "      \"Arguments\" : [ {\n" +
        "        \"Value\" : \"false\",\n" +
        "        \"Name\" : \"debugMode\"\n" +
        "      }, {\n" +
        "        \"Value\" : \"3\",\n" +
        "        \"Name\" : \"email\"\n" +
        "      }, {\n" +
        "        \"Value\" : \"3\",\n" +
        "        \"Name\" : \"sla\"\n" +
        "      }, {\n" +
        "        \"Value\" : \"3\",\n" +
        "        \"Name\" : \"smtpServer\"\n" +
        "      }, {\n" +
        "        \"Value\" : \"true\",\n" +
        "        \"Name\" : \"sslEnabled\"\n" +
        "      } ]\n" +
        "    }, {\n" +
        "      \"PolicyEntityUuid\" : \"66728e8d-116b-4021-b74b-dfaabf2ed4f0\",\n" +
        "      \"Arguments\" : [ {\n" +
        "        \"Value\" : \"false\",\n" +
        "        \"Name\" : \"debugMode\"\n" +
        "      }, {\n" +
        "        \"Value\" : \"apim.dev.ca.com\",\n" +
        "        \"Name\" : \"portalHostName\"\n" +
        "      }, {\n" +
        "        \"Value\" : \"false\",\n" +
        "        \"Name\" : \"sslEnabled\"\n" +
        "      }, {\n" +
        "        \"Value\" : \"72dc7738-a2b5-4ab5-9956-bfea85eccf95\",\n" +
        "        \"Name\" : \"tenantUuid\"\n" +
        "      } ]\n" +
        "    } ]\n" +
        "  }, {\n" +
        "    \"Uuid\" : \"17c076e3-790d-49d7-8cd8-33e93e25ee88\",\n" +
        "    \"Name\" : \"apitest2\",\n" +
        "    \"ServiceEnabled\" : true,\n" +
        "    \"PortalPublished\" : false,\n" +
        "    \"SsgUrl\" : \"apitest2\",\n" +
        "    \"PortalModifyTS\" : 1522200783811,\n" +
        "    \"ApiLocationUrl\" : \"http://localhost:8080/echo\",\n" +
        "    \"CustomFields\" : [ ],\n" +
        "    \"PolicyEntities\" : [ ]\n" +
        "  } ]\n" +
        "}";

    when(queryingManager.performJdbcQuery(anyString(), anyString(), anyString(), anyInt(), anyInt(), anyListOf(Object.class)))
        .thenReturn(buildApiResultsMap(), buildCustomFieldsResults(), buildPolicyEntitiesResults(), buildPolicyTemplateArgumentResults());

    String json = serverAssertion.getJsonMessage("blah", "blah");
    // remove timestamp for comparison
    assertEquals(json.replaceAll("\\s+", ""), ref.replaceAll("\\s+", ""));
  }

  private Map<String, List> buildApiResultsMap() {
    Map<String, List> results = new HashMap<>();
    results.put("uuid", Arrays.asList("e7c076e3-790d-49d7-8cd8-33e93e25ee88", "17c076e3-790d-49d7-8cd8-33e93e25ee88"));
    results.put("name", Arrays.asList("apitest", "apitest2"));
    results.put("portal_status", Arrays.asList("ENABLED", "ENABLED"));
    results.put("api_location_url", Arrays.asList("http://localhost:8080/echo", "http://localhost:8080/echo"));
    results.put("published_by_portal", Arrays.asList(true, false));
    results.put("ssg_url", Arrays.asList("apitest", "apitest2"));
    results.put("modify_ts", Arrays.asList(1522200783810L,1522200783811L));
    return results;
  }

  private Map<String, List> buildPolicyEntitiesResults() {
    Map<String, List> results = new HashMap<>();
    results.put("uuid", Arrays.asList("f2c076e3-790d-49d7-8cd8-33e93e25ee88", "132f4526-9c23-416d-be73-eaba4da83249", "133e4526-9c23-416d-be73-eaba4da83249"));
    results.put("api_uuid", Arrays.asList("e7c076e3-790d-49d7-8cd8-33e93e25ee88", "e7c076e3-790d-49d7-8cd8-33e93e25ee88", "e7c076e3-790d-49d7-8cd8-33e93e25ee88"));
    results.put("policy_entity_uuid", Arrays.asList("cc9a43a5-d67c-4e86-81c4-f0ca58b93a1b", "172594b6-18ba-4b0c-8d61-807db457e81d", "66728e8d-116b-4021-b74b-dfaabf2ed4f0"));
    return results;
  }

  private Map<String, List> buildPolicyTemplateArgumentResults() {
    Map<String, List> results = new HashMap<>();
    results.put("api_policy_entity_xref_uuid",
        Arrays.asList("f2c076e3-790d-49d7-8cd8-33e93e25ee88", "f2c076e3-790d-49d7-8cd8-33e93e25ee88", "f2c076e3-790d-49d7-8cd8-33e93e25ee88",
            "132f4526-9c23-416d-be73-eaba4da83249", "132f4526-9c23-416d-be73-eaba4da83249", "132f4526-9c23-416d-be73-eaba4da83249", "132f4526-9c23-416d-be73-eaba4da83249",
            "132f4526-9c23-416d-be73-eaba4da83249", "133e4526-9c23-416d-be73-eaba4da83249", "133e4526-9c23-416d-be73-eaba4da83249", "133e4526-9c23-416d-be73-eaba4da83249",
            "133e4526-9c23-416d-be73-eaba4da83249"));
    results.put("name",
        Arrays.asList("password", "portalHostName", "userName", "debugMode", "email", "sla", "smtpServer", "sslEnabled",
            "debugMode", "portalHostName", "sslEnabled", "tenantUuid"));
    results.put("value",
        Arrays.asList("123", "asdasd", "asdasd", "false", "3", "3", "3", "true", "false", "apim.dev.ca.com", "false",
            "72dc7738-a2b5-4ab5-9956-bfea85eccf95"));
    return results;
  }

  private Map<String, List> buildCustomFieldsResults() {
    Map<String, List> results = new HashMap<>();
    results.put("entity_uuid", Arrays.asList("e7c076e3-790d-49d7-8cd8-33e93e25ee88"));
    results.put("value", Arrays.asList("test"));
    results.put("name", Arrays.asList("test"));
    return results;
  }
}