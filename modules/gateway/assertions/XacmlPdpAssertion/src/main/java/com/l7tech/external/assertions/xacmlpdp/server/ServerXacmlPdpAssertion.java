package com.l7tech.external.assertions.xacmlpdp.server;

import com.l7tech.common.http.GenericHttpClientFactory;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.external.assertions.xacmlpdp.XacmlAssertionEnums;
import com.l7tech.external.assertions.xacmlpdp.XacmlPdpAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.Message;
import com.l7tech.policy.StaticResourceInfo;
import com.l7tech.policy.AssertionResourceInfo;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.ServerPolicyException;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.server.url.AbstractUrlObjectCache;
import com.l7tech.server.url.HttpObjectCache;
import com.l7tech.server.util.res.ResourceGetter;
import static com.l7tech.server.util.res.ResourceGetter.*;
import com.l7tech.server.util.res.ResourceObjectFactory;
import com.l7tech.util.CausedIOException;
import com.l7tech.util.ExceptionUtils;
import com.sun.xacml.Indenter;
import com.sun.xacml.PDP;
import com.sun.xacml.PDPConfig;
import com.sun.xacml.ParsingException;
import com.sun.xacml.ctx.RequestCtx;
import com.sun.xacml.ctx.ResponseCtx;
import com.sun.xacml.ctx.Result;
import com.sun.xacml.finder.AttributeFinder;
import com.sun.xacml.finder.AttributeFinderModule;
import com.sun.xacml.finder.PolicyFinder;
import com.sun.xacml.finder.impl.CurrentEnvModule;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.soap.SOAPConstants;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.text.ParseException;
import java.util.*;
import java.util.logging.Logger;

/**
 * Copyright (C) 2009, Layer 7 Technologies Inc.
 * User: njordan
 * Date: 13-Mar-2009
 * Time: 11:33:42 PM
 */
public class ServerXacmlPdpAssertion extends AbstractServerAssertion<XacmlPdpAssertion> {
    public ServerXacmlPdpAssertion(XacmlPdpAssertion ea, ApplicationContext applicationContext) throws ServerPolicyException {
        super(ea);
        auditor = new Auditor(this, applicationContext, logger);
        stashManagerFactory = (StashManagerFactory) applicationContext.getBean("stashManagerFactory", StashManagerFactory.class);
        envModule = new CurrentEnvModule();

        this.variablesUsed = ea.getVariablesUsed();

        ResourceObjectFactory<PolicyFinder> resourceObjectfactory =
            new ResourceObjectFactory<PolicyFinder>()
            {
                @Override
                public PolicyFinder createResourceObject(final String resourceString) throws ParseException {
                    try {
                        return cacheObjectFactory.createUserObject("", new AbstractUrlObjectCache.UserObjectSource(){
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
                        throw (ParseException)new ParseException("Unable to parse: " +
                                ExceptionUtils.getMessage(e), 0).initCause(e);
                    }
                }

                @Override
                public void closeResourceObject( final PolicyFinder resourceObject ) {
                }
            };

        //If assertion.getResourceInfo() returns a SingleUrlResourceGetter then the above ResourceObjectFactory
        //will NEVER be used by the ResourceGetter
        boolean hasPolicyVariable = false;
        AssertionResourceInfo resourceInfo = assertion.getResourceInfo();
        if ( resourceInfo instanceof StaticResourceInfo ) {
            StaticResourceInfo sri = (StaticResourceInfo) resourceInfo;
            String doc = sri.getDocument();
            if (doc != null)
                hasPolicyVariable = Syntax.getReferencedNames(doc).length > 0;
        }

        ResourceGetter<PolicyFinder> resourceGetter;
        try {
            resourceGetter = hasPolicyVariable ? null : ResourceGetter.createResourceGetter(
                    assertion,
                    assertion.getResourceInfo(),
                    resourceObjectfactory,
                    null,
                    getCache(applicationContext),
                    auditor);
        } catch ( ServerPolicyException spe ) {
            ParsingException pe = ExceptionUtils.getCauseIfCausedBy( spe, ParsingException.class );
            if ( pe == null ) {
                throw spe;
            } else {
                logger.warning( "Error parsing XACML policy '"+ExceptionUtils.getMessage( pe )+"'." );
                resourceGetter = ResourceGetter.createErrorResourceGetter( new ResourceParseException(pe, "static") );
            }
        }
        this.resourceGetter = resourceGetter;
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        try {
            AttributeFinder attrFinder = new AttributeFinder();
            List<AttributeFinderModule> attrModules = new ArrayList<AttributeFinderModule>();
            attrModules.add(envModule);
            attrModules.add(new ContextVariableAttributeFinderModule(context));
            attrFinder.setModules(attrModules);

            PolicyFinder policyFinder = getPolicyFinder(context);
            PDP pdp = new PDP(new PDPConfig(attrFinder, policyFinder, null));

            Element rootElement;
            try {
                if(assertion.getInputMessageSource() == XacmlAssertionEnums.MessageLocation.DEFAULT_RESPONSE) {
                    rootElement = context.getResponse().getXmlKnob().getDocumentReadOnly().getDocumentElement();
                } else if(assertion.getInputMessageSource() == XacmlAssertionEnums.MessageLocation.CONTEXT_VARIABLE) {
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

            if(assertion.getSoapEncapsulation().equals(XacmlPdpAssertion.SoapEncapsulationType.REQUEST) ||
                    assertion.getSoapEncapsulation().equals(XacmlPdpAssertion.SoapEncapsulationType.REQUEST_AND_RESPONSE))
            {
                rootElement = XmlUtil.findFirstChildElementByName(rootElement, rootElement.getNamespaceURI(), "Body");
                if(rootElement == null) {
                    return AssertionStatus.FAILED;
                }
                //the namespace varies with the version, do not check namespace here, it is checked below
                rootElement = XmlUtil.findFirstChildElementByName(rootElement, (String) null, "Request");
                if(rootElement == null) {
                    return AssertionStatus.FAILED;
                }
            }

            //check the namespace on rootElement is valid. The namespace represents the xacml request version
            String nameSpace = rootElement.getNamespaceURI();
            if(!XacmlAssertionEnums.XacmlVersionType.isValidXacmlVersionType(nameSpace)){
                return AssertionStatus.FAILED;
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
                        status = AssertionStatus.FALSIFIED;
                    }
                }
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            byte[] messageBytes;
            if(assertion.getSoapEncapsulation().equals(XacmlPdpAssertion.SoapEncapsulationType.RESPONSE) ||
                    assertion.getSoapEncapsulation().equals(XacmlPdpAssertion.SoapEncapsulationType.REQUEST_AND_RESPONSE))
            {
                responseCtx.encode(baos, new Indenter() {
                    private String spaces = "    ";

                    @Override
                    public void in() {
                        spaces += "  ";
                    }

                    @Override
                    public String makeString() {
                        return spaces;
                    }

                    @Override
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
            if(assertion.getOutputMessageTarget() == XacmlAssertionEnums.MessageLocation.CONTEXT_VARIABLE) {
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
        } catch (InvalidPolicyException e) {
            auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] {ExceptionUtils.getMessage(e)}, ExceptionUtils.getDebugException(e));
            return AssertionStatus.SERVER_ERROR;
        }
    }

    /**
     * Access the PolicyFinder
     *
     * @throws IOException If there is an IO issue for the request message
     * @throws InvalidMessageException If the request is not valid
     * @throws InvalidPolicyException If there is a problem with the XACML policy
     */
    private PolicyFinder getPolicyFinder( final PolicyEnforcementContext context ) throws IOException, InvalidPolicyException {
        if (resourceGetter != null) {
            try {
                return resourceGetter.getResource(null, new HashMap<String, String>());
            } catch (ResourceIOException e) {
                throw new InvalidPolicyException("Error accessing XACML policy resource '" + ExceptionUtils.getMessage(e) + "'.", e);
            } catch (GeneralSecurityException e) {
                throw new InvalidPolicyException("Error accessing XACML policy resource '" + ExceptionUtils.getMessage(e) + "'.", e);
            } catch (ResourceParseException e) {
                throw new InvalidPolicyException("Invalid XACML policy '" + ExceptionUtils.getMessage(e) + "'.", e );
            } catch (UrlResourceException e) { // should not happen since we are not examining the message
                throw new InvalidPolicyException("URL error accessing XACML policy resource '" + ExceptionUtils.getMessage(e) + "'.", e);
            } catch (UrlNotFoundException e) { // should not happen since we are not examining the message
                throw new InvalidPolicyException("URL error accessing XACML policy resource '" + ExceptionUtils.getMessage(e) + "'.", e);
            } catch (InvalidMessageException e) { // should not happen since we are not examining the message
                throw new InvalidPolicyException("URL error accessing XACML policy resource '" + ExceptionUtils.getMessage(e) + "'.", e);
            }
        }
        PolicyFinder policyFinder = new PolicyFinder();
        try {
            String policy = ((StaticResourceInfo)assertion.getResourceInfo()).getDocument();
            String expandedPolicy = ExpandVariables.process(policy, context.getVariableMap(variablesUsed, auditor), auditor, false);
            ConstantPolicyModule policyModule = new ConstantPolicyModule(expandedPolicy);
            Set<ConstantPolicyModule> policyModules = new HashSet<ConstantPolicyModule>();
            policyModules.add(policyModule);
            policyFinder.setModules(policyModules);
        } catch (IOException e) {
            throw new InvalidPolicyException("Error processing XACML policy after variable expansion '" + ExceptionUtils.getMessage(e) + "'.", e);
        } catch (SAXException e) {
            throw new InvalidPolicyException("Invalid XACML policy after variable expansion '" + ExceptionUtils.getMessage(e) + "'.", e);
        } catch (ParsingException e) {
            throw new InvalidPolicyException("Invalid XACML policy after variable expansion '" + ExceptionUtils.getMessage(e) + "'.", e);
        }

        return policyFinder;
    }

    private static synchronized HttpObjectCache<PolicyFinder> getCache(BeanFactory spring) {
        if (httpObjectCache != null)
            return httpObjectCache;

        GenericHttpClientFactory clientFactory = (GenericHttpClientFactory)spring.getBean("httpClientFactory");
        if (clientFactory == null) throw new IllegalStateException("No httpClientFactory bean");

        httpObjectCache = new HttpObjectCache<PolicyFinder>(
                ServerConfig.getInstance().getIntProperty(XacmlPdpAssertion.PARAM_XACML_POLICY_CACHE_MAX_ENTRIES, 100),
                ServerConfig.getInstance().getIntProperty(XacmlPdpAssertion.PARAM_XACML_POLICY_CACHE_MAX_AGE, 300000),
                clientFactory,
                cacheObjectFactory,
                HttpObjectCache.WAIT_INITIAL);

        return httpObjectCache;
    }

    private CurrentEnvModule envModule;
    private final ResourceGetter<PolicyFinder> resourceGetter;
    private final String[] variablesUsed;

    private static final Logger logger = Logger.getLogger(ServerXacmlPdpAssertion.class.getName());
    private final Auditor auditor;

    private final StashManagerFactory stashManagerFactory;
    private static HttpObjectCache<PolicyFinder> httpObjectCache = null;

    private static final class InvalidPolicyException extends Exception {
        public InvalidPolicyException( String message, Throwable cause ) {
            super( message, cause );
        }
    }

    private static final HttpObjectCache.UserObjectFactory<PolicyFinder> cacheObjectFactory =
                new HttpObjectCache.UserObjectFactory<PolicyFinder>() {
                    @Override
                    public PolicyFinder createUserObject(String url, AbstractUrlObjectCache.UserObjectSource responseSource) throws IOException {
                        String response = responseSource.getString(true);
                        try {
                            ConstantPolicyModule policyModule = new ConstantPolicyModule(response);
                            PolicyFinder policyFinder = new PolicyFinder();
                            Set<ConstantPolicyModule> policyModules = new HashSet<ConstantPolicyModule>();
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
