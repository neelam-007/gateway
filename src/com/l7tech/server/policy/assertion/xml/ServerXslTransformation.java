package com.l7tech.server.policy.assertion.xml;

import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.message.SoapRequest;
import com.l7tech.message.SoapResponse;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xml.XslTransformation;
import com.l7tech.server.policy.assertion.ServerAssertion;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Server side class that executes an XslTransformation assertion within a policy tree.
 *
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Feb 10, 2004<br/>
 * $Id$<br/>
 *
 */
public class ServerXslTransformation implements ServerAssertion {

    public ServerXslTransformation(XslTransformation assertion) {
        if (assertion == null) throw new IllegalArgumentException("must provide assertion");
        subject = assertion;
    }

    /**
     * preformes the transformation
     */
    public AssertionStatus checkRequest(Request req, Response res) throws IOException, PolicyAssertionException {
        // 1. Get document to transform
        Document doctotransform = null;
        try {
            switch (subject.getDirection()) {
                case XslTransformation.APPLY_TO_REQUEST:
                    logger.finest("transforming request");
                    doctotransform = ((SoapRequest)req).getDocument();
                    break;
                case XslTransformation.APPLY_TO_RESPONSE:
                    logger.finest("transforming response");
                    doctotransform = ((SoapResponse)res).getDocument();
                    break;
                default:
                    // should not get here!
                    logger.warning("assertion is not configured properly. should specify if transformation should" +
                                   "apply to request or to response. returning failure.");
                    return AssertionStatus.SERVER_ERROR;
            }
        } catch (SAXException e) {
            String msg = "cannot get document to tranform";
            logger.log(Level.WARNING, msg, e);
            throw new PolicyAssertionException(msg, e);
        }

        // 2. Apply the transformation
        String output = null;
        try {
            output = transform(doctotransform);
        } catch (TransformerException e) {
            String msg = "error transforming document";
            logger.log(Level.WARNING, msg, e);
            throw new PolicyAssertionException(msg, e);
        }

        // 3. Replace original document with output from transformation
        switch (subject.getDirection()) {
            case XslTransformation.APPLY_TO_REQUEST:
                ((SoapRequest)req).setXml(output);
                break;
            case XslTransformation.APPLY_TO_RESPONSE:
                ((SoapResponse)res).setXml(output);
                break;
        }

        return AssertionStatus.NONE;
    }

    String transform(Document source) throws TransformerException {
        Transformer transformer = makeTransformer(subject.getXslSrc());
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        transformer.transform(new DOMSource(source), new StreamResult(output));
        return output.toString();
    }

    /**
     * Unfortunately, Transformer objects are not thread safe. This is why we create them for every requests.
     * An alternative would be to maintain a pool of transformers.
     * @param xslstr
     * @return
     * @throws TransformerConfigurationException
     */
    private Transformer makeTransformer(String xslstr) throws TransformerConfigurationException {
        TransformerFactory transfoctory = TransformerFactory.newInstance();
        StreamSource xsltsource = new StreamSource(new StringReader(xslstr));
        return transfoctory.newTransformer(xsltsource);
    }

    private XslTransformation subject;
    private final Logger logger = Logger.getLogger(getClass().getName());
}
