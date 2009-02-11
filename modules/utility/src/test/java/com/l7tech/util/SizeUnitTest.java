/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.util;

import org.junit.Test;
import org.junit.Assert;

public class SizeUnitTest {

    @Test
    public void testParser() {
        Assert.assertEquals("2KiB == 2048", SizeUnit.parse("2KiB"), 2048);
        Assert.assertEquals("1,024KiB == one mibibytes", SizeUnit.parse("1,024KiB"), 1024*1024);
        Assert.assertEquals("5 == five bytes", SizeUnit.parse("5"), 5);
        Assert.assertEquals("10MiB == 10 mibibytes", SizeUnit.parse("10MiB"), 10*1024*1024);
        Assert.assertEquals("1GiB == 1 gibibytes", SizeUnit.parse("1GiB"), 1024*1024*1024);

        try { SizeUnit.parse("-1"); Assert.fail("Expected exception was not thrown"); } catch (Exception e) {}
        try { SizeUnit.parse("-"); Assert.fail("Expected exception was not thrown"); } catch (Exception e) {}
        try { SizeUnit.parse(""); Assert.fail("Expected exception was not thrown"); } catch (Exception e) {}
        try { SizeUnit.parse(null); Assert.fail("Expected exception was not thrown"); } catch (Exception e) {}
    }

    @Test
    public void testFormat() {
        Assert.assertEquals( "1B", SizeUnit.format(1) );
        Assert.assertEquals( "1KiB", SizeUnit.format(1024) );
        Assert.assertEquals( "2KiB", SizeUnit.format(1025) );
        Assert.assertEquals( "3MiB", SizeUnit.format(1024*1024*3) );
        Assert.assertEquals( "1GiB", SizeUnit.format(1024*1024*1024) );
    }
}