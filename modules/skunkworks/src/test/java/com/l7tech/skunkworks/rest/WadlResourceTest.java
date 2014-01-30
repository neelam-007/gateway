package com.l7tech.skunkworks.rest;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.skunkworks.rest.tools.RestEntityTestBase;
import com.l7tech.skunkworks.rest.tools.RestResponse;
import com.l7tech.test.conditional.ConditionalIgnore;
import com.l7tech.test.conditional.IgnoreOnDaily;
import org.junit.Test;

import java.util.logging.Level;
import java.util.logging.Logger;

@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class WadlResourceTest extends RestEntityTestBase {
    private static final Logger logger = Logger.getLogger(WadlResourceTest.class.getName());

    @Test
    public void getDefaultWadlTest() throws Exception {
        RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest("application.wadl", HttpMethod.GET, null, "");
        logger.info(response.toString());
        org.junit.Assert.assertEquals(404, response.getStatus());
    }

    @Test
    public void getWadlTest() throws Exception {
        RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest("/rest.wadl", HttpMethod.GET, null, "");
        logger.log(Level.FINE, response.toString());
        org.junit.Assert.assertEquals(200, response.getStatus());
    }
}
