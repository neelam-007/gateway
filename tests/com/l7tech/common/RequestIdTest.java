package com.l7tech.common;

import junit.framework.TestCase;

/**
 * [class_desc]
 *
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jan 13, 2004<br/>
 * $Id$<br/>
 *
 */
public class RequestIdTest extends TestCase {
    public void testParsing() {
        try {
            RequestId redid1 = new RequestId(123, 456);
            RequestId redid2 = new RequestId(redid1.toString());
            assertTrue(redid1.equals(redid2));
            redid1 = new RequestId(0, 0);
            redid2 = new RequestId(redid1.toString());
            assertTrue(redid1.equals(redid2));
        } catch (Exception e) {
            e.printStackTrace(System.err);
            assertTrue(e.getMessage(), false);
        }
    }

    public void testExtremeParsing() {
        try {
            RequestId redid1 = new RequestId(0, Long.MAX_VALUE);
            RequestId redid2 = new RequestId(redid1.toString());
            assertTrue(redid1.equals(redid2));
            redid1 = new RequestId(Long.MAX_VALUE, 0);
            redid2 = new RequestId(redid1.toString());
            assertTrue(redid1.equals(redid2));
        } catch (Exception e) {
            e.printStackTrace(System.err);
            assertTrue(e.getMessage(), false);
        }
    }
}
