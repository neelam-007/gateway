package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestManVersion;
import org.junit.*;

import java.util.logging.Logger;

public class WADLRestServerGatewayManagementAssertionTest extends ServerRestGatewayManagementAssertionTestBase {
    private static final Logger logger = Logger.getLogger(WADLRestServerGatewayManagementAssertionTest.class.getName());

    @Before
    public void before() throws Exception {
        super.before();
    }

    @After
    public void after() throws Exception {
        super.after();
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        ServerRestGatewayManagementAssertionTestBase.beforeClass();
    }

    @Test
    public void getDefaultWadlTest() throws Exception {
        RestResponse response = processRequest("application.wadl", HttpMethod.GET, null, "");
        logger.info(response.toString());
        Assert.assertEquals(404, response.getStatus());
    }

    /**
     * Note that this test will not run from idea. It should run on the build machine.
     */
    @Test
    public void getRestWadlTest() throws Exception {
        for (RestManVersion restManVersion : RestManVersion.values()){
            RestResponse response = processRequest("rest.wadl?version="+restManVersion.getStringRepresentation(), HttpMethod.GET, null, "");
            logger.info(response.toString());
            Assert.assertEquals(200, response.getStatus());
        }
    }

    @Test
    public void getBadVersionTest() throws Exception {
        RestResponse response = processRequest("rest.wadl?version=", HttpMethod.GET, null, "");
        logger.info(response.toString());
        Assert.assertEquals(404, response.getStatus());
    }
}
