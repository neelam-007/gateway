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
        headerArray[0].setType(getClass().getName());
        headerArray[1].setType(getClass().getName());
        headerArray[2].setType(getClass().getName());
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
        res = TypeTranslator.headerArrayToCollection(null);
        assertTrue("getting a result when passing null", res != null);
        res = TypeTranslator.headerArrayToCollection(new com.l7tech.adminws.identity.Header[0]);
        assertTrue("getting a result when passing empty array", res != null);
    }

    public void testCollectionToServiceHeaders() throws Exception {
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
        headerArray[0].setType(getClass().getName());
        headerArray[1].setType(getClass().getName());
        headerArray[2].setType(getClass().getName());
        java.util.Collection tmpres = TypeTranslator.headerArrayToCollection(headerArray);
        com.l7tech.adminws.identity.Header[] res = TypeTranslator.collectionToServiceHeaders(tmpres);
        assertTrue("getting a result", res != null);
        for (int i = 0; i < 2; i++)
        {
            assertTrue("name integrity", res[i].getName().equals(headerArray[i].getName()));
            assertTrue("oid integrity", res[i].getOid() == headerArray[i].getOid());
            assertTrue("type integrity", res[i].getType().equals(headerArray[i].getType()));
        }
    }

    public void testGenericToServiceIdProviderConfig() throws Exception {
        com.l7tech.identity.IdentityProviderConfig genConfig = new com.l7tech.identity.imp.IdentityProviderConfigImp();
        com.l7tech.adminws.identity.IdentityProviderConfig res = TypeTranslator.genericToServiceIdProviderConfig(genConfig);
        assertTrue("getting a result when nothing is set", res != null);
        genConfig.setDescription("description");
        genConfig.setName("name");
        genConfig.setOid(123);
        res = TypeTranslator.genericToServiceIdProviderConfig(genConfig);
        assertTrue("getting a result when no type is set", res != null);
        com.l7tech.identity.IdentityProviderType configType = new com.l7tech.identity.imp.IdentityProviderTypeImp();
        genConfig.setType(configType);
        res = TypeTranslator.genericToServiceIdProviderConfig(genConfig);
        assertTrue("getting a result with empty type", res != null);
        configType.setClassName(getClass().getName());
        configType.setDescription("description");
        configType.setName("name");
        configType.setOid(123);
        res = TypeTranslator.genericToServiceIdProviderConfig(genConfig);
        assertTrue("getting a result", res != null);
        assertTrue("name integrity", res.getName().equals(genConfig.getName()));
        assertTrue("oid integrity", res.getOid() == genConfig.getOid());
        assertTrue("description integrity", res.getDescription().equals(genConfig.getDescription()));
        assertTrue("type integrity", res.getTypeClassName().equals(configType.getClassName()));
        assertTrue("type desc integrity", res.getTypeDescription().equals(configType.getDescription()));
        assertTrue("type name integrity", res.getTypeName().equals(configType.getName()));
        assertTrue("type oid integrity", res.getTypeOid() == configType.getOid());
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
