package com.l7tech.server.security.sharedkey;

import com.l7tech.objectmodel.FindException;
import com.l7tech.util.ExceptionUtils;
import org.hibernate.exception.ConstraintViolationException;
import static org.junit.Assert.*;
import org.junit.Test;
import org.springframework.dao.DataAccessException;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Unit test for the higher-level functionality of SharedKeyManagerImpl (that is, everything except the Hibernate interaction).
 */                                 
public class SharedKeyManagerTest {
    private static final Logger logger = Logger.getLogger(SharedKeyManagerTest.class.getName());
    private static final char[] PASS = "sekrit".toCharArray();


    @Test
    public void testCreateNewKey() throws FindException {
        TestSkm skm = new TestSkm(PASS);
        assertIsDecentKey(skm.getSharedKey());
        assertTrue(skm.saved.getB64edKey().length() < 200); // check for too-long record that DB may truncate
        logger.log(Level.INFO, "Shared key record: " + skm.saved.getEncodingID() + "," + skm.saved.getB64edKey());
    }

    @Test
    public void testReuseExistingKeySameInstance() throws FindException {
        TestSkm skm = new TestSkm(PASS);
        byte[] firstKey = skm.getSharedKey();
        assertIsDecentKey(firstKey);
        byte[] secondKey = skm.getSharedKey();
        assertTrue(Arrays.equals(firstKey, secondKey));
    }

    @Test
    public void testReuseExistingKeyDifferentInstance() throws FindException {
        TestSkm skm1 = new TestSkm(PASS);
        byte[] firstKey = skm1.getSharedKey();
        assertIsDecentKey(firstKey);

        TestSkm skm2 = new TestSkm(skm1);
        byte[] secondKey = skm2.getSharedKey();
        assertTrue(Arrays.equals(firstKey, secondKey));
    }

    @Test
    public void testCollisionOnSave() throws FindException {
        // Simulate a collision with another node that creates the key in between when we look for it and
        // when we save it
        TestSkm skm = new TestSkm(PASS) {
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
        TestSkm skm = new TestSkm(PASS);
        skm.saved = new SharedKeyRecord(TestSkm.CLUSTER_WIDE_IDENTIFIER, "$TOTALLYBOGUS$");
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

    private static class TestSkm extends SharedKeyManagerImpl {
        private String passphrase;
        private SharedKeyRecord saved;

        public TestSkm(char[] clusterPassphrase) {
            super(clusterPassphrase);
            this.passphrase = new String(clusterPassphrase);
        }

        public TestSkm(TestSkm template) {
            super(template.passphrase.toCharArray());
            copyFrom(template);
        }

        protected void saveSharedKeyRecord(SharedKeyRecord sharedKeyToSave) throws DataAccessException {
            if (saved != null)
                throw new ConstraintViolationException("a row with primary key value " + sharedKeyToSave.getEncodingID() + " already exists", null, null);
            saved = sharedKeyToSave;
        }

        protected Collection<SharedKeyRecord> selectSharedKeyRecordsByEncodingId() {
            return saved == null ? Collections.<SharedKeyRecord>emptyList() : Arrays.asList(saved);
        }

        public void copyFrom(TestSkm from) {
            this.passphrase = from.passphrase;
            this.saved = new SharedKeyRecord();
            this.saved.setEncodingID(from.saved.getEncodingID());
            this.saved.setB64edKey(from.saved.getB64edKey());
        }
    }
}
