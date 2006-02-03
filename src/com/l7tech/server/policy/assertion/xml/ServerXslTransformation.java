package com.l7tech.server.policy.assertion.xml;

import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.common.audit.Auditor;
import com.l7tech.common.message.Message;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.mime.PartInfo;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.TarariLoader;
import com.l7tech.common.xml.tarari.GlobalTarariContext;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xml.XslTransformation;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.tarari.xml.XmlParseException;
import com.tarari.xml.XmlResult;
import com.tarari.xml.XmlSource;
import com.tarari.xml.xslt11.Stylesheet;
import com.tarari.xml.xslt11.XsltException;
import com.tarari.xml.xslt11.parser.XsltParseException;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.transform.Templates;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
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
    private GlobalTarariContext tarariContext;
    private Stylesheet master;

    private final String[] varsUsed;

    public ServerXslTransformation(XslTransformation assertion, ApplicationContext springContext) {
        if (assertion == null) throw new IllegalArgumentException("must provide assertion");
        subject = assertion;
        auditor = new Auditor(this, springContext, logger);
        varsUsed = assertion.getVariablesUsed();

        tarariContext = TarariLoader.getGlobalContext();
        // if we're in tarari mode, prepare a stylesheet object
        if (tarariContext != null) {
            try {
                master = Stylesheet.create(new XmlSource(subject.getXslSrc().getBytes()));
            } catch (XsltParseException e) {
                logger.log(Level.SEVERE, "cannot create tarari stylesheet, will operate in software mode", e);
            } catch (IOException e) {
                logger.log(Level.SEVERE, "cannot create tarari stylesheet, will operate in software mode", e);
            } catch (XmlParseException e) {
                logger.log(Level.SEVERE, "cannot create tarari stylesheet, will operate in software mode", e);
            } catch (UnsatisfiedLinkError e) {
                logger.log(Level.SEVERE, "cannot create tarari stylesheet, will operate in software mode", e);
            }
            if (master == null) {
                tarariContext = null;
            }
        }
    }

    /**
     * preformes the transformation
     * @param context
     */
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        // 1. Get document to transform
        final Message msgtotransform;

        switch (subject.getDirection()) {
            case XslTransformation.APPLY_TO_REQUEST:
                auditor.logAndAudit(AssertionMessages.XSLT_REQUEST);
                msgtotransform = context.getRequest();
                break;
            case XslTransformation.APPLY_TO_RESPONSE:
                auditor.logAndAudit(AssertionMessages.XSLT_RESPONSE);
                msgtotransform = context.getResponse();
                break;
            default:
                // should not get here!
                auditor.logAndAudit(AssertionMessages.XSLT_CONFIG_ISSUE);
                return AssertionStatus.SERVER_ERROR;
        }

        int whichMimePart = subject.getWhichMimePart();
        if (whichMimePart <= 0) whichMimePart = 0;

        Map vars = context.getVariableMap(varsUsed, auditor);

        // 2. Apply the transformation
        if (tarariContext == null) {
            final Document doctotransform;

            PartInfo mimePart = null;
            try {
                if (whichMimePart == 0) {
                    if (!msgtotransform.isXml()) return notXml();
                    doctotransform = msgtotransform.getXmlKnob().getDocumentWritable();
                } else {
                    try {
                        mimePart = msgtotransform.getMimeKnob().getPart(whichMimePart);
                        if (!mimePart.getContentType().isXml()) return notXml();

                        InputStream is = mimePart.getInputStream(false);
                        doctotransform = XmlUtil.parse(is, false);
                    } catch (NoSuchPartException e) {
                        auditor.logAndAudit(AssertionMessages.XSLT_NO_SUCH_PART, new String[] { Integer.toString(whichMimePart)});
                        return AssertionStatus.BAD_REQUEST;
                    }
                }
                Document output;
                try {
                    output = XmlUtil.softXSLTransform(doctotransform, getTemplate().newTransformer(), vars);
                } catch (TransformerException e) {
                    String msg = "error transforming document";
                    auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] {msg}, e);
                    throw new PolicyAssertionException(msg, e);
                }
                if (whichMimePart == 0) {
                    msgtotransform.getXmlKnob().setDocument(output);
                } else {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    XmlUtil.nodeToOutputStream(output, baos);
                    mimePart.setBodyBytes(baos.toByteArray());
                }
                logger.finest("software xsl transformation completed");
                return AssertionStatus.NONE;
            } catch (SAXException e) {
                String msg = "cannot get document to tranform";
                auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] {msg}, e);
            }
            return AssertionStatus.FAILED;
        } else { // tarari-style xslt
            try {
                Stylesheet transformer = new Stylesheet(master);

                for (Iterator i = vars.keySet().iterator(); i.hasNext();) {
                    String name = (String)i.next();
                    Object value = vars.get(name);
                    transformer.setParameter(name, value);
                }

                transformer.setValidate(false);
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                XmlResult result = new XmlResult(output);
                XmlSource source = new XmlSource(msgtotransform.getMimeKnob().getPart(whichMimePart).getInputStream(false));

                transformer.transform(source, result);
                byte[] transformedmessage = output.toByteArray();
                msgtotransform.getMimeKnob().getPart(whichMimePart).setBodyBytes(transformedmessage);
                logger.finest("tarari xsl transformation completed. result: " + new String(transformedmessage));
                return AssertionStatus.NONE;
            } catch (NoSuchPartException e) {
                logger.log(Level.WARNING, "Cannot operate on mime part " + whichMimePart +
                                          " while trying to xsl transform in tarari mode", e);
            } catch (IOException e) {
                logger.log(Level.WARNING, "Tarari exception when attempting xsl transformation", e);
            } catch (XmlParseException e) {
                logger.log(Level.WARNING, "Tarari exception when attempting xsl transformation", e);
            } catch (XsltException e) {
                logger.log(Level.WARNING, "Tarari exception when attempting xsl transformation", e);
            }
            return AssertionStatus.FAILED;
        }
    }

    private AssertionStatus notXml() {
        auditor.logAndAudit(AssertionMessages.XSLT_REQ_NOT_XML);
        return AssertionStatus.NOT_APPLICABLE;
    }

    private Templates makeTemplate(String xslstr) throws TransformerConfigurationException {
        TransformerFactory transfoctory = TransformerFactory.newInstance();
        StreamSource xsltsource = new StreamSource(new StringReader(xslstr));
        return transfoctory.newTemplates(xsltsource);
    }

    private Templates getTemplate() throws TransformerConfigurationException {
        if (template == null) {
            template = makeTemplate(subject.getXslSrc());
        }
        return template;
    }
}
