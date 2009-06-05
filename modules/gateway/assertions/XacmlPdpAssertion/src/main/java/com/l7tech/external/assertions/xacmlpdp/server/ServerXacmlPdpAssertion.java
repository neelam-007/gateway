package com.l7tech.external.assertions.xacmlpdp.server;

import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.ServerPolicyException;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.util.res.ResourceGetter;
import com.l7tech.server.util.res.ResourceObjectFactory;
import com.l7tech.server.util.res.UrlFinder;
import com.l7tech.external.assertions.xacmlpdp.XacmlPdpAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.util.CausedIOException;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.message.Message;
import com.l7tech.server.url.HttpObjectCache;
import com.l7tech.server.url.AbstractUrlObjectCache;
import com.l7tech.common.http.GenericHttpClientFactory;
import com.l7tech.xml.ElementCursor;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.server.cluster.ClusterPropertyCache;
import com.sun.xacml.*;
import com.sun.xacml.ctx.RequestCtx;
import com.sun.xacml.ctx.ResponseCtx;
import com.sun.xacml.ctx.Result;
import com.sun.xacml.finder.impl.CurrentEnvModule;
import com.sun.xacml.finder.PolicyFinder;
import com.sun.xacml.finder.AttributeFinder;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.logging.Logger;
import java.util.*;
import java.text.ParseException;
import java.security.GeneralSecurityException;

import org.springframework.context.ApplicationContext;
import org.springframework.beans.factory.BeanFactory;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.soap.SOAPConstants;

/**
 * Copyright (C) 2009, Layer 7 Technologies Inc.
 * User: njordan
 * Date: 13-Mar-2009
 * Time: 11:33:42 PM
 * To change this template use File | Settings | File Templates.
 */
public class ServerXacmlPdpAssertion extends AbstractServerAssertion<XacmlPdpAssertion> {
    public ServerXacmlPdpAssertion(XacmlPdpAssertion ea, ApplicationContext applicationContext) throws ServerPolicyException {
        super(ea);
        auditor = new Auditor(this, applicationContext, logger);
        stashManagerFactory = (StashManagerFactory) applicationContext.getBean("stashManagerFactory", StashManagerFactory.class);
        ClusterPropertyCache clusterPropertyCache = (ClusterPropertyCache) applicationContext.getBean("clusterPropertyCache", ClusterPropertyCache.class);

        envModule = new CurrentEnvModule();
        clusterPropertyEnvModule = new ClusterPropertyAttributeFinderModule(clusterPropertyCache);

        ResourceObjectFactory<PolicyFinder> resourceObjectfactory =
            new ResourceObjectFactory<PolicyFinder>()
            {
                public PolicyFinder createResourceObject(final String resourceString) throws ParseException {
                    try {
                        return cacheObjectFactory.createUserObject("", new AbstractUrlObjectCache.UserObjectSource(){
                            public byte[] getBytes() throws IOException {
                                throw new IOException("Not supported");
                            }
                            public ContentTypeHeader getContentType() {
                                return null;
                            }
                            public String getString(boolean isXml) {
                                return resourceString;
                            }
                        });
                    } catch (IOException e) {
                        throw (ParseException)new ParseException("Unable to parse stylesheet: " +
                                ExceptionUtils.getMessage(e), 0).initCause(e);
                    }
                }

                public void closeResourceObject( final PolicyFinder resourceObject ) {
                }
            };

        UrlFinder urlFinder = new UrlFinder() {
            public String findUrl(ElementCursor message) throws ResourceGetter.InvalidMessageException {
                return null;
            }
        };

        resourceGetter = ResourceGetter.createResourceGetter(
            assertion, assertion.getResourceInfo(), resourceObjectfactory, urlFinder, getCache(applicationContext), auditor);
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        try {
            AttributeFinder attrFinder = new AttributeFinder();
            List attrModules = new ArrayList();
            attrModules.add(envModule);
            attrModules.add(new ContextVariableAttributeFinderModule(context));
            attrModules.add(clusterPropertyEnvModule);
            attrFinder.setModules(attrModules);

            PolicyFinder policyFinder = resourceGetter.getResource(null, new HashMap<String, String>());
            PDP pdp = new PDP(new PDPConfig(attrFinder, policyFinder, null));

            Element rootElement;
            try {
                if(assertion.getInputMessageSource() == XacmlPdpAssertion.RESPONSE_MESSAGE) {
                    rootElement = context.getResponse().getXmlKnob().getDocumentReadOnly().getDocumentElement();
                } else if(assertion.getInputMessageSource() == XacmlPdpAssertion.MESSAGE_VARIABLE) {
                    try {
                        rootElement = ((Message)context.getVariable(assertion.getInputMessageVariableName())).getXmlKnob().getDocumentReadOnly().getDocumentElement();
                    } catch(NoSuchVariableException nsve) {
                        return AssertionStatus.FAILED;
                    }
                } else {
                    rootElement = context.getRequest().getXmlKnob().getDocumentReadOnly().getDocumentElement();
                }
            } catch(SAXException se) {
                return AssertionStatus.FAILED;
            }

            if(assertion.getSoapEncapsulation().equals(XacmlPdpAssertion.SOAP_ENCAPSULATION_VALUES[1]) ||
                    assertion.getSoapEncapsulation().equals(XacmlPdpAssertion.SOAP_ENCAPSULATION_VALUES[3]))
            {
                rootElement = XmlUtil.findFirstChildElementByName(rootElement, rootElement.getNamespaceURI(), "Body");
                if(rootElement == null) {
                    return AssertionStatus.FAILED;
                }
                rootElement = XmlUtil.findFirstChildElementByName(rootElement, "urn:oasis:names:tc:xacml:2.0:context", "Request");
                if(rootElement == null) {
                    return AssertionStatus.FAILED;
                }
            }

            RequestCtx requestCtx = RequestCtx.getInstance(rootElement);
            ResponseCtx responseCtx = pdp.evaluate(requestCtx);

            AssertionStatus status = AssertionStatus.NONE;

            if(assertion.getFailIfNotPermit()) {
                Set results = responseCtx.getResults();
                if(results.isEmpty() || results.size() > 1) {
                    status = AssertionStatus.FAILED;
                } else {
                    Result result = (Result)results.iterator().next();
                    if(result.getDecision() != Result.DECISION_PERMIT) {
                        status = AssertionStatus.FAILED;
                    }
                }
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            byte[] messageBytes;
            if(assertion.getSoapEncapsulation().equals(XacmlPdpAssertion.SOAP_ENCAPSULATION_VALUES[2]) ||
                    assertion.getSoapEncapsulation().equals(XacmlPdpAssertion.SOAP_ENCAPSULATION_VALUES[3]))
            {
                responseCtx.encode(baos, new Indenter() {
                    private String spaces = "    ";

                    public void in() {
                        spaces += "  ";
                    }

                    public String makeString() {
                        return spaces;
                    }

                    public void out() {
                        spaces = spaces.substring(2);
                    }
                });

                byte[] prefix = ("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<soapenv:Envelope xmlns:soapenv=\"" + SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE + "\">\n  <soapenv:Header/>\n  <soapenv:Body>\n").getBytes("UTF-8");
                byte[] suffix = "  </soapenv:Body>\n</soapenv:Envelope>\n".getBytes("UTF-8");
                byte[] content = baos.toByteArray();

                messageBytes = new byte[prefix.length + content.length + suffix.length];

                System.arraycopy(prefix, 0, messageBytes, 0, prefix.length);
                System.arraycopy(content, 0, messageBytes, prefix.length, content.length);
                System.arraycopy(suffix, 0, messageBytes, prefix.length + content.length, suffix.length);
            } else {
                responseCtx.encode(baos, new Indenter(2));
                messageBytes = baos.toByteArray();
            }

            ByteArrayInputStream bais = new ByteArrayInputStream(messageBytes);

            ContentTypeHeader cth = ContentTypeHeader.parseValue("text/xml; charset=UTF-8");
            if(assertion.getOutputMessageTarget() == XacmlPdpAssertion.MESSAGE_VARIABLE) {
                Message m = new Message(stashManagerFactory.createStashManager(),
                        cth,
                        bais);
                context.setVariable(assertion.getOutputMessageVariableName(), m);
            } else {
                context.getResponse().initialize(stashManagerFactory.createStashManager(),
                        cth,
                        bais);
            }

            return status;
        } catch(NoSuchPartException nspe) {
            return AssertionStatus.FAILED;
        } catch(ParsingException pe) {
            return AssertionStatus.FAILED;
        } catch(ResourceGetter.InvalidMessageException ime) {
            auditor.logAndAudit(AssertionMessages.REQUEST_NOT_SOAP);
            return AssertionStatus.BAD_REQUEST;
        } catch(ResourceGetter.UrlNotFoundException unfe) {
            return AssertionStatus.BAD_REQUEST;
        } catch(ResourceGetter.MalformedResourceUrlException mue) {
            return AssertionStatus.BAD_REQUEST;
        } catch(ResourceGetter.UrlNotPermittedException unpe) {
            return AssertionStatus.BAD_REQUEST;
        } catch(ResourceGetter.ResourceIOException rioe) {
            return AssertionStatus.BAD_REQUEST;
        } catch(ResourceGetter.ResourceParseException rpe) {
            return AssertionStatus.BAD_REQUEST;
        } catch(GeneralSecurityException gse) {
            return AssertionStatus.BAD_REQUEST;
        }
    }

    private static synchronized HttpObjectCache<PolicyFinder> getCache(BeanFactory spring) {
        if (httpObjectCache != null)
            return httpObjectCache;

        GenericHttpClientFactory clientFactory = (GenericHttpClientFactory)spring.getBean("httpClientFactory");
        if (clientFactory == null) throw new IllegalStateException("No httpClientFactory bean");

        httpObjectCache = new HttpObjectCache<PolicyFinder>(
                    ServerConfig.getInstance().getIntProperty(ServerConfig.PARAM_XSLT_CACHE_MAX_ENTRIES, 10000),
                    ServerConfig.getInstance().getIntProperty(ServerConfig.PARAM_XSLT_CACHE_MAX_AGE, 300000),
                    clientFactory, cacheObjectFactory, HttpObjectCache.WAIT_INITIAL);

        return httpObjectCache;
    }

    private CurrentEnvModule envModule;
    private ClusterPropertyAttributeFinderModule clusterPropertyEnvModule;
    private final ResourceGetter<PolicyFinder> resourceGetter;

    private static final Logger logger = Logger.getLogger(ServerXacmlPdpAssertion.class.getName());
    private final Auditor auditor;

    private final StashManagerFactory stashManagerFactory;
    private static HttpObjectCache<PolicyFinder> httpObjectCache = null;

    private static final HttpObjectCache.UserObjectFactory<PolicyFinder> cacheObjectFactory =
                new HttpObjectCache.UserObjectFactory<PolicyFinder>() {
                    public PolicyFinder createUserObject(String url, AbstractUrlObjectCache.UserObjectSource responseSource) throws IOException {
                        String response = responseSource.getString(true);
                        try {
                            ConstantPolicyModule policyModule = new ConstantPolicyModule(response);
                            PolicyFinder policyFinder = new PolicyFinder();
                            Set policyModules = new HashSet();
                            policyModules.add(policyModule);
                            policyFinder.setModules(policyModules);

                            return policyFinder;
                        } catch(SAXException saxe) {
                            throw new CausedIOException(saxe);
                        } catch(ParsingException pe) {
                            throw new CausedIOException(pe);
                        }
                    }
                };
}
