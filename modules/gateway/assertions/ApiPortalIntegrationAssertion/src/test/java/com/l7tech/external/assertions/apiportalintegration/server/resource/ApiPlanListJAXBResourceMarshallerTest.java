package com.l7tech.external.assertions.apiportalintegration.server.resource;

import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

public class ApiPlanListJAXBResourceMarshallerTest {
    // this is not the EXACT format that jaxb uses by default - jaxb actually adds a colon to the time zone
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    private DefaultJAXBResourceMarshaller marshaller;
    private ApiPlanListResource planList;
    private List<ApiPlanResource> plans;

    @Before
    public void setup() throws Exception {
        marshaller = new DefaultJAXBResourceMarshaller();
        plans = new ArrayList<ApiPlanResource>();
        planList = new ApiPlanListResource(plans);
    }

    @Test
    public void marshall() throws Exception {
        plans.add(new ApiPlanResource("p1", "pName1", new Date(), "policy xml 1", false));
        plans.add(new ApiPlanResource("p2", "pName2", new Date(), "policy xml 2", false));

        final String xml = marshaller.marshal(planList);

        assertEquals(StringUtils.deleteWhitespace(buildExpectedXml(planList)), StringUtils.deleteWhitespace(xml));
    }

    @Test
    public void marshallNoPlans() throws Exception {
        plans.clear();

        final String xml = marshaller.marshal(planList);

        assertEquals(StringUtils.deleteWhitespace("<l7:ApiPlans xmlns:l7=\"http://ns.l7tech.com/2012/04/api-management\"/>"), StringUtils.deleteWhitespace(xml));
    }

    @Test
    public void marshallNullPlans() throws Exception {
        planList = new ApiPlanListResource(null);

        final String xml = marshaller.marshal(planList);

        assertEquals(StringUtils.deleteWhitespace("<l7:ApiPlans xmlns:l7=\"http://ns.l7tech.com/2012/04/api-management\"/>"), StringUtils.deleteWhitespace(xml));
    }

    @Test
    public void marshallNullPlanId() throws Exception {
        plans.add(new ApiPlanResource(null, "pName", new Date(), "policy xml", false));

        final String xml = marshaller.marshal(planList);

        assertEquals(StringUtils.deleteWhitespace(buildExpectedXml(planList)), StringUtils.deleteWhitespace(xml));
    }

    @Test
    public void marshallNullPlanName() throws Exception {
        plans.add(new ApiPlanResource("p1", null, new Date(), "policy xml", false));

        final String xml = marshaller.marshal(planList);

        assertEquals(StringUtils.deleteWhitespace(buildExpectedXml(planList)), StringUtils.deleteWhitespace(xml));
    }

    @Test
    public void marshallNullLastUpdated() throws Exception {
        plans.add(new ApiPlanResource("p1", "pName", null, "policy xml", false));

        final String xml = marshaller.marshal(planList);

        assertEquals(StringUtils.deleteWhitespace(buildExpectedXml(planList)), StringUtils.deleteWhitespace(xml));
    }

    @Test
    public void marshallNullPolicyXml() throws Exception {
        plans.add(new ApiPlanResource("p1", "pName", new Date(), null, false));

        final String xml = marshaller.marshal(planList);

        assertEquals(StringUtils.deleteWhitespace(buildExpectedXml(planList)), StringUtils.deleteWhitespace(xml));
    }

    private String buildExpectedXml(final ApiPlanListResource planList) {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<l7:ApiPlans xmlns:l7=\"http://ns.l7tech.com/2012/04/api-management\">");
        for (final ApiPlanResource plan : planList.getApiPlans()) {
            stringBuilder.append("<l7:ApiPlan>");
            stringBuilder.append("<l7:PlanId>");
            stringBuilder.append(plan.getPlanId());
            stringBuilder.append("</l7:PlanId>");
            stringBuilder.append("<l7:PlanName>");
            stringBuilder.append(plan.getPlanName());
            stringBuilder.append("</l7:PlanName>");
            if (plan.getLastUpdate() != null) {
                stringBuilder.append("<l7:LastUpdate>");
                final String formatted = DATE_FORMAT.format(plan.getLastUpdate());
                stringBuilder.append(formatted);
                stringBuilder.insert(stringBuilder.toString().length() - 2, ":");
                stringBuilder.append("</l7:LastUpdate>");
            }
            stringBuilder.append("<l7:PlanPolicy>");
            stringBuilder.append(plan.getPolicyXml());
            stringBuilder.append("</l7:PlanPolicy>");
            stringBuilder.append("<l7:DefaultPlan>");
            stringBuilder.append(plan.isDefaultPlan() ? "true" : "false");
            stringBuilder.append("</l7:DefaultPlan>");
            stringBuilder.append("</l7:ApiPlan>");
        }

        stringBuilder.append("</l7:ApiPlans>");
        return stringBuilder.toString();
    }
}
