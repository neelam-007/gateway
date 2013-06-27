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
        Random random = new Random();
        for (int i = 0; i < NUM_TESTS; i++) {
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
        Random random = new Random();
        for (int i = 0; i < NUM_TESTS; i++) {
            byte[] bytes = new byte[16];
            random.nextBytes(bytes);

            Goid goid = new Goid(bytes);
            Assert.assertArrayEquals(bytes, goid.getBytes());

            String goidString = goid.toString();
            Goid goidFromString = new Goid(goidString);

            Assert.assertArrayEquals(bytes, goidFromString.getBytes());
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void fromBadString(){
        new Goid("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void fromBadString2(){
        new Goid("A8HZQvUlgDlyjF83TPJetzz");
    }

    @Test(expected = IllegalArgumentException.class)
    public void fromBadByteArray(){
        Random random = new Random();
        byte[] bytes = new byte[17];
        random.nextBytes(bytes);
        new Goid(bytes);
    }

    @Test(expected = IllegalArgumentException.class)
    public void fromBadByteArray2(){
        Random random = new Random();
        byte[] bytes = new byte[15];
        random.nextBytes(bytes);
        new Goid(bytes);
    }

    @Test(expected = IllegalArgumentException.class)
    public void fromBadByteArray3(){
        Random random = new Random();
        byte[] bytes = new byte[0];
        random.nextBytes(bytes);
        new Goid(bytes);
    }

    @Test
    public void cloneTest(){
        Random random = new Random();
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        Goid original = new Goid(bytes);
        Goid clone = original.clone();

        Assert.assertFalse(original == clone);
        Assert.assertEquals(original, clone);
        Assert.assertArrayEquals(original.getBytes(), clone.getBytes());
    }
}
