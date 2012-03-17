package com.l7tech.policy.assertion;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

/**
 *
 */
public class HttpRoutingAssertionTest {
    HttpRoutingAssertion ass = new HttpRoutingAssertion();
        
    EntityHeader fooHeader = new EntityHeader(22L, EntityType.TRUSTED_CERT, "foo", "a cert");
    EntityHeader barHeader = new EntityHeader(33L, EntityType.TRUSTED_CERT, "bar", "a cert");
    
    @Test
    public void testGetEntitiesUsed_nullcerts() throws Exception {
        ass.setTlsTrustedCertOids(null);
        ass.setTlsTrustedCertNames(new String[] { "blah" });
        
        EntityHeader[] headers = ass.getEntitiesUsed();
        assertEquals(0, headers.length);
    }
    
    @Test
    public void testGetEntitiesUsed_emptycerts() throws Exception{
        ass.setTlsTrustedCertOids(new Long[0]);
        ass.setTlsTrustedCertNames(null);

        EntityHeader[] headers = ass.getEntitiesUsed();
        assertEquals(0, headers.length);
    }
    
    @Test
    public void testGetEntitiesUsed_onecert() throws Exception {
        ass.setTlsTrustedCertOids(new Long[] { 44L });
        ass.setTlsTrustedCertNames(null);

        EntityHeader[] headers = ass.getEntitiesUsed();
        assertEquals(1, headers.length);
        assertEquals(44L, headers[0].getOid());
        assertNull(headers[0].getName());
    }

    @Test
    public void testGetEntitiesUsed_onecertWithName() throws Exception {
        ass.setTlsTrustedCertOids(new Long[] { 44L });
        ass.setTlsTrustedCertNames(new String[] { "blah" });

        EntityHeader[] headers = ass.getEntitiesUsed();
        assertEquals(1, headers.length);
        assertEquals(44L, headers[0].getOid());
        assertEquals("blah", headers[0].getName());
    }

    @Test
    public void testGetEntitiesUsed_onecertWithExtraName() throws Exception {
        ass.setTlsTrustedCertOids(new Long[] { 44L });
        ass.setTlsTrustedCertNames(new String[] { "blah", "foo" });

        EntityHeader[] headers = ass.getEntitiesUsed();
        assertEquals(1, headers.length);
        assertEquals(44L, headers[0].getOid());
        assertEquals("blah", headers[0].getName());
    }

    @Test
    public void testReplaceEntity_nocerts() throws Exception {
        ass.setTlsTrustedCertOids(null);
        ass.setTlsTrustedCertNames(new String[] { "blah" });

        ass.replaceEntity(fooHeader, barHeader);
        assertNull("oids should have been left null", ass.getTlsTrustedCertOids());
        assertEquals("names should not have been touched", 1, ass.getTlsTrustedCertNames().length);
        assertEquals("names should not have been touched", "blah", ass.getTlsTrustedCertNames()[0]);
    }

    @Test
    public void testReplaceEntity_emptycerts() throws Exception {
        ass.setTlsTrustedCertOids(new Long[0]);
        ass.setTlsTrustedCertNames(new String[] { "blah" });

        ass.replaceEntity(fooHeader, barHeader);
        assertEquals("oids should have been left empty", 0, ass.getTlsTrustedCertOids().length);
        assertEquals("names should not have been touched", 1, ass.getTlsTrustedCertNames().length);
        assertEquals("names should not have been touched", "blah", ass.getTlsTrustedCertNames()[0]);
    }

    @Test
    public void testReplaceEntity_mismatchingcerts() throws Exception {
        ass.setTlsTrustedCertOids(new Long[] { 77L });
        ass.setTlsTrustedCertNames(null);

        ass.replaceEntity(fooHeader, barHeader);
        assertEquals("oids should have been left alone", 1, ass.getTlsTrustedCertOids().length);
        assertEquals(new Long(77L), ass.getTlsTrustedCertOids()[0]);
        assertNull("names should not have been touched", ass.getTlsTrustedCertNames());
    }

    @Test
    public void testReplaceEntity_matchingNoName() throws Exception {
        ass.setTlsTrustedCertOids(new Long[] { 22L });
        ass.setTlsTrustedCertNames(null);

        ass.replaceEntity(fooHeader, barHeader);
        assertEquals(1, ass.getTlsTrustedCertOids().length);
        assertEquals("oid should have been replaced", new Long(33L), ass.getTlsTrustedCertOids()[0]);
        assertNull("names should not have been touched", ass.getTlsTrustedCertNames());
    }

    @Test
    public void testReplaceEntity_matchingWithName() throws Exception {
        ass.setTlsTrustedCertOids(new Long[] { 22L });
        ass.setTlsTrustedCertNames(new String[] { "blag" });

        ass.replaceEntity(fooHeader, barHeader);
        assertEquals(1, ass.getTlsTrustedCertOids().length);
        assertEquals("oid should have been replaced", new Long(33L), ass.getTlsTrustedCertOids()[0]);
        assertEquals(1, ass.getTlsTrustedCertNames().length);
        assertEquals("bar", ass.getTlsTrustedCertNames()[0]);
    }

    @Test
    public void testReplaceEntity_matchingWithTooFewNames() throws Exception {
        ass.setTlsTrustedCertOids(new Long[] { 11L, 44L, 22L, 88L });
        ass.setTlsTrustedCertNames(new String[] { "blag" });

        ass.replaceEntity(fooHeader, barHeader);
        assertTrue("oid should have been replaced", Arrays.equals(ass.getTlsTrustedCertOids(), new Long[] { 11L, 44L, 33L, 88L }));
        assertEquals(1, ass.getTlsTrustedCertNames().length);
        assertEquals("blag", ass.getTlsTrustedCertNames()[0]);
    }
}
