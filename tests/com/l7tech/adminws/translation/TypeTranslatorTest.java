package com.l7tech.adminws.translation;

import junit.framework.TestCase;

import java.util.Iterator;

import com.l7tech.objectmodel.EntityHeader;

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

    public void testHeaderArrayToCollection() throws Exception {
        com.l7tech.adminws.identity.Header[] headerArray = new com.l7tech.adminws.identity.Header[3];
        headerArray[0] = new com.l7tech.adminws.identity.Header();
        headerArray[1] = new com.l7tech.adminws.identity.Header();
        headerArray[2] = new com.l7tech.adminws.identity.Header();
        headerArray[0].setName("zero");
        headerArray[1].setName("one");
        headerArray[2].setName("two");
        headerArray[0].setOid(0);
        headerArray[1].setOid(1);
        headerArray[2].setOid(2);
        headerArray[0].setType("com.l7tech.adminws.translation.TypeTranslatorTest");
        headerArray[1].setType("com.l7tech.adminws.translation.TypeTranslatorTest");
        headerArray[2].setType("com.l7tech.adminws.translation.TypeTranslatorTest");
        java.util.Collection res = TypeTranslator.headerArrayToCollection(headerArray);
        assertTrue("getting a result", res != null);
        assertTrue("the size is right", res.size() == 3);
        Iterator iter = res.iterator();
        iter.hasNext();
        EntityHeader header = (EntityHeader)iter.next();
        assertTrue("getting a result", header != null);
        assertTrue("result type is valid", header.getType() == getClass());
        assertTrue("result name is valid", header.getName().equals("zero"));
        assertTrue("result oid is valid", header.getOid() == 0);
        iter.hasNext();
        header = (EntityHeader)iter.next();
        assertTrue("getting a result", header != null);
        assertTrue("result type is valid", header.getType() == getClass());
        assertTrue("result name is valid", header.getName().equals("one"));
        assertTrue("result oid is valid", header.getOid() == 1);
        iter.hasNext();
        header = (EntityHeader)iter.next();
        assertTrue("getting a result", header != null);
        assertTrue("result type is valid", header.getType() == getClass());
        assertTrue("result name is valid", header.getName().equals("two"));
        assertTrue("result oid is valid", header.getOid() == 2);
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
