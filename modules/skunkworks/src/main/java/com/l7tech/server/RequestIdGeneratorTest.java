package com.l7tech.server;

import com.l7tech.gateway.common.RequestId;
import org.junit.*;
import static org.junit.Assert.*;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.Set;

/**
 * @author alex
 * @version $Revision$
 */
public class RequestIdGeneratorTest {

    RequestIdGenerator generator;

    @Before
    public void setUp() {
        generator = new RequestIdGenerator(System.currentTimeMillis() - 10, RequestIdGenerator.MAX_SEQ - 100000);
    }

    class CounterThread extends Thread {
        public void run() {
            RequestId id;
            RequestId lastId = null;
            for ( int i = 0; i < 100000; i++ ) {
                id = generator.doNext();
                _ids.add( id );

                if (lastId != null) {
                    assertFalse( "consecutive IDs were equal!", id.compareTo( lastId ) == 0 );
                    assertFalse( "second ID smaller than previous!", id.compareTo( lastId ) < 0 );
                    assertTrue( "second ID larger than previous", id.compareTo( lastId ) > 0 );
                }

                lastId = id;
            }
        }

        Set<RequestId> _ids = new HashSet<RequestId>();
    }

    @Test
    public void testSimple() {
        BigInteger bigTime = new BigInteger(Long.toString(ServerConfig.getInstance().getServerBootTime()));
        System.err.println( "bigTime = " + bigTime + " (" + bytesToHex( bigTime.toByteArray() ) + ")" );

        RequestId id1 = generator.doNext();
        System.err.println( "id1 = " + id1 + " (" + id1.toString() + ")" );
        RequestId id2 = generator.doNext();
        System.err.println( "id2 = " + id2 + " (" + id2.toString() + ")" );
        assertNotSame( "Generated IDs the same", id1, id2 );
    }

    @Test
    public void testMonotonicity() throws Exception {
        CounterThread ct1 = new CounterThread();
        CounterThread ct2 = new CounterThread();
        CounterThread ct3 = new CounterThread();

        System.err.println( "Starting thread #1" );
        ct1.start();
        System.err.println( "Starting thread #2" );
        ct2.start();
        System.err.println( "Starting thread #3" );
        ct3.start();

        System.err.println( "Waiting for thread #1" );
        ct1.join();
        System.err.println( "Waiting for thread #2" );
        ct2.join();
        System.err.println( "Waiting for thread #3" );
        ct3.join();

        Set set1 = ct1._ids;
        Set set2 = ct2._ids;
        Set set3 = ct3._ids;

        set2.retainAll( set3 );
        set1.retainAll( set2 );
        assertTrue( "Overlapping ID sets!", set1.isEmpty() );
    }

    public static String bytesToHex(byte[] binaryData) {
        if (binaryData == null) return "";

        char[] hexadecimal ={'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

        StringBuffer buffer = new StringBuffer();

        for (byte b : binaryData) {
            int low = (b & 0x0f);
            int high = ((b & 0xf0) >> 4);
            buffer.append(hexadecimal[high]);
            buffer.append(hexadecimal[low]);
        }
        return buffer.toString();
    }
}