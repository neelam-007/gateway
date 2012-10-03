package com.l7tech.external.assertions.apiportalintegration.server.resource;

import org.junit.Before;
import org.junit.Test;

import javax.xml.bind.JAXBException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import static org.junit.Assert.*;

public class ApiPlanJAXBResourceUnmarshallerTest {
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
        final String xml = "<l7:ApiPlan xmlns:l7=\"http://ns.l7tech.com/2012/04/api-management\">\n" +
                "  <l7:PlanId>p1</l7:PlanId>\n" +
                "  <l7:PlanName>pName</l7:PlanName>\n" +
                "  <l7:LastUpdate>" + LAST_UPDATE_STRING + "</l7:LastUpdate>\n" +
                "  <l7:PlanPolicy>policy xml</l7:PlanPolicy>\n" +
                "  <l7:DefaultPlan>true</l7:DefaultPlan>\n" +
                "</l7:ApiPlan>";

        final ApiPlanResource plan = (ApiPlanResource) unmarshaller.unmarshal(xml, ApiPlanResource.class);

        assertEquals("p1", plan.getPlanId());
        assertEquals("pName", plan.getPlanName());
        assertEquals(lastUpdate, plan.getLastUpdate());
        assertEquals("policy xml", plan.getPolicyXml());
        assertTrue(plan.isDefaultPlan());
    }

    @Test
    public void unmarshalNoPlanId() throws Exception {
        final String xml = "<l7:ApiPlan xmlns:l7=\"http://ns.l7tech.com/2012/04/api-management\">\n" +
                "  <l7:PlanName>pName</l7:PlanName>\n" +
                "  <l7:LastUpdate>" + LAST_UPDATE_STRING + "</l7:LastUpdate>\n" +
                "  <l7:PlanPolicy>policy xml</l7:PlanPolicy>\n" +
                "  <l7:DefaultPlan>true</l7:DefaultPlan>\n" +
                "</l7:ApiPlan>";

        final ApiPlanResource plan = (ApiPlanResource) unmarshaller.unmarshal(xml, ApiPlanResource.class);

        assertTrue(plan.getPlanId().isEmpty());
        assertEquals("pName", plan.getPlanName());
        assertEquals(lastUpdate, plan.getLastUpdate());
        assertEquals("policy xml", plan.getPolicyXml());
        assertTrue(plan.isDefaultPlan());
    }

    @Test
    public void unmarshalNoPlanName() throws Exception {
        final String xml = "<l7:ApiPlan xmlns:l7=\"http://ns.l7tech.com/2012/04/api-management\">\n" +
                "  <l7:PlanId>p1</l7:PlanId>\n" +
                "  <l7:LastUpdate>" + LAST_UPDATE_STRING + "</l7:LastUpdate>\n" +
                "  <l7:PlanPolicy>policy xml</l7:PlanPolicy>\n" +
                "  <l7:DefaultPlan>true</l7:DefaultPlan>\n" +
                "</l7:ApiPlan>";

        final ApiPlanResource plan = (ApiPlanResource) unmarshaller.unmarshal(xml, ApiPlanResource.class);

        assertEquals("p1", plan.getPlanId());
        assertTrue(plan.getPlanName().isEmpty());
        assertEquals(lastUpdate, plan.getLastUpdate());
        assertEquals("policy xml", plan.getPolicyXml());
        assertTrue(plan.isDefaultPlan());
    }

    @Test
    public void unmarshalNoLastUpdate() throws Exception {
        final String xml = "<l7:ApiPlan xmlns:l7=\"http://ns.l7tech.com/2012/04/api-management\">\n" +
                "  <l7:PlanId>p1</l7:PlanId>\n" +
                "  <l7:PlanName>pName</l7:PlanName>\n" +
                "  <l7:PlanPolicy>policy xml</l7:PlanPolicy>\n" +
                "  <l7:DefaultPlan>true</l7:DefaultPlan>\n" +
                "</l7:ApiPlan>";

        final ApiPlanResource plan = (ApiPlanResource) unmarshaller.unmarshal(xml, ApiPlanResource.class);

        assertEquals("p1", plan.getPlanId());
        assertEquals("pName", plan.getPlanName());
        assertNull(plan.getLastUpdate());
        assertEquals("policy xml", plan.getPolicyXml());
        assertTrue(plan.isDefaultPlan());
    }

    @Test
    public void unmarshalLastUpdateMissingTime() throws Exception {
        final String xml = "<l7:ApiPlan xmlns:l7=\"http://ns.l7tech.com/2012/04/api-management\">\n" +
                "  <l7:PlanId>p1</l7:PlanId>\n" +
                "  <l7:PlanName>pName</l7:PlanName>\n" +
                "  <l7:LastUpdate>2012-05-22</l7:LastUpdate>\n" +
                "  <l7:PlanPolicy>policy xml</l7:PlanPolicy>\n" +
                "  <l7:DefaultPlan>true</l7:DefaultPlan>\n" +
                "</l7:ApiPlan>";

        final ApiPlanResource plan = (ApiPlanResource) unmarshaller.unmarshal(xml, ApiPlanResource.class);

        assertEquals("p1", plan.getPlanId());
        assertEquals("pName", plan.getPlanName());
        assertEquals(new GregorianCalendar(2012, Calendar.MAY, 22).getTime(), plan.getLastUpdate());
        assertEquals("policy xml", plan.getPolicyXml());
        assertTrue(plan.isDefaultPlan());
    }

    @Test
    public void unmarshalNoPlanPolicy() throws Exception {
        final String xml = "<l7:ApiPlan xmlns:l7=\"http://ns.l7tech.com/2012/04/api-management\">\n" +
                "  <l7:PlanId>p1</l7:PlanId>\n" +
                "  <l7:PlanName>pName</l7:PlanName>\n" +
                "  <l7:LastUpdate>" + LAST_UPDATE_STRING + "</l7:LastUpdate>\n" +
                "  <l7:DefaultPlan>true</l7:DefaultPlan>\n" +
                "</l7:ApiPlan>";

        final ApiPlanResource plan = (ApiPlanResource) unmarshaller.unmarshal(xml, ApiPlanResource.class);

        assertEquals("p1", plan.getPlanId());
        assertEquals("pName", plan.getPlanName());
        assertEquals(lastUpdate, plan.getLastUpdate());
        assertTrue(plan.getPolicyXml().isEmpty());
        assertTrue(plan.isDefaultPlan());
    }

    @Test
    public void unmarshalNoDefaultPlan() throws Exception {
        final String xml = "<l7:ApiPlan xmlns:l7=\"http://ns.l7tech.com/2012/04/api-management\">\n" +
                "  <l7:PlanId>p1</l7:PlanId>\n" +
                "  <l7:PlanName>pName</l7:PlanName>\n" +
                "  <l7:LastUpdate>" + LAST_UPDATE_STRING + "</l7:LastUpdate>\n" +
                "  <l7:PlanPolicy>policy xml</l7:PlanPolicy>\n" +
                "</l7:ApiPlan>";

        final ApiPlanResource plan = (ApiPlanResource) unmarshaller.unmarshal(xml, ApiPlanResource.class);

        assertEquals("p1", plan.getPlanId());
        assertEquals("pName", plan.getPlanName());
        assertEquals(lastUpdate, plan.getLastUpdate());
        assertEquals("policy xml", plan.getPolicyXml());
        assertFalse(plan.isDefaultPlan());
    }

    @Test(expected = JAXBException.class)
    public void unmarshalInvalidXml() throws Exception {
        final String xml = "<invalid></invalid>";

        unmarshaller.unmarshal(xml, ApiPlanResource.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void unmarshalClassNotResource() throws Exception {
        final String xml = "<l7:ApiPlan xmlns:l7=\"http://ns.l7tech.com/2012/04/api-management\">\n" +
                "  <l7:PlanId>p1</l7:PlanId>\n" +
                "  <l7:PlanName>pName</l7:PlanName>\n" +
                "  <l7:LastUpdate>" + LAST_UPDATE_STRING + "</l7:LastUpdate>\n" +
                "  <l7:PlanPolicy>policy xml</l7:PlanPolicy>\n" +
                "  <l7:DefaultPlan>true</l7:DefaultPlan>\n" +
                "</l7:ApiPlan>";

        unmarshaller.unmarshal(xml, String.class);
    }

    @Test(expected = JAXBException.class)
    public void unmarshallWrongResourceClass() throws Exception {
        final String xml = "<l7:ApiPlan xmlns:l7=\"http://ns.l7tech.com/2012/04/api-management\">\n" +
                "  <l7:PlanId>p1</l7:PlanId>\n" +
                "  <l7:PlanName>pName</l7:PlanName>\n" +
                "  <l7:LastUpdate>" + LAST_UPDATE_STRING + "</l7:LastUpdate>\n" +
                "  <l7:PlanPolicy>policy xml</l7:PlanPolicy>\n" +
                "  <l7:DefaultPlan>true</l7:DefaultPlan>\n" +
                "</l7:ApiPlan>";

        unmarshaller.unmarshal(xml, ApiResource.class);
    }
}
