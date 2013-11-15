package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.gateway.api.DependencyResultsMO;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.service.ServiceManagerStub;
import org.junit.*;

import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.util.logging.Logger;

/**
 * This was created: 10/23/13 as 4:47 PM
 *
 * @author Victor Kazakov
 */
public class PublishedServiceRestServerGatewayManagementAssertionTest extends ServerRestGatewayManagementAssertionTestBase {
    private static final Logger logger = Logger.getLogger(PublishedServiceRestServerGatewayManagementAssertionTest.class.getName());

    private static final PublishedService publishedService = new PublishedService();
    private static ServiceManagerStub serviceManager;
    private static final String roleBasePath = "services/";

    @Before
    public void before() throws Exception {
        super.before();
        serviceManager = applicationContext.getBean("serviceManager", ServiceManagerStub.class);
        publishedService.setName("Service1");
        publishedService.setPolicy(new Policy(PolicyType.INCLUDE_FRAGMENT, "Service1 Policy", "", false));
        serviceManager.save(publishedService);
    }

    @After
    public void after() throws Exception {
        super.after();
        serviceManager.delete(publishedService);
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        ServerRestGatewayManagementAssertionTestBase.beforeClass();
    }

    @Test
    public void getServiceDependenciesTest() throws Exception {
        Response response = processRequest(roleBasePath + publishedService.getId() + "/dependencies", HttpMethod.GET, null, "");
        logger.info(response.toString());
        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());

        final StreamSource source = new StreamSource( new StringReader(response.getBody()) );
        DependencyResultsMO dependencyResultsMO = MarshallingUtils.unmarshal(DependencyResultsMO.class, source);

        dependencyResultsMO.toString();
    }
}
