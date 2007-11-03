/*
 * Copyright (C) 2003-2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.policy.assertion.xml;

import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.server.audit.Auditor;
import com.l7tech.common.urlcache.HttpObjectCache;
import com.l7tech.common.urlcache.AbstractUrlObjectCache;
import com.l7tech.common.message.Message;
import com.l7tech.common.message.TarariMessageContextFactory;
import com.l7tech.common.message.XmlKnob;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.mime.PartInfo;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.util.CausedIOException;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.*;
import com.l7tech.common.xml.tarari.TarariMessageContext;
import com.l7tech.common.xml.xpath.CompiledXpath;
import com.l7tech.common.xml.xpath.XpathResult;
import com.l7tech.common.xml.xpath.XpathResultNodeSet;
import com.l7tech.common.xml.xpath.XpathExpression;
import com.l7tech.common.xml.xslt.CompiledStylesheet;
import com.l7tech.common.xml.xslt.TransformInput;
import com.l7tech.common.xml.xslt.TransformOutput;
import com.l7tech.common.xml.xslt.StylesheetCompiler;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xml.XslTransformation;
import com.l7tech.policy.AssertionResourceInfo;
import com.l7tech.policy.MessageUrlResourceInfo;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.ServerPolicyException;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.util.HttpClientFactory;
import com.l7tech.server.util.res.ResourceGetter;
import com.l7tech.server.util.res.ResourceObjectFactory;
import com.l7tech.server.util.res.UrlFinder;
import org.springframework.context.ApplicationContext;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.TransformerException;
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
public class ServerXslTransformation
        extends AbstractServerAssertion<XslTransformation>
        implements ServerAssertion
{
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

    private static final HttpObjectCache.UserObjectFactory<CompiledStylesheet> cacheObjectFactory =
                new HttpObjectCache.UserObjectFactory<CompiledStylesheet>() {
                    public CompiledStylesheet createUserObject(String url, AbstractUrlObjectCache.UserObjectSource responseSource) throws IOException {
                        String response = responseSource.getString();                        
                        try {
                            return StylesheetCompiler.compileStylesheet(response);
                        } catch (ParseException e) {
                            throw new CausedIOException(e);
                        }
                    }
                };

    /** A cache for remotely loaded stylesheets. */
    private static HttpObjectCache<CompiledStylesheet> httpObjectCache = null;

    private final Auditor auditor;
    private final ResourceGetter<CompiledStylesheet> resourceGetter;
    private final boolean allowMessagesWithNoProcessingInstruction;
    private final String[] varsUsed;

    public ServerXslTransformation(XslTransformation assertion, ApplicationContext springContext) throws ServerPolicyException {
        super(assertion);
        if (assertion == null) throw new IllegalArgumentException("must provide assertion");

        this.auditor = new Auditor(this, springContext, logger);
        this.varsUsed = assertion.getVariablesUsed();

        // Create ResourceGetter that will produce the XSLT for us, depending on assertion config

        ResourceObjectFactory<CompiledStylesheet> resourceObjectfactory =
                new ResourceObjectFactory<CompiledStylesheet>()
                {
                    public CompiledStylesheet createResourceObject(final String resourceString) throws ParseException {
                        try {
                            return cacheObjectFactory.createUserObject("", new AbstractUrlObjectCache.UserObjectSource(){
                                public byte[] getBytes() throws IOException {
                                    throw new IOException("Not supported");
                                }
                                public ContentTypeHeader getContentType() {
                                    return null;
                                }
                                public String getString() {
                                    return resourceString;
                                }
                            });
                        } catch (IOException e) {
                            throw (ParseException)new ParseException("Unable to parse stylesheet: " +
                                    ExceptionUtils.getMessage(e), 0).initCause(e);
                        }
                    }
                };

        UrlFinder urlFinder = new UrlFinder() {
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

        MessageUrlResourceInfo muri = null;
        final AssertionResourceInfo ri = assertion.getResourceInfo();
        if (ri instanceof MessageUrlResourceInfo)
            muri = (MessageUrlResourceInfo)ri;
        allowMessagesWithNoProcessingInstruction = muri != null && muri.isAllowMessagesWithoutUrl();

        this.resourceGetter = ResourceGetter.createResourceGetter(
                assertion, ri, resourceObjectfactory, urlFinder, getCache(springContext), auditor);
    }

    private static synchronized HttpObjectCache<CompiledStylesheet> getCache(ApplicationContext spring) {
        if (httpObjectCache != null)
            return httpObjectCache;

        HttpClientFactory clientFactory = (HttpClientFactory)spring.getBean("httpClientFactory");
        if (clientFactory == null) throw new IllegalStateException("No httpClientFactory bean");

        httpObjectCache = new HttpObjectCache<CompiledStylesheet>(
                    ServerConfig.getInstance().getIntProperty(ServerConfig.PARAM_XSLT_CACHE_MAX_ENTRIES, 10000),
                    ServerConfig.getInstance().getIntProperty(ServerConfig.PARAM_XSLT_CACHE_MAX_AGE, 300000),
                    clientFactory, cacheObjectFactory, HttpObjectCache.WAIT_INITIAL);

        return httpObjectCache;
    }

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

        try {
            Map vars = context.getVariableMap(varsUsed, auditor);
            return doCheckRequest(message, isrequest, vars);
        } catch (SAXException e) {
            auditor.logAndAudit(isrequest ? AssertionMessages.XSLT_REQ_NOT_XML : AssertionMessages.XSLT_RESP_NOT_XML);
            return AssertionStatus.BAD_REQUEST;
        }
    }


    /*
     * performes the transformation
     * @param context  the context for the current request.  required
     */
    private AssertionStatus doCheckRequest(final Message message,
                                           boolean isrequest,
                                           Map vars)
            throws IOException, PolicyAssertionException, SAXException
    {

        int whichMimePart = assertion.getWhichMimePart();
        if (whichMimePart <= 0) whichMimePart = 0;

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
            transformInput = makeFirstPartTransformInput(xmlKnob, vars);
            transformOutput = new TransformOutput() {
                public void setBytes(byte[] bytes) throws IOException {
                    message.getMimeKnob().getFirstPart().setBodyBytes(bytes);
                }
            };
        } else {
            // Make a new PartInfo based input and/or output
            final PartInfo partInfo;
            try {
                partInfo = message.getMimeKnob().getPart(whichMimePart);
            } catch (NoSuchPartException e) {
                auditor.logAndAudit(AssertionMessages.XSLT_NO_SUCH_PART, Integer.toString(whichMimePart));
                return AssertionStatus.BAD_REQUEST;
            }

            transformInput = makePartInfoTransformInput(partInfo, vars);
            transformOutput = new TransformOutput() {
                public void setBytes(byte[] bytes) throws IOException {
                    partInfo.setBodyBytes(bytes);
                }
            };
        }

        return transform(transformInput, transformOutput, isrequest);
    }

    //  Get a stylesheet from the resourceGetter and transform input into output
    private AssertionStatus transform(TransformInput input, TransformOutput output, boolean isReq)
            throws IOException, PolicyAssertionException
    {
        try {
            final ElementCursor ec = input.getElementCursor();
            CompiledStylesheet resource = resourceGetter.getResource(ec, input.getVars());

            if (resource == null) {
                if (allowMessagesWithNoProcessingInstruction) {
                    auditor.logAndAudit(AssertionMessages.XSLT_NO_PI_OK);
                    return AssertionStatus.NONE;
                }

                // Can't happen
                auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "Internal server error: null resource");
                return AssertionStatus.SERVER_ERROR;
            }

            resource.transform(input, output, getErrorListener());
            return AssertionStatus.NONE;

        } catch (ResourceGetter.InvalidMessageException e) {
            auditor.logAndAudit(isReq ? AssertionMessages.XSLT_REQ_NOT_XML : AssertionMessages.XSLT_RESP_NOT_XML);
            return AssertionStatus.BAD_REQUEST;
        } catch (SAXException e) {
            auditor.logAndAudit(isReq ? AssertionMessages.XSLT_REQ_NOT_XML : AssertionMessages.XSLT_RESP_NOT_XML);
            return AssertionStatus.BAD_REQUEST;
        } catch (ResourceGetter.MalformedResourceUrlException e) {
            auditor.logAndAudit(AssertionMessages.XSLT_CANT_READ_XSL, e.getUrl(), "URL is invalid");
            return AssertionStatus.BAD_REQUEST;
        } catch (ResourceGetter.UrlNotPermittedException e) {
            auditor.logAndAudit(AssertionMessages.XSLT_BAD_URL, e.getUrl());
            return AssertionStatus.BAD_REQUEST;
        } catch (ResourceGetter.ResourceIOException e) {
            auditor.logAndAudit(AssertionMessages.XSLT_CANT_READ_XSL, new String[] {e.getUrl(), ExceptionUtils.getMessage(e)}, e);
            return AssertionStatus.SERVER_ERROR;
        } catch (ResourceGetter.ResourceParseException e) {
            auditor.logAndAudit(AssertionMessages.XSLT_BAD_EXT_XSL, new String[] {e.getUrl(), ExceptionUtils.getMessage(e)}, e);
            return AssertionStatus.SERVER_ERROR;
        } catch (GeneralSecurityException e) {
            auditor.logAndAudit(AssertionMessages.XSLT_CANT_READ_XSL, "HTTPS url: unable to create an SSL context", ExceptionUtils.getMessage(e));
            return AssertionStatus.SERVER_ERROR;
        } catch (ResourceGetter.UrlNotFoundException e) {
            auditor.logAndAudit(AssertionMessages.XSLT_NO_PI);
            return AssertionStatus.BAD_REQUEST;
        } catch (TransformerException e) {
            String msg = "error transforming document";
            auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] {msg}, e);
            throw new PolicyAssertionException(assertion, msg, e);
        }
    }

    private ErrorListener getErrorListener() {
        return new ErrorListener() {
            public void warning(TransformerException exception) throws TransformerException {
                auditor.logAndAudit(AssertionMessages.XSLT_TRANS_WARN, exception.getMessageAndLocation());
            }
            public void error(TransformerException exception) throws TransformerException {
                auditor.logAndAudit(AssertionMessages.XSLT_TRANS_ERR, exception.getMessageAndLocation());
            }
            public void fatalError(TransformerException exception) throws TransformerException {
                throw exception;
            }
        };
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

    private TransformInput makeFirstPartTransformInput(XmlKnob xmlKnob, Map vars) throws IOException, SAXException {
        return new TransformInput(xmlKnob.getElementCursor(), vars);
    }

    // Builds a TarariMessageContext for the specified PartInfo, if possible, or returns null
    private TarariMessageContext makeTarariMessageContext(PartInfo partInfo) throws IOException, SAXException {
        TarariMessageContextFactory mcf = TarariLoader.getMessageContextFactory();
        if (mcf == null) return null;
        try {
            return  mcf.makeMessageContext(partInfo.getInputStream(false));
        } catch (SoftwareFallbackException e) {
            if (logger.isLoggable(Level.INFO))
                logger.log(Level.INFO, "Falling back from Tarari to software processing for XSLT on MIME part #" +
                        partInfo.getPosition(), e);
            return null;
        } catch (NoSuchPartException e) {
            throw new RuntimeException(e); // can't happen -- we never destructively read MIME parts currently
        }
    }

    /*
     * This factory for TransformInput has the special case and slower code needed to do Tarari transformation
     * of a MIME part other than the first part.
     */
    private TransformInput makePartInfoTransformInput(PartInfo partInfo, Map vars) throws IOException, SAXException {
        TarariMessageContext tmc = makeTarariMessageContext(partInfo);
        if (tmc != null)
            return new TransformInput(tmc.getElementCursor(), vars);

        try {
            return new TransformInput(new DomElementCursor(XmlUtil.parse(partInfo.getInputStream(false))), vars);
        } catch (NoSuchPartException e) {
            throw new RuntimeException(e); // can't happen -- we never destructively read MIME parts currently
        }
    }
}
