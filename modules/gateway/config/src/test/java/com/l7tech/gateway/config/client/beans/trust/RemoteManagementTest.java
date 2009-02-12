package com.l7tech.gateway.config.client.beans.trust;

import org.junit.Ignore;

import java.io.File;

/**
 *
 */
@Ignore("Developer test for running remote management configuration.")
public class RemoteManagementTest {

    public static void main( final String[] args ) throws Exception {
        File tempDir = new File( System.getProperty("java.io.tmpdir") );
        File hostFile = File.createTempFile( "host.properties", null, tempDir );

        new TrustInterviewer().doTrustInterview( hostFile, new File("etc/omp.dat"), tempDir );
    }
}
