package com.l7tech.external.assertions.apiportalintegration.server.resource;

import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.junit.Assert.assertEquals;

public class AccountPlanJAXBResourceMarshallerTest {
    // this is not the EXACT format that jaxb uses by default - jaxb actually adds a colon to the time zone
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    private static final ThroughputQuotaDetails QUOTA_DETAILS = new ThroughputQuotaDetails(true, 100, 1, 2);
    private static final List<String> ORG_IDS = new ArrayList<String>();
    static {
        ORG_IDS.add("o1");
        ORG_IDS.add("o2");
        ORG_IDS.add("o3");
    }

    private DefaultJAXBResourceMarshaller marshaller;
    private AccountPlanResource plan;
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

        plan = new AccountPlanResource("p1", "pName", lastUpdate, false,
                new PlanDetails(QUOTA_DETAILS), new AccountPlanMapping(ORG_IDS), "the xml");

        final String xml = marshaller.marshal(plan);

        assertEquals(StringUtils.deleteWhitespace(buildExpectedXml(plan)), StringUtils.deleteWhitespace(xml));
    }

    @Test
    public void marshallNullPlanId() throws Exception {
        plan = new AccountPlanResource(null, "pName", lastUpdate, false,
                new PlanDetails(QUOTA_DETAILS), new AccountPlanMapping(ORG_IDS), "the xml");

        final String xml = marshaller.marshal(plan);

        assertEquals(StringUtils.deleteWhitespace(buildExpectedXml(plan)), StringUtils.deleteWhitespace(xml));
    }

    @Test
    public void marshallNullPlanName() throws Exception {
        plan = new AccountPlanResource("p1", null, lastUpdate, false,
                new PlanDetails(QUOTA_DETAILS), new AccountPlanMapping(ORG_IDS), "the xml");

        final String xml = marshaller.marshal(plan);

        final String expected = StringUtils.deleteWhitespace(buildExpectedXml(plan));
        System.out.println(expected);
        assertEquals(expected, StringUtils.deleteWhitespace(xml));
    }

    @Test
    public void marshallNullLastUpdate() throws Exception {
        plan = new AccountPlanResource("p1", "pName", null, false,
                new PlanDetails(QUOTA_DETAILS), new AccountPlanMapping(ORG_IDS), "the xml");

        final String xml = marshaller.marshal(plan);

        assertEquals(StringUtils.deleteWhitespace(buildExpectedXml(plan)), StringUtils.deleteWhitespace(xml));
    }
    @Test
    public void marshallDefaultPlan() throws Exception {
        plan = new AccountPlanResource("p1", "pName", lastUpdate, true,
                new PlanDetails(QUOTA_DETAILS), new AccountPlanMapping(ORG_IDS), "the xml");

        final String xml = marshaller.marshal(plan);

        assertEquals(StringUtils.deleteWhitespace(buildExpectedXml(plan)), StringUtils.deleteWhitespace(xml));
    }

    @Test
    public void marshallNullPlanDetails() throws Exception {
        plan = new AccountPlanResource("p1", "pName", lastUpdate, true,
                null, new AccountPlanMapping(ORG_IDS), "the xml");

        final String xml = marshaller.marshal(plan);

        assertEquals(StringUtils.deleteWhitespace(buildExpectedXml(plan)), StringUtils.deleteWhitespace(xml));
    }

    @Test
    public void marshallNullAccountPlanOrganizations() throws Exception {
        plan = new AccountPlanResource("p1", "pName", lastUpdate, true,
                new PlanDetails(QUOTA_DETAILS), null, "the xml");

        final String xml = marshaller.marshal(plan);

        assertEquals(StringUtils.deleteWhitespace(buildExpectedXml(plan)), StringUtils.deleteWhitespace(xml));
    }

    @Test
    public void marshallNullPolicyXml() throws Exception {
        plan = new AccountPlanResource("p1", "pName", lastUpdate, true,
                new PlanDetails(QUOTA_DETAILS), new AccountPlanMapping(ORG_IDS), null);

        final String xml = marshaller.marshal(plan);

        assertEquals(StringUtils.deleteWhitespace(buildExpectedXml(plan)), StringUtils.deleteWhitespace(xml));
    }

    private String buildExpectedXml(final AccountPlanResource plan) {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<l7:AccountPlan xmlns:l7=\"http://ns.l7tech.com/2012/04/api-management\">");
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
        stringBuilder.append("</l7:PlanDetails>");
        if(plan.getPlanMapping().getIds().size() > 0) {
            stringBuilder.append("<l7:PlanMapping>");
            for(String org : plan.getPlanMapping().getIds()) {
                stringBuilder.append("<l7:Id>" + org + "</l7:Id>");
            }
            stringBuilder.append("</l7:PlanMapping>");
        } else {
            stringBuilder.append("<l7:PlanMapping/>");
        }
        stringBuilder.append("<l7:PlanPolicy>");
        stringBuilder.append(plan.getPolicyXml());
        stringBuilder.append("</l7:PlanPolicy>");
        stringBuilder.append("</l7:AccountPlan>");
        return stringBuilder.toString();
    }
}
