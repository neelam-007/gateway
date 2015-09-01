package com.l7tech.external.assertions.apiportalintegration.server.apiplan.manager;

import static junit.framework.Assert.*;
import com.l7tech.external.assertions.apiportalintegration.server.PortalGenericEntityManagerTestParent;
import com.l7tech.external.assertions.apiportalintegration.server.apiplan.ApiPlan;
import com.l7tech.objectmodel.Goid;
import java.util.Date;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author jbagtas, 9/16/13
 */
public class ApiPlanGenericEntityManagerTest extends PortalGenericEntityManagerTestParent {
  private static final String PLAN_DESCRIPTION = "The Plan";
  private static final String POLICY_XML = "the xml";
  private static final Date DATE = new Date();

  private ApiPlanManager apiPlanManager;
  private Goid testId;

  @Before
  public void setUp() throws Exception {
    apiPlanManager = new ApiPlanManager(applicationContext);
  }

  @After
  public void tearDown() throws Exception {
    genericEntityManager.unRegisterClass(ApiPlan.class.getName());
  }

  @Test
  public void testSave() throws Exception {
    ApiPlan plan = createApiPlan(null, "test1", PLAN_DESCRIPTION, DATE, POLICY_XML);

    plan = apiPlanManager.add(plan);
    session.flush();
    assertNotNull(plan.getGoid());
  }

  @Test
  public void testFind() throws Exception {
    ApiPlan plan = createApiPlan(null, "test1", PLAN_DESCRIPTION, DATE, POLICY_XML);

    plan = apiPlanManager.add(plan);
    session.flush();

    assertNotNull(plan.getGoid());
    ApiPlan foundPlan = apiPlanManager.find(plan.getName(), true);

    assertEquals(plan.getGoid(), foundPlan.getGoid());
  }

  @Test
  public void testFindAll() throws Exception {
    for (int i = 0; i < 5; i++) {
      ApiPlan plan = createApiPlan(null, "test" + i, PLAN_DESCRIPTION, DATE, POLICY_XML);
      apiPlanManager.add(plan);
    }
    session.flush();

    List<ApiPlan> plans = apiPlanManager.findAll();
    assertEquals(5, plans.size());
  }

  @Test
  public void testUpdate() throws Exception {
    ApiPlan plan = createApiPlan(null, "test1", PLAN_DESCRIPTION, DATE, POLICY_XML);
    plan = apiPlanManager.add(plan);
    session.flush();

    assertNotNull(plan.getGoid());
    ApiPlan foundPlan = apiPlanManager.find(plan.getName());
    foundPlan.setPolicyXml(POLICY_XML + "updated");
    apiPlanManager.update(foundPlan);
    session.flush();

    foundPlan = apiPlanManager.find(plan.getName());
    assertEquals(POLICY_XML + "updated", foundPlan.getPolicyXml());
  }

  @Test
  public void testDelete() throws Exception {
    ApiPlan plan = createApiPlan(null, "test1", PLAN_DESCRIPTION, DATE, POLICY_XML);
    plan = apiPlanManager.add(plan);
    session.flush();

    assertNotNull(plan.getGoid());
    ApiPlan foundPlan = apiPlanManager.find(plan.getName());

    assertEquals(plan.getGoid(), foundPlan.getGoid());

    apiPlanManager.delete(plan.getName());
    session.flush();
    assertNull(apiPlanManager.find(plan.getName()));
  }

  private ApiPlan createApiPlan(final Goid goid, final String planId, final String planDescription, final Date lastUpdate, final String policyXml) {
    final ApiPlan plan = new ApiPlan();
    if (goid != null) {
      plan.setGoid(goid);
    }
    plan.setName(planId);
    plan.setDescription(planDescription);
    plan.setLastUpdate(lastUpdate);
    plan.setPolicyXml(policyXml);
    return plan;
  }
}
