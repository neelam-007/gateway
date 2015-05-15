package com.l7tech.external.assertions.apiportalintegration.server.resource;

import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ApiFragmentJAXBResourceMarshallerTest {
    private DefaultJAXBResourceMarshaller marshaller;
    private ApiFragmentResource apiFragment;

    @Before
    public void setup() throws Exception {
        marshaller = new DefaultJAXBResourceMarshaller();
    }

    @Test
    public void marshall() throws Exception {
        apiFragment = new ApiFragmentResource("a1", "id1", "true", "details");

        final String xml = marshaller.marshal(apiFragment);

        assertEquals(StringUtils.deleteWhitespace(buildExpectedXml(apiFragment)), StringUtils.deleteWhitespace(xml));
    }

    @Test
    public void marshallNullApiId() throws Exception {
        apiFragment = new ApiFragmentResource(null, null, null, null);

        final String xml = marshaller.marshal(apiFragment);

        assertEquals(StringUtils.deleteWhitespace(buildExpectedXml(apiFragment)), StringUtils.deleteWhitespace(xml));
    }

    @Test
    public void marshallNullApiGroup() throws Exception {
        apiFragment = new ApiFragmentResource("a1", "id1", "true", "details");

        final String xml = marshaller.marshal(apiFragment);

        assertEquals(StringUtils.deleteWhitespace(buildExpectedXml(apiFragment)), StringUtils.deleteWhitespace(xml));
    }

    @Test
    public void marshallNullServiceOid() throws Exception {
        apiFragment = new ApiFragmentResource("a1", "id1", "true", "details");

        final String xml = marshaller.marshal(apiFragment);

        assertEquals(StringUtils.deleteWhitespace(buildExpectedXml(apiFragment)), StringUtils.deleteWhitespace(xml));
    }

    private String buildExpectedXml(final ApiFragmentResource apiFragment) {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<l7:ApiFragment xmlns:l7=\"http://ns.l7tech.com/2012/04/api-management\">");
        stringBuilder.append("<l7:EncapsulatedAssertionGuid>");
        stringBuilder.append(apiFragment.getEncassGuid());
        stringBuilder.append("</l7:EncapsulatedAssertionGuid>");
        stringBuilder.append("<l7:EncapsulatedAssertionId>");
        stringBuilder.append(apiFragment.getEncassId());
        stringBuilder.append("</l7:EncapsulatedAssertionId>");
        stringBuilder.append("<l7:FragmentDetails>");
        stringBuilder.append("<l7:HasRouting>");
        stringBuilder.append(apiFragment.getFragmentDetails().getHasRouting());
        stringBuilder.append("</l7:HasRouting>");
        stringBuilder.append("<l7:ParsedPolicyDetails>");
        stringBuilder.append(apiFragment.getFragmentDetails().getParsedPolicyDetails());
        stringBuilder.append("</l7:ParsedPolicyDetails>");
        stringBuilder.append("</l7:FragmentDetails>");
        stringBuilder.append("</l7:ApiFragment>");
        return stringBuilder.toString();
    }
}
