package com.l7tech.adminservice;

import junit.framework.*;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: May 7, 2003
 *
 * Unit Test for the ListResultEntry class
 */
public class TestListResultEntry extends TestCase {

    public void testListResultEntry() throws Exception {
        ListResultEntry tst = new ListResultEntry();
        assertTrue("ListResultEntry() constructor works", tst != null);
        tst = new ListResultEntry(2, "blah");
        assertTrue("ListResultEntry(long, string) constructor works", tst != null);
        tst = new ListResultEntry(-2, null);
        assertTrue("ListResultEntry(long, string) constructor works with bad arguments", tst != null);
    }

    public void testGetUid() throws Exception {
        ListResultEntry tst = new ListResultEntry(45, "blah");
        assertTrue("getUid() returns correct value", tst.getUid() == 45);
        tst = new ListResultEntry(-654, "blah");
        assertTrue("getUid() returns correct value", tst.getUid() == -654);
    }

    public void testSetUid() throws Exception {
        ListResultEntry tst = new ListResultEntry();
        tst.setUid(45);
        assertTrue("setUid() sets the value", tst.getUid() == 45);
        tst.setUid(-654);
        assertTrue("setUid() sets negative value", tst.getUid() == -654);
    }

    public void testGetName() throws Exception {
        ListResultEntry tst = new ListResultEntry(45, "blah");
        assertTrue("getName() returns correct value", tst.getName().equals("blah"));
        tst = new ListResultEntry(-22, null);
        assertTrue("getName() never returns null", tst.getName().equals(""));
    }

    public void testSetName() throws Exception {
        ListResultEntry tst = new ListResultEntry(45, "blah");
        tst = new ListResultEntry(-22, null);
    }
}
