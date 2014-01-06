package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.objectmodel.Goid;
import org.junit.*;

import java.util.logging.Logger;

/**
 * This was created: 11/21/13 as 1:52 PM
 *
 * @author Victor Kazakov
 */
public class XslStyleSheetResourceTest extends ServerRestGatewayManagementAssertionTestBase {
    private static final Logger logger = Logger.getLogger(XslStyleSheetResourceTest.class.getName());


    @BeforeClass
    public static void beforeClass() throws Exception {
        ServerRestGatewayManagementAssertionTestBase.beforeClass();
    }

    @Before
    public void before() throws Exception {
        super.before();
    }

    @After
    public void after() throws Exception {
        super.after();
    }

    @Test
    public void testLoadDefaultStyleSheet() throws Exception {
        Response response = processRequest("/../stylesheets/defaultStyleSheet.xsl", HttpMethod.GET, null, "");
        logger.info(response.toString());

        Assert.assertEquals(200, response.getStatus());
    }
}
