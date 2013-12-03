package com.l7tech.server.policy;

import com.l7tech.server.policy.module.ModulesScanner;
import com.l7tech.util.IOUtils;
import com.l7tech.common.io.RandomInputStream;
import org.junit.Test;
import static org.junit.Assert.*;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Logger;

/**
 *
 */
public class ServerAssertionRegistryTest {
    private static final Logger log = Logger.getLogger(ServerAssertionRegistryTest.class.getName());

    @Test
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
        IOUtils.copyStream(new RandomInputStream(1, 16384), os);
        IOUtils.copyStream(new RandomInputStream(2, 382733), os);
        os.close();

        String hash1 =  new ModulesScanner.Sha1Builder().file(file).build();
        log.info("First hash = " + hash1);
        assertEquals("4a427de06d2a4973800103501f449f8dd41e787b", hash1);

        file.delete();
        os = new FileOutputStream(file);
        IOUtils.copyStream(new RandomInputStream(1, 16384), os);
        IOUtils.copyStream(new RandomInputStream(3, 442233), os);
        os.close();

        String hash2 = new ModulesScanner.Sha1Builder().file(file).build();
        log.info("Second hash = " + hash2);
        assertEquals("552c282fabc472b6c2fabb8f21c52b748754d31c", hash2);

        assertFalse(hash1.equals(hash2));
    }
}
