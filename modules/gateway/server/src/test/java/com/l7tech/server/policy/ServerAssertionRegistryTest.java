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

        String hash1 =  new ModulesScanner.DigestBuilder().file(file).build();
        log.info("First hash = " + hash1);
        assertEquals("a2ea5f6e5dc3e355fe8846afa10c553e7a9b62b2aab041eef5efd9128a420f6f", hash1);

        file.delete();
        os = new FileOutputStream(file);
        IOUtils.copyStream(new RandomInputStream(1, 16384), os);
        IOUtils.copyStream(new RandomInputStream(3, 442233), os);
        os.close();

        String hash2 = new ModulesScanner.DigestBuilder().file(file).build();
        log.info("Second hash = " + hash2);
        assertEquals("eb81b449fa9a4f8c850b546afce313e6bf7b8eafb92b86f9070bf74a248918e4", hash2);

        assertFalse(hash1.equals(hash2));
    }
}
