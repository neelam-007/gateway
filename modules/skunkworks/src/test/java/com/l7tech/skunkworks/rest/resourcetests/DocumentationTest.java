package com.l7tech.skunkworks.rest.resourcetests;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.skunkworks.rest.tools.RestEntityTestBase;
import com.l7tech.skunkworks.rest.tools.RestResponse;
import com.l7tech.test.conditional.ConditionalIgnore;
import com.l7tech.test.conditional.IgnoreOnDaily;
import org.junit.Test;

import java.util.logging.Level;
import java.util.logging.Logger;

@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class DocumentationTest extends RestEntityTestBase {
    private static final Logger logger = Logger.getLogger(DocumentationTest.class.getName());

    /**
     * Note that this test will not run from idea. It should run on the build machine.
     */
    @Test
    public void getDocTest() throws Exception {
        RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest("/doc/", HttpMethod.GET, null, "");
        logger.log(Level.FINE, response.toString());
        org.junit.Assert.assertEquals(200, response.getStatus());
    }
}
