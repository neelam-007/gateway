package com.l7tech.server.globalresources;

import com.l7tech.gateway.common.resources.HttpProxyConfiguration;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
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

    final String host = "whatever.l7tech.com";
    final long oid = 4718592;
    final Goid goid = new Goid(-5746916922401531998L, 156631003489687645L);
    final int port = 8080;
    final String user = "admin";

    private DefaultHttpProxyManager defaultHttpProxyManager;

    private ClusterPropertyManager clusterPropertyManager;

    @Before
    public void before() {
        clusterPropertyManager = new MockClusterPropertyManager();
        defaultHttpProxyManager = new DefaultHttpProxyManager(clusterPropertyManager);
    }

    @Test
    public void testSetThenGet() throws UpdateException, SaveException, FindException {
        HttpProxyConfiguration defaultConfig = new HttpProxyConfiguration();
        defaultConfig.setHost(host);
        defaultConfig.setPort(port);
        defaultConfig.setUsername(user);
        defaultConfig.setPasswordGoid(goid);

        defaultHttpProxyManager.setDefaultHttpProxyConfiguration(defaultConfig);

        String httpConfigXml = clusterPropertyManager.getProperty("ioHttpProxy");
        System.out.println(httpConfigXml);

        HttpProxyConfiguration unParsed = defaultHttpProxyManager.getDefaultHttpProxyConfiguration();

        Assert.assertEquals(host, unParsed.getHost());
        Assert.assertEquals(port, unParsed.getPort());
        Assert.assertEquals(user, unParsed.getUsername());
        Assert.assertEquals(goid, unParsed.getPasswordGoid());
    }

    @Test
    public void testGetDefaultHttpProxyConfigurationGoidString() throws FindException, UpdateException, SaveException {
        clusterPropertyManager.putProperty("ioHttpProxy", "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<java version=\"1.7.0_09\" class=\"java.beans.XMLDecoder\">\n" +
                " <object class=\"com.l7tech.gateway.common.resources.HttpProxyConfiguration\">\n" +
                "  <void property=\"host\">\n" +
                "   <string>" + host + "</string>\n" +
                "  </void>\n" +
                "  <void property=\"passwordGoid\">\n" +
                "   <object class=\"com.l7tech.objectmodel.Goid\">\n" +
                "    <string>" + goid.toString() + "</string>\n" +
                "   </object>\n" +
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
        Assert.assertEquals(goid, defaultConfig.getPasswordGoid());
    }

    @BugId("SSG-7545")
    @Test
    public void testGetDefaultHttpProxyConfigurationOid() throws FindException, UpdateException, SaveException {
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

    @Test
    public void testGetDefaultHttpProxyConfigurationGoidByte() throws FindException, UpdateException, SaveException {
        clusterPropertyManager.putProperty("ioHttpProxy", "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<java version=\"1.7.0_09\" class=\"java.beans.XMLDecoder\">\n" +
                " <object class=\"com.l7tech.gateway.common.resources.HttpProxyConfiguration\">\n" +
                "  <void property=\"host\">\n" +
                "   <string>" + host + "</string>\n" +
                "  </void>\n" +
                "  <void property=\"passwordGoid\">\n" +
                "   <object class=\"com.l7tech.objectmodel.Goid\">\n" +
                "    <array class=\"byte\" length=\"16\">\n" +
                "     <void index=\"0\">\n" +
                "      <byte>-80</byte>\n" +
                "     </void>\n" +
                "     <void index=\"1\">\n" +
                "      <byte>62</byte>\n" +
                "     </void>\n" +
                "     <void index=\"2\">\n" +
                "      <byte>-39</byte>\n" +
                "     </void>\n" +
                "     <void index=\"3\">\n" +
                "      <byte>-127</byte>\n" +
                "     </void>\n" +
                "     <void index=\"4\">\n" +
                "      <byte>-18</byte>\n" +
                "     </void>\n" +
                "     <void index=\"5\">\n" +
                "      <byte>-109</byte>\n" +
                "     </void>\n" +
                "     <void index=\"6\">\n" +
                "      <byte>87</byte>\n" +
                "     </void>\n" +
                "     <void index=\"7\">\n" +
                "      <byte>-94</byte>\n" +
                "     </void>\n" +
                "     <void index=\"8\">\n" +
                "      <byte>2</byte>\n" +
                "     </void>\n" +
                "     <void index=\"9\">\n" +
                "      <byte>44</byte>\n" +
                "     </void>\n" +
                "     <void index=\"10\">\n" +
                "      <byte>119</byte>\n" +
                "     </void>\n" +
                "     <void index=\"11\">\n" +
                "      <byte>17</byte>\n" +
                "     </void>\n" +
                "     <void index=\"12\">\n" +
                "      <byte>91</byte>\n" +
                "     </void>\n" +
                "     <void index=\"13\">\n" +
                "      <byte>-48</byte>\n" +
                "     </void>\n" +
                "     <void index=\"14\">\n" +
                "      <byte>-40</byte>\n" +
                "     </void>\n" +
                "     <void index=\"15\">\n" +
                "      <byte>93</byte>\n" +
                "     </void>\n" +
                "    </array>\n" +
                "   </object>\n" +
                "  </void>\n" +
                "  <void property=\"port\">\n" +
                "   <int>" + port + "</int>\n" +
                "  </void>\n" +
                "  <void property=\"username\">\n" +
                "   <string>" + user + "</string>\n" +
                "  </void>\n" +
                " </object>\n" +
                "</java>\n");

        HttpProxyConfiguration defaultConfig = defaultHttpProxyManager.getDefaultHttpProxyConfiguration();

        Assert.assertEquals(host, defaultConfig.getHost());
        Assert.assertEquals(port, defaultConfig.getPort());
        Assert.assertEquals(user, defaultConfig.getUsername());
        Assert.assertEquals(goid, defaultConfig.getPasswordGoid());
    }
}
