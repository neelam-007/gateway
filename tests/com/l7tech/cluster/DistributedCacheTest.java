package com.l7tech.cluster;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.jgroups.*;

import java.util.Vector;

/**
 * @author alex
 * @version $Revision$
 */
public class DistributedCacheTest extends TestCase {
    /**
     * test <code>DistributedCacheTest</code> constructor
     */
    public DistributedCacheTest( String name ) {
        super( name );
    }

    /**
     * create the <code>TestSuite</code> for the DistributedCacheTest <code>TestCase</code>
     */
    public static Test suite() {
        TestSuite suite = new TestSuite( DistributedCacheTest.class );
        return suite;
    }

    public void testSend() throws Exception {
        Channel channel = getChannel();
        System.out.print("Sending");
        for ( int i = 0; i < 1000; i++ ) {
            channel.send(null, null, "Hi there #" + i);
            System.out.print(".");
        }
        Thread.sleep(10000); // Necessary to prevent the JVM from taking down the NAKACK thread(s)
        System.out.println();
    }

    private Channel getChannel() throws ChannelException {
        channel = new JChannel(JCHANNEL_PROPS);
        channel.setOpt(Channel.LOCAL, Boolean.FALSE);
        channel.setOpt(Channel.AUTO_RECONNECT, Boolean.TRUE);
        channel.setChannelListener(new ChannelListener() {
            public void channelConnected( Channel channel ) {
                System.out.println("Connected to " + channel);
            }

            public void channelDisconnected( Channel channel ) {
                System.out.println("Disconnected from " + channel);
            }

            public void channelClosed( Channel channel ) {
                System.out.println("Closed " + channel);
            }

            public void channelShunned() {
                System.out.println("Shunned from " + channel);
            }

            public void channelReconnected( Address address ) {
                System.out.println("Reconnected to " + channel);
            }
        });
        channel.connect("testchannel");
        final Vector members = channel.getView().getMembers();
        System.out.println("Members = " + members);
        return channel;
    }

    public void testReceive() throws ChannelException {
        Channel channel = getChannel();
        while(true) {
            Object received = null;
            try {
                received = channel.receive(1000);
                System.out.println("Received " + received);
            } catch ( TimeoutException e ) {
                System.out.print(".");
            }

            if ( received instanceof Message ) {
                Message m = (Message)received;
                System.out.println("Recieved message " + m.getObject() + " from " + m.getSrc() );
            } else if ( received instanceof View ) {
                View v = (View)received;
                System.out.println("Received view " + v.getMembers() + " from " + v.getCreator() );
            } else if ( received instanceof BlockEvent ) {
                System.out.println("Received BlockEvent");
            } else if ( received instanceof SuspectEvent ) {
                System.out.println("Received SuspectEvent");
            } else if ( received instanceof GetStateEvent ) {
                System.out.println("Received GetStateEvent");
            } else if ( received instanceof SetStateEvent ) {
                System.out.println("Received SetStateEvent");
            } else if ( received instanceof ExitEvent ) {
                System.out.println("Received ExitEvent. Exiting.");
                break;
            }
        }
    }

    public void tearDown() throws Exception {
        channel.close();
    }

    /**
     * Test <code>DistributedCacheTest</code> main.
     */
    public static void main( String[] args ) throws
                                             Throwable {
        DistributedCacheTest me = new DistributedCacheTest("DCT");
        String which = args[0];
        if ( which.equals("send") ) {
            me.testSend();
        } else {
            me.testReceive();
        }
    }

    public static final String JCHANNEL_PROPS =
            "UDP(mcast_addr=224.10.10.10;mcast_port=5555;ip_ttl=32):" +
            "PING(timeout=500;num_initial_members=2):" +
            "FD(timeout=500):" +
            "VERIFY_SUSPECT(timeout=500):" +
            "pbcast.NAKACK(gc_lag=10;retransmit_timeout=1000):" +
            "FC:" +
            "UNICAST(timeout=600,1200,2400,4800):" +
            "pbcast.STABLE(desired_avg_gossip=10000):" +
            "FRAG(frag_size=8096;down_thread=false;up_thread=false):" +
            "pbcast.GMS(join_timeout=500;join_retry_timeout=250;" +
            "shun=false;print_local_addr=true)";

    public static final String props =
            "UDP(mcast_addr=228.8.8.8;mcast_port=45566;ip_ttl=32;" +
            "mcast_send_buf_size=64000;mcast_recv_buf_size=64000):" +
                    //"PIGGYBACK(max_wait_time=100;max_size=32000):" +
            "PING(timeout=2000;num_initial_members=3):" +
            "MERGE2(min_interval=5000;max_interval=10000):" +
            "FD_SOCK:" +
            "VERIFY_SUSPECT(timeout=1500):" +
            "pbcast.NAKACK(max_xmit_size=8096;gc_lag=50;retransmit_timeout=600,1200,2400,4800):" +
            "UNICAST(timeout=600,1200,2400,4800):" +
            "pbcast.STABLE(desired_avg_gossip=20000):" +
            "FRAG(frag_size=8096;down_thread=false;up_thread=false):" +
                    // "CAUSAL:" +
            "pbcast.GMS(join_timeout=5000;join_retry_timeout=2000;" +
            "shun=false;print_local_addr=true)";


    private Channel channel;
}