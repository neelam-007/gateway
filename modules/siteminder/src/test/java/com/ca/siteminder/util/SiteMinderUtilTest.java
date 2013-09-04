package com.ca.siteminder.util;

import com.l7tech.gateway.common.siteminder.SiteMinderFipsModeOption;
import com.l7tech.gateway.common.siteminder.SiteMinderHost;
import netegrity.siteminder.javaagent.Attribute;
import org.junit.Ignore;
import org.junit.Test;


import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

@Ignore("Requires connection to the SiteMinder Policy Server")
public class SiteMinderUtilTest {

    @Test
    public void testRegHost() throws Exception {

        SiteMinderHost host = SiteMinderUtil.regHost("10.7.34.32", "siteminder", "7layer", "awitrisna", "Layer7HostSettings", SiteMinderFipsModeOption.COMPAT);
        assertNotNull(host.getHostname());
        assertNotNull(host.getHostConfigObject());
        assertNotNull(host.getPolicyServer());
        assertNotNull(host.getRequestTimeout());
        assertNotNull(host.getSharedSecret());
        assertNotNull(host.getSharedSecretTime());
        assertNotNull(host.getFipsMode());
        System.out.println(host);

    }

    @Test
    public void testConvertAttributeValueToInt() throws Exception {
        byte[] attrVal = {49,51,55,54,51,50,54,51,56,53,0};
        int id = 154;

        Attribute attr = new Attribute(id, 0,0,"", attrVal);

        assertEquals(1376326385,SiteMinderUtil.convertAttributeValueToInt(attr));
    }
}
