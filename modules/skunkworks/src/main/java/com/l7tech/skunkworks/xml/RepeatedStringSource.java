package com.l7tech.skunkworks.xml;

/**
 * Repeats a source a specified number of times.
 */
class RepeatedStringSource implements StringSource {
    private final StringSource source;
    private final long maxCount;

    private long count;

    public RepeatedStringSource(StringSource source, long count) {
        this.source = source;
        this.maxCount = count;

        this.count = maxCount;
    }

    public String next() {
        while (count > 0) {
            String ret = source.next();
            if (ret != null)
                return ret;
            count--;
            source.reset();
        }
        return null;
    }

    public void reset() {
        count = maxCount;
        source.reset();
    }
}
