/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server.identity;

import com.l7tech.identity.GroupBean;
import com.l7tech.identity.UserBean;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.logging.Logger;

/**
 * @author mike
 */
public class AuthenticationResultTest extends TestCase {
    private static Logger log = Logger.getLogger(AuthenticationResultTest.class.getName());

    public AuthenticationResultTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(AuthenticationResultTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testGroupMembershipCache() throws Exception {
        UserBean aliceUser = new UserBean("alice");
        aliceUser.setUniqueIdentifier("1234");

        UserBean bobUser = new UserBean("bob");
        bobUser.setUniqueIdentifier("2372");

        GroupBean aliceGroup = new GroupBean();
        aliceGroup.setName("alices");
        aliceGroup.setUniqueIdentifier("3456");

        GroupBean fooGroup = new GroupBean();
        fooGroup.setName("foos");
        fooGroup.setUniqueIdentifier("8737");

        AuthenticationResult aliceAuth = new AuthenticationResult(aliceUser, null, false);
        AuthenticationResult bobAuth = new AuthenticationResult(bobUser, null, false);

        Boolean b = aliceAuth.getCachedGroupMembership(aliceGroup);
        assertNull(b);

        aliceAuth.setCachedGroupMembership(aliceGroup, true);
        b = aliceAuth.getCachedGroupMembership(aliceGroup);
        assertNotNull(b);
        assertTrue(b.booleanValue());

        b = aliceAuth.getCachedGroupMembership(fooGroup);
        assertNull(b);

        aliceAuth.setCachedGroupMembership(fooGroup, false);
        b = aliceAuth.getCachedGroupMembership(fooGroup);
        assertNotNull(b);
        assertFalse(b.booleanValue());
        
        b = bobAuth.getCachedGroupMembership(aliceGroup);
        assertNull(b);

        bobAuth.setCachedGroupMembership(aliceGroup, false);
        b = bobAuth.getCachedGroupMembership(aliceGroup);
        assertNotNull(b);
        assertFalse(b.booleanValue());

        bobAuth.setCachedGroupMembership(fooGroup, true);
        b = bobAuth.getCachedGroupMembership(fooGroup);
        assertNotNull(b);
        assertTrue(b.booleanValue());

        b = aliceAuth.getCachedGroupMembership(fooGroup);
        assertNotNull(b);
        assertFalse(b.booleanValue());
    }
}
