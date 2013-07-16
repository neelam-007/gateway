package com.l7tech.external.assertions.apiportalintegration.server.resource;

import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import static org.junit.Assert.*;

public class ApiPlanListJAXBResourceMarshallerTest {

    private DefaultJAXBResourceMarshaller marshaller;
    private ApiPlanListResource planList;
    private List<ApiPlanResource> plans;
    private final GregorianCalendar calendar = new GregorianCalendar();

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

    private String buildExpectedXml(final ApiPlanListResource planList) throws DatatypeConfigurationException {
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
                calendar.setTime(plan.getLastUpdate());
                final XMLGregorianCalendar gregorianCalendar = DatatypeFactory.newInstance().newXMLGregorianCalendar(calendar);
                final String formatted = gregorianCalendar.toString();
                stringBuilder.append(formatted);
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