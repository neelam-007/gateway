package com.l7tech.external.assertions.apiportalintegration.server.resource;

import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class APIJAXBResourceMarshallerTest {
    private DefaultJAXBResourceMarshaller marshaller;
    private ApiResource api;

    @Before
    public void setup() throws Exception {
        marshaller = new DefaultJAXBResourceMarshaller();
    }

    @Test
    public void marshall() throws Exception {
        api = new ApiResource("a1", "g1", "1234");

        final String xml = marshaller.marshal(api);

        assertEquals(StringUtils.deleteWhitespace(buildExpectedXml(api)), StringUtils.deleteWhitespace(xml));
    }

    @Test
    public void marshallNullApiId() throws Exception {
        api = new ApiResource(null, "g1", "1234");

        final String xml = marshaller.marshal(api);

        assertEquals(StringUtils.deleteWhitespace(buildExpectedXml(api)), StringUtils.deleteWhitespace(xml));
    }

    @Test
    public void marshallNullApiGroup() throws Exception {
        api = new ApiResource("a1", null, "1234");

        final String xml = marshaller.marshal(api);

        assertEquals(StringUtils.deleteWhitespace(buildExpectedXml(api)), StringUtils.deleteWhitespace(xml));
    }

    @Test
    public void marshallNullServiceOid() throws Exception {
        api = new ApiResource("a1", "g1", null);

        final String xml = marshaller.marshal(api);

        assertEquals(StringUtils.deleteWhitespace(buildExpectedXml(api)), StringUtils.deleteWhitespace(xml));
    }

    private String buildExpectedXml(final ApiResource api) {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<l7:Api xmlns:l7=\"http://ns.l7tech.com/2012/04/api-management\">");
        stringBuilder.append("<l7:ApiId>");
        stringBuilder.append(api.getApiId());
        stringBuilder.append("</l7:ApiId>");
        stringBuilder.append("<l7:ApiGroup>");
        stringBuilder.append(api.getApiGroup());
        stringBuilder.append("</l7:ApiGroup>");
        stringBuilder.append("<l7:ServiceOID>");
        stringBuilder.append(api.getServiceOid());
        stringBuilder.append("</l7:ServiceOID>");
        stringBuilder.append("</l7:Api>");
        return stringBuilder.toString();
    }
}
