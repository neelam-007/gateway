package com.ca.siteminder.util;


import com.l7tech.gateway.common.siteminder.SiteMinderFipsModeOption;
import com.l7tech.gateway.common.siteminder.SiteMinderHost;
import com.l7tech.test.BugId;
import com.l7tech.util.FileUtils;
import netegrity.siteminder.javaagent.Attribute;
import org.junit.Ignore;
import org.junit.Test;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

public class SiteMinderUtilTest {

    public static final String HOST_NAME = "юра-тест";

    private static final String SMHOST_FILE = "#NOTE: PKCS11 crypto provider is deprecated. Please use ETPKI instead. (/tmp/SMHOST3917880917627939729.tmp/smHost.conf)\n" +
            "#This file contains bootstrap information required by the SiteMinder Agent API to connect to Policy Servers at startup.\n" +
            "#Be sure the IP addresses and ports below identify valid listening Policy Servers.\n" +
            "#Please do not edit the encrypted SharedSecret entry.\n" +
            "hostname=\"юра-тест\"\n" +
            "sharedsecret=\"{RC2}V3Z2avTEt2FfxCg5foFfwSQt1b6ViV40myJ/aY/F7o9z0M0Px5CRBLQCjlofD4FyOyE2pU+UQ4j6cv9cXD041H9fGqNhRglbd/ow3zZFu+FMkZn++DY1p9W5mNBy/uhoNOk18+ZgJe7YmZAMcfSbD27TMTY7sJMwF+XDK4Od/Av3YxG8F8KJ8XSsRI8sE3Mc\"\n" +
            "sharedsecrettime=\"0\"\n" +
            "enabledynamichco=\"NO\"\n" +
            "hostconfigobject=\"Layer7HostSettings\"\n" +
            "#Add additional bootstrap policy servers here for fault tolerance.\n" +
            "policyserver=\"10.7.34.32,44441,44442,44443\"\n" +
            "requesttimeout=\"60\"\n" +
            "cryptoprovider=\"ETPKI\"\n" +
            "fipsmode=\"COMPAT\"\n" +
            "\n" +
            "# <EOF>\n";

    @Ignore("Requires connection to the SiteMinder Policy Server")
    @Test
    public void testRegHost() throws Exception {

        SiteMinderHost host = SiteMinderUtil.regHost("10.7.34.32", "siteminder", "7layer", HOST_NAME, "Layer7HostSettings", SiteMinderFipsModeOption.COMPAT);
        assertNotNull(host.getHostname());
        assertEquals(HOST_NAME, host.getHostname());
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

    @Test
    @BugId("SSG-7906")
    public void testSiteMinderHostObjectMultiByteHostName() throws Exception {
        File tmpDir = null;
        try{
            tmpDir = FileUtils.createTempDirectory("SMTEST", null, null, false);
            try(Writer writer = new BufferedWriter(new FileWriter(new File(tmpDir.getAbsolutePath() + File.separator + "smHostTest.conf")))) {
                writer.write(SMHOST_FILE);
            }
            SiteMinderHost smHost = new SiteMinderHost(tmpDir.getAbsolutePath() + File.separator + "smHostTest.conf");
            assertEquals(HOST_NAME, smHost.getHostname());
        } finally {
            if (tmpDir != null && tmpDir.exists()) {
                FileUtils.deleteDir(tmpDir);
            }
        }

    }
}
