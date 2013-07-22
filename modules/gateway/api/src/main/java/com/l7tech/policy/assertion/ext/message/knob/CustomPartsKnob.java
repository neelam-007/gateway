package com.l7tech.policy.assertion.ext.message.knob;

import com.l7tech.policy.assertion.ext.message.CustomContentType;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Simple custom message multipart extractor knob.
 * <p/>
 * This is an iterable interface optimized for lazily iterating through the message multipart parts.
 * The purpose of this knob is to extract multipart part's content type and content body input-stream.
 * <p/>
 * Note that the iterator always contains at least one element, even if the message is single-part,
 * in that case it will contain the single part.
 */
public interface CustomPartsKnob extends CustomMessageKnob, Iterable<CustomPartsKnob.Part> {

    /**
     * The custom message part info interface.
     * <p/>
     * Extracts multipart part's content-type and content body input-stream.
     */
    public interface Part {
        /**
         * Obtain this multipart part's content type.
         *
         * @return the custom message part's content type.
         */
        CustomContentType getContentType();

        /**
         * Obtain this custom message multipart part's content as an InputStream
         * <p/>
         * Depending whether message streaming is enabled or not, by using <tt>MessageBuffering</tt> assertion,
         * the returned input-stream might not be saved and subsequent calls to <code>getInputStream()</code> on the same
         * <code>Part</code> will fail with {@link IOException}.
         *
         * @return the custom message part input stream.
         * @throws IOException if this part's InputStream has already been destructively read (if message streaming is enabled) or
         *                     if there is a problem retrieving a stashed InputStream (if message streaming is disabled).
         */
        InputStream getInputStream() throws IOException;
    }

    /**
     * Retrieve the iterator optimized for lazily iterating the {@link Part} instances found in the message.
     *
     * @return iterator for lazily iterating through message multipart parts.
     * @throws NoSuchElementException can be thrown if:
     * <ul>
     *     <li>
     *         there are no more parts in the message.
     *         <p/>
     *         Note that this can occur even if {@link java.util.Iterator#hasNext()} returned true,
     *         but the original InputStream did not contain a properly terminated mime body.
     *     </li>
     *     <li>this part's InputStream has already been destructively read once, with message streaming enabled.</li>
     * </ul>
     * @throws RuntimeException actually an unchecked IOException, if there was a problem reading the message stream.
     */
    Iterator<Part> iterator() throws NoSuchElementException;
}
