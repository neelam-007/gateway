/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.util;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.logging.Logger;
import java.security.MessageDigest;

/**
 *
 * User: mike
 * Date: Sep 4, 2003
 * Time: 12:15:53 PM
 */
public class HexUtilsTest extends TestCase {
    private static Logger log = Logger.getLogger(HexUtilsTest.class.getName());

    public HexUtilsTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(HexUtilsTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testEncodeMd5Digest() throws Exception {
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        md5.reset();
        md5.update("alice:myrealm:secret".getBytes());
        String result = HexUtils.encodeMd5Digest(md5.digest());
        log.info("result = " + result);
        assertTrue(result != null);
        assertTrue(result.equals("049fcf3ec19e0d6fd33212b90ce599e4"));
    }
}
