package com.l7tech.external.assertions.encodedecode.server;

import com.l7tech.common.io.CertUtils;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.AuditDetailMessage;
import com.l7tech.message.Message;
import com.l7tech.message.MimeKnob;
import com.l7tech.policy.assertion.MessageTargetableSupport;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.audit.LogOnlyAuditor;
import com.l7tech.external.assertions.encodedecode.EncodeDecodeAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.assertion.AssertionStatusException;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.Charsets;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.HexUtils;
import com.l7tech.util.IOUtils;
import com.l7tech.util.Pair;
import com.l7tech.util.ValidationUtils;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Server side implementation of the EncodeDecodeAssertion.
 *
 * @see com.l7tech.external.assertions.encodedecode.EncodeDecodeAssertion
 */
public class ServerEncodeDecodeAssertion extends AbstractServerAssertion<EncodeDecodeAssertion> {

    //- PUBLIC

    public ServerEncodeDecodeAssertion( final EncodeDecodeAssertion assertion,
                                        final ApplicationContext context ) throws PolicyAssertionException {
        super(assertion);
        this.auditor = context != null ? new Auditor(this, context, logger) : new LogOnlyAuditor(logger);
        Charset encoding;
        try {
            encoding = assertion.getCharacterEncoding()==null ? null : Charset.forName( assertion.getCharacterEncoding() );
        } catch ( IllegalArgumentException iae ) {
            encoding = Charsets.UTF8;
            logger.warning( "Unable to use charset '"+assertion.getCharacterEncoding()+"', falling back to UTF-8" );
        }
        ContentTypeHeader contentTypeHeader;
        try {
            contentTypeHeader = assertion.getTargetContentType()==null ? ContentTypeHeader.XML_DEFAULT : ContentTypeHeader.parseValue( assertion.getTargetContentType() );
        } catch ( IOException e ) {
            contentTypeHeader = ContentTypeHeader.XML_DEFAULT;
            logger.warning( "Unable to target Content-Type '"+assertion.getTargetContentType()+"', falling back to text/xml" );
        }
        this.encodeDecodeContext = new EncodeDecodeContext( auditor, assertion, encoding, contentTypeHeader );
    }

    @Override
    public AssertionStatus checkRequest( final PolicyEnforcementContext context ) throws IOException, PolicyAssertionException {
        final Map<String,Object> variables = context.getVariableMap( new String[]{ assertion.getSourceVariableName() }, auditor );
        final Object source = ExpandVariables.processSingleVariableAsObject( Syntax.SYNTAX_PREFIX + assertion.getSourceVariableName() + Syntax.SYNTAX_SUFFIX, variables, auditor );
        final Object result = encodeDecode( source );

        if ( result instanceof MessagePair ) {
            final MessagePair messageData = (MessagePair) result;
            Message output;
            try {
                output = context.getOrCreateTargetMessage( new MessageTargetableSupport( assertion.getTargetVariableName() ), false );
            } catch ( NoSuchVariableException e ) {
                auditor.logAndAudit( AssertionMessages.ENCODE_DECODE_ERROR, "Unable to create target message." );
                throw new AssertionStatusException( AssertionStatus.FAILED );
            }
            output.initialize( messageData.left, messageData.right, 0 );
        } else {
            context.setVariable( assertion.getTargetVariableName(), result );
        }

        return AssertionStatus.NONE;
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(ServerEncodeDecodeAssertion.class.getName());

    private final Auditor auditor;
    private final EncodeDecodeContext encodeDecodeContext;

    private Object encodeDecode( final Object source ) {
        final EncodeDecodeTransformer transformer = getTransformer( assertion.getTransformType(), encodeDecodeContext );
        return transformer.transform( source );
    }

    private EncodeDecodeTransformer getTransformer( final EncodeDecodeAssertion.TransformType transformType,
                                                    final EncodeDecodeContext encodeDecodeContext ) {
        final EncodeDecodeTransformer transformer;

        switch ( transformType ) {
            case BASE64_ENCODE:
                transformer = new Base64EncodingTransformer( encodeDecodeContext );
                break;
            case BASE64_DECODE:
                transformer = new Base64DecodingTransformer( encodeDecodeContext );
                break;
            case HEX_ENCODE:
                transformer = new HexEncodingTransformer( encodeDecodeContext );
                break;
            case HEX_DECODE:
                transformer = new HexDecodingTransformer( encodeDecodeContext );
                break;
            case URL_ENCODE:
                transformer = new UrlEncodingTransformer( encodeDecodeContext );
                break;
            case URL_DECODE:
                transformer = new UrlDecodingTransformer( encodeDecodeContext );
                break;
            default:
                auditor.logAndAudit( AssertionMessages.ENCODE_DECODE_ERROR, "Unknown transform requested '" + transformType + "'" );
                throw new AssertionStatusException( AssertionStatus.FAILED );
        }

        return transformer;
    }

    private static class EncodeDecodeContext {
        private final Auditor auditor;
        private final EncodeDecodeAssertion assertion;
        private final Charset encoding;
        private final ContentTypeHeader targetContentType;

        EncodeDecodeContext( final Auditor auditor,
                             final EncodeDecodeAssertion assertion,
                             final Charset encoding,
                             final ContentTypeHeader targetContentType ) {
            this.auditor = auditor;
            this.assertion = assertion;
            this.encoding = encoding;
            this.targetContentType = targetContentType;
        }

        Charset getEncoding() {
            if ( encoding == null ) fail("encoding required but not configured");
            return encoding;
        }

        boolean isStrict() {
            return assertion.isStrict();
        }

        Charset getInputEncoding() {
            return getEncoding();
        }

        Charset getOutputEncoding() {
            if ( encoding == null && targetContentType == null ) throw fail("output encoding required but not configured");
            return encoding != null ? encoding : targetContentType.getEncoding();
        }

        ContentTypeHeader getOutputContentType() {
            if ( targetContentType == null ) fail("content type required but not configured");
            return targetContentType;
        }

        DataType getOutputDataType() {
            return assertion.getTargetDataType() == null ? DataType.MESSAGE : assertion.getTargetDataType();
        }

        int getLineBreakInterval() {
            return assertion.getLineBreakInterval();
        }

        void audit( final AuditDetailMessage message, final String... parameters ) {
            audit( message, null, parameters );
        }

        void audit( final AuditDetailMessage message, final Exception e, final String... parameters ) {
            auditor.logAndAudit( message, parameters, e );
        }

        AssertionStatusException fail( final String message ) {
            return fail( AssertionMessages.ENCODE_DECODE_ERROR, new String[]{message}, null );
        }

        AssertionStatusException fail( final String message, final Exception e ) {
            return fail( AssertionMessages.ENCODE_DECODE_ERROR, new String[]{message}, e );
        }

        AssertionStatusException fail( final AuditDetailMessage detailMessage, final String[] params, final Exception e ) {
            auditor.logAndAudit( detailMessage, params, e );
            throw new AssertionStatusException(AssertionStatus.FAILED);
        }
    }

    private static class MessagePair extends Pair<ContentTypeHeader,byte[]>{
        MessagePair( final ContentTypeHeader left, final byte[] right ) {
            super( left, right );
        }
    }

    private static abstract class EncodeDecodeTransformer {
        private final EncodeDecodeContext encodeDecodeContext;

        EncodeDecodeTransformer( final EncodeDecodeContext encodeDecodeContext ) {
            this.encodeDecodeContext = encodeDecodeContext;
        }

        abstract Object transform( Object source );

        protected Charset getEncoding() {
            return encodeDecodeContext.getEncoding();
        }

        protected boolean isStrict() {
            return encodeDecodeContext.isStrict();
        }

        protected String getTextInput( final Object source ) {
            String text;
            if ( source instanceof Message ) {
                final Pair<Charset,byte[]> firstPartEncodingAndContent = getMessageFirstPart( (Message) source, true );
                text = new String( firstPartEncodingAndContent.right, firstPartEncodingAndContent.left );
            } else if ( source instanceof String ) {
                text = (String) source;
            } else {
                encodeDecodeContext.audit( AssertionMessages.ENCODE_DECODE_IN_TYPE, (source==null ? "<NULL>" : source.getClass().getName()), "text" );
                throw new AssertionStatusException( AssertionStatus.FALSIFIED );
            }
            return text;
        }

        protected Object buildTextOutput( final String value ){
            String formatted = value;
            final int lineBreak = encodeDecodeContext.getLineBreakInterval();
            if ( lineBreak > 0 && lineBreak < formatted.length() ) {
                final StringBuilder builder = new StringBuilder( formatted.length() + (formatted.length() / lineBreak) );
                for ( int i=0; i<formatted.length(); i++ ) {
                    if ( i>0 && i%lineBreak==0 ) builder.append( '\n' );
                    builder.append( formatted.charAt( i ) );
                }
                formatted = builder.toString();
            }

            Object output;
            final DataType outputDataType = encodeDecodeContext.getOutputDataType();
            if ( outputDataType == DataType.MESSAGE ) {
                output = new MessagePair( encodeDecodeContext.getOutputContentType(), formatted.getBytes( encodeDecodeContext.getOutputEncoding() ) );
            } else if ( outputDataType == DataType.STRING ) {
                output = formatted;
            } else {
                encodeDecodeContext.audit( AssertionMessages.ENCODE_DECODE_ERROR, "Cannot output text as X.509 Certificate" );
                throw new AssertionStatusException( AssertionStatus.FAILED );
            }

            return output;
        }

        protected byte[] getBinaryInput( final Object source ) {
            byte[] data;
            if ( source instanceof Message ) {
                data = getMessageFirstPart((Message) source, false).right;
            } else if ( source instanceof X509Certificate ) {
                try {
                    data = ((X509Certificate) source).getEncoded();
                } catch ( CertificateEncodingException e ) {
                    throw encodeDecodeContext.fail( "certificate error - " + ExceptionUtils.getMessage( e ), ExceptionUtils.getDebugException( e ) );
                }
            } else if ( source instanceof String ) {
                data = ((String) source).getBytes( encodeDecodeContext.getInputEncoding() );
            } else {
                encodeDecodeContext.audit( AssertionMessages.ENCODE_DECODE_IN_TYPE, (source==null ? "<NULL>" : source.getClass().getName()), "binary" );
                throw new AssertionStatusException( AssertionStatus.FALSIFIED );
            }

            return data;
        }

        protected Object buildBinaryOutput( final byte[] value ) {
            Object output;
            final DataType outputDataType = encodeDecodeContext.getOutputDataType();
            if ( outputDataType == DataType.MESSAGE ) {
                output = new MessagePair( encodeDecodeContext.getOutputContentType(), value );
            } else if ( outputDataType == DataType.CERTIFICATE ) {
                try {
                    output = CertUtils.decodeCert( value );
                } catch ( CertificateException e ) {
                    throw encodeDecodeContext.fail(
                            AssertionMessages.ENCODE_DECODE_OUT_TYPE,
                            new String[]{DataType.CERTIFICATE.getName(), ExceptionUtils.getMessage( e )},
                            ExceptionUtils.getDebugException( e ) );
                }
            } else if (  outputDataType == DataType.STRING ) {
                output = new String( value, encodeDecodeContext.getOutputEncoding() );
            } else {
                throw encodeDecodeContext.fail( "unsupported output type " + outputDataType );
            }
            return output;
        }

        protected void audit( final AuditDetailMessage message,
                         final Exception exception,
                         final String... parameters ) {
            encodeDecodeContext.audit( message, exception, parameters );
        }

        private Pair<Charset,byte[]> getMessageFirstPart( final Message message, final boolean requireText ) {
            Pair<Charset,byte[]> content;
            try {
                final MimeKnob mimeKnob = message.getKnob(MimeKnob.class);
                if ( mimeKnob != null ) {
                    final ContentTypeHeader contentType = mimeKnob.getFirstPart().getContentType();
                    if ( requireText && !contentType.isTextualContentType()) {
                        audit( AssertionMessages.ENCODE_DECODE_IN_ACCESS, null, "Message", "non-text content" );
                        throw new AssertionStatusException( AssertionStatus.FALSIFIED );
                    }
                    // TODO maximum size? This could be huge and OOM
                    final byte[] bytes = IOUtils.slurpStream(mimeKnob.getFirstPart().getInputStream(false));
                    content = new Pair<Charset,byte[]>(contentType.getEncoding(), bytes);
                } else {
                    audit( AssertionMessages.ENCODE_DECODE_IN_ACCESS, null, "Message", "not initialized" );
                    throw new AssertionStatusException( AssertionStatus.FALSIFIED );
                }
            } catch (IOException e) {
                audit( AssertionMessages.ENCODE_DECODE_IN_ACCESS, ExceptionUtils.getDebugException(e), "Message", ExceptionUtils.getMessage(e) );
                throw new AssertionStatusException( AssertionStatus.FAILED );
            } catch (NoSuchPartException e) {
                audit( AssertionMessages.ENCODE_DECODE_IN_ACCESS, ExceptionUtils.getDebugException(e), "Message", ExceptionUtils.getMessage(e) );
                throw new AssertionStatusException( AssertionStatus.FAILED );
            }
            return content;
        }
    }

    private static class Base64EncodingTransformer extends EncodeDecodeTransformer {
        Base64EncodingTransformer( final EncodeDecodeContext encodeDecodeContext ) {
            super( encodeDecodeContext );
        }

        @Override
        Object transform( final Object source ) {
            final byte[] data = getBinaryInput( source );
            final String encoded = HexUtils.encodeBase64( data, true );
            return buildTextOutput( encoded );
        }
    }

    private static class Base64DecodingTransformer extends EncodeDecodeTransformer {
        private static final String BASE64_CHARACTERS = ValidationUtils.ALPHA_NUMERIC + "+/= \t\n\r";

        Base64DecodingTransformer( final EncodeDecodeContext encodeDecodeContext ) {
            super( encodeDecodeContext );
        }

        @Override
        Object transform( final Object source ) {
            final String data = getTextInput( source );
            if ( isStrict() && !ValidationUtils.isValidCharacters( data, BASE64_CHARACTERS ) ) {
                audit( AssertionMessages.ENCODE_DECODE_STRICT, null );
                throw new AssertionStatusException( AssertionStatus.FALSIFIED );
            }
            final byte[] decoded = HexUtils.decodeBase64( data, true );
            return buildBinaryOutput( decoded );
        }
    }

    private static class HexEncodingTransformer extends EncodeDecodeTransformer {
        HexEncodingTransformer( final EncodeDecodeContext encodeDecodeContext ) {
            super( encodeDecodeContext );
        }

        @Override
        Object transform( final Object source ) {
            final byte[] data = getBinaryInput( source );
            final String encoded = HexUtils.hexDump( data, 0, data.length, true );
            return buildTextOutput( encoded );
        }
    }

    private static class HexDecodingTransformer extends EncodeDecodeTransformer {
        private static final String HEX_CHARACTERS = ValidationUtils.DIGITS + "abcdefABCDEF:% \t\n\r";  // not any standard

        HexDecodingTransformer( final EncodeDecodeContext encodeDecodeContext ) {
            super( encodeDecodeContext );
        }

        @Override
        Object transform( final Object source ) {
            final String data = getTextInput( source );
            if ( isStrict() && !ValidationUtils.isValidCharacters( data, HEX_CHARACTERS ) ) {
                audit( AssertionMessages.ENCODE_DECODE_STRICT, null );
                throw new AssertionStatusException( AssertionStatus.FALSIFIED );
            }

            // remove non-hex characters
            final StringBuilder builder = new StringBuilder();
            for ( int i=0; i< data.length(); i++ ) {
                char character = data.charAt( i );
                if ( (character >= '0' && character <= '9') ||
                     (character >= 'A' && character <= 'F') ||
                     (character >= 'a' && character <= 'f') ) {
                    builder.append( character );
                }
            }

            // decode
            final byte[] decoded;
            try {
                decoded = HexUtils.unHexDump( builder.toString() );
            } catch ( IOException e ) {
                audit( AssertionMessages.ENCODE_DECODE_ERROR, null, ExceptionUtils.getMessage(e) );
                throw new AssertionStatusException( AssertionStatus.FALSIFIED );
            }

            return buildBinaryOutput( decoded );
        }
    }

    private static class UrlEncodingTransformer extends EncodeDecodeTransformer {
        UrlEncodingTransformer( final EncodeDecodeContext encodeDecodeContext ) {
            super( encodeDecodeContext );
        }

        @Override
        Object transform( final Object source ) {
            final String data = getTextInput( source );
            final String encoded;
            try {
                encoded = URLEncoder.encode( data, getEncoding().name() );
            } catch ( IOException e ) {
                audit( AssertionMessages.ENCODE_DECODE_ERROR, ExceptionUtils.getDebugException(e), ExceptionUtils.getMessage(e) );
                throw new AssertionStatusException( AssertionStatus.FAILED );
            }
            return buildTextOutput( encoded );
        }
    }

    private static class UrlDecodingTransformer extends EncodeDecodeTransformer {
        UrlDecodingTransformer( final EncodeDecodeContext encodeDecodeContext ) {
            super( encodeDecodeContext );
        }

        @Override
        Object transform( final Object source ) {
            final String data = getTextInput( source );
            final String decoded;
            try {
                decoded = URLDecoder.decode( data, getEncoding().name() );
            } catch ( IOException e ) {
                audit( AssertionMessages.ENCODE_DECODE_ERROR, ExceptionUtils.getDebugException(e), ExceptionUtils.getMessage(e) );
                throw new AssertionStatusException( AssertionStatus.FAILED );
            }
            return buildTextOutput( decoded );
        }
    }
}
