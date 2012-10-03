package com.l7tech.external.assertions.apiportalintegration.server.resource;

import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import static org.junit.Assert.*;

public class APIPlanJAXBResourceMarshallerTest {
    private DefaultJAXBResourceMarshaller marshaller;
    private ApiPlanResource plan;
    // this is not the EXACT format that jaxb uses by default - jaxb actually adds a colon to the time zone
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    private Date lastUpdate;

    @Before
    public void setup() throws Exception {
        marshaller = new DefaultJAXBResourceMarshaller();
        final Calendar calendar = new GregorianCalendar(2012, Calendar.JANUARY, 1, 1, 1, 1);
        calendar.set(Calendar.MILLISECOND, 1);
        calendar.setTimeZone(TimeZone.getTimeZone("GMT-7:00"));
        lastUpdate = calendar.getTime();
    }

    @Test
    public void marshall() throws Exception {
        plan = new ApiPlanResource("p1", "pName", lastUpdate, "the xml", false);

        final String xml = marshaller.marshal(plan);

        assertEquals(StringUtils.deleteWhitespace(buildExpectedXml(plan)), StringUtils.deleteWhitespace(xml));
    }

    @Test
    public void marshallNullPlanId() throws Exception {
        plan = new ApiPlanResource(null, "pName", lastUpdate, "the xml", false);

        final String xml = marshaller.marshal(plan);

        assertEquals(StringUtils.deleteWhitespace(buildExpectedXml(plan)), StringUtils.deleteWhitespace(xml));
    }

    @Test
    public void marshallNullPlanName() throws Exception {
        plan = new ApiPlanResource("p1", null, lastUpdate, "the xml", false);

        final String xml = marshaller.marshal(plan);

        final String expected = StringUtils.deleteWhitespace(buildExpectedXml(plan));
        System.out.println(expected);
        assertEquals(expected, StringUtils.deleteWhitespace(xml));
    }

    @Test
    public void marshallNullLastUpdate() throws Exception {
        plan = new ApiPlanResource("p1", "pName", null, "the xml", false);

        final String xml = marshaller.marshal(plan);

        assertEquals(StringUtils.deleteWhitespace(buildExpectedXml(plan)), StringUtils.deleteWhitespace(xml));
    }

    @Test
    public void marshallNullPolicyXml() throws Exception {
        plan = new ApiPlanResource("p1", "pName", lastUpdate, null, false);

        final String xml = marshaller.marshal(plan);

        assertEquals(StringUtils.deleteWhitespace(buildExpectedXml(plan)), StringUtils.deleteWhitespace(xml));
    }

    @Test
    public void marshallDefaultPlan() throws Exception {
        plan = new ApiPlanResource("p1", "pName", lastUpdate, "the xml", true);

        final String xml = marshaller.marshal(plan);

        assertEquals(StringUtils.deleteWhitespace(buildExpectedXml(plan)), StringUtils.deleteWhitespace(xml));
    }

    private String buildExpectedXml(final ApiPlanResource plan) {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<l7:ApiPlan xmlns:l7=\"http://ns.l7tech.com/2012/04/api-management\">");
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
        return stringBuilder.toString();
    }
}
