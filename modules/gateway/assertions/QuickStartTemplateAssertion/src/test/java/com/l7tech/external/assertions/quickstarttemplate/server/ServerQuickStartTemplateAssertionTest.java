package com.l7tech.external.assertions.quickstarttemplate.server;

import com.l7tech.common.http.HttpHeader;
import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.external.assertions.quickstarttemplate.QuickStartTemplateAssertion;
import com.l7tech.external.assertions.quickstarttemplate.server.parser.ServiceContainer;
import com.l7tech.message.Message;
import com.l7tech.message.MimeKnob;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;
import org.codehaus.jackson.map.JsonMappingException;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.logging.Logger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ServerQuickStartTemplateAssertionTest {

    private ServerQuickStartTemplateAssertion fixture;

    @Before
    public void setUp() throws PolicyAssertionException {
        fixture = new ServerQuickStartTemplateAssertion(new QuickStartTemplateAssertion(), mock(ApplicationContext.class));
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
        assertThat(container.service, notNullValue());
        assertThat(container.service.name, notNullValue());
        assertThat(container.service.gatewayUri, notNullValue());
        assertThat(container.service.httpMethods, notNullValue());
        assertThat(container.service.httpMethods, contains(HttpMethod.GET, HttpMethod.PUT));
        assertThat(container.service.policy, notNullValue());
        assertThat(container.service.policy.size(), equalTo(3));
        assertThat(container.service.policy.get(0).get("RequireSSL"), instanceOf(Map.class));
        assertThat(container.service.policy.get(1).get("Cors"), instanceOf(Map.class));
        assertThat(container.service.policy.get(2).get("RateLimit"), instanceOf(Map.class));
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


}
