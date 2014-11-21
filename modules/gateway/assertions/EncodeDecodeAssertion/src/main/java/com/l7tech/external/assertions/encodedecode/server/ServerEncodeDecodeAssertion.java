package com.l7tech.external.assertions.encodedecode.server;

import com.l7tech.common.io.CertUtils;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.mime.PartInfo;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.gateway.common.audit.AuditDetailMessage;
import com.l7tech.message.Message;
import com.l7tech.message.MimeKnob;
import com.l7tech.policy.assertion.MessageTargetableSupport;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.external.assertions.encodedecode.EncodeDecodeAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.assertion.AssertionStatusException;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.zip.*;

/**
 * Server side implementation of the EncodeDecodeAssertion.
 *
 * @see com.l7tech.external.assertions.encodedecode.EncodeDecodeAssertion
 */
public class ServerEncodeDecodeAssertion extends AbstractServerAssertion<EncodeDecodeAssertion> {

    //- PUBLIC

    public ServerEncodeDecodeAssertion( final EncodeDecodeAssertion assertion ) throws PolicyAssertionException {
        super(assertion);
        Charset encoding;
        try {
            encoding = assertion.getCharacterEncoding()==null ? null : Charset.forName( assertion.getCharacterEncoding() );
        } catch ( IllegalArgumentException iae ) {
            encoding = Charsets.UTF8;
            logger.warning( "Unable to use charset '"+assertion.getCharacterEncoding()+"', falling back to UTF-8" );
        }
        ContentTypeHeader contentTypeHeader;
        contentTypeHeader = assertion.getTargetContentType()==null ? ContentTypeHeader.XML_DEFAULT : ContentTypeHeader.create(assertion.getTargetContentType());
        this.encodeDecodeContext = new EncodeDecodeContext( getAudit(), assertion, encoding, contentTypeHeader );
    }

    @Override
    public AssertionStatus checkRequest( final PolicyEnforcementContext context ) throws IOException, PolicyAssertionException {
        final Map<String,Object> variables = context.getVariableMap( new String[]{ assertion.getSourceVariableName() }, getAudit() );
        final Object source = ExpandVariables.processSingleVariableAsObject( Syntax.SYNTAX_PREFIX + assertion.getSourceVariableName() + Syntax.SYNTAX_SUFFIX, variables, getAudit() );
        final Object result = encodeDecode( source );

        if ( result instanceof MessagePair ) {
            final MessagePair messageData = (MessagePair) result;
            Message output;
            try {
                output = context.getOrCreateTargetMessage( new MessageTargetableSupport( assertion.getTargetVariableName() ), false );
            } catch ( NoSuchVariableException e ) {
                logAndAudit( AssertionMessages.ENCODE_DECODE_ERROR, "Unable to create target message." );
                throw new AssertionStatusException( AssertionStatus.FAILED );
            }
            output.initialize( messageData.left, messageData.right );
        } else {
            context.setVariable( assertion.getTargetVariableName(), result );
        }

        return AssertionStatus.NONE;
    }

    //- PRIVATE

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
            case ZIP:
                transformer = new ZipCompressingTransformer( encodeDecodeContext );
                break;
            case UNZIP:
                transformer = new ZipDecompressingTransformer( encodeDecodeContext );
                break;
            case GZIP:
                transformer = new GzipCompressingTransformer( encodeDecodeContext );
                break;
            case GUNZIP:
                transformer = new GzipDecompressingTransformer( encodeDecodeContext );
                break;
            default:
                logAndAudit( AssertionMessages.ENCODE_DECODE_ERROR, "Unknown transform requested '" + transformType + "'" );
                throw new AssertionStatusException( AssertionStatus.FAILED );
        }

        return transformer;
    }

    private static class EncodeDecodeContext {
        private final Audit auditor;
        private final EncodeDecodeAssertion assertion;
        private final Charset encoding;
        private final ContentTypeHeader targetContentType;

        EncodeDecodeContext( final Audit auditor,
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
        final EncodeDecodeContext encodeDecodeContext;

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
            } else if (source instanceof PartInfo) {
                data = getPartInfoBody((PartInfo) source, false).right;
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

        private Pair<Charset,byte[]> getPartInfoBody( PartInfo partInfo, boolean requireText ) {
            Pair<Charset,byte[]> content;
            try {
                final ContentTypeHeader contentType = partInfo.getContentType();
                if ( requireText && !contentType.isTextualContentType()) {
                    audit( AssertionMessages.ENCODE_DECODE_IN_ACCESS, null, "Message", "non-text content" );
                    throw new AssertionStatusException( AssertionStatus.FALSIFIED );
                }
                // TODO maximum size? This could be huge and OOM
                final byte[] bytes = IOUtils.slurpStream(partInfo.getInputStream(false));
                content = new Pair<Charset,byte[]>(contentType.getEncoding(), bytes);
            } catch (IOException e) {
                audit( AssertionMessages.ENCODE_DECODE_IN_ACCESS, ExceptionUtils.getDebugException(e), "Message", ExceptionUtils.getMessage(e) );
                throw new AssertionStatusException( AssertionStatus.FAILED );
            } catch (NoSuchPartException e) {
                audit( AssertionMessages.ENCODE_DECODE_IN_ACCESS, ExceptionUtils.getDebugException(e), "Message", ExceptionUtils.getMessage(e) );
                throw new AssertionStatusException( AssertionStatus.FAILED );
            }
            return content;
        }

        private Pair<Charset,byte[]> getMessageFirstPart( final Message message, final boolean requireText ) {
            Pair<Charset,byte[]> content;
            try {
                final MimeKnob mimeKnob = message.getKnob(MimeKnob.class);
                if ( mimeKnob != null && message.isInitialized() ) {
                    content = getPartInfoBody(mimeKnob.getFirstPart(), requireText);
                } else {
                    audit( AssertionMessages.ENCODE_DECODE_IN_ACCESS, null, "Message", "not initialized" );
                    throw new AssertionStatusException( AssertionStatus.FALSIFIED );
                }
            } catch (IOException e) {
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
            final String data = getTextInput( source ).trim();
            // if there is padding nothing must be trailing it
            if ( isStrict() && (!ValidationUtils.isValidCharacters( data, BASE64_CHARACTERS )
                    || (data.contains("=") && data.lastIndexOf("=") != data.length() - 1)))  {
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

    private static class ZipCompressingTransformer extends EncodeDecodeTransformer {
        ZipCompressingTransformer( EncodeDecodeContext encodeDecodeContext ) {
            super( encodeDecodeContext );
        }

        @Override
        Object transform( Object source ) {
            // TODO support streaming input; get InputStream to transform instead of byte array
            byte[] data = getBinaryInput( source );
            ByteArrayInputStream in = new ByteArrayInputStream( data );
            PoolByteArrayOutputStream out = new PoolByteArrayOutputStream();
            ZipOutputStream zos = new ZipOutputStream( out, Charsets.UTF8 );

            // TODO we use the source variable name as the filename, which many might find... suprrising,
            // but seems better than just always hardcoding it
            String filename = encodeDecodeContext.assertion.getSourceVariableName();
            if ( filename == null || filename.length() > 0 )
                filename = "contents.dat";

            byte[] compressed;
            try {
                zos.putNextEntry( new ZipEntry( filename ) );
                IOUtils.copyStream( in, zos );
                zos.finish();
                zos.flush();
                compressed = out.toByteArray();
            } catch ( IOException e ) {
                audit( AssertionMessages.ENCODE_DECODE_ERROR, ExceptionUtils.getDebugException( e ), ExceptionUtils.getMessage( e ) );
                throw new AssertionStatusException( AssertionStatus.FAILED );
            } finally {
                ResourceUtils.closeQuietly( out );
            }

            // TODO support copy avoidance; initialize output Message with ByteArrayInputStream( outDetachedByteArray, 0, outSize )
            return buildBinaryOutput( compressed );
        }
    }

    private static class ZipDecompressingTransformer extends EncodeDecodeTransformer {
        ZipDecompressingTransformer( EncodeDecodeContext encodeDecodeContext ) {
            super( encodeDecodeContext );
        }

        @Override
        Object transform( Object source ) {
            // TODO support streaming input; get InputStream to transform instead of byte array
            byte[] data = getBinaryInput( source );

            ZipInputStream in = new ZipInputStream( new ByteArrayInputStream( data ), Charsets.UTF8 );

            // Read the first file from the ZIP, regardless of filename, and ignore the rest
            byte[] decompressed;
            try ( PoolByteArrayOutputStream out = new PoolByteArrayOutputStream() ) {
                ZipEntry entry = in.getNextEntry();
                if ( entry == null ) {
                    encodeDecodeContext.audit( AssertionMessages.ENCODE_DECODE_ERROR, "Zip archive does not contain any entries" );
                    throw new AssertionStatusException( AssertionStatus.FAILED );
                }
                if ( entry.isDirectory() ) {
                    encodeDecodeContext.audit( AssertionMessages.ENCODE_DECODE_ERROR, "First entry in zip archive is a directory" );
                    throw new AssertionStatusException( AssertionStatus.FAILED );
                }
                IOUtils.copyStream( in, out );
                decompressed = out.toByteArray();
            } catch ( IOException e ) {
                audit( AssertionMessages.ENCODE_DECODE_ERROR, ExceptionUtils.getDebugException( e ), ExceptionUtils.getMessage( e ) );
                throw new AssertionStatusException( AssertionStatus.FAILED );
            }

            // TODO support copy avoidance; initialize output Message with ByteArrayInputStream( outDetachedByteArray, 0, outSize )
            return buildBinaryOutput( decompressed );
        }
    }

    private static class GzipCompressingTransformer extends EncodeDecodeTransformer {
        GzipCompressingTransformer( EncodeDecodeContext encodeDecodeContext ) {
            super( encodeDecodeContext );
        }

        @Override
        Object transform( Object source ) {
            // TODO support streaming input; get InputStream to transform instead of byte array
            byte[] data = getBinaryInput( source );
            ByteArrayInputStream in = new ByteArrayInputStream( data );
            PoolByteArrayOutputStream out = new PoolByteArrayOutputStream();

            byte[] compressed;
            try {
                GZIPOutputStream zos = new GZIPOutputStream( out );
                IOUtils.copyStream( in, zos );
                zos.finish();
                zos.flush();
                compressed = out.toByteArray();
            } catch ( IOException e ) {
                audit( AssertionMessages.ENCODE_DECODE_ERROR, ExceptionUtils.getDebugException( e ), ExceptionUtils.getMessage( e ) );
                throw new AssertionStatusException( AssertionStatus.FAILED );
            } finally {
                ResourceUtils.closeQuietly( out );
            }

            // TODO support copy avoidance; initialize output Message with ByteArrayInputStream( outDetachedByteArray, 0, outSize )
            return buildBinaryOutput( compressed );
        }
    }

    private static class GzipDecompressingTransformer extends EncodeDecodeTransformer {
        GzipDecompressingTransformer( EncodeDecodeContext encodeDecodeContext ) {
            super( encodeDecodeContext );
        }

        @Override
        Object transform( Object source ) {
            // TODO support streaming input; get InputStream to transform instead of byte array
            byte[] data = getBinaryInput( source );

            byte[] decompressed;
            try ( PoolByteArrayOutputStream out = new PoolByteArrayOutputStream() ) {
                GZIPInputStream in = new GZIPInputStream( new ByteArrayInputStream( data ) );
                IOUtils.copyStream( in, out );
                decompressed = out.toByteArray();
            } catch ( IOException e ) {
                audit( AssertionMessages.ENCODE_DECODE_ERROR, ExceptionUtils.getDebugException( e ), ExceptionUtils.getMessage( e ) );
                throw new AssertionStatusException( AssertionStatus.FAILED );
            }

            // TODO support copy avoidance; initialize output Message with ByteArrayInputStream( outDetachedByteArray, 0, outSize )
            return buildBinaryOutput( decompressed );
        }
    }


}
