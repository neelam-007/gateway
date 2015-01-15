package com.l7tech.util;

import org.jetbrains.annotations.NotNull;

/**
 * Interface implemented by an object that can generate a requested number of bytes of some kind.
 */
public interface ByteGen {

    /**
     * Generate length bytes into the specified array at the specified offset.
     *
     * @param array     array to generate into.  Required.
     * @param offset    position of first byte.
     * @param length    number of bytes to generate.  Must be nonnegative.  May be zero.  Must not exceed array length - offset.
     * @throws java.lang.ArrayIndexOutOfBoundsException if offset + length >= array.length
     */
    void generateBytes( @NotNull byte[] array, int offset, int length );
}
