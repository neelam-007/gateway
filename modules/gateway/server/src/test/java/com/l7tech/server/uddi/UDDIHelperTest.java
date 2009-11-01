/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
package com.l7tech.server.uddi;

import org.junit.Test;
import org.junit.Assert;
import org.junit.Before;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.ServerConfig;

import java.net.UnknownHostException;
import java.util.Properties;

public class UDDIHelperTest {
    private UDDIHelper uddiHelper;

    @Before
    public void setUp(){
        uddiHelper = new UDDIHelper(ServerConfig.getInstance(), new Properties());
    }

    @Test
    public void testURLsClusterPropNotSet() throws FindException, UnknownHostException {
        final String hostName = ServerConfig.getInstance().getHostname();
        testUrls(hostName);
    }

//    @Test
//    public void testGetHostName() throws Exception {
//        Assert.assertEquals("", InetAddress.getLocalHost().getHostName(), urlManager.getHostNameOnly());
//    }

    private void testUrls(final String hostName) throws FindException {

        final long serviceOid = 87293952;
        final String expected = "http://" + hostName + ":8080/service/" + serviceOid;

        final String externalUrl = uddiHelper.getExternalUrlForService(serviceOid);
        System.out.println(externalUrl);
        Assert.assertEquals("Incorrect External URL found", expected, externalUrl);

        final String wsdlUrl = uddiHelper.getExternalWsdlUrlForService(serviceOid);
        System.out.println(wsdlUrl);
        final String expectedWsdlUrl = "http://" + hostName + ":8080/ssg/wsdl?serviceoid=87293952";
        Assert.assertEquals("Incorrect WSDL URL found", expectedWsdlUrl, wsdlUrl);

        final String wsPolicyOnlyUrl = uddiHelper.getExternalPolicyUrlForService(serviceOid, false);
        System.out.println(wsPolicyOnlyUrl);
        final String expectedWsPolicy = "http://" + hostName + ":8080/ssg/policy/disco?serviceoid=87293952&fulldoc=no&inline=no";
        Assert.assertEquals("Incorrect External URL found", expectedWsPolicy, uddiHelper.getExternalPolicyUrlForService(serviceOid, false));

        final String layer7PolicyFull = uddiHelper.getExternalPolicyUrlForService(serviceOid, true);
        System.out.println(layer7PolicyFull);
        final String expectedLayer7Policy = "http://" + hostName + ":8080/ssg/policy/disco?serviceoid=87293952&fulldoc=yes&inline=no";
        Assert.assertEquals("Incorrect External URL found", expectedLayer7Policy, layer7PolicyFull);
    }
}
