package com.l7tech.security.xml.processor;

import com.ibm.xml.dsig.Canonicalizer;
import com.ibm.xml.dsig.Transform;
import com.ibm.xml.dsig.TransformContext;
import com.ibm.xml.dsig.TransformException;
import com.l7tech.common.io.CertUtils;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.util.DomUtils;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.HexUtils;
import com.l7tech.xml.soap.SoapUtil;
import org.apache.xml.security.c14n.CanonicalizationException;
import org.apache.xml.security.c14n.InvalidCanonicalizerException;
import org.apache.xml.security.signature.XMLSignature;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.io.OutputStream;
import java.security.cert.X509Certificate;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Makes the Sun exclusive canonicalizer available as an XSS4J canonicalizer.
 */
public class ApacheExclusiveC14nAdaptor extends Transform implements Canonicalizer {
    private static final Logger logger = Logger.getLogger(ApacheExclusiveC14nAdaptor.class.getName());

    public static final String URI = "http://www.w3.org/2001/10/xml-exc-c14n#";

    private String prefixList = null;

    private static org.apache.xml.security.c14n.Canonicalizer makeApacheC11r() {
        try {
            if (!apacheInit.get())
                initApacheC11r();
            return org.apache.xml.security.c14n.Canonicalizer.getInstance("http://www.w3.org/2001/10/xml-exc-c14n#");
        } catch (InvalidCanonicalizerException e) {
            throw new RuntimeException("Unable to initialize Apache exclusive canonicalizer: " + ExceptionUtils.getMessage(e), e);
        }
    }

    public String getURI() {
        return URI;
    }

    public void transform(TransformContext transformContext) throws TransformException {
        try {
            doTransform(transformContext);
        } catch (CanonicalizationException e) {
            final String msg = "Unable to transform: " + ExceptionUtils.getMessage(e);
            logger.log(Level.WARNING, msg, e);
            throw (TransformException)new TransformException(msg).initCause(e);
        } catch (IOException e) {
            final String msg = "Unable to transform: " + ExceptionUtils.getMessage(e);
            logger.log(Level.WARNING, msg, e);
            throw (TransformException)new TransformException(msg).initCause(e);
        }
    }

    private void doTransform(TransformContext context) throws CanonicalizationException, TransformException, IOException {
        switch(context.getType())
        {
            case 0: // '\0'
            case 1: // '\001'
            {
                byte result[] = serializeNode(context.getDocument(), prefixList);
                context.setContent(result, "UTF-8");
                break;
            }

            case 3: // '\003'
            {
                byte result[] = serializeNode(context.getNode(), prefixList);
                context.setContent(result, "UTF-8");
                break;
            }

            case 2: // '\002'
            {
                byte[] result = serializeNodeset(context.getNodeset(), prefixList);
                context.setContent(result, "UTF-8");
                break;
            }

            default:
                throw new RuntimeException("Internal Error: Unknown type: " + context.getType());
        }
    }

    public void setParameter(Node node)
    {
        if (node == null) {
            prefixList = null;
            return;
        }

        if (Node.ELEMENT_NODE != node.getNodeType())
            throw new IllegalArgumentException("The parameter must be an element.");

        Element element = (Element)node;

        if(!URI.equals(element.getNamespaceURI()))
            throw new IllegalArgumentException("The parameter must belong to the '" + URI + "' namespace.");

        if(!element.getLocalName().equals("InclusiveNamespaces"))
            throw new IllegalArgumentException("The parameter element must be 'InclusiveNamespaces'.");

        prefixList = element.getAttribute("PrefixList");
    }


    public void canonicalize(Node node, OutputStream outputStream) throws IOException {
        try {
            byte[] result = serializeNode(node, prefixList);
            outputStream.write(result);
        } catch (CanonicalizationException e) {
            throw new IOException(e);
        }
    }

    private static AtomicBoolean apacheInit = new AtomicBoolean(false);

    private static final String MINIMAL_SIGNED_XML =
            "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\"><SOAP-ENV:Header><wsse:Security xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\" xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\" SOAP-ENV:mustUnderstand=\"1\"><wsu:Timestamp wsu:Id=\"Timestamp-1-905e2dc529da31a389276ff3bfdaddda\"><wsu:Created>2008-11-17T23:26:43.164566982Z</wsu:Created><wsu:Expires>2008-11-17T23:31:43.164Z</wsu:Expires></wsu:Timestamp><wsse:BinarySecurityToken EncodingType=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary\" ValueType=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509v3\" wsu:Id=\"BinarySecurityToken-0-410c34e0a37b8d5b555a2383b0f88e49\">MIIBizCCATWgAwIBAgIJAJXEos4jEooJMA0GCSqGSIb3DQEBBQUAMA8xDTALBgNVBAMMBHRlc3QwHhcNMDgxMTE3MjMxNjQyWhcNMjgxMTEyMjMxNjQyWjAPMQ0wCwYDVQQDDAR0ZXN0MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAKagK9wVwmqzPPFkZiNuotholM7mcsM8lU25vxJaGhE/hJfZADiFk+LEOfM9MR9AOzM7aeMNJpB0YtcperCeDocCAwEAAaN0MHIwDAYDVR0TAQH/BAIwADAOBgNVHQ8BAf8EBAMCBeAwEgYDVR0lAQH/BAgwBgYEVR0lADAdBgNVHQ4EFgQUwrBRJLFI4+UP6o3FsnUzqhk422gwHwYDVR0jBBgwFoAUwrBRJLFI4+UP6o3FsnUzqhk422gwDQYJKoZIhvcNAQEFBQADQQAxPYfzI0y5KCFAHiwk4pAwvB+zPo1b0/rlH4e6QaQssNffxrjg3akFzKBIXIFHLc78s75rgy2UljAhCm0llMGy</wsse:BinarySecurityToken><ds:Signature xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\"><ds:SignedInfo><ds:CanonicalizationMethod Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"></ds:CanonicalizationMethod><ds:SignatureMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#rsa-sha1\"></ds:SignatureMethod><ds:Reference URI=\"#Timestamp-1-905e2dc529da31a389276ff3bfdaddda\"><ds:Transforms><ds:Transform Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"></ds:Transform></ds:Transforms><ds:DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\"></ds:DigestMethod><ds:DigestValue>5IYzmjA3NqsM1NIatXRbEQyiPUo=</ds:DigestValue></ds:Reference></ds:SignedInfo><ds:SignatureValue>gpoeK36EAgi366WTsc9JOTWRGD734aYVxPmiJ8JGDojdVVMZNgmQcPNNduW67plIPj1ZvqzcwN7naH66biHHow==</ds:SignatureValue><ds:KeyInfo><wsse:SecurityTokenReference><wsse:Reference URI=\"#BinarySecurityToken-0-410c34e0a37b8d5b555a2383b0f88e49\" ValueType=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509v3\"></wsse:Reference></wsse:SecurityTokenReference></ds:KeyInfo></ds:Signature></wsse:Security></SOAP-ENV:Header><SOAP-ENV:Body></SOAP-ENV:Body></SOAP-ENV:Envelope>";

    // During tests we found numerous problems using the JDK 6 dsig internal classes by themselves since they
    // had various initialization requirements.  In case this is also the case for the Apache c11r classes,
    // we run through a complete signature verification using the public APIs to ensure that everything is good to go
    // from now on in the various internal static fields.
    private synchronized static void initApacheC11r() {
        if (!apacheInit.get()) {
            try {
                org.apache.xml.security.Init.init();

                Document doc = XmlUtil.stringAsDocument(MINIMAL_SIGNED_XML);

                NodeList allElements = doc.getElementsByTagName( "*" );
                for ( int i = 0; i < allElements.getLength(); i++ ) {
                    final Element elem = (Element) allElements.item( i );
                    String id = elem.getAttribute( "wsu:Id" );
                    if ( id != null && id.length() > 0 ) {
                        elem.setIdAttribute( "wsu:Id", true );
                    }
                }

                final Element sechdr = SoapUtil.getSecurityElement(doc);
                final Element bst = DomUtils.findOnlyOneChildElementByName(sechdr, SoapUtil.SECURITY_NAMESPACE, "BinarySecurityToken");
                final Element signature = DomUtils.findOnlyOneChildElementByName(sechdr, SoapUtil.DIGSIG_URI, "Signature");

                X509Certificate signingCert = CertUtils.decodeCert(HexUtils.decodeBase64(XmlUtil.getTextValue(bst), true));
                XMLSignature xmlSignature = new XMLSignature(signature, "");

                if (!xmlSignature.checkSignatureValue(signingCert.getPublicKey()))
                    throw new RuntimeException("Unable to initialize Apache canonicalizers: unable to validate test signature");

                apacheInit.set(true);
            } catch (Exception e) {
                throw new RuntimeException("Unable to initialize Apache canonicalizers: " + ExceptionUtils.getMessage(e), e);
            }
        }
    }

    private byte[] serializeNode(Node node, String inclusiveNamespaceList) throws CanonicalizationException {
        return makeApacheC11r().canonicalizeSubtree(node, inclusiveNamespaceList);
    }

    private byte[] serializeNodeset(NodeList nodelist, String inclusiveNamespaceList) throws CanonicalizationException {
        return makeApacheC11r().canonicalizeXPathNodeSet(nodelist, inclusiveNamespaceList);
    }
}
