package com.l7tech.skunkworks.rest.resourcetests;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.skunkworks.rest.tools.RestEntityTestBase;
import com.l7tech.skunkworks.rest.tools.RestResponse;
import com.l7tech.test.BugId;
import com.l7tech.test.conditional.ConditionalIgnore;
import com.l7tech.test.conditional.IgnoreOnDaily;
import junit.framework.Assert;
import org.junit.Test;

@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class InvalidRequestTests extends RestEntityTestBase {

    @BugId("SSG-8198")
    @Test
    public void testInvalidOffset() throws Exception {
        RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest("roles", "offset=asd", HttpMethod.GET, null, null);
        Assert.assertEquals("Incorrect Response:\n" + response.toString(), 400, response.getStatus());

        response = getDatabaseBasedRestManagementEnvironment().processRequest("roles", "offset=" + Integer.MAX_VALUE + "0", HttpMethod.GET, null, null);
        Assert.assertEquals("Incorrect Response:\n" + response.toString(), 400, response.getStatus());

        response = getDatabaseBasedRestManagementEnvironment().processRequest("roles", "offset=-1", HttpMethod.GET, null, null);
        Assert.assertEquals("Incorrect Response:\n" + response.toString(), 400, response.getStatus());

        response = getDatabaseBasedRestManagementEnvironment().processRequest("roles", "offset=" + Integer.MAX_VALUE, HttpMethod.GET, null, null);
        Assert.assertEquals("Incorrect Response:\n" + response.toString(), 400, response.getStatus());

        response = getDatabaseBasedRestManagementEnvironment().processRequest("roles", "offset", HttpMethod.GET, null, null);
        Assert.assertEquals("Incorrect Response:\n" + response.toString(), 400, response.getStatus());

        //test the valid cases
        response = getDatabaseBasedRestManagementEnvironment().processRequest("roles", "offset=0", HttpMethod.GET, null, null);
        Assert.assertEquals("Incorrect Response:\n" + response.toString(), 200, response.getStatus());

        response = getDatabaseBasedRestManagementEnvironment().processRequest("roles", "offset=5", HttpMethod.GET, null, null);
        Assert.assertEquals("Incorrect Response:\n" + response.toString(), 200, response.getStatus());
    }

    @BugId("SSG-8196")
    @Test
    public void testInvalidCount() throws Exception {
        RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest("roles", "count=asd", HttpMethod.GET, null, null);
        Assert.assertEquals("Incorrect Response:\n" + response.toString(), 400, response.getStatus());

        response = getDatabaseBasedRestManagementEnvironment().processRequest("roles", "count=501", HttpMethod.GET, null, null);
        Assert.assertEquals("Incorrect Response:\n" + response.toString(), 400, response.getStatus());

        response = getDatabaseBasedRestManagementEnvironment().processRequest("roles", "count=0", HttpMethod.GET, null, null);
        Assert.assertEquals("Incorrect Response:\n" + response.toString(), 400, response.getStatus());

        response = getDatabaseBasedRestManagementEnvironment().processRequest("roles", "count=-1", HttpMethod.GET, null, null);
        Assert.assertEquals("Incorrect Response:\n" + response.toString(), 400, response.getStatus());

        response = getDatabaseBasedRestManagementEnvironment().processRequest("roles", "count=", HttpMethod.GET, null, null);
        Assert.assertEquals("Incorrect Response:\n" + response.toString(), 400, response.getStatus());

        response = getDatabaseBasedRestManagementEnvironment().processRequest("roles", "count", HttpMethod.GET, null, null);
        Assert.assertEquals("Incorrect Response:\n" + response.toString(), 400, response.getStatus());

        //test the valid cases
        response = getDatabaseBasedRestManagementEnvironment().processRequest("roles", "count=500", HttpMethod.GET, null, null);
        Assert.assertEquals("Incorrect Response:\n" + response.toString(), 200, response.getStatus());

        response = getDatabaseBasedRestManagementEnvironment().processRequest("roles", "count=1", HttpMethod.GET, null, null);
        Assert.assertEquals("Incorrect Response:\n" + response.toString(), 200, response.getStatus());

        response = getDatabaseBasedRestManagementEnvironment().processRequest("roles", "count=234", HttpMethod.GET, null, null);
        Assert.assertEquals("Incorrect Response:\n" + response.toString(), 200, response.getStatus());
    }

    @Test
    public void testInvalidOrder() throws Exception {
        RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest("roles", "order=asd", HttpMethod.GET, null, null);
        Assert.assertEquals("Incorrect Response:\n" + response.toString(), 400, response.getStatus());

        response = getDatabaseBasedRestManagementEnvironment().processRequest("roles", "order=123", HttpMethod.GET, null, null);
        Assert.assertEquals("Incorrect Response:\n" + response.toString(), 400, response.getStatus());

        response = getDatabaseBasedRestManagementEnvironment().processRequest("roles", "order=", HttpMethod.GET, null, null);
        Assert.assertEquals("Incorrect Response:\n" + response.toString(), 400, response.getStatus());

        response = getDatabaseBasedRestManagementEnvironment().processRequest("roles", "order", HttpMethod.GET, null, null);
        Assert.assertEquals("Incorrect Response:\n" + response.toString(), 400, response.getStatus());

        //test the valid cases
        response = getDatabaseBasedRestManagementEnvironment().processRequest("roles", "order=asc", HttpMethod.GET, null, null);
        Assert.assertEquals("Incorrect Response:\n" + response.toString(), 200, response.getStatus());

        response = getDatabaseBasedRestManagementEnvironment().processRequest("roles", "order=desc", HttpMethod.GET, null, null);
        Assert.assertEquals("Incorrect Response:\n" + response.toString(), 200, response.getStatus());
    }

    @BugId("SSG-8195")
    @Test
    public void testInvalidSort() throws Exception {
        RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest("roles", "sort=asd", HttpMethod.GET, null, null);
        Assert.assertEquals("Incorrect Response:\n" + response.toString(), 400, response.getStatus());

        response = getDatabaseBasedRestManagementEnvironment().processRequest("roles", "sort=123", HttpMethod.GET, null, null);
        Assert.assertEquals("Incorrect Response:\n" + response.toString(), 400, response.getStatus());

        response = getDatabaseBasedRestManagementEnvironment().processRequest("roles", "sort=", HttpMethod.GET, null, null);
        Assert.assertEquals("Incorrect Response:\n" + response.toString(), 400, response.getStatus());

        response = getDatabaseBasedRestManagementEnvironment().processRequest("roles", "sort", HttpMethod.GET, null, null);
        Assert.assertEquals("Incorrect Response:\n" + response.toString(), 400, response.getStatus());

        response = getDatabaseBasedRestManagementEnvironment().processRequest("roles", "sort=description", HttpMethod.GET, null, null);
        Assert.assertEquals("Incorrect Response:\n" + response.toString(), 400, response.getStatus());

        //test the valid cases
        response = getDatabaseBasedRestManagementEnvironment().processRequest("roles", "sort=id", HttpMethod.GET, null, null);
        Assert.assertEquals("Incorrect Response:\n" + response.toString(), 200, response.getStatus());

        response = getDatabaseBasedRestManagementEnvironment().processRequest("roles", "sort=name", HttpMethod.GET, null, null);
        Assert.assertEquals("Incorrect Response:\n" + response.toString(), 200, response.getStatus());
    }

    @BugId("SSG-8199")
    @Test
    public void testInvalidFilter() throws Exception {
        RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest("roles", "test=asd", HttpMethod.GET, null, null);
        Assert.assertEquals("Incorrect Response:\n" + response.toString(), 400, response.getStatus());

        response = getDatabaseBasedRestManagementEnvironment().processRequest("roles", "test=123", HttpMethod.GET, null, null);
        Assert.assertEquals("Incorrect Response:\n" + response.toString(), 400, response.getStatus());

        response = getDatabaseBasedRestManagementEnvironment().processRequest("roles", "test=", HttpMethod.GET, null, null);
        Assert.assertEquals("Incorrect Response:\n" + response.toString(), 400, response.getStatus());

        response = getDatabaseBasedRestManagementEnvironment().processRequest("roles", "test", HttpMethod.GET, null, null);
        Assert.assertEquals("Incorrect Response:\n" + response.toString(), 400, response.getStatus());

        response = getDatabaseBasedRestManagementEnvironment().processRequest("roles", "id=123", HttpMethod.GET, null, null);
        Assert.assertEquals("Incorrect Response:\n" + response.toString(), 400, response.getStatus());

        response = getDatabaseBasedRestManagementEnvironment().processRequest("roles", "userCreated=nottrue", HttpMethod.GET, null, null);
        Assert.assertEquals("Incorrect Response:\n" + response.toString(), 400, response.getStatus());

        response = getDatabaseBasedRestManagementEnvironment().processRequest("roles", "usercreated=nottrue", HttpMethod.GET, null, null);
        Assert.assertEquals("Incorrect Response:\n" + response.toString(), 400, response.getStatus());

        //test the valid cases
        response = getDatabaseBasedRestManagementEnvironment().processRequest("roles", "name=test", HttpMethod.GET, null, null);
        Assert.assertEquals("Incorrect Response:\n" + response.toString(), 200, response.getStatus());

        response = getDatabaseBasedRestManagementEnvironment().processRequest("roles", "userCreated=true", HttpMethod.GET, null, null);
        Assert.assertEquals("Incorrect Response:\n" + response.toString(), 200, response.getStatus());
    }
}
