package com.l7tech.kerberos;

import org.junit.Test;
import sun.security.util.DerValue;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

/**
 * Copyright: Layer 7 Technologies, 2012
 * User: ymoiseyenko
 */
public class GSSSpnegoTest {

    @Test
    public void shouldWrapUnwrapSpnegoToken() throws  Exception{
        byte[] token = {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0f};
        DerValue val = new DerValue((byte) 4, token);
        byte[] gssToken = GSSSpnego.addSpnegoWrapper(val.toByteArray());
        byte[] actualVal = GSSSpnego.removeSpnegoWrapper(gssToken);
        byte[] actualToken = new DerValue(actualVal).getDataBytes();
        assertArrayEquals(token, actualToken);
    }
}
