package com.l7tech.external.assertions.apiportalintegration.server.resource;

import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;

import javax.xml.datatype.DatatypeConfigurationException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class AccountPlanListJAXBResourceMarshallerTest {

    // this is not the EXACT format that jaxb uses by default - jaxb actually adds a colon to the time zone
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    private static final ThroughputQuotaDetails QUOTA_DETAILS = new ThroughputQuotaDetails(true, 100, 1, 2);
    private static final RateLimitDetails RATE_LIMIT_DETAILS = new RateLimitDetails(true, 100, 60, true);
    private static final String ORG_IDS = "o1,o2,o3";

    private DefaultJAXBResourceMarshaller marshaller;
    private AccountPlanListResource planList;
    private List<AccountPlanResource> plans;
    private final GregorianCalendar calendar = new GregorianCalendar();

    @Before
    public void setup() throws Exception {
        marshaller = new DefaultJAXBResourceMarshaller();
        plans = new ArrayList<AccountPlanResource>();
        planList = new AccountPlanListResource(plans);
    }

    @Test
    public void marshall() throws Exception {
        plans.add(new AccountPlanResource("p1", "pName1", new Date(), false,
                new PlanDetails(QUOTA_DETAILS, RATE_LIMIT_DETAILS), new AccountPlanMapping(ORG_IDS), "the xml 1"));
        plans.add(new AccountPlanResource("p2", "pName2", new Date(), false,
                new PlanDetails(QUOTA_DETAILS, RATE_LIMIT_DETAILS), new AccountPlanMapping(ORG_IDS), "the xml 2"));

        final String xml = marshaller.marshal(planList);

        assertEquals(StringUtils.deleteWhitespace(buildExpectedXml(planList)), StringUtils.deleteWhitespace(xml));
    }

    @Test
    public void marshallNoPlans() throws Exception {
        plans.clear();

        final String xml = marshaller.marshal(planList);

        assertEquals(StringUtils.deleteWhitespace("<l7:AccountPlans xmlns:l7=\"http://ns.l7tech.com/2012/04/api-management\"/>"), StringUtils.deleteWhitespace(xml));
    }

    @Test
    public void marshallNullPlans() throws Exception {
        planList = new AccountPlanListResource(null);

        final String xml = marshaller.marshal(planList);

        assertEquals(StringUtils.deleteWhitespace("<l7:AccountPlans xmlns:l7=\"http://ns.l7tech.com/2012/04/api-management\"/>"), StringUtils.deleteWhitespace(xml));
    }

    @Test
    public void marshallNullPlanId() throws Exception {
        plans.add(new AccountPlanResource(null, "pName", new Date(), false,
                new PlanDetails(QUOTA_DETAILS, RATE_LIMIT_DETAILS), new AccountPlanMapping(ORG_IDS), "the xml"));

        final String xml = marshaller.marshal(planList);

        assertEquals(StringUtils.deleteWhitespace(buildExpectedXml(planList)), StringUtils.deleteWhitespace(xml));
    }

    @Test
    public void marshallNullPlanName() throws Exception {
        plans.add(new AccountPlanResource("p1", null, new Date(), false,
                new PlanDetails(QUOTA_DETAILS, RATE_LIMIT_DETAILS), new AccountPlanMapping(ORG_IDS), "the xml"));

        final String xml = marshaller.marshal(planList);

        assertEquals(StringUtils.deleteWhitespace(buildExpectedXml(planList)), StringUtils.deleteWhitespace(xml));
    }

    @Test
    public void marshallNullLastUpdated() throws Exception {
        plans.add(new AccountPlanResource("p1", "pName", null, false,
                new PlanDetails(QUOTA_DETAILS, RATE_LIMIT_DETAILS), new AccountPlanMapping(ORG_IDS), "the xml"));

        final String xml = marshaller.marshal(planList);

        assertEquals(StringUtils.deleteWhitespace(buildExpectedXml(planList)), StringUtils.deleteWhitespace(xml));
    }

    @Test
    public void marshallNullPolicyXml() throws Exception {
        plans.add(new AccountPlanResource("p1", "pName", new Date(), false,
                new PlanDetails(QUOTA_DETAILS, RATE_LIMIT_DETAILS), new AccountPlanMapping(ORG_IDS), "the xml"));

        final String xml = marshaller.marshal(planList);

        assertEquals(StringUtils.deleteWhitespace(buildExpectedXml(planList)), StringUtils.deleteWhitespace(xml));
    }

    private String buildExpectedXml(final AccountPlanListResource planList) throws DatatypeConfigurationException {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<l7:AccountPlans xmlns:l7=\"http://ns.l7tech.com/2012/04/api-management\">");
            for (final AccountPlanResource plan : planList.getAccountPlans()) {
            stringBuilder.append("<l7:AccountPlan>");
                stringBuilder.append("<l7:PlanId>");
                stringBuilder.append(plan.getPlanId());
                stringBuilder.append("</l7:PlanId>");
                stringBuilder.append("<l7:PlanName>");
                stringBuilder.append(plan.getPlanName());
                stringBuilder.append("</l7:PlanName>");
                if (plan.getLastUpdate() != null) {
                    stringBuilder.append("<l7:LastUpdated>");
                    final String formatted = DATE_FORMAT.format(plan.getLastUpdate());
                    stringBuilder.append(formatted);
                    stringBuilder.insert(stringBuilder.toString().length() - 2, ":");
                    stringBuilder.append("</l7:LastUpdated>");
                }
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
                if(plan.getPlanMapping().getIds().length() > 0) {
                    stringBuilder.append("<l7:PlanMapping>");
                    stringBuilder.append("<l7:Ids>").append(plan.getPlanMapping().getIds()).append("</l7:Ids>");
                    stringBuilder.append("</l7:PlanMapping>");
                } else {
                    stringBuilder.append("<l7:PlanMapping/>");
                }
                stringBuilder.append("<l7:PlanPolicy>");
                stringBuilder.append(plan.getPolicyXml());
                stringBuilder.append("</l7:PlanPolicy>");
            stringBuilder.append("</l7:AccountPlan>");
        }

        stringBuilder.append("</l7:AccountPlans>");
        return stringBuilder.toString();
    }
}
