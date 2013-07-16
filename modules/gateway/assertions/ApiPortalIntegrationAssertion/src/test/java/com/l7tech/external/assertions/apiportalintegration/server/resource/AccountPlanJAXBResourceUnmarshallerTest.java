package com.l7tech.external.assertions.apiportalintegration.server.resource;

import org.junit.Before;
import org.junit.Test;

import javax.xml.bind.JAXBException;
import java.util.*;

import static org.junit.Assert.*;

public class AccountPlanJAXBResourceUnmarshallerTest {
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
        final String xml = "<l7:AccountPlan xmlns:l7=\"http://ns.l7tech.com/2012/04/api-management\">\n" +
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
                "</l7:AccountPlan>\n";

        final AccountPlanResource plan = (AccountPlanResource) unmarshaller.unmarshal(xml, AccountPlanResource.class);

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
        assertEquals(3, orgIds.size());
        assertEquals("123", orgIds.get(0));
        assertEquals("456", orgIds.get(1));
        assertEquals("789", orgIds.get(2));
    }

    @Test
    public void unmarshalNoPlanId() throws Exception {

        final String xml = "<l7:AccountPlan xmlns:l7=\"http://ns.l7tech.com/2012/04/api-management\">\n" +
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
                "</l7:AccountPlan>\n";

        final AccountPlanResource plan = (AccountPlanResource) unmarshaller.unmarshal(xml, AccountPlanResource.class);

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

        final String xml = "<l7:AccountPlan xmlns:l7=\"http://ns.l7tech.com/2012/04/api-management\">\n" +
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
                "</l7:AccountPlan>\n";

        final AccountPlanResource plan = (AccountPlanResource) unmarshaller.unmarshal(xml, AccountPlanResource.class);

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

        final String xml = "<l7:AccountPlan xmlns:l7=\"http://ns.l7tech.com/2012/04/api-management\">\n" +
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
                "</l7:AccountPlan>\n";

        final AccountPlanResource plan = (AccountPlanResource) unmarshaller.unmarshal(xml, AccountPlanResource.class);

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

        final String xml = "<l7:AccountPlan xmlns:l7=\"http://ns.l7tech.com/2012/04/api-management\">\n" +
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
                "</l7:AccountPlan>\n";

        final AccountPlanResource plan = (AccountPlanResource) unmarshaller.unmarshal(xml, AccountPlanResource.class);

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

        final String xml = "<l7:AccountPlan xmlns:l7=\"http://ns.l7tech.com/2012/04/api-management\">\n" +
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
                "</l7:AccountPlan>\n";

        final AccountPlanResource plan = (AccountPlanResource) unmarshaller.unmarshal(xml, AccountPlanResource.class);

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

        final String xml = "<l7:AccountPlan xmlns:l7=\"http://ns.l7tech.com/2012/04/api-management\">\n" +
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
                "</l7:AccountPlan>\n";

        final AccountPlanResource plan = (AccountPlanResource) unmarshaller.unmarshal(xml, AccountPlanResource.class);

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
        final String xml = "<l7:AccountPlan xmlns:l7=\"http://ns.l7tech.com/2012/04/api-management\">\n" +
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
                "</l7:AccountPlan>\n";

        final AccountPlanResource plan = (AccountPlanResource) unmarshaller.unmarshal(xml, AccountPlanResource.class);

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
    public void unmarshalNoThroughputDetails() throws Exception {
        final String xml = "<l7:AccountPlan xmlns:l7=\"http://ns.l7tech.com/2012/04/api-management\">\n" +
                "  <l7:PlanId>plan-abc-123</l7:PlanId>\n" +
                "  <l7:PlanName>Gold</l7:PlanName>\n" +
                "  <l7:LastUpdated>" + LAST_UPDATE_STRING + "</l7:LastUpdated>\n" +
                "  <l7:DefaultPlan>true</l7:DefaultPlan>\n" +
                "  <l7:PlanDetails/>\n" +
                "  <l7:PlanMapping>\n" +
                "    <l7:Id>123</l7:Id>\n" +
                "    <l7:Id>456</l7:Id>\n" +
                "    <l7:Id>789</l7:Id>\n" +
                "  </l7:PlanMapping>\n" +
                "  <l7:PlanPolicy>policy xml</l7:PlanPolicy>\n" +
                "</l7:AccountPlan>\n";

        final AccountPlanResource plan = (AccountPlanResource) unmarshaller.unmarshal(xml, AccountPlanResource.class);

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
        final String xml = "<l7:AccountPlan xmlns:l7=\"http://ns.l7tech.com/2012/04/api-management\">\n" +
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
                "</l7:AccountPlan>\n";

        final AccountPlanResource plan = (AccountPlanResource) unmarshaller.unmarshal(xml, AccountPlanResource.class);

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

        unmarshaller.unmarshal(xml, AccountPlanResource.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void unmarshalClassNotResource() throws Exception {
        final String xml = "<l7:AccountPlan xmlns:l7=\"http://ns.l7tech.com/2012/04/api-management\">\n" +
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
                "</l7:AccountPlan>\n";

        unmarshaller.unmarshal(xml, String.class);
    }

    @Test(expected = JAXBException.class)
    public void unmarshallWrongResourceClass() throws Exception {
        final String xml = "<l7:AccountPlan xmlns:l7=\"http://ns.l7tech.com/2012/04/api-management\">\n" +
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
                "</l7:AccountPlan>\n";

        unmarshaller.unmarshal(xml, ApiResource.class);
    }
}


