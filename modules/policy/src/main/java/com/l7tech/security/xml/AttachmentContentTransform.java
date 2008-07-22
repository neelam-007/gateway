package com.l7tech.security.xml;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.SequenceInputStream;
import java.io.FilterInputStream;

import com.ibm.xml.dsig.Transform;
import com.ibm.xml.dsig.TransformContext;
import com.ibm.xml.dsig.TransformException;
import com.ibm.xml.dsig.StreamingTransformContext;
import org.w3c.dom.Node;

import com.l7tech.util.ResourceUtils;
import com.l7tech.util.BufferPool;
import com.l7tech.util.SoapConstants;
import com.l7tech.common.mime.MimeUtil;
import com.l7tech.common.mime.MimeHeaders;
import com.l7tech.common.mime.ContentTypeHeader;

/**
 * Transform to canonicalize MIME attachment contents.
 *
 * <p>See Attachment-Content-Only Reference Transform section in Web Services
 * Security SOAP Messages with Attachments (SwA) Profile 1.0.</p>
 *
 * @author Steve Jones
 */
public class AttachmentContentTransform extends Transform {

     //- PUBLIC

    /**
     * Get the URI that identifies this transform.
     *
     * @return The transform identifier.
     */
    public String getURI() {
        return SoapConstants.TRANSFORM_ATTACHMENT_CONTENT;
    }

    /**
     * This transform has no parameters.
     *
     * <p>Setting parameters on this transform will result in an error when
     * the transform is used.</p>
     *
     * @param node The parameter node
     * @see #transform(TransformContext)
     */
    public void setParameter(final Node node) {
        if (node != null) {
            errorMessage = "Unexpected parameters for attachment transform.";
        }
    }

    /**
     * Transform the given transform context.
     *
     * @param transformContext The context with the data to transform.
     * @throws TransformException if an error occurs.
     */
    public void transform(final TransformContext transformContext) throws TransformException {
        if (errorMessage != null)
            throw new TransformException(errorMessage);

        final int type = transformContext.getType();
        if ((type != TransformContext.TYPE_BINARY &&
             type != TransformContext.TYPE_URI &&
             type != StreamingTransformContext.TYPE_STREAM) ||
            !(transformContext instanceof StreamingTransformContext)  )  {
            throw new TransformException("Expected streaming input for transform ("+type+").");
        }

        StreamingTransformContext streamingTransformContext = (StreamingTransformContext) transformContext;

        //
        InputStream partIn = null;
        try {
            partIn = streamingTransformContext.getInputStream();
            ByteArrayOutputStream canondHeaders = new ByteArrayOutputStream(1024);

            MimeHeaders headers = MimeUtil.parseHeaders(partIn);
            processHeaders(headers, canondHeaders);

            ContentTypeHeader cth;
            if ( headers.get(MimeUtil.CONTENT_TYPE) == null ) {
                cth = ContentTypeHeader.parseValue(VALUE_HEADER_CONTENT_TYPE);    
            } else {
                cth = headers.getContentType();
            }
            byte[] headerContent = canondHeaders.toByteArray();

            //noinspection IOResourceOpenedButNotSafelyClosed
            streamingTransformContext.setContent(
                    new SequenceInputStream(
                            new ByteArrayInputStream(headerContent),
                            processBody(cth, partIn)),
                    null);

            partIn = null;
        } catch (IOException e) {
            throw (TransformException) new TransformException().initCause(e);
        } finally {
            ResourceUtils.closeQuietly(partIn);
        }
    }

    //- PROTECTED

    /**
     * Default content-type header in canonical form
     */
    protected static final String VALUE_HEADER_CONTENT_TYPE = "text/plain;charset=\"us-ascii\"";

    /**
     * CRLF byte sequence
     */
    protected static final byte[] CRLF = {13, 10};

    /**
     * Process MIME headers.
     *
     * <p>This implementation does nothing.</p>
     *
     * @param headers The headers to transform
     * @param out The stream for canonical header output
     * @throws IOException on error reading/writing headers
     * @throws TransformException if the headers cannot be transformed
     */
    protected void processHeaders(final MimeHeaders headers,
                                  final OutputStream out) throws IOException, TransformException {
    }

    /**
     * Process MIME body.
     *
     * <p>Can be implemented as a filtering stream.</p>
     *
     * @param contentTypeHeader The type of the content.
     * @param bodyIn The body input stream
     * @return The (possibly replaced) body input stream
     */
    protected InputStream processBody(final ContentTypeHeader contentTypeHeader,
                                      final InputStream bodyIn) throws IOException {
        return new BodyInputStream(contentTypeHeader, bodyIn);
    }

    //- PRIVATE

    private String errorMessage = null;

    /**
     * InputStream to process MIME body.
     *
     * <p>This will fiter any text content to canonical form '\n' -> '\r\n'</p>
     */
    private static class BodyInputStream extends FilterInputStream {
        boolean isText = false;
        byte lastByte;

        private BodyInputStream(final ContentTypeHeader contentTypeHeader,
                               final InputStream bodyIn) throws IOException {
            super(bodyIn);

            if (contentTypeHeader.isText()) {
                // transform line endings
                isText = true;
            }
        }

        /**
         * Disable unless we need this 
         */
        public int read() throws IOException {
            throw new IOException("read() not supported.");
        }

        /**
         * For text input streams rewrite any '\n' as '\r\n' 
         */
        public int read(byte b[], int off, int len) throws IOException {
            if (isText) {
                if (len < 2)
                    throw new IOException("read of <2 bytes not supported.");

                byte prevByte = lastByte;
                final int count =  super.read(b, off, len / CRLF.length); // ensure space for CRLF expansion

                if (count > 0 ) {
                    byte[] source = BufferPool.getBuffer(b.length);
                    System.arraycopy(b, off, source, 0, count);

                    int writePos = 0;
                    for (int n=0; n<count; n++) {
                        byte read = source[off+n];

                        if (prevByte!='\r' && read == '\n') {
                            for(int i=0; i<CRLF.length; i++) {
                                b[off+(writePos++)] = CRLF[i];
                            }
                            prevByte = '\n';
                        } else {
                            b[off+(writePos++)] = read;
                            prevByte = read;
                        }
                    }

                    BufferPool.returnBuffer(source);

                    lastByte = b[off+(writePos-1)];

                    return writePos;
                } else {
                    return count;
                }
            }

            return super.read(b, off, len);
        }
    }
}
