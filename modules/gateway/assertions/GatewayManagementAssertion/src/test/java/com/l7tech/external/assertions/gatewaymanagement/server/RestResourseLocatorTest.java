package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.external.assertions.gatewaymanagement.server.rest.URLAccessibleLocator;
import com.l7tech.objectmodel.EntityType;
import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class RestResourseLocatorTest extends ServerRestGatewayManagementAssertionTestBase {
    private URLAccessibleLocator URLAccessibleLocator;

    @Before
    public void before() throws Exception {
        super.before();

        URLAccessibleLocator = restManagementAssertion.assertionContext.getBean("urlAccessibleLocator", URLAccessibleLocator.class);
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
        Assert.assertNotNull("The URLAccessibleLocator cannot be loaded", URLAccessibleLocator);

        Assert.assertNull("There should never be a restEntity resource for the ANY entity type", URLAccessibleLocator.findByEntityType(EntityType.ANY.toString()));
        Assert.assertNotNull("Cannot find the rest entity resource for SERVICE", URLAccessibleLocator.findByEntityType(EntityType.SERVICE.toString()));
        Assert.assertNotNull("Cannot find the rest entity resource for Folder", URLAccessibleLocator.findByEntityType(EntityType.FOLDER.toString()));
        Assert.assertNotNull("Cannot find the rest entity resource for Policy", URLAccessibleLocator.findByEntityType(EntityType.POLICY.toString()));
        Assert.assertNotNull("Cannot find the rest entity resource for Trusted Cert", URLAccessibleLocator.findByEntityType(EntityType.TRUSTED_CERT.toString()));

    }
}
