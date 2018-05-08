package com.l7tech.objectmodel;

import java.util.Random;

/**
 * Helper class for unit tests.
 */
public final class GoidUtils {
    private GoidUtils(){}

    public static final Goid randomGoid() {
        final byte[] bytes = new byte[16];
        new Random().nextBytes(bytes);
        return new Goid(bytes);
    }
}
