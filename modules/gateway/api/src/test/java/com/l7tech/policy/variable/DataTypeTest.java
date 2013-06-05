package com.l7tech.policy.variable;

import junit.framework.Assert;
import junit.framework.TestCase;

public class DataTypeTest extends TestCase {
    public void testForNameUsesShortname() {
        Assert.assertEquals(DataType.STRING, DataType.forName(DataType.STRING.getShortName()));
    }

    public void testForNameUnknown() {
        Assert.assertEquals(DataType.UNKNOWN, DataType.forName("what?"));
    }

    public void testForNameNull() {
        Assert.assertEquals(DataType.UNKNOWN, DataType.forName(null));
    }
}
