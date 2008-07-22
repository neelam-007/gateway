package com.l7tech.skunkworks.xml;

/**
 * Zips two StringSource together, alternating their output.  Ends as soon as either one ends.
 */
class ZipStringSource implements StringSource {
    private final StringSource left;
    private final StringSource right;
    private boolean nextLeft = true;
    private boolean done = false;

    public ZipStringSource(StringSource left, StringSource right) {
        this.left = left;
        this.right = right;
    }

    public String next() {
        if (done)
            return null;

        String ret = (nextLeft ? left : right).next();
        nextLeft = !nextLeft;
        if (ret == null)
            done = true;

        return ret;
    }

    public void reset() {
        left.reset();
        right.reset();
        nextLeft = true;
        done = false;
    }
}
