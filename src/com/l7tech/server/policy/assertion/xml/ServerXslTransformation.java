package com.l7tech.server.policy.assertion.xml;

import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.common.audit.Auditor;
import com.l7tech.common.message.Message;
import com.l7tech.common.mime.PartInfo;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.TarariLoader;
import com.l7tech.common.xml.tarari.GlobalTarariContext;
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
import java.io.InputStream;
import java.util.logging.Logger;

/**
 * Server side class that executes an XslTransformation assertion within a policy tree.
 *
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Feb 10, 2004<br/>
 *
 */
public class ServerXslTransformation implements ServerAssertion {
    private final Logger logger = Logger.getLogger(ServerXslTransformation.class.getName());

    private final Auditor auditor;
    private Templates template;
    private XslTransformation subject;
    private final GlobalTarariContext tarariContext;

    public ServerXslTransformation(XslTransformation assertion, ApplicationContext springContext) {
        if (assertion == null) throw new IllegalArgumentException("must provide assertion");
        subject = assertion;
        auditor = new Auditor(this, springContext, logger);
        tarariContext = TarariLoader.getGlobalContext();
    }

    /**
     * preformes the transformation
     * @param context
     */
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {

        // 1. Get document to transform
        Message msgtotransform;
        try {
            int whichMimePart = subject.getWhichMimePart();
            if (whichMimePart <= 0) whichMimePart = 0;
            switch (subject.getDirection()) {
                case XslTransformation.APPLY_TO_REQUEST:
                    if (!context.getRequest().isXml()) {
                        auditor.logAndAudit(AssertionMessages.XSL_TRAN_REQUEST_NOT_XML);
                        return AssertionStatus.NOT_APPLICABLE;
                    }

                    auditor.logAndAudit(AssertionMessages.XSL_TRAN_REQUEST);
                    msgtotransform = context.getRequest();
                    break;
                case XslTransformation.APPLY_TO_RESPONSE:
                    if (!context.getResponse().isXml()) {
                        auditor.logAndAudit(AssertionMessages.XSL_TRAN_RESPONSE_NOT_XML);
                        return AssertionStatus.NOT_APPLICABLE;
                    }

                    auditor.logAndAudit(AssertionMessages.XSL_TRAN_RESPONSE);
                    msgtotransform = context.getResponse();
                    break;
                default:
                    // should not get here!
                    auditor.logAndAudit(AssertionMessages.XSL_TRAN_CONFIG_ISSUE);
                    return AssertionStatus.SERVER_ERROR;
            }

            Document doctotransform = null;
            if (whichMimePart == 0) {
                doctotransform = msgtotransform.getXmlKnob().getDocumentWritable();
            } else {
                try {
                    PartInfo mimePart = msgtotransform.getMimeKnob().getPart(whichMimePart);
                    if (mimePart.getContentType().isXml()) {
                        InputStream is = mimePart.getInputStream(false);
                        doctotransform = XmlUtil.parse(is, false);
                    }
                } catch (NoSuchPartException e) {
                    auditor.logAndAudit(AssertionMessages.XSL_TRAN_NO_SUCH_PART, new String[] { Integer.toString(whichMimePart)});
                    return AssertionStatus.BAD_REQUEST;
                }
            }

            // 2. Apply the transformation
            Document output = null;
            try {
                if (tarariContext != null) {
                    logger.warning("TODO, tarari accelerated xslt");
                    // todo, invoke the tarari xsl transformation
                    // output = something tarari
                }
                // fallback on software when necessary
                if (output == null) {
                    output = XmlUtil.softXSLTransform(doctotransform, getTemplate().newTransformer());
                }
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
