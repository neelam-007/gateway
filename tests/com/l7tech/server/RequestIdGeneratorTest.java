package com.l7tech.server;

import com.l7tech.common.util.HexUtils;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.math.BigInteger;
import java.util.Set;
import java.util.HashSet;

/**
 * @author alex
 * @version $Revision$
 */
public class RequestIdGeneratorTest extends TestCase {
    /**
     * test <code>RequestIdGeneratorTest</code> constructor
     */
    public RequestIdGeneratorTest(String name) {
        super(name);
    }

    /**
     * create the <code>TestSuite</code> for the
     * RequestIdGeneratorTest <code>TestCase</code>
     */
    public static Test suite() {
        TestSuite suite = new TestSuite(RequestIdGeneratorTest.class);
        return suite;
    }

    class CounterThread extends Thread {
        public void run() {
            BigInteger id = null, lastId = null;
            for ( int i = 0; i < 100000; i++ ) {
                id = RequestIdGenerator.next();
                _ids.add( id );
                if ( lastId == null ) continue;

                assertFalse( "consecutive IDs were equal!", id.compareTo( lastId ) == 0 );
                assertFalse( "second ID smaller than previous!", id.compareTo( lastId ) < 0 );
                assertTrue( "second ID larger than previous", id.compareTo( lastId ) > 0 );

                lastId = id;
            }
        }

        Set _ids = new HashSet();
    }

    public void testSimple() {
        BigInteger bigTime = new BigInteger( new Long( ServerConfig.getInstance().getServerBootTime() ).toString() );
        System.err.println( "bigTime = " + bigTime + " (" + bytesToHex( bigTime.toByteArray() ) + ")" );

        BigInteger id1 = RequestIdGenerator.next();
        System.err.println( "id1 = " + id1 + " (" + bytesToHex( id1.toByteArray() ) + ")" );
        BigInteger id2 = RequestIdGenerator.next();
        System.err.println( "id2 = " + id2 + " (" + bytesToHex( id2.toByteArray() ) + ")" );
        assertNotSame( "Generated IDs the same", id1, id2 );
    }

    public void testMonotonicity() throws Exception {
        CounterThread ct1 = new CounterThread();
        CounterThread ct2 = new CounterThread();
        System.err.println( "Starting thread #1" );
        ct1.start();
        System.err.println( "Starting thread #2" );
        ct2.start();
        System.err.println( "Waiting for thread #1" );
        ct1.join();
        System.err.println( "Waiting for thread #2" );
        ct2.join();
        Set set1 = ct1._ids;
        Set set2 = ct2._ids;

        set1.retainAll(set2);
        assertTrue( "Overlapping ID sets!", set1.isEmpty() );
    }

    public static String bytesToHex(byte[] binaryData) {
        if (binaryData == null) return "";

        char[] hexadecimal ={'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

        StringBuffer buffer = new StringBuffer();

        for (int i = 0; i < binaryData.length; i++) {
            int low = (binaryData[i] & 0x0f);
            int high = ((binaryData[i] & 0xf0) >> 4);
            buffer.append( hexadecimal[high] );
            buffer.append( hexadecimal[low] );
        }
        return buffer.toString();
    }

    /**
     * Test <code>RequestIdGeneratorTest</code> main.
     */
    public static void main(String[] args) throws
            Throwable {
        junit.textui.TestRunner.run(suite());
    }

}