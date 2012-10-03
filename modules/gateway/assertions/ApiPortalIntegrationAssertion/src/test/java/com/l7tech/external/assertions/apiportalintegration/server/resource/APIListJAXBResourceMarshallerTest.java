package com.l7tech.external.assertions.apiportalintegration.server.resource;

import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class APIListJAXBResourceMarshallerTest {
    private DefaultJAXBResourceMarshaller marshaller;
    private ApiListResource apiList;
    private List<ApiResource> apis;

    @Before
    public void setup() throws Exception {
        marshaller = new DefaultJAXBResourceMarshaller();
        apis = new ArrayList<ApiResource>();
        apiList = new ApiListResource(apis);
    }

    @Test
    public void marshall() throws Exception {
        final ApiResource api1 = new ApiResource("a1", "g1", "1111");
        final ApiResource api2 = new ApiResource("a2", "g2", "2222");
        apis.add(api1);
        apis.add(api2);

        final String xml = marshaller.marshal(apiList);

        assertEquals(StringUtils.deleteWhitespace(buildExpectedXml(apiList)), StringUtils.deleteWhitespace(xml));
    }

    @Test
    public void marshallNoAPIs() throws Exception {
        apis.clear();

        final String xml = marshaller.marshal(apiList);

        assertEquals(StringUtils.deleteWhitespace("<l7:Apis xmlns:l7=\"http://ns.l7tech.com/2012/04/api-management\"/>"), StringUtils.deleteWhitespace(xml));
    }

    @Test
    public void marshallNullAPIs() throws Exception {
        apiList = new ApiListResource(null);

        final String xml = marshaller.marshal(apiList);

        assertEquals(StringUtils.deleteWhitespace("<l7:Apis xmlns:l7=\"http://ns.l7tech.com/2012/04/api-management\"/>"), StringUtils.deleteWhitespace(xml));
    }

    @Test
    public void marshallNullApiId() throws Exception {
        final ApiResource api = new ApiResource(null, "g1", "1111");
        apis.add(api);

        final String xml = marshaller.marshal(apiList);

        assertEquals(StringUtils.deleteWhitespace(buildExpectedXml(apiList)), StringUtils.deleteWhitespace(xml));
    }

    @Test
    public void marshallNullApiGroup() throws Exception {
        final ApiResource api = new ApiResource("a1", null, "1111");
        apis.add(api);

        final String xml = marshaller.marshal(apiList);

        assertEquals(StringUtils.deleteWhitespace(buildExpectedXml(apiList)), StringUtils.deleteWhitespace(xml));
    }

    @Test
    public void marshallNullServiceOid() throws Exception {
        final ApiResource api = new ApiResource("a1", "g1", null);
        apis.add(api);

        final String xml = marshaller.marshal(apiList);

        assertEquals(StringUtils.deleteWhitespace(buildExpectedXml(apiList)), StringUtils.deleteWhitespace(xml));
    }

    private String buildExpectedXml(final ApiListResource apiList) {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<l7:Apis xmlns:l7=\"http://ns.l7tech.com/2012/04/api-management\">");
        for (final ApiResource api : apiList.getApis()) {
            stringBuilder.append("<l7:Api>");
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
        }
        stringBuilder.append("</l7:Apis>");
        return stringBuilder.toString();
    }
}
