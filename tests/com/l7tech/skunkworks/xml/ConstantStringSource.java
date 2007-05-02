package com.l7tech.skunkworks.xml;

/**
 * A StringSource that returns a single String and then EOF.
 */
class ConstantStringSource implements StringSource {
    private final String init;
    private final long maxCount;
    private long count;

    /**
     * Create a StringSource that will generate the specified String followed by EOF.
     *
     * @param constantString the String to emit.  Must not be null.
     */
    public ConstantStringSource(String constantString) {
        init = constantString;
        maxCount = 1;
        count = maxCount;
    }

    /**
     * Create a StringSource that will generate the specified String the specified number of times (possibly infinite),
     * followed by EOF.
     *
     * @param constantString the String to emit.  Must not be null.
     * @param repetitions the number of times to repeat the string, or -1 to repeat it forever.
     */
    public ConstantStringSource(String constantString, long repetitions) {
        init = constantString;
        maxCount = repetitions;
        count = maxCount;
    }

    public String next() {
        if (maxCount < 0)
            return init;
        if (count < 1)
            return null;
        count--;
        return init;
    }

    public void reset() {
        count = maxCount;
    }
}
