package com.l7tech.security.xml;

import java.util.Hashtable;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.Writer;
import java.io.OutputStreamWriter;
import java.io.IOException;

import org.w3c.dom.Node;
import org.w3c.dom.Element;

import com.l7tech.util.DomUtils;
import com.l7tech.util.ResourceUtils;
import com.l7tech.util.SoapConstants;
import com.l7tech.util.TooManyChildElementsException;
import com.l7tech.util.BufferPoolByteArrayOutputStream;

import com.ibm.xml.dsig.Transform;
import com.ibm.xml.dsig.TransformContext;
import com.ibm.xml.dsig.TransformException;
import com.ibm.dom.util.ExclusiveCanonicalizer;

/**
 * STRTransform implementation for XSS4J.
 *
 * @author Steve Jones
 */
public class STRTransform extends Transform {

    //- PUBLIC

    public STRTransform(Map<Node,Node> sourceTargetMapping) {
        strToTokenMap = sourceTargetMapping;
    }

    public String getURI() {
        return SoapConstants.TRANSFORM_STR;
    }

    public void setParameter(Node node) {
        Element params = (Element) node;
        if (params == null) {
            // The parameter is manditory but we'll allow none for backwards compatibility
            //errorMessage = "Missing parameter for STR-Transform (expected 'TransformationParameters')";
        }
        else if (!DomUtils.elementInNamespace(params, SoapConstants.SECURITY_URIS_ARRAY) &&
            "TransformationParameters".equals(params.getLocalName())) {
             errorMessage = "Invalid parameter for STR-Transform '"+params.getLocalName()+"' (expected 'TransformationParameters')";
        }
        else {
            try {
                Element canonMethodEle = DomUtils.findOnlyOneChildElementByName(params, SoapConstants.DIGSIG_URI, "CanonicalizationMethod");
                if (canonMethodEle == null) {
                    errorMessage = "Missing canonicalization method for STR-Transform.";
                }
                else if (!Transform.C14N_EXCLUSIVE.equals(canonMethodEle.getAttribute("Algorithm"))) {
                    errorMessage = "Unsupported canonicalization method algorithm for STR-Transform '"+canonMethodEle.getAttribute("Algorithm")+"'.";
                }
            }
            catch (TooManyChildElementsException tmcee) {
                errorMessage = "Invalid TransformationParameters for STR-Transform, too many child elements.";
            }
        }
    }

    public void transform(TransformContext c) throws TransformException {
        if (errorMessage != null)
            throw new TransformException(errorMessage);

        Node source = c.getNode();
        if (source == null)
            throw new TransformException("Source node is null");

        final Node result = strToTokenMap.get(source);
        if (result == null)
            throw new TransformException("Unable to check signature of element signed indirectly through SecurityTokenReference transform: the referenced SecurityTokenReference has not yet been seen");

        //
        Hashtable ht = new Hashtable();
        ht.put("", ""); // default ns should be included even if not utilized

        BufferPoolByteArrayOutputStream bo = new BufferPoolByteArrayOutputStream(4096);
        Writer wr = null;
        try {
            wr = new OutputStreamWriter(bo, "UTF-8");
            ExclusiveCanonicalizer.serializeSubset(ht, result, false, wr);
            wr.flush();
            String cannond = new String(bo.toByteArray(), "UTF-8");
            int firstElementEndIndex = cannond.indexOf('>');
            int xmlnsIndex = cannond.indexOf("xmlns=");
            if (xmlnsIndex < 0 || xmlnsIndex > firstElementEndIndex) {
                // Add default NS if not present
                if (logger.isLoggable(Level.FINEST))
                    logger.log(Level.FINEST, "Adding default namespace declaration to canonicalized form.");
                cannond = cannond.replaceFirst(" ", " xmlns=\"\" ");
            }
            c.setContent(cannond.getBytes("UTF-8"), "UTF-8");
        } catch (IOException e) {
            throw (TransformException) new TransformException().initCause(e);
        } finally {
            ResourceUtils.closeQuietly(wr);
            bo.close();
        }
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(STRTransform.class.getName());

    private final Map<Node,Node> strToTokenMap;
    private String errorMessage = null;

}
