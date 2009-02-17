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
        Assert.assertEquals("2KB == 2048", SizeUnit.parse("2KB"), 2048);
        Assert.assertEquals("1,024KB == one megabytes", SizeUnit.parse("1,024KB"), 1024*1024);
        Assert.assertEquals("5 == five bytes", SizeUnit.parse("5"), 5);
        Assert.assertEquals("10MB == 10 megabytes", SizeUnit.parse("10MB"), 10*1024*1024);
        Assert.assertEquals("1GB == 1 gigabytes", SizeUnit.parse("1GB"), 1024*1024*1024);

        try { SizeUnit.parse("-1"); Assert.fail("Expected exception was not thrown"); } catch (Exception e) {}
        try { SizeUnit.parse("-"); Assert.fail("Expected exception was not thrown"); } catch (Exception e) {}
        try { SizeUnit.parse(""); Assert.fail("Expected exception was not thrown"); } catch (Exception e) {}
        try { SizeUnit.parse(null); Assert.fail("Expected exception was not thrown"); } catch (Exception e) {}
    }

    @Test
    public void testFormat() {
        Assert.assertEquals( "1B", SizeUnit.format(1) );
        Assert.assertEquals( "1KB", SizeUnit.format(1024) );
        Assert.assertEquals( "2KB", SizeUnit.format(1025) );
        Assert.assertEquals( "3MB", SizeUnit.format(1024*1024*3) );
        Assert.assertEquals( "1GB", SizeUnit.format(1024*1024*1024) );
    }
}