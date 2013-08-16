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
    public void blah(){
        System.out.println(new Goid(123,456));
    }


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
    public void fromBadString() {
        new Goid("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void fromBadString2() {
        new Goid("A8HZQvUlgDlyjF83TPJetzz");
    }

    @Test(expected = IllegalArgumentException.class)
    public void fromBadByteArray() {
        Random random = new Random();
        byte[] bytes = new byte[17];
        random.nextBytes(bytes);
        new Goid(bytes);
    }

    @Test(expected = IllegalArgumentException.class)
    public void fromBadByteArray2() {
        Random random = new Random();
        byte[] bytes = new byte[15];
        random.nextBytes(bytes);
        new Goid(bytes);
    }

    @Test(expected = IllegalArgumentException.class)
    public void fromBadByteArray3() {
        Random random = new Random();
        byte[] bytes = new byte[0];
        random.nextBytes(bytes);
        new Goid(bytes);
    }

    @Test
    public void cloneTest() {
        Goid original = createRandomGoid();
        Goid clone = original.clone();

        Assert.assertFalse(original == clone);
        Assert.assertEquals(original, clone);
        Assert.assertArrayEquals(original.getBytes(), clone.getBytes());
    }

    @Test
    public void compareTest() {
        Goid goid;
        do {
            goid = createRandomGoid();
        }
        while (goid.getHi() == Long.MAX_VALUE || goid.getHi() == Long.MIN_VALUE || goid.getLow() == Long.MAX_VALUE || goid.getLow() == Long.MIN_VALUE);
        Goid clone = goid.clone();

        Assert.assertEquals("The compare to method should return 0 if both goings are equal", 0, goid.compareTo(goid));
        Assert.assertEquals("The compare to method should return 0 if both goings are equal", 0, goid.compareTo(clone));
        Assert.assertEquals("The compare to method should return 0 if both goings are equal", 0, clone.compareTo(goid));

        Goid biggerHiGoid = new Goid(goid.getHi() + 1, goid.getLow());
        Goid biggerLowGoid = new Goid(goid.getHi(), goid.getLow() + 1);
        Goid smallerHiGoid = new Goid(goid.getHi() - 1, goid.getLow());
        Goid smallerLowGoid = new Goid(goid.getHi(), goid.getLow() - 1);

        Assert.assertEquals(-1, goid.compareTo(biggerHiGoid));
        Assert.assertEquals(-1, goid.compareTo(biggerLowGoid));
        Assert.assertEquals(1, goid.compareTo(smallerHiGoid));
        Assert.assertEquals(1, goid.compareTo(smallerLowGoid));

        Assert.assertEquals(-1, smallerHiGoid.compareTo(goid));
        Assert.assertEquals(-1, smallerLowGoid.compareTo(goid));
        Assert.assertEquals(1, biggerHiGoid.compareTo(goid));
        Assert.assertEquals(1, biggerLowGoid.compareTo(goid));
    }

    @Test
    public void equalsTest() {
        Goid goid;
        do {
            goid = createRandomGoid();
        }
        while (goid.getHi() == Long.MAX_VALUE || goid.getHi() == Long.MIN_VALUE || goid.getLow() == Long.MAX_VALUE || goid.getLow() == Long.MIN_VALUE);
        Goid clone = goid.clone();

        Assert.assertTrue(goid.equals(goid));
        Assert.assertTrue(goid.equals(clone));
        Assert.assertTrue(clone.equals(goid));

        Goid biggerHiGoid = new Goid(goid.getHi() + 1, goid.getLow());
        Goid biggerLowGoid = new Goid(goid.getHi(), goid.getLow() + 1);
        Goid smallerHiGoid = new Goid(goid.getHi() - 1, goid.getLow());
        Goid smallerLowGoid = new Goid(goid.getHi(), goid.getLow() - 1);

        Assert.assertFalse(goid.equals(biggerHiGoid));
        Assert.assertFalse(goid.equals(biggerLowGoid));
        Assert.assertFalse(goid.equals(smallerHiGoid));
        Assert.assertFalse(goid.equals(smallerLowGoid));

        Assert.assertFalse(smallerHiGoid.equals(goid));
        Assert.assertFalse(smallerLowGoid.equals(goid));
        Assert.assertFalse(biggerHiGoid.equals(goid));
        Assert.assertFalse(biggerLowGoid.equals(goid));

        Assert.assertFalse(goid.equals(null));

        Assert.assertFalse(goid.equals(goid.toHexString()));
    }

    @Test
    public void staticEqualsTest() {
        Goid goid;
        do {
            goid = createRandomGoid();
        }
        while (goid.getHi() == Long.MAX_VALUE || goid.getHi() == Long.MIN_VALUE || goid.getLow() == Long.MAX_VALUE || goid.getLow() == Long.MIN_VALUE);
        Goid clone = goid.clone();

        Assert.assertTrue(Goid.equals(goid, goid));
        Assert.assertTrue(Goid.equals(goid, clone));
        Assert.assertTrue(Goid.equals(clone, goid));

        Goid biggerHiGoid = new Goid(goid.getHi() + 1, goid.getLow());
        Goid biggerLowGoid = new Goid(goid.getHi(), goid.getLow() + 1);
        Goid smallerHiGoid = new Goid(goid.getHi() - 1, goid.getLow());
        Goid smallerLowGoid = new Goid(goid.getHi(), goid.getLow() - 1);

        Assert.assertFalse(Goid.equals(goid, biggerHiGoid));
        Assert.assertFalse(Goid.equals(goid, biggerLowGoid));
        Assert.assertFalse(Goid.equals(goid, smallerHiGoid));
        Assert.assertFalse(Goid.equals(goid, smallerLowGoid));

        Assert.assertFalse(Goid.equals(smallerHiGoid, goid));
        Assert.assertFalse(Goid.equals(smallerLowGoid, goid));
        Assert.assertFalse(Goid.equals(biggerHiGoid, goid));
        Assert.assertFalse(Goid.equals(biggerLowGoid, goid));

        Assert.assertFalse(Goid.equals(goid, null));
        Assert.assertFalse(Goid.equals(null, goid));
        Assert.assertTrue(Goid.equals(null, null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullStringTest(){
        String nullString = null;
        new Goid(nullString);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullGoidTest(){
        Goid nullGoid = null;
        new Goid(nullGoid);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullParseGoidTest(){
        String nullString = null;
        Goid.parseGoid(nullString);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullToStringTest(){
        Goid nullGoid = null;
        Goid.toString(nullGoid);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullCompareToTest(){
        Goid goid = createRandomGoid();
        Goid nullGoid = null;
        goid.compareTo(nullGoid);
    }

    @Test
    public void lowercaseGoidTest() {
        for (int i = 0; i < NUM_TESTS; i++) {
            Goid goid = createRandomGoid();
            Assert.assertEquals("The goid string format should contain only lowercase characters.", goid.toHexString().toLowerCase(), goid.toHexString());
            Assert.assertEquals("The goid string format should contain only lowercase characters.", goid.toString().toLowerCase(), goid.toString());
            Assert.assertEquals("The goid string format should contain only lowercase characters.", Goid.toString(goid).toLowerCase(), Goid.toString(goid));
        }
    }

    private static Goid createRandomGoid() {
        Random random = new Random();
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        return new Goid(bytes);
    }
}
