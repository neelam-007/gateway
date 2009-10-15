package com.l7tech.server.identity.ldap;

import org.junit.Test;
import org.junit.Assert;

/**
 *
 */
public class LdapSearchFilterTest {

    @Test
    public void testEmptyExpressions() {
        {
            LdapSearchFilter filter = new LdapSearchFilter();
            filter.or();
            filter.end();
            Assert.assertTrue( "Empty or filter", filter.isEmpty() );
        }
        {
            LdapSearchFilter filter = new LdapSearchFilter();
            filter.and();
            filter.end();
            Assert.assertTrue( "Empty and filter", filter.isEmpty() );
        }
    }

    @Test
    public void testBuildFilter() {
        LdapSearchFilter filter = new LdapSearchFilter();
        filter.or();
          filter.and();
            filter.objectClass( "myobjectclass" );
            filter.attrPresent( "myrequiredAttr" );
            filter.attrEquals( "myattribute", "myvalue" );
          filter.end();
          filter.and();
            filter.objectClass( "myobjectclass2" );
            filter.attrPresent( "myrequiredAttr2" );
          filter.end();
        filter.end();
        Assert.assertEquals( "Expression test", "(|(&(objectclass=myobjectclass)(myrequiredAttr=*)(myattribute=myvalue))(&(objectclass=myobjectclass2)(myrequiredAttr2=*)))", filter.buildFilter() );
    }
}
