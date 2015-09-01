package com.l7tech.external.assertions.apiportalintegration.server.resource;

import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ApiFragmentListJAXBResourceMarshallerTest {
    private DefaultJAXBResourceMarshaller marshaller;
    private ApiFragmentListResource apiFragmentList;
    private List<ApiFragmentResource> apiFragments;

    @Before
    public void setup() throws Exception {
        marshaller = new DefaultJAXBResourceMarshaller();
        apiFragments = new ArrayList<ApiFragmentResource>();
        apiFragmentList = new ApiFragmentListResource(apiFragments);
    }

    @Test
    public void marshall() throws Exception {
        final ApiFragmentResource api1 = new ApiFragmentResource("a1", "id1", "true", "details");
        final ApiFragmentResource api2 = new ApiFragmentResource("a2", "id2", "false", "");
        apiFragments.add(api1);
        apiFragments.add(api2);

        final String xml = marshaller.marshal(apiFragmentList);

        assertEquals(StringUtils.deleteWhitespace(buildExpectedXml(apiFragmentList)), StringUtils.deleteWhitespace(xml));
    }

    @Test
    public void marshallNoAPIs() throws Exception {
        apiFragments.clear();

        final String xml = marshaller.marshal(apiFragmentList);

        assertEquals(StringUtils.deleteWhitespace("<l7:ApiFragments xmlns:l7=\"http://ns.l7tech.com/2012/04/api-management\"/>"), StringUtils.deleteWhitespace(xml));
    }

    @Test
    public void marshallNullAPIs() throws Exception {
        apiFragmentList = new ApiFragmentListResource(null);

        final String xml = marshaller.marshal(apiFragmentList);

        assertEquals(StringUtils.deleteWhitespace("<l7:ApiFragments xmlns:l7=\"http://ns.l7tech.com/2012/04/api-management\"/>"), StringUtils.deleteWhitespace(xml));
    }

    @Test
    public void marshallNullApiFragmentGuid() throws Exception {
        final ApiFragmentResource apiFragment = new ApiFragmentResource(null, null, null, null);
        apiFragments.add(apiFragment);

        final String xml = marshaller.marshal(apiFragmentList);

        assertEquals(StringUtils.deleteWhitespace(buildExpectedXml(apiFragmentList)), StringUtils.deleteWhitespace(xml));
    }

    @Test
    public void marshallNullHasRouting() throws Exception {
        final ApiFragmentResource api = new ApiFragmentResource("a1", "id1", null, "details");
        apiFragments.add(api);

        final String xml = marshaller.marshal(apiFragmentList);

        assertEquals(StringUtils.deleteWhitespace(buildExpectedXml(apiFragmentList)), StringUtils.deleteWhitespace(xml));
    }

    @Test
    public void marshallNullParsedPolicyDetails() throws Exception {
        final ApiFragmentResource apiFragment = new ApiFragmentResource("a1", "id1", "true", null);
        apiFragments.add(apiFragment);

        final String xml = marshaller.marshal(apiFragmentList);

        assertEquals(StringUtils.deleteWhitespace(buildExpectedXml(apiFragmentList)), StringUtils.deleteWhitespace(xml));
    }

    private String buildExpectedXml(final ApiFragmentListResource apiFragmentList) {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<l7:ApiFragments xmlns:l7=\"http://ns.l7tech.com/2012/04/api-management\">");
        for (final ApiFragmentResource apiFragment : apiFragmentList.getApiFragments()) {
            stringBuilder.append("<l7:ApiFragment>");
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
        }
        stringBuilder.append("</l7:ApiFragments>");
        return stringBuilder.toString();
    }
}
