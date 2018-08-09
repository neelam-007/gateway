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

    private static final ThroughputQuotaDetails QUOTA_DETAILS = new ThroughputQuotaDetails(true, 100, 1, 2);
    private static final RateLimitDetails RATE_LIMIT_DETAILS = new RateLimitDetails(true, 100, 60, true);

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
        plan = new ApiPlanResource("p1", "pName", lastUpdate, "the xml", false, new PlanDetails(QUOTA_DETAILS, RATE_LIMIT_DETAILS));

        final String xml = marshaller.marshal(plan);

        assertEquals(StringUtils.deleteWhitespace(buildExpectedXml(plan)), StringUtils.deleteWhitespace(xml));
    }

    @Test
    public void marshallNullPlanId() throws Exception {
        plan = new ApiPlanResource(null, "pName", lastUpdate, "the xml", false, new PlanDetails(QUOTA_DETAILS, RATE_LIMIT_DETAILS));

        final String xml = marshaller.marshal(plan);

        assertEquals(StringUtils.deleteWhitespace(buildExpectedXml(plan)), StringUtils.deleteWhitespace(xml));
    }

    @Test
    public void marshallNullPlanName() throws Exception {
        plan = new ApiPlanResource("p1", null, lastUpdate, "the xml", false, new PlanDetails(QUOTA_DETAILS, RATE_LIMIT_DETAILS));

        final String xml = marshaller.marshal(plan);

        final String expected = StringUtils.deleteWhitespace(buildExpectedXml(plan));
        System.out.println(expected);
        assertEquals(expected, StringUtils.deleteWhitespace(xml));
    }

    @Test
    public void marshallNullLastUpdate() throws Exception {
        plan = new ApiPlanResource("p1", "pName", null, "the xml", false, new PlanDetails(QUOTA_DETAILS, RATE_LIMIT_DETAILS));

        final String xml = marshaller.marshal(plan);

        assertEquals(StringUtils.deleteWhitespace(buildExpectedXml(plan)), StringUtils.deleteWhitespace(xml));
    }

    @Test
    public void marshallNullPolicyXml() throws Exception {
        plan = new ApiPlanResource("p1", "pName", lastUpdate, null, false, new PlanDetails(QUOTA_DETAILS, RATE_LIMIT_DETAILS));

        final String xml = marshaller.marshal(plan);

        assertEquals(StringUtils.deleteWhitespace(buildExpectedXml(plan)), StringUtils.deleteWhitespace(xml));
    }

    @Test
    public void marshallDefaultPlan() throws Exception {
        plan = new ApiPlanResource("p1", "pName", lastUpdate, "the xml", true, new PlanDetails(QUOTA_DETAILS, RATE_LIMIT_DETAILS));

        final String xml = marshaller.marshal(plan);

        assertEquals(StringUtils.deleteWhitespace(buildExpectedXml(plan)), StringUtils.deleteWhitespace(xml));
    }

    @Test
    public void marshallNullPlanDetails() throws Exception {
        plan = new ApiPlanResource("p1", "pName", lastUpdate, "the xml", false, null);

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

        stringBuilder.append("<l7:PlanDetails>");

        ThroughputQuotaDetails details = plan.getPlanDetails().getThroughputQuota();
        stringBuilder.append("<l7:ThroughputQuota l7:enabled=\"" + (details.isEnabled() ? "true" : "false") + "\">");
        stringBuilder.append("<l7:Quota>");
        stringBuilder.append(details.getQuota());
        stringBuilder.append("</l7:Quota>");
        stringBuilder.append("<l7:TimeUnit>");
        stringBuilder.append(details.getTimeUnit());
        stringBuilder.append("</l7:TimeUnit>");
        stringBuilder.append("<l7:CounterStrategy>");
        stringBuilder.append(details.getCounterStrategy());
        stringBuilder.append("</l7:CounterStrategy>");
        stringBuilder.append("</l7:ThroughputQuota>");

        RateLimitDetails rateLimitDetails = plan.getPlanDetails().getRateLimit();
        stringBuilder.append("<l7:RateLimit l7:enabled=\"" + (rateLimitDetails.isEnabled() ? "true" : "false") + "\">");
        stringBuilder.append("<l7:MaxRequestRate>");
        stringBuilder.append(rateLimitDetails.getMaxRequestRate());
        stringBuilder.append("</l7:MaxRequestRate>");
        stringBuilder.append("<l7:WindowSizeInSeconds>");
        stringBuilder.append(rateLimitDetails.getWindowSizeInSeconds());
        stringBuilder.append("</l7:WindowSizeInSeconds>");
        stringBuilder.append("<l7:HardLimit>");
        stringBuilder.append(rateLimitDetails.isHardLimit());
        stringBuilder.append("</l7:HardLimit>");
        stringBuilder.append("</l7:RateLimit>");

        stringBuilder.append("</l7:PlanDetails>");

        stringBuilder.append("</l7:ApiPlan>");
        return stringBuilder.toString();
    }
}
