package com.l7tech.common.mime;

import java.io.IOException;

/**
 * An Iterator with types and checked exceptions optimized for lazily iterating the {@link PartInfo} instances found
 * by a {@link MultipartMessage}.
 */
public interface PartIterator {
    /**
     * Check if there are any more {@link PartInfo}s remaining to iterate.
     * @return true if there appear to be more parts remaining in the {@link MultipartMessage};
     *         false if additional parts have been completely ruled out.
     */
    public boolean hasNext();

    /**
     * @return the next {@link PartInfo} in the {@link MultipartMessage}.  Never null.
     * @throws NoSuchPartException if there are no more parts in this MultipartMessage.  Note that
     *                             this can occur even if {@link #hasNext}() returned true, if the
     *                             original InputStream did not contain a properly terminated MultipartMessage.
     *                             <p>
     *                             This differs from {@link java.util.Iterator}
     *
     * @throws IOException if there was a problem reading the message stream
     */
    public PartInfo next() throws IOException, NoSuchPartException;
}
