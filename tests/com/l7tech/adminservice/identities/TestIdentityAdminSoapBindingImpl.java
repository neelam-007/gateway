package com.l7tech.adminservice.identities;

import junit.framework.TestCase;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: May 7, 2003
 *
 */
public class TestIdentityAdminSoapBindingImpl extends TestCase {
    public void testListProviders() throws Exception {
        IdentityAdminSoapBindingImpl idWS = new IdentityAdminSoapBindingImpl();
        assertTrue("listProviders() does not return null", idWS.listProviders() != null);
    }
    public void testListUsersInProvider() throws Exception {
        IdentityAdminSoapBindingImpl idWS = new IdentityAdminSoapBindingImpl();
        assertTrue("listUsersInProvider() does not return null", idWS.listUsersInProvider(5646) != null);
    }
    public void testListGroupsInProvider() throws Exception {
        IdentityAdminSoapBindingImpl idWS = new IdentityAdminSoapBindingImpl();
        assertTrue("listGroupsInProvider() does not return null", idWS.listGroupsInProvider(5646) != null);
    }
}
