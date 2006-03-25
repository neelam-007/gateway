/*
 * Copyright (C) 2003-2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.policy.assertion.xml;

import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.common.audit.Auditor;
import com.l7tech.common.http.GenericHttpClient;
import com.l7tech.common.http.GenericHttpRequestParams;
import com.l7tech.common.http.GenericHttpResponse;
import com.l7tech.common.http.HttpConstants;
import com.l7tech.common.http.cache.HttpObjectCache;
import com.l7tech.common.http.prov.apache.CommonsHttpClient;
import com.l7tech.common.io.BufferPoolByteArrayOutputStream;
import com.l7tech.common.message.Message;
import com.l7tech.common.message.TarariKnob;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.mime.PartInfo;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.KeystoreUtils;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.*;
import com.l7tech.common.xml.tarari.GlobalTarariContext;
import com.l7tech.common.xml.tarari.TarariCompiledStylesheet;
import com.l7tech.common.xml.tarari.TarariMessageContext;
import com.l7tech.common.xml.xpath.CompiledXpath;
import com.l7tech.common.xml.xpath.XpathResult;
import com.l7tech.common.xml.xpath.XpathResultNodeSet;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.HttpRoutingAssertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xml.XslTransformation;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.ServerPolicyException;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.transport.http.SslClientTrustManager;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
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
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Server-side implementation of {@link XslTransformation}.
 */
public class ServerXslTransformation implements ServerAssertion {
    private static final Logger logger = Logger.getLogger(ServerXslTransformation.class.getName());
    private static final SAXParserFactory piParser = SAXParserFactory.newInstance();
    static {
        piParser.setNamespaceAware(false);
        piParser.setValidating(false);
    }
    private static volatile SSLContext sslContext;
    private Pattern[] fetchUrlPatterns;

    private static final int maxCacheAge;
    static {
        String sage = ServerConfig.getInstance().getProperty(ServerConfig.PARAM_XSLT_MAX_CACHE_AGE);
        int age;
        try {
            age = Integer.parseInt(sage);
        } catch (NumberFormatException e) {
            logger.warning("Maximum cache age parameter '" + sage + "' not a valid number; using 300000 instead");
            age = 300000;
        }
        maxCacheAge = age;
    }

    private static final HttpObjectCache httpObjectCache;
    static {
        String smax = ServerConfig.getInstance().getProperty(ServerConfig.PARAM_XSLT_MAX_CACHE_ENTRIES);
        int max;
        try {
            max = Integer.parseInt(smax);
        } catch (NumberFormatException e) {
            logger.warning("Maximum cache size parameter '" + smax + "' not a valid number; using 100 instead");
            max = 100;
        }
        httpObjectCache = new HttpObjectCache(max, maxCacheAge);
    }

    private static final ThreadLocal httpRequest = new ThreadLocal() {
        protected Object initialValue() {
            return new GenericHttpRequestParams();
        }
    };

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

    private final Templates softwareStylesheet;
    private final TarariCompiledStylesheet tarariStylesheet;

    private final String[] varsUsed;

    private final ApplicationContext springContext;

    public ServerXslTransformation(XslTransformation assertion, ApplicationContext springContext) throws ServerPolicyException {
        if (assertion == null) throw new IllegalArgumentException("must provide assertion");

        this.springContext = springContext;
        this.assertion = assertion;
        this.auditor = new Auditor(this, springContext, logger);
        this.varsUsed = assertion.getVariablesUsed();

        if (assertion.isFetchXsltFromMessageUrls()) {
            softwareStylesheet = null;
            tarariStylesheet = null;
            List patterns = new ArrayList();
            for (int i = 0; i < assertion.getFetchUrlRegexes().length; i++) {
                String regex = assertion.getFetchUrlRegexes()[i];
                Pattern p;
                try {
                    p = Pattern.compile(regex);
                    patterns.add(p);
                } catch (PatternSyntaxException e) {
                    throw new ServerPolicyException(assertion, "Couldn't compile regular expression '" + regex + "'", e);
                }
            }
            this.fetchUrlPatterns = (Pattern[])patterns.toArray(new Pattern[0]);
        } else {
            final byte[] xsltBytes = assertion.getXslSrc().getBytes();
            Templates softwareStylesheet = null;
            try {
                softwareStylesheet = compileSoftware(xsltBytes);
            } catch (ParseException e) {
                auditor.logAndAudit(AssertionMessages.XSLT_BAD_XSL, new String[] { ExceptionUtils.getMessage(e) });
            }

            this.softwareStylesheet = softwareStylesheet;
            this.tarariStylesheet = compileTarari(xsltBytes);
        }
    }

    /**
     * Get the process-wide shared SSL context, creating it if this thread is the first one
     * to need it.
     *
     * @param springContext the spring context, in case one needs to be created.  Must not be null.
     * @return the current SSL context.  Never null.
     * @throws GeneralSecurityException  if an SSL context is needed but can't be created because the current server
     *                                   configuration is incomplete or invalid (keystores, truststores, and whatnot)
     */
    private static SSLContext getSslContext(ApplicationContext springContext) throws GeneralSecurityException
    {
        // no harm done if multiple threads try to create it the very first time.  s'all good.
        if (sslContext != null) return sslContext;
        synchronized(ServerXslTransformation.class) {
            if (sslContext != null) return sslContext;
        }
        SSLContext sc = SSLContext.getInstance("SSL");
        KeystoreUtils keystore = (KeystoreUtils)springContext.getBean("keystore");
        SslClientTrustManager trustManager = (SslClientTrustManager)springContext.getBean("httpRoutingAssertionTrustManager");
        KeyManager[] keyman = keystore.getSSLKeyManagerFactory().getKeyManagers();
        sc.init(keyman, new TrustManager[]{trustManager}, null);
        final int timeout = Integer.getInteger(HttpRoutingAssertion.PROP_SSL_SESSION_TIMEOUT, HttpRoutingAssertion.DEFAULT_SSL_SESSION_TIMEOUT).intValue();
        sc.getClientSessionContext().setSessionTimeout(timeout);
        synchronized(ServerXslTransformation.class) {
            return sslContext = sc;
        }
    }

    private static TarariCompiledStylesheet compileTarari(byte[] bytes) {
        TarariCompiledStylesheet tarariStylesheet = null;
        GlobalTarariContext tarariContext = TarariLoader.getGlobalContext();
        if (tarariContext != null) {
            try {
                tarariStylesheet = tarariContext.compileStylesheet(bytes);
            } catch (ParseException e) {
                logger.log(Level.WARNING, "cannot create tarari stylesheet, will operate in software mode", e);
            }
        }
        return tarariStylesheet;
    }

    private static Templates compileSoftware(byte[] bytes) throws ParseException {
        // Prepare a software template
        Templates softwareStylesheet;
        try {
            TransformerFactory transfoctory = TransformerFactory.newInstance();
            StreamSource xsltsource = new StreamSource(new ByteArrayInputStream(bytes));
            softwareStylesheet = transfoctory.newTemplates(xsltsource);
        } catch (TransformerConfigurationException e) {
            throw (ParseException)new ParseException(ExceptionUtils.getMessage(e), 0).initCause(e);
        }
        return softwareStylesheet;
    }

    /**
     * preformes the transformation
     * @param context
     */
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        // 1. Get document to transform
        final Message msgtotransform;
        final boolean isrequest;

        switch (assertion.getDirection()) {
            case XslTransformation.APPLY_TO_REQUEST:
                auditor.logAndAudit(AssertionMessages.XSLT_REQUEST);
                isrequest = true;
                msgtotransform = context.getRequest();
                break;
            case XslTransformation.APPLY_TO_RESPONSE:
                auditor.logAndAudit(AssertionMessages.XSLT_RESPONSE);
                isrequest = false;
                msgtotransform = context.getResponse();
                break;
            default:
                // should not get here!
                auditor.logAndAudit(AssertionMessages.XSLT_CONFIG_ISSUE);
                return AssertionStatus.SERVER_ERROR;
        }

        int whichMimePart = assertion.getWhichMimePart();
        if (whichMimePart <= 0) whichMimePart = 0;

        Map vars = context.getVariableMap(varsUsed, auditor);

        if (assertion.isFetchXsltFromMessageUrls()) {
            return transformFetchingly(msgtotransform, whichMimePart, vars, isrequest);
        } else {
            // 2. Apply the transformation
            return transform(msgtotransform, whichMimePart, vars, softwareStylesheet, tarariStylesheet);
        }
    }

    private AssertionStatus transformFetchingly(Message msgtotransform, int whichMimePart, Map vars, boolean isrequest) throws IOException, PolicyAssertionException {
        String href = null;
        try {
            ElementCursor ec = msgtotransform.getXmlKnob().getElementCursor();
            href = findXslHref(ec);
            if (href != null) {
                boolean anyMatch = false;
                for (int i = 0; i < fetchUrlPatterns.length; i++) {
                    Pattern fetchUrlPattern = fetchUrlPatterns[i];
                    if (fetchUrlPattern.matcher(href).matches()) anyMatch = true;
                }

                if (!anyMatch) {
                    auditor.logAndAudit(AssertionMessages.XSLT_BAD_URL, new String[] {href});
                    return AssertionStatus.BAD_REQUEST;
                }

                CachedStylesheet entry = getStylesheet(href, springContext);
                if (entry == null) {
                    // getStylesheet() already logged the reason
                    return AssertionStatus.BAD_REQUEST;
                }
                transform(msgtotransform, whichMimePart, vars, entry.softwareStylesheet, entry.tarariStylesheet);
                return AssertionStatus.NONE;
            }

            if (assertion.isFetchAllowWithoutStylesheet())
                return AssertionStatus.NONE;

            auditor.logAndAudit(AssertionMessages.XSLT_NO_PI);
            return AssertionStatus.BAD_REQUEST;
        } catch (SAXException e) {
            auditor.logAndAudit(isrequest ? AssertionMessages.XSLT_REQ_NOT_XML : AssertionMessages.XSLT_RESP_NOT_XML);
            return AssertionStatus.BAD_REQUEST;
        } catch (ParseException e) {
            auditor.logAndAudit(AssertionMessages.XSLT_BAD_EXT_XSL, new String[] { href, ExceptionUtils.getMessage(e) });
            return AssertionStatus.FAILED;
        } catch (GeneralSecurityException e) {
            auditor.logAndAudit(AssertionMessages.XSLT_CANT_READ_XSL, new String[] { href, ExceptionUtils.getMessage(e) });
            return AssertionStatus.FAILED;
        } catch (MalformedURLException e) {
            auditor.logAndAudit(AssertionMessages.XSLT_CANT_READ_XSL, new String[] { href, ExceptionUtils.getMessage(e) });
            return null;
        } catch (InvalidDocumentFormatException e) {
            auditor.logAndAudit(AssertionMessages.XSLT_MULTIPLE_PIS, new String[] { href, ExceptionUtils.getMessage(e) });
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
                if (pis.size() != 1)
                    throw new InvalidDocumentFormatException();
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

    private CachedStylesheet getStylesheet(String href, ApplicationContext spring)
            throws ParseException, GeneralSecurityException, MalformedURLException {
        GenericHttpRequestParams params = (GenericHttpRequestParams)httpRequest.get();
        final URL url = new URL(href);
        params.setTargetUrl(url);
        if (url.getProtocol().equals("https"))
            params.setSslSocketFactory(getSslContext(spring).getSocketFactory());

        final HttpObjectCache.UserObjectFactory userObjectFactory = new HttpObjectCache.UserObjectFactory() {
            public Object createUserObject(GenericHttpResponse response) throws IOException {
                try {
                    if (response.getStatus() != HttpConstants.STATUS_OK)
                        throw new IOException("HTTP status was " + response.getStatus());
                    byte[] bytes = HexUtils.slurpStreamLocalBuffer(response.getInputStream());
                    logger.fine("Downloaded new XSLT from " + url.toExternalForm());
                    final GlobalTarariContext gtc = TarariLoader.getGlobalContext();
                    TarariCompiledStylesheet tarariStylesheet = gtc == null ? null : gtc.compileStylesheet(bytes);
                    return new CachedStylesheet(tarariStylesheet, compileSoftware(bytes));
                } catch (ParseException e) {
                    final String msg = ExceptionUtils.getMessage(e);
                    // TODO uncomment this superfluous warning when debugging is complete
                    logger.warning("Unable to parse XSLT downloaded from " + url.toExternalForm() + ": " + msg);
                    throw (IOException)new IOException(msg).initCause(e);
                }
            }
        };

        // Get cached, possibly checking if-modified-since against server, possibly downloading a new stylesheet
        HttpObjectCache.FetchResult result = httpObjectCache.fetchCached(CachedStylesheet.httpClient,
                                                                         params,
                                                                         false,
                                                                         userObjectFactory);

        CachedStylesheet sheet = (CachedStylesheet)result.getUserObject();
        IOException err = result.getException();

        if (sheet == null) {
            // Didn't manage to get a stylesheet.  See if we got an error instead.
            // If it's actually a ParseException, we can just rethrow it and it'll get logged properly.
            Throwable pe = ExceptionUtils.getCauseIfCausedBy(err, ParseException.class);
            if (pe != null)
                throw (ParseException)pe;

            // Other IOExceptions we'll have to log here, as our caller won't be able to tell them apart from
            // IOExceptions due to reading the request itself.
            auditor.logAndAudit(AssertionMessages.XSLT_CANT_READ_XSL, new String[] { href, ExceptionUtils.getMessage(err) });
            return null;
        }

        // Got a sheet.  See if we need to log any warnings.
        if (err != null)
            auditor.logAndAudit(AssertionMessages.XSLT_CANT_READ_XSL2, new String[] { href, ExceptionUtils.getMessage(err) });

        return sheet;
    }

    private AssertionStatus transform(Message message, int whichPart, Map vars, Templates softwareStylesheet,
                                      TarariCompiledStylesheet tarariStylesheet)
            throws PolicyAssertionException, IOException
    {
        if (tarariStylesheet != null) {
            return runWithTarari(message, whichPart, tarariStylesheet, vars);
        } else {
            return runInSoftware(message, whichPart, softwareStylesheet, vars);
        }
    }

    private AssertionStatus runInSoftware(Message msgtotransform,
                                          int whichMimePart,
                                          Templates stylesheet,
                                          Map vars)
            throws IOException, PolicyAssertionException
    {
            if (stylesheet == null) {
                String msg = "Invalid stylesheet - assertion fails";
                auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING, new String[] {msg});
                throw new PolicyAssertionException(assertion, msg);
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
                    output = XmlUtil.softXSLTransform(doctotransform, stylesheet.newTransformer(), vars);
                } catch (TransformerException e) {
                    String msg = "error transforming document";
                    auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] {msg}, e);
                    throw new PolicyAssertionException(assertion, msg, e);
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

    private AssertionStatus runWithTarari(Message msgtotransform,
                                          int whichMimePart,
                                          TarariCompiledStylesheet stylesheet,
                                          Map vars)
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
                                stylesheet.transform(tc, output, vars);
                                transformedmessage = output.toByteArray();
                            }
                        }
                    }

                    if (transformedmessage == null) {
                        InputStream input = msgtotransform.getMimeKnob().getPart(whichMimePart).getInputStream(false);
                        stylesheet.transform(input, output, vars);
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

    private static class CachedStylesheet {
        private TarariCompiledStylesheet tarariStylesheet;
        private Templates softwareStylesheet;

        // This is in here so it doesn't get initialized until someone needs to use it
        private static final GenericHttpClient httpClient;
        static {
            MultiThreadedHttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();
            connectionManager.setMaxConnectionsPerHost(100);
            connectionManager.setMaxTotalConnections(1000);
            httpClient = new CommonsHttpClient(connectionManager);
        }

        public CachedStylesheet(TarariCompiledStylesheet tarariStylesheet, Templates softwareStylesheet) {
            this.tarariStylesheet = tarariStylesheet;
            this.softwareStylesheet = softwareStylesheet;
        }
    }

}
