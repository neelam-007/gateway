/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server.identity;

import com.l7tech.identity.GroupBean;
import com.l7tech.identity.UserBean;
import com.l7tech.common.TestDocuments;
import com.l7tech.objectmodel.Goid;
import com.l7tech.security.token.OpaqueSecurityToken;
import org.junit.Test;
import org.junit.Assert;

/**
 * @author mike
 */
public class AuthenticationResultTest {

    @Test
    public void testEquality() throws Exception {
        {
            AuthenticationResult result1 = new AuthenticationResult( new UserBean(new Goid(0,123), "Alice"), new OpaqueSecurityToken() );
            AuthenticationResult result2 = new AuthenticationResult( new UserBean(new Goid(0,123), "Alice"), new OpaqueSecurityToken() );
            Assert.assertEquals("Same user", result1, result2);

            AuthenticationResult result3 = new AuthenticationResult( new UserBean(new Goid(0,123), "Alice"), new OpaqueSecurityToken() );
            AuthenticationResult result4 = new AuthenticationResult( new UserBean(new Goid(0,123), "Bob"), new OpaqueSecurityToken() );
            Assert.assertFalse("Different user", result3.equals(result4));
            Assert.assertFalse("Different user", result4.equals(result3));

            AuthenticationResult result5 = new AuthenticationResult( new UserBean(new Goid(0,123), "Alice"), new OpaqueSecurityToken() );
            AuthenticationResult result6 = new AuthenticationResult( new UserBean(new Goid(0,124), "Alice"), new OpaqueSecurityToken() );
            Assert.assertFalse("Different user", result5.equals(result6));
            Assert.assertFalse("Different user", result6.equals(result5));
        }
        {
            AuthenticationResult result1 = new AuthenticationResult( new UserBean(new Goid(0,123), "Alice"), new OpaqueSecurityToken(), TestDocuments.getWssInteropAliceCert(), false );
            AuthenticationResult result2 = new AuthenticationResult( new UserBean(new Goid(0,123), "Alice"), new OpaqueSecurityToken(), TestDocuments.getWssInteropAliceCert(), false );
            Assert.assertEquals("Same user", result1, result2);

            AuthenticationResult result3 = new AuthenticationResult( new UserBean(new Goid(0,123), "Alice"), new OpaqueSecurityToken(), TestDocuments.getWssInteropAliceCert(), false  );
            AuthenticationResult result4 = new AuthenticationResult( new UserBean(new Goid(0,124), "Alice"), new OpaqueSecurityToken(), TestDocuments.getWssInteropAliceCert(), false  );
            Assert.assertFalse("Different user", result3.equals(result4));
            Assert.assertFalse("Different user", result4.equals(result3));

            AuthenticationResult result5 = new AuthenticationResult( new UserBean(new Goid(0,123), "Alice"), new OpaqueSecurityToken(), TestDocuments.getWssInteropAliceCert(), false  );
            AuthenticationResult result6 = new AuthenticationResult( new UserBean(new Goid(0,123), "Alice"), new OpaqueSecurityToken(), TestDocuments.getWssInteropBobCert(), false  );
            Assert.assertFalse("Different certificate", result5.equals(result6));
            Assert.assertFalse("Different certificate", result6.equals(result5));

            AuthenticationResult result7 = new AuthenticationResult( new UserBean(new Goid(0,123), "Alice"), new OpaqueSecurityToken(), TestDocuments.getWssInteropAliceCert(), false  );
            AuthenticationResult result8 = new AuthenticationResult( new UserBean(new Goid(0,123), "Alice"), new OpaqueSecurityToken(), TestDocuments.getWssInteropAliceCert(), true  );
            Assert.assertFalse("Different certificate status", result7.equals(result8));
            Assert.assertFalse("Different certificate status", result8.equals(result7));
        }
        {
            AuthenticationResult result1 = new AuthenticationResult( new UserBean(new Goid(0,123), "Alice"), new OpaqueSecurityToken(), null, false  );
            AuthenticationResult result2 = new AuthenticationResult( new UserBean(new Goid(0,123), "Alice"), new OpaqueSecurityToken(), TestDocuments.getWssInteropAliceCert(), false  );
            Assert.assertFalse("Null certificate", result1.equals(result2));
            Assert.assertFalse("Null certificate", result2.equals(result1));
        }
    }

    @Test
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

        AuthenticationResult aliceAuth = new AuthenticationResult(aliceUser, new OpaqueSecurityToken(), null, false);
        AuthenticationResult bobAuth = new AuthenticationResult(bobUser, new OpaqueSecurityToken(), null, false);

        Boolean b = aliceAuth.getCachedGroupMembership(aliceGroup);
        Assert.assertNull(b);

        aliceAuth.setCachedGroupMembership(aliceGroup, true);
        b = aliceAuth.getCachedGroupMembership(aliceGroup);
        Assert.assertNotNull(b);
        Assert.assertTrue(b);

        b = aliceAuth.getCachedGroupMembership(fooGroup);
        Assert.assertNull(b);

        aliceAuth.setCachedGroupMembership(fooGroup, false);
        b = aliceAuth.getCachedGroupMembership(fooGroup);
        Assert.assertNotNull(b);
        Assert.assertFalse(b);
        
        b = bobAuth.getCachedGroupMembership(aliceGroup);
        Assert.assertNull(b);

        bobAuth.setCachedGroupMembership(aliceGroup, false);
        b = bobAuth.getCachedGroupMembership(aliceGroup);
        Assert.assertNotNull(b);
        Assert.assertFalse(b);

        bobAuth.setCachedGroupMembership(fooGroup, true);
        b = bobAuth.getCachedGroupMembership(fooGroup);
        Assert.assertNotNull(b);
        Assert.assertTrue(b);

        b = aliceAuth.getCachedGroupMembership(fooGroup);
        Assert.assertNotNull(b);
        Assert.assertFalse(b);
    }
}
