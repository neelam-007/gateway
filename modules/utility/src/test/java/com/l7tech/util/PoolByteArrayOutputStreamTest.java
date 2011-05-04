/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.util;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Unit tests for {@link com.l7tech.util.PoolByteArrayOutputStream}.
 */
public class PoolByteArrayOutputStreamTest {

    @Test
    public void testOutputStream() throws Exception {
        final String STR = "blah blah blah asdfasdfas ";
        StringBuffer sb = new StringBuffer();
        PoolByteArrayOutputStream bo = new PoolByteArrayOutputStream(4096);
        for (int i = 0; i < 3000; ++i) {
            bo.write(STR.getBytes());
            sb.append(STR);
        }
        assertEquals(bo.toString(), sb.toString());
    }

    @Test
    public void testWriteChars() throws IOException {
        PoolByteArrayOutputStream stream = new PoolByteArrayOutputStream(16);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 512; ++i)
            sb.append("More test data ").append(i).append(" string thing blah blah blah!\00 with nulls!").append(i).append("\n");
        String expect = sb.toString();

        for (byte b : expect.getBytes(Charsets.UTF8)) {
            stream.write((int)b);
        }

        byte[] bytes = stream.toByteArray();
        String got = new String(bytes, Charsets.UTF8);
        assertEquals(expect, got);
    }

    @Test(expected = IOException.class)
    public void testThrowIfClosed() throws IOException {
        PoolByteArrayOutputStream stream = new PoolByteArrayOutputStream(16);
        stream.write('b');
        stream.close();
        stream.write('b');
    }

    @Test
    public void testWrite() throws IOException {
            String testString = "Test string";
            String longString = "This is a really really really really long string.";
            PoolByteArrayOutputStream stream = new PoolByteArrayOutputStream();

            stream.write(testString.getBytes());

            assertEquals("Matches original", testString,stream.toString());

            stream.write(testString.getBytes(),5,4);
            assertEquals("Append part of original string", testString+testString.substring(5,9),stream.toString() );

            stream.reset();
            assertTrue("String empty after reset",stream.toString().isEmpty());
            assertTrue("buffer empty after rest",stream.size()==0);

            try{
                new PoolByteArrayOutputStream(-1);
                fail("Illegal argument not thrown");
            }catch(IllegalArgumentException ex){
                // Ok
            }

            stream = new PoolByteArrayOutputStream(10);
            stream.write(testString.getBytes());
            stream.write(longString.getBytes());
            assertEquals("Test expanding internal buffer",testString+longString,stream.toString() );


            String streamString = new String(stream.toByteArray());
            assertEquals("string lengths match", (testString+longString).length(), streamString.length());
            assertEquals("test to string after expanding internal buffer",testString+longString,streamString );

            for(int i = 0 ; i < 100; ++i)
                stream.write('?');
            assertEquals("Test append byte",testString.length()+longString.length()+100,stream.size() );
    }
}
