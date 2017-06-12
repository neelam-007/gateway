package com.l7tech.external.assertions.quickstarttemplate.server.parser;

import com.google.common.collect.ImmutableMap;
import com.l7tech.common.http.HttpMethod;
import org.codehaus.jackson.map.JsonMappingException;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.IsNull.notNullValue;

public class QuickStartParserTest {

    private QuickStartParser fixture;

    @Before
    public void setUp() {
        fixture = new QuickStartParser();
    }

    @Test
    public void parseJson() throws Exception {
        final InputStream is = new ByteArrayInputStream(("{\n" +
                "  \"Service\": {\n" +
                "    \"name\": \"MyService1\",\n" +
                "    \"gatewayUri\": \"/MyService1\",\n" +
                "    \"httpMethods\": [ \"get\", \"put\" ],\n" +
                "    \"policy\": [\n" +
                "      {\n" +
                "        \"RequireSSL\" : {\n" +
                "          \"clientCert\": \"optional\"\n" +
                "        }\n" +
                "      },\n" +
                "      {\n" +
                "        \"Cors\" : {}\n" +
                "      },\n" +
                "      {\n" +
                "        \"RateLimit\" : {\n" +
                "          \"maxRequestsPerSecond\": 250,\n" +
                "          \"hardLimit\": true,\n" +
                "          \"counterName\": \"RateLimit-${request.clientId}-b0938b7ad6ff\"\n" +
                "        }\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}").getBytes("UTF-8"));
        final ServiceContainer container = fixture.parseJson(is);
        assertThat(container, notNullValue());
        assertThat(container.service.name, equalTo("MyService1"));
        assertThat(container.service.gatewayUri, equalTo("/MyService1"));
        assertThat(container.service.httpMethods, contains(HttpMethod.GET, HttpMethod.PUT));
        assertThat(container.service.policy.size(), equalTo(3));
        assertThat(container.service.policy.get(0).get("RequireSSL"), instanceOf(Map.class));
        assertThat(container.service.policy.get(0).get("RequireSSL"), equalTo(ImmutableMap.of(
                "clientCert", "optional"
        )));
        assertThat(container.service.policy.get(1).get("Cors"), instanceOf(Map.class));
        assertThat(container.service.policy.get(1).get("Cors"), equalTo(ImmutableMap.of()));
        assertThat(container.service.policy.get(2).get("RateLimit"), instanceOf(Map.class));
        assertThat(container.service.policy.get(2).get("RateLimit"), equalTo(ImmutableMap.of(
                "maxRequestsPerSecond", 250,
                "hardLimit", true,
                "counterName", "RateLimit-${request.clientId}-b0938b7ad6ff"
        )));
    }

    @Test(expected = JsonMappingException.class)
    public void parseJsonMissingGatewayUri() throws Exception {
        final InputStream is = new ByteArrayInputStream(("{\n" +
                "  \"Service\": {\n" +
                "    \"name\": \"MyService1\",\n" +
                "    \"httpMethods\": [ \"get\", \"put\" ],\n" +
                "    \"policy\": [\n" +
                "      {\n" +
                "        \"RequireSSL\" : {\n" +
                "          \"clientCert\": \"optional\"\n" +
                "        }\n" +
                "      },\n" +
                "      {\n" +
                "        \"Cors\" : {}\n" +
                "      },\n" +
                "      {\n" +
                "        \"RateLimit\" : {\n" +
                "          \"maxRequestsPerSecond\": 250,\n" +
                "          \"hardLimit\": true,\n" +
                "          \"counterName\": \"RateLimit-${request.clientId}-b0938b7ad6ff\"\n" +
                "        }\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}").getBytes("UTF-8"));
        fixture.parseJson(is);
    }

    @Test(expected = JsonMappingException.class)
    public void parseJsonMissingName() throws Exception {
        final InputStream is = new ByteArrayInputStream(("{\n" +
                "  \"Service\": {\n" +
                "    \"gatewayUri\": \"/MyService1\",\n" +
                "    \"httpMethods\": [ \"get\", \"put\" ],\n" +
                "    \"policy\": [\n" +
                "      {\n" +
                "        \"RequireSSL\" : {\n" +
                "          \"clientCert\": \"optional\"\n" +
                "        }\n" +
                "      },\n" +
                "      {\n" +
                "        \"Cors\" : {}\n" +
                "      },\n" +
                "      {\n" +
                "        \"RateLimit\" : {\n" +
                "          \"maxRequestsPerSecond\": 250,\n" +
                "          \"hardLimit\": true,\n" +
                "          \"counterName\": \"RateLimit-${request.clientId}-b0938b7ad6ff\"\n" +
                "        }\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}").getBytes("UTF-8"));
        fixture.parseJson(is);
    }

    @Test(expected = JsonMappingException.class)
    public void parseJsonMissingHttpMethods() throws Exception {
        final InputStream is = new ByteArrayInputStream(("{\n" +
                "  \"Service\": {\n" +
                "    \"name\": \"MyService1\",\n" +
                "    \"gatewayUri\": \"/MyService1\",\n" +
                "    \"policy\": [\n" +
                "      {\n" +
                "        \"RequireSSL\" : {\n" +
                "          \"clientCert\": \"optional\"\n" +
                "        }\n" +
                "      },\n" +
                "      {\n" +
                "        \"Cors\" : {}\n" +
                "      },\n" +
                "      {\n" +
                "        \"RateLimit\" : {\n" +
                "          \"maxRequestsPerSecond\": 250,\n" +
                "          \"hardLimit\": true,\n" +
                "          \"counterName\": \"RateLimit-${request.clientId}-b0938b7ad6ff\"\n" +
                "        }\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}").getBytes("UTF-8"));
        fixture.parseJson(is);
    }

    @Test(expected = JsonMappingException.class)
    public void parseJsonMissingPolicy() throws Exception {
        final InputStream is = new ByteArrayInputStream(("{\n" +
                "  \"Service\": {\n" +
                "    \"name\": \"MyService1\",\n" +
                "    \"gatewayUri\": \"/MyService1\",\n" +
                "    \"httpMethods\": [ \"get\", \"put\" ]\n" +
                "}").getBytes("UTF-8"));
        fixture.parseJson(is);
    }

    @Test(expected = JsonMappingException.class)
    public void parseJsonInvalidHttpMethods() throws Exception {
        final InputStream is = new ByteArrayInputStream(("{\n" +
                "  \"Service\": {\n" +
                "    \"name\": \"MyService1\",\n" +
                "    \"gatewayUri\": \"/MyService1\",\n" +
                "    \"httpMethods\": [ \"invalid\" ],\n" +
                "    \"policy\": [\n" +
                "      {\n" +
                "        \"RequireSSL\" : {\n" +
                "          \"clientCert\": \"optional\"\n" +
                "        }\n" +
                "      },\n" +
                "      {\n" +
                "        \"Cors\" : {}\n" +
                "      },\n" +
                "      {\n" +
                "        \"RateLimit\" : {\n" +
                "          \"maxRequestsPerSecond\": 250,\n" +
                "          \"hardLimit\": true,\n" +
                "          \"counterName\": \"RateLimit-${request.clientId}-b0938b7ad6ff\"\n" +
                "        }\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}").getBytes("UTF-8"));
        fixture.parseJson(is);
    }

    @Test
    public void parseJsonEmptyPolicyIsAllowed() throws Exception {
        final InputStream is = new ByteArrayInputStream(("{\n" +
                "  \"Service\": {\n" +
                "    \"name\": \"MyService1\",\n" +
                "    \"gatewayUri\": \"/MyService1\",\n" +
                "    \"httpMethods\": [ \"get\" ],\n" +
                "    \"policy\": []\n" +
                "  }\n" +
                "}").getBytes("UTF-8"));
        fixture.parseJson(is);
    }

    @Test(expected = JsonMappingException.class)
    public void parseJsonInvalidPolicy() throws Exception {
        final InputStream is = new ByteArrayInputStream(("{\n" +
                "  \"Service\": {\n" +
                "    \"name\": \"MyService1\",\n" +
                "    \"gatewayUri\": \"/MyService1\",\n" +
                "    \"httpMethods\": [ \"get\" ],\n" +
                "    \"policy\": [ \"true\", 1 ]\n" +
                "  }\n" +
                "}").getBytes("UTF-8"));
        fixture.parseJson(is);
    }

    @Test(expected = JsonMappingException.class)
    public void parseJsonPolicyIsMalformed() throws Exception {
        final InputStream is = new ByteArrayInputStream(("{\n" +
                "  \"Service\": {\n" +
                "    \"name\": \"MyService1\",\n" +
                "    \"gatewayUri\": \"/MyService1\",\n" +
                "    \"httpMethods\": [ \"get\" ],\n" +
                "    \"policy\": [\n" +
                "      {\n" +
                "        \"RequireSSL\" : {\n" +
                "          \"clientCert\": \"optional\"\n" +
                "        },\n" +
                "        \"RequireSSLAgain\" : {\n" +
                "          \"clientCert\": \"optional\"\n" +
                "        }\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}").getBytes("UTF-8"));
        fixture.parseJson(is);
    }

    @Test(expected = JsonMappingException.class)
    public void parseJsonEmptyServiceName() throws Exception {
        final InputStream is = new ByteArrayInputStream(("{\n" +
                "  \"Service\": {\n" +
                "    \"name\": \"\",\n" +
                "    \"gatewayUri\": \"/MyService1\",\n" +
                "    \"httpMethods\": [ \"get\", \"put\" ],\n" +
                "    \"policy\": [\n" +
                "      {\n" +
                "        \"RequireSSL\" : {\n" +
                "          \"clientCert\": \"optional\"\n" +
                "        }\n" +
                "      },\n" +
                "      {\n" +
                "        \"Cors\" : {}\n" +
                "      },\n" +
                "      {\n" +
                "        \"RateLimit\" : {\n" +
                "          \"maxRequestsPerSecond\": 250,\n" +
                "          \"hardLimit\": true,\n" +
                "          \"counterName\": \"RateLimit-${request.clientId}-b0938b7ad6ff\"\n" +
                "        }\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}").getBytes("UTF-8"));
        fixture.parseJson(is);
    }

    @Test(expected = JsonMappingException.class)
    public void parseJsonEmptyGatewayUri() throws Exception {
        final InputStream is = new ByteArrayInputStream(("{\n" +
                "  \"Service\": {\n" +
                "    \"name\": \"MyService1\",\n" +
                "    \"gatewayUri\": \"\",\n" +
                "    \"httpMethods\": [ \"get\", \"put\" ],\n" +
                "    \"policy\": [\n" +
                "      {\n" +
                "        \"RequireSSL\" : {\n" +
                "          \"clientCert\": \"optional\"\n" +
                "        }\n" +
                "      },\n" +
                "      {\n" +
                "        \"Cors\" : {}\n" +
                "      },\n" +
                "      {\n" +
                "        \"RateLimit\" : {\n" +
                "          \"maxRequestsPerSecond\": 250,\n" +
                "          \"hardLimit\": true,\n" +
                "          \"counterName\": \"RateLimit-${request.clientId}-b0938b7ad6ff\"\n" +
                "        }\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}").getBytes("UTF-8"));
        fixture.parseJson(is);
    }

}