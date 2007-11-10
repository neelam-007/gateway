package com.l7tech.common.security.xml;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.io.InputStream;

import com.ibm.xml.dsig.Transform;
import com.ibm.xml.dsig.TransformContext;
import com.ibm.xml.dsig.TransformException;
import org.w3c.dom.Node;

import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.io.BufferPoolByteArrayOutputStream;
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
        return SoapUtil.TRANSFORM_ATTACHMENT_CONTENT;
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
        if (type != TransformContext.TYPE_BINARY && type != TransformContext.TYPE_URI)  {
            throw new TransformException("Expected binary input for transform ("+type+").");
        }

        byte[] data = transformContext.getOctets();
        ByteArrayInputStream partIn = new ByteArrayInputStream(data);
        BufferPoolByteArrayOutputStream canond = new BufferPoolByteArrayOutputStream(4096);

        //
        try {
            MimeHeaders headers = MimeUtil.parseHeaders(partIn);
            processHeaders(headers, canond);

            ContentTypeHeader cth;
            if ( headers.get(MimeUtil.CONTENT_TYPE) == null ) {
                cth = ContentTypeHeader.parseValue(VALUE_HEADER_CONTENT_TYPE);    
            } else {
                cth = headers.getContentType();
            }
            processBody(cth, partIn, canond);

            byte[] content = canond.toByteArray();

            transformContext.setContent(content, "application/octet-stream");
        } catch (IOException e) {
            throw (TransformException) new TransformException().initCause(e);
        } finally {
            canond.close();
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
     * <p>This implementation does nothing.</p>
     *
     * @param contentTypeHeader The body content type
     * @param bodyIn The MIME body stream
     * @param out The stream for canonical body output
     * @throws IOException on error reading/writing the body
     */
    protected void processBody(final ContentTypeHeader contentTypeHeader,
                               final InputStream bodyIn,
                               final OutputStream out) throws IOException {
        if (contentTypeHeader.isText()) {
            // transform line endings
            int character = -1;
            while ((character = bodyIn.read()) >= 0) {
                if (character == '\n') {
                    out.write(CRLF);
                } else {
                    out.write(character);
                }
            }
        } else {
            // no transform required
            HexUtils.copyStream(bodyIn, out);
        }
    }

    //- PRIVATE

    private String errorMessage = null;

}
