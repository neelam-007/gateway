package com.l7tech.server.ems.migration;

import junit.framework.TestCase;
import junit.framework.Test;
import junit.framework.TestSuite;
import com.l7tech.server.ems.gateway.GatewayContext;
import com.l7tech.server.management.api.node.MigrationApi;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.objectmodel.EntityHeaderSet;
import com.l7tech.objectmodel.EntityHeader;

/**
 * @author jbufu
 */
public class MigrationApiTest extends TestCase {

    public MigrationApiTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(MigrationApiTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testApi() throws Exception {

        System.setProperty("org.apache.cxf.nofastinfoset", "true");

        GatewayContext gwContext = new GatewayContext(null, "darmok" , 8443 ,"esmId", "userId");
        MigrationApi api = gwContext.getMigrationApi();

        EntityHeaderSet<EntityHeader> headers = api.listEntities(PublishedService.class);

        System.out.println(headers);
    }
}
