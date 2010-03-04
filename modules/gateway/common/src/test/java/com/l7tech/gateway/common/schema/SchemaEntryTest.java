/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
package com.l7tech.gateway.common.schema;

import com.l7tech.test.BugNumber;
import org.junit.Assert;
import org.junit.Test;

public class SchemaEntryTest {

    /**
     * Tests that setting the name property always sets the nameHash property
     */
    @Test
    @BugNumber(8417)
    public void testCreateHash(){
        String s = SchemaEntry.createNameHash("");
        Assert.assertNotNull(s);

        SchemaEntry se = new SchemaEntry();
        se.setName("my name");

        Assert.assertNotNull(se.getNameHash());
    }
}
