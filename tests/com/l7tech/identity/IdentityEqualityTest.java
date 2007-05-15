/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.identity;

import com.l7tech.identity.internal.InternalGroup;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.identity.ldap.LdapGroup;
import com.l7tech.identity.ldap.LdapUser;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * @author alex
 */
public class IdentityEqualityTest extends TestCase {
    public IdentityEqualityTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(IdentityEqualityTest.class);
    }

    /**
     * Test that DNs in {@link LdapUser#equals} are compared semantically (e.g. case, quoting, escaping) 
     */
    public void testLdapUserSameSemanticDn() throws Exception {
        LdapUser u1 = ldapBob();
        u1.setDn("CN=bob, ou=\"Developers, developers\", DC=l7tech, DC=com");

        LdapUser u2 = ldapBob();
        u2.setDn("cn=bob,ou=Developers\\, developers,dc=l7tech,dc=com");

        assertEquals(u1, u2);
    }

    /**
     * Test that DNs in {@link LdapUser#equals} are compared semantically (e.g. case, quoting, escaping)
     */
    public void testLdapUserDiffSemanticDn() throws Exception {
        LdapUser u1 = ldapBob();
        u1.setDn("CN=bob, ou=\"Developers, developers\", DC=l7tech, DC=com");

        LdapUser u2 = ldapBob();
        u2.setDn("cn=bob,ou=Developers\\, developers,dc=l7tech,dc=org");

        assertFalse(u1.equals(u2));
    }

    /**
     * Test that DNs are compared semantically (e.g. case, quoting, escaping) by {@link LdapGroup#equals}
     */
    public void testLdapGroupSameSemanticDn() throws Exception {
        LdapGroup g1 = ldapDevelopers();
        g1.setDn("ou=\"Developers, developers\", DC=l7tech, DC=com");

        LdapGroup g2 = ldapDevelopers();
        g2.setDn("ou=Developers\\, developers,dc=l7tech,dc=com");

        assertEquals(g1, g2);
    }

    /**
     * Test that DNs are compared semantically (e.g. case, quoting, escaping) by {@link LdapGroup#equals}
     */
    public void testLdapGroupDiffSemanticDn() throws Exception {
        LdapGroup g1 = ldapDevelopers();
        g1.setDn("ou=\"Developers, developers\", DC=l7tech, DC=com");

        LdapGroup g2 = ldapDevelopers();
        g2.setDn("ou=Developers\\, developers,dc=l7tech,dc=org");

        assertFalse(g1.equals(g2));
    }

    /**
     * Test that {@link com.l7tech.objectmodel.PersistentEntity#getVersion} is not con
     * @throws Exception
     */
    public void testInternalUserVersionIgnored() throws Exception {
        InternalUser u1 = internalBob();
        u1.setVersion(-1);

        InternalUser u2 = internalBob();
        u2.setVersion(-2);

        assertEquals(u1, u2);
    }

    /**
     * Test that {@link com.l7tech.objectmodel.PersistentEntity#getVersion} is not con
     * @throws Exception
     */
    public void testInternalUserDepartment() throws Exception {
        InternalUser u1 = internalBob();
        u1.setDepartment("Development");

        InternalUser u2 = internalBob();
        u2.setDepartment("Sales");

        assertFalse(u1.equals(u2));
    }

    public void testInternalGroupDiffDescription() throws Exception {
        InternalGroup g1 = internalDevelopers();
        g1.setDescription("Just a bunch of developers");

        InternalGroup g2 = internalDevelopers();
        g2.setDescription("T-shirt brigade");

        assertFalse(g1.equals(g2));
    }

    private InternalGroup internalDevelopers() {
        return new InternalGroup("Developers");
    }

    private LdapUser ldapBob() {
        return new LdapUser(1234, null, "bob");
    }

    private LdapGroup ldapDevelopers() {
        return new LdapGroup(1234, null, "developers");
    }

    private InternalUser internalBob() {
        return new InternalUser("bob");
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
}
