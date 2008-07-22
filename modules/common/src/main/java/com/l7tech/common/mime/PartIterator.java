package com.l7tech.common.mime;

import com.l7tech.common.io.UncheckedIOException;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * An Iterator with types and checked exceptions optimized for lazily iterating the {@link PartInfo} instances found
 * by a {@link MimeBody}.
 */
public interface PartIterator extends Iterator<PartInfo> {
    /**
     * Check if there are any more {@link PartInfo}s remaining to iterate.
     * @return true if there appear to be more parts remaining in the {@link MimeBody};
     *         false if additional parts have been completely ruled out.
     * @throws UncheckedIOException if there was a problem reading the message stream
     */
    public boolean hasNext() throws UncheckedIOException;

    /**
     * @return the next {@link PartInfo} in the {@link MimeBody}.  Never null.
     * @throws NoSuchElementException if there are no more parts in this MimeBody.  Note that
     *                             this can occur even if {@link #hasNext}() returned true, if the
     *                             original InputStream did not contain a properly terminated MimeBody.
     *                             <p>
     *                             This differs from {@link java.util.Iterator}
     *
     * @throws UncheckedIOException if there was a problem reading the message stream
     */
    public PartInfo next() throws UncheckedIOException, NoSuchElementException;
}
