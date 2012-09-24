package com.l7tech.server.identity.ldap;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;

import java.util.ArrayList;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.when;

/**
 * Created with IntelliJ IDEA.
 * User: rballantyne
 * Date: 9/21/12
 * Time: 11:11 AM
 */
public class LdapUtilsTest {
    @Mock
    DirContext dirContext;
    @Mock
    Name name;
    @Mock
    Attribute attribute;
    @Mock
    Attributes attributes;
    @Mock
    Attributes attributes2;
    @Mock
    NamingEnumeration<String> attributeNames;
    @Mock
    NamingEnumeration<String> attributeNames2;
    @Mock
    NamingEnumeration<String> attributeValues;
    @Mock
    NamingEnumeration<String> attributeValues2;

    private static String TEST_RANGE_NAME = "member;range=0-2";

    LdapUtils.LdapTemplate ldapTemplate;

    @Before
    public void initMocks() throws Exception {

        MockitoAnnotations.initMocks(this);

        // define behaviour
        //  - first part of paged response
        when(attributes.getIDs()).thenReturn(attributeNames);
        when(attributeNames.hasMore()).thenReturn(true);
        when(attributeNames.next()).thenReturn(TEST_RANGE_NAME);
        when(attributeValues.hasMore()).thenReturn(true).thenReturn(true).thenReturn(true).thenReturn(false);
        when(attributeValues.next()).thenReturn("cn=one").thenReturn("cn=two").thenReturn("cn=three");
        when((NamingEnumeration<String>) (attribute.getAll())).thenReturn(attributeValues2);

        //  - second and final part of paged  response
        when(attributes2.getIDs()).thenReturn(attributeNames2);
        when(attributes2.get("member;range=3-*")).thenReturn(attribute);
        when(attributeNames2.hasMore()).thenReturn(true);
        when(attributeNames2.next()).thenReturn("member;range=3-*");
        when(attributeValues2.hasMore()).thenReturn(true).thenReturn(true).thenReturn(false);
        when(attributeValues2.next()).thenReturn("cn=four").thenReturn("cn=five");
        when(dirContext.getAttributes((Name) any(),(String[])any())).thenReturn(attributes2);

        ldapTemplate = new LdapUtils.LdapTemplate("dc=searchBase",new String[] { "member","login" }) {
            @Override
            DirContext getDirContext() throws NamingException {
                return dirContext;
            }
        };
    }

    @Test
    public void testRangedResponseOfAttribute() throws NamingException {

        Assert.assertTrue(ldapTemplate.rangedResponseOfAttribute("member", attributes).equals(TEST_RANGE_NAME));
    }

    @Test
    public void testObtainAllRangedResponsesOfAttribute() throws NamingException {

       String results[] = ldapTemplate.obtainAllRangedResponsesOfAttribute(TEST_RANGE_NAME,attributeValues,dirContext,"cn=dummy");
       Assert.assertArrayEquals(results,new String[] { "cn=one", "cn=two", "cn=three", "cn=four", "cn=five" } );
    }
}
