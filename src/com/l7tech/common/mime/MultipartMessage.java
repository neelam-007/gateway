/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.mime;

import com.l7tech.common.io.EmptyInputStream;
import com.l7tech.common.io.IOExceptionThrowingInputStream;
import com.l7tech.common.io.NullOutputStream;
import com.l7tech.common.util.HexUtils;

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Encapsulates the body of a message that might be multipart/related.  Can be used even for messages that are not
 * multipart -- in such cases, it pretends that there was a multipart message with only a single part.
 * <p>
 * The general contract of this class is that you give it ownership of an InputStream that may or may not contain
 * multipart parts, and it gives you a view of one or more PartInfo instances, each of which can be used to retrieve
 * metadata and an InputStream.  The first PartInfo can be completely changed, but subsequent PartInfos are read-only.
 * <p>
 * None of the methods in this class are guaranteed to be reentrant.  Care should be taken when providing a custom
 * InputStream implementation not to call MultipartMessage methods from inside it, when it is itself being called
 * from MultipartMessage.
 */
public class MultipartMessage {
    private static final Logger logger = Logger.getLogger(MultipartMessage.class.getName());
    private static final int BLOCKSIZE = 4096;

    private final PushbackInputStream mainInputStream;
    private final int pushbackSize;
    private final StashManager stashManager;
    private final ContentTypeHeader outerContentType;

    private final List partInfos = new ArrayList(); // our PartInfo instances.
    private final PartInfo firstPart; // equivalent to (PartInfo)partInfos.get(0)
    private final Map partInfosByCid = new HashMap(); // our PartInfo-by-cid lookup.

    private final String boundaryStr; // multpart boundary not including initial dashses or any CRLFs; or null if singlepart
    private final byte[] boundary; // multipart crlfBoundary bytes including initial dashes but not including trailing CRLF; or null if singlepart.
    private final byte[] boundaryScanbuf; // a buffer exactly crlfBoundary.length bytes long, if multipart; or null if singlepart.

    private boolean moreParts = true; // assume there are more parts until we find the end of the stream

    private MultipartMessage(StashManager stashManager,
                             ContentTypeHeader outerContentType,
                             InputStream mainInputStream)
            throws IOException, NoSuchPartException
    {
        if (stashManager == null || outerContentType == null || mainInputStream == null)
                throw new IllegalArgumentException("stashManager, outerContentType, and mainInputStream must all be provided");

        this.outerContentType = outerContentType;
        this.stashManager = stashManager;

        if (outerContentType.isMultipart()) {
            // Multipart message.  Prepare the first part for reading.
            boundaryStr = outerContentType.getMultipartBoundary();
            boundary = ("--" + boundaryStr).getBytes(MimeHeader.ENCODING);
            if (boundary.length > BLOCKSIZE)
                throw new IOException("This multipart message cannot be processed because it uses a multipart crlfBoundary which is more than 4kb in length");
            boundaryScanbuf = new byte[boundary.length];
            pushbackSize = BLOCKSIZE + boundaryScanbuf.length;
            this.mainInputStream = new PushbackInputStream(mainInputStream, pushbackSize);
            readInitialBoundary();
            readNextPartHeaders();
            firstPart = (PartInfo)partInfos.get(0);
        } else {
            // Single-part message.  Configure first and only part accordingly.
            boundaryStr = null;
            boundary = null;
            boundaryScanbuf = null;
            pushbackSize = BLOCKSIZE;
            this.mainInputStream = new PushbackInputStream(mainInputStream, pushbackSize);
            final MimeHeaders outerHeaders = new MimeHeaders();
            outerHeaders.add(outerContentType);
            // TODO refactor this to share more code with PartInfoImpl
            final PartInfoImpl mainPartInfo = new PartInfoImpl(0, outerHeaders) {
                            public byte[] getContent() throws IOException {
                                throw new UnsupportedOperationException("Not yet implemented for singlepart");
                            }

                            public InputStream getInputStream(boolean destroyAsRead) throws IOException {
                                // See if we have one stashed already
                                InputStream is = MultipartMessage.this.stashManager.recall(ordinal);
                                if (is != null)
                                    return is;

                                // Guarantee: if this PartInfo exists, its headers have been read.
                                // So, either our mainInputStream is already positioned right at this Part's body content,
                                // or someone has already destructively read this Part's body content.

                                // Are we the current Part?
                                if (partInfos.size() != ordinal + 1 || !moreParts)
                                    throw new IOException("This MIME multipart part's body content has already been destructively read.");

                                is = MultipartMessage.this.mainInputStream;
                                moreParts = false;

                                // We are ready to return an InputStream.  Do we need to stash the data first?
                                if (destroyAsRead) {
                                    // No -- allow caller to consume it.
                                    return is;
                                }

                                // Yes -- stash it first, then recall it.
                                MultipartMessage.this.stashManager.stash(ordinal, is);
                                is = MultipartMessage.this.stashManager.recall(ordinal);
                                if (is == null)
                                    throw new IllegalStateException("Stash succeeds but recall fails"); // StashManager contract violation
                                return is;

                            }
                        };
            partInfos.add(mainPartInfo);
            firstPart = mainPartInfo;

            final String mainContentId = mainPartInfo.getContentId();
            if (mainContentId != null)
                partInfosByCid.put(mainContentId, mainPartInfo);
        }

    }

    /**
     * Consume the headers of the next part, and store a PartInfo.  The main InputStream must be positioned just
     * beyond the boundary, at the first byte of the next part's headers.
     *
     * @throws IOException if the headers could not be read
     */
    private void readNextPartHeaders() throws IOException, NoSuchPartException {
        if (boundary == null) throw new IllegalStateException("Not supported in single-part mode");
        if (!moreParts)
            throw new NoSuchPartException("Out of parts");
        // Consume the headers of the first part.
        MimeHeaders headers = MimeUtil.parseHeaders(mainInputStream);
        final PartInfoImpl partInfo = new PartInfoImpl(partInfos.size(), headers);
        partInfos.add(partInfo);
        final String cid = partInfo.getContentId();
        if (cid != null)
            partInfosByCid.put(cid, partInfo);

    }

    /**
     * Consume the preamble and initial multipart boundary.  When this returns, input stream will be positioned at
     * the first byte of the first multipart part.
     *
     * @throws IOException if the mainInputStream could not be ready, or was not pointing at exactly
     *                     boundaryScanbuf.length bytes that matched crlfBoundary
     */
    private void readInitialBoundary() throws IOException {
        if (boundary == null)
            throw new IllegalStateException("readInitialBoundary does not work on single-part messages");
        mainInputStream.unread("\r\n".getBytes()); // Fix problem reading initial boundary when there's no preamble
        MimeBoundaryTerminatedInputStream preamble = new MimeBoundaryTerminatedInputStream(boundary, mainInputStream, pushbackSize);
        NullOutputStream nowhere = new NullOutputStream();
        HexUtils.copyStream(preamble, nowhere);
        if (preamble.isLastPartProcessed()) {
            moreParts = false;  // just in case
            throw new IOException("Multipart message had zero parts");
        }
    }

    /**
     * Create a new MultipartMessage instance that will read from the specified mainInputStream, treating the content
     * as the specified outerContentType.  After this call
     *
     * @param stashManager the StashManager to use.  Must not be null.  See {@link ByteArrayStashManager} for an example.
     * @param outerContentType the content-type of the outer stream.  May not be null.
     *                         Use {@link MimeHeaders#DEFAULT_CONTENT_TYPE} if you have absolutely no clue.
     * @param mainInputStream  the primary InputStream.  May not be null.  Must be positioned to the first byte
     *                         of the body content, regardless of whether or not the body is multipart.
     *                         If a MultipartMessage is successfully created, it takes ownership of the mainInputStream.
     * @return a new MultipartMessage ready to provide access to the content of any MIME parts.
     * @throws NoSuchPartException if this message is multpart/related but does not have any parts
     * @throws IOException if the mainInputStream cannot be read
     */
    public static MultipartMessage createMultipartMessage(StashManager stashManager,
                                                          ContentTypeHeader outerContentType,
                                                          InputStream mainInputStream)
            throws IOException, NoSuchPartException
    {
        return new MultipartMessage(stashManager, outerContentType, mainInputStream);
    }

    /**
     * Get the specified PartInfo from this message by ordinal position, with the first part as part #0.
     * Note that getting the parts out of order is supported but will involve reading and stashing the
     * intervening parts in the current StashManager.
     *
     * @param ordinal   the ordinal number of the part within this multipart message, where the first part is Part #0
     * @return the PartInfo describing the specified MIME part.  Never null.
     * @throws NoSuchPartException if the specified part does not exist in this message
     * @throws IOException if there was a problem reading the message stream
     */
    public PartInfo getPart(int ordinal) throws IOException, NoSuchPartException {
        if (ordinal < 0)
            throw new IllegalArgumentException("ordinal must be non-negative");
        if (ordinal > 0 && boundary == null)
            throw new NoSuchPartException("There is only one part in a single-part message");
        // Have we already prepared this part?
        if (ordinal >= partInfos.size())
            readUpToPart(ordinal);
        return (PartInfo)partInfos.get(ordinal);
    }

    /**
     * Get the specified PartInfo from this message by Content-ID.  If the specified Content-ID has not already
     * been found, this may require reading, stashing, and parsing the rest of the message InputStream all
     * the way up to and including the closing delimiter of the multipart message in order to rule out the
     * existence of an attachment with this Content-ID.
     *
     * @param contentId   the Content-ID to look for, without any enclosing angle brackets.  May not be null.
     * @return the PartInfo describing the MIME part with the specified Content-ID.  Never null.
     * @throws NoSuchPartException if the entire message was examined and no part with that Content-ID was found.
     * @throws IOException if there was a problem reading the message stream
     */
    public PartInfo getPartByContentId(String contentId) throws IOException, NoSuchPartException {
        PartInfo partInfo = (PartInfo)partInfosByCid.get(contentId);
        while (partInfo == null) {
            if (!moreParts)
                throw new NoSuchPartException("No part was found with the Content-ID: " + contentId);
            stashCurrentPartBody();
            readNextPartHeaders();
            partInfo = (PartInfo)partInfosByCid.get(contentId);
        }
        return partInfo;
    }

    /**
     * Produce an InputStream that, when read, will essentially reconstruct the original multipart message body
     * that was passed to the createMultipartMessage() factory method.  The reconstructed InputStream will start
     * with the "--" of the first part's initial multipart boundary (unless this is a singlepart message,
     * destroyAsRead is true, and the first part's body has not been read; in which case the caller will receive
     * whatever comes in from the original InputStream).
     * <p>
     * If destroyAsRead is false, or if the final part needs to be reconstructed, it will end with the "--\r\n" at
     * the end of the last multipart boundary.  Otherwise, if destroyAsRead is true and the final part has not yet
     * been read, the caller will receive whatever comes in from the original InputStream.
     * <p>
     * If any parts have been touched, the preamble will already have been thrown away.
     * <p>
     * If any parts have already been destructively read, this method will throw immediately.
     * <p>
     * If destroyAsRead is true, as-yet unexamined Parts in this MultipartMessage will no longer be available after
     * this call.  Parts will be read directly from the source stream where possible and will not be stashed;
     * in fact, Parts whose headers have not yet been parsed will never become available.
     * <p>
     * If destroyAsRead is false, all parts will be examined and stashed as the returned InputStream is read.
     *
     * @param destroyAsRead  if true, the parts will be read destructively.
     * @return an InputStream that, when read, will endeavor to reproduce the original multipart message body.
     * @throws IOException if a problem is detected early, such as one or more parts already having been read destructively
     */
    public InputStream getEntireMessageBodyAsInputStream(boolean destroyAsRead) throws IOException {

        if (!destroyAsRead) {
            // Ensure that everything is stashed right now
            if (boundary != null)
                readAllParts(); // multipart
            else
                firstPart.getInputStream(false).close();  // singlepart
        }

        for (Iterator i = partInfos.iterator(); i.hasNext();) {
            PartInfoImpl partInfo = (PartInfoImpl)i.next();
            if (!partInfo.bodyAvailable())
                throw new IOException("Part #" + partInfo.getPosition() + " has already been destructively read");
        }

        if (boundary == null) {
            // Special case for single-part
            return firstPart.getInputStream(destroyAsRead);
        }

        Enumeration enumeration = new Enumeration() {
            private int nextPart = 0; // the part which is being sent
            private boolean sentNextPartOpeningBoundary = false;
            private boolean sentNextPartHeaders = false;
            private boolean sentNextPartBody = false;
            private boolean sentAllStashedParts = false;
            private boolean sentMainInputStream = false;
            private boolean needFinalCrap = false;
            private boolean sentFinalCrap = false;
            private boolean done = false;

            public boolean hasMoreElements() {
                return !done;
            }

            public Object nextElement() {
                // Generate the next input stream for the user to read.
                if (done)
                    throw new NoSuchElementException();

                if (!sentAllStashedParts) {
                    // We are still sending the stashed parts.

                    if (!sentNextPartOpeningBoundary) {
                        sentNextPartOpeningBoundary = true;
                        return new ByteArrayInputStream(("\r\n--" + boundaryStr + "\r\n").getBytes());
                    }

                    if (!sentNextPartHeaders) {
                        sentNextPartHeaders = true;
                        try {
                            return new ByteArrayInputStream(((PartInfo)partInfos.get(nextPart)).getHeaders().toByteArray());
                        } catch (IOException e) {
                            done = true;
                            return new IOExceptionThrowingInputStream(e);
                        }
                    }

                    if (!sentNextPartBody) {
                        try {
                            if (stashManager.peek(nextPart)) {
                                InputStream ret = stashManager.recall(nextPart);
                                if (ret == null)
                                    throw new IllegalStateException("Peek succeeds but recall fails"); // StashManager contract violation
                                nextPart++;
                                if (nextPart >= partInfos.size()) {
                                    sentAllStashedParts = true;
                                    if (moreParts)
                                        return new SequenceInputStream(ret,
                                                                       new ByteArrayInputStream(("\r\n--" + boundaryStr + "\r\n").getBytes()));
                                    else
                                        return ret;
                                }

                                sentNextPartOpeningBoundary = false;
                                sentNextPartHeaders = false;
                                sentNextPartBody = false;
                                return ret;
                            } else {
                                // Next part body is waiting at front of mainInputStream.  We are done with known parts.
                                sentNextPartOpeningBoundary = false;
                                sentNextPartHeaders = false;
                                sentNextPartBody = false;

                                sentAllStashedParts = true;

                                // FALLTHROUGH and return main input stream
                            }
                        } catch (IOException e) {
                            done = true;
                            return new IOExceptionThrowingInputStream(e);
                        }
                        // FALLTHROUGH and return main input stream
                    }

                    if (!sentAllStashedParts)
                        throw new IllegalStateException("send boundary, headers, and body, but not sentAllStashedParts");
                }

                if (!sentMainInputStream) {
                    sentMainInputStream = true;
                    if (moreParts) {
                        moreParts = false;
                        needFinalCrap = false;
                        return mainInputStream;
                    }
                    needFinalCrap = true;
                    // FALLTHROUGH and return final crap
                }

                if (!sentFinalCrap) {
                    sentFinalCrap = true;
                    if (needFinalCrap) {
                        // mainInputStream did not contain the final boundary anymore, so we need to make one
                        done = true;
                        return new ByteArrayInputStream(("\r\n--" + boundaryStr + "--\r\n").getBytes());
                    }
                    // FALLTHROUGH
                }

                done = true;
                return new EmptyInputStream();
            }
        };

        return new SequenceInputStream(enumeration);
    }

    /**
     * Stashes the current part's body.  When called, the input stream must be positioned at the first byte
     * of the current part's body.  When this method returns, the input stream will be positioned at the first
     * byte of the next part's headers, unless this is the final part, in which case moreParts will be false.
     *
     * @throws IOException if the main input stream could not be read or the info could not be stashed.
     */
    private void stashCurrentPartBody() throws IOException {
        int currentOrdinal = partInfos.size() - 1;
        final MimeBoundaryTerminatedInputStream in = new MimeBoundaryTerminatedInputStream(boundary, mainInputStream, pushbackSize);
        stashManager.stash(currentOrdinal, in);
        if (in.isLastPartProcessed())
            moreParts = false;
    }

    /**
     * Consume the main input stream until we are positioned at the first byte of the body of the specified part.
     * When called, the main input stream must be positioned at the first byte of the body of the current part.
     *
     * @param ordinal  the ordinal of the part to position before.
     * @throws NoSuchPartException if there turn out to be fewer than (ordinal + 1) parts in this MultipartMessage.
     * @throws IOException  if there was a problem reading the main InputStream.
     */
    private void readUpToPart(int ordinal) throws IOException, NoSuchPartException {
        if (boundary == null) throw new IllegalStateException("Not supported in single-part mode");
        while (partInfos.size() <= ordinal) {
            if (!moreParts)
                throw new NoSuchPartException("This message does not have a part #" + ordinal +
                                              "; there were only " + partInfos.size() + " parts");
            stashCurrentPartBody();
            readNextPartHeaders();
        }
    }

    /**
     * Read and stash all remaining parts until the end of the message is encountered.
     *
     * @throws IOException if the main input stream could not be read or the info could not be stashed.
     * @throws IOException if the headers could not be read
     */
    private void readAllParts() throws IOException {
        if (boundary == null) throw new IllegalStateException("Not supported in single-part mode");
        while (moreParts) {
            stashCurrentPartBody();
            if (!moreParts)
                return;
            try {
                readNextPartHeaders();
            } catch (NoSuchPartException e) {
                throw new RuntimeException(e); // can't happen, we checked moreParts before calling it
            }
        }
    }

    /** @return the outer Content-Type of this possibly-multipart message.  Never null. */
    public ContentTypeHeader getOuterContentType() {
        return outerContentType;
    }

    /** @return true if there might be more parts in the stream not yet reported by getNumPartsKnown */
    public boolean isMorePartsPossible() {
        return moreParts;
    }

    public int getNumPartsKnown() {
        return partInfos.size();
    }

    /** Our PartInfo implementation. */
    private class PartInfoImpl implements PartInfo {
        protected final int ordinal;
        protected final MimeHeaders headers;

        private PartInfoImpl(int ordinal, MimeHeaders headers) {
            this.ordinal = ordinal;
            this.headers = headers;
        }

        public MimeHeader getHeader(String name) {
            return headers.get(name);
        }

        public int getPosition() {
            return ordinal;
        }

        public byte[] getContent() throws IOException {
            throw new UnsupportedOperationException();
        }

        /**
         * See if it will be possible to call getInputStream() on this PartInfo and get back a non-null InputStream.
         *
         * @return true if this Part's body has been stashed, or is currently waiting to be read; false if it
         *         has already been read destructively.
         */
        boolean bodyAvailable() {
            // See if we have one stashed already
            try {
                if (stashManager.peek(ordinal))
                    return true;
            } catch (IOException e) {
                logger.log(Level.FINE, "Unable to peek ordinal in StashManager; ignoring", e);
            }

            // Guarantee: if this PartInfo exists, its headers have been read.
            // So, either our mainInputStream is already positioned right at this Part's body content,
            // or someone has already destructively read this Part's body content.

            // Are we the current Part?
            if (partInfos.size() != ordinal + 1 || !moreParts)
                return false;  // we've been consumed

            // looks like we are ready to go
            return true;
        }

        public InputStream getInputStream(boolean destroyAsRead) throws IOException {
            // See if we have one stashed already
            InputStream is = stashManager.recall(ordinal);
            if (is != null)
                return is;

            // Guarantee: if this PartInfo exists, its headers have been read.
            // So, either our mainInputStream is already positioned right at this Part's body content,
            // or someone has already destructively read this Part's body content.

            // Are we the current Part?
            if (partInfos.size() != ordinal + 1 || !moreParts)
                throw new IOException("This MIME multipart part's body content has already been destructively read.");

            // Prepare to read this Part's body
            is = new MimeBoundaryTerminatedInputStream(boundary, mainInputStream, pushbackSize);
            ((MimeBoundaryTerminatedInputStream)is).setEndOfStreamHook(new Runnable() {
                public void run() {
                    moreParts = false;
                }
            });

            // We are ready to return an InputStream.  Do we need to stash the data first?
            if (destroyAsRead) {
                // No -- allow caller to consume it.
                return is;
            }

            // Yes -- stash it first, then recall it.
            stashManager.stash(ordinal, is);
            is = stashManager.recall(ordinal);
            if (is == null)
                throw new IllegalStateException("Stash succeeds but recall fails"); // StashManager contract violation
            return is;
        }

        public MimeHeaders getHeaders() {
            return this.headers;
        }

        public long getContentLength() {
            long size = stashManager.getSize(ordinal);
            if (size >= 0)
                return size;
            if (headers.hasContentLength())
                try {
                    return headers.getContentLength();
                } catch (IOException e) {
                    logger.log(Level.WARNING, "Invalid Content-Length header", e); // can't happen now
                }
            return -1;
        }

        public ContentTypeHeader getContentType() {
            return headers.getContentType();
        }

        public String getContentId() {
            return headers.getContentId();
        }

        // TODO do we need this?
        public boolean isValidated() {
            throw new UnsupportedOperationException();
        }

        // TODO do we need this?
        public void setValidated(boolean validated) {
            throw new UnsupportedOperationException();
        }
    }
}
