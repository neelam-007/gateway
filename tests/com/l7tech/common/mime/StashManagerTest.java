/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.mime;

import com.l7tech.common.io.RandomInputStream;
import com.l7tech.common.util.HexUtils;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

/**
 * @author mike
 */
public class StashManagerTest extends TestCase {
    private static Logger log = Logger.getLogger(StashManagerTest.class.getName());

    public StashManagerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(StashManagerTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    private static interface StashManagerFactory {
        StashManager createNewStashManager() throws IOException;
    }

    private void stashAndRecall(StashManager sm, int num, long size) throws IOException {
        final long seed = 1000 * 8000 + 9737 + num + size;

        for (int i = 0; i < num; ++i) {
            // test stash
            sm.stash(i, new RandomInputStream(seed + i, size));
        }

        for (int i = 0; i < num; ++i) {
            // test peek, getSize, and recall
            assertTrue(sm.peek(i));
            assertEquals(size, sm.getSize(i));
            InputStream is = sm.recall(i);
            assertTrue(HexUtils.compareInputStreams(is, true, new RandomInputStream(seed + i, size), true));
        }

        if (num > 0) {
            // Test replacement stash without unstash
            assertTrue(sm.peek(0));
            assertTrue(HexUtils.compareInputStreams(sm.recall(0), true, new RandomInputStream(seed + 0, size), true));
            sm.stash(0, new RandomInputStream(seed + 333, size));
            assertTrue(HexUtils.compareInputStreams(sm.recall(0), true, new RandomInputStream(seed + 333, size), true));
            assertTrue(sm.peek(0));
        }

        if (num > 0) {
            // test unstash
            assertTrue(sm.peek(0));
            sm.unstash(0);
            assertFalse(sm.peek(0));
        }

        sm.close();

        for (int i = 0; i < num; ++i)
            assertFalse(sm.peek(i));
    }

    private void doTestStashManager(boolean isLimitedByRam, StashManagerFactory factory) throws IOException {
        StashManager sm = factory.createNewStashManager();
        stashAndRecall(sm, 20, 2048);
        sm.close();

        sm = factory.createNewStashManager();
        stashAndRecall(sm, 10, 65536);
        sm.close();

        sm = factory.createNewStashManager();
        stashAndRecall(sm, 4, 512000);
        sm.close();

        sm = factory.createNewStashManager();
        stashAndRecall(sm, 1, 5500000);
        sm.close();

        if (!isLimitedByRam) {
            // Verify that it works
            sm = factory.createNewStashManager();
            stashAndRecall(sm, 1, 30 * 1024 * 1024);
            sm.close();
        }
    }

    public void testByteArrayStashManager() throws IOException {
        // this is limited by RAM, so should not attempt to test the 30mb part
        doTestStashManager(true, new StashManagerFactory() {
            public StashManager createNewStashManager() {
                return new ByteArrayStashManager();
            }
        });
    }

    public void testFileStashManager() throws IOException {
        // this is not limited by ram, so go ahead and test the 30mb part
        doTestStashManager(false, new StashManagerFactory() {
            public StashManager createNewStashManager() throws IOException {
                return new FileStashManager(new File("."), "StashManagerTest" + unique++);
            }
        });
    }

    public void testHybridStashManager() throws IOException {
        // this is not limited by ram, so go ahead and test the 30mb part
        doTestStashManager(false, new StashManagerFactory() {
            public StashManager createNewStashManager() {
                return new HybridStashManager(100000, new File("."), "StashManagerTest" + unique++);
            }
        });
    }

    private static long unique = 0;
}
