package com.l7tech.external.assertions.apiportalintegration.server.accountplan.manager;

import static junit.framework.Assert.*;
import com.l7tech.external.assertions.apiportalintegration.server.PortalGenericEntityManagerTest;
import com.l7tech.external.assertions.apiportalintegration.server.accountplan.AccountPlan;
import com.l7tech.objectmodel.Goid;
import java.util.Date;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author jbagtas, 9/16/13
 */
public class AccountPlanGenericEntityManagerTest extends PortalGenericEntityManagerTest {
  private static final String PLAN_DESCRIPTION = "The Plan";
  private static final String PLAN_NAME = "The Plan";
  private static final String POLICY_XML = "the xml";
  private static final String ORG_IDS = "1,2";
  private static final Date DATE = new Date();
  public static final boolean DEFAULT_PLAN_ENABLED = true;
  public static final boolean THROUGHPUT_QUOTA_ENABLED = true;
  public static final int QUOTA_10 = 10;
  public static final int TIME_UNIT_1 = 1;
  public static final int COUNTER_STRATEGY_1 = 1;

  private AccountPlanManager accountPlanManager;
  private Goid testId;

  @Before
  public void setUp() throws Exception {
    accountPlanManager = new AccountPlanManager(applicationContext);
  }

  @After
  public void tearDown() throws Exception {
    genericEntityManager.unRegisterClass(AccountPlan.class.getName());
  }

  @Test
  public void testSave() throws Exception {
    AccountPlan plan = createAccountPlan(null, "plan1", PLAN_NAME, DATE, POLICY_XML, DEFAULT_PLAN_ENABLED, THROUGHPUT_QUOTA_ENABLED, QUOTA_10, TIME_UNIT_1, COUNTER_STRATEGY_1, ORG_IDS);

    plan = accountPlanManager.add(plan);
    session.flush();
    assertNotNull(plan.getGoid());
  }

  @Test
  public void testFind() throws Exception {
    AccountPlan plan = createAccountPlan(null, "plan1", PLAN_NAME, DATE, POLICY_XML, DEFAULT_PLAN_ENABLED, THROUGHPUT_QUOTA_ENABLED, QUOTA_10, TIME_UNIT_1, COUNTER_STRATEGY_1, ORG_IDS);

    plan = accountPlanManager.add(plan);
    session.flush();

    assertNotNull(plan.getGoid());
    AccountPlan foundPlan = accountPlanManager.find(plan.getName());

    assertEquals(plan.getGoid(), foundPlan.getGoid());
  }

  @Test
  public void testFindAll() throws Exception {
    for (int i = 0; i < 5; i++) {
      AccountPlan plan = createAccountPlan(null, "plan" + i, PLAN_NAME, DATE, POLICY_XML, DEFAULT_PLAN_ENABLED, THROUGHPUT_QUOTA_ENABLED, QUOTA_10, TIME_UNIT_1, COUNTER_STRATEGY_1, ORG_IDS);
      accountPlanManager.add(plan);
    }
    session.flush();

    List<AccountPlan> plans = accountPlanManager.findAll();
    assertEquals(5, plans.size());
  }

  @Test
  public void testUpdate() throws Exception {
    AccountPlan plan = createAccountPlan(null, "plan1", PLAN_NAME, DATE, POLICY_XML, DEFAULT_PLAN_ENABLED, THROUGHPUT_QUOTA_ENABLED, QUOTA_10, TIME_UNIT_1, COUNTER_STRATEGY_1, ORG_IDS);
    plan = accountPlanManager.add(plan);
    session.flush();

    assertNotNull(plan.getGoid());
    AccountPlan foundPlan = accountPlanManager.find(plan.getName());
    foundPlan.setPolicyXml(POLICY_XML + "updated");
    accountPlanManager.update(foundPlan);
    session.flush();

    foundPlan = accountPlanManager.find(plan.getName());
    assertEquals(POLICY_XML + "updated", foundPlan.getPolicyXml());
  }

  @Test
  public void testDelete() throws Exception {
    AccountPlan plan = createAccountPlan(null, "plan1", PLAN_NAME, DATE, POLICY_XML, DEFAULT_PLAN_ENABLED, THROUGHPUT_QUOTA_ENABLED, QUOTA_10, TIME_UNIT_1, COUNTER_STRATEGY_1, ORG_IDS);
    plan = accountPlanManager.add(plan);
    session.flush();

    assertNotNull(plan.getGoid());
    AccountPlan foundPlan = accountPlanManager.find(plan.getName());

    assertEquals(plan.getGoid(), foundPlan.getGoid());

    accountPlanManager.delete(plan.getName());
    session.flush();
    assertNull(accountPlanManager.find(plan.getName()));
  }

  private AccountPlan createAccountPlan(final Goid goid, final String planId, final String planName, final Date lastUpdate, final String policyXml, final boolean defaultPlan, final boolean throughputQuotaEnabled, final int quota, final int timeUnit, final int counterStrategy, final String organizationIds) {
    final AccountPlan plan = new AccountPlan();
    if (goid != null) {
      plan.setGoid(goid);
    }
    plan.setName(planId);
    plan.setDescription(planName);
    plan.setLastUpdate(lastUpdate);
    plan.setPolicyXml(policyXml);
    plan.setDefaultPlan(defaultPlan);
    plan.setThroughputQuotaEnabled(throughputQuotaEnabled);
    plan.setQuota(quota);
    plan.setTimeUnit(timeUnit);
    plan.setCounterStrategy(counterStrategy);
    plan.setIds(organizationIds);
    return plan;
  }
}
