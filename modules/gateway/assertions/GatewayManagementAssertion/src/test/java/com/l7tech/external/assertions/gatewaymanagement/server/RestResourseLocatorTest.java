package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.external.assertions.gatewaymanagement.server.rest.RestResourceLocator;
import com.l7tech.objectmodel.EntityType;
import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class RestResourseLocatorTest extends ServerRestGatewayManagementAssertionTestBase {
    private RestResourceLocator restResourceLocator;

    @Before
    public void before() throws Exception {
        super.before();

        restResourceLocator = restManagementAssertion.assertionContext.getBean("restResourceLocator", RestResourceLocator.class);
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
    public void test() {
        Assert.assertNotNull("The restResourceLocator cannot be loaded", restResourceLocator);

        Assert.assertNull("There should never be a restEntity resource for the ANY entity type", restResourceLocator.findByEntityType(EntityType.ANY));
        Assert.assertNotNull("Cannot find the rest entity resource for SERVICE", restResourceLocator.findByEntityType(EntityType.SERVICE));
        Assert.assertNotNull("Cannot find the rest entity resource for Folder", restResourceLocator.findByEntityType(EntityType.FOLDER));
        Assert.assertNotNull("Cannot find the rest entity resource for Policy", restResourceLocator.findByEntityType(EntityType.POLICY));
        Assert.assertNotNull("Cannot find the rest entity resource for Trusted Cert", restResourceLocator.findByEntityType(EntityType.TRUSTED_CERT));

    }
}
