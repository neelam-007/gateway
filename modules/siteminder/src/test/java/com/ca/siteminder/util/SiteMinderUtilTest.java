package com.ca.siteminder.util;

import com.l7tech.gateway.common.siteminder.SiteMinderHost;
import org.junit.Ignore;
import org.junit.Test;

import static junit.framework.Assert.assertNotNull;

@Ignore("Requires connection to the SiteMinder Policy Server")
public class SiteMinderUtilTest {

    @Test
    public void testRegHost() throws Exception {

        SiteMinderHost host = SiteMinderUtil.regHost("10.7.34.32", "siteminder", "7layer", "awitrisna", "Layer7HostSettings", 1);
        assertNotNull(host.getHostname());
        assertNotNull(host.getHostConfigObject());
        assertNotNull(host.getPolicyServer());
        assertNotNull(host.getRequestTimeout());
        assertNotNull(host.getSharedSecret());
        assertNotNull(host.getSharedSecretTime());
        assertNotNull(host.getFipsMode());

    }
}
