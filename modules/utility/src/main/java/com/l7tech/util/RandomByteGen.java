package com.l7tech.util;

import org.jetbrains.annotations.NotNull;

/**
 * A ByteGen that will generate random numbers obtained from RandomUtil.
 */
public class RandomByteGen implements ByteGen {
    @Override
    public void generateBytes( @NotNull byte[] array, int offset, int length ) {
        byte[] b = new byte[ length ];
        RandomUtil.nextBytes( b );
        System.arraycopy( b, 0, array, offset, length );
    }
}
