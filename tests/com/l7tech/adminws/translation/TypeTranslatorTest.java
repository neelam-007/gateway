package com.l7tech.adminws.translation;

import junit.framework.TestCase;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: May 15, 2003
 *
 */
public class TypeTranslatorTest extends TestCase {

    public void testserviceHeaderToGenHeader() throws Exception {
        com.l7tech.adminws.identity.Header stubHeader = new com.l7tech.adminws.identity.Header();
        stubHeader.setName("name");
        stubHeader.setOid(123);
        stubHeader.setType("com.l7tech.adminws.translation.TypeTranslatorTest");
        com.l7tech.objectmodel.EntityHeader res = TypeTranslator.serviceHeaderToGenHeader(stubHeader);
        assertTrue("getting a result", res != null);
        assertTrue("result type is valid", res.getType() == getClass());
        assertTrue("result name is valid", res.getName().equals("name"));
        assertTrue("result oid is valid", res.getOid() == 123);
    }

    public void headerArrayToCollection() {
    }

    public void collectionToServiceHeaders() {
    }

    public void genericToServiceIdProviderConfig() {
    }

    public void serviceIdentityProviderConfigToGenericOne() {
    }

    public void genUserToServiceUser() {
    }

    public void serviceUserToGenUser() {
    }

    public void genGroupToServiceGroup() {
    }

    public void serviceGroupToGenGroup() {
    }
}
