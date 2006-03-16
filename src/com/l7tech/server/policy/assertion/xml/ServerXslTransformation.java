package com.l7tech.server.policy.assertion.xml;

import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.common.audit.Auditor;
import com.l7tech.common.io.BufferPoolByteArrayOutputStream;
import com.l7tech.common.message.Message;
import com.l7tech.common.message.TarariKnob;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.mime.PartInfo;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.TarariLoader;
import com.l7tech.common.xml.tarari.GlobalTarariContext;
import com.l7tech.common.xml.tarari.TarariCompiledStylesheet;
import com.l7tech.common.xml.tarari.TarariMessageContext;
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
import java.io.InputStream;
import java.io.StringReader;
import java.text.ParseException;
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
public class  ServerXslTransformation implements ServerAssertion {
    private static final Logger logger = Logger.getLogger(ServerXslTransformation.class.getName());

    private final Auditor auditor;
    private final XslTransformation subject;
    private final Templates softwareStylesheet;
    private final TarariCompiledStylesheet tarariStylesheet;

    private final String[] varsUsed;

    public ServerXslTransformation(XslTransformation assertion, ApplicationContext springContext) {
        if (assertion == null) throw new IllegalArgumentException("must provide assertion");
        subject = assertion;
        auditor = new Auditor(this, springContext, logger);
        varsUsed = assertion.getVariablesUsed();

        // Prepare a software template
        Templates softwareStylesheet = null;
        try {
            softwareStylesheet = makeTemplate(subject.getXslSrc());
        } catch (TransformerConfigurationException e) {
            String msg = "Invalid stylesheet - XSL assertion will always fail";
            auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] {msg}, e);
        }
        this.softwareStylesheet = softwareStylesheet;

        // if we're in tarari mode, prepare a stylesheet object
        TarariCompiledStylesheet tarariStylesheet = null;
        GlobalTarariContext tarariContext = TarariLoader.getGlobalContext();
        if (tarariContext != null) {
            try {
                tarariStylesheet = tarariContext.compileStylesheet(subject.getXslSrc().getBytes());
            } catch (ParseException e) {
                logger.log(Level.WARNING, "cannot create tarari stylesheet, will operate in software mode", e);
            }
        }
        this.tarariStylesheet = tarariStylesheet;
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
        if (tarariStylesheet != null)
            return runWithTarari(whichMimePart, msgtotransform);
        return runInSoftware(whichMimePart, msgtotransform, vars);
    }

    private AssertionStatus runInSoftware(int whichMimePart, Message msgtotransform, Map vars)
            throws IOException, PolicyAssertionException
    {
            if (softwareStylesheet == null) {
                String msg = "Invalid stylesheet - assertion fails";
                auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING, new String[] {msg});
                throw new PolicyAssertionException(subject, msg);
            }

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
                    output = XmlUtil.softXSLTransform(doctotransform, softwareStylesheet.newTransformer(), vars);
                } catch (TransformerException e) {
                    String msg = "error transforming document";
                    auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] {msg}, e);
                    throw new PolicyAssertionException(subject, msg, e);
                }
                if (whichMimePart == 0) {
                    msgtotransform.getXmlKnob().setDocument(output);
                } else {
                    BufferPoolByteArrayOutputStream baos = new BufferPoolByteArrayOutputStream(4096);
                    try {
                        XmlUtil.nodeToOutputStream(output, baos);
                        mimePart.setBodyBytes(baos.toByteArray());
                    } finally {
                        baos.close();
                    }
                }
                logger.finest("software xsl transformation completed");
                return AssertionStatus.NONE;
            } catch (SAXException e) {
                String msg = "cannot get document to tranform";
                auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] {msg}, e);
            }
            return AssertionStatus.FAILED;
    }

    private AssertionStatus runWithTarari(int whichMimePart, Message msgtotransform)
    {
            try {
                BufferPoolByteArrayOutputStream output = new BufferPoolByteArrayOutputStream(4096);
                byte[] transformedmessage = null;
                try {
                    // If we are transforming the first part, first try to reuse an existing RaxDocument
                    if (whichMimePart == 0) {
                        TarariKnob tk = (TarariKnob)msgtotransform.getKnob(TarariKnob.class);
                        if (tk != null) {
                            TarariMessageContext tc = tk.getContext();
                            if (tc != null) {
                                tarariStylesheet.transform(tc, output);
                                transformedmessage = output.toByteArray();
                            }
                        }
                    }

                    if (transformedmessage == null) {
                        InputStream input = msgtotransform.getMimeKnob().getPart(whichMimePart).getInputStream(false);
                        tarariStylesheet.transform(input, output);
                        transformedmessage = output.toByteArray();
                    }
                } finally {
                    output.close();
                }
                msgtotransform.getMimeKnob().getPart(whichMimePart).setBodyBytes(transformedmessage);
                logger.finest("tarari xsl transformation completed. result: " + new String(transformedmessage));
                return AssertionStatus.NONE;
            } catch (NoSuchPartException e) {
                logger.log(Level.WARNING, "Cannot operate on mime part " + whichMimePart +
                                          " while trying to xsl transform in tarari mode", e);
            } catch (IOException e) {
                logger.log(Level.WARNING, "IOException when attempting xsl transformation", e);
            } catch (SAXException e) {
                String msg = "cannot get document to tranform";
                auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] {msg}, e);
            }
            return AssertionStatus.FAILED;
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
}
