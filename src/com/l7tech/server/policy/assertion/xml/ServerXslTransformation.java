/*
 * Copyright (C) 2003-2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.policy.assertion.xml;

import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.common.audit.Auditor;
import com.l7tech.common.io.BufferPoolByteArrayOutputStream;
import com.l7tech.common.message.Message;
import com.l7tech.common.message.TarariKnob;
import com.l7tech.common.message.TarariMessageContextFactory;
import com.l7tech.common.message.XmlKnob;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.mime.PartInfo;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.*;
import com.l7tech.common.xml.tarari.GlobalTarariContext;
import com.l7tech.common.xml.tarari.TarariCompiledStylesheet;
import com.l7tech.common.xml.tarari.TarariMessageContext;
import com.l7tech.common.xml.xpath.CompiledXpath;
import com.l7tech.common.xml.xpath.XpathResult;
import com.l7tech.common.xml.xpath.XpathResultNodeSet;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xml.XslTransformation;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.ServerPolicyException;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.assertion.ServerAssertion;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPathExpressionException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.text.ParseException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Server-side implementation of {@link XslTransformation}.
 */
public class ServerXslTransformation extends AbstractServerAssertion implements ServerAssertion {
    private static final Logger logger = Logger.getLogger(ServerXslTransformation.class.getName());
    private static final SAXParserFactory piParser = SAXParserFactory.newInstance();
    static {
        piParser.setNamespaceAware(false);
        piParser.setValidating(false);
    }

    private static final CompiledXpath findStylesheetPIs;
    static {
        try {
            findStylesheetPIs = new XpathExpression("processing-instruction('xml-stylesheet')").compile();
        } catch (InvalidXpathException e) {
            throw new RuntimeException(e); // Can't happen
        }
    }

    private final Auditor auditor;
    private final XslTransformation assertion;
    private final ResourceGetter resourceGetter;
    private final String[] varsUsed;


    public ServerXslTransformation(XslTransformation assertion, ApplicationContext springContext) throws ServerPolicyException {
        super(assertion);
        if (assertion == null) throw new IllegalArgumentException("must provide assertion");

        this.assertion = assertion;
        this.auditor = new Auditor(this, springContext, logger);
        this.varsUsed = assertion.getVariablesUsed();

        // Create ResourceGetter that will produce the XSLT for us, depending on assertion config
        ResourceGetter.ResourceObjectFactory resourceObjectfactory = new ResourceGetter.ResourceObjectFactory() {
            public Object createResourceObject(byte[] resourceBytes) throws ParseException {
                final GlobalTarariContext gtc = TarariLoader.getGlobalContext();
                TarariCompiledStylesheet tarariStylesheet = gtc == null ? null : gtc.compileStylesheet(resourceBytes);
                return new CachedStylesheet(compileSoftware(resourceBytes), tarariStylesheet);
            }
        };

        ResourceGetter.UrlFinder urlFinder = new ResourceGetter.UrlFinder() {
            public String findUrl(ElementCursor message) throws ResourceGetter.InvalidMessageException {
                try {
                    return findXslHref(message);
                } catch (SAXException e) {
                    throw new ResourceGetter.InvalidMessageException(e);
                } catch (InvalidDocumentFormatException e) {
                    throw new ResourceGetter.InvalidMessageException(e);
                }
            }
        };

        this.resourceGetter = ResourceGetter.createResourceGetter(assertion,
                                                                  resourceObjectfactory,
                                                                  urlFinder,
                                                                  springContext,
                                                                  auditor);
    }

    /**
     * @return successfully compiled stylesheet.  Never null
     * @throws ParseException if the stylesheet can't be parsed
     */
    private static Templates compileSoftware(byte[] bytes) throws ParseException {
        // Prepare a software template
        try {
            TransformerFactory transfoctory = TransformerFactory.newInstance();
            StreamSource xsltsource = new StreamSource(new ByteArrayInputStream(bytes));
            return transfoctory.newTemplates(xsltsource);
        } catch (TransformerConfigurationException e) {
            throw (ParseException)new ParseException(ExceptionUtils.getMessage(e), 0).initCause(e);
        }
    }

    private abstract class TransformOutput {
        abstract void setDocument(Document doc);
        abstract void setBytes(byte[] bytes) throws IOException;
    }

    private abstract class TransformInput {
        private final Map vars;

        protected TransformInput(Map vars) {
            this.vars = vars;
        }

        abstract Document asDocument() throws SAXException, IOException;

        abstract TarariMessageContext asTarari() throws IOException, SAXException;

        abstract ElementCursor getElementCursor() throws SAXException, IOException;

        Auditor getAuditor() { return auditor; }

        Assertion getAssertion() { return assertion; }
    }

    /**
     * preformes the transformation
     * @param context
     */
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        // 1. Get document to transform
        final Message message;
        final boolean isrequest;

        switch (assertion.getDirection()) {
            case XslTransformation.APPLY_TO_REQUEST:
                auditor.logAndAudit(AssertionMessages.XSLT_REQUEST);
                isrequest = true;
                message = context.getRequest();
                break;
            case XslTransformation.APPLY_TO_RESPONSE:
                auditor.logAndAudit(AssertionMessages.XSLT_RESPONSE);
                isrequest = false;
                message = context.getResponse();
                break;
            default:
                // should not get here!
                auditor.logAndAudit(AssertionMessages.XSLT_CONFIG_ISSUE);
                return AssertionStatus.SERVER_ERROR;
        }

        int whichMimePart = assertion.getWhichMimePart();
        if (whichMimePart <= 0) whichMimePart = 0;

        Map vars = context.getVariableMap(varsUsed, auditor);

        final TransformInput transformInput;
        final TransformOutput transformOutput;
        if (whichMimePart == 0) {
            // Special case for part zero
            final XmlKnob xmlKnob;
            try {
                xmlKnob = message.getXmlKnob();
            } catch (SAXException e) {
                auditor.logAndAudit(isrequest ? AssertionMessages.XSLT_REQ_NOT_XML : AssertionMessages.XSLT_RESP_NOT_XML);
                return AssertionStatus.BAD_REQUEST;
            }
            transformInput = new FirstPartTransformInput(message, xmlKnob, vars);
            transformOutput = new FirstPartTransformOutput(message);
        } else {
            // Make a new PartInfo based input and/or output
            final PartInfo partInfo;
            try {
                partInfo = message.getMimeKnob().getPart(whichMimePart);
            } catch (NoSuchPartException e) {
                auditor.logAndAudit(AssertionMessages.XSLT_NO_SUCH_PART, new String[] { Integer.toString(whichMimePart) });
                return AssertionStatus.BAD_REQUEST;
            }

            transformInput = new PartInfoTransformInput(partInfo, vars);
            transformOutput = new PartInfoTransformOutput(partInfo);
        }

        return transform(transformInput, transformOutput, isrequest);
    }

    /** Get a stylesheet from the resourceGetter and transform input into output. */
    private AssertionStatus transform(TransformInput input, TransformOutput output, boolean isReq)
            throws IOException, PolicyAssertionException
    {
        try {
            final ElementCursor ec = input.getElementCursor();
            Object resource = resourceGetter.getResource(ec);
            if (resource instanceof CachedStylesheet) {
                return ((CachedStylesheet)resource).transform(input, output);
            } else {
                if (resource == null) {
                    // Can't happen
                    auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] {"Internal server error: null resource"});
                    return AssertionStatus.SERVER_ERROR;
                }

                // XXX This is a design flaw when cache shared with Schema Val.  Not yet fixed.  See Bug #2535
                auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO,
                                    new String[] {"The specified XSL URL has recently been fetched and found to contain something other than XSL"});
                return AssertionStatus.SERVER_ERROR;
            }
        } catch (ResourceGetter.InvalidMessageException e) {
            auditor.logAndAudit(isReq ? AssertionMessages.XSLT_REQ_NOT_XML : AssertionMessages.XSLT_RESP_NOT_XML);
            return AssertionStatus.BAD_REQUEST;
        } catch (SAXException e) {
            auditor.logAndAudit(isReq ? AssertionMessages.XSLT_REQ_NOT_XML : AssertionMessages.XSLT_RESP_NOT_XML);
            return AssertionStatus.BAD_REQUEST;
        } catch (ResourceGetter.MalformedResourceUrlException e) {
            auditor.logAndAudit(AssertionMessages.XSLT_CANT_READ_XSL, new String[] {e.getUrl(), "URL is invalid"});
            return AssertionStatus.BAD_REQUEST;
        } catch (ResourceGetter.UrlNotPermittedException e) {
            auditor.logAndAudit(AssertionMessages.XSLT_BAD_URL, new String[] {e.getUrl()});
            return AssertionStatus.BAD_REQUEST;
        } catch (ResourceGetter.ResourceIOException e) {
            auditor.logAndAudit(AssertionMessages.XSLT_CANT_READ_XSL, new String[] {e.getUrl(), ExceptionUtils.getMessage(e)}, e);
            return AssertionStatus.SERVER_ERROR;
        } catch (ResourceGetter.ResourceParseException e) {
            auditor.logAndAudit(AssertionMessages.XSLT_BAD_EXT_XSL, new String[] {e.getUrl(), ExceptionUtils.getMessage(e)}, e);
            return AssertionStatus.SERVER_ERROR;
        } catch (GeneralSecurityException e) {
            auditor.logAndAudit(AssertionMessages.XSLT_CANT_READ_XSL, new String[] { "HTTPS url: unable to create an SSL context", ExceptionUtils.getMessage(e) });
            return AssertionStatus.SERVER_ERROR;
        } catch (ResourceGetter.UrlNotFoundException e) {
            auditor.logAndAudit(AssertionMessages.XSLT_NO_PI);
            return AssertionStatus.BAD_REQUEST;
        }
    }

    /**
     * Get the URL of the xml-stylesheet processing instruction in this document, if any.
     *
     * @param ec  the cursor to check for processing instructions.
     * @return the href attribute of the singular xml-stylesheet PI with type text/xsl, or null
     *         if no xml-stylesheet processing instructions were found
     * @throws SAXException if there was an xml-stylesheet PI that was not well-formed
     * @throws InvalidDocumentFormatException if there was more than one xml-stylesheet with type text/xsl
     */
    private String findXslHref(ElementCursor ec) throws SAXException, InvalidDocumentFormatException {
        ec.moveToRoot();
        XpathResult pxr = null;
        try {
            pxr = ec.getXpathResult(findStylesheetPIs);
        } catch (XPathExpressionException e) {
            // Log it, but leave it as null
            if (logger.isLoggable(Level.WARNING)) logger.log(Level.WARNING, "XPath failed: " + ExceptionUtils.getMessage(e), e);
        }
        if (pxr != null) {
            XpathResultNodeSet pis = pxr.getNodeSet();
            if (pis != null && pis.size() > 0) {
                if (pis.size() != 1) {
                    auditor.logAndAudit(AssertionMessages.XSLT_MULTIPLE_PIS);
                    throw new InvalidDocumentFormatException();
                }
                String val = pis.getNodeValue(0);
                return extractHref(val);
            }
        }
        // No processing instructions
        return null;
    }

    /**
     * Parse the attribute list from a processing instruction and verify that the type is
     * text/xsl and return the href attribute if it is.
     *
     * @param attrlist  the attribute list to parse, ie: href="blah" type="text/xsl"
     * @return the value of the href attribute, or null if this wasn't a valid text/xsl reference
     * @throws SAXException if the attribute list was not well formed
     */
    private String extractHref(String attrlist) throws SAXException {
        try {
            String fakeXml = "<dummy " + attrlist + " />";
            SAXParser parser = piParser.newSAXParser();

            final String[] found = new String[] { null, null }; // name, type
            parser.parse(new ByteArrayInputStream(fakeXml.getBytes()), new DefaultHandler() {
                public void startElement(String uri, String localName, String qName, Attributes attributes) {
                    int numAttrs = attributes.getLength();
                    for (int i = 0; i < numAttrs; ++i) {
                        String name = attributes.getQName(i);
                        String value = attributes.getValue(i);
                        if ("href".equals(name))
                            found[0] = value;
                        else if ("type".equals(name))
                            found[1] = value;
                        else
                            continue;
                        if (found[0] != null && found[1] != null)
                            return;
                    }
                }
            });

            String href = found[0];
            String type = found[1];

            if (!("text/xsl".equals(type))) {
                logger.fine("Ignoring xml-stylesheet processing instruction with non-XSL type");
                return null;
            }

            return href;
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e); // can't happen, misconfigured VM
        } catch (IOException e) {
            throw new RuntimeException(e); // can't happen, it's a bytearrayinputstream
        }
    }

    private static class CachedStylesheet {
        private TarariCompiledStylesheet tarariStylesheet;
        private Templates softwareStylesheet;

        public CachedStylesheet(Templates softwareStylesheet, TarariCompiledStylesheet tarariStylesheet) {
            this.tarariStylesheet = tarariStylesheet;
            this.softwareStylesheet = softwareStylesheet;
            if (softwareStylesheet == null)
                throw new IllegalArgumentException("softwareStylesheet must be provided");
        }

        public AssertionStatus transform(TransformInput input, TransformOutput output)
                throws SAXException, IOException, PolicyAssertionException
        {
            if (tarariStylesheet != null) {
                TarariMessageContext tmc = input.asTarari();
                if (tmc != null)
                    return transformTarari(input, tmc, output);
            }

            return transformDom(input, output);
        }

        private AssertionStatus transformTarari(TransformInput t, TarariMessageContext tmc, TransformOutput output)
                throws IOException, SAXException
        {
            assert tmc != null;
            assert tarariStylesheet != null;

            BufferPoolByteArrayOutputStream os = new BufferPoolByteArrayOutputStream();
            try {
                tarariStylesheet.transform(tmc, os, t.vars);
                output.setBytes(os.toByteArray());
                logger.finest("Tarari xsl transformation completed");
                return AssertionStatus.NONE;
            } finally {
                os.close();
            }
        }

        private AssertionStatus transformDom(TransformInput t, TransformOutput output)
                throws PolicyAssertionException, SAXException, IOException {
            final Document doctotransform = t.asDocument();
            final Document outDoc;

            try {
                outDoc = XmlUtil.softXSLTransform(doctotransform, softwareStylesheet.newTransformer(), t.vars);
            } catch (TransformerException e) {
                String msg = "error transforming document";
                t.getAuditor().logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] {msg}, e);
                throw new PolicyAssertionException(t.getAssertion(), msg, e);
            }
            output.setDocument(outDoc);
            logger.finest("software xsl transformation completed");
            return AssertionStatus.NONE;
        }
    }

    private class FirstPartTransformInput extends TransformInput {
        private final XmlKnob xmlKnob;
        private final Message message;

        public FirstPartTransformInput(Message message, XmlKnob xmlKnob, Map vars) {
            super(vars);
            this.xmlKnob = xmlKnob;
            this.message = message;
        }

        Document asDocument() throws SAXException, IOException {
            return xmlKnob.getDocumentReadOnly();
        }

        TarariMessageContext asTarari()
                throws IOException, SAXException
        {
            TarariKnob tk = (TarariKnob)message.getKnob(TarariKnob.class);
            try {
                return tk == null ? null : tk.getContext();
            } catch (NoSuchPartException e) {
                throw new RuntimeException(e); // can't happen -- we never destructively read MIME parts currently
            }
        }

        ElementCursor getElementCursor() throws SAXException, IOException {
            return xmlKnob.getElementCursor();
        }
    }

    private class FirstPartTransformOutput extends TransformOutput {
        private final Message message;

        public FirstPartTransformOutput(Message message) {
            this.message = message;
        }

        void setDocument(Document doc) {
            try {
                message.getXmlKnob().setDocument(doc);
            } catch (SAXException e) {
                throw new RuntimeException(e); // can't happen -- we already checked that the input was XML
            }
        }

        void setBytes(byte[] bytes) throws IOException {
            message.getMimeKnob().getFirstPart().setBodyBytes(bytes);
        }
    }

    /**
     * This version of TransformInput has the special case and slower code needed to do Tarari transformation
     * of a MIME part other than the first part.
     */
    private class PartInfoTransformInput extends TransformInput {
        TarariMessageContext tmc;
        Document doc;
        private final PartInfo partInfo;

        public PartInfoTransformInput(PartInfo partInfo, Map vars) {
            super(vars);
            this.partInfo = partInfo;
            tmc = null;
            doc = null;
        }

        Document asDocument() throws SAXException, IOException {
            if (doc != null) return doc;
            try {
                return doc = XmlUtil.parse(partInfo.getInputStream(false));
            } catch (NoSuchPartException e) {
                throw new RuntimeException(e); // can't happen -- we never destructively read MIME parts currently
            }
        }

        TarariMessageContext asTarari() throws IOException, SAXException {
            if (tmc != null) return tmc;
            TarariMessageContextFactory mcf = TarariLoader.getMessageContextFactory();
            if (mcf == null) return null;
            try {
                return tmc = mcf.makeMessageContext(partInfo.getInputStream(false));
            } catch (SoftwareFallbackException e) {
                if (logger.isLoggable(Level.INFO))
                    logger.log(Level.INFO, "Falling back from Tarari to software processing for XSLT on MIME part #" +
                            partInfo.getPosition(), e);
                return null;
            } catch (NoSuchPartException e) {
                throw new RuntimeException(e); // can't happen -- we never destructively read MIME parts currently
            }
        }

        ElementCursor getElementCursor() throws SAXException, IOException {
            TarariMessageContext tmc = asTarari();
            if (tmc != null) return tmc.getElementCursor();
            return new DomElementCursor(asDocument());
        }
    }

    private class PartInfoTransformOutput extends TransformOutput {
        private final PartInfo partInfo;

        public PartInfoTransformOutput(PartInfo partInfo) {
            this.partInfo = partInfo;
        }

        void setDocument(Document doc) {
            BufferPoolByteArrayOutputStream os = new BufferPoolByteArrayOutputStream();
            try {
                XmlUtil.nodeToOutputStream(doc, os);
                setBytes(os.toByteArray());
            } catch (IOException e) {
                throw new RuntimeException(e); // can't happen -- it's a byte array
            } finally {
                os.close();
            }
        }

        void setBytes(byte[] bytes) throws IOException {
            partInfo.setBodyBytes(bytes);
        }
    }
}
