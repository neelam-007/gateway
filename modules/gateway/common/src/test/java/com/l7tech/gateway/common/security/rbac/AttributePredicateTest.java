package com.l7tech.gateway.common.security.rbac;

import com.l7tech.objectmodel.EntityType;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.test.BugId;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit test for {@link AttributePredicate}.
 */
public class AttributePredicateTest {
    Role role = new Role();
    Permission perm = new Permission(role, OperationType.CREATE, EntityType.ANY);
    AttributePredicate p = new AttributePredicate(perm, "name", "blah");

    @Test
    public void testNullaryConstructor() {
        p = new AttributePredicate();
        assertTrue(p.getPermission() == null);
        assertTrue(p.getAttribute() == null);
        assertTrue(p.getValue() == null);
        assertTrue(p.getMode() == null);
    }

    @Test
    public void testFullConstructor() {
        p = new AttributePredicate(perm, "name", "blah");
        assertTrue(p.getPermission() == perm);
        assertEquals("name", p.getAttribute());
        assertEquals("blah", p.getValue());
        assertTrue(p.getMode() == null);
    }

    @Test
    public void testNameMatchesTrustedCert() {
        final TrustedCert cert = new TrustedCert();
        cert.setName("blah");
        assertTrue(p.matches(cert));
    }

    @Test
    public void testNameNoMatchTrustedCert() {
        final TrustedCert cert = new TrustedCert();
        cert.setName("blah2");
        assertFalse(p.matches(cert));
    }

    @Test
    public void testNameMatchPolicy() {
        final Policy policyBlah = new Policy(PolicyType.GLOBAL_FRAGMENT, "blah", null, false);
        assertTrue(p.matches(policyBlah));
    }

    @Test
    public void testNameNoMatchPolicy() {
        final Policy policyBloof = new Policy(PolicyType.GLOBAL_FRAGMENT, "bloof", null, false);
        assertFalse(p.matches(policyBloof));
    }

    @Test
    public void testNameNoMatchNotNamedEntity() {
        assertFalse(p.matches(new AttributePredicate(perm, "name", "blah")));
    }

    @Test
    public void testName_sw_success() {
        p.setMode("sw");
        final TrustedCert cert = new TrustedCert();
        cert.setName("blah.bloof");
        assertTrue(p.matches(cert));
    }

    @Test
    public void testName_sw_fail() {
        p.setMode("sw");
        final TrustedCert cert = new TrustedCert();
        cert.setName("bleh.bloof");
        assertFalse(p.matches(cert));
    }

    @Test
    public void testName_eq_fail() {
        p.setMode("eq");
        final TrustedCert cert = new TrustedCert();
        cert.setName("blah.bloof");
        assertFalse(p.matches(cert));
    }

    @Test
    public void testTrustedCert() {
        Role role = new Role();
        Permission perm = new Permission(role, OperationType.CREATE, EntityType.TRUSTED_CERT);
        AttributePredicate p = new AttributePredicate(perm, "issuerDn", "cn=blah");

        TrustedCert gus = new TrustedCert();
        gus.setName("gus");
        gus.setIssuerDn("cn=blah");

        assertTrue(p.matches(gus));
    }

    @Test
    public void testTrustedCert_fail() {
        Role role = new Role();
        Permission perm = new Permission(role, OperationType.CREATE, EntityType.TRUSTED_CERT);
        AttributePredicate p = new AttributePredicate(perm, "issuerDn", "cn=blah");

        TrustedCert gus = new TrustedCert();
        gus.setName("gus");
        gus.setIssuerDn("cn=bleh");

        assertFalse(p.matches(gus));
    }

    @Test
    public void testTrustedCertStartsWith() {
        Role role = new Role();
        Permission perm = new Permission(role, OperationType.CREATE, EntityType.TRUSTED_CERT);
        AttributePredicate p = new AttributePredicate(perm, "issuerDn", "cn=blah");
        p.setMode("sw");

        TrustedCert gus = new TrustedCert();
        gus.setName("gus");
        gus.setIssuerDn("cn=blah,o=foo");

        assertTrue(p.matches(gus));
    }

    @Test
    public void testTrustedCertStartsWith_fail() {
        Role role = new Role();
        Permission perm = new Permission(role, OperationType.CREATE, EntityType.TRUSTED_CERT);
        AttributePredicate p = new AttributePredicate(perm, "issuerDn", "cn=blah");
        p.setMode("sw");

        TrustedCert gus = new TrustedCert();
        gus.setName("gus");
        gus.setIssuerDn("cn=bleh,o=foo");

        assertFalse(p.matches(gus));
    }

    @BugId("SSG-5354")
    @Test
    public void createAnonymousCloneKeepsEntityType() {
        final Permission perm = new Permission(new Role(), OperationType.CREATE, EntityType.TRUSTED_CERT);
        final AttributePredicate predicate = new AttributePredicate(perm, "issuerDn", "cn=blah");
        final AttributePredicate copy = (AttributePredicate) predicate.createAnonymousClone();
        assertEquals(EntityType.TRUSTED_CERT, copy.getPermission().getEntityType());
        assertNull(copy.getPermission().getRole());
    }

}
