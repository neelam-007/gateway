package com.l7tech.external.assertions.apiportalintegration.server.resource;

import org.junit.Before;
import org.junit.Test;

import javax.xml.bind.JAXBException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import static org.junit.Assert.*;

public class ApiPlanListJAXBResourceUnmarshallerTest {
    private DefaultJAXBResourceUnmarshaller unmarshaller;
    private static final String LAST_UPDATE_STRING = "2012-05-22T16:38:25.025-07:00";
    private Date lastUpdate;
    private Calendar calendar;

    @Before
    public void setup() throws Exception {
        unmarshaller = new DefaultJAXBResourceUnmarshaller();
        calendar = new GregorianCalendar(2012, Calendar.MAY, 22, 16, 38, 25);
        calendar.set(Calendar.MILLISECOND, 25);
        calendar.setTimeZone(TimeZone.getTimeZone("GMT-7:00"));
        lastUpdate = calendar.getTime();
    }

    @Test
    public void unmarshal() throws Exception {
        final String xml = "<l7:ApiPlans xmlns:l7=\"http://ns.l7tech.com/2012/04/api-management\">\n" +
                "<l7:ApiPlan>\n" +
                "  <l7:PlanId>p1</l7:PlanId>\n" +
                "  <l7:PlanName>pName1</l7:PlanName>\n" +
                "  <l7:LastUpdate>" + LAST_UPDATE_STRING + "</l7:LastUpdate>\n" +
                "  <l7:PlanPolicy>policy xml 1</l7:PlanPolicy>\n" +
                "  <l7:DefaultPlan>true</l7:DefaultPlan>\n" +
                "</l7:ApiPlan>\n" +
                "<l7:ApiPlan>\n" +
                "  <l7:PlanId>p2</l7:PlanId>\n" +
                "  <l7:PlanName>pName2</l7:PlanName>\n" +
                "  <l7:LastUpdate>" + LAST_UPDATE_STRING + "</l7:LastUpdate>\n" +
                "  <l7:PlanPolicy>policy xml 2</l7:PlanPolicy>\n" +
                "  <l7:DefaultPlan>false</l7:DefaultPlan>\n" +
                "</l7:ApiPlan>\n" +
                "</l7:ApiPlans>";

        final ApiPlanListResource planList = (ApiPlanListResource) unmarshaller.unmarshal(xml, ApiPlanListResource.class);

        assertEquals(2, planList.getApiPlans().size());
        final ApiPlanResource plan1 = planList.getApiPlans().get(0);
        assertEquals("p1", plan1.getPlanId());
        assertEquals("pName1", plan1.getPlanName());
        assertEquals("policy xml 1", plan1.getPolicyXml());
        assertTrue(plan1.isDefaultPlan());
        assertEquals(lastUpdate, plan1.getLastUpdate());
        final ApiPlanResource plan2 = planList.getApiPlans().get(1);
        assertEquals("p2", plan2.getPlanId());
        assertEquals("pName2", plan2.getPlanName());
        assertEquals("policy xml 2", plan2.getPolicyXml());
        assertFalse(plan2.isDefaultPlan());
        assertEquals(lastUpdate, plan2.getLastUpdate());
    }

    @Test
    public void unmarshalNone() throws Exception {
        final String xml = "<l7:ApiPlans xmlns:l7=\"http://ns.l7tech.com/2012/04/api-management\"></l7:ApiPlans>";

        final ApiPlanListResource planList = (ApiPlanListResource) unmarshaller.unmarshal(xml, ApiPlanListResource.class);

        assertTrue(planList.getApiPlans().isEmpty());
    }

    @Test
    public void unmarshalNoPlanId() throws Exception {
        final String xml = "<l7:ApiPlans xmlns:l7=\"http://ns.l7tech.com/2012/04/api-management\">\n" +
                "<l7:ApiPlan>\n" +
                "  <l7:PlanName>pName1</l7:PlanName>\n" +
                "  <l7:LastUpdate>" + LAST_UPDATE_STRING + "</l7:LastUpdate>\n" +
                "  <l7:PlanPolicy>policy xml 1</l7:PlanPolicy>\n" +
                "  <l7:DefaultPlan>true</l7:DefaultPlan>\n" +
                "</l7:ApiPlan>\n" +
                "</l7:ApiPlans>";

        final ApiPlanListResource planList = (ApiPlanListResource) unmarshaller.unmarshal(xml, ApiPlanListResource.class);

        assertEquals(1, planList.getApiPlans().size());
        final ApiPlanResource plan = planList.getApiPlans().get(0);
        assertTrue(plan.getPlanId().isEmpty());
        assertEquals("pName1", plan.getPlanName());
        assertEquals("policy xml 1", plan.getPolicyXml());
        assertTrue(plan.isDefaultPlan());
        assertEquals(lastUpdate, plan.getLastUpdate());
    }

    @Test
    public void unmarshalNoPlanName() throws Exception {
        final String xml = "<l7:ApiPlans xmlns:l7=\"http://ns.l7tech.com/2012/04/api-management\">\n" +
                "<l7:ApiPlan>\n" +
                "  <l7:PlanId>p1</l7:PlanId>\n" +
                "  <l7:LastUpdate>" + LAST_UPDATE_STRING + "</l7:LastUpdate>\n" +
                "  <l7:PlanPolicy>policy xml 1</l7:PlanPolicy>\n" +
                "  <l7:DefaultPlan>true</l7:DefaultPlan>\n" +
                "</l7:ApiPlan>\n" +
                "</l7:ApiPlans>";

        final ApiPlanListResource planList = (ApiPlanListResource) unmarshaller.unmarshal(xml, ApiPlanListResource.class);

        assertEquals(1, planList.getApiPlans().size());
        final ApiPlanResource plan = planList.getApiPlans().get(0);
        assertEquals("p1", plan.getPlanId());
        assertTrue(plan.getPlanName().isEmpty());
        assertEquals("policy xml 1", plan.getPolicyXml());
        assertTrue(plan.isDefaultPlan());
        assertEquals(lastUpdate, plan.getLastUpdate());
    }

    @Test
    public void unmarshalNoLastUpdate() throws Exception {
        final String xml = "<l7:ApiPlans xmlns:l7=\"http://ns.l7tech.com/2012/04/api-management\">\n" +
                "<l7:ApiPlan>\n" +
                "  <l7:PlanId>p1</l7:PlanId>\n" +
                "  <l7:PlanName>pName1</l7:PlanName>\n" +
                "  <l7:PlanPolicy>policy xml 1</l7:PlanPolicy>\n" +
                "  <l7:DefaultPlan>true</l7:DefaultPlan>\n" +
                "</l7:ApiPlan>\n" +
                "</l7:ApiPlans>";

        final ApiPlanListResource planList = (ApiPlanListResource) unmarshaller.unmarshal(xml, ApiPlanListResource.class);

        assertEquals(1, planList.getApiPlans().size());
        final ApiPlanResource plan = planList.getApiPlans().get(0);
        assertEquals("p1", plan.getPlanId());
        assertEquals("pName1", plan.getPlanName());
        assertEquals("policy xml 1", plan.getPolicyXml());
        assertTrue(plan.isDefaultPlan());
        assertNull(plan.getLastUpdate());
    }

    @Test
    public void unmarshalNoPlanPolicy() throws Exception {
        final String xml = "<l7:ApiPlans xmlns:l7=\"http://ns.l7tech.com/2012/04/api-management\">\n" +
                "<l7:ApiPlan>\n" +
                "  <l7:PlanId>p1</l7:PlanId>\n" +
                "  <l7:PlanName>pName1</l7:PlanName>\n" +
                "  <l7:LastUpdate>" + LAST_UPDATE_STRING + "</l7:LastUpdate>\n" +
                "  <l7:DefaultPlan>true</l7:DefaultPlan>\n" +
                "</l7:ApiPlan>\n" +
                "</l7:ApiPlans>";

        final ApiPlanListResource planList = (ApiPlanListResource) unmarshaller.unmarshal(xml, ApiPlanListResource.class);

        assertEquals(1, planList.getApiPlans().size());
        final ApiPlanResource plan = planList.getApiPlans().get(0);
        assertEquals("p1", plan.getPlanId());
        assertEquals("pName1", plan.getPlanName());
        assertTrue(plan.getPolicyXml().isEmpty());
        assertTrue(plan.isDefaultPlan());
        assertEquals(lastUpdate, plan.getLastUpdate());
    }

    @Test
    public void unmarshalNoDefaultPlan() throws Exception {
        final String xml = "<l7:ApiPlans xmlns:l7=\"http://ns.l7tech.com/2012/04/api-management\">\n" +
                "<l7:ApiPlan>\n" +
                "  <l7:PlanId>p1</l7:PlanId>\n" +
                "  <l7:PlanName>pName1</l7:PlanName>\n" +
                "  <l7:LastUpdate>" + LAST_UPDATE_STRING + "</l7:LastUpdate>\n" +
                "  <l7:PlanPolicy>policy xml 1</l7:PlanPolicy>\n" +
                "</l7:ApiPlan>\n" +
                "</l7:ApiPlans>";

        final ApiPlanListResource planList = (ApiPlanListResource) unmarshaller.unmarshal(xml, ApiPlanListResource.class);

        assertEquals(1, planList.getApiPlans().size());
        final ApiPlanResource plan = planList.getApiPlans().get(0);
        assertEquals("p1", plan.getPlanId());
        assertEquals("pName1", plan.getPlanName());
        assertEquals("policy xml 1", plan.getPolicyXml());
        assertFalse(plan.isDefaultPlan());
        assertEquals(lastUpdate, plan.getLastUpdate());
    }

    @Test(expected = JAXBException.class)
    public void unmarshalInvalidXml() throws Exception {
        final String xml = "<invalid></invalid>";

        unmarshaller.unmarshal(xml, ApiPlanListResource.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void unmarshalClassNotResource() throws Exception {
        final String xml = "<l7:ApiPlans xmlns:l7=\"http://ns.l7tech.com/2012/04/api-management\">\n" +
                "<l7:ApiPlan>\n" +
                "  <l7:PlanId>p1</l7:PlanId>\n" +
                "  <l7:PlanName>pName1</l7:PlanName>\n" +
                "  <l7:LastUpdate>" + LAST_UPDATE_STRING + "</l7:LastUpdate>\n" +
                "  <l7:PlanPolicy>policy xml 1</l7:PlanPolicy>\n" +
                "  <l7:DefaultPlan>true</l7:DefaultPlan>\n" +
                "</l7:ApiPlan>\n" +
                "</l7:ApiPlans>";

        unmarshaller.unmarshal(xml, String.class);
    }

    @Test(expected = JAXBException.class)
    public void unmarshalWrongResourceClass() throws Exception {
        final String xml = "<l7:ApiPlans xmlns:l7=\"http://ns.l7tech.com/2012/04/api-management\">\n" +
                "<l7:ApiPlan>\n" +
                "  <l7:PlanId>p1</l7:PlanId>\n" +
                "  <l7:PlanName>pName1</l7:PlanName>\n" +
                "  <l7:LastUpdate>" + LAST_UPDATE_STRING + "</l7:LastUpdate>\n" +
                "  <l7:PlanPolicy>policy xml 1</l7:PlanPolicy>\n" +
                "  <l7:DefaultPlan>true</l7:DefaultPlan>\n" +
                "</l7:ApiPlan>\n" +
                "</l7:ApiPlans>";

        unmarshaller.unmarshal(xml, ApiPlanResource.class);
    }
}
