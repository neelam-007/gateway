package com.l7tech.server.policy.assertion.xml;

import com.l7tech.common.http.GenericHttpClientFactory;
import com.l7tech.common.mime.ByteArrayStashManager;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.mime.PartInfo;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.Message;
import com.l7tech.message.XmlKnob;
import com.l7tech.policy.AssertionResourceInfo;
import com.l7tech.policy.MessageUrlResourceInfo;
import com.l7tech.policy.SingleUrlResourceInfo;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.assertion.xml.XslTransformation;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.ServerPolicyException;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.url.AbstractUrlObjectCache;
import com.l7tech.server.url.HttpObjectCache;
import com.l7tech.server.url.UrlResolver;
import com.l7tech.server.util.res.ResourceGetter;
import com.l7tech.server.util.res.ResourceObjectFactory;
import com.l7tech.server.util.res.UrlFinder;
import com.l7tech.util.*;
import com.l7tech.xml.ElementCursor;
import com.l7tech.xml.InvalidXpathException;
import com.l7tech.xml.xpath.CompiledXpath;
import com.l7tech.xml.xpath.XpathExpression;
import com.l7tech.xml.xpath.XpathResult;
import com.l7tech.xml.xpath.XpathResultNodeSet;
import com.l7tech.xml.xslt.CompiledStylesheet;
import com.l7tech.xml.xslt.StylesheetCompiler;
import com.l7tech.xml.xslt.TransformInput;
import com.l7tech.xml.xslt.TransformOutput;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.BeanFactory;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Server-side implementation of {@link XslTransformation}.
 */
public class ServerXslTransformation
        extends AbstractServerAssertion<XslTransformation>
{
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
                    @Override
                    public CompiledStylesheet createUserObject(String url, AbstractUrlObjectCache.UserObjectSource responseSource) throws IOException {
                        String response = responseSource.getString(true);                        
                        try {
                            String xsltVersion = getXsltVersionFromUrl(url);
                            return StylesheetCompiler.compileStylesheet(response, xsltVersion);
                        } catch (ParseException e) {
                            throw new CausedIOException(e);
                        }
                    }
                };

    /** A cache for remotely loaded stylesheets. */
    static UrlResolver<CompiledStylesheet> httpObjectCache = null;

    final ResourceGetter<CompiledStylesheet, XmlKnob> resourceGetter;
    final boolean allowMessagesWithNoProcessingInstruction;
    final String[] urlVarsUsed;

    public ServerXslTransformation(XslTransformation assertion, BeanFactory beanFactory) throws ServerPolicyException {
        super(assertion);

        this.urlVarsUsed = assertion.getVariablesUsed();

        // Create ResourceGetter that will produce the XSLT for us, depending on assertion config

        ResourceObjectFactory<CompiledStylesheet> resourceObjectfactory =
                new ResourceObjectFactory<CompiledStylesheet>()
                {
                    @Override
                    public CompiledStylesheet createResourceObject(final String resourceString) throws ParseException {
                        try {
                            return cacheObjectFactory.createUserObject(decorateWithXsltVersion(""), new AbstractUrlObjectCache.UserObjectSource(){
                                @Override
                                public byte[] getBytes() throws IOException {
                                    throw new IOException("Not supported");
                                }
                                @Override
                                public ContentTypeHeader getContentType() {
                                    return null;
                                }
                                @Override
                                public String getString(boolean isXml) {
                                    return resourceString;
                                }
                            });
                        } catch (IOException e) {
                            throw (ParseException)new ParseException("Unable to parse stylesheet: " +
                                    ExceptionUtils.getMessage(e), 0).initCause(e);
                        }
                    }
                    @Override
                    public void closeResourceObject( final CompiledStylesheet resourceObject ) {
                    }
                };

        UrlFinder<XmlKnob> urlFinder = new UrlFinder<XmlKnob>() {
            /**
             *
             * @param message the XmlKnob to inspect.  Never null. The knob's ElementCursor may be moved by this method.
             * @return String URL found from the message
             * @throws ResourceGetter.InvalidMessageException
             */
            @Override
            public String findUrl(XmlKnob message) throws ResourceGetter.InvalidMessageException {
                try {
                    return findXslHref(message);
                } catch (SAXException e) {
                    throw new ResourceGetter.InvalidMessageException(e);
                } catch (InvalidDocumentFormatException e) {
                    throw new ResourceGetter.InvalidMessageException(e);
                } catch (IOException e) {
                    throw new ResourceGetter.InvalidMessageException(e);
                }
            }
        };

        MessageUrlResourceInfo muri = null;
        final AssertionResourceInfo ri = assertion.getResourceInfo();
        if (ri instanceof MessageUrlResourceInfo)
            muri = (MessageUrlResourceInfo)ri;
        allowMessagesWithNoProcessingInstruction = muri != null && muri.isAllowMessagesWithoutUrl();

        if (ri instanceof SingleUrlResourceInfo) {
            // Ensure XSLT version gets tacked onto URL string, so XSLT 2.0 uses the correct engine
            SingleUrlResourceInfo resourceInfo = (SingleUrlResourceInfo) ri;
            resourceInfo.setUrl(decorateWithXsltVersion(resourceInfo.getUrl()));
        }

        this.resourceGetter = ResourceGetter.createResourceGetter(
                assertion, ri, resourceObjectfactory, urlFinder, getCache(cacheObjectFactory, beanFactory), getAudit());
    }

    protected UrlResolver<CompiledStylesheet> getCache( final HttpObjectCache.UserObjectFactory<CompiledStylesheet> cacheObjectFactory,
                                                        final BeanFactory spring ) {
        synchronized(ServerXslTransformation.class) {
            if (httpObjectCache != null)
                return httpObjectCache;

            GenericHttpClientFactory clientFactory = (GenericHttpClientFactory)spring.getBean("httpClientFactory");
            if (clientFactory == null) throw new IllegalStateException("No httpClientFactory bean");

            Config config = validated( ServerConfig.getInstance(), logger );
            httpObjectCache = new HttpObjectCache<CompiledStylesheet>(
                        "XSL-T",
                        config.getIntProperty( ServerConfigParams.PARAM_XSLT_CACHE_MAX_ENTRIES, 10000),
                        config.getIntProperty( ServerConfigParams.PARAM_XSLT_CACHE_MAX_AGE, 300000),
                        config.getIntProperty( ServerConfigParams.PARAM_XSLT_CACHE_MAX_STALE_AGE, -1),
                        clientFactory, cacheObjectFactory, HttpObjectCache.WAIT_INITIAL, ServerConfigParams.PARAM_XSL_MAX_DOWNLOAD_SIZE);

            return httpObjectCache;
        }
    }

    private static Config validated( final Config config, final Logger logger ) {
        final ValidatedConfig vc = new ValidatedConfig( config, logger, new Resolver<String,String>(){
            @Override
            public String resolve( final String key ) {
                return ServerConfig.getInstance().getClusterPropertyName( key );
            }
        } );

        vc.setMinimumValue( ServerConfigParams.PARAM_XSLT_CACHE_MAX_ENTRIES, 0 );
        vc.setMaximumValue( ServerConfigParams.PARAM_XSLT_CACHE_MAX_ENTRIES, 1000000 );

        vc.setMinimumValue( ServerConfigParams.PARAM_XSLT_CACHE_MAX_STALE_AGE, -1 );

        return vc;
    }

    @Override
    public AssertionStatus checkRequest(final PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        // Reset variables
        final String xsltMsgVarName = getXsltMessagesContextVariableName();
        context.setVariable(xsltMsgVarName, new String[] {""});
        context.setVariable(xsltMsgVarName + ".first", "");
        context.setVariable(xsltMsgVarName + ".last", "");

        // Get document to transform
        final Message message;
        final TargetMessageType isrequest = assertion.getTarget();

        switch (isrequest) {
            case REQUEST:
                logAndAudit(AssertionMessages.XSLT_REQUEST);
                message = context.getRequest();
                break;
            case RESPONSE:
                logAndAudit(AssertionMessages.XSLT_RESPONSE);
                message = context.getResponse();
                break;
            case OTHER:
                final String mvar = assertion.getOtherTargetMessageVariable();
                if (mvar == null) throw new PolicyAssertionException(assertion, "Target message variable not set");
                try {
                    logAndAudit(AssertionMessages.XSLT_OTHER, mvar);
                    message = context.getTargetMessage(assertion, true);
                    break;
                } catch (NoSuchVariableException e) {
                    logAndAudit(AssertionMessages.NO_SUCH_VARIABLE, new String[]{ e.getVariable() }, e);
                    return AssertionStatus.FAILED;
                }
            default:
                // should not get here!
                logAndAudit(AssertionMessages.XSLT_CONFIG_ISSUE);
                return AssertionStatus.SERVER_ERROR;
        }

        final List<String> xsltMessages = new ArrayList<String>();
        try {
            return doCheckRequest(message, context, xsltMessages);
        } catch (SAXException e) {
            logAndAudit(AssertionMessages.XSLT_MSG_NOT_XML);
            return AssertionStatus.BAD_REQUEST;
        } finally {
            if ( !xsltMessages.isEmpty() ) {
                context.setVariable(xsltMsgVarName, xsltMessages.toArray( new String[xsltMessages.size()] ));
                context.setVariable(xsltMsgVarName + ".first", xsltMessages.get( 0 ));
                context.setVariable(xsltMsgVarName + ".last", xsltMessages.get( xsltMessages.size()-1 ));
            }
        }
    }


    /*
     * Performs the transformation
     */
    private AssertionStatus doCheckRequest( final Message message,
                                            final PolicyEnforcementContext context,
                                            final List<String> xsltMessages )
            throws IOException, PolicyAssertionException, SAXException
    {

        int whichMimePart = assertion.getWhichMimePart();
        if (whichMimePart <= 0) whichMimePart = 0;

        Functions.Unary<Object, String> variableGetter = new Functions.Unary<Object, String>() {
            @Override
            public Object call(String varName) {
                try {
                    return context.getVariable(varName);
                } catch (NoSuchVariableException e) {
                    logAndAudit(AssertionMessages.NO_SUCH_VARIABLE, varName);
                    return null;
                }
            }
        };

        TransformInput transformInput = null;
        final TransformOutput transformOutput;
        try {
            if (whichMimePart == 0) {
                // Special case for part zero
                final XmlKnob xmlKnob;
                try {
                    xmlKnob = message.getXmlKnob();
                } catch (SAXException e) {
                    logAndAudit(AssertionMessages.XSLT_MSG_NOT_XML);
                    return AssertionStatus.BAD_REQUEST;
                }
                transformInput = makeFirstPartTransformInput(xmlKnob, variableGetter);
                transformOutput = new TransformOutput() {
                    @Override
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
                    logAndAudit(AssertionMessages.XSLT_NO_SUCH_PART, Integer.toString(whichMimePart));
                    return AssertionStatus.BAD_REQUEST;
                }

                transformInput = makePartInfoTransformInput(partInfo, variableGetter);
                transformOutput = new TransformOutput() {
                    @Override
                    public void setBytes(byte[] bytes) throws IOException {
                        partInfo.setBodyBytes(bytes);
                    }
                };
            }

            // These variables are used ONLY for interpolation into a remote URL; variables used inside
            // the stylesheet itself are fed in via the variableGetter inside TransformInput
            Map<String,Object> urlVars = context.getVariableMap(urlVarsUsed, getAudit());
            return transform(transformInput, transformOutput, urlVars, xsltMessages);
        } finally {
            if (transformInput != null) {
                transformInput.close();
            }
        }
    }

    //  Get a stylesheet from the resourceGetter and transform input into output
    private AssertionStatus transform( final TransformInput input,
                                       final TransformOutput output,
                                       final Map<String,Object> urlVars,
                                       final List<String> xsltMessages )
            throws IOException, PolicyAssertionException
    {
        try {
            CompiledStylesheet resource = resourceGetter.getResource(input.getXmlKnob(), urlVars);

            if (resource == null) {
                if (allowMessagesWithNoProcessingInstruction) {
                    logAndAudit(AssertionMessages.XSLT_NO_PI_OK);
                    return AssertionStatus.NONE;
                }

                // Can't happen
                logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "Internal server error: null resource");
                return AssertionStatus.SERVER_ERROR;
            }

            resource.transform(input, output, getErrorListener(xsltMessages));
            return AssertionStatus.NONE;

        } catch (ResourceGetter.InvalidMessageException e) {
            logAndAudit(AssertionMessages.XSLT_MSG_NOT_XML);
            return AssertionStatus.BAD_REQUEST;
        } catch (SAXException e) {
            logAndAudit(AssertionMessages.XSLT_MSG_NOT_XML);
            return AssertionStatus.BAD_REQUEST;
        } catch (ResourceGetter.MalformedResourceUrlException e) {
            logAndAudit(AssertionMessages.XSLT_CANT_READ_XSL, e.getUrl(), "URL is invalid");
            return AssertionStatus.BAD_REQUEST;
        } catch (ResourceGetter.UrlNotPermittedException e) {
            logAndAudit(AssertionMessages.XSLT_BAD_URL, e.getUrl());
            return AssertionStatus.BAD_REQUEST;
        } catch (ResourceGetter.ResourceIOException e) {
            //noinspection ThrowableResultOfMethodCallIgnored
            logAndAudit(AssertionMessages.XSLT_CANT_READ_XSL, new String[]{e.getUrl(), ExceptionUtils.getMessage(e)}, ExceptionUtils.getDebugException(e));
            return AssertionStatus.SERVER_ERROR;
        } catch (ResourceGetter.ResourceParseException e) {
            //noinspection ThrowableResultOfMethodCallIgnored
            logAndAudit(AssertionMessages.XSLT_BAD_EXT_XSL, new String[]{e.getUrl(), ExceptionUtils.getMessage(e)}, ExceptionUtils.getDebugException(e));
            return AssertionStatus.SERVER_ERROR;
        } catch (GeneralSecurityException e) {
            logAndAudit(AssertionMessages.XSLT_CANT_READ_XSL, "HTTPS url: unable to create an SSL context", ExceptionUtils.getMessage(e));
            return AssertionStatus.SERVER_ERROR;
        } catch (ResourceGetter.UrlNotFoundException e) {
            logAndAudit(AssertionMessages.XSLT_NO_PI);
            return AssertionStatus.BAD_REQUEST;
        } catch (TransformerException e) {
            AssertionStatus status = AssertionStatus.SERVER_ERROR;
            final Throwable cause = e.getCause();
            if ( (cause != null && "terminate".equals(cause.getMessage())) ||
                 (cause == null && "Stylesheet directed termination".equals(e.getMessage())) ) {
                status = AssertionStatus.FALSIFIED; // Stylesheet directed termination, already audited
            } else {
                String msg = "Error transforming document: " + ExceptionUtils.getMessage( e );
                //noinspection ThrowableResultOfMethodCallIgnored
                logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] {msg}, ExceptionUtils.getDebugException(e));
                // bug #6486 - Do not re-throw transform exception as a PolicyAssertionException
            }
            return status;
        }
    }

    private ErrorListener getErrorListener( final List<String> xsltMessages ) {
        return new ErrorListener() {
            @Override
            public void warning(TransformerException exception) throws TransformerException {
                xsltMessages.add( ExceptionUtils.getMessage(exception) );
                logAndAudit(AssertionMessages.XSLT_TRANS_WARN, exception.getMessageAndLocation());
            }
            @Override
            public void error(TransformerException exception) throws TransformerException {
                xsltMessages.add( ExceptionUtils.getMessage(exception) );
                logAndAudit(AssertionMessages.XSLT_TRANS_ERR, exception.getMessageAndLocation());
            }
            @Override
            public void fatalError(TransformerException exception) throws TransformerException {
                final String message = ExceptionUtils.getMessage(exception);
                if ( !xsltMessages.contains(message) ) { // can be reported more than once
                    xsltMessages.add( message );
                }
                throw exception;
            }
        };
    }

    private String getXsltMessagesContextVariableName() {
        return assertion.getMsgVarPrefix() + "." + XslTransformation.VARIABLE_NAME;
    }

    /**
     * Get the URL of the xml-stylesheet processing instruction in this document, if any.
     *
     * @param knob the XmlKnob to check for processing instructions.  Required.
     * @return the href attribute of the singular xml-stylesheet PI with type text/xsl, or null
     *         if no xml-stylesheet processing instructions were found
     * @throws SAXException if there was an xml-stylesheet PI that was not well-formed
     * @throws InvalidDocumentFormatException if there was more than one xml-stylesheet with type text/xsl
     * @throws java.io.IOException if there is a problem reading the source document
     */
    private String findXslHref(XmlKnob knob) throws SAXException, InvalidDocumentFormatException, IOException {
        if (!knob.isDomParsed() && !knob.isTarariParsed()) {
            // TODO use StAX to find URL without parsing the whole document, if it hasn't already been parsed (and hence we are going to end up streaming it into the actual transform)
            // the goal is to avoid forcing a DOM or Tarari parse when we are just going to throw it all away after the transform anyway
            //return findXslHrefWithoutDomOrTarariParse_perhapsUsingStAX(knob);
        }

        ElementCursor ec = knob.getElementCursor();
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
            if (pis != null && !pis.isEmpty()) {
                if (pis.size() != 1) {
                    logAndAudit(AssertionMessages.XSLT_MULTIPLE_PIS);
                    throw new InvalidDocumentFormatException();
                }
                String val = pis.getNodeValue(0);
                String url = extractHref(val);
                return url == null ? null : decorateWithXsltVersion(url);
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
    @Nullable
    private String extractHref(String attrlist) throws SAXException {
        try {
            String fakeXml = "<dummy " + attrlist + " />";
            SAXParser parser = piParser.newSAXParser();

            final String[] found = new String[] { null, null }; // name, type
            parser.parse(new ByteArrayInputStream(fakeXml.getBytes()), new DefaultHandler() {
                @Override
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

    private static TransformInput makeFirstPartTransformInput(XmlKnob xmlKnob, Functions.Unary<Object, String> variableGetter) throws IOException, SAXException {
        return new TransformInput(xmlKnob, null, variableGetter);
    }

    /*
     * This factory for TransformInput has the special case and slower code needed to do Tarari transformation
     * of a MIME part other than the first part.
     */
    private TransformInput makePartInfoTransformInput(PartInfo partInfo, Functions.Unary<Object, String> variableGetter) throws IOException, SAXException {
        try {
            // Destructively parse the part, since it will be overwritten with the transformation output
            Message partMessage = new Message(new ByteArrayStashManager(), ContentTypeHeader.XML_DEFAULT, partInfo.getInputStream(true));
            return new TransformInput(partMessage.getXmlKnob(), partMessage, variableGetter);
        } catch (NoSuchPartException e) {
            throw new IOException("MIME part has already been destructively read");
        }
    }

    private static String getXsltVersionFromUrl(@NotNull String urlString) {
        if (urlString.endsWith("#xslt20")) {
            return "2.0";
        } else {
            return null;
        }
    }

    // Append "#xslt20" to the specified URL string if the current assertion is configured with an explicit XSLT 2.0 version.
    private String decorateWithXsltVersion(@NotNull String urlString) {
        return "2.0".equals(assertion.getXsltVersion()) ? (urlString + "#xslt20") : urlString;
    }
}
