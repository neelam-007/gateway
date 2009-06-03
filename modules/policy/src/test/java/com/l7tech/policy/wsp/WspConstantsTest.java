/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.policy.wsp;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.AllAssertions;

import java.util.logging.Logger;
import java.util.Set;
import java.util.HashSet;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Test;
import org.junit.Assert;

/**
 * Ensures that everything that can be serialized is also listed in {@link AllAssertions#SERIALIZABLE_EVERYTHING}.
 */
public class WspConstantsTest {
    private static Logger log = Logger.getLogger(WspConstantsTest.class.getName());

    private String getLocalName(Class c) {
        String fullname = c.getName();
        int lastdot = fullname.lastIndexOf('.');
        return fullname.substring(lastdot + 1);
    }

    @Test
    public void testDuplicateMappings() throws Exception {
        TypeMapping[] mappings = WspConstants.typeMappings;
        Set<String> typeMapSet = new HashSet<String>();

        // Accepted duplication for backwards compatibility
        Set<String> dupeWhitelist = new HashSet<String>( Arrays.asList(
            "com.l7tech.policy.wsp.IntegrityMapping/com.l7tech.policy.assertion.xmlsec.RequireWssSignedElement/RequestWssIntegrity",
            "com.l7tech.policy.wsp.IntegrityMapping/com.l7tech.policy.assertion.xmlsec.RequireWssSignedElement/Integrity",
            "com.l7tech.policy.wsp.ConfidentialityMapping/com.l7tech.policy.assertion.xmlsec.RequireWssEncryptedElement/RequestWssConfidentiality",
            "com.l7tech.policy.wsp.ConfidentialityMapping/com.l7tech.policy.assertion.xmlsec.RequireWssEncryptedElement/Confidentiality",
            "com.l7tech.policy.wsp.AssertionMapping/com.l7tech.policy.assertion.xmlsec.WssSignElement/ResponseWssIntegrity",
            "com.l7tech.policy.wsp.AssertionMapping/com.l7tech.policy.assertion.xmlsec.WssEncryptElement/ResponseWssConfidentiality",
            "com.l7tech.policy.wsp.AssertionMapping/com.l7tech.policy.assertion.xmlsec.WssReplayProtection/RequestWssReplayProtection",
            "com.l7tech.policy.wsp.AssertionMapping/com.l7tech.policy.assertion.xmlsec.AddWssTimestamp/ResponseWssTimestamp",
            "com.l7tech.policy.wsp.AssertionMapping/com.l7tech.policy.assertion.xmlsec.RequireWssTimestamp/RequestWssTimestamp",
            "com.l7tech.policy.wsp.AssertionMapping/com.l7tech.policy.assertion.xmlsec.AddWssSecurityToken/ResponseWssSecurityToken",
            "com.l7tech.policy.wsp.ArrayTypeMapping/[Ljava.lang.String;/fieldNames"
        ) );

        // Check for duplicates
        for (TypeMapping typeMapping : mappings) {
            String mappingId = typeMapping.getClass().getName() + "/" + typeMapping.getMappedClass().getName();
            String fullId = mappingId + "/" + typeMapping.getExternalName();
            if ( !typeMapSet.add(mappingId) && !dupeWhitelist.contains(fullId)) {
                Assert.fail("Duplicate type mapping: " + fullId);
            }
        }
    }

    @Test
    public void testAllAssertionsIsComplete() throws Exception {
        Set<Class<? extends Assertion>> assertionClasses = new HashSet<Class<? extends Assertion>>();
        Assertion[] all = AllAssertions.SERIALIZABLE_EVERYTHING;
        for (Assertion ass : all)
            assertionClasses.add(ass.getClass());

        boolean failed = false;
        Collection<Class<? extends Assertion>> needed = new ArrayList<Class<? extends Assertion>>();

        TypeMapping[] mappings = WspConstants.typeMappings;
        for (TypeMapping typeMapping : mappings) {
            Class clazz = typeMapping.getMappedClass();
            if (Assertion.class.isAssignableFrom(clazz)) {
                if (!assertionClasses.contains(clazz)) {
                    //noinspection unchecked
                    needed.add(clazz);
                    log.warning("Class not listed in AllAssertions.SERIALIZABLE_EVERYTHING: " + clazz.getName());
                    failed = true;
                }
            }
        }

        if (failed) {
            System.err.println("Add the following lines to AllAssertions.SERIALIZABLE_EVERYTHING: \n");
            for (Class<? extends Assertion> assclass : needed) {
                String assname = getLocalName(assclass);
                System.out.println("        new " + assname + "(),");
            }

            Assert.fail("At least one assertion mapping was found for an assertion class not represented in AllAssertions.SERIALIZABLE_EVERYTHING");
        }
    }
}
