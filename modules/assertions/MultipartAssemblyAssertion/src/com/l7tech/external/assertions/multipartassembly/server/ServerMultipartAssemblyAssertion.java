package com.l7tech.external.assertions.multipartassembly.server;

import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.common.io.EmptyInputStream;
import com.l7tech.common.io.IOExceptionThrowingInputStream;
import com.l7tech.common.message.Message;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.mime.StashManager;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.IteratorEnumeration;
import com.l7tech.common.util.ResourceUtils;
import com.l7tech.external.assertions.multipartassembly.MultipartAssemblyAssertion;
import static com.l7tech.external.assertions.multipartassembly.MultipartAssemblyAssertion.*;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import org.springframework.context.ApplicationContext;

import java.io.*;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.*;
import java.util.logging.Logger;

/**
 * Server side implementation of the MultipartAssemblyAssertion.
 *
 * @see com.l7tech.external.assertions.multipartassembly.MultipartAssemblyAssertion
 */
public class ServerMultipartAssemblyAssertion extends AbstractServerAssertion<MultipartAssemblyAssertion> {
    private static final Logger logger = Logger.getLogger(ServerMultipartAssemblyAssertion.class.getName());

    private static final Random random = new SecureRandom();
    private final StashManagerFactory stashManagerFactory;
    private final Auditor auditor;
    private final String payloadsVariable;
    private final String contentTypesVariable;
    private final String partIdsVariable;

    public ServerMultipartAssemblyAssertion(MultipartAssemblyAssertion assertion, ApplicationContext context) throws PolicyAssertionException {
        super(assertion);

        //noinspection ThisEscapedInObjectConstruction
        this.auditor = new Auditor(this, context, logger);
        this.stashManagerFactory = (StashManagerFactory)context.getBean("stashManagerFactory", StashManagerFactory.class);
        String prefix = assertion.getVariablePrefix();
        payloadsVariable = prefix + SUFFIX_PAYLOADS;
        contentTypesVariable = prefix + SUFFIX_CONTENT_TYPES;
        partIdsVariable = prefix + SUFFIX_PART_IDS;
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        try {
            Message message = assertion.isActOnRequest() ? context.getRequest() : context.getResponse();

            List payloads = getCollectionVariable(context, payloadsVariable);
            List contentTypes = getCollectionVariable(context, contentTypesVariable);
            List partIds = getCollectionVariable(context, partIdsVariable);

            final int numPayloads = payloads.size();
            if (numPayloads < 1) {
                logger.info("Payloads collection is empty.  Leaving message unchanged.");
                return AssertionStatus.NONE;
            }

            final int numContentTypes = contentTypes.size();
            if (numPayloads > numContentTypes) {
                auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO,
                                    new String[] { payloadsVariable + " has " + numPayloads + " payloads, but " + contentTypesVariable + " only has " + numContentTypes + " content types" },
                                    null);
                return AssertionStatus.FAILED;
            }

            final int numPartIds = partIds.size();
            if (numPayloads > numPartIds) {
                auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO,
                                    new String[] { payloadsVariable + " has " + numPayloads + " payloads, but " + partIdsVariable + " only has " + numPartIds + " part IDs" },
                                    null);
                return AssertionStatus.FAILED;
            }

            stashPartsAndBuildMultipartMessage(context, message, payloads, contentTypes, partIds);
            return AssertionStatus.NONE;

        } catch (NoSuchVariableException e) {
            logFailure(e);
            return AssertionStatus.FAILED;
        } catch (NoSuchPartException e) {
            logFailure(e);
            return AssertionStatus.SERVER_ERROR;
        } catch (BadContentTypeHeaderException e) {
            logFailure(e);
            return AssertionStatus.FAILED;
        } catch (BadPayloadException e) {
            logFailure(e);
            return AssertionStatus.FAILED;
        } catch (BadPartIdException e) {
            logFailure(e);
            return AssertionStatus.FAILED;
        } catch (BadPayloadValueException e) {
            logFailure(e);
            return AssertionStatus.FAILED;
        }
    }

    private void stashPartsAndBuildMultipartMessage(PolicyEnforcementContext context,
                                                    Message message,
                                                    List payloads,
                                                    List contentTypes,
                                                    List partIds)
            throws IOException, NoSuchPartException, BadContentTypeHeaderException, BadPartIdException, BadPayloadException
    {
        StashManager sm = null;
        try {
            // Stash the current message, whatever it is
            sm = stashManagerFactory.createStashManager();
            stashMessage(sm, message);
            ContentTypeHeader origOutCtype = message.getMimeKnob().getOuterContentType();
            final List<ContentTypeHeader> contentTypeHeaders = new ArrayList<ContentTypeHeader>();
            contentTypeHeaders.add(origOutCtype);
            final List<String> partIdStrings = new ArrayList<String>();
            final String startPartId = makePartId();
            partIdStrings.add(startPartId);
            final ContentTypeHeader multipartRelated = makeMultipartRelated(startPartId);

            // Stash each attachment, checking content IDs and part IDs as we go
            for (int i = 0; i < payloads.size(); i++) {
                Object payloadObj = payloads.get(i);
                int ordinal = i + 1; // ordinal zero is the entire original message, before we wrapped it

                ContentTypeHeader ctype = getContentType(contentTypes.get(i));
                String encoding = ctype.getParam("charset") == null ? null : ctype.getEncoding(); // avoid defaulting to ISO8859-1
                contentTypeHeaders.add(ctype);

                partIdStrings.add(getPartIdString(partIds.get(i)));

                stashInputStream(sm, ordinal, getPayloadInputStream(payloadObj, encoding));
            }

            message.initialize(stashManagerFactory.createStashManager(),
                               multipartRelated,
                               makeBodyInputStream(multipartRelated, sm, contentTypeHeaders, partIdStrings));

            transferOwnershipToContext(context, sm);
            sm = null;
        } finally {
            if (sm != null) sm.close();
        }
    }

    // Transfers ownership of the specified stashmanager to the context, so it will be closed when the context is closed
    private static void transferOwnershipToContext(PolicyEnforcementContext context, StashManager sm) {
        // Hand off our stash manager to the context to close, since it now must survive past this checkRequest method
        final StashManager toClose = sm;
        context.runOnClose(new Runnable() {
            public void run() {
                if (toClose != null) toClose.close();
            }
        });
    }

    static InputStream makeBodyInputStream(final ContentTypeHeader multipartRelated,
                                            final StashManager stashManager,
                                            final List<ContentTypeHeader> contentTypeHeaders,
                                            final List<String> partIdStrings)
    {

        Enumeration<InputStream> enumeration = new Enumeration<InputStream>() {
            private String boundary = multipartRelated.getMultipartBoundary();
            private IOException errorCondition = null;
            private int nextPart = 0; // the part which is being sent
            private final int numParts = contentTypeHeaders.size();

            public boolean hasMoreElements() {
                return nextPart < numParts;
            }

            public InputStream nextElement() {
                try {
                    return doNextElement();
                } catch (IOException e) {
                    nextPart = numParts;
                    errorCondition = e;
                    return new IOExceptionThrowingInputStream(e);
                } catch (NoSuchPartException e) {
                    nextPart = numParts;
                    errorCondition = new IOException(e);
                    return new IOExceptionThrowingInputStream(errorCondition);
                }
            }

            private InputStream doNextElement() throws IOException, NoSuchPartException {
                if (errorCondition != null)
                    return new IOExceptionThrowingInputStream(errorCondition);

                // Generate the next input stream for the user to read.
                if (nextPart >= numParts)
                    throw new NoSuchElementException();

                final InputStream openingBoundary = boundaryStream(boundary);
                final InputStream partHeaders = headersStream(nextPart);
                final InputStream partBody = stashManager.recall(nextPart);
                nextPart++;
                final InputStream finalBoundary = nextPart >= numParts ? finalBoundaryStream(boundary) : empty();

                return sequence(openingBoundary, partHeaders, partBody, finalBoundary);
            }

            private InputStream headersStream(int nextPart) throws UnsupportedEncodingException {
                StringBuilder headers = new StringBuilder(100);
                headers.append("Content-Type: ").append(contentTypeHeaders.get(nextPart).getFullValue()).append("\r\n");
                long contentLength = stashManager.getSize(nextPart);
                headers.append("Content-Length: ").append(contentLength).append("\r\n");
                headers.append("Content-ID: ").append(partIdStrings.get(nextPart)).append("\r\n");
                headers.append("\r\n");
                return new ByteArrayInputStream(headers.toString().getBytes("ISO8859-1"));
            }
        };

        return new SequenceInputStream(enumeration);
    }

    private static ByteArrayInputStream finalBoundaryStream(String boundary) {
        return new ByteArrayInputStream(("\r\n--" + boundary + "--\r\n").getBytes());
    }

    private static ByteArrayInputStream boundaryStream(String boundary) {
        return new ByteArrayInputStream(("\r\n--" + boundary + "\r\n").getBytes());
    }
    private static InputStream empty() {
        return new EmptyInputStream();
    }

    private static InputStream sequence(InputStream... in) {
        Enumeration<InputStream> en = new IteratorEnumeration<InputStream>(Arrays.asList(in).iterator());
        return new SequenceInputStream(en);
    }

    static ContentTypeHeader makeMultipartRelated(String startId) throws IOException {
        final String boundary = makeBoundary();
        final String hdr = "multipart/related; boundary=\"" + boundary + "\"; start=\"" + startId + '\"';
        return ContentTypeHeader.parseValue(hdr);
    }

    static String makeBoundary() {
        StringBuilder sb = new StringBuilder(50);
        for (int i = 0; i < 5; i++)
            sb.append('-');
        sb.append("=_");
        for (int i = 0; i < 36; i++)
            sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
        return sb.toString();
    }

    private static final String CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ01234567890";

    static String makePartId() {
        StringBuilder sb = new StringBuilder(50);
        for (int i = 0; i < 15; i++)
            sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
        return sb.toString();
    }

    private void logFailure(Throwable e) {
        auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO,
                            new String[] { "Unable to assemble multipart message: " + ExceptionUtils.getMessage(e) },
                            e);
    }

    private static class BadPayloadException extends Exception {
        private BadPayloadException(String message) {
            super(message);
        }
    }

    // Attempt to convert the specified object into an attachment byte stream encoded using the specified encoding, or UTF-8 if no encoding is specified.
    private static InputStream getPayloadInputStream(Object payloadObj, String encoding) throws UnsupportedEncodingException, BadPayloadException {
        if (payloadObj == null)
            throw new BadPayloadException("Payload is null");

        //noinspection ChainOfInstanceofChecks,IfStatementWithTooManyBranches
        if (payloadObj instanceof byte[]) {
            byte[] bytes = (byte[])payloadObj;
            return new ByteArrayInputStream(bytes);
        } else if (payloadObj instanceof CharSequence) {
            CharSequence charSequence = (CharSequence)payloadObj;
            byte[] bytes = encoding != null ? charSequence.toString().getBytes(encoding) : charSequence.toString().getBytes("UTF-8");
            return new ByteArrayInputStream(bytes);
        } else if (payloadObj instanceof ByteBuffer) {
            ByteBuffer buf = (ByteBuffer)payloadObj;
            return new ByteArrayInputStream(buf.array());
        } else if (payloadObj instanceof InputStream) {
            return (InputStream)payloadObj;
        } else
            throw new BadPayloadException("Unsupport payload format: " + payloadObj.getClass());
    }

    private static class BadContentTypeHeaderException extends Exception {
        private BadContentTypeHeaderException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    // Attempt to interpret the specified object as a content type.
    private static ContentTypeHeader getContentType(Object ctype) throws BadContentTypeHeaderException {
        if (ctype == null)
            throw new BadContentTypeHeaderException("Content type value is null", null);
        //noinspection ChainOfInstanceofChecks
        if (ctype instanceof ContentTypeHeader) {
            return (ContentTypeHeader)ctype;
        } else if (ctype instanceof CharSequence) {
            CharSequence charSequence = (CharSequence)ctype;
            try {
                return ContentTypeHeader.parseValue(charSequence.toString());
            } catch (IOException e) {
                throw new BadContentTypeHeaderException("Bad content type header value: " + charSequence, e);
            }
        } else
            throw new BadContentTypeHeaderException("Unsupport content type header value format: " + ctype.getClass(), null);
    }

    private static class BadPartIdException extends Exception {
        private BadPartIdException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private static String getPartIdString(Object o) throws BadPartIdException {
        if (o == null)
            throw new BadPartIdException("Part ID value is null", null);
        if (o instanceof CharSequence) {
            CharSequence charSequence = (CharSequence)o;
            return charSequence.toString();
        } else
            throw new BadPartIdException("Unsupported part ID value format: " + o.getClass(), null);
    }

    private static class BadPayloadValueException extends Exception {
        private BadPayloadValueException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    // Returns a Collection view of a multivalued context variable
    private static List getCollectionVariable(PolicyEnforcementContext context, String variableName) throws NoSuchVariableException, BadPayloadValueException {
        Object valueObj = context.getVariable(variableName);
        //noinspection ChainOfInstanceofChecks,IfStatementWithTooManyBranches
        if (valueObj instanceof Object[]) {
            Object[] objects = (Object[])valueObj;
            return Arrays.asList(objects);
        } else if (valueObj instanceof List) {
            return (List)valueObj;
        } else if (valueObj instanceof Collection) {
            final Collection c = (Collection)valueObj;
            //noinspection unchecked
            return new ArrayList(c);
        } else {
            throw new BadPayloadValueException("The value of the context variable '" + variableName + "' was neither an Object array nor a Collection", null);
        }
    }

    // Stashes the input stream and closes it when finishes
    private static void stashInputStream(StashManager sm, int ordinal, InputStream is) throws IOException {
        try {
            sm.stash(ordinal, is);
        } finally {
            ResourceUtils.closeQuietly(is);
        }
    }

    // Stashes an entire message into ordinal zero of the specified StashManager
    private static void stashMessage(StashManager sm, Message message) throws IOException, NoSuchPartException {
        InputStream is = null;
        try {
            is = message.getMimeKnob().getEntireMessageBodyAsInputStream();
            sm.stash(0, is);
        } finally {
            ResourceUtils.closeQuietly(is);
        }
    }
}
