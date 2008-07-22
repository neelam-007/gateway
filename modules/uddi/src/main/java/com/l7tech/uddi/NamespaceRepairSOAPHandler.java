package com.l7tech.uddi;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import javax.xml.XMLConstants;

import org.xml.sax.XMLReader;
import org.xml.sax.InputSource;
import org.xml.sax.XMLFilter;
import org.xml.sax.SAXException;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.XMLReaderFactory;
import org.xml.sax.helpers.XMLFilterImpl;

import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.CausedIOException;

/**
 * SOAPHandler to repair namespaces in SOAP messages if required.
 *
 * <p>For some reason, on Java 5, a bogus namespace declaration is created
 * for <code>http://www.w3.org/XML/1998/namespace</code>. This (understandably)
 * causes the message to be rejected by some servers.</p>
 *
 * <p>This class will fix these namespace declarations to use the correct
 * <code>xml</code> prefix and rewrite any prefixed attributes</p>
 *
 * <ul>
 *   <li>xmlns:illegal="http://www.w3.org/XML/1998/namespace" -> xmlns:xml="http://www.w3.org/XML/1998/namespace"</li>
 *   <li>illegal:lang="en" -> xml:lang="en"</li>
 * </ul>
 *
 * <p>Without this fix CentraSite UDDI will reject our SOAP messages.</p>
 *
 * <p>There is probably a much better way to do this than using this handler,
 * there is presumably some combination of JAX-M, JAX-B, SAX/DOM, JAX-WS and
 * TrAX that work with Java 5.</p>
 *
 * @author Steve Jones
 */
class NamespaceRepairSOAPHandler implements SOAPHandler<SOAPMessageContext> {

    //- PUBLIC

    public Set getHeaders() {
        return null;
    }

    /**
     * If a message is outbound, it is checked for the invalid namespace declaration.
     *
     * @param context the SOAP message context
     * @return true
     */
    public boolean handleMessage(final SOAPMessageContext context) {
        Boolean outboundProperty = (Boolean) context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);

        if ( outboundProperty!=null && outboundProperty.booleanValue() ) {
            SOAPMessage soapMessage = context.getMessage();

            if ( soapMessage != null ) {
                try {
                    SOAPPart soapPart = soapMessage.getSOAPPart();

                    Source source = soapPart.getContent();

                    if (source instanceof StreamSource) {
                        StreamSource streamSource = (StreamSource) source;

                        // replace content with cleaned version
                        StreamSource contentSource = rewrite(streamSource);
                        soapPart.setContent(contentSource);
                    }
                } catch (SOAPException se) {
                    logger.log(Level.INFO,
                            "Error processing SOAP message when checking namespaces: " + ExceptionUtils.getMessage(se),
                            ExceptionUtils.getDebugException(se));
                } catch (IOException ioe) {
                    logger.log(Level.INFO,
                            "Error processing SOAP message when checking namespaces: " + ExceptionUtils.getMessage(ioe),
                            ExceptionUtils.getDebugException(ioe));
                } 
            }
        }

        return true;
    }

    /**
     * Does nothing.
     *
     * @param context The soap message context to process
     * @return true
     */
    public boolean handleFault(final SOAPMessageContext context) {
        return true;
    }

    /**
     * Does nothing
     *
     * @param context ignored
     */
    public void close(final MessageContext context) {
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(NamespaceRepairSOAPHandler.class.getName());

    private static final String FEATURE_NAMESPACE_PREFIXES = "http://xml.org/sax/features/namespace-prefixes";
    private static final String FEATURE_NAMESPACES = "http://xml.org/sax/features/namespaces";

    /**
     * Rewrite the message.
     *
     * @param streamSource The source to rewrite
     * @return The rewritten streamSource
     * @throws IOException if an error occurs
     */
    private static StreamSource rewrite(final StreamSource streamSource) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);

        // build input source from TrAX source
        InputSource inputSource = new InputSource();
        inputSource.setCharacterStream(streamSource.getReader());
        inputSource.setByteStream(streamSource.getInputStream());
        inputSource.setSystemId(streamSource.getSystemId());
        inputSource.setPublicId(streamSource.getPublicId());

        try {
            // create parser that will not barf on the invalid attributes
            // i.e. is not namespace aware
            XMLReader xmlReader = XMLReaderFactory.createXMLReader();
            xmlReader.setFeature(FEATURE_NAMESPACE_PREFIXES, true);
            xmlReader.setFeature(FEATURE_NAMESPACES, false);

            // create a SAX filter to rewrite the invalid info
            final Map fixMap = new HashMap();
            final Map unfixMap = new HashMap();
            XMLFilter filterReader = new XMLFilterImpl(xmlReader){
                public void startElement(final String uri,
                                         final String localName,
                                         final String qName,
                                         final Attributes atts) throws SAXException {
                    for (int i=0; i<atts.getLength(); i++) {
                        String attrQName = atts.getQName(i);
                        if ( attrQName != null && XMLConstants.XML_NS_URI.equals(atts.getValue(i)) ) {
                            String[] parts = attrQName.split(":", 2);
                            if ( parts.length == 2 &&
                                 XMLConstants.XMLNS_ATTRIBUTE.equals(parts[0]) &&
                                 !XMLConstants.XML_NS_PREFIX.equals(parts[1])) {
                                fixMap.put(parts[1], XMLConstants.XML_NS_PREFIX);
                                unfixMap.put(XMLConstants.XML_NS_PREFIX, parts[1]);
                            }
                        }
                    }

                    super.startElement(uri, localName, qName, new AttributesFilter(atts){
                        private String map(String value) {
                            return domap(value, fixMap);
                        }

                        private String unmap(String value) {
                            return domap(value, unfixMap);
                        }

                        private String domap(String value, Map map) {
                            String result = value;
                            String[] parts = result==null ? new String[0] : result.split(":", 2);

                            if (parts.length == 2) {
                                String prefix = parts[0];
                                String name = parts[1];

                                if ( prefix.equals(XMLConstants.XMLNS_ATTRIBUTE) ) {
                                    // map a namespace
                                    if ( map.containsKey(name) ) {
                                        result = XMLConstants.XMLNS_ATTRIBUTE + ":" + map.get(name);
                                    }
                                } else {
                                    // map a prefixed attribute
                                    if ( map.containsKey(prefix) ) {
                                        result = map.get(prefix) + ":" + name;
                                    }
                                }
                            }

                            return result;
                        }

                        public int getIndex(String qName) {
                            return super.getIndex(unmap(qName));
                        }

                        public String getQName(int index) {
                            return map(super.getQName(index));
                        }

                        public String getType(String qName) {
                            return super.getType(unmap(qName));
                        }

                        public String getValue(String qName) {
                            return super.getValue(unmap(qName));
                        }
                    });
                }
            };

            // perform identity transform
            SAXSource saxSource = new SAXSource(filterReader, inputSource);
            StreamResult streamResult = new StreamResult(baos);

            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.transform(saxSource, streamResult);
        } catch (SAXException se) {
            throw new CausedIOException("SAX error during namespace repair: " + ExceptionUtils.getMessage(se), se);
        } catch (TransformerException te) {
            throw new CausedIOException("Transformer error during namespace repair: " + ExceptionUtils.getMessage(te), te);
        }

        return new StreamSource(new ByteArrayInputStream(baos.toByteArray()));
    }

    /**
     * Delegating attributes filter
     */
    private static class AttributesFilter implements Attributes {
        private final Attributes delegate;

        private AttributesFilter(final Attributes attributes) {
            delegate = attributes;
        }

        public int getIndex(String qName) {
            return delegate.getIndex(qName);
        }

        public int getIndex(String uri, String localName) {
            return delegate.getIndex(uri, localName);
        }

        public int getLength() {
            return delegate.getLength();
        }

        public String getLocalName(int index) {
            return delegate.getLocalName(index);
        }

        public String getQName(int index) {
            return delegate.getQName(index);
        }

        public String getType(int index) {
            return delegate.getType(index);
        }

        public String getType(String qName) {
            return delegate.getType(qName);
        }

        public String getType(String uri, String localName) {
            return delegate.getType(uri, localName);
        }

        public String getURI(int index) {
            return delegate.getURI(index);
        }

        public String getValue(int index) {
            return delegate.getValue(index);
        }

        public String getValue(String qName) {
            return delegate.getValue(qName);
        }

        public String getValue(String uri, String localName) {
            return delegate.getValue(uri, localName);
        }
    }
}
