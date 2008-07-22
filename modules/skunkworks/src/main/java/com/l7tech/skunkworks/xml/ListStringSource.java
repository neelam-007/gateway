package com.l7tech.skunkworks.xml;

import java.util.LinkedList;
import java.util.Queue;

/**
 * A StringSource that flattens a list.
 */
class ListStringSource implements StringSource {
    private final Queue<StringSource> init;
    private Queue<StringSource> sources;
    private StringSource curSource;

    /**
     * Create a StringSource that will return the concatenation of the specified Objects as Strings (or StringSources),
     * and then EOF.
     *
     * @param constantString an array of objects to fold together.  May be empty, but must not be null and must not
     *                       contain nulls.  Objects that are instances of StringSource will be recursively expanded.
     *                       Other objects will have their toString() returned.
     */
    public ListStringSource(Object... constantString) {
        LinkedList<StringSource> sources = new LinkedList<StringSource>();

        for (Object source : constantString)
            sources.add(source instanceof StringSource
                        ? ((StringSource)source)
                        : new ConstantStringSource(source.toString()));

        this.init = sources;
        this.sources = new LinkedList<StringSource>(init);
        curSource = this.sources.remove();
    }

    public String next() {
        while (curSource != null) {
            String ret = curSource.next();
            if (ret != null)
                return ret;
            curSource = sources.isEmpty() ? null : sources.remove();
        }
        return null;
    }

    public void reset() throws UnsupportedOperationException {
        this.sources = new LinkedList<StringSource>(init);
        for (StringSource source : sources)
            source.reset();
        curSource = sources.remove();
    }
}
