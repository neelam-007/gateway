package com.l7tech.external.assertions.apiportalintegration.server.resource;

import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;

import javax.xml.datatype.DatatypeConfigurationException;
import java.util.*;

import static org.junit.Assert.assertEquals;

public class ApiPlanListJAXBResourceMarshallerTest {

    private DefaultJAXBResourceMarshaller marshaller;
    private ApiPlanListResource planList;
    private List<ApiPlanResource> plans;
    private Calendar calendar;
    private Date lastUpdate;
    private static final String LAST_UPDATE_STRING = "2012-05-22T16:38:25.025-07:00";
    private static final ThroughputQuotaDetails QUOTA_DETAILS = new ThroughputQuotaDetails(true, 100, 1, 2);
    private static final RateLimitDetails RATE_LIMIT_DETAILS = new RateLimitDetails(true, 100, 60, true);


    @Before
    public void setup() throws Exception {
        marshaller = new DefaultJAXBResourceMarshaller();
        plans = new ArrayList<ApiPlanResource>();
        planList = new ApiPlanListResource(plans);
        calendar = new GregorianCalendar(2012, Calendar.MAY, 22, 16, 38, 25);
        calendar.set(Calendar.MILLISECOND, 25);
        calendar.setTimeZone(TimeZone.getTimeZone("GMT-7:00"));
        lastUpdate = calendar.getTime();
    }

    @Test
    public void marshall() throws Exception {
        plans.add(new ApiPlanResource("p1", "pName1", lastUpdate, "policy xml 1", false, new PlanDetails(QUOTA_DETAILS, RATE_LIMIT_DETAILS)));
        plans.add(new ApiPlanResource("p2", "pName2", lastUpdate, "policy xml 2", false, new PlanDetails(QUOTA_DETAILS, RATE_LIMIT_DETAILS)));

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
        plans.add(new ApiPlanResource(null, "pName", lastUpdate, "policy xml", false, new PlanDetails(QUOTA_DETAILS, RATE_LIMIT_DETAILS)));

        final String xml = marshaller.marshal(planList);

        assertEquals(StringUtils.deleteWhitespace(buildExpectedXml(planList)), StringUtils.deleteWhitespace(xml));
    }

    @Test
    public void marshallNullPlanName() throws Exception {
        plans.add(new ApiPlanResource("p1", null, lastUpdate, "policy xml", false, new PlanDetails(QUOTA_DETAILS, RATE_LIMIT_DETAILS)));

        final String xml = marshaller.marshal(planList);

        assertEquals(StringUtils.deleteWhitespace(buildExpectedXml(planList)), StringUtils.deleteWhitespace(xml));
    }

    @Test
    public void marshallNullLastUpdated() throws Exception {
        plans.add(new ApiPlanResource("p1", "pName", null, "policy xml", false, new PlanDetails(QUOTA_DETAILS, RATE_LIMIT_DETAILS)));

        final String xml = marshaller.marshal(planList);

        assertEquals(StringUtils.deleteWhitespace(buildExpectedXml(planList)), StringUtils.deleteWhitespace(xml));
    }

    @Test
    public void marshallNullPolicyXml() throws Exception {
        plans.add(new ApiPlanResource("p1", "pName", lastUpdate, null, false, new PlanDetails(QUOTA_DETAILS, RATE_LIMIT_DETAILS)));

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
                stringBuilder.append(LAST_UPDATE_STRING);
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
        }

        stringBuilder.append("</l7:ApiPlans>");
        return stringBuilder.toString();
    }
}