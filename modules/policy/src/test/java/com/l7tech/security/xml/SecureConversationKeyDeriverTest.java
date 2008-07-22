/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.security.xml;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.Random;
import java.util.logging.Logger;

import com.l7tech.security.xml.SecureConversationKeyDeriver;

/**
 * @author mike
 */
public class SecureConversationKeyDeriverTest extends TestCase {
    private static Logger log = Logger.getLogger(SecureConversationKeyDeriverTest.class.getName());

    public SecureConversationKeyDeriverTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(SecureConversationKeyDeriverTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testSecureConversationKeyDeriver() throws Exception {
        String label = "WS-SecureConversation";
        int length = 16;
        byte[] nonce = new byte[length];
        final Random random = new Random();
        random.nextBytes(nonce);
        byte[] seed = new byte[label.length() + nonce.length];
        System.arraycopy(label.getBytes(), 0, seed, 0, label.length());
        System.arraycopy(nonce, 0, seed, label.length(), nonce.length);
        byte[] secretKey = new byte[256];
        random.nextBytes(secretKey);
        byte[] derivedKey = new SecureConversationKeyDeriver().pSHA1(secretKey, seed, length);
        assertNotNull(derivedKey);
    }
}
