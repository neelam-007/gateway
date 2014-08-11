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

    static {
        org.apache.xml.security.Init.init();
    }

    private static org.apache.xml.security.c14n.Canonicalizer makeApacheC11r() {
        try {
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

    private byte[] serializeNode(Node node, String inclusiveNamespaceList) throws CanonicalizationException {
        return makeApacheC11r().canonicalizeSubtree(node, inclusiveNamespaceList);
    }

    private byte[] serializeNodeset(NodeList nodelist, String inclusiveNamespaceList) throws CanonicalizationException {
        return makeApacheC11r().canonicalizeXPathNodeSet(nodelist, inclusiveNamespaceList);
    }
}
