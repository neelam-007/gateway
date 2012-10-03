package com.l7tech.external.assertions.apiportalintegration.server.resource;

import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class ApiKeyJAXBResourceMarshallerTest {
    private DefaultJAXBResourceMarshaller marshaller;
    private ApiKeyResource key;
    private HashMap<String, String> apis;


    @Before
    public void setup() throws Exception {
        marshaller = new DefaultJAXBResourceMarshaller();
        apis = new HashMap<String, String>();
        key = new ApiKeyResource();
        key.setApis(apis);
    }

    @Test
    public void marshal() throws Exception {
        initKey();

        final String xml = marshaller.marshal(key);

        assertEquals(StringUtils.deleteWhitespace(buildExpectedXml(key)), StringUtils.deleteWhitespace(xml));
    }

    @Test
    public void marshalNullKey() throws Exception {
        initKey();
        key.setKey(null);

        final String xml = marshaller.marshal(key);

        assertEquals(StringUtils.deleteWhitespace(buildExpectedXml(key)), StringUtils.deleteWhitespace(xml));
    }

    @Test
    public void marshalNullStatus() throws Exception {
        initKey();
        key.setStatus(null);

        final String xml = marshaller.marshal(key);

        assertEquals(StringUtils.deleteWhitespace(buildExpectedXml(key)), StringUtils.deleteWhitespace(xml));
    }

    @Test
    public void marshalNullApis() throws Exception {
        initKey();
        key.setApis(null);

        final String xml = marshaller.marshal(key);

        assertEquals(StringUtils.deleteWhitespace(buildExpectedXml(key)), StringUtils.deleteWhitespace(xml));
    }

    @Test
    public void marshalNoApis() throws Exception {
        initKey();
        apis.clear();

        final String xml = marshaller.marshal(key);

        assertEquals(StringUtils.deleteWhitespace(buildExpectedXml(key)), StringUtils.deleteWhitespace(xml));
    }

    @Test
    public void marshalNullSecret() throws Exception {
        initKey();
        key.setSecret(null);

        final String xml = marshaller.marshal(key);

        assertEquals(StringUtils.deleteWhitespace(buildExpectedXml(key)), StringUtils.deleteWhitespace(xml));
    }

    @Test
    public void marshalNullLabel() throws Exception {
        initKey();
        key.setLabel(null);

        final String xml = marshaller.marshal(key);

        assertEquals(StringUtils.deleteWhitespace(buildExpectedXml(key)), StringUtils.deleteWhitespace(xml));
    }

    @Test
    public void marshalNullPlatform() throws Exception {
        initKey();
        key.setPlatform(null);

        final String xml = marshaller.marshal(key);

        assertEquals(StringUtils.deleteWhitespace(buildExpectedXml(key)), StringUtils.deleteWhitespace(xml));
    }

    @Test
    public void marshalNullSecurity() throws Exception {
        initKey();
        key.setSecurity(null);

        final String xml = marshaller.marshal(key);

        assertEquals(StringUtils.deleteWhitespace(buildExpectedXml(key)), StringUtils.deleteWhitespace(xml));
    }

    @Test
    public void marshalNullOauthCallback() throws Exception {
        initKey();
        key.getSecurity().getOauth().setCallbackUrl(null);

        final String xml = marshaller.marshal(key);

        assertEquals(StringUtils.deleteWhitespace(buildExpectedXml(key)), StringUtils.deleteWhitespace(xml));
    }

    @Test
    public void marshalNullOauthScope() throws Exception {
        initKey();
        key.getSecurity().getOauth().setScope(null);

        final String xml = marshaller.marshal(key);

        assertEquals(StringUtils.deleteWhitespace(buildExpectedXml(key)), StringUtils.deleteWhitespace(xml));
    }

    @Test
    public void marshalNullOauthType() throws Exception {
        initKey();
        key.getSecurity().getOauth().setType(null);

        final String xml = marshaller.marshal(key);

        assertEquals(StringUtils.deleteWhitespace(buildExpectedXml(key)), StringUtils.deleteWhitespace(xml));
    }

    @Test
    public void marshalNullOauth() throws Exception {
        initKey();
        key.getSecurity().setOauth(null);

        final String xml = marshaller.marshal(key);

        assertEquals(StringUtils.deleteWhitespace(buildExpectedXml(key)), StringUtils.deleteWhitespace(xml));
    }

    @Test
    public void marshalNullApiId() throws Exception {
        initKey();
        apis.clear();
        apis.put(null, "p1");

        final String xml = marshaller.marshal(key);

        assertEquals(StringUtils.deleteWhitespace(buildExpectedXml(key)), StringUtils.deleteWhitespace(xml));
    }

    @Test
    public void marshalNullPlanId() throws Exception {
        initKey();
        apis.clear();
        apis.put("s1", null);

        final String xml = marshaller.marshal(key);

        assertEquals(StringUtils.deleteWhitespace(buildExpectedXml(key)), StringUtils.deleteWhitespace(xml));
    }

    private void initKey() {
        apis.put("s1", "p1");
        apis.put("s2", "p2");
        key.setKey("k1");
        key.setLabel("label");
        key.setPlatform("platform");
        key.setSecret("secret");
        key.setStatus("active");
        key.setSecurity(new SecurityDetails(new OAuthDetails("callback", "scope", "type")));
    }

    private String buildExpectedXml(final ApiKeyResource key) {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<l7:ApiKey xmlns:l7=\"http://ns.l7tech.com/2012/04/api-management\">");
        stringBuilder.append(buildExpectedXmlForElement("Key", key.getKey()));
        stringBuilder.append(buildExpectedXmlForElement("Status", key.getStatus()));
        if (key.getApis().isEmpty()) {
            stringBuilder.append("<l7:Apis/>");
        } else {
            stringBuilder.append("<l7:Apis>");
            for (final Map.Entry<String, String> entry : key.getApis().entrySet()) {
                final String apiId = entry.getKey() == null ? "" : entry.getKey();
                final String planId = entry.getValue() == null ? "" : entry.getValue();
                stringBuilder.append("<l7:Api apiId=\"" + apiId + "\" planId=\"" + planId + "\"/>");
            }
            stringBuilder.append("</l7:Apis>");
        }
        stringBuilder.append(buildExpectedXmlForElement("Secret", key.getSecret()));
        stringBuilder.append(buildExpectedXmlForElement("Label", key.getLabel()));
        stringBuilder.append(buildExpectedXmlForElement("Platform", key.getPlatform()));
        stringBuilder.append(buildExpectedSecurityXml(key.getSecurity()));
        stringBuilder.append("</l7:ApiKey>");
        return stringBuilder.toString();
    }

    private String buildExpectedSecurityXml(final SecurityDetails security) {
        final StringBuilder stringBuilder = new StringBuilder();
        if (security.getOauth() != null) {
            stringBuilder.append("<l7:Security>");
            stringBuilder.append("<l7:OAuth>");
            stringBuilder.append(buildExpectedXmlForElement("CallbackUrl", security.getOauth().getCallbackUrl()));
            stringBuilder.append(buildExpectedXmlForElement("Scope", security.getOauth().getScope()));
            stringBuilder.append(buildExpectedXmlForElement("Type", security.getOauth().getType()));
            stringBuilder.append("</l7:OAuth>");
            stringBuilder.append("</l7:Security>");
        } else {
            stringBuilder.append("<l7:Security/>");
        }
        return stringBuilder.toString();
    }


    private String buildExpectedXmlForElement(final String elementName, final String elementValue) {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<l7:" + elementName + ">");
        stringBuilder.append(elementValue);
        stringBuilder.append("</l7:" + elementName + ">");
        return stringBuilder.toString();
    }
}
