package com.l7tech.cluster;

/*
 * Test stub for ClusterStatusAdmin interface
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

public class ClusterStatusAdminStub implements ClusterStatusAdmin{
    public ClusterInfo[] getClusterStatus() {

           ClusterInfo  c1 = new ClusterInfo();
           c1.setName("SSG1"); c1.setAddress("192.128.1.100"); c1.setAvgLoad(1.5); c1.setUptime(1072746384);

           ClusterInfo  c2 = new ClusterInfo();
           c2.setName("SSG2"); c2.setAddress("192.128.1.101"); c2.setAvgLoad(1.8); c2.setUptime(1072656394);

           ClusterInfo  c3 = new ClusterInfo();
           c3.setName("SSG3"); c3.setAddress("192.128.1.102"); c3.setAvgLoad(0); c3.setUptime(1072746404);

           ClusterInfo  c4 = new ClusterInfo();
           c4.setName("SSG4"); c4.setAddress("192.128.2.10"); c4.setAvgLoad(1.1); c4.setUptime(1072776414);

           ClusterInfo  c5 = new ClusterInfo();
           c5.setName("SSG5"); c5.setAddress("192.128.2.11"); c5.setAvgLoad(2.1); c5.setUptime(1072746484);

           ClusterInfo  c6 = new ClusterInfo();
           c6.setName("SSG6"); c6.setAddress("192.128.3.1"); c6.setAvgLoad(0.8); c6.setUptime(1072736464);

           ClusterInfo  c7 = new ClusterInfo();
           c7.setName("SSG7"); c7.setAddress("192.128.3.2"); c7.setAvgLoad(0); c7.setUptime(1072808010);

           ClusterInfo  c8 = new ClusterInfo();
           c8.setName("SSG8"); c8.setAddress("192.128.3.3"); c8.setAvgLoad(0); c8.setUptime(1072808325);

           ClusterInfo[] cluster = new ClusterInfo[8];

           cluster[0] = c1;
           cluster[1] = c2;
           cluster[2] = c3;
           cluster[3] = c4;
           cluster[4] = c5;
           cluster[5] = c6;
           cluster[6] = c7;
           cluster[7] = c8;
        return cluster;
    }

    public ServiceUsage[] getServiceUsage() {
        return new ServiceUsage[0];
    }
}
