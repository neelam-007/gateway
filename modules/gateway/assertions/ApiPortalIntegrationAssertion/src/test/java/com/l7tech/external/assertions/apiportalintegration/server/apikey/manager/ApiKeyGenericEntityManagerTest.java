package com.l7tech.external.assertions.apiportalintegration.server.apikey.manager;

import static junit.framework.Assert.*;
import com.l7tech.external.assertions.apiportalintegration.server.PortalGenericEntityManagerTest;
import com.l7tech.objectmodel.Goid;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author jbagtas, 9/16/13
 */
public class ApiKeyGenericEntityManagerTest extends PortalGenericEntityManagerTest {

  private ApiKeyManager apiKeyManager;
  private Goid testId;
  private HashMap<String, String> serviceIds;

  @Before
  public void setUp() throws Exception {
    apiKeyManager = new ApiKeyManager(applicationContext);
    serviceIds = new HashMap<>();
    serviceIds.put("serviceId1", "keyId1");
    serviceIds.put("serviceId2", "keyId2");
  }

  @After
  public void tearDown() throws Exception {
    genericEntityManager.unRegisterClass(ApiKey.class.getName());
  }

  @Test
  public void testSave() throws Exception {
    ApiKey key = createApiKey("testkey", serviceIds);

    key = apiKeyManager.add(key);
    session.flush();
    assertNotNull(key.getGoid());
  }

  @Test
  public void testFind() throws Exception {
    ApiKey key = createApiKey("testkey", serviceIds);

    key = apiKeyManager.add(key);
    session.flush();

    assertNotNull(key.getGoid());
    ApiKey foundkey = apiKeyManager.find(key.getName(), true);

    assertEquals(key.getGoid(), foundkey.getGoid());
  }

  @Test
  public void testFindAll() throws Exception {
    for (int i = 0; i < 5; i++) {
      ApiKey key = createApiKey("testkey" + i, serviceIds);
      apiKeyManager.add(key);
    }
    session.flush();

    List<ApiKey> keys = apiKeyManager.findAll();
    assertEquals(5, keys.size());
  }

  @Test
  public void testUpdate() throws Exception {
    ApiKey key = createApiKey("testkey", new HashMap<String, String>());
    key = apiKeyManager.add(key);
    session.flush();
    assertNotNull(key.getGoid());

    ApiKey foundkey = apiKeyManager.find(key.getName(), true);
    serviceIds.put("serviceId3", "keyId3");
    foundkey.setServiceIds(serviceIds);
    apiKeyManager.delete("testkey");
    session.flush();

    foundkey.setGoid(Goid.DEFAULT_GOID);
    apiKeyManager.add(foundkey);
    session.flush();

    foundkey = apiKeyManager.find(key.getName(), true);
    assertEquals(3, foundkey.getServiceIds().keySet().size());
  }

  @Test
  public void testDelete() throws Exception {
    ApiKey key = createApiKey("testkey", serviceIds);
    key = apiKeyManager.add(key);
    session.flush();

    assertNotNull(key.getGoid());
    ApiKey foundkey = apiKeyManager.find(key.getName());

    assertEquals(key.getGoid(), foundkey.getGoid());

    apiKeyManager.delete(key.getName());
    session.flush();
    assertNull(apiKeyManager.find(key.getName()));
  }

  private ApiKey createApiKey(String name, HashMap<String, String> serviceIds) {
    ApiKey key = new ApiKey();
    key.setName(name);
    key.setSecret("somesecret");
    key.setServiceIds(serviceIds);
    key.setStatus("somestatus");
    key.setVersion(1);
    key.setLabel("somelabel");
    key.setOauthCallbackUrl("someOauthCallbackUrl");
    key.setOauthScope("someOauthScope");
    key.setOauthType("someOauthType");
    key.setPlatform("somePlatform");
    key.setAccountPlanMappingId("1");
    key.setLastUpdate(new Date());
    return key;
  }
}
