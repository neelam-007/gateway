package com.l7tech.adminws.translation;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.framework.Test;

import java.util.Iterator;
import com.l7tech.objectmodel.EntityHeader;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: May 15, 2003
 *
 */
public class TypeTranslatorTest extends TestCase {

    public static Test suite() {
        return new TestSuite(TypeTranslatorTest.class);
    }

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

    public void testServiceIdentityProviderConfigToGenericOne() throws Exception {
        com.l7tech.adminws.identity.IdentityProviderConfig svcConfig = new com.l7tech.adminws.identity.IdentityProviderConfig();
        com.l7tech.identity.IdentityProviderConfig res = TypeTranslator.serviceIdentityProviderConfigToGenericOne(svcConfig);
        assertTrue("getting a result prior to setting anything", res != null);
        svcConfig.setDescription("description");
        svcConfig.setName("name");
        svcConfig.setOid(123);
        res = TypeTranslator.serviceIdentityProviderConfigToGenericOne(svcConfig);
        assertTrue("getting a result prior to setting type info", res != null);
        assertTrue("getting a result", res != null);
        assertTrue("name integrity", res.getName().equals(svcConfig.getName()));
        assertTrue("oid integrity", res.getOid() == svcConfig.getOid());
        svcConfig.setTypeClassName("type class name");
        svcConfig.setTypeDescription("type description");
        svcConfig.setTypeName("type name");
        svcConfig.setTypeOid(456);
        res = TypeTranslator.serviceIdentityProviderConfigToGenericOne(svcConfig);
        assertTrue("getting a result", res != null);
        assertTrue("getting a result", res != null);
        assertTrue("name integrity", res.getName().equals(svcConfig.getName()));
        assertTrue("oid integrity", res.getOid() == svcConfig.getOid());
        com.l7tech.identity.IdentityProviderType configType = res.getType();
        assertTrue("type integrity", svcConfig.getTypeClassName().equals(configType.getClassName()));
        assertTrue("type desc integrity", svcConfig.getTypeDescription().equals(configType.getDescription()));
        assertTrue("type name integrity", svcConfig.getTypeName().equals(configType.getName()));
        assertTrue("type oid integrity", svcConfig.getTypeOid() == configType.getOid());
    }

    public void testGenUserToServiceUser() throws Exception {
        com.l7tech.identity.User genUser = new com.l7tech.identity.internal.imp.UserImp();
        com.l7tech.adminws.identity.User res = TypeTranslator.genUserToServiceUser(genUser);
        assertTrue("getting a result prior to setting anything", res != null);
        genUser.setEmail("email");
        genUser.setFirstName("firstname");
        genUser.setLastName("lastname");
        genUser.setLogin("login");
        genUser.setOid(321);
        genUser.setPassword("password");
        res = TypeTranslator.genUserToServiceUser(genUser);
        assertTrue("getting a result", res != null);
        assertTrue("email integrity", genUser.getEmail().equals(res.getEmail()));
        assertTrue("firstname integrity", genUser.getFirstName().equals(res.getFirstName()));
        assertTrue("lastname integrity", genUser.getLastName().equals(res.getLastName()));
        assertTrue("login integrity", genUser.getLogin().equals(res.getLogin()));
        assertTrue("oid integrity", genUser.getOid() == res.getOid());
        assertTrue("password integrity", genUser.getPassword().equals(res.getPassword()));
    }

    public void testServiceUserToGenUser() throws Exception {
        com.l7tech.adminws.identity.User svcUser = new com.l7tech.adminws.identity.User();
        com.l7tech.identity.User res = TypeTranslator.serviceUserToGenUser(svcUser);
        assertTrue("getting a result prior to setting anything", res != null);
        svcUser.setEmail("email");
        svcUser.setFirstName("firstname");
        svcUser.setLastName("lastname");
        svcUser.setLogin("login");
        svcUser.setOid(321);
        svcUser.setPassword("password");
        res = TypeTranslator.serviceUserToGenUser(svcUser);
        assertTrue("getting a result", res != null);
        assertTrue("email integrity", svcUser.getEmail().equals(res.getEmail()));
        assertTrue("firstname integrity", svcUser.getFirstName().equals(res.getFirstName()));
        assertTrue("lastname integrity", svcUser.getLastName().equals(res.getLastName()));
        assertTrue("login integrity", svcUser.getLogin().equals(res.getLogin()));
        assertTrue("oid integrity", svcUser.getOid() == res.getOid());
        assertTrue("password integrity", svcUser.getPassword().equals(res.getPassword()));
    }

    public void testGenGroupToServiceGroup() throws Exception {
        com.l7tech.identity.Group genGroup = new com.l7tech.identity.internal.imp.GroupImp();
        com.l7tech.adminws.identity.Group res = TypeTranslator.genGroupToServiceGroup(genGroup);
        assertTrue("getting a result prior to setting anything", res != null);
        genGroup.setDescription("description");
        genGroup.setName("name");
        genGroup.setOid(654);
        res = TypeTranslator.genGroupToServiceGroup(genGroup);
        assertTrue("getting a result prior to setting members", res != null);
        assertTrue("Description integrity", genGroup.getDescription().equals(res.getDescription()));
        assertTrue("Name integrity", genGroup.getName().equals(res.getName()));
        assertTrue("oid integrity", genGroup.getOid() == res.getOid());
        java.util.Collection members = genGroup.getMemberHeaders();
        assertTrue("com.l7tech.identity.internal.imp.GroupImp shall never return null members collection", members != null);
        com.l7tech.objectmodel.imp.EntityHeaderImp[] headerArray = new com.l7tech.objectmodel.imp.EntityHeaderImp[3];
        headerArray[0] = new com.l7tech.objectmodel.imp.EntityHeaderImp();
        headerArray[1] = new com.l7tech.objectmodel.imp.EntityHeaderImp();
        headerArray[2] = new com.l7tech.objectmodel.imp.EntityHeaderImp();
        headerArray[0].setName("zero");
        headerArray[1].setName("one");
        headerArray[2].setName("two");
        headerArray[0].setOid(0);
        headerArray[1].setOid(1);
        headerArray[2].setOid(2);
        headerArray[0].setType(com.l7tech.identity.User.class);
        headerArray[1].setType(com.l7tech.identity.User.class);
        headerArray[2].setType(com.l7tech.identity.User.class);
        members.add(headerArray[0]);members.add(headerArray[1]);members.add(headerArray[2]);
        res = TypeTranslator.genGroupToServiceGroup(genGroup);
        assertTrue("getting a result", res != null);
        assertTrue("Description integrity", genGroup.getDescription().equals(res.getDescription()));
        assertTrue("Name integrity", genGroup.getName().equals(res.getName()));
        assertTrue("oid integrity", genGroup.getOid() == res.getOid());
        for (int i = 0; i < 3; i++) {
            assertTrue("Member name integrity", res.getMembers()[i].getName().equals(headerArray[i].getName()));
            assertTrue("Member oid integrity", res.getMembers()[i].getOid() == headerArray[i].getOid());
            assertTrue("Member type integrity", Class.forName(res.getMembers()[i].getType()).equals(headerArray[i].getType()));
        }
    }

    public void testServiceGroupToGenGroup() throws Exception {
        com.l7tech.adminws.identity.Group svcGroup = new com.l7tech.adminws.identity.Group();
        com.l7tech.identity.Group res = TypeTranslator.serviceGroupToGenGroup(svcGroup);
        assertTrue("getting a result prior to setting anything", res != null);
        svcGroup.setDescription("description");
        svcGroup.setName("name");
        svcGroup.setOid(654);
        res = TypeTranslator.serviceGroupToGenGroup(svcGroup);
        assertTrue("getting a result prior to setting members", res != null);
        assertTrue("Description integrity", svcGroup.getDescription().equals(res.getDescription()));
        assertTrue("Name integrity", svcGroup.getName().equals(res.getName()));
        assertTrue("oid integrity", svcGroup.getOid() == res.getOid());
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
        headerArray[0].setType(com.l7tech.identity.User.class.getName());
        headerArray[1].setType(com.l7tech.identity.User.class.getName());
        headerArray[2].setType(com.l7tech.identity.User.class.getName());
        svcGroup.setMembers(headerArray);
        res = TypeTranslator.serviceGroupToGenGroup(svcGroup);
        assertTrue("getting a result", res != null);
        assertTrue("Description integrity", svcGroup.getDescription().equals(res.getDescription()));
        assertTrue("Name integrity", svcGroup.getName().equals(res.getName()));
        assertTrue("oid integrity", svcGroup.getOid() == res.getOid());
        java.util.Collection membersCol = res.getMembers();
        java.util.Iterator iter = membersCol.iterator();
        int count = 0;
        while (iter.hasNext()) {
            EntityHeader header = (EntityHeader)iter.next();
            assertTrue("Member name integrity", header.getName().equals(headerArray[count].getName()));
            assertTrue("Member oid integrity", header.getOid() == headerArray[count].getOid());
            assertTrue("Member type integrity", header.getType().equals(Class.forName(headerArray[count].getType())));
            ++count;
        }
    }
}
