package com.l7tech.server.security.sharedkey;

import com.l7tech.objectmodel.FindException;
import com.l7tech.util.ExceptionUtils;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.*;

/**
 * Unit test for the higher-level functionality of SharedKeyManagerImpl (that is, everything except the Hibernate interaction).
 */                                 
public class SharedKeyManagerTest {
    private static final Logger logger = Logger.getLogger(SharedKeyManagerTest.class.getName());
    private static final char[] PASS = "sekrit".toCharArray();


    @Test
    public void testCreateNewKey() throws FindException {
        SharedKeyManagerStub skm = new SharedKeyManagerStub(PASS);
        assertIsDecentKey(skm.getSharedKey());
        assertTrue(skm.getSaved().getB64edKey().length() < 200); // check for too-long record that DB may truncate
        logger.log(Level.INFO, "Shared key record: " + skm.getSaved().getEncodingID() + "," + skm.getSaved().getB64edKey());
    }

    @Test
    public void testReuseExistingKeySameInstance() throws FindException {
        SharedKeyManagerStub skm = new SharedKeyManagerStub(PASS);
        byte[] firstKey = skm.getSharedKey();
        assertIsDecentKey(firstKey);
        byte[] secondKey = skm.getSharedKey();
        assertTrue(Arrays.equals(firstKey, secondKey));
    }

    @Test
    public void testReuseExistingKeyDifferentInstance() throws FindException {
        SharedKeyManagerStub skm1 = new SharedKeyManagerStub(PASS);
        byte[] firstKey = skm1.getSharedKey();
        assertIsDecentKey(firstKey);

        SharedKeyManagerStub skm2 = new SharedKeyManagerStub(skm1);
        byte[] secondKey = skm2.getSharedKey();
        assertTrue(Arrays.equals(firstKey, secondKey));
    }

    @Test
    public void testCollisionOnSave() throws FindException {
        // Simulate a collision with another node that creates the key in between when we look for it and
        // when we save it
        SharedKeyManagerStub skm = new SharedKeyManagerStub(PASS) {
            protected Collection<SharedKeyRecord> selectSharedKeyRecordsByEncodingId() {
                return Collections.emptyList();
            }
        };

        // Let it create one
        byte[] key = skm.getSharedKey();
        assertIsDecentKey(key);

        // Now second instance tries to create one
        try {
            skm.getSharedKey();
            fail("Expected exception was not thrown");
        } catch (ConstraintViolationException cve) {
            logger.info("Expected exception was thrown: " + ExceptionUtils.getMessage(cve));
        }
    }
    
    @Test
    public void testInvalidEncodedFormat() {
        SharedKeyManagerStub skm = new SharedKeyManagerStub(PASS);
        skm.setSaved(new SharedKeyRecord(SharedKeyManagerStub.CLUSTER_WIDE_IDENTIFIER, "$TOTALLYBOGUS$"));
        try {
            skm.getSharedKey();
            fail("Expected excception was not thrown");
        } catch (FindException e) {
            logger.info("Expected exception was thrown: " + ExceptionUtils.getMessage(e));
        }
    }

    private void assertIsDecentKey(byte[] sharedKey) {
        assertNotNull(sharedKey);
        assertTrue(sharedKey.length > 15);
        assertIsNotAllZeroes(sharedKey);
    }

    private void assertIsNotAllZeroes(byte[] thing) {
        boolean sawNonzero = false;
        for (byte b : thing) {
            if (b != 0)
                sawNonzero = true;
        }
        assertTrue(sawNonzero);
    }

}
