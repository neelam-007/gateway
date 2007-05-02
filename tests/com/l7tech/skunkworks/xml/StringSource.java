package com.l7tech.skunkworks.xml;

/**
 * A source of String instances.
 */
interface StringSource {
    /** @return the next String, or null if we have reached the end. */
    String next();

    /**
     * Reset the source, so the next call to next() returns what the very first call would have returned.
     */
    void reset();
}
