/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.mime;

import com.l7tech.common.io.RandomInputStream;
import com.l7tech.util.HexUtils;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Random;
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

    private void stashAndRecall(StashManager sm, int num, int minSize, int maxSize) throws IOException, NoSuchPartException {
        stashAndRecall(sm, num, minSize, maxSize, sm instanceof ByteArrayStashManager);
    }

    /**
     * If the specified stashmanager is a HybridStashManager, ensure that getCurrentTotalSize() agrees with
     * the result of computeTotalSize().
     *
     * @param sm  the stash maanger to evaluate, or null to take no action.
     * @throws IllegalStateException if sm.getCurrentTotalSize() != computeTotalSize(sm)
     */
    private void checkSize(StashManager sm) {
        if (sm instanceof HybridStashManager) {
            HybridStashManager hsm = (HybridStashManager)sm;
            assertEquals(hsm.getCurrentTotalSize(), computeTotalSize(sm));
        }
    }

    private void stashAndRecall(StashManager sm, int num, int minSize, int maxSize, boolean recallBytesMustWork) throws IOException, NoSuchPartException {
        doStashAndRecall(sm, num, minSize, minSize, recallBytesMustWork);
        doStashAndRecall(sm, num, minSize, maxSize, recallBytesMustWork);
        doStashAndRecall(sm, num, maxSize, maxSize, recallBytesMustWork);
    }

    private long makeSize(Random rand, long min, long max) {
        if (min > Integer.MAX_VALUE || max > Integer.MAX_VALUE) throw new IllegalArgumentException();
        if (min == max) return min;
        return rand.nextInt((int)((int)(max - min) + min));
    }

    private void doStashAndRecall(StashManager sm, int num, int minSize, int maxSize, boolean recallBytesMustWork) throws IOException, NoSuchPartException {
        log.info("Stashandrecall: " + sm.getClass().getName() + ": " + num + " " + minSize + "-" + maxSize);
        final long seed = 1000 * 8000 + 9737 + num + minSize + maxSize + sm.hashCode();
        final Random rand = new Random(seed - 1);

        long[] lastSize = new long[num];
        long[] lastSeed = new long[num];
        for (int i = 0; i < num; ++i) {
            // test stash
            if (minSize > 40000)
                System.out.println("minSize = " + minSize);
            final long size = lastSize[i] = makeSize(rand, minSize, maxSize);
            final long mseed = lastSeed[i] = seed + i;
            sm.stash(i, new RandomInputStream(mseed, size));
            checkSize(sm);
        }

        for (int i = 0; i < num; ++i) {
            // test peek, getSize, and recall
            assertTrue(sm.peek(i));
            assertEquals(lastSize[i], sm.getSize(i));
            InputStream is = sm.recall(i);
            assertTrue(HexUtils.compareInputStreams(is, true, new RandomInputStream(lastSeed[i], lastSize[i]), true));
            checkSize(sm);
        }

        for (int i = 0; i < num; ++i) {
            assertTrue(sm.peek(i));
            assertEquals(lastSize[i], sm.getSize(i));


            final boolean byteArrayAvailable = sm.isByteArrayAvailable(i);
            if (recallBytesMustWork || byteArrayAvailable) {
                assertTrue(byteArrayAvailable);
                byte[] got = sm.recallBytes(i); // must not throw

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                HexUtils.copyStream(new RandomInputStream(lastSeed[i], lastSize[i]), out);
                assertTrue(Arrays.equals(out.toByteArray(), got));

            } else {
                try {
                    sm.recallBytes(i);
                    fail("Failed to throw on recallBytes() after isByteArrayAvailable() returned false");
                } catch (NoSuchPartException e) {
                    log.info("Caught expected exception on recallBytes after !isBytes: " + e.getMessage());
                }
            }
        }

        if (num > 0) {
            // Test replacement stash without unstash
            assertTrue(sm.peek(0));
            assertTrue(HexUtils.compareInputStreams(sm.recall(0), true, new RandomInputStream(lastSeed[0], lastSize[0]), true));
            checkSize(sm);
            lastSize[0] = makeSize(rand, minSize, maxSize);
            lastSeed[0] = seed + 333;
            sm.stash(0, new RandomInputStream(lastSeed[0], lastSize[0]));
            assertTrue(HexUtils.compareInputStreams(sm.recall(0), true, new RandomInputStream(lastSeed[0], lastSize[0]), true));
            checkSize(sm);
            assertTrue(sm.peek(0));
        }

        // Test replacing parts with different sizes, with and without unstash first
        if (num > 0 && minSize != maxSize && sm instanceof HybridStashManager) {
            // Do six passes of random size replacement
            for (int pass = 0; pass < 6; ++pass) {
                for (int i = 0; i < num; ++i) {
                    assertTrue(sm.peek(i));
                    assertTrue(HexUtils.compareInputStreams(sm.recall(i), true, new RandomInputStream(lastSeed[i], lastSize[i]), true));
                    checkSize(sm);
                    if (rand.nextBoolean())
                        sm.unstash(i);
                    lastSize[i] = makeSize(rand, minSize, maxSize);
                    lastSeed[i] = seed + i + 333;
                    sm.stash(i, new RandomInputStream(lastSeed[i], lastSize[i]));
                    assertTrue(HexUtils.compareInputStreams(sm.recall(i), true, new RandomInputStream(lastSeed[i], lastSize[i]), true));
                    checkSize(sm);
                    assertTrue(sm.peek(i));
                }
            }
        }

        if (num > 0) {
            // test unstash
            assertTrue(sm.peek(0));
            sm.unstash(0);
            lastSize[0] = -1;
            lastSeed[0] = -1;
            checkSize(sm);
            assertFalse(sm.peek(0));
        }

        sm.close();

        for (int i = 0; i < num; ++i)
            assertFalse(sm.peek(i));
    }

    private void doTestStashManager(boolean isLimitedByRam, StashManagerFactory factory) throws IOException, NoSuchPartException {
        // Tiny
        StashManager sm = factory.createNewStashManager();
        stashAndRecall(sm, 20, 1500, 3000);
        sm.close();

        // Small
        sm = factory.createNewStashManager();
        stashAndRecall(sm, 10, 5000, 15000);
        sm.close();

        // Medium
        sm = factory.createNewStashManager();
        stashAndRecall(sm, 4, 256000, 512000);
        sm.close();

        // Large
        sm = factory.createNewStashManager();
        stashAndRecall(sm, 1, 2500000, 5500000);
        sm.close();

        // Variable
        sm = factory.createNewStashManager();
        stashAndRecall(sm, 1, 500, 5500000);
        sm.close();

        if (!isLimitedByRam) {
            // Verify that it works
            sm = factory.createNewStashManager();
            stashAndRecall(sm, 1, 15 * 1024 * 1024, 30 * 1024 * 1024);
            sm.close();
        }
    }

    public void testByteArrayStashManager() throws Exception {
        // this is limited by RAM, so should not attempt to test the 30mb part
        doTestStashManager(true, new StashManagerFactory() {
            public StashManager createNewStashManager() {
                return new ByteArrayStashManager();
            }
        });
    }

    public void testFileStashManager() throws Exception {
        // this is not limited by ram, so go ahead and test the 30mb part
        doTestStashManager(false, new StashManagerFactory() {
            public StashManager createNewStashManager() throws IOException {
                return new FileStashManager(new File("."), "StashManagerTest" + unique++);
            }
        });
    }

    public void testHybridStashManager() throws Exception {
        // this is not limited by ram, so go ahead and test the 30mb part
        doTestStashManager(false, new StashManagerFactory() {
            public StashManager createNewStashManager() {
                return new HybridStashManager(100000, new File("."), "StashManagerTest" + unique++);
            }
        });
    }

    /**
     * Compute the total size of all elements in the specified stash manager.
     * This will call getSize() on all ordinals less than or equal to getMaxOrdinal() and return
     * the sum.
     *
     * @param stashManager  the stash manager to evaluate.  Must not be null.
     * @return the total size of all InputStreams currently stashed in this stash manager.
     */
    private long computeTotalSize(StashManager stashManager) {
        int max = stashManager.getMaxOrdinal();
        long totalSize = 0;
        for (int i = 0; i <= max; ++i) {
            long size = stashManager.getSize(i);
            if (size > 0) totalSize += size;
        }
        return totalSize;
    }

    private static long unique = 0;
}
