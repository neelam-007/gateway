package com.l7tech.external.assertions.apiportalintegration.server.portalmanagedservices.manager;

import static junit.framework.Assert.*;
import com.l7tech.external.assertions.apiportalintegration.server.PortalGenericEntityManagerTestParent;
import com.l7tech.external.assertions.apiportalintegration.server.PortalManagedService;
import com.l7tech.objectmodel.Goid;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author jbagtas, 9/16/13
 */
public class PortalManagedServiceGenericEntityManagerTest extends PortalGenericEntityManagerTestParent {
  private static final Goid SERVICE_A = new Goid(0, 1L);
  private static final String SERVICE_A_STRING = String.valueOf(SERVICE_A);

  private PortalManagedServiceManager portalManagedServiceManager;
  private Goid testId;

  @Before
  public void setUp() throws Exception {
    portalManagedServiceManager = new PortalManagedServiceManagerImpl(applicationContext);
  }

  @After
  public void tearDown() throws Exception {
    genericEntityManager.unRegisterClass(PortalManagedService.class.getName());
  }

  @Test
  public void testSave() throws Exception {
    final PortalManagedService portalManagedService = createPortalManagedService(null, "a1", SERVICE_A, "group1");

    PortalManagedService service = portalManagedServiceManager.add(portalManagedService);
    session.flush();
    assertNotSame(Goid.DEFAULT_GOID, service.getGoid());
  }

  @Test
  public void testFind() throws Exception {
    final PortalManagedService portalManagedService = createPortalManagedService(null, "a1", SERVICE_A, "group1");

    PortalManagedService service = portalManagedServiceManager.add(portalManagedService);
    session.flush();
    assertNotSame(Goid.DEFAULT_GOID, service.getGoid());

    PortalManagedService foundService = portalManagedServiceManager.find(portalManagedService.getName(), true);
    assertEquals(foundService.getApiGroup(), portalManagedService.getApiGroup());
    assertEquals(foundService.getName(), portalManagedService.getName());
    assertEquals(foundService.getDescription(), SERVICE_A_STRING);
  }

  @Test
  public void testFindAll() throws Exception {
    for (int i = 0; i < 5; i++) {
      final PortalManagedService portalManagedService = createPortalManagedService(null, "a" + i, SERVICE_A, "group1");
      portalManagedServiceManager.add(portalManagedService);
    }
    session.flush();

    List<PortalManagedService> portalManagedServices = portalManagedServiceManager.findAll();
    assertEquals(5, portalManagedServices.size());
  }

  @Test
  public void testUpdate() throws Exception {
    final PortalManagedService portalManagedService = createPortalManagedService(null, "a1", SERVICE_A, "group1");
    PortalManagedService service = portalManagedServiceManager.add(portalManagedService);
    session.flush();
    assertNotSame(Goid.DEFAULT_GOID, service.getGoid());

    PortalManagedService foundService = portalManagedServiceManager.find(portalManagedService.getName(), true);
    portalManagedServiceManager.delete(foundService.getName());
    session.flush();

    foundService.setGoid(Goid.DEFAULT_GOID);
    foundService.setApiGroup("group2");
    portalManagedServiceManager.add(foundService);
    session.flush();

    foundService = portalManagedServiceManager.find(portalManagedService.getName());
    assertEquals(foundService.getApiGroup(), "group2");
  }

  @Test
  public void testDelete() throws Exception {
    final PortalManagedService portalManagedService = createPortalManagedService(null, "a1", SERVICE_A, "group1");
    PortalManagedService service = portalManagedServiceManager.add(portalManagedService);
    session.flush();
    assertNotSame(Goid.DEFAULT_GOID, service.getGoid());

    PortalManagedService foundPortalManagedService = portalManagedServiceManager.find(portalManagedService.getName());

    portalManagedServiceManager.delete(foundPortalManagedService.getName());
    session.flush();
    assertNull(portalManagedServiceManager.find(foundPortalManagedService.getName()));
  }

  private PortalManagedService createPortalManagedService(final Goid goid, final String apiId, final Goid serviceGoid, final String apiGroup) {
    final PortalManagedService portalManagedService = new PortalManagedService();
    if (goid != null) {
      portalManagedService.setGoid(goid);
    }
    portalManagedService.setName(apiId);
    portalManagedService.setDescription(String.valueOf(serviceGoid));
    portalManagedService.setApiGroup(apiGroup);
    return portalManagedService;
  }
}
