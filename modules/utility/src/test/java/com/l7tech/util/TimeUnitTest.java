/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.util;

import org.junit.Assert;
import org.junit.Test;


public class TimeUnitTest {
    
    @Test
    public void testParser() throws Exception {
        Assert.assertEquals("60s == one minute", 60 * 1000, TimeUnit.parse("60s"));
        Assert.assertEquals("1ms == one millisecond", 1, TimeUnit.parse("1ms"));
        Assert.assertEquals("1h == one hour", 60 * 60 * 1000, TimeUnit.parse("1h"));
        Assert.assertEquals("1000(default ms) == one second", 1000, TimeUnit.parse("1000"));
        Assert.assertEquals("12m == 720000 ms", 720000, TimeUnit.parse("12m"));
        Assert.assertEquals("1d == 86400000ms", 86400000, TimeUnit.parse("1d"));
        Assert.assertEquals("1,000,000 == 1000s", 1000000, TimeUnit.parse("1,000,000"));
        Assert.assertEquals("1.5s == 1500ms", 1500, TimeUnit.parse("1.5s"));
        Assert.assertEquals(".5s == 500ms", 500, TimeUnit.parse(".5s"));
        Assert.assertEquals(".5d == 43200000", 43200000, TimeUnit.parse(".5d"));
        Assert.assertEquals("-1s == -1000s", -1000, TimeUnit.parse("-1s"));
        Assert.assertEquals("0 == 0", 0, TimeUnit.parse("0"));
        Assert.assertEquals("0.0 == 0", 0, TimeUnit.parse("0.0"));
        Assert.assertEquals(".000 == 0", 0, TimeUnit.parse(".000"));
        TimeUnit.parse("12345678901234567890"); // Ensure 20-digit input passes
        try { TimeUnit.parse("-12345678901234567890"); Assert.fail("Expected exception was not thrown"); } catch (Exception e) {}
        try { TimeUnit.parse("0.0.0"); Assert.fail("Expected exception was not thrown"); } catch (Exception e) {}
        try { TimeUnit.parse("-"); Assert.fail("Expected exception was not thrown"); } catch (Exception e) {}
        try { TimeUnit.parse(""); Assert.fail("Expected exception was not thrown"); } catch (Exception e) {}
        try { TimeUnit.parse(null);Assert.fail("Expected exception was not thrown"); } catch (Exception e) {}
    }
}