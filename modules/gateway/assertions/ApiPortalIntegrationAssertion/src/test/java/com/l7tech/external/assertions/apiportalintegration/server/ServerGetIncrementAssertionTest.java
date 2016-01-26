package com.l7tech.external.assertions.apiportalintegration.server;

import com.l7tech.external.assertions.apiportalintegration.GetIncrementAssertion;
import com.l7tech.message.Message;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.jdbc.JdbcQueryingManagerStub;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

import java.util.*;

import static org.junit.Assert.assertEquals;

/**
 * @author chean22, 1/15/2016
 */
public class ServerGetIncrementAssertionTest {
    private ServerGetIncrementAssertion serverAssertion;
    private GetIncrementAssertion assertion;
    private ApplicationContext applicationContext;
    private PolicyEnforcementContext policyContext;

    @Before
    public void setup() throws Exception {
        applicationContext = ApplicationContexts.getTestApplicationContext();
        policyContext = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
        assertion = new GetIncrementAssertion();
        serverAssertion = new ServerGetIncrementAssertion(assertion, applicationContext);
    }

    @Test
    public void testGetJsonMessage() throws Exception {
        final String ref = "{\n" +
                "  \"incrementStart\" : ,\n" +
                "  \"entityType\" : \"application\",\n" +
                "  \"newOrUpdatedEntities\" : [ {\n" +
                "    \"id\" : \"085f4526-9c23-416d-be73-eaba4da83249\",\n" +
                "    \"key\" : \"l7xx2738fab70d824c059f28a922a1edab15\",\n" +
                "    \"secret\" : \"6106dceac26844f0b1d9688bb565acf6\",\n" +
                "    \"status\" : \"ENABLED\",\n" +
                "    \"organizationId\" : \"4c35f9cd-8eb2-11e3-ae6b-000c2911a4db\",\n" +
                "    \"organizationName\" : \"Sample Org\",\n" +
                "    \"label\" : \"app1\",\n" +
                "    \"oauthScope\" : \"\\\\\\\\\\\\\\\\\\\\\\\\%^&*()\",\n" +
                "    \"apis\" : [ {\r\n" +
                "      \"id\" : \"efb6f420-69da-49f6-bcd2-e283409e87fc\"\n" +
                "    } ]\n" +
                "  } ]\n" +
                "}";

        Map<String, List> results = buildResultsMap();
        JdbcQueryingManagerStub jdbcQueryingManager = (JdbcQueryingManagerStub) applicationContext.getBean("jdbcQueryingManager");
        jdbcQueryingManager.setMockResults(results);
        String json = serverAssertion.getJsonMessage("conn", null, "");
        // remove timestamp for comparison
        assertEquals(json.replaceFirst("\\d{13}", "").replaceAll("\\s+", ""), ref.replaceAll("\\s+", ""));
    }

    private Map<String, List> buildResultsMap() {
        List urlList = new ArrayList();
        urlList.add(null);
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
        return results;
    }

    @Test
    public void testGetJsonMessageWithMultipleApis() throws Exception {
        final String ref = "{\n" +
                "  \"incrementStart\" : ,\n" +
                "  \"entityType\" : \"application\",\n" +
                "  \"newOrUpdatedEntities\" : [ {\n" +
                "    \"id\" : \"085f4526-9c23-416d-be73-eaba4da83249\",\n" +
                "    \"key\" : \"l7xx2738fab70d824c059f28a922a1edab15\",\n" +
                "    \"secret\" : \"6106dceac26844f0b1d9688bb565acf6\",\n" +
                "    \"status\" : \"ENABLED\",\n" +
                "    \"organizationId\" : \"4c35f9cd-8eb2-11e3-ae6b-000c2911a4db\",\n" +
                "    \"organizationName\" : \"Sample Org\",\n" +
                "    \"label\" : \"app1\",\n" +
                "    \"oauthScope\" : \"\\\\\\\\\\\\\\\\\\\\\\\\%^&*()\",\n" +
                "    \"apis\" : [ {\r\n" +
                "      \"id\" : \"efb6f420-69da-49f6-bcd2-e283409e87fc\"\n" +
                "    }, {\n" +
                "      \"id\" : \"efb6f420-69da-49f6-bcd2-e283409e87fc\"\n" +
                "    } ]\n" +
                "  } ]\n" +
                "}";

        Map<String, List> results = buildResultsMapWithMultipleApis();
        JdbcQueryingManagerStub jdbcQueryingManager = (JdbcQueryingManagerStub) applicationContext.getBean("jdbcQueryingManager");
        jdbcQueryingManager.setMockResults(results);
        String json = serverAssertion.getJsonMessage("conn", null, "");
        // remove timestamp for comparison
        assertEquals(json.replaceFirst("\\d{13}", "").replaceAll("\\s+", ""), ref.replaceAll("\\s+", ""));
    }

    private Map<String, List> buildResultsMapWithMultipleApis() {
        List urlList = new ArrayList();
        urlList.add(null);
        urlList.add(null);
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
        return results;
    }
}
