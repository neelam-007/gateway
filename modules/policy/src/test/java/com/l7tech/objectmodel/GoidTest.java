package com.l7tech.objectmodel;

import org.junit.Assert;
import org.junit.Test;

import java.util.Random;

/**
 * This was created: 6/24/13 as 6:02 PM
 *
 * @author Victor Kazakov
 */
public class GoidTest {
    private static final int NUM_TESTS = 10;

    @Test
    public void goidFromLong() {
        for (int i = 0; i < NUM_TESTS; i++) {
            Random random = new Random();
            long hi = random.nextLong();
            long low = random.nextLong();
            Goid goid = new Goid(hi, low);

            Assert.assertEquals(hi, goid.getHi());
            Assert.assertEquals(low, goid.getLow());

            String goidString = goid.toString();

            Goid goidFromString = new Goid(goidString);

            Assert.assertEquals(hi, goidFromString.getHi());
            Assert.assertEquals(low, goidFromString.getLow());
        }
    }

    @Test
    public void goidFromByteArray() {
        for (int i = 0; i < NUM_TESTS; i++) {
            Random random = new Random();
            byte[] bytes = new byte[16];
            random.nextBytes(bytes);

            Goid goid = new Goid(bytes);
            Assert.assertArrayEquals(bytes, goid.getBytes());

            String goidString = goid.toString();
            Goid goidFromString = new Goid(goidString);

            Assert.assertArrayEquals(bytes, goidFromString.getBytes());
        }
    }
}
