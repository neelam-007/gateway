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
 * @author mike
 */
public class BufferPoolByteArrayOutputStreamTest {

    @Test
    public void testOutputStream() throws Exception {
        final String STR = "blah blah blah asdfasdfas ";
        StringBuffer sb = new StringBuffer();
        BufferPoolByteArrayOutputStream bo = new BufferPoolByteArrayOutputStream(4096);
        for (int i = 0; i < 3000; ++i) {
            bo.write(STR.getBytes());
            sb.append(STR);
        }
        assertEquals(bo.toString(), sb.toString());
    }

    @Test
    public void testWrite() throws IOException {
            String testString = "Test string";
            String longString = "This is a really really really really long string.";
            BufferPoolByteArrayOutputStream stream = new BufferPoolByteArrayOutputStream();

            stream.write(testString.getBytes());

            assertEquals("Matches original", testString,stream.toString());

            stream.write(testString.getBytes(),5,4);
            assertEquals("Append part of original string", testString+testString.substring(5,9),stream.toString() );

            stream.reset();
            assertTrue("String empty after reset",stream.toString().isEmpty());
            assertTrue("buffer empty after rest",stream.size()==0);

            try{
                new BufferPoolByteArrayOutputStream(-1);
                fail("Illegal argument not thrown");
            }catch(IllegalArgumentException ex){
                // Ok
            }

            stream = new BufferPoolByteArrayOutputStream(10);
            stream.write(testString.getBytes());
            stream.write(longString.getBytes());
            assertEquals("Test expanding internal buffer",testString+longString,stream.toString() );


            String streamString = new String(stream.toByteArray());
            System.out.println("expected length: " + (testString+longString).length());
            assertEquals("string lengths match", (testString+longString).length(), streamString.length());
            assertEquals("test to string after expanding internal buffer",testString+longString,streamString );

            for(int i = 0 ; i < 100; ++i)
                stream.write('?');
            stream.writeTo(System.out);
            assertEquals("Test append byte",testString.length()+longString.length()+100,stream.size() );
    }
}
