package com.l7tech.server.policy;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.framework.Test;

import java.util.logging.Logger;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.IOException;

import com.l7tech.util.HexUtils;
import com.l7tech.common.io.RandomInputStream;

/**
 *
 */
public class ServerAssertionRegistryTest extends TestCase {
    private static final Logger log = Logger.getLogger(ServerAssertionRegistryTest.class.getName());

    public ServerAssertionRegistryTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(ServerAssertionRegistryTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testGetFileSha1() throws Exception {
        File file = new File("gfsha1_test.txt");
        try {
            doTestGetFileSha1(file);
        } finally {
            file.delete();
        }
    }

    private void doTestGetFileSha1(File file) throws IOException {
        file.delete();
        OutputStream os = new FileOutputStream(file);
        HexUtils.copyStream(new RandomInputStream(1, 16384), os);
        HexUtils.copyStream(new RandomInputStream(2, 382733), os);
        os.close();

        String hash1 = ServerAssertionRegistry.getFileSha1(file);
        log.info("First hash = " + hash1);
        assertEquals("4a427de06d2a4973800103501f449f8dd41e787b", hash1);

        file.delete();
        os = new FileOutputStream(file);
        HexUtils.copyStream(new RandomInputStream(1, 16384), os);
        HexUtils.copyStream(new RandomInputStream(3, 442233), os);
        os.close();

        String hash2 = ServerAssertionRegistry.getFileSha1(file);
        log.info("Second hash = " + hash2);
        assertEquals("552c282fabc472b6c2fabb8f21c52b748754d31c", hash2);

        assertFalse(hash1.equals(hash2));
    }
}
