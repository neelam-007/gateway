package com.l7tech.server.globalresources;

import com.l7tech.gateway.common.resources.HttpProxyConfiguration;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.server.MockClusterPropertyManager;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.test.BugId;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * This was created: 8/26/13 as 12:07 PM
 *
 * @author Victor Kazakov
 */
public class DefaultHttpProxyManagerTest {

    private DefaultHttpProxyManager defaultHttpProxyManager;

    private ClusterPropertyManager clusterPropertyManager = new MockClusterPropertyManager();

    @Before
    public void before(){
        defaultHttpProxyManager = new DefaultHttpProxyManager(clusterPropertyManager);
    }

    @BugId("SSG-7545")
    @Test
    public void testGetDefaultHttpProxyConfigurationOid() throws FindException, UpdateException, SaveException {
        final String host = "whatever.l7tech.com";
        final long oid = 4718592;
        final int port = 8080;
        final String user = "admin";
        clusterPropertyManager.putProperty("ioHttpProxy", "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<java version=\"1.7.0_21\" class=\"java.beans.XMLDecoder\">\n" +
                " <object class=\"com.l7tech.gateway.common.resources.HttpProxyConfiguration\">\n" +
                "  <void property=\"host\">\n" +
                "   <string>" + host + "</string>\n" +
                "  </void>\n" +
                "  <void property=\"passwordOid\">\n" +
                "   <long>" + oid + "</long>\n" +
                "  </void>\n" +
                "  <void property=\"port\">\n" +
                "   <int>" + port + "</int>\n" +
                "  </void>\n" +
                "  <void property=\"username\">\n" +
                "   <string>" + user + "</string>\n" +
                "  </void>\n" +
                " </object>\n" +
                "</java>");

        HttpProxyConfiguration defaultConfig = defaultHttpProxyManager.getDefaultHttpProxyConfiguration();

        Assert.assertEquals(host, defaultConfig.getHost());
        Assert.assertEquals(port, defaultConfig.getPort());
        Assert.assertEquals(user, defaultConfig.getUsername());
        Assert.assertEquals(oid, defaultConfig.getPasswordGoid().getLow());
    }
}
