package com.l7tech.server.cluster;


import java.net.InetAddress;

import org.junit.Test;
import org.junit.Assert;

/**
 * @author alex
 * @version $Revision$
 */
public class TestMulticastAddress {

    @Test
    public void testAddressGeneration() throws Exception {
        boolean[] found = new boolean[256];
        for ( int i = 0; i < 10000; i++ ) {
            String addr = ClusterBootProcess.generateMulticastAddress();
            InetAddress ia = InetAddress.getByName(addr);
            found[(ia.getAddress()[3] & 0xff)] = true;
        }

        Assert.assertTrue(found[0]);
        Assert.assertTrue(found[255]);
    }

}