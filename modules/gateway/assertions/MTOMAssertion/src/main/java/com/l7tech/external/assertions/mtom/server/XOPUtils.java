package com.l7tech.external.assertions.mtom.server;

import org.w3c.dom.Element;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.PartInfo;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.mime.MimeUtil;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.util.ResourceUtils;
import com.l7tech.util.IOUtils;
import com.l7tech.util.HexUtils;
import com.l7tech.util.DomUtils;
import com.l7tech.util.BufferPoolByteArrayOutputStream;
import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.util.Pair;
import com.l7tech.message.Message;
import com.l7tech.message.MimeKnob;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.xml.ElementCursor;

import javax.xml.XMLConstants;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.SequenceInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Collections;
import java.util.UUID;
import java.util.Set;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.nio.charset.Charset;

/**
 * Utility class for working with XOP (XML-binary Optimized Packaging) Messages.
 *
 * <p>See also:</p>
 *
 * <ul>
 *   <li>http://www.w3.org/TR/xop10/#terminology</li>
 *   <li>http://www.w3.org/TR/soap12-mtom/</li>
 *   <li>http://www.w3.org/Submission/soap11mtom10/</li>
 * </ul>
 */
public class XOPUtils {

    //- PUBLIC

    /**
     * ContentTypeHeader for XOP
     */
    public static final ContentTypeHeader XOP_CONTENT_TYPE = ContentTypeHeader.create( "application/xop+xml" );

    /**
     * Namespace for XOP
     */
    public static final String NS_XOP = "http://www.w3.org/2004/08/xop/include";

    /**
     * XOP Include Element
     */
    public static final String XOP_ELEMENT_INCLUDE = "Include";

    /**
     * XOP Include Elements href attribute
     */
    public static final String XOP_ATTRIBUTE_HREF = "href";

    /**
     * XMLMIME NS as of W3C Working Draft 2 November 2004 (referenced from XOP specification)
     */
    public static final String NS_XMLMIME_1 = "http://www.w3.org/2004/11/xmlmime";

    /**
     * XMLMIME NS as of W3C Working Group Note 4 May 2005
     */
    public static final String NS_XMLMIME_2 = "http://www.w3.org/2005/05/xmlmime";

    /**
     * XMLMIME contentType attribute
     */
    public static final String XMLMIME_ATTR_CONTENT_TYPE = "contentType";

    /**
     * Check if the given content type is XOP.
     *
     * @return True if the content type header is not null and represents an XOP package
     */
    public static boolean isXop( final ContentTypeHeader contentTypeHeader ) {
        return contentTypeHeader != null &&
                CONTENT_TYPE_MULTIPART.equalsIgnoreCase(contentTypeHeader.getMainValue()) &&
                CONTENT_TYPE_XOP.equalsIgnoreCase( contentTypeHeader.getParam( CONTENT_TYPE_PARAM_TYPE ) );

    }

    /**
     * Validate an XOP message.
     *
     * <p>The following are validated:</p>
     * <ul>
     * <li>All referenced attachments are present</li>
     * <li>Each attachment is referenced only once</li>
     * <li>There are no extra attachments (unreferenced attachments, this is not a spec item)</li>
     * </ul>
     *
     * @param message The message to check.
     * @throws IOException If there is an error reading the message
     * @throws SAXException If there is an error parsing the XML part of the message
     * @throws XOPException If the message fails validation or is not XOP
     */
    public static void validate( final Message message ) throws IOException, SAXException, XOPException {
        final Set<String> contentIds = new HashSet<String>();
        final ElementCursor cursor = message.getXmlKnob().getElementCursor();
        cursor.moveToRoot();
        try {
            cursor.visitElements( new ElementCursor.Visitor(){
                @Override
                public void visit( final ElementCursor elementCursor ) throws InvalidDocumentFormatException {
                    if ( NS_XOP.equals( elementCursor.getNamespaceUri() ) ) {
                        if ( !XOP_ELEMENT_INCLUDE.equals( elementCursor.getLocalName() ) ) {
                            throw new InvalidDocumentFormatException("Unknown XOP element: " + elementCursor.getLocalName());
                        }

                        try {
                            final String href = elementCursor.getAttributeValue( XOP_ATTRIBUTE_HREF );
                            if ( !contentIds.add( toContentId(href) ) ) {
                                throw new InvalidDocumentFormatException("Multiple references to binary part: " + href);
                            }
                        } catch (XOPException e) {
                            throw new InvalidDocumentFormatException(e.getMessage());
                        }
                    }
                }
            } );
        } catch (InvalidDocumentFormatException e) {
            throw new XOPException( e.getMessage() );
        }

        final String mainPartId = message.getMimeKnob().getOuterContentType().getParam( CONTENT_TYPE_PARAM_START );
        final Set<String> partCids = new HashSet<String>();
        for ( PartInfo partInfo : message.getMimeKnob() ) {
            final String cid = partInfo.getContentId(true);
            if ( !contentIds.contains( cid ) && !mainPartId.equals("<" + cid + ">") ) {
                throw new XOPException( "Unreferenced MIME part: " + cid );
            }
            partCids.add( cid );
        }

        contentIds.removeAll( partCids );
        if ( !contentIds.isEmpty() ) {
            throw new XOPException( "Binary parts not found for Content-IDs " + contentIds );
        }
    }

    /**
     * Get the size for the given Element which must be either BASE64 or an XOP include.
     *
     * @param message The message for the element
     * @param elementCursor The cursor for the element to check.
     * @return The size in bytes (not characters)
     */
    public static long getSize( final Message message, final ElementCursor elementCursor ) throws IOException, XOPException {
        long size;

        if ( elementCursor.moveToFirstChildElement() ) {
            // Then it is either XOP or invalid
            final String href = elementCursor.getAttributeValue( XOP_ATTRIBUTE_HREF );
            if ( NS_XOP.equals( elementCursor.getNamespaceUri() ) &&
                 XOP_ELEMENT_INCLUDE.equals( elementCursor.getLocalName() ) &&
                 href != null && !href.isEmpty() ) {
                MimeKnob mimeKnob = message.getMimeKnob();
                try {
                    PartInfo partInfo = mimeKnob.getPartByContentId( toContentId(href) );
                    size = partInfo.getContentLength();
                } catch ( NoSuchPartException e ) {
                    throw new XOPException( "Mime part not found for Content-ID URL '"+href+"'", e );
                }
            } else {
                throw new XOPException("Element child is not an XOP Include");
            }
        } else {
            String text = elementCursor.getTextValue();
            if ( text.length()%4 != 0 ) throw new XOPException("Element child is not valid Base64");
            size = getBase64DataLength( text );
        }

        return size;
    }

    /**
     * Convert a regular message to XOP (or MTOM if soap 1.2) format.
     *
     * @param message The message to convert.
     * @param base64BinaryElements base64BinaryElements The Elements to extract
     * @param threshold The minimum size of data to encode
     * @param alwaysEncode True to encode even if no data is extracted
     * @param stashManagerFactory The stash manager factory to use
     */
    public static void extract( final Message message,
                                final Iterable<Element> base64BinaryElements,
                                final int threshold,
                                final boolean alwaysEncode,
                                final StashManagerFactory stashManagerFactory ) throws IOException, SAXException, XOPException {
        extract(  message, message, base64BinaryElements, threshold, alwaysEncode, stashManagerFactory );
    }

    /**
     * Convert a regular message to XOP (or MTOM if soap 1.2) format.
     *
     * @param sourceMessage The message to read.
     * @param targetMessage The message to update.
     * @param base64BinaryElements base64BinaryElements The Elements to extract
     * @param threshold The minimum size of data to encode (bytes)
     * @param alwaysEncode True to encode even if no data is extracted
     * @param stashManagerFactory The stash manager factory to use
     */
    public static void extract( final Message sourceMessage,
                                final Message targetMessage,
                                final Iterable<Element> base64BinaryElements,
                                final int threshold,
                                final boolean alwaysEncode,
                                final StashManagerFactory stashManagerFactory ) throws IOException, SAXException, XOPException {
        MimeKnob mimeKnob = sourceMessage.getMimeKnob();
        ContentTypeHeader originalContentType = mimeKnob.getOuterContentType();
        if ( !originalContentType.isXml() ) {
            throw new XOPException( "Message is not XML, content type is '"+originalContentType.getMainValue()+"'" );
        }

        Document document = sourceMessage.getXmlKnob().getDocumentReadOnly();
        Map<String, Pair<byte[],String>> parts = new HashMap<String,Pair<byte[],String>>();
        for ( Element element : base64BinaryElements ) {
            String base64Text = DomUtils.getTextValue( element ); //TODO [steve] Validate nothing but text children and BASE64 (must be no spaces)
            if ( getBase64DataLength(base64Text) > threshold ) {
                DomUtils.removeAllChildren( element );

                //TODO [steve] Don't modify elements passed in (copy the DOM, or modify on write)
                Element partIncludeElement = element.getOwnerDocument().createElementNS( NS_XOP, XOP_ELEMENT_INCLUDE );
                partIncludeElement.setAttributeNS( XMLConstants.XMLNS_ATTRIBUTE_NS_URI, XMLConstants.XMLNS_ATTRIBUTE, NS_XOP );
                String contentId = UUID.randomUUID().toString() + DOMAIN_SUFFIX;
                partIncludeElement.setAttribute( XOP_ATTRIBUTE_HREF, CID_PREFIX+contentId );
                parts.put( contentId, new Pair<byte[],String>(HexUtils.decodeBase64( base64Text ), getXMLMimeContentType(element)) );
                element.appendChild( partIncludeElement );
            }
        }

        if ( alwaysEncode || !parts.isEmpty() ) {
            final byte[] mainPart = XmlUtil.toByteArray( document );
            final String mimeBoundary = MIME_BOUNDARY_PREFIX + UUID.randomUUID();
            final String mainPartContentId = UUID.randomUUID().toString() + DOMAIN_SUFFIX;

            final Collection<InputStream> streams = new LinkedList<InputStream>();

            StringBuilder bodyTypeBuilder = new StringBuilder(256);
            bodyTypeBuilder.append( originalContentType.getMainValue() );
            if ( CONTENT_TYPE_SOAP12.equalsIgnoreCase( originalContentType.getMainValue() ) && originalContentType.getParam( CONTENT_TYPE_PARAM_ACTION ) != null) {
                appendParameter( bodyTypeBuilder, CONTENT_TYPE_PARAM_ACTION, originalContentType.getParam( CONTENT_TYPE_PARAM_ACTION ), true );
            }

            StringBuilder mainPartContentTypeBuilder = new StringBuilder(512);
            mainPartContentTypeBuilder.append( CONTENT_TYPE_XOP );
            appendParameter( mainPartContentTypeBuilder, CONTENT_TYPE_PARAM_CHARSET, CHARSET_UTF8_ID, false );
            appendParameter( mainPartContentTypeBuilder, CONTENT_TYPE_PARAM_TYPE, bodyTypeBuilder.toString(), true );
            streams.add( buildPartHeadersInputStream( mimeBoundary, true, mainPartContentId, mainPartContentTypeBuilder.toString(), mainPart.length ) );
            streams.add( new ByteArrayInputStream(mainPart) );

            for ( Map.Entry<String,Pair<byte[],String>> part : parts.entrySet() ) {
                String contentId = part.getKey();
                byte[] content = part.getValue().left;
                String contentType = part.getValue().right;
                if ( contentType == null ) {
                    contentType = CONTENT_TYPE_RAW;
                }
                streams.add( buildPartHeadersInputStream( mimeBoundary, false, contentId, contentType, content.length ) );
                streams.add( new ByteArrayInputStream(content) );
            }

            streams.add( buildClosingBoundaryInputStream(mimeBoundary) );

            StringBuilder contentTypeBuilder = new StringBuilder(512);
            contentTypeBuilder.append( CONTENT_TYPE_MULTIPART );
            appendParameter( contentTypeBuilder, CONTENT_TYPE_PARAM_BOUNDARY, mimeBoundary, false );
            appendParameter( contentTypeBuilder, CONTENT_TYPE_PARAM_TYPE, CONTENT_TYPE_XOP, false );
            appendParameter( contentTypeBuilder, CONTENT_TYPE_PARAM_START, "<" + mainPartContentId + ">", false );
            appendParameter( contentTypeBuilder, CONTENT_TYPE_PARAM_INFO, bodyTypeBuilder.toString(), true );

            InputStream in = new SequenceInputStream( Collections.enumeration(streams) );
            try {
                targetMessage.initialize(
                        stashManagerFactory.createStashManager(),
                        ContentTypeHeader.parseValue( contentTypeBuilder.toString() ),
                        in );
            } catch ( IOException ioe ) {
                ResourceUtils.closeQuietly( in );
                throw ioe;
            }
        } else if (sourceMessage != targetMessage) {
            InputStream in = null;
            try {
                in = mimeKnob.getEntireMessageBodyAsInputStream();
                targetMessage.initialize(
                        stashManagerFactory.createStashManager(),
                        originalContentType,
                        in );
            } catch ( NoSuchPartException e ) {
                ResourceUtils.closeQuietly( in );
                throw new IOException( "MIME first part cannot be read." );
            } 
        }
    }

    /**
     * Convert an XOP message to a regular XML (or SOAP) message.
     *
     * @param message The message to convert.
     * @param removePackaging True to remove the MIME XOP wrapper
     * @param stashManagerFactory The stash manager factory to use
     * @throws IOException if an error occurs reading the given message
     * @throws SAXException if an error occurs parsing the given message
     * @throws XOPException if any other error occurs
     */
    public static void reconstitute( final Message message,
                                     final boolean removePackaging,
                                     final StashManagerFactory stashManagerFactory ) throws IOException, SAXException, XOPException {
        reconstitute( message, message, removePackaging, stashManagerFactory );
    }

    /**
     * Convert an XOP message to a regular XML (or SOAP) message.
     *
     * <p>Any extra MIME parts are destroyed.</p>
     *
     * @param sourceMessage The message to read.
     * @param targetMessage The message to update.
     * @param removePackaging True to remove the MIME XOP wrapper
     * @param stashManagerFactory The stash manager factory to use
     * @throws IOException if an error occurs reading the given message
     * @throws SAXException if an error occurs parsing the given message
     * @throws XOPException if any other error occurs
     */
    public static void reconstitute( final Message sourceMessage,
                                     final Message targetMessage,
                                     final boolean removePackaging,
                                     final StashManagerFactory stashManagerFactory ) throws IOException, SAXException, XOPException {
        final MimeKnob mimeKnob = sourceMessage.getMimeKnob();
        final ContentTypeHeader originalContentType = mimeKnob.getOuterContentType();
        if ( !originalContentType.isMultipart() ||
             !XOP_CONTENT_TYPE.matches(ContentTypeHeader.parseValue(originalContentType.getParam(CONTENT_TYPE_PARAM_TYPE)))) {
            throw new XOPException( "Message is not XOP, content type is '" + originalContentType.getMainValue() + "'" );
        }

        final String mainPartId = originalContentType.getParam( CONTENT_TYPE_PARAM_START );
        final PartInfo mainPart = mimeKnob.getFirstPart();
        if ( mainPartId != null && !mainPartId.equals( mainPart.getContentId(false) )) {
            throw new XOPException( "MIME first part is not main part." );
        }

        final Document document;
        try {
            document = XmlUtil.parse( sourceMessage.getMimeKnob().getFirstPart().getInputStream( false ) );
        } catch (NoSuchPartException e) {
            throw new IOException( "MIME first part cannot be read." );
        }
        NodeList includeNodeList = document.getElementsByTagNameNS( NS_XOP, XOP_ELEMENT_INCLUDE );  //TODO [steve] Validate XOP Include is an only child (be lax and allow empty text nodes)
        Map<Element,PartInfo> includeMap = new HashMap<Element,PartInfo>();
        for ( int i=0; i<includeNodeList.getLength(); i++ ) {
            Element includeElement = (Element) includeNodeList.item( i );
            String contentIdUrl = includeElement.getAttribute( XOP_ATTRIBUTE_HREF );
            try {
                PartInfo partInfo = mimeKnob.getPartByContentId( toContentId(contentIdUrl) );
                includeMap.put( includeElement, partInfo );
            } catch ( NoSuchPartException e ) {
                throw new XOPException( "Mime part not found for Content-ID URL '"+contentIdUrl+"'", e );
            }
        }

        for ( Map.Entry<Element, PartInfo> entry : includeMap.entrySet() ) {
            Element includeElement = entry.getKey();
            PartInfo attachment = entry.getValue();

            String base64BinaryContent = null;
            InputStream in = null;
            try {
                in = attachment.getInputStream( false );
                base64BinaryContent = HexUtils.encodeBase64( IOUtils.slurpStream( in, 1024*1024 ), true ); //TODO [steve] Attachement size limit here? (xml max bytes as approximation?)
            } catch (NoSuchPartException e) {
                throw new XOPException( "Error reading content for MIME part '"+attachment.getContentId( true )+"'", e );
            } finally {
                ResourceUtils.closeQuietly( in );
            }

            Element parent = (Element) includeElement.getParentNode();
            DomUtils.removeAllChildren( parent );
            parent.appendChild( document.createTextNode( base64BinaryContent ));
        }

        if ( removePackaging ) {
            ContentTypeHeader bodyContentType;
            String startInfo = originalContentType.getParam( CONTENT_TYPE_PARAM_INFO );
            if ( startInfo != null ) {
                bodyContentType = ContentTypeHeader.parseValue( startInfo );
            } else {
                bodyContentType = mainPart.getContentType();
            }

            String mainType = bodyContentType.getMainValue();
            StringBuilder contentTypeBuilder = new StringBuilder(512);
            contentTypeBuilder.append( mainType );
            appendParameter( contentTypeBuilder, CONTENT_TYPE_PARAM_CHARSET, CHARSET_UTF8_ID, false );
            if ( CONTENT_TYPE_SOAP12.equalsIgnoreCase( mainType ) && bodyContentType.getParam( CONTENT_TYPE_PARAM_ACTION ) != null) {
                appendParameter( contentTypeBuilder, CONTENT_TYPE_PARAM_ACTION, bodyContentType.getParam( CONTENT_TYPE_PARAM_ACTION ), true );
            }

            targetMessage.initialize(
                    stashManagerFactory.createStashManager(),
                    ContentTypeHeader.parseValue( contentTypeBuilder.toString() ),
                    new ByteArrayInputStream( XmlUtil.toByteArray(document) ) );
        } else {
            final Collection<InputStream> streams = new LinkedList<InputStream>();
            final byte[] mainPartBytes = XmlUtil.toByteArray(document);
            final String mimeBoundary = originalContentType.getParam( CONTENT_TYPE_PARAM_BOUNDARY );
            final String mainPartContentId = mainPart.getContentId(true);

            streams.add( buildPartHeadersInputStream( mimeBoundary, true, mainPartContentId, mainPart.getContentType().getFullValue(), mainPartBytes.length ) );
            streams.add( new ByteArrayInputStream( mainPartBytes ) );
            streams.add( buildClosingBoundaryInputStream(mimeBoundary) );

            targetMessage.initialize(
                    stashManagerFactory.createStashManager(),
                    originalContentType,
                    new SequenceInputStream(Collections.enumeration(streams)) );
        }
    }


    public static class XOPException extends Exception {
        public XOPException( final String message ) {
            super( message );
        }

        public XOPException( final String message, final Throwable cause ) {
            super( message, cause );
        }
    }

    //- PRIVATE

    private static final String MIME_BOUNDARY_PREFIX = "MIME_boundary_";
    private static final String CONTENT_TYPE_MULTIPART = MimeUtil.MULTIPART_CONTENT_TYPE;
    private static final String CONTENT_TYPE_RAW = "application/octet-stream";
    private static final String CONTENT_TYPE_XOP = "application/xop+xml";
    private static final String CONTENT_TYPE_SOAP12 = "application/soap+xml";
    private static final String CONTENT_TYPE_PARAM_ACTION = "action";
    private static final String CONTENT_TYPE_PARAM_BOUNDARY = MimeUtil.MULTIPART_BOUNDARY;
    private static final String CONTENT_TYPE_PARAM_CHARSET = "charset";
    private static final String CONTENT_TYPE_PARAM_START = "start";
    private static final String CONTENT_TYPE_PARAM_INFO = "startinfo";
    private static final String CONTENT_TYPE_PARAM_TYPE = MimeUtil.MULTIPART_TYPE;
    private static final String CHARSET_UTF8_ID = "UTF-8";
    private static final Charset CHARSET_UTF8 = Charset.forName( CHARSET_UTF8_ID );
    private static final String ENCODING_BINARY = "binary";
    private static final String CID_PREFIX = "cid:";
    private static final String DOMAIN_SUFFIX = "@127.0.0.1";
    private static final byte[] DASHDASH = MimeUtil.MULTIPART_BOUNDARY_PREFIX.getBytes(CHARSET_UTF8);
    private static final byte[] HEADER_SEPARATOR = ": ".getBytes(CHARSET_UTF8);
    private static final String[] XMLMIME_NAMESPACES = new String[]{ NS_XMLMIME_2, NS_XMLMIME_1 };

    private static String toContentId( final String contentIdUrl ) throws XOPException {
        String contentId = null;

        if ( contentIdUrl != null ) {
            if ( contentIdUrl.toLowerCase().startsWith( CID_PREFIX ) ) {
                contentId = contentIdUrl.substring( CID_PREFIX.length() );
            }
        }

        if ( contentId == null )
            throw new XOPException( "Include missing or invalid Content-ID URL '"+contentIdUrl+"'" );

        return contentId;
    }

    /**
     * Append a parameter to the buffer (as with content-type parameters)
     */
    private static void appendParameter( final StringBuilder builder,
                                         final String name,
                                         final String value,
                                         final boolean quote ) {
        builder.append( "; " );
        builder.append( name );
        builder.append( "=" );
        if ( quote )  {
            String quotedString = value;
            if ( value.startsWith( "\"" ) && value.endsWith( "\"" ) && value.length() > 1 ) {
                quotedString = value.substring( 1, value.length()-1 );
            }
            builder.append( "\"" );
            builder.append( quotedString.replaceAll( "\"", Matcher.quoteReplacement( "\\\"" ) ));
            builder.append( "\"" );
        } else {
            builder.append( value );
        }
    }

    /**
     * Write a header to the output (as with content-type parameters)
     */
    private static void writeHeader( final OutputStream os,
                                     final String name,
                                     final String value ) throws IOException {
        os.write( name.getBytes( CHARSET_UTF8 ) );
        os.write( HEADER_SEPARATOR );
        os.write( value.getBytes( CHARSET_UTF8 ) );
        os.write( MimeUtil.CRLF );
    }

    private static InputStream buildPartHeadersInputStream( final String mimeBoundary,
                                                            final boolean mainPart,
                                                            final String contentId,
                                                            final String contentType,
                                                            final int contentLength ) throws XOPException {
        BufferPoolByteArrayOutputStream os = new BufferPoolByteArrayOutputStream();

        try {
            if ( !mainPart ) os.write( MimeUtil.CRLF );
            os.write( DASHDASH );
            os.write( mimeBoundary.getBytes( CHARSET_UTF8 ));
            os.write( MimeUtil.CRLF );

            writeHeader( os, MimeUtil.CONTENT_TYPE, contentType );
            writeHeader( os, MimeUtil.CONTENT_TRANSFER_ENCODING, ENCODING_BINARY );
            writeHeader( os, MimeUtil.CONTENT_LENGTH, Integer.toString( contentLength ) );
            writeHeader( os, MimeUtil.CONTENT_ID, "<" + contentId + ">" );

            os.write( MimeUtil.CRLF );
        } catch (IOException e) {
            throw new XOPException( "Error creating mime part.", e );
        }

        return os.toInputStream();
    }

    private static InputStream buildClosingBoundaryInputStream( final String mimeBoundary ) throws XOPException {
        ByteArrayOutputStream os = new ByteArrayOutputStream( mimeBoundary.length() + 10 );

        byte[] boundaryBytes = mimeBoundary.getBytes( CHARSET_UTF8 );
        os.write( MimeUtil.CRLF, 0, MimeUtil.CRLF.length );
        os.write( DASHDASH, 0, DASHDASH.length ); 
        os.write( boundaryBytes, 0, boundaryBytes.length );
        os.write( DASHDASH, 0, DASHDASH.length );
        os.write( MimeUtil.CRLF, 0, MimeUtil.CRLF.length );

        return new ByteArrayInputStream( os.toByteArray() );
    }

    public static String getXMLMimeContentType( final Element element ) {
        String contentType = null;

        for ( String ns : XMLMIME_NAMESPACES ) {
            if ( element.hasAttributeNS( ns, XMLMIME_ATTR_CONTENT_TYPE )) {
                contentType = element.getAttributeNS( ns, XMLMIME_ATTR_CONTENT_TYPE );
                if ( !contentType.isEmpty() ) {
                    break;
                }
            }
        }

        return contentType;
    }

    private static int getBase64DataLength( final String text ) {
        int size = (text.length()/4)*3;
        if ( text.endsWith( "==" )) size -= 2;
        else if ( text.endsWith( "=" )) size--;
        return size;
    }
}
