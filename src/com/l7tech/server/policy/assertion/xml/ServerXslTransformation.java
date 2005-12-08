package com.l7tech.server.policy.assertion.xml;

import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.common.audit.Auditor;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xml.XslTransformation;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerAssertion;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.transform.Templates;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.StringReader;
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

    //- PUBLIC

    public ServerXslTransformation(XslTransformation assertion, ApplicationContext springContext) {
        if (assertion == null) throw new IllegalArgumentException("must provide assertion");
        subject = assertion;
        auditor = new Auditor(this, springContext, logger);
    }

    /**
     * preformes the transformation
     * @param context
     */
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {

        // 1. Get document to transform
        Document doctotransform = null;
        try {
            switch (subject.getDirection()) {
                case XslTransformation.APPLY_TO_REQUEST:
                    if (!context.getRequest().isXml()) {
                        auditor.logAndAudit(AssertionMessages.XSL_TRAN_REQUEST_NOT_XML);
                        return AssertionStatus.NOT_APPLICABLE;
                    }

                    auditor.logAndAudit(AssertionMessages.XSL_TRAN_REQUEST);
                    doctotransform = context.getRequest().getXmlKnob().getDocumentWritable();
                    break;
                case XslTransformation.APPLY_TO_RESPONSE:
                    if (!context.getResponse().isXml()) {
                        auditor.logAndAudit(AssertionMessages.XSL_TRAN_RESPONSE_NOT_XML);
                        return AssertionStatus.NOT_APPLICABLE;
                    }

                    auditor.logAndAudit(AssertionMessages.XSL_TRAN_RESPONSE);
                    doctotransform = context.getResponse().getXmlKnob().getDocumentWritable();
                    break;
                default:
                    // should not get here!
                    auditor.logAndAudit(AssertionMessages.XSL_TRAN_CONFIG_ISSUE);
                    return AssertionStatus.SERVER_ERROR;
            }

            // 2. Apply the transformation
            Document output = null;
            try {
                // todo, invoke the tarari xsl transformation if we detect that it is available
                output = XmlUtil.softXSLTransform(doctotransform, getTemplate().newTransformer());
                //output = transform(doctotransform);
            } catch (TransformerException e) {
                String msg = "error transforming document";
                auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] {msg}, e);
                throw new PolicyAssertionException(msg, e);
            }

            // 3. Replace original document with output from transformation
            switch (subject.getDirection()) {
                case XslTransformation.APPLY_TO_REQUEST:
                    context.getRequest().getXmlKnob().setDocument(output);
                    break;
                case XslTransformation.APPLY_TO_RESPONSE:
                    context.getResponse().getXmlKnob().setDocument(output);
                    break;
            }
        } catch (SAXException e) {
            String msg = "cannot get document to tranform";
            auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] {msg}, e);
            throw new PolicyAssertionException(msg, e);
        }

        return AssertionStatus.NONE;
    }

    /*
    Document transform(Document source) throws TransformerException {
        Transformer transformer = getTemplate().newTransformer();
        final DOMResult outputTarget = new DOMResult();
        transformer.transform(new DOMSource(source), outputTarget);
        final Node node = outputTarget.getNode();
        if (node instanceof Document) {
            return (Document)node;
        } else if (node != null) {
            return node.getOwnerDocument();
        } else {
            return null;
        }
    }*/

    private final Logger logger = Logger.getLogger(ServerXslTransformation.class.getName());

    private final Auditor auditor;
    private Templates template;
    private XslTransformation subject;

    private Templates makeTemplate(String xslstr) throws TransformerConfigurationException {
        TransformerFactory transfoctory = TransformerFactory.newInstance();
        StreamSource xsltsource = new StreamSource(new StringReader(xslstr));
        return transfoctory.newTemplates(xsltsource);
    }

    private Templates getTemplate() throws TransformerConfigurationException {
        if(template==null) {
            template = makeTemplate(subject.getXslSrc());
        }
        return template;
    }
}
