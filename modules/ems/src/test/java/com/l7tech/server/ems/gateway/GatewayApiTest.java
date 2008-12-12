package com.l7tech.server.ems.gateway;

import org.junit.Test;
import org.junit.Ignore;
import com.l7tech.server.management.api.node.GatewayApi;
import com.l7tech.test.BugNumber;

/**
 *
 */
public class GatewayApiTest {


    /**
     * Test to reproduce bugs 6008, 6159.
     *
     * To run this test you'll want to edit cxfSupportContext and remove the security
     * interceptor for the gateway api. You can also chagne the protocol in GatewayContext to
     * http and remove the TLS configuration if you want to observe messages on the wire.
     *
     * To test the Server side issues it is best to use GClient with added logging for output
     * messages (though once the CXF client side is fixed this test can be used to test the server).
     */
    @BugNumber(6300)
    @Test
    @Ignore("Developer test")
    public void testConcurrentInvocation() throws Exception {
        System.setProperty("org.apache.cxf.nofastinfoset", "true");

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    GatewayContext context = new GatewayContext( null, "127.0.0.1", 8080, "ESM1", null);
                    GatewayApi api = context.getApi();
                    
                    for ( GatewayApi.GatewayInfo info : api.getGatewayInfo() ) {
                        System.out.println( info.getName() + " " + info.getIpAddress() + " " + info.getSoftwareVersion() );
                    }
                } catch ( Exception e ) {
                    throw new RuntimeException(e);
                }
            }
        };

        Thread thread1 = new Thread(runnable);
        Thread thread2 = new Thread(runnable);

        thread1.start();
        thread2.start();

        thread1.join();
        thread2.join();

        Thread.sleep( 1000 );
    }


}
