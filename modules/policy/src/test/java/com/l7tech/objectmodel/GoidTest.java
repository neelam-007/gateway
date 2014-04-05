package com.l7tech.objectmodel;

import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;
import java.util.Random;

import static org.junit.Assert.assertEquals;

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

            assertEquals( hi, goid.getHi() );
            assertEquals( low, goid.getLow() );

            String goidString = goid.toString();

            Goid goidFromString = new Goid(goidString);

            assertEquals( hi, goidFromString.getHi() );
            assertEquals( low, goidFromString.getLow() );
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
        assertEquals( original, clone );
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

        assertEquals( "The compare to method should return 0 if both goings are equal", 0, goid.compareTo( goid ) );
        assertEquals( "The compare to method should return 0 if both goings are equal", 0, goid.compareTo( clone ) );
        assertEquals( "The compare to method should return 0 if both goings are equal", 0, clone.compareTo( goid ) );

        Goid biggerHiGoid = new Goid(goid.getHi() + 1, goid.getLow());
        Goid biggerLowGoid = new Goid(goid.getHi(), goid.getLow() + 1);
        Goid smallerHiGoid = new Goid(goid.getHi() - 1, goid.getLow());
        Goid smallerLowGoid = new Goid(goid.getHi(), goid.getLow() - 1);

        assertEquals( -1, goid.compareTo( biggerHiGoid ) );
        assertEquals( -1, goid.compareTo( biggerLowGoid ) );
        assertEquals( 1, goid.compareTo( smallerHiGoid ) );
        assertEquals( 1, goid.compareTo( smallerLowGoid ) );

        assertEquals( -1, smallerHiGoid.compareTo( goid ) );
        assertEquals( -1, smallerLowGoid.compareTo( goid ) );
        assertEquals( 1, biggerHiGoid.compareTo( goid ) );
        assertEquals( 1, biggerLowGoid.compareTo( goid ) );
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
            assertEquals( "The goid string format should contain only lowercase characters.", goid.toHexString().toLowerCase(), goid.toHexString() );
            assertEquals( "The goid string format should contain only lowercase characters.", goid.toString().toLowerCase(), goid.toString() );
            assertEquals( "The goid string format should contain only lowercase characters.", Goid.toString( goid ).toLowerCase(), Goid.toString( goid ) );
        }
    }

    @Test
    public void testGoidLongIntegerByteOrder() {
        Goid goid = new Goid("8db55bbdc8b6f5951f10faf1386979fe");
        assertEquals( "724aa44237490a6b", BigInteger.valueOf( goid.getHi() ).abs().toString( 16 ) );
        assertEquals( "1f10faf1386979fe", BigInteger.valueOf( goid.getLow() ).abs().toString( 16 ) );
    }

    @Test
    public void sanityTestForDefaultGoid(){
        assertEquals( "You've changed the default Goid to something else!!!! Are you sure you want to do that?", new Goid( 0, -1 ), Goid.DEFAULT_GOID );
        assertEquals( "You've changed the default Goid to something else!!!! Are you sure you want to do that?", new Goid( 0, -1 ), PersistentEntity.DEFAULT_GOID );
    }

    @Test
    public void testToCompressedString() {
        assertEquals( "Uncompressable Goid shan't be compressed", "8db55bbdc8b6f5951f10faf1386979fe", new Goid( "8db55bbdc8b6f5951f10faf1386979fe" ).toCompressedString() );
        assertEquals( "Not enough leading ones to be worth it are left alone", "fdb55bbdc8b6f5951f10faf1386979fe", new Goid( "fdb55bbdc8b6f5951f10faf1386979fe" ).toCompressedString() );
        assertEquals( "Not enough leading zeroes to be worth it are left alone", "0db55bbdc8b6f5951f10faf1386979fe", new Goid( "0db55bbdc8b6f5951f10faf1386979fe" ).toCompressedString() );
        assertEquals( "nb55bbdc8b6f5951f10faf1386979fe", new Goid( "ffb55bbdc8b6f5951f10faf1386979fe" ).toCompressedString() );
        assertEquals( "nn", new Goid( "ffffffffffffffffffffffffffffffff" ).toCompressedString() );
        assertEquals( "zz", new Goid( "00000000000000000000000000000000" ).toCompressedString() );
        assertEquals( "zn", new Goid( "0000000000000000ffffffffffffffff" ).toCompressedString() );
        assertEquals( "nz", new Goid( "ffffffffffffffff0000000000000000" ).toCompressedString() );
        assertEquals( "zz1", new Goid( "00000000000000000000000000000001" ).toCompressedString() );
        assertEquals( "zz746", new Goid( "00000000000000000000000000000746" ).toCompressedString() );
        assertEquals( "nz746", new Goid( "ffffffffffffffff0000000000000746" ).toCompressedString() );
        assertEquals( "nedz746", new Goid( "ffffffffffffffed0000000000000746" ).toCompressedString() );
        assertEquals( "z746ned", new Goid( "0000000000000746ffffffffffffffed" ).toCompressedString() );
        assertEquals( "z746zfff1", new Goid( "0000000000000746000000000000fff1" ).toCompressedString() );
        assertEquals( "zne", new Goid( "0000000000000000fffffffffffffffe" ).toCompressedString() );
    }

    @Test
    public void testFromCompressedString() {
        assertEquals( "Uncompressable Goid is Ok", new Goid( "8db55bbdc8b6f5951f10faf1386979fe" ), new Goid( "8db55bbdc8b6f5951f10faf1386979fe" ) );
        assertEquals( new Goid( "00000000000000000000000000000000" ), new Goid( "zz" ) );
        assertEquals( new Goid( "ffffffffffffffffffffffffffffffff" ), new Goid( "nn" ) );
        assertEquals( new Goid( "0000000000000000ffffffffffffffff" ), new Goid( "zn" ) );
        assertEquals( Goid.DEFAULT_GOID, new Goid( "zn" ) );
        assertEquals( new Goid( "ffffffffffffffff0000000000000000" ), new Goid( "nz" ) );
        assertEquals( new Goid( "00000000000000000000000000000001" ), new Goid( "zz1" ) );
        assertEquals( new Goid( "00000000000000000000000000000746" ), new Goid( "zz746" ) );
        assertEquals( new Goid( "ffffffffffffffff0000000000000746" ), new Goid( "nz746" ) );
        assertEquals( new Goid( "ffffffffffffffed0000000000000746" ), new Goid( "nedz746" ) );
        assertEquals( new Goid( "0000000000000746ffffffffffffffed" ), new Goid( "z746ned" ) );
        assertEquals( new Goid( "0000000000000746000000000000fff1" ), new Goid( "z746zfff1" ) );
        assertEquals( new Goid( "0000000000000000fffffffffffffffe" ), new Goid( "zne" ) );
    }

    @Test( expected = IllegalArgumentException.class )
    public void testFromCompressedStringNotTrimmed() {
        new Goid( " zz " );
    }

    @Test( expected = IllegalArgumentException.class )
    public void testFromCompressedStringBadCharacter() {
        new Goid( "z746zfgf1" );
    }

    private static Goid createRandomGoid() {
        Random random = new Random();
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        return new Goid(bytes);
    }
}
