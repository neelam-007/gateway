/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
package com.l7tech.uddi;

import com.l7tech.test.BugNumber;
import org.junit.Assert;
import org.junit.Test;

public class UDDIKeyedReferenceTest {

    /**
     * tModelKey names are case insensitive and need to be treated as such in both equals() and hashCode()
     */
    @Test
    @BugNumber(9455)
    public void testEqualsAndHashCodeImplementedCorrectly(){

        final UDDIKeyedReference gatewayReference =
                new UDDIKeyedReference("uddi:uddi.org:xml:localname", "service local name", "BugOverwriteBusinessService");

        final UDDIKeyedReference systinetReference =
                new UDDIKeyedReference("uddi:uddi.org:xml:localName", "uddi.org:xml:localName", "BugOverwriteBusinessService");

        Assert.assertEquals("Should be equal", gatewayReference, systinetReference);

        Assert.assertEquals("Hash codes should be equal", gatewayReference.hashCode(), systinetReference.hashCode());
    }
}
