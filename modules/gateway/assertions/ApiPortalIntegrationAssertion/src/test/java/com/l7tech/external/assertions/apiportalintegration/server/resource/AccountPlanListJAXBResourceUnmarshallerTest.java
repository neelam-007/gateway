package com.l7tech.external.assertions.apiportalintegration.server.resource;

import org.junit.Before;
import org.junit.Test;

import javax.xml.bind.JAXBException;
import java.util.*;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

public class AccountPlanListJAXBResourceUnmarshallerTest {

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
        final String xml = "<l7:AccountPlans xmlns:l7=\"http://ns.l7tech.com/2012/04/api-management\">\n" +
                "<l7:AccountPlan>\n" +
                "  <l7:PlanId>plan-abc-123</l7:PlanId>\n" +
                "  <l7:PlanName>Gold</l7:PlanName>\n" +
                "  <l7:LastUpdated>" + LAST_UPDATE_STRING + "</l7:LastUpdated>\n" +
                "  <l7:DefaultPlan>true</l7:DefaultPlan>\n" +
                "  <l7:PlanDetails>\n" +
                "    <l7:ThroughputQuota l7:enabled=\"true\">\n" +
                "      <l7:Quota>100000</l7:Quota>\n" +
                "      <l7:TimeUnit>4</l7:TimeUnit>\n" +
                "      <l7:CounterStrategy>2</l7:CounterStrategy>\n" +
                "    </l7:ThroughputQuota>\n" +
                "  </l7:PlanDetails>\n" +
                "  <l7:PlanMapping>\n" +
                "    <l7:Id>123</l7:Id>\n" +
                "    <l7:Id>456</l7:Id>\n" +
                "    <l7:Id>789</l7:Id>\n" +
                "  </l7:PlanMapping>\n" +
                "  <l7:PlanPolicy>policy xml</l7:PlanPolicy>\n" +
                "</l7:AccountPlan>\n" +
                "<l7:AccountPlan>\n" +
                "  <l7:PlanId>plan-def-456</l7:PlanId>\n" +
                "  <l7:PlanName>Silver</l7:PlanName>\n" +
                "  <l7:LastUpdated>" + LAST_UPDATE_STRING + "</l7:LastUpdated>\n" +
                "  <l7:DefaultPlan>false</l7:DefaultPlan>\n" +
                "  <l7:PlanDetails>\n" +
                "    <l7:ThroughputQuota l7:enabled=\"true\">\n" +
                "      <l7:Quota>200000</l7:Quota>\n" +
                "      <l7:TimeUnit>1</l7:TimeUnit>\n" +
                "      <l7:CounterStrategy>1</l7:CounterStrategy>\n" +
                "    </l7:ThroughputQuota>\n" +
                "  </l7:PlanDetails>\n" +
                "  <l7:PlanMapping>\n" +
                "    <l7:Id>abc</l7:Id>\n" +
                "    <l7:Id>def</l7:Id>\n" +
                "    <l7:Id>ghi</l7:Id>\n" +
                "  </l7:PlanMapping>\n" +
                "  <l7:PlanPolicy>policy xml2</l7:PlanPolicy>\n" +
                "</l7:AccountPlan>\n" +
                "</l7:AccountPlans>";

        final AccountPlanListResource planList = (AccountPlanListResource) unmarshaller.unmarshal(xml, AccountPlanListResource.class);

        assertEquals(2, planList.getAccountPlans().size());
        final AccountPlanResource plan1 = planList.getAccountPlans().get(0);
        assertEquals("plan-abc-123", plan1.getPlanId());
        assertEquals("Gold", plan1.getPlanName());
        assertEquals(lastUpdate, plan1.getLastUpdate());
        assertEquals("policy xml", plan1.getPolicyXml());
        assertTrue(plan1.isDefaultPlan());
        ThroughputQuotaDetails details = plan1.getPlanDetails().getThroughputQuota();
        assertTrue(details.isEnabled());
        assertEquals(100000, details.getQuota());
        assertEquals(4, details.getTimeUnit());
        assertEquals(2, details.getCounterStrategy());
        List<String> orgIds = plan1.getPlanMapping().getIds();
        assertEquals(3, orgIds.size());
        assertEquals("123", orgIds.get(0));
        assertEquals("456", orgIds.get(1));
        assertEquals("789", orgIds.get(2));
        final AccountPlanResource plan2 = planList.getAccountPlans().get(1);
        assertEquals("plan-def-456", plan2.getPlanId());
        assertEquals("Silver", plan2.getPlanName());
        assertEquals(lastUpdate, plan2.getLastUpdate());
        assertEquals("policy xml2", plan2.getPolicyXml());
        assertFalse(plan2.isDefaultPlan());
        details = plan2.getPlanDetails().getThroughputQuota();
        assertTrue(details.isEnabled());
        assertEquals(200000, details.getQuota());
        assertEquals(1, details.getTimeUnit());
        assertEquals(1, details.getCounterStrategy());
        orgIds = plan2.getPlanMapping().getIds();
        assertEquals(3, orgIds.size());
        assertEquals("abc", orgIds.get(0));
        assertEquals("def", orgIds.get(1));
        assertEquals("ghi", orgIds.get(2));
    }

    @Test
    public void unmarshalNoPlanId() throws Exception {

        final String xml = "<l7:AccountPlans xmlns:l7=\"http://ns.l7tech.com/2012/04/api-management\">\n" +
                "<l7:AccountPlan>\n" +
                "  <l7:PlanName>Gold</l7:PlanName>\n" +
                "  <l7:LastUpdated>" + LAST_UPDATE_STRING + "</l7:LastUpdated>\n" +
                "  <l7:DefaultPlan>true</l7:DefaultPlan>\n" +
                "  <l7:PlanDetails>\n" +
                "    <l7:ThroughputQuota l7:enabled=\"true\">\n" +
                "      <l7:Quota>100000</l7:Quota>\n" +
                "      <l7:TimeUnit>4</l7:TimeUnit>\n" +
                "      <l7:CounterStrategy>2</l7:CounterStrategy>\n" +
                "    </l7:ThroughputQuota>\n" +
                "  </l7:PlanDetails>\n" +
                "  <l7:PlanMapping>\n" +
                "    <l7:Id>123</l7:Id>\n" +
                "    <l7:Id>456</l7:Id>\n" +
                "    <l7:Id>789</l7:Id>\n" +
                "  </l7:PlanMapping>\n" +
                "  <l7:PlanPolicy>policy xml</l7:PlanPolicy>\n" +
                "</l7:AccountPlan>\n" +
                "</l7:AccountPlans>";

        final AccountPlanListResource planList = (AccountPlanListResource) unmarshaller.unmarshal(xml, AccountPlanListResource.class);

        final AccountPlanResource plan = planList.getAccountPlans().get(0);
        assertEquals(1, planList.getAccountPlans().size());
        assertTrue(plan.getPlanId().isEmpty());
        assertEquals("Gold", plan.getPlanName());
        assertEquals(lastUpdate, plan.getLastUpdate());
        assertEquals("policy xml", plan.getPolicyXml());
        assertTrue(plan.isDefaultPlan());
        ThroughputQuotaDetails details = plan.getPlanDetails().getThroughputQuota();
        assertTrue(details.isEnabled());
        assertEquals(100000, details.getQuota());
        assertEquals(4, details.getTimeUnit());
        assertEquals(2, details.getCounterStrategy());
        List<String> orgIds = plan.getPlanMapping().getIds();
        assertEquals(3, orgIds.size());
        assertEquals("123", orgIds.get(0));
        assertEquals("456", orgIds.get(1));
        assertEquals("789", orgIds.get(2));
    }

    @Test
    public void unmarshalNoPlanName() throws Exception {

        final String xml = "<l7:AccountPlans xmlns:l7=\"http://ns.l7tech.com/2012/04/api-management\">\n" +
                "<l7:AccountPlan>\n" +
                "  <l7:PlanId>plan-abc-123</l7:PlanId>\n" +
                "  <l7:LastUpdated>" + LAST_UPDATE_STRING + "</l7:LastUpdated>\n" +
                "  <l7:DefaultPlan>true</l7:DefaultPlan>\n" +
                "  <l7:PlanDetails>\n" +
                "    <l7:ThroughputQuota l7:enabled=\"true\">\n" +
                "      <l7:Quota>100000</l7:Quota>\n" +
                "      <l7:TimeUnit>4</l7:TimeUnit>\n" +
                "      <l7:CounterStrategy>2</l7:CounterStrategy>\n" +
                "    </l7:ThroughputQuota>\n" +
                "  </l7:PlanDetails>\n" +
                "  <l7:PlanMapping>\n" +
                "    <l7:Id>123</l7:Id>\n" +
                "    <l7:Id>456</l7:Id>\n" +
                "    <l7:Id>789</l7:Id>\n" +
                "  </l7:PlanMapping>\n" +
                "  <l7:PlanPolicy>policy xml</l7:PlanPolicy>\n" +
                "</l7:AccountPlan>\n" +
                "</l7:AccountPlans>";

        final AccountPlanListResource planList = (AccountPlanListResource) unmarshaller.unmarshal(xml, AccountPlanListResource.class);

        assertEquals(1, planList.getAccountPlans().size());
        final AccountPlanResource plan = planList.getAccountPlans().get(0);
        assertEquals("plan-abc-123", plan.getPlanId());
        assertTrue(plan.getPlanName().isEmpty());
        assertEquals(lastUpdate, plan.getLastUpdate());
        assertEquals("policy xml", plan.getPolicyXml());
        assertTrue(plan.isDefaultPlan());
        ThroughputQuotaDetails details = plan.getPlanDetails().getThroughputQuota();
        assertTrue(details.isEnabled());
        assertEquals(100000, details.getQuota());
        assertEquals(4, details.getTimeUnit());
        assertEquals(2, details.getCounterStrategy());
        List<String> orgIds = plan.getPlanMapping().getIds();
        assertEquals(3, orgIds.size());
        assertEquals("123", orgIds.get(0));
        assertEquals("456", orgIds.get(1));
        assertEquals("789", orgIds.get(2));
    }

    @Test
    public void unmarshalNoLastUpdate() throws Exception {

        final String xml = "<l7:AccountPlans xmlns:l7=\"http://ns.l7tech.com/2012/04/api-management\">\n" +
                "<l7:AccountPlan>\n" +
                "  <l7:PlanId>plan-abc-123</l7:PlanId>\n" +
                "  <l7:PlanName>Gold</l7:PlanName>\n" +
                "  <l7:DefaultPlan>true</l7:DefaultPlan>\n" +
                "  <l7:PlanDetails>\n" +
                "    <l7:ThroughputQuota l7:enabled=\"true\">\n" +
                "      <l7:Quota>100000</l7:Quota>\n" +
                "      <l7:TimeUnit>4</l7:TimeUnit>\n" +
                "      <l7:CounterStrategy>2</l7:CounterStrategy>\n" +
                "    </l7:ThroughputQuota>\n" +
                "  </l7:PlanDetails>\n" +
                "  <l7:PlanMapping>\n" +
                "    <l7:Id>123</l7:Id>\n" +
                "    <l7:Id>456</l7:Id>\n" +
                "    <l7:Id>789</l7:Id>\n" +
                "  </l7:PlanMapping>\n" +
                "  <l7:PlanPolicy>policy xml</l7:PlanPolicy>\n" +
                "</l7:AccountPlan>\n" +
                "</l7:AccountPlans>";

        final AccountPlanListResource planList = (AccountPlanListResource) unmarshaller.unmarshal(xml, AccountPlanListResource.class);

        assertEquals(1, planList.getAccountPlans().size());
        final AccountPlanResource plan = planList.getAccountPlans().get(0);
        assertEquals("plan-abc-123", plan.getPlanId());
        assertEquals("Gold", plan.getPlanName());
        assertNull(plan.getLastUpdate());
        assertEquals("policy xml", plan.getPolicyXml());
        assertTrue(plan.isDefaultPlan());
        ThroughputQuotaDetails details = plan.getPlanDetails().getThroughputQuota();
        assertTrue(details.isEnabled());
        assertEquals(100000, details.getQuota());
        assertEquals(4, details.getTimeUnit());
        assertEquals(2, details.getCounterStrategy());
        List<String> orgIds = plan.getPlanMapping().getIds();
        assertEquals(3, orgIds.size());
        assertEquals("123", orgIds.get(0));
        assertEquals("456", orgIds.get(1));
        assertEquals("789", orgIds.get(2));
    }

    @Test
    public void unmarshalLastUpdateMissingTime() throws Exception {

        final String xml = "<l7:AccountPlans xmlns:l7=\"http://ns.l7tech.com/2012/04/api-management\">\n" +
                "<l7:AccountPlan>\n" +
                "  <l7:PlanId>plan-abc-123</l7:PlanId>\n" +
                "  <l7:PlanName>Gold</l7:PlanName>\n" +
                "  <l7:LastUpdated>2012-11-05</l7:LastUpdated>\n" +
                "  <l7:DefaultPlan>true</l7:DefaultPlan>\n" +
                "  <l7:PlanDetails>\n" +
                "    <l7:ThroughputQuota l7:enabled=\"true\">\n" +
                "      <l7:Quota>100000</l7:Quota>\n" +
                "      <l7:TimeUnit>4</l7:TimeUnit>\n" +
                "      <l7:CounterStrategy>2</l7:CounterStrategy>\n" +
                "    </l7:ThroughputQuota>\n" +
                "  </l7:PlanDetails>\n" +
                "  <l7:PlanMapping>\n" +
                "    <l7:Id>123</l7:Id>\n" +
                "    <l7:Id>456</l7:Id>\n" +
                "    <l7:Id>789</l7:Id>\n" +
                "  </l7:PlanMapping>\n" +
                "  <l7:PlanPolicy>policy xml</l7:PlanPolicy>\n" +
                "</l7:AccountPlan>\n" +
                "</l7:AccountPlans>";

        final AccountPlanListResource planList = (AccountPlanListResource) unmarshaller.unmarshal(xml, AccountPlanListResource.class);

        assertEquals(1, planList.getAccountPlans().size());
        final AccountPlanResource plan = planList.getAccountPlans().get(0);
        assertEquals("plan-abc-123", plan.getPlanId());
        assertEquals("Gold", plan.getPlanName());
        assertEquals(new GregorianCalendar(2012, Calendar.NOVEMBER, 5).getTime(), plan.getLastUpdate());
        assertEquals("policy xml", plan.getPolicyXml());
        assertTrue(plan.isDefaultPlan());
        ThroughputQuotaDetails details = plan.getPlanDetails().getThroughputQuota();
        assertTrue(details.isEnabled());
        assertEquals(100000, details.getQuota());
        assertEquals(4, details.getTimeUnit());
        assertEquals(2, details.getCounterStrategy());
        List<String> orgIds = plan.getPlanMapping().getIds();
        assertEquals(3, orgIds.size());
        assertEquals("123", orgIds.get(0));
        assertEquals("456", orgIds.get(1));
        assertEquals("789", orgIds.get(2));
    }

    @Test
    public void unmarshalNoPlanPolicy() throws Exception {

        final String xml = "<l7:AccountPlans xmlns:l7=\"http://ns.l7tech.com/2012/04/api-management\">\n" +
                "<l7:AccountPlan>\n" +
                "  <l7:PlanId>plan-abc-123</l7:PlanId>\n" +
                "  <l7:PlanName>Gold</l7:PlanName>\n" +
                "  <l7:LastUpdated>" + LAST_UPDATE_STRING + "</l7:LastUpdated>\n" +
                "  <l7:DefaultPlan>true</l7:DefaultPlan>\n" +
                "  <l7:PlanDetails>\n" +
                "    <l7:ThroughputQuota l7:enabled=\"true\">\n" +
                "      <l7:Quota>100000</l7:Quota>\n" +
                "      <l7:TimeUnit>4</l7:TimeUnit>\n" +
                "      <l7:CounterStrategy>2</l7:CounterStrategy>\n" +
                "    </l7:ThroughputQuota>\n" +
                "  </l7:PlanDetails>\n" +
                "  <l7:PlanMapping>\n" +
                "    <l7:Id>123</l7:Id>\n" +
                "    <l7:Id>456</l7:Id>\n" +
                "    <l7:Id>789</l7:Id>\n" +
                "  </l7:PlanMapping>\n" +
                "</l7:AccountPlan>\n" +
                "</l7:AccountPlans>";

        final AccountPlanListResource planList = (AccountPlanListResource) unmarshaller.unmarshal(xml, AccountPlanListResource.class);

        assertEquals(1, planList.getAccountPlans().size());
        final AccountPlanResource plan = planList.getAccountPlans().get(0);
        assertEquals("plan-abc-123", plan.getPlanId());
        assertEquals("Gold", plan.getPlanName());
        assertEquals(lastUpdate, plan.getLastUpdate());
        assertTrue(plan.getPolicyXml().isEmpty());
        assertTrue(plan.isDefaultPlan());
        ThroughputQuotaDetails details = plan.getPlanDetails().getThroughputQuota();
        assertTrue(details.isEnabled());
        assertEquals(100000, details.getQuota());
        assertEquals(4, details.getTimeUnit());
        assertEquals(2, details.getCounterStrategy());
        List<String> orgIds = plan.getPlanMapping().getIds();
        assertEquals(3, orgIds.size());
        assertEquals("123", orgIds.get(0));
        assertEquals("456", orgIds.get(1));
        assertEquals("789", orgIds.get(2));
    }

    @Test
    public void unmarshalNoDefaultPlan() throws Exception {

        final String xml = "<l7:AccountPlans xmlns:l7=\"http://ns.l7tech.com/2012/04/api-management\">\n" +
                "<l7:AccountPlan>\n" +
                "  <l7:PlanId>plan-abc-123</l7:PlanId>\n" +
                "  <l7:PlanName>Gold</l7:PlanName>\n" +
                "  <l7:LastUpdated>" + LAST_UPDATE_STRING + "</l7:LastUpdated>\n" +
                "  <l7:PlanDetails>\n" +
                "    <l7:ThroughputQuota l7:enabled=\"true\">\n" +
                "      <l7:Quota>100000</l7:Quota>\n" +
                "      <l7:TimeUnit>4</l7:TimeUnit>\n" +
                "      <l7:CounterStrategy>2</l7:CounterStrategy>\n" +
                "    </l7:ThroughputQuota>\n" +
                "  </l7:PlanDetails>\n" +
                "  <l7:PlanMapping>\n" +
                "    <l7:Id>123</l7:Id>\n" +
                "    <l7:Id>456</l7:Id>\n" +
                "    <l7:Id>789</l7:Id>\n" +
                "  </l7:PlanMapping>\n" +
                "  <l7:PlanPolicy>policy xml</l7:PlanPolicy>\n" +
                "</l7:AccountPlan>\n" +
                "</l7:AccountPlans>";

        final AccountPlanListResource planList = (AccountPlanListResource) unmarshaller.unmarshal(xml, AccountPlanListResource.class);

        assertEquals(1, planList.getAccountPlans().size());
        final AccountPlanResource plan = planList.getAccountPlans().get(0);
        assertEquals("plan-abc-123", plan.getPlanId());
        assertEquals("Gold", plan.getPlanName());
        assertEquals(lastUpdate, plan.getLastUpdate());
        assertEquals("policy xml", plan.getPolicyXml());
        assertFalse(plan.isDefaultPlan());
        ThroughputQuotaDetails details = plan.getPlanDetails().getThroughputQuota();
        assertTrue(details.isEnabled());
        assertEquals(100000, details.getQuota());
        assertEquals(4, details.getTimeUnit());
        assertEquals(2, details.getCounterStrategy());
        List<String> orgIds = plan.getPlanMapping().getIds();
        assertEquals(3, orgIds.size());
        assertEquals("123", orgIds.get(0));
        assertEquals("456", orgIds.get(1));
        assertEquals("789", orgIds.get(2));
    }

    @Test
    public void unmarshalNoPlanDetails() throws Exception {
        final String xml = "<l7:AccountPlans xmlns:l7=\"http://ns.l7tech.com/2012/04/api-management\">\n" +
                "<l7:AccountPlan>\n" +
                "  <l7:PlanId>plan-abc-123</l7:PlanId>\n" +
                "  <l7:PlanName>Gold</l7:PlanName>\n" +
                "  <l7:LastUpdated>" + LAST_UPDATE_STRING + "</l7:LastUpdated>\n" +
                "  <l7:DefaultPlan>true</l7:DefaultPlan>\n" +
                "  <l7:PlanMapping>\n" +
                "    <l7:Id>123</l7:Id>\n" +
                "    <l7:Id>456</l7:Id>\n" +
                "    <l7:Id>789</l7:Id>\n" +
                "  </l7:PlanMapping>\n" +
                "  <l7:PlanPolicy>policy xml</l7:PlanPolicy>\n" +
                "</l7:AccountPlan>\n" +
                "</l7:AccountPlans>";

        final AccountPlanListResource planList = (AccountPlanListResource) unmarshaller.unmarshal(xml, AccountPlanListResource.class);

        assertEquals(1, planList.getAccountPlans().size());
        final AccountPlanResource plan = planList.getAccountPlans().get(0);
        assertEquals("plan-abc-123", plan.getPlanId());
        assertEquals("Gold", plan.getPlanName());
        assertEquals(lastUpdate, plan.getLastUpdate());
        assertEquals("policy xml", plan.getPolicyXml());
        assertTrue(plan.isDefaultPlan());
        ThroughputQuotaDetails details = plan.getPlanDetails().getThroughputQuota();
        assertFalse(details.isEnabled());
        assertEquals(0, details.getQuota());
        assertEquals(0, details.getTimeUnit());
        assertEquals(0, details.getCounterStrategy());
        List<String> orgIds = plan.getPlanMapping().getIds();
        assertEquals(3, orgIds.size());
        assertEquals("123", orgIds.get(0));
        assertEquals("456", orgIds.get(1));
        assertEquals("789", orgIds.get(2));
    }

    @Test
    public void unmarshalNoPlanMapping() throws Exception {
        final String xml = "<l7:AccountPlans xmlns:l7=\"http://ns.l7tech.com/2012/04/api-management\">\n" +
                "<l7:AccountPlan>\n" +
                "  <l7:PlanId>plan-abc-123</l7:PlanId>\n" +
                "  <l7:PlanName>Gold</l7:PlanName>\n" +
                "  <l7:LastUpdated>" + LAST_UPDATE_STRING + "</l7:LastUpdated>\n" +
                "  <l7:DefaultPlan>true</l7:DefaultPlan>\n" +
                "  <l7:PlanDetails>\n" +
                "    <l7:ThroughputQuota l7:enabled=\"true\">\n" +
                "      <l7:Quota>100000</l7:Quota>\n" +
                "      <l7:TimeUnit>4</l7:TimeUnit>\n" +
                "      <l7:CounterStrategy>2</l7:CounterStrategy>\n" +
                "    </l7:ThroughputQuota>\n" +
                "  </l7:PlanDetails>\n" +
                "  <l7:PlanPolicy>policy xml</l7:PlanPolicy>\n" +
                "</l7:AccountPlan>\n" +
                "</l7:AccountPlans>";

        final AccountPlanListResource planList = (AccountPlanListResource) unmarshaller.unmarshal(xml, AccountPlanListResource.class);

        assertEquals(1, planList.getAccountPlans().size());
        final AccountPlanResource plan = planList.getAccountPlans().get(0);
        assertEquals("plan-abc-123", plan.getPlanId());
        assertEquals("Gold", plan.getPlanName());
        assertEquals(lastUpdate, plan.getLastUpdate());
        assertEquals("policy xml", plan.getPolicyXml());
        assertTrue(plan.isDefaultPlan());
        ThroughputQuotaDetails details = plan.getPlanDetails().getThroughputQuota();
        assertTrue(details.isEnabled());
        assertEquals(100000, details.getQuota());
        assertEquals(4, details.getTimeUnit());
        assertEquals(2, details.getCounterStrategy());
        List<String> orgIds = plan.getPlanMapping().getIds();
        assertEquals(0, orgIds.size());
    }

    @Test(expected = JAXBException.class)
    public void unmarshalInvalidXml() throws Exception {
        final String xml = "<invalid></invalid>";

        unmarshaller.unmarshal(xml, AccountPlanListResource.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void unmarshalClassNotResource() throws Exception {
        final String xml = "<l7:AccountPlans xmlns:l7=\"http://ns.l7tech.com/2012/04/api-management\">\n" +
                "<l7:AccountPlan>\n" +
                "  <l7:PlanId>plan-abc-123</l7:PlanId>\n" +
                "  <l7:PlanName>Gold</l7:PlanName>\n" +
                "  <l7:LastUpdated>" + LAST_UPDATE_STRING + "</l7:LastUpdated>\n" +
                "  <l7:DefaultPlan>true</l7:DefaultPlan>\n" +
                "  <l7:PlanDetails>\n" +
                "    <l7:ThroughputQuota l7:enabled=\"true\">\n" +
                "      <l7:Quota>100000</l7:Quota>\n" +
                "      <l7:TimeUnit>4</l7:TimeUnit>\n" +
                "      <l7:CounterStrategy>2</l7:CounterStrategy>\n" +
                "    </l7:ThroughputQuota>\n" +
                "  </l7:PlanDetails>\n" +
                "  <l7:PlanPolicy>policy xml</l7:PlanPolicy>\n" +
                "</l7:AccountPlan>\n" +
                "</l7:AccountPlans>";

        unmarshaller.unmarshal(xml, String.class);
    }

    @Test(expected = JAXBException.class)
    public void unmarshallWrongResourceClass() throws Exception {
        final String xml = "<l7:AccountPlans xmlns:l7=\"http://ns.l7tech.com/2012/04/api-management\">\n" +
                "<l7:AccountPlan>\n" +
                "  <l7:PlanId>plan-abc-123</l7:PlanId>\n" +
                "  <l7:PlanName>Gold</l7:PlanName>\n" +
                "  <l7:LastUpdated>" + LAST_UPDATE_STRING + "</l7:LastUpdated>\n" +
                "  <l7:DefaultPlan>true</l7:DefaultPlan>\n" +
                "  <l7:PlanDetails>\n" +
                "    <l7:ThroughputQuota l7:enabled=\"true\">\n" +
                "      <l7:Quota>100000</l7:Quota>\n" +
                "      <l7:TimeUnit>4</l7:TimeUnit>\n" +
                "      <l7:CounterStrategy>2</l7:CounterStrategy>\n" +
                "    </l7:ThroughputQuota>\n" +
                "  </l7:PlanDetails>\n" +
                "  <l7:PlanPolicy>policy xml</l7:PlanPolicy>\n" +
                "</l7:AccountPlan>\n" +
                "</l7:AccountPlans>";

        unmarshaller.unmarshal(xml, AccountPlanResource.class);
    }
}
