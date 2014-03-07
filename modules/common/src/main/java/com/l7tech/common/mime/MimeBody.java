package com.l7tech.common.mime;

 import com.l7tech.common.io.*;
 import com.l7tech.util.*;

 import java.io.*;
 import java.util.*;
 import java.util.concurrent.atomic.AtomicLong;
 import java.util.logging.Level;
 import java.util.logging.Logger;

/**
 * Encapsulates the body of a message that might be multipart/related.
 *
 * <p>Can be used even for messages that are not multipart; in such cases, it
 * pretends that there was a multipart message with only a single part.</p>
 *
 * <p>The general contract of this class is that you give it ownership of an
 * InputStream that may or may not contain multipart parts, and it gives you
 * a view of one or more PartInfo instances, each of which can be used to
 * retrieve metadata and an InputStream.</p>
 *
 * <p>The first PartInfo can be completely changed, but subsequent PartInfos
 * are read-only.</p>
 *
 * <p>None of the methods in this class are guaranteed to be reentrant. Care
 * should be taken when providing a custom InputStream implementation not to
 * call MimeBody methods from inside it, when it is itself being called from
 * MimeBody.</p>
 * @noinspection ForLoopReplaceableByForEach,unchecked
 */
public class MimeBody implements Iterable<PartInfo>, Closeable {
    private static final Logger logger = Logger.getLogger(MimeBody.class.getName());
    private static final int BLOCKSIZE = 4096;

    private static final AtomicLong firstPartMaxBytes = new AtomicLong(0);

    private static final long PREAMBLE_MAX_SIZE = ConfigFactory.getLongProperty( "com.l7tech.common.mime.preambleMaxSize", 1024 * 32 );
    private static final long HEADERS_MAX_SIZE = ConfigFactory.getLongProperty( "com.l7tech.common.mime.headersMaxSize", 1024 * 32 );
    private static final boolean RAW_PARTS = ConfigFactory.getBooleanProperty( "com.l7tech.common.mime.rawParts", false );
    private static boolean ALLOW_LAX_START_PARAM_MATCH = ConfigFactory.getBooleanProperty( "com.l7tech.common.mime.allowLaxStartParamMatch", false );
    private static boolean ALLOW_LAX_EMPTY_MUTLIPART = ConfigFactory.getBooleanProperty( "com.l7tech.common.mime.allowLaxEmptyMultipart", false );

    /**
     * Allow system loaded lax start param match variable to be reloaded, used initially in test coverage
     */
    static void loadLaxStartParam(){
        ALLOW_LAX_START_PARAM_MATCH = ConfigFactory.getBooleanProperty( "com.l7tech.common.mime.allowLaxStartParamMatch", false );
    }
    static void loadLaxEmptyMutipart(){
        ALLOW_LAX_EMPTY_MUTLIPART = ConfigFactory.getBooleanProperty( "com.l7tech.common.mime.allowLaxEmptyMultipart", false );
    }


    private final FlaggingByteLimitInputStream mainInputStream; // always pointed at current part's body, or just past end of message
    private final int pushbackSize;
    private final StashManager stashManager;
    private ContentTypeHeader outerContentType;

    private final List partInfos = new ArrayList(); // our PartInfo instances.  current part is (partInfos.size() - 1)
    private final PartInfoImpl firstPart; // equivalent to (PartInfo)partInfos.get(0)
    private final Map partInfosByCid = new HashMap(); // our PartInfo-by-cid lookup.

    private String boundaryStr; // multpart boundary not including initial dashses or any CRLFs; or null if singlepart
    private byte[] boundary; // multipart crlfBoundary bytes including initial dashes but not including trailing CRLF; or null if singlepart.

    private boolean streamValidatedOnly = false; // flag for filtering parts based on validation flag
    private boolean moreParts = true; // assume there are more parts until we find the end of the stream

    private Exception errorCondition = null;  // If non-null, the specified error condition will be reported by public methods

    /**
     * Create a new MimeBody instance that will read from the specified mainInputStream, treating the content
     * as the specified outerContentType.
     * <p>
     * When you have finished with a MimeBody, call {@link #close} to free any resources being used, including
     * the StashManager.
     *
     * @param stashManager the StashManager to use.  Must not be null.  See {@link ByteArrayStashManager} for an example.
     *                     If a MimeBody is succesfully created, it takes ownership of the stashManager.
     *                     {@link #close} to free resources used by this MimeBody
     * @param outerContentType   a ContentTypeHeader describing the bytes produced by mainInputStream.
     *                           May be any single-part type, and
     *                           may be multipart/related if the message body is appropriately formatted (including
     *                           boundary lines matching the boundary in the ContentTypeHeader, etc).  Must not be null.
     *                           Use {@link ContentTypeHeader#OCTET_STREAM_DEFAULT} if you have absolutely no clue.
     * @param mainInputStream  the primary InputStream.  May not be null.  Must be positioned to the first byte
     *                         of the body content, regardless of whether or not the body is multipart.
     *                         If a MimeBody is successfully created, it takes ownership of the mainInputStream.
     *                         If the MimeBody constructor throws a checked exception, this StashManager will already have been closed.
     * @param firstPartMaxBytes maximum number of bytes to read into first MIME part before throwing IOException, or 0 for unlimited.
     * @throws IOException if the mainInputStream cannot be read or a multipart message is not in valid MIME format
     */
    public MimeBody(StashManager stashManager,
                             ContentTypeHeader outerContentType,
                             InputStream mainInputStream,
                             long firstPartMaxBytes)
            throws IOException
    {
        boolean itworked = false;
        try {
            if (stashManager == null || outerContentType == null || mainInputStream == null)
                throw new IllegalArgumentException("stashManager, outerContentType, and mainInputStream must all be provided");

            this.outerContentType = outerContentType;
            this.stashManager = stashManager;

            if (outerContentType.isMultipart()) {
                // Multipart message.  Prepare the first part for reading.
                String start = outerContentType.hasParams() ? outerContentType.getParam("start") : null;
                if (start != null && start.length() < 1) throw new IOException("Multipart content type has a \"start\" parameter but it is empty");

                boundaryStr = outerContentType.getMultipartBoundary();
                boundary = ("--" + boundaryStr).getBytes(MimeHeader.ENCODING);
                if (boundary.length > BLOCKSIZE)
                    throw new IOException("This multipart message cannot be processed because it uses a multipart crlfBoundary which is more than 4kb in length");
                byte[] boundaryScanbuf = new byte[boundary.length];
                pushbackSize = BLOCKSIZE + boundaryScanbuf.length;
                this.mainInputStream = new FlaggingByteLimitInputStream(mainInputStream);
                readInitialBoundary();
                if(!moreParts && ALLOW_LAX_EMPTY_MUTLIPART){
                    // Is multi part with no content. Read as single-part message.  Configure first and only part.
                    moreParts = true;
                    boundaryStr = null;
                    boundary = null;
                    if (firstPartMaxBytes > 0)
                        this.mainInputStream.setSizeLimitNonFlagging(firstPartMaxBytes);
                    final MimeHeaders outerHeaders = new MimeHeaders();
                    outerHeaders.add(outerContentType);
                    final PartInfoImpl mainPartInfo = new PartInfoImpl(0, outerHeaders) {
                        @Override
                        public InputStream getInputStream(boolean destroyAsRead) throws IOException, NoSuchPartException {
                            InputStream stashedStream = preparePartInputStream();
                            if (stashedStream != null)
                                return stashedStream;

                            InputStream is = MimeBody.this.mainInputStream;
                            moreParts = false;
                            onBodyRead();

                            // We are ready to return an InputStream.
                            // Do we need to stash the data first?
                            if (destroyAsRead) {
                                // No -- allow caller to consume it.
                                return is;
                            }

                            // Yes -- stash it first, then recall it.
                            return stashAndRecall(is);
                        }
                    };
                    partInfos.add(mainPartInfo);
                    firstPart = mainPartInfo;
                }else{
                    readNextPartHeaders();
                    firstPart = (PartInfoImpl)partInfos.get(0);
                    if (start != null && !(
                                    firstPart.getHeaders().size() < 1 ||
                                    start.equals(firstPart.getContentId(false)) ||
                                    (ALLOW_LAX_START_PARAM_MATCH && start.equals(firstPart.getContentId(true))) )
                            ) {
                        throw new IOException("Multipart content type has a \"start\" parameter, but it doesn't match the cid of the first MIME part.");
                    }

                    if (firstPartMaxBytes > 0)
                        this.mainInputStream.setSizeLimitNonFlagging(firstPartMaxBytes + this.mainInputStream.getBytesRead());
                }
            } else {
                // Single-part message.  Configure first and only part accordingly.
                boundaryStr = null;
                boundary = null;
                pushbackSize = BLOCKSIZE;
                this.mainInputStream = new FlaggingByteLimitInputStream(mainInputStream, pushbackSize);
                if (firstPartMaxBytes > 0)
                    this.mainInputStream.setSizeLimitNonFlagging(firstPartMaxBytes);
                final MimeHeaders outerHeaders = new MimeHeaders();
                outerHeaders.add(outerContentType);
                final PartInfoImpl mainPartInfo = new PartInfoImpl(0, outerHeaders) {
                    @Override
                    public InputStream getInputStream(boolean destroyAsRead) throws IOException, NoSuchPartException {
                        InputStream stashedStream = preparePartInputStream();
                        if (stashedStream != null)
                            return stashedStream;

                        InputStream is = MimeBody.this.mainInputStream;
                        moreParts = false;
                        onBodyRead();

                        // We are ready to return an InputStream.
                        // Do we need to stash the data first?
                        if (destroyAsRead) {
                            // No -- allow caller to consume it.
                            return is;
                        }

                        // Yes -- stash it first, then recall it.
                        return stashAndRecall(is);
                    }
                };
                partInfos.add(mainPartInfo);
                firstPart = mainPartInfo;

                final String mainContentId = mainPartInfo.getContentId(true);
                if (mainContentId != null)
                    partInfosByCid.put(mainContentId, mainPartInfo);
            }
            itworked = true;
        } catch (NoSuchPartException e) {
            throw (IOException) new IOException("Message MIME type is multipart/related, but no initial boundary found").initCause(e);
        } finally {
            if (!itworked) ResourceUtils.closeQuietly(stashManager);
        }
    }

    /**
     * Create a MimeBody out of the specified byte array interpreted as the specified outerContentType.
     * This will always create a new ByteArrayStashManager.
     *
     * @param bytes  bytes of the message body.  Must not be null.
     * @param outerContentType   a ContentTypeHeader describing the bytes.  May be any single-part type, and
     *                           may be multipart/related if the message body is appropriately formatted (including
     *                           boundary lines matching the boundary in the ContentTypeHeader, etc).  Must not be null.
     *                           Use {@link ContentTypeHeader#OCTET_STREAM_DEFAULT} if you have absolutely no clue.
     * @throws NoSuchPartException if this message is multpart/related but does not have any parts
     * @throws IOException if the mainInputStream cannot be read, or a multipart message is not in valid MIME format
     */
    public MimeBody(byte[] bytes, ContentTypeHeader outerContentType, long firstPartMaxBytes) throws IOException, NoSuchPartException {
        this(new ByteArrayStashManager(),
             outerContentType,
             new ByteArrayInputStream(bytes),
            firstPartMaxBytes);
    }

    /**
    * @param firstPartMaxBytes  size limit to enforce for the first part for new MimeBody instances created from now on, or zero for no limit
    */
    public static void setFirstPartMaxBytes(long firstPartMaxBytes) {
        MimeBody.firstPartMaxBytes.set(firstPartMaxBytes);
    }

    /**
     * Value from cluster property "io.xmlPartMaxBytes"
     * @return size limit to enforce for the first part for new MimeBody instances created from now on, or zero for no limit
     */
    public static long getFirstPartMaxBytes(){
        return MimeBody.firstPartMaxBytes.get();
    }



    /**
     * Consume the headers of the next part, and store a PartInfo.  The main InputStream must be positioned just
     * beyond the boundary, at the first byte of the next part's headers.
     *
     * @throws IOException if the headers could not be read
     * @throws NoSuchPartException  if all parts have already been read
     */
    private void readNextPartHeaders() throws IOException, NoSuchPartException {
        if (boundary == null) throw new IllegalStateException("Not supported in single-part mode");
        if (!moreParts)
            throw new NoSuchPartException("Out of parts");

        // Turn off limit
        this.mainInputStream.clearLimitNonFlagging();

        // Consume the headers of the first part.
        final ByteLimitInputStream headersIn = new ByteLimitInputStream(mainInputStream, 32, HEADERS_MAX_SIZE);
        try {
            final MimeHeaders headers = MimeUtil.parseHeaders( headersIn );
            final PartInfoImpl partInfo = new PartInfoImpl(partInfos.size(), headers);
            partInfos.add(partInfo);
            final String cid = partInfo.getContentId(true);
            if (cid != null)
                partInfosByCid.put(cid, partInfo);
        } catch ( ByteLimitInputStream.DataSizeLimitExceededException e ) {
            throw new IOException("Multipart message headers too large");
        } finally {
            headersIn.dispose();
        }

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
        final MimeBoundaryTerminatedInputStream preamble = new MimeBoundaryTerminatedInputStream(boundary, mainInputStream, pushbackSize);
        final NullOutputStream nowhere = new NullOutputStream();
        final ByteLimitInputStream limitedPreamble = new ByteLimitInputStream(preamble, 32, PREAMBLE_MAX_SIZE);
        try {
            IOUtils.copyStream( limitedPreamble, nowhere );
        } catch ( ByteLimitInputStream.DataSizeLimitExceededException e ) {
            throw new IOException("Multipart message preamble too large");
        } finally {
            limitedPreamble.dispose();
        }
        if (preamble.isLastPartProcessed()) {
            moreParts = false;  // just in case
        }
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
            throw new NoSuchPartException("There is only one part in a single-part message", ordinal);
        // Have we already prepared this part?
        if (ordinal >= partInfos.size())
            readUpToPart(ordinal);
        return (PartInfo)partInfos.get(ordinal);
    }

    /**
     * Obtain an iterator that can be used to lazily iterate some or all parts in this MimeBody.
     * The iterator can be abandoned at any time, in which case any still-unread parts will be left in the main InputStream
     * (as long as they hadn't already needed to be read due to other method calls on MimeBody or PartInfo).
     * <p>
     * Since this iterator is roughly equivalent to just calling {@link #getPart(int)}
     * while {@link #isMorePartsPossible()}, it is safe to call other MimeBody and {@link PartInfo} methods
     * while iterating.  The usual caveats regarding PartInfo methods apply though: specifically, it is not safe to
     * call any MimeBody or PartInfo methods whatsoever if any destroyAsRead InputStreams are open
     * on a PartInfo.
     * <p>
     * Note that, differing from {@link java.util.Iterator}, this PartIterator might throw NoSuchPartException
     * from next() even if hasNext() returned true, if the input message was not properly terminated.
     *
     * @return a {@link PartIterator} ready to iterate all parts of this message from beginning to end.  Never null.
     */
    @Override
    public PartIterator iterator() {
        return new PartIterator() {
            int nextPart = 0;

            @Override
            public boolean hasNext() throws UncheckedIOException {
                try {
                    return nextPart < partInfos.size() || isMorePartsPossible() && readUpToPartNoThrow(nextPart);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }

            @Override
            public PartInfo next() throws UncheckedIOException, NoSuchElementException {
                try {
                    return getPart(nextPart++);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                } catch (NoSuchPartException e) {
                    throw (NoSuchElementException)new NoSuchElementException().initCause(e);
                }
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
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
                throw new NoSuchPartException("No part was found with the Content-ID: " + contentId, contentId);
            stashCurrentPartBody();
            readNextPartHeaders();
            partInfo = (PartInfo)partInfosByCid.get(contentId);
        }
        return partInfo;
    }

    /**
     * Set this MimeBody to respect part validation.
     *
     * <p>This will effect the output of the getEntireMessageBodyLength and
     * getEntireMessageBodyAsInputStream methods.</p>
     *
     * <p>Calling this will force all parts to be stashed when the
     * getEntireMessageBodyAsInputStream method is called (if this has not
     * already occurred)</p>
     *
     * <p>Note that validation does NOT apply to the first part.</p>
     */
    public void setEntireMessageBodyAsInputStreamIsValidatedOnly() {
        this.streamValidatedOnly = true;
    }

    /**
     * Produce an InputStream that, when read, will essentially reconstruct the original multipart message body
     * that was passed to the createMultipartMessage() factory method.
     * <p>
     * The reconstructed InputStream will start
     * with the "--" of the first part's initial multipart boundary (unless this is a singlepart message,
     * destroyAsRead is true, and the first part's body has not been read; in which case the caller will receive
     * whatever comes in from the original InputStream).
     * </p><p>
     * If destroyAsRead is false, or if the final part needs to be reconstructed, it will end with the "--\r\n" at
     * the end of the last multipart boundary.  Otherwise, if destroyAsRead is true and the final part has not yet
     * been read, the caller will receive whatever comes in from the original InputStream.
     * </p><p>
     * If destroyAsRead is true and setEntireMessageBodyAsInputStreamIsValidatedOnly has been called then
     * the body will only be read distructively if it is a single part.
     * </p><p>
     * If any parts have been touched, the preamble will already have been thrown away.
     * </p><p>
     * If any parts have already been destructively read, this method will throw immediately.
     * </p><p>
     * If destroyAsRead is true, as-yet unexamined Parts in this MimeBody will no longer be available after
     * this call (unless setEntireMessageBodyAsInputStreamIsValidatedOnly has been called).  Parts will be read
     * directly from the source stream where possible and will not be stashed; in fact, Parts whose headers have
     * not yet been parsed will never become available.
     * </p><p>
     * If destroyAsRead is false (or setEntireMessageBodyAsInputStreamIsValidatedOnly has been called), all parts
     * will be examined and stashed as the returned InputStream is read.
     * </p>
     *
     * @param destroyAsRead  if true, the parts will be read destructively.
     * @return an InputStream that, when read, will endeavor to reproduce the original multipart message body.
     * @throws IOException if there is a problem reading enough of the main input stream to prepare the new InputStream
     * @throws NoSuchPartException if one or more part bodies have already been read destructively
     */
    public InputStream getEntireMessageBodyAsInputStream(boolean destroyAsRead) throws IOException, NoSuchPartException {
        checkErrorBoth();

        if (!destroyAsRead || (streamValidatedOnly && boundary != null))
            readAndStashEntireMessage();

        assertNoPartBodiesDestroyed();

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

            @Override
            public boolean hasMoreElements() {
                return !done;
            }

            @Override
            public Object nextElement() {
                if (errorCondition != null)
                    //noinspection ThrowableInstanceNeverThrown
                    return new IOExceptionThrowingInputStream(new CausedIOException(errorCondition));

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
                                final InputStream ret;
                                try {
                                    ret = stashManager.recall(nextPart);
                                } catch (NoSuchPartException e) {
                                    throw new CausedIllegalStateException("Peek succeeds but recall fails", e); // StashManager contract violation
                                }
                                nextPart++;
                                boolean followingPartValidated = false;
                                while (!followingPartValidated) {
                                    if (nextPart >= partInfos.size()) {
                                        sentAllStashedParts = true;
                                        if (moreParts)
                                            return new SequenceInputStream(ret,
                                                                           new ByteArrayInputStream(("\r\n--" + boundaryStr + "\r\n").getBytes()));
                                        else
                                            return ret;
                                    }
                                    else if (streamValidatedOnly) {
                                        PartInfo followingPartInfo = (PartInfo) partInfos.get(nextPart);
                                        if (!followingPartInfo.isValidated()) {
                                            nextPart++; // skip
                                        }
                                        else {
                                            followingPartValidated = true;
                                        }
                                    }
                                    else {
                                        followingPartValidated = true;
                                    }
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
                                ((PartInfoImpl)partInfos.get(nextPart)).onBodyRead(); // tell this part that it's body is gone

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
     * Assert that no part bodies have been destructively read.  If this returns, it means that all remaining
     * multipart parts either have not yet been read from the main input stream, or have already been read and
     * their bodies stashed.
     *
     * @throws NoSuchPartException if one or more part bodies have already been read destructively
     * @throws IOException if there is an IOException reading from the StashManager
     */
    private void assertNoPartBodiesDestroyed() throws NoSuchPartException, IOException {
        checkErrorNoPart();

        for (Iterator i = partInfos.iterator(); i.hasNext();) {
            PartInfoImpl partInfo = (PartInfoImpl)i.next();
            if (!partInfo.isBodyAvailable())
                throw new NoSuchPartException("Part #" + partInfo.getPosition() + " has already been destructively read", partInfo.getPosition());
        }
    }

    /**
     * Compute the length of the entire message body, were it to be reserialized right now.  Calling this method
     * will force any still-unread part bodies to be read and stashed.
     *
     * <p>If setEntireMessageBodyAsInputStreamIsValidatedOnly has been called then this will return the length
     * of the parts flagged as valid (the first part is always counted).</p>
     *
     * @return the number of bytes that would be produced by the InputStream returned by
     *         getEntireMessageBodyAsInputStream() if it were to be called right now; or, a number less than zero
     *         if the total length could not be determined.
     * @throws NoSuchPartException if one or more part bodies have already been read destructively
     * @throws IOException if the main input stream could not be read or the info could not be stashed, or
     *                     if the headers could not be read
     */
    public long getEntireMessageBodyLength() throws IOException, NoSuchPartException {
        checkErrorBoth();

        readAndStashEntireMessage();
        assertNoPartBodiesDestroyed();

        if (boundary == null) {
            // singlepart.  Nice and easy :)
            return firstPart.getActualContentLength();
        }

        long len = 0;
        for (Iterator i = partInfos.iterator(); i.hasNext();) {
            PartInfo partInfo = (PartInfo) i.next();

            // if only streaming validated parts then count only those lengths (and the first part)
            if (len==0 || !streamValidatedOnly || partInfo.isValidated()) {
                // Opening delimiter: CRLF + boundary (which includes initial 2 dashes) + CRLF
                len += 2 + boundary.length + 2;
                len += partInfo.getHeaders().getSerializedLength();
                long bodylen = partInfo.getActualContentLength();
                if (bodylen < 0)
                    return bodylen;
                len += bodylen;
            }
        }
        // Closing delimiter: CRLF + boundary (which includes initial 2 dashes) + two dashses + CRLF
        len += 2 + boundary.length + 2 + 2;
        return len;
    }

    /**
     * Stashes the current part's body.  When called, the input stream must be positioned at the first byte
     * of the current part's body.  When this method returns, the input stream will be positioned at the first
     * byte of the next part's headers, unless this is the final part, in which case moreParts will be false.
     *
     * @throws IOException if the main input stream could not be read or the info could not be stashed.
     */
    private void stashCurrentPartBody() throws IOException {
        checkErrorIO();

        PartInfoImpl currentPart = (PartInfoImpl)partInfos.get(partInfos.size() - 1);
        final MimeBoundaryTerminatedInputStream in = new MimeBoundaryTerminatedInputStream(boundary, mainInputStream, pushbackSize);
        currentPart.stashAndCheckContentLength(in);
        currentPart.onBodyRead();
        if (in.isLastPartProcessed())
            moreParts = false;
    }

    /**
     * Consume the main input stream until we are positioned at the first byte of the body of the specified part.
     * When called, the main input stream must be positioned at the first byte of the body of the current part.
     *
     * @param ordinal  the ordinal of the part to position before.
     * @throws NoSuchPartException if there turn out to be fewer than (ordinal + 1) parts in this MimeBody.
     * @throws IOException  if there was a problem reading the main InputStream.
     */
    private void readUpToPart(int ordinal) throws IOException, NoSuchPartException {
        checkErrorBoth();

        if (boundary == null) throw new IllegalStateException("Not supported in single-part mode");
        while (partInfos.size() <= ordinal) {
            if (!moreParts)
                throw new NoSuchPartException("This message does not have a part #" + ordinal +
                                              "; there were only " + partInfos.size() + " parts", ordinal);
            stashCurrentPartBody();
            readNextPartHeaders();
        }
    }

    /**
     * Consume the main input stream until we are positioned at the first byte of the body of the specified part.
     * When called, the main input stream must be positioned at the first byte of the body of the current part.
     *
     * @param ordinal  the ordinal of the part to position before.
     * @return true if we have read the headers and are positioned to read the body of the requested part ordinal
     *         false if we ran out of parts to read before we found the headers for the requested part ordinal
     * @throws IOException  if there was a problem reading the main InputStream.
     */
    private boolean readUpToPartNoThrow(int ordinal) throws IOException {
        checkErrorIO();

        if (boundary == null) return false;
        while (partInfos.size() <= ordinal) {
            if (!moreParts) return false;
            stashCurrentPartBody();
            if (!moreParts) return false;
            try {
                readNextPartHeaders();
            } catch (NoSuchPartException e) {
                return false;
            }
        }

        return true;
    }

    /**
     * Read and stash all remaining parts until the end of the message is encountered, perhaps so that
     * the main InputStream can be safely closed.
     * <p>
     * For a singlepart message, this just ensures that the body content has been read and stashed.
     *
     * @throws IOException if the main input stream could not be read or the info could not be stashed, or
     *                     if the headers could not be read
     * @throws NoSuchPartException if any part message body has already been destructively read
     */
    public void readAndStashEntireMessage() throws IOException, NoSuchPartException {
        checkErrorIO();

        if (boundary == null) {
            // singlepart
            firstPart.getInputStream(false).close();
            firstPart.onBodyRead();
            return;
        }

        // multipart
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

    /**
     * Change the outer content type.
     * <p/>
     * This will change the outer content type reported by this MimeBody but will not affect the processing
     * of the current MIME body in any way.
     * <p/>
     * If this is a multipart message, changing its content type will have no effect on current multipart boundary
     * for any parts that have not yet been read; nor will it change the number of parts.
     * <p/>
     * Similarly, if this is a single-part message, changing its content type will not cause its contents to be recognized
     * as a multipart message.
     *
     * @param outerContentType a new content type to return from {@link #getOuterContentType()}.  Required.
     */
    public void setOuterContentType(ContentTypeHeader outerContentType) {
        if (outerContentType == null)
            throw new NullPointerException("outerContentType is required");
        this.outerContentType = outerContentType;
    }

    /**
     * Check if more parts might remain in the stream, in addition to the ones already reported by {@link #getNumPartsKnown}().
     *
     * @return true if there might be more parts in the stream not yet reported by {@link #getNumPartsKnown}().
     */
    public boolean isMorePartsPossible() {
        return moreParts;
    }

    /**
     * @return the number of parts known to exist in this message.  Will always be greater than zero.
     * <p>
     *         Calling {@link #getPart(int)}() with a number between zero (inclusive) and getNumPartsKnown() (exclusive)
     *         is guaranteed to return a {@link PartInfo} without throwing.
     * <p>
     *         If {@link #isMorePartsPossible}() is true, calling {@link #getPart(int)}() with the number getNumPartsKnown() will
     *         return another {@link PartInfo} without throwing unless the input message is malformed.
     */
    public int getNumPartsKnown() {
        return partInfos.size();
    }

    /**
     * @return the first part of this message.  For multipart messages, this is the part right after the boundary
     *         that ends the preamble.  For singlepart messages, this is the virtual part representing the
     *         entire message body.
     */
    public PartInfo getFirstPart() {
        return firstPart;
    }

    /**
     * Check if this is a multipart message or not.  This should almost never be necessary.
     *
     * @return true if this message is multipart, or false if it is single-part
     */
    public boolean isMultipart() {
        return boundary != null;
    }

    private void checkErrorIO() throws IOException {
        if (errorCondition != null)
            throw new CausedIOException(errorCondition.getMessage(), errorCondition);
    }

    private void checkErrorNoPart() throws NoSuchPartException {
        if (errorCondition instanceof NoSuchPartException) {
            NoSuchPartException e = (NoSuchPartException) errorCondition;
            throw new NoSuchPartException(e.getMessage(), e.getCid(), e.getOrdinal(), e);
        }

        if (errorCondition != null)
            throw new NoSuchPartException(errorCondition.getMessage(), errorCondition);
    }

    private void checkErrorBoth() throws IOException, NoSuchPartException {
        if (errorCondition instanceof NoSuchPartException) {
            NoSuchPartException e = (NoSuchPartException) errorCondition;
            throw new NoSuchPartException(e.getMessage(), e.getCid(), e.getOrdinal(), e);
        }

        if (errorCondition != null)
            throw new CausedIOException(errorCondition.getMessage(), errorCondition);
    }

    /**
     * Free any resources being used by this MimeBody.  In particular this closes the StashManager.
     * The behaviour of other MimeBody or PartInfo methods is undefined after close() has been called.
     * <p>
     * Note that this does *not* close the main InputStream.  This is in case the user wishes to parse
     * more than one MimeBody out of the same InputStream.
     * <p>
     * TODO: to make multiple messages per stream actualyl work, the PushbackInputStream will need to be passed into
     *       the MimeBody constructor instead of being created locally.
     */
    public void close() {
        stashManager.close();
        if(mainInputStream!=null) mainInputStream.dispose();
    }

    /**
     * Set the maximum size of the MIME body that this MimeBody should be prepared to process.
     *
     * <p>This may have no effect if the parts have already all been read and stashed.</p>
     *
     * <p>If the limit has already been reached when this method is called, the stream will be closed immediately
     * and an IOException thrown.</p>
     *
     * @param sizeLimit  the new size limit, or 0 for no limit.  Must be nonnegative.
     * @throws IOException if the limit has already been reached.
     */
    public void setBodyLengthLimit(long sizeLimit) throws IOException {
        mainInputStream.setSizeLimit(sizeLimit);
    }

    public long getBodyLengthLimit() {
        return mainInputStream.getSizeLimit();
    }

    /** Our PartInfo implementation. */
    private class PartInfoImpl implements PartInfo {
        protected final int ordinal;
        protected final MimeHeaders headers;
        private boolean bodyRead = false;   // if true, body has been read from main input stream.
                                            // body may still be available if it was stashed
        private boolean validated = false;

        private PartInfoImpl(int ordinal, MimeHeaders headers) {
            this.ordinal = ordinal;
            this.headers = headers;
        }

        @Override
        public MimeHeader getHeader(String name) {
            return headers.get(name);
        }

        @Override
        public int getPosition() {
            return ordinal;
        }

        @Override
        public boolean isBodyRead() {
            return bodyRead;
        }

        void onBodyRead() {
            this.bodyRead = true;
        }

        @Override
        public boolean isBodyStashed() {
            return stashManager.peek(ordinal);
        }

        @Override
        public boolean isBodyAvailable() {
            if (!bodyRead)
                return true;

            // See if we have one stashed already
            return stashManager.peek(ordinal);
        }

        @Override
        public void setContentType(ContentTypeHeader newContentType) {
            headers.replace(newContentType);
        }

        @Override
        public void setBodyBytes(byte[] newBody) throws IOException {
            setBodyBytes( newBody, 0, newBody.length );
        }

        @Override
        public void setBodyBytes(byte[] newBody, int offset, int length) throws IOException {
            checkErrorIO();

            if (stashManager.peek(ordinal))
                stashManager.unstash(ordinal);

            // Are we the current Part?
            if (partInfos.size() == ordinal + 1 && moreParts)
                try {
                    IOUtils.copyStream(getInputStream(true), new NullOutputStream()); // Read and discard existing body
                } catch (NoSuchPartException e) {
                    throw new CausedIllegalStateException("getInputStream threw unexpectedly even though moreParts is true", e);
                }

            final String contentTransferEncoding = getContentTransferEncoding();
            if ( !RAW_PARTS && MimeUtil.isProcessedTransferEncoding(contentTransferEncoding) ) {
                InputStream in = null;
                try {
                    in = MimeUtil.getEncodingInputStream( newBody, offset, length, contentTransferEncoding );
                    stashManager.stash(ordinal, in );
                } finally {
                    ResourceUtils.closeQuietly(in);
                }
            } else if ( RAW_PARTS || MimeUtil.isIdentityTransferEncoding(contentTransferEncoding) ) {
                stashManager.stash(ordinal, newBody, offset, length);
            } else {
                throw new IOException( "Unsupported content transfer encoding '"+contentTransferEncoding+"'." );
            }
            headers.replace(new MimeHeader(MimeUtil.CONTENT_LENGTH, Integer.toString(length), null, false));
        }

        @Override
        public InputStream getInputStream(boolean destroyAsRead) throws IOException, NoSuchPartException {
            InputStream is = preparePartInputStream();

            if (is != null)
                return decodeIfNecessary(is, true);

            // Prepare to read this Part's body
            is = new MimeBoundaryTerminatedInputStream(boundary, mainInputStream, pushbackSize);
            ((MimeBoundaryTerminatedInputStream)is).setEndOfStreamHook(new Runnable() {
                @Override
                public void run() {
                    try {
                        onBodyRead();
                        readNextPartHeaders();
                    } catch (IOException e) {
                        // No way to report this error directly.  Remember it for next call
                        errorCondition = e;
                    } catch (NoSuchPartException e) {
                        // No way to report this error directly.  Remember it for next call
                        errorCondition = e;
                    }
                }
            });
            ((MimeBoundaryTerminatedInputStream)is).setFinalBoundaryHook(new Runnable() {
                @Override
                public void run() {
                    onBodyRead();
                    moreParts = false;
                }
            });

            // We are ready to return an InputStream.  Do we need to stash the data first?
            if (destroyAsRead) {
                // No -- allow caller to consume it.
                return decodeIfNecessary(is, false);
            }

            // Yes -- stash it first, then recall it.
            return decodeIfNecessary(stashAndRecall(is), true);
        }

        @Override
        public byte[] getBytesIfAlreadyAvailable() {
            final boolean decodingNotRequired = RAW_PARTS || MimeUtil.isIdentityTransferEncoding(getContentTransferEncoding());
            try {
                return decodingNotRequired && stashManager.isByteArrayAvailable(ordinal) ?
                        stashManager.recallBytes(ordinal) :
                        null;
            } catch (NoSuchPartException e) {
                throw new RuntimeException(e); // can't happen -- we checked first
            }
        }

        @Override
        public byte[] getBytesIfAvailableOrSmallerThan(int maxSize) throws IOException, NoSuchPartException {
            byte[] bytes = getBytesIfAlreadyAvailable();
            if (bytes != null)
                return bytes;

            long len = getActualContentLength();
            if (len > (long)maxSize)
                return null;

            InputStream is = null;
            PoolByteArrayOutputStream baos = new PoolByteArrayOutputStream((int)len);
            try {
                is = stashManager.recall(ordinal);
                IOUtils.copyStream(is = decodeIfNecessary(is, false), baos);
                return baos.toByteArray();
            } finally {
                ResourceUtils.closeQuietly(baos);
                ResourceUtils.closeQuietly(is);
            }
        }

        /**
         * Get a previously stashed InputStream or, if there isn't one, ensure that the main input stream
         * is positioned to read this part's body.
         *
         * @return A previously-stashed InputStream, if there was one; or,
         *         null if and only if the main input stream is positioned to read this Part's body.
         * @throws IOException if there is a pending IOException from a previous operation on this
         *                        MimeBody or one of its parts, or
         *                     if there is a problem stashing or recalling from the StashManager
         * @throws NoSuchPartException if this Part's body has already been destructively read, or
         *                             if there is a pending NoSuchPartException from a previous operation on this
         *                               MimeBody or one of its parts, or
         *                             if the part was not stashed or consumed, but the main inputstream does not
         *                               appear to be positioned to read this part's body.
         *
         */
        protected InputStream preparePartInputStream() throws IOException, NoSuchPartException {
            checkErrorBoth();

            // See if we have one stashed already
            InputStream is = null;
            if (stashManager.peek(ordinal)) {
                try {
                    is = stashManager.recall(ordinal);
                    if (is == null)
                        throw new IllegalStateException("StashManager.recall() returned null");
                } catch (NoSuchPartException e) {
                    throw new CausedIllegalStateException("Peek succeeds but recall fails", e);
                }

                // Fall through and use this input stream

            } else {
                if (isBodyRead())
                    throw new NoSuchPartException("MIME multipart body has already been read, and was not saved");

                if (!moreParts)
                    throw new IllegalStateException("No more parts left in this stream"); // shouldn't happen here

                // Guarantee: if this PartInfo exists, its headers have been read.
                // So, either our mainInputStream is already positioned right at this Part's body content,
                // or someone has already destructively read this Part's body content.

                // Are we the current Part?
                if (partInfos.size() != ordinal + 1)
                    throw new IllegalStateException("Stream not positioned to read this part's body"); // shouldn't happen

                // Fall through and read next part from main input stream
            }
            return is;
        }

        @Override
        public MimeHeaders getHeaders() {
            return this.headers;
        }

        @Override
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

        @Override
        public long getActualContentLength() throws IOException, NoSuchPartException {
            getInputStream(false).close(); // force stash
            long size = stashManager.getSize(ordinal);
            if (size < 0)
                throw new IllegalStateException("Unable to determine length of stashed body");
            return size;
        }

        @Override
        public ContentTypeHeader getContentType() {
            return headers.getContentType();
        }

        @Override
        public String getContentId(boolean stripAngleBrackets) {
            return headers.getContentId(stripAngleBrackets);
        }

        /**
         * Is this part marked as validated (default is false).
         *
         * <p>For the first part the validation status has no special meaning
         * within the MimeBody class.</p>
         *
         * @return The validation status.
         */
        @Override
        public boolean isValidated() {
            return validated;
        }

        /**
         * Set the validation status for this part.
         *
         * <p>This can be used to filter out parts when re-serializing.</p>
         *
         * @param validated True if this part is validated.
         */
        @Override
        public void setValidated(boolean validated) {
            this.validated = validated;
        }

        /**
         * Stash the current part body from the specified input stream (assuming that everything read until EOF
         * is the part body), enforce that the declared content length was correct, and return the stashed
         * InputStream.
         *
         * @param is an InputStream that will return this part's body when read to EOF.
         * @throws IOException if the content-length header contains anything other than a decimal number, or
         *                     if the part had a content-length header that disagreed with what it's actual length
         *                        turned out to be, or
         *                     if the stashmanager throws IOExceptiona
         */
        void stashAndCheckContentLength(InputStream is) throws IOException {
            if (is == null) throw new NullPointerException();
            try {
                stashManager.stash(ordinal, is);
                // Replace content length with up-to-date version
                final long actualLength = MimeBody.this.stashManager.getSize(ordinal);
                final String clen = Long.toString(actualLength);
                if (headers.hasContentLength()) {
                    // Make sure the declared content length is accurate
                    long declaredLength = headers.getContentLength();
                    if (declaredLength != actualLength) {
                        String part = isMultipart() ? "MIME multipart message part #" + ordinal : "message";
                        throw new IOException(part + " declared in Content-Length header that size was " + declaredLength +
                                " bytes, but actual size was " + actualLength + " bytes");
                    }
                    headers.replace(new MimeHeader(MimeUtil.CONTENT_LENGTH, clen, null, false));
                }
            } catch (IOException e) {
                errorCondition = e;
                throw e;
            }
        }

        /**
         * Stash the current part body from the specified input stream (assuming that everything read until EOF
         * is the part body), enforce that the declared content length was correct, and return the stashed
         * InputStream.
         *
         * @param is an InputStream that will return this part's body when read to EOF.
         * @return the stashed and recalled InputStream for this part body, ready to read.  Never null.
         * @throws IOException if the content-length header contains anything other than a decimal number, or
         *                     if the part had a content-length header that disagreed with what it's actual length
         *                        turned out to be, or
         *                     if the stashmanager throws IOExceptiona
         */
        protected InputStream stashAndRecall(InputStream is) throws IOException {
            if (is == null) throw new NullPointerException();
            stashAndCheckContentLength(is);
            final InputStream got;
            try {
                got = stashManager.recall(ordinal);
            } catch (NoSuchPartException e) {
                throw new RuntimeException(e); // can't happen, illegal state
            }
            if (got == null) throw new IllegalStateException(); // can't happen
            return got;
        }

        private InputStream decodeIfNecessary( final InputStream in, boolean closeOnError ) throws IOException {
            final String contentTransferEncoding = getContentTransferEncoding();
            InputStream decodedStream = in;

            if ( !RAW_PARTS ) {
                if ( MimeUtil.isProcessedTransferEncoding(contentTransferEncoding) ) {
                    try {
                        decodedStream = MimeUtil.getDecodingInputStream( in, contentTransferEncoding );
                    } catch ( IOException e ) {
                        if ( closeOnError ) {
                            ResourceUtils.closeQuietly( in );
                        }
                        throw e;
                    }
                } else if ( MimeUtil.isIdentityTransferEncoding(contentTransferEncoding) ) {
                    // nothing to do
                } else {
                    if ( closeOnError ) {
                        ResourceUtils.closeQuietly( in );
                    }
                    throw new IOException( "Unsupported content transfer encoding '"+contentTransferEncoding+"'." );
                }
            }

            return decodedStream;
        }

        private String getContentTransferEncoding() {
            final MimeHeader contentTransferEncodingHeader = headers.get( MimeUtil.CONTENT_TRANSFER_ENCODING );
            return contentTransferEncodingHeader==null ?
                    MimeUtil.TRANSFER_ENCODING_DEFAULT :
                    contentTransferEncodingHeader.getMainValue().toLowerCase();
        }
    }

    private class FlaggingByteLimitInputStream extends ByteLimitInputStream {
        private boolean limitCustomized = false;

        private FlaggingByteLimitInputStream(InputStream mainInputStream) {
            super(mainInputStream, MimeBody.this.pushbackSize);
        }

        private FlaggingByteLimitInputStream(InputStream mainInputStream, int pushbackSize) {
            super(mainInputStream, pushbackSize);
        }

        @Override
        public void setSizeLimit(long newLimit) throws IOException {
            limitCustomized = true;
            super.setSizeLimit(newLimit);
        }

        private void setSizeLimitNonFlagging(long newLimit) throws IOException {
            if (!isLimitCustomized())
                super.setSizeLimit(newLimit);
        }

        private void clearLimitNonFlagging() throws IOException {
            if (!isLimitCustomized())
                super.setSizeLimit(0);
        }

        private boolean isLimitCustomized() {
            return limitCustomized;
        }
    }
}
