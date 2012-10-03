package com.l7tech.external.assertions.apiportalintegration.server.resource;

import org.junit.Before;
import org.junit.Test;

import javax.xml.bind.JAXBException;

import static org.junit.Assert.*;

public class ApiKeyJAXBResourceUnmarshallerTest {
    private DefaultJAXBResourceUnmarshaller unmarshaller;

    @Before
    public void setup() throws Exception {
        unmarshaller = new DefaultJAXBResourceUnmarshaller();
    }

    @Test
    public void unmarshal() throws Exception {
        final String xml = "<l7:ApiKey xmlns:l7=\"http://ns.l7tech.com/2012/04/api-management\">\n" +
                "    <l7:Key>k1</l7:Key>\n" +
                "    <l7:Status>active</l7:Status>\n" +
                "    <l7:Apis>\n" +
                "        <l7:Api apiId=\"s2\" planId=\"p2\"/>\n" +
                "        <l7:Api apiId=\"s1\" planId=\"p1\"/>\n" +
                "    </l7:Apis>\n" +
                "    <l7:Secret>secret</l7:Secret>\n" +
                "    <l7:Label>label</l7:Label>\n" +
                "    <l7:Platform>platform</l7:Platform>\n" +
                "    <l7:Security>\n" +
                "        <l7:OAuth>\n" +
                "            <l7:CallbackUrl>callback</l7:CallbackUrl>\n" +
                "            <l7:Scope>scope</l7:Scope>\n" +
                "            <l7:Type>type</l7:Type>\n" +
                "        </l7:OAuth>\n" +
                "    </l7:Security>\n" +
                "</l7:ApiKey>";

        final ApiKeyResource result = (ApiKeyResource) unmarshaller.unmarshal(xml, ApiKeyResource.class);

        assertEquals("k1", result.getKey());
        assertEquals("label", result.getLabel());
        assertEquals("callback", result.getSecurity().getOauth().getCallbackUrl());
        assertEquals("scope", result.getSecurity().getOauth().getScope());
        assertEquals("type", result.getSecurity().getOauth().getType());
        assertEquals("platform", result.getPlatform());
        assertEquals("secret", result.getSecret());
        assertEquals("active", result.getStatus());
        assertEquals(2, result.getApis().size());
        assertEquals("p1", result.getApis().get("s1"));
        assertEquals("p2", result.getApis().get("s2"));
    }

    @Test
    public void unmarshalNoKey() throws Exception {
        final String xml = "<l7:ApiKey xmlns:l7=\"http://ns.l7tech.com/2012/04/api-management\">\n" +
                "    <l7:Status>active</l7:Status>\n" +
                "    <l7:Apis>\n" +
                "        <l7:Api apiId=\"s2\" planId=\"p2\"/>\n" +
                "        <l7:Api apiId=\"s1\" planId=\"p1\"/>\n" +
                "    </l7:Apis>\n" +
                "    <l7:Secret>secret</l7:Secret>\n" +
                "    <l7:Label>label</l7:Label>\n" +
                "    <l7:Platform>platform</l7:Platform>\n" +
                "    <l7:Security>\n" +
                "        <l7:OAuth>\n" +
                "            <l7:CallbackUrl>callback</l7:CallbackUrl>\n" +
                "            <l7:Scope>scope</l7:Scope>\n" +
                "            <l7:Type>type</l7:Type>\n" +
                "        </l7:OAuth>\n" +
                "    </l7:Security>\n" +
                "</l7:ApiKey>";

        final ApiKeyResource result = (ApiKeyResource) unmarshaller.unmarshal(xml, ApiKeyResource.class);

        assertTrue(result.getKey().isEmpty());
        assertEquals("label", result.getLabel());
        assertEquals("callback", result.getSecurity().getOauth().getCallbackUrl());
        assertEquals("scope", result.getSecurity().getOauth().getScope());
        assertEquals("type", result.getSecurity().getOauth().getType());
        assertEquals("platform", result.getPlatform());
        assertEquals("secret", result.getSecret());
        assertEquals("active", result.getStatus());
        assertEquals(2, result.getApis().size());
        assertEquals("p1", result.getApis().get("s1"));
        assertEquals("p2", result.getApis().get("s2"));
    }

    @Test
    public void unmarshalNoLabel() throws Exception {
        final String xml = "<l7:ApiKey xmlns:l7=\"http://ns.l7tech.com/2012/04/api-management\">\n" +
                "    <l7:Key>k1</l7:Key>\n" +
                "    <l7:Status>active</l7:Status>\n" +
                "    <l7:Apis>\n" +
                "        <l7:Api apiId=\"s2\" planId=\"p2\"/>\n" +
                "        <l7:Api apiId=\"s1\" planId=\"p1\"/>\n" +
                "    </l7:Apis>\n" +
                "    <l7:Secret>secret</l7:Secret>\n" +
                "    <l7:Platform>platform</l7:Platform>\n" +
                "    <l7:Security>\n" +
                "        <l7:OAuth>\n" +
                "            <l7:CallbackUrl>callback</l7:CallbackUrl>\n" +
                "            <l7:Scope>scope</l7:Scope>\n" +
                "            <l7:Type>type</l7:Type>\n" +
                "        </l7:OAuth>\n" +
                "    </l7:Security>\n" +
                "</l7:ApiKey>";

        final ApiKeyResource result = (ApiKeyResource) unmarshaller.unmarshal(xml, ApiKeyResource.class);

        assertEquals("k1", result.getKey());
        assertTrue(result.getLabel().isEmpty());
        assertEquals("callback", result.getSecurity().getOauth().getCallbackUrl());
        assertEquals("scope", result.getSecurity().getOauth().getScope());
        assertEquals("type", result.getSecurity().getOauth().getType());
        assertEquals("platform", result.getPlatform());
        assertEquals("secret", result.getSecret());
        assertEquals("active", result.getStatus());
        assertEquals(2, result.getApis().size());
        assertEquals("p1", result.getApis().get("s1"));
        assertEquals("p2", result.getApis().get("s2"));
    }

    @Test
    public void unmarshalNoSecurity() throws Exception {
        final String xml = "<l7:ApiKey xmlns:l7=\"http://ns.l7tech.com/2012/04/api-management\">\n" +
                "    <l7:Key>k1</l7:Key>\n" +
                "    <l7:Status>active</l7:Status>\n" +
                "    <l7:Apis>\n" +
                "        <l7:Api apiId=\"s2\" planId=\"p2\"/>\n" +
                "        <l7:Api apiId=\"s1\" planId=\"p1\"/>\n" +
                "    </l7:Apis>\n" +
                "    <l7:Secret>secret</l7:Secret>\n" +
                "    <l7:Label>label</l7:Label>\n" +
                "    <l7:Platform>platform</l7:Platform>\n" +
                "</l7:ApiKey>";

        final ApiKeyResource result = (ApiKeyResource) unmarshaller.unmarshal(xml, ApiKeyResource.class);

        assertEquals("k1", result.getKey());
        assertEquals("label", result.getLabel());
        assertNull(result.getSecurity().getOauth());
        assertEquals("platform", result.getPlatform());
        assertEquals("secret", result.getSecret());
        assertEquals("active", result.getStatus());
        assertEquals(2, result.getApis().size());
        assertEquals("p1", result.getApis().get("s1"));
        assertEquals("p2", result.getApis().get("s2"));
    }

    @Test
    public void unmarshalNoCallback() throws Exception {
        final String xml = "<l7:ApiKey xmlns:l7=\"http://ns.l7tech.com/2012/04/api-management\">\n" +
                "    <l7:Key>k1</l7:Key>\n" +
                "    <l7:Status>active</l7:Status>\n" +
                "    <l7:Apis>\n" +
                "        <l7:Api apiId=\"s2\" planId=\"p2\"/>\n" +
                "        <l7:Api apiId=\"s1\" planId=\"p1\"/>\n" +
                "    </l7:Apis>\n" +
                "    <l7:Secret>secret</l7:Secret>\n" +
                "    <l7:Label>label</l7:Label>\n" +
                "    <l7:Platform>platform</l7:Platform>\n" +
                "    <l7:Security>\n" +
                "        <l7:OAuth>\n" +
                "            <l7:Scope>scope</l7:Scope>\n" +
                "            <l7:Type>type</l7:Type>\n" +
                "        </l7:OAuth>\n" +
                "    </l7:Security>\n" +
                "</l7:ApiKey>";

        final ApiKeyResource result = (ApiKeyResource) unmarshaller.unmarshal(xml, ApiKeyResource.class);

        assertEquals("k1", result.getKey());
        assertEquals("label", result.getLabel());
        assertTrue(result.getSecurity().getOauth().getCallbackUrl().isEmpty());
        assertEquals("scope", result.getSecurity().getOauth().getScope());
        assertEquals("type", result.getSecurity().getOauth().getType());
        assertEquals("platform", result.getPlatform());
        assertEquals("secret", result.getSecret());
        assertEquals("active", result.getStatus());
        assertEquals(2, result.getApis().size());
        assertEquals("p1", result.getApis().get("s1"));
        assertEquals("p2", result.getApis().get("s2"));
    }

    @Test
    public void unmarshalNoScope() throws Exception {
        final String xml = "<l7:ApiKey xmlns:l7=\"http://ns.l7tech.com/2012/04/api-management\">\n" +
                "    <l7:Key>k1</l7:Key>\n" +
                "    <l7:Status>active</l7:Status>\n" +
                "    <l7:Apis>\n" +
                "        <l7:Api apiId=\"s2\" planId=\"p2\"/>\n" +
                "        <l7:Api apiId=\"s1\" planId=\"p1\"/>\n" +
                "    </l7:Apis>\n" +
                "    <l7:Secret>secret</l7:Secret>\n" +
                "    <l7:Label>label</l7:Label>\n" +
                "    <l7:Platform>platform</l7:Platform>\n" +
                "    <l7:Security>\n" +
                "        <l7:OAuth>\n" +
                "            <l7:CallbackUrl>callback</l7:CallbackUrl>\n" +
                "            <l7:Type>type</l7:Type>\n" +
                "        </l7:OAuth>\n" +
                "    </l7:Security>\n" +
                "</l7:ApiKey>";

        final ApiKeyResource result = (ApiKeyResource) unmarshaller.unmarshal(xml, ApiKeyResource.class);

        assertEquals("k1", result.getKey());
        assertEquals("label", result.getLabel());
        assertEquals("callback", result.getSecurity().getOauth().getCallbackUrl());
        assertTrue(result.getSecurity().getOauth().getScope().isEmpty());
        assertEquals("type", result.getSecurity().getOauth().getType());
        assertEquals("platform", result.getPlatform());
        assertEquals("secret", result.getSecret());
        assertEquals("active", result.getStatus());
        assertEquals(2, result.getApis().size());
        assertEquals("p1", result.getApis().get("s1"));
        assertEquals("p2", result.getApis().get("s2"));
    }

    @Test
    public void unmarshalNoOauthType() throws Exception {
        final String xml = "<l7:ApiKey xmlns:l7=\"http://ns.l7tech.com/2012/04/api-management\">\n" +
                "    <l7:Key>k1</l7:Key>\n" +
                "    <l7:Status>active</l7:Status>\n" +
                "    <l7:Apis>\n" +
                "        <l7:Api apiId=\"s2\" planId=\"p2\"/>\n" +
                "        <l7:Api apiId=\"s1\" planId=\"p1\"/>\n" +
                "    </l7:Apis>\n" +
                "    <l7:Secret>secret</l7:Secret>\n" +
                "    <l7:Label>label</l7:Label>\n" +
                "    <l7:Platform>platform</l7:Platform>\n" +
                "    <l7:Security>\n" +
                "        <l7:OAuth>\n" +
                "            <l7:CallbackUrl>callback</l7:CallbackUrl>\n" +
                "            <l7:Scope>scope</l7:Scope>\n" +
                "        </l7:OAuth>\n" +
                "    </l7:Security>\n" +
                "</l7:ApiKey>";

        final ApiKeyResource result = (ApiKeyResource) unmarshaller.unmarshal(xml, ApiKeyResource.class);

        assertEquals("k1", result.getKey());
        assertEquals("label", result.getLabel());
        assertEquals("callback", result.getSecurity().getOauth().getCallbackUrl());
        assertEquals("scope", result.getSecurity().getOauth().getScope());
        assertTrue(result.getSecurity().getOauth().getType().isEmpty());
        assertEquals("platform", result.getPlatform());
        assertEquals("secret", result.getSecret());
        assertEquals("active", result.getStatus());
        assertEquals(2, result.getApis().size());
        assertEquals("p1", result.getApis().get("s1"));
        assertEquals("p2", result.getApis().get("s2"));
    }

    @Test
    public void unmarshalNoOauth() throws Exception {
        final String xml = "<l7:ApiKey xmlns:l7=\"http://ns.l7tech.com/2012/04/api-management\">\n" +
                "    <l7:Key>k1</l7:Key>\n" +
                "    <l7:Status>active</l7:Status>\n" +
                "    <l7:Apis>\n" +
                "        <l7:Api apiId=\"s2\" planId=\"p2\"/>\n" +
                "        <l7:Api apiId=\"s1\" planId=\"p1\"/>\n" +
                "    </l7:Apis>\n" +
                "    <l7:Secret>secret</l7:Secret>\n" +
                "    <l7:Label>label</l7:Label>\n" +
                "    <l7:Platform>platform</l7:Platform>\n" +
                "    <l7:Security>\n" +
                "    </l7:Security>\n" +
                "</l7:ApiKey>";

        final ApiKeyResource result = (ApiKeyResource) unmarshaller.unmarshal(xml, ApiKeyResource.class);

        assertEquals("k1", result.getKey());
        assertEquals("label", result.getLabel());
        assertNull(result.getSecurity().getOauth());
        assertEquals("platform", result.getPlatform());
        assertEquals("secret", result.getSecret());
        assertEquals("active", result.getStatus());
        assertEquals(2, result.getApis().size());
        assertEquals("p1", result.getApis().get("s1"));
        assertEquals("p2", result.getApis().get("s2"));
    }

    @Test
    public void unmarshalNoPlatform() throws Exception {
        final String xml = "<l7:ApiKey xmlns:l7=\"http://ns.l7tech.com/2012/04/api-management\">\n" +
                "    <l7:Key>k1</l7:Key>\n" +
                "    <l7:Status>active</l7:Status>\n" +
                "    <l7:Apis>\n" +
                "        <l7:Api apiId=\"s2\" planId=\"p2\"/>\n" +
                "        <l7:Api apiId=\"s1\" planId=\"p1\"/>\n" +
                "    </l7:Apis>\n" +
                "    <l7:Secret>secret</l7:Secret>\n" +
                "    <l7:Label>label</l7:Label>\n" +
                "    <l7:Security>\n" +
                "        <l7:OAuth>\n" +
                "            <l7:CallbackUrl>callback</l7:CallbackUrl>\n" +
                "            <l7:Scope>scope</l7:Scope>\n" +
                "            <l7:Type>type</l7:Type>\n" +
                "        </l7:OAuth>\n" +
                "    </l7:Security>\n" +
                "</l7:ApiKey>";

        final ApiKeyResource result = (ApiKeyResource) unmarshaller.unmarshal(xml, ApiKeyResource.class);

        assertEquals("k1", result.getKey());
        assertEquals("label", result.getLabel());
        assertEquals("callback", result.getSecurity().getOauth().getCallbackUrl());
        assertEquals("scope", result.getSecurity().getOauth().getScope());
        assertEquals("type", result.getSecurity().getOauth().getType());
        assertTrue(result.getPlatform().isEmpty());
        assertEquals("secret", result.getSecret());
        assertEquals("active", result.getStatus());
        assertEquals(2, result.getApis().size());
        assertEquals("p1", result.getApis().get("s1"));
        assertEquals("p2", result.getApis().get("s2"));
    }

    @Test
    public void unmarshalNoSecret() throws Exception {
        final String xml = "<l7:ApiKey xmlns:l7=\"http://ns.l7tech.com/2012/04/api-management\">\n" +
                "    <l7:Key>k1</l7:Key>\n" +
                "    <l7:Status>active</l7:Status>\n" +
                "    <l7:Apis>\n" +
                "        <l7:Api apiId=\"s2\" planId=\"p2\"/>\n" +
                "        <l7:Api apiId=\"s1\" planId=\"p1\"/>\n" +
                "    </l7:Apis>\n" +
                "    <l7:Label>label</l7:Label>\n" +
                "    <l7:Platform>platform</l7:Platform>\n" +
                "    <l7:Security>\n" +
                "        <l7:OAuth>\n" +
                "            <l7:CallbackUrl>callback</l7:CallbackUrl>\n" +
                "            <l7:Scope>scope</l7:Scope>\n" +
                "            <l7:Type>type</l7:Type>\n" +
                "        </l7:OAuth>\n" +
                "    </l7:Security>\n" +
                "</l7:ApiKey>";

        final ApiKeyResource result = (ApiKeyResource) unmarshaller.unmarshal(xml, ApiKeyResource.class);

        assertEquals("k1", result.getKey());
        assertEquals("label", result.getLabel());
        assertEquals("callback", result.getSecurity().getOauth().getCallbackUrl());
        assertEquals("scope", result.getSecurity().getOauth().getScope());
        assertEquals("type", result.getSecurity().getOauth().getType());
        assertEquals("platform", result.getPlatform());
        assertTrue(result.getSecret().isEmpty());
        assertEquals("active", result.getStatus());
        assertEquals(2, result.getApis().size());
        assertEquals("p1", result.getApis().get("s1"));
        assertEquals("p2", result.getApis().get("s2"));
    }

    @Test
    public void unmarshalNoStatus() throws Exception {
        final String xml = "<l7:ApiKey xmlns:l7=\"http://ns.l7tech.com/2012/04/api-management\">\n" +
                "    <l7:Key>k1</l7:Key>\n" +
                "    <l7:Apis>\n" +
                "        <l7:Api apiId=\"s2\" planId=\"p2\"/>\n" +
                "        <l7:Api apiId=\"s1\" planId=\"p1\"/>\n" +
                "    </l7:Apis>\n" +
                "    <l7:Secret>secret</l7:Secret>\n" +
                "    <l7:Label>label</l7:Label>\n" +
                "    <l7:Platform>platform</l7:Platform>\n" +
                "    <l7:Security>\n" +
                "        <l7:OAuth>\n" +
                "            <l7:CallbackUrl>callback</l7:CallbackUrl>\n" +
                "            <l7:Scope>scope</l7:Scope>\n" +
                "            <l7:Type>type</l7:Type>\n" +
                "        </l7:OAuth>\n" +
                "    </l7:Security>\n" +
                "</l7:ApiKey>";

        final ApiKeyResource result = (ApiKeyResource) unmarshaller.unmarshal(xml, ApiKeyResource.class);

        assertEquals("k1", result.getKey());
        assertEquals("label", result.getLabel());
        assertEquals("callback", result.getSecurity().getOauth().getCallbackUrl());
        assertEquals("scope", result.getSecurity().getOauth().getScope());
        assertEquals("type", result.getSecurity().getOauth().getType());
        assertEquals("platform", result.getPlatform());
        assertEquals("secret", result.getSecret());
        assertTrue(result.getStatus().isEmpty());
        assertEquals(2, result.getApis().size());
        assertEquals("p1", result.getApis().get("s1"));
        assertEquals("p2", result.getApis().get("s2"));
    }

    @Test
    public void unmarshalNoApis() throws Exception {
        final String xml = "<l7:ApiKey xmlns:l7=\"http://ns.l7tech.com/2012/04/api-management\">\n" +
                "    <l7:Key>k1</l7:Key>\n" +
                "    <l7:Status>active</l7:Status>\n" +
                "    <l7:Status>active</l7:Status>\n" +
                "    <l7:Secret>secret</l7:Secret>\n" +
                "    <l7:Label>label</l7:Label>\n" +
                "    <l7:Platform>platform</l7:Platform>\n" +
                "    <l7:Security>\n" +
                "        <l7:OAuth>\n" +
                "            <l7:CallbackUrl>callback</l7:CallbackUrl>\n" +
                "            <l7:Scope>scope</l7:Scope>\n" +
                "            <l7:Type>type</l7:Type>\n" +
                "        </l7:OAuth>\n" +
                "    </l7:Security>\n" +
                "</l7:ApiKey>";

        final ApiKeyResource result = (ApiKeyResource) unmarshaller.unmarshal(xml, ApiKeyResource.class);

        assertEquals("k1", result.getKey());
        assertEquals("label", result.getLabel());
        assertEquals("callback", result.getSecurity().getOauth().getCallbackUrl());
        assertEquals("scope", result.getSecurity().getOauth().getScope());
        assertEquals("type", result.getSecurity().getOauth().getType());
        assertEquals("platform", result.getPlatform());
        assertEquals("secret", result.getSecret());
        assertEquals("active", result.getStatus());
        assertTrue(result.getApis().isEmpty());
    }

    @Test
    public void unmarshalEmptyApis() throws Exception {
        final String xml = "<l7:ApiKey xmlns:l7=\"http://ns.l7tech.com/2012/04/api-management\">\n" +
                "    <l7:Key>k1</l7:Key>\n" +
                "    <l7:Status>active</l7:Status>\n" +
                "    <l7:Apis>\n" +
                "    </l7:Apis>\n" +
                "    <l7:Secret>secret</l7:Secret>\n" +
                "    <l7:Label>label</l7:Label>\n" +
                "    <l7:Platform>platform</l7:Platform>\n" +
                "    <l7:Security>\n" +
                "        <l7:OAuth>\n" +
                "            <l7:CallbackUrl>callback</l7:CallbackUrl>\n" +
                "            <l7:Scope>scope</l7:Scope>\n" +
                "            <l7:Type>type</l7:Type>\n" +
                "        </l7:OAuth>\n" +
                "    </l7:Security>\n" +
                "</l7:ApiKey>";

        final ApiKeyResource result = (ApiKeyResource) unmarshaller.unmarshal(xml, ApiKeyResource.class);

        assertEquals("k1", result.getKey());
        assertEquals("label", result.getLabel());
        assertEquals("callback", result.getSecurity().getOauth().getCallbackUrl());
        assertEquals("scope", result.getSecurity().getOauth().getScope());
        assertEquals("type", result.getSecurity().getOauth().getType());
        assertEquals("platform", result.getPlatform());
        assertEquals("secret", result.getSecret());
        assertEquals("active", result.getStatus());
        assertTrue(result.getApis().isEmpty());
    }

    @Test(expected = JAXBException.class)
    public void unmarshalInvalidXml() throws Exception {
        unmarshaller.unmarshal("<invalid>", ApiKeyResource.class);
    }
}
