package com.l7tech.common.io;

import org.junit.Test;
import static org.junit.Assert.*;
import sun.security.util.DerValue;

/**
 *
 */
public class X500DirectoryStringTest {

    @SuppressWarnings({ "UseOfSunClasses" })
    @Test
    public void testEncoding() throws Exception {
            byte[] tags = new byte[]{
                    DerValue.tag_T61String, // telex
                    DerValue.tag_PrintableString,
//                DerValue.tag_UniversalString,   //DerValue doesn't support UniversalString
                    DerValue.tag_UTF8String,
                    DerValue.tag_BMPString,
            };

            String[] testTexts = new String[]{
                    "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890",
                    "`~!@#$%^&*()--+={[}]|\\\"':;?/>.<,",
                    "   \r\n",
                    "\u00E3\u00E9",
                    "\u0B67\u0B68",
            };

            for ( byte tag : tags ) {
                for ( int i=0; i<testTexts.length; i++ ) {
                    if ( (tag == 19 && i > 2) ||  // Extended latin character tests not applicable for ASCII encoding
                         (tag == 20 && i > 3) ) { // Non latin character tests not applicable for IS0-8859-1 encoding
                        continue;
                    }
                    String text = testTexts[i];
                    DerValue value = new DerValue( tag, text );
                    String decodedText = new X500DirectoryString( value.toByteArray() ).getContents();
                    assertEquals( "round tripped text for tag " + tag, text, decodedText );
                }
            }
    }
}
